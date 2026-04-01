import { APIError, betterAuth } from 'better-auth';
import { genericOAuth } from 'better-auth/plugins';

import type { AppConfig } from '../config.js';
import type { CosmeticsFileStore } from '../lib/file-store.js';
import type { AppAuth } from './types.js';

interface McidAccount {
  primary?: boolean;
  username?: string;
  uuid?: string;
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
}

function canonicalizeMinecraftUuid(value: string): string | null {
  const trimmedValue = value.trim().toLowerCase();

  if (/^[0-9a-f]{32}$/.test(trimmedValue)) {
    return `${trimmedValue.slice(0, 8)}-${trimmedValue.slice(8, 12)}-${trimmedValue.slice(
      12,
      16
    )}-${trimmedValue.slice(16, 20)}-${trimmedValue.slice(20)}`;
  }

  if (
    /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(
      trimmedValue
    )
  ) {
    return trimmedValue;
  }

  return null;
}

export function extractPrimaryMcidAccount(profile: Record<string, unknown>): {
  username: string | null;
  uuid: string | null;
} {
  const accounts = Array.isArray(profile.accounts)
    ? profile.accounts.filter(isRecord) as McidAccount[]
    : [];
  const primaryAccount =
    accounts.find((account) => account.primary === true && typeof account.uuid === 'string') ??
    accounts.find((account) => typeof account.uuid === 'string') ??
    null;
  const rawUuid = typeof primaryAccount?.uuid === 'string' ? primaryAccount.uuid : null;
  const rawUsername =
    typeof primaryAccount?.username === 'string'
      ? primaryAccount.username
      : typeof profile.preferred_username === 'string'
        ? profile.preferred_username
        : typeof profile.name === 'string'
          ? profile.name
          : null;

  return {
    uuid: rawUuid ? canonicalizeMinecraftUuid(rawUuid) : null,
    username: rawUsername?.trim() ? rawUsername.trim() : null
  };
}

export function createBetterAuth(config: AppConfig, cosmeticsStore: CosmeticsFileStore): AppAuth {
  return betterAuth({
    secret: config.BETTER_AUTH_SECRET,
    baseURL: config.BETTER_AUTH_URL,
    basePath: config.BETTER_AUTH_BASE_PATH,
    trustedOrigins: [config.FRONTEND_ORIGIN, config.BETTER_AUTH_URL],
    advanced: {
      trustedProxyHeaders: config.TRUST_PROXY,
      useSecureCookies: config.SESSION_COOKIE_SECURE,
      defaultCookieAttributes: {
        sameSite: config.SESSION_COOKIE_SAME_SITE
      }
    },
    session: {
      expiresIn: config.SESSION_TTL_SECONDS,
      updateAge: Math.min(60 * 60, config.SESSION_TTL_SECONDS),
      cookieCache: {
        enabled: true,
        maxAge: 5 * 60,
        refreshCache: true,
        strategy: 'jwe'
      }
    },
    user: {
      additionalFields: {
        minecraftUuid: {
          type: 'string',
          required: false,
          input: false
        },
        minecraftUsername: {
          type: 'string',
          required: false,
          input: false
        }
      }
    },
    account: {
      accountLinking: {
        enabled: false
      },
      storeAccountCookie: true,
      storeStateStrategy: 'cookie',
      updateAccountOnSignIn: true
    },
    databaseHooks: {
      user: {
        create: {
          before: async (user) => {
            const minecraftUuid =
              typeof user.minecraftUuid === 'string' ? canonicalizeMinecraftUuid(user.minecraftUuid) : null;

            if (!minecraftUuid) {
              throw APIError.fromStatus(403, {
                code: 'MCID_ACCOUNT_REQUIRED',
                message: 'MC-ID account is missing a valid Minecraft UUID'
              });
            }

            const snapshot = await cosmeticsStore.getSnapshot();

            if (!snapshot.snapshot.players[minecraftUuid]) {
              throw APIError.fromStatus(403, {
                code: 'PLAYER_NOT_ALLOWED',
                message: 'This Minecraft account is not allowed to use this API'
              });
            }

            return {
              data: {
                minecraftUuid
              }
            };
          }
        }
      }
    },
    plugins: [
      genericOAuth({
        config: [
          {
            providerId: 'mc-id',
            clientId: config.MCID_CLIENT_ID,
            clientSecret: config.MCID_CLIENT_SECRET,
            discoveryUrl: config.MCID_DISCOVERY_URL,
            scopes: ['openid', 'profile', 'email', 'connections'],
            pkce: true,
            requireIssuerValidation: true,
            mapProfileToUser: async (profile) => {
              const mcidProfile = isRecord(profile) ? profile : {};
              const primaryAccount = extractPrimaryMcidAccount(mcidProfile);

              return {
                email:
                  typeof mcidProfile.email === 'string'
                    ? mcidProfile.email
                    : `${mcidProfile.sub ?? 'mcid-user'}@mc-id.local`,
                emailVerified: mcidProfile.email_verified === true,
                name: primaryAccount.username ?? (typeof mcidProfile.name === 'string' ? mcidProfile.name : 'Minecraft User'),
                minecraftUuid: primaryAccount.uuid,
                minecraftUsername: primaryAccount.username
              };
            }
          }
        ]
      })
    ]
  }) as AppAuth;
}
