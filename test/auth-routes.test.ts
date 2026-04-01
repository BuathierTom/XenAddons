import { mkdtemp, rm, writeFile } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join } from 'node:path';

import { afterEach, describe, expect, it } from 'vitest';

import type { McidClient } from '../src/auth/types.js';
import { createApp } from '../src/app.js';
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
  const directory = await mkdtemp(join(tmpdir(), 'xenaddons-auth-'));
  const filePath = join(directory, 'cosmeticPeople.json');

  createdDirectories.push(directory);

  await writeFile(
    filePath,
    `${JSON.stringify(
      {
        [testPlayerUuid]: {
          username: 'dtXenus',
          aliases: ['dtXenus', 'dtx'],
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

function createFakeMcidClient(verifiedUuid: string = testPlayerUuid): McidClient {
  return {
    requestCode: async ({ uuid }) => ({
      expiresAt: '2026-04-01T12:05:00.000Z',
      message: 'Successfully generated the code.',
      userId: uuid.replaceAll('-', '')
    }),
    verifyCode: async () => ({
      userId: verifiedUuid.replaceAll('-', ''),
      username: 'Notch'
    })
  };
}

function toCookieHeader(value: string | string[] | undefined): string {
  const firstCookie = Array.isArray(value) ? value[0] : value;

  if (!firstCookie) {
    throw new Error('Missing Set-Cookie header');
  }

  return firstCookie.split(';', 1)[0];
}

afterEach(async () => {
  while (createdDirectories.length > 0) {
    const directory = createdDirectories.pop();

    if (directory) {
      await rm(directory, { recursive: true, force: true });
    }
  }
});

describe('MC-ID verification-code auth routes', () => {
  it('requests a code, verifies it, creates a session, and allows self-service updates', async () => {
    const cosmeticsFile = await createTempCosmeticsFile();
    const app = await createApp({
      mcidClient: createFakeMcidClient(),
      config: createTestConfig(cosmeticsFile),
      logger: false
    });

    try {
      const configResponse = await app.inject({
        method: 'GET',
        url: '/auth/mc-id/config'
      });

      expect(configResponse.statusCode).toBe(200);
      expect(configResponse.json()).toEqual({
        codeLength: 6,
        codeTtlSeconds: 300,
        flow: 'verification_code',
        identifierField: 'identifier'
      });

      const requestCodeResponse = await app.inject({
        method: 'POST',
        url: '/auth/mc-id/request-code',
        headers: {
          origin: 'http://localhost:5173'
        },
        payload: {
          identifier: 'dtx'
        }
      });

      expect(requestCodeResponse.statusCode).toBe(200);
      expect(requestCodeResponse.json()).toEqual({
        expiresAt: '2026-04-01T12:05:00.000Z',
        message: 'Successfully generated the code.',
        player: {
          uuid: testPlayerUuid,
          username: 'dtXenus',
          aliases: ['dtXenus', 'dtx'],
          displayName: 'InitialName',
          scale: {
            x: 2,
            y: 0.5,
            z: 1
          }
        },
        requested: true
      });

      const verifyCodeResponse = await app.inject({
        method: 'POST',
        url: '/auth/mc-id/verify-code',
        headers: {
          origin: 'http://localhost:5173'
        },
        payload: {
          code: '123456',
          uuid: testPlayerUuid
        }
      });

      expect(verifyCodeResponse.statusCode).toBe(200);
      expect(verifyCodeResponse.json()).toEqual({
        authenticated: true,
        authorized: true,
        authUser: {
          email: null,
          emailVerified: null,
          id: `mcid:${testPlayerUuid}`,
          minecraftUsername: 'Notch',
          minecraftUuid: testPlayerUuid,
          name: 'Notch'
        },
        player: {
          uuid: testPlayerUuid,
          username: 'dtXenus',
          aliases: ['dtXenus', 'dtx'],
          displayName: 'InitialName',
          scale: {
            x: 2,
            y: 0.5,
            z: 1
          }
        }
      });

      const cookieHeader = toCookieHeader(verifyCodeResponse.headers['set-cookie']);

      const meResponse = await app.inject({
        method: 'GET',
        url: '/me',
        headers: {
          cookie: cookieHeader
        }
      });

      expect(meResponse.statusCode).toBe(200);
      expect(meResponse.json()).toEqual({
        authenticated: true,
        authorized: true,
        authUser: {
          email: null,
          emailVerified: null,
          id: `mcid:${testPlayerUuid}`,
          minecraftUsername: 'Notch',
          minecraftUuid: testPlayerUuid,
          name: 'Notch'
        },
        player: {
          uuid: testPlayerUuid,
          username: 'dtXenus',
          aliases: ['dtXenus', 'dtx'],
          displayName: 'InitialName',
          scale: {
            x: 2,
            y: 0.5,
            z: 1
          }
        }
      });

      const patchResponse = await app.inject({
        method: 'PATCH',
        url: '/me/cosmetics',
        headers: {
          cookie: cookieHeader,
          origin: 'http://localhost:5173'
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

      expect(patchResponse.statusCode).toBe(200);
      expect(patchResponse.json()).toEqual({
        version: 1,
        updatedAt: expect.any(String),
        player: {
          uuid: testPlayerUuid,
          username: 'dtXenus',
          aliases: ['dtXenus', 'dtx'],
          displayName: 'UpdatedName',
          scale: {
            x: 1.25,
            y: 0.75,
            z: 0.5
          }
        }
      });

      const forbiddenFieldResponse = await app.inject({
        method: 'PATCH',
        url: '/me/cosmetics',
        headers: {
          cookie: cookieHeader,
          origin: 'http://localhost:5173'
        },
        payload: {
          username: 'should-not-work'
        }
      });

      expect(forbiddenFieldResponse.statusCode).toBe(400);
      expect(forbiddenFieldResponse.json()).toEqual({
        error: 'Invalid request body',
        details: expect.stringContaining('Unrecognized key')
      });

      const logoutResponse = await app.inject({
        method: 'POST',
        url: '/auth/logout',
        headers: {
          cookie: cookieHeader,
          origin: 'http://localhost:5173'
        }
      });

      expect(logoutResponse.statusCode).toBe(204);

      const afterLogoutResponse = await app.inject({
        method: 'GET',
        url: '/me',
        headers: {
          cookie: cookieHeader
        }
      });

      expect(afterLogoutResponse.statusCode).toBe(401);
      expect(afterLogoutResponse.json()).toEqual({
        authenticated: false
      });
    } finally {
      await app.close();
    }
  });

  it('rejects unknown players before calling MC-ID verify', async () => {
    const cosmeticsFile = await createTempCosmeticsFile();
    const app = await createApp({
      mcidClient: createFakeMcidClient('11111111-1111-4111-8111-111111111111'),
      config: createTestConfig(cosmeticsFile),
      logger: false
    });

    try {
      const requestResponse = await app.inject({
        method: 'POST',
        url: '/auth/mc-id/request-code',
        headers: {
          origin: 'http://localhost:5173'
        },
        payload: {
          identifier: 'unknown-user'
        }
      });

      expect(requestResponse.statusCode).toBe(404);
      expect(requestResponse.json()).toEqual({
        error: 'Player not found'
      });

      const verifyResponse = await app.inject({
        method: 'POST',
        url: '/auth/mc-id/verify-code',
        headers: {
          origin: 'http://localhost:5173'
        },
        payload: {
          code: '123456',
          uuid: '11111111-1111-4111-8111-111111111111'
        }
      });

      expect(verifyResponse.statusCode).toBe(404);
      expect(verifyResponse.json()).toEqual({
        error: 'Player not found'
      });
    } finally {
      await app.close();
    }
  });
});
