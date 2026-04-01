import { mkdtemp, readFile, rm, writeFile } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join } from 'node:path';

import { afterEach, describe, expect, it } from 'vitest';

import { createApp } from '../src/app.js';
import type { AppConfig } from '../src/config.js';

const createdDirectories: string[] = [];

function createTestConfig(cosmeticsFile: string): AppConfig {
  return {
    NODE_ENV: 'test',
    PORT: 3000,
    HOST: '127.0.0.1',
    TRUST_PROXY: false,
    COSMETICS_FILE: cosmeticsFile,
    API_KEY: 'super-secret-key',
    FRONTEND_ORIGIN: 'http://localhost:5173',
    SESSION_SECRET: 'test-session-secret-which-is-long-enough',
    SESSION_TTL_SECONDS: 3600,
    SESSION_COOKIE_SECURE: false,
    SESSION_COOKIE_SAME_SITE: 'lax',
    MCID_API_KEY: 'mcid-api-key',
    MCID_CODE_REQUEST_URL: 'https://mc-id.com/api/v1/codes/request',
    MCID_CODE_VERIFY_URL: 'https://mc-id.com/api/v1/codes/verify'
  };
}

async function createTempCosmeticsFile(): Promise<string> {
  const directory = await mkdtemp(join(tmpdir(), 'xenaddons-cosmetics-'));
  const filePath = join(directory, 'cosmeticPeople.json');

  createdDirectories.push(directory);

  await writeFile(
    filePath,
    `${JSON.stringify(
      {
        '24c56ab5-b504-4d38-bd06-e5af6686fcaa': {
          username: 'dtXenus',
          aliases: ['dtXenus'],
          displayName: 'InitialName',
          sizeX: 2,
          sizeY: 0.5,
          sizeZ: 1
        }
      },
      null,
      2
    )}\n`,
    'utf8'
  );

  return filePath;
}

afterEach(async () => {
  while (createdDirectories.length > 0) {
    const directory = createdDirectories.pop();

    if (directory) {
      await rm(directory, { recursive: true, force: true });
    }
  }
});

describe('PATCH /v1/cosmetics/:uuid', () => {
  it('rejects requests without a valid API key', async () => {
    const cosmeticsFile = await createTempCosmeticsFile();
    const app = await createApp({
      config: createTestConfig(cosmeticsFile),
      logger: false
    });

    try {
      const response = await app.inject({
        method: 'GET',
        url: '/v1/cosmetics'
      });

      expect(response.statusCode).toBe(401);
      expect(response.json()).toEqual({
        error: 'Unauthorized'
      });
    } finally {
      await app.close();
    }
  });

  it('updates only displayName and scale in the source file and response snapshot', async () => {
    const cosmeticsFile = await createTempCosmeticsFile();
    const app = await createApp({
      config: createTestConfig(cosmeticsFile),
      logger: false
    });

    try {
      const response = await app.inject({
        method: 'PATCH',
        url: '/v1/cosmetics/24c56ab5-b504-4d38-bd06-e5af6686fcaa',
        headers: {
          'x-api-key': 'super-secret-key'
        },
        payload: {
          displayName: 'UpdatedName',
          scale: {
            x: 1.25,
            y: 0.75,
            z: 0.5
          }
        }
      });

      expect(response.statusCode).toBe(200);
      expect(response.headers['cache-control']).toBe('no-store');
      expect(response.json()).toEqual({
        version: 1,
        updatedAt: expect.any(String),
        player: {
          uuid: '24c56ab5-b504-4d38-bd06-e5af6686fcaa',
          username: 'dtXenus',
          aliases: ['dtXenus'],
          displayName: 'UpdatedName',
          scale: {
            x: 1.25,
            y: 0.75,
            z: 0.5
          }
        }
      });

      const fileContents = await readFile(cosmeticsFile, 'utf8');
      expect(JSON.parse(fileContents)).toEqual({
        '24c56ab5-b504-4d38-bd06-e5af6686fcaa': {
          username: 'dtXenus',
          aliases: ['dtXenus'],
          displayName: 'UpdatedName',
          sizeX: 1.25,
          sizeY: 0.75,
          sizeZ: 0.5
        }
      });
    } finally {
      await app.close();
    }
  });

  it('rejects forbidden writable fields like username and aliases', async () => {
    const cosmeticsFile = await createTempCosmeticsFile();
    const app = await createApp({
      config: createTestConfig(cosmeticsFile),
      logger: false
    });

    try {
      const response = await app.inject({
        method: 'PATCH',
        url: '/v1/cosmetics/24c56ab5-b504-4d38-bd06-e5af6686fcaa',
        headers: {
          'x-api-key': 'super-secret-key'
        },
        payload: {
          username: 'forbidden'
        }
      });

      expect(response.statusCode).toBe(400);
      expect(response.json()).toEqual({
        error: 'Invalid request body',
        details: expect.stringContaining('Unrecognized key')
      });
    } finally {
      await app.close();
    }
  });
});
