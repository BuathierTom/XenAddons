import type { FastifyPluginAsync } from 'fastify';
import { ZodError } from 'zod';

import {
  canonicalizeMinecraftUuid,
  getMcidVerificationConfig,
  McidApiError
} from '../auth/mc-id.js';
import {
  createSessionCookie,
  destroyRequestSession,
  isTrustedFrontendOrigin
} from '../auth/session.js';
import type { McidClient, SessionStore } from '../auth/types.js';
import type { AppConfig } from '../config.js';
import type { NormalizedPlayer, NormalizedSnapshot } from '../domain/schemas.js';
import {
  mcidCodeRequestBodySchema,
  mcidCodeVerifyBodySchema
} from '../domain/schemas.js';
import { buildEtag } from '../lib/etag.js';
import type { CosmeticsFileStore } from '../lib/file-store.js';

const NO_STORE_HEADER = 'no-store';

interface AuthRoutesOptions {
  config: AppConfig;
  cosmeticsStore: CosmeticsFileStore;
  mcidClient: McidClient;
  sessionStore: SessionStore;
}

interface ResolvedAllowedPlayer {
  player: NormalizedPlayer;
  uuid: string;
}

function formatZodError(error: ZodError): string {
  return error.issues
    .map((issue) => {
      const path = issue.path.length > 0 ? issue.path.join('.') : '<root>';
      return `${path}: ${issue.message}`;
    })
    .join('; ');
}

function normalizeLookup(value: string): string {
  return value.trim().toLowerCase();
}

function resolveAllowedPlayer(
  snapshot: NormalizedSnapshot,
  identifier: string
): ResolvedAllowedPlayer | null {
  const normalizedIdentifier = identifier.trim();
  const canonicalUuid = canonicalizeMinecraftUuid(normalizedIdentifier);

  if (canonicalUuid) {
    const player = snapshot.players[canonicalUuid];

    if (player) {
      return {
        player,
        uuid: canonicalUuid
      };
    }
  }

  const lookup = normalizeLookup(normalizedIdentifier);

  for (const [uuid, player] of Object.entries(snapshot.players)) {
    if (player.username !== null && normalizeLookup(player.username) === lookup) {
      return {
        player,
        uuid
      };
    }
  }

  for (const [uuid, player] of Object.entries(snapshot.players)) {
    if (player.aliases.some((alias) => normalizeLookup(alias) === lookup)) {
      return {
        player,
        uuid
      };
    }
  }

  return null;
}

function mapUpstreamStatus(statusCode: number): number {
  if (statusCode >= 500) {
    return 502;
  }

  return statusCode;
}

export const authRoutes: FastifyPluginAsync<AuthRoutesOptions> = async (app, options) => {
  app.get('/auth/mc-id/config', async (_request, reply) => {
    const responseBody = getMcidVerificationConfig();

    reply.header('Cache-Control', NO_STORE_HEADER);
    reply.header('ETag', buildEtag(responseBody));
    return responseBody;
  });

  app.post('/auth/mc-id/request-code', async (request, reply) => {
    if (!isTrustedFrontendOrigin(request, options.config)) {
      reply.code(403);
      return {
        error: 'Origin not allowed'
      };
    }

    const bodyResult = mcidCodeRequestBodySchema.safeParse(request.body);

    if (!bodyResult.success) {
      reply.code(400);
      return {
        error: 'Invalid request body',
        details: formatZodError(bodyResult.error)
      };
    }

    const snapshot = await options.cosmeticsStore.getSnapshot();
    const resolvedPlayer = resolveAllowedPlayer(snapshot.snapshot, bodyResult.data.identifier);

    if (!resolvedPlayer) {
      reply.code(404);
      return {
        error: 'Player not found'
      };
    }

    try {
      const requestedCode = await options.mcidClient.requestCode({
        uuid: resolvedPlayer.uuid
      });
      const requestedUuid = canonicalizeMinecraftUuid(requestedCode.userId);

      if (requestedUuid !== resolvedPlayer.uuid) {
        reply.code(502);
        return {
          error: 'MC-ID returned a mismatched player UUID'
        };
      }

      const responseBody = {
        expiresAt: requestedCode.expiresAt,
        message: requestedCode.message,
        player: {
          uuid: resolvedPlayer.uuid,
          ...resolvedPlayer.player
        },
        requested: true
      };

      reply.header('Cache-Control', NO_STORE_HEADER);
      reply.header('ETag', buildEtag(responseBody));
      return responseBody;
    } catch (error) {
      if (error instanceof McidApiError) {
        reply.code(mapUpstreamStatus(error.statusCode));
        return {
          error: error.message
        };
      }

      app.log.error({ err: error }, 'Unexpected MC-ID request-code failure');
      reply.code(500);
      return {
        error: 'Unexpected authentication failure'
      };
    }
  });

  app.post('/auth/mc-id/verify-code', async (request, reply) => {
    if (!isTrustedFrontendOrigin(request, options.config)) {
      reply.code(403);
      return {
        error: 'Origin not allowed'
      };
    }

    const bodyResult = mcidCodeVerifyBodySchema.safeParse(request.body);

    if (!bodyResult.success) {
      reply.code(400);
      return {
        error: 'Invalid request body',
        details: formatZodError(bodyResult.error)
      };
    }

    const canonicalUuid = canonicalizeMinecraftUuid(bodyResult.data.uuid);

    if (!canonicalUuid) {
      reply.code(400);
      return {
        error: 'Invalid Minecraft UUID'
      };
    }

    const snapshot = await options.cosmeticsStore.getSnapshot();
    const player = snapshot.snapshot.players[canonicalUuid];

    if (!player) {
      reply.code(404);
      return {
        error: 'Player not found'
      };
    }

    try {
      const verifiedIdentity = await options.mcidClient.verifyCode({
        code: bodyResult.data.code,
        uuid: canonicalUuid
      });
      const verifiedUuid = canonicalizeMinecraftUuid(verifiedIdentity.userId);

      if (verifiedUuid !== canonicalUuid) {
        reply.code(502);
        return {
          error: 'MC-ID returned a mismatched player UUID'
        };
      }

      const session = options.sessionStore.create({
        email: null,
        emailVerified: null,
        id: `mcid:${canonicalUuid}`,
        minecraftUsername: verifiedIdentity.username,
        minecraftUuid: canonicalUuid,
        name: verifiedIdentity.username
      });

      createSessionCookie(reply, options.config, session.id);

      const responseBody = {
        authenticated: true,
        authorized: true,
        authUser: session.user,
        player: {
          uuid: canonicalUuid,
          ...player
        }
      };

      reply.header('Cache-Control', NO_STORE_HEADER);
      reply.header('ETag', buildEtag(responseBody));
      return responseBody;
    } catch (error) {
      if (error instanceof McidApiError) {
        reply.code(mapUpstreamStatus(error.statusCode));
        return {
          error: error.message
        };
      }

      app.log.error({ err: error }, 'Unexpected MC-ID verify-code failure');
      reply.code(500);
      return {
        error: 'Unexpected authentication failure'
      };
    }
  });

  app.post('/auth/logout', async (request, reply) => {
    if (!isTrustedFrontendOrigin(request, options.config)) {
      reply.code(403);
      return {
        error: 'Origin not allowed'
      };
    }

    destroyRequestSession(request, reply, options.sessionStore, options.config);
    reply.header('Cache-Control', NO_STORE_HEADER);
    reply.code(204).send();
  });
};
