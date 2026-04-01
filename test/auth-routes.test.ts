import { mkdtemp, rm, writeFile } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join } from 'node:path';

import { afterEach, describe, expect, it } from 'vitest';

import { createApp } from '../src/app.js';
import type { AppAuth } from '../src/auth/types.js';
import type { AppConfig } from '../src/config.js';

const createdDirectories: string[] = [];
const testPlayerUuid = '24c56ab5-b504-4d38-bd06-e5af6686fcaa';

function createTestConfig(cosmeticsFile: string): AppConfig {
  return {
    NODE_ENV: 'test',
    PORT: 3000,
    HOST: '127.0.0.1',
    TRUST_PROXY: false,
    COSMETICS_FILE: cosmeticsFile,
    API_KEY: 'admin-secret-key',
    FRONTEND_ORIGIN: 'http://localhost:5173',
    BETTER_AUTH_URL: 'http://127.0.0.1:3000',
    BETTER_AUTH_SECRET: 'test-secret-which-is-long-enough-for-better-auth',
    BETTER_AUTH_BASE_PATH: '/api/auth',
    SESSION_TTL_SECONDS: 3600,
    SESSION_COOKIE_SECURE: false,
    SESSION_COOKIE_SAME_SITE: 'lax',
    MCID_CLIENT_ID: 'mcid-client-id',
    MCID_CLIENT_SECRET: 'mcid-client-secret',
    MCID_DISCOVERY_URL: 'https://mc-id.com/.well-known/openid-configuration'
  };
}

async function createTempCosmeticsFile(): Promise<string> {
  const directory = await mkdtemp(join(tmpdir(), 'xenaddons-auth-'));
  const filePath = join(directory, 'cosmeticPeople.json');

  createdDirectories.push(directory);

  await writeFile(
    filePath,
    `${JSON.stringify(
      {
        [testPlayerUuid]: {
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

function createAuthenticatedAuth(): AppAuth {
  return {
    handler: async (request) => {
      if (request.url.endsWith('/api/auth/test')) {
        return new Response(JSON.stringify({ ok: true }), {
          status: 200,
          headers: {
            'content-type': 'application/json'
          }
        });
      }

      return new Response(null, { status: 204 });
    },
    api: {
      getSession: async () => ({
        session: {
          id: 'session-id',
          expiresAt: new Date('2026-04-01T12:00:00.000Z')
        },
        user: {
          id: 'user-id',
          email: 'user@example.com',
          emailVerified: true,
          name: 'Notch',
          minecraftUuid: testPlayerUuid,
          minecraftUsername: 'Notch'
        }
      })
    }
  };
}

afterEach(async () => {
  while (createdDirectories.length > 0) {
    const directory = createdDirectories.pop();

    if (directory) {
      await rm(directory, { recursive: true, force: true });
    }
  }
});

describe('better-auth integration', () => {
  it('proxies the mounted better-auth handler', async () => {
    const cosmeticsFile = await createTempCosmeticsFile();
    const app = await createApp({
      auth: createAuthenticatedAuth(),
      config: createTestConfig(cosmeticsFile),
      logger: false
    });

    try {
      const response = await app.inject({
        method: 'GET',
        url: '/api/auth/test'
      });

      expect(response.statusCode).toBe(200);
      expect(response.json()).toEqual({
        ok: true
      });
    } finally {
      await app.close();
    }
  });

  it('returns the authenticated Minecraft player and allows self-service username updates', async () => {
    const cosmeticsFile = await createTempCosmeticsFile();
    const app = await createApp({
      auth: createAuthenticatedAuth(),
      config: createTestConfig(cosmeticsFile),
      logger: false
    });

    try {
      const meResponse = await app.inject({
        method: 'GET',
        url: '/me'
      });

      expect(meResponse.statusCode).toBe(200);
      expect(meResponse.json()).toEqual({
        authenticated: true,
        authorized: true,
        authUser: {
          email: 'user@example.com',
          emailVerified: true,
          id: 'user-id',
          minecraftUsername: 'Notch',
          minecraftUuid: testPlayerUuid,
          name: 'Notch'
        },
        player: {
          uuid: testPlayerUuid,
          username: 'dtXenus',
          aliases: ['dtXenus'],
          displayName: 'InitialName',
          badges: [],
          scale: null
        }
      });

      const patchResponse = await app.inject({
        method: 'PATCH',
        url: '/me/username',
        headers: {
          origin: 'http://localhost:5173'
        },
        payload: {
          username: 'RenamedPlayer'
        }
      });

      expect(patchResponse.statusCode).toBe(200);
      expect(patchResponse.json()).toEqual({
        version: 1,
        updatedAt: expect.any(String),
        player: {
          uuid: testPlayerUuid,
          username: 'RenamedPlayer',
          aliases: ['dtXenus'],
          displayName: 'InitialName',
          badges: [],
          scale: null
        }
      });
    } finally {
      await app.close();
    }
  });
});
