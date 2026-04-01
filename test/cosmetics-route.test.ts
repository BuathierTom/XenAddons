import { mkdtemp, readFile, rm, writeFile } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join } from 'node:path';

import { afterEach, describe, expect, it } from 'vitest';

import { createApp } from '../src/app.js';

const createdDirectories: string[] = [];

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
          displayName: 'InitialName'
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

describe('PATCH /v1/cosmetics/:uuid/username', () => {
  it('rejects requests without a valid API key', async () => {
    const cosmeticsFile = await createTempCosmeticsFile();
    const app = await createApp({
      config: {
        PORT: 3000,
        HOST: '127.0.0.1',
        COSMETICS_FILE: cosmeticsFile,
        API_KEY: 'super-secret-key'
      },
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

  it('updates the username in the source file and response snapshot', async () => {
    const cosmeticsFile = await createTempCosmeticsFile();
    const app = await createApp({
      config: {
        PORT: 3000,
        HOST: '127.0.0.1',
        COSMETICS_FILE: cosmeticsFile,
        API_KEY: 'super-secret-key'
      },
      logger: false
    });

    try {
      const response = await app.inject({
        method: 'PATCH',
        url: '/v1/cosmetics/24c56ab5-b504-4d38-bd06-e5af6686fcaa/username',
        headers: {
          'x-api-key': 'super-secret-key'
        },
        payload: {
          username: 'RenamedPlayer'
        }
      });

      expect(response.statusCode).toBe(200);
      expect(response.headers['cache-control']).toBe('no-store');
      expect(response.json()).toEqual({
        version: 1,
        updatedAt: expect.any(String),
        player: {
          uuid: '24c56ab5-b504-4d38-bd06-e5af6686fcaa',
          username: 'RenamedPlayer',
          aliases: ['dtXenus'],
          displayName: 'InitialName',
          badges: [],
          scale: null
        }
      });

      const fileContents = await readFile(cosmeticsFile, 'utf8');
      expect(JSON.parse(fileContents)).toEqual({
        '24c56ab5-b504-4d38-bd06-e5af6686fcaa': {
          username: 'RenamedPlayer',
          aliases: ['dtXenus'],
          displayName: 'InitialName'
        }
      });
    } finally {
      await app.close();
    }
  });
});
