import type { FastifyPluginAsync } from 'fastify';
import { ZodError } from 'zod';

import {
  getRequestSession,
  getSessionMinecraftUsername,
  getSessionMinecraftUuid,
  isTrustedFrontendOrigin
} from '../auth/session.js';
import type { AppAuth } from '../auth/types.js';
import type { AppConfig } from '../config.js';
import { usernameUpdateBodySchema } from '../domain/schemas.js';
import type { CosmeticsFileStore } from '../lib/file-store.js';
import { buildEtag } from '../lib/etag.js';

const NO_STORE_HEADER = 'no-store';

interface MeRoutesOptions {
  auth: AppAuth;
  config: AppConfig;
  cosmeticsStore: CosmeticsFileStore;
}

function formatZodError(error: ZodError): string {
  return error.issues
    .map((issue) => {
      const path = issue.path.length > 0 ? issue.path.join('.') : '<root>';
      return `${path}: ${issue.message}`;
    })
    .join('; ');
}

export const meRoutes: FastifyPluginAsync<MeRoutesOptions> = async (app, options) => {
  app.get('/me', async (request, reply) => {
    const session = await getRequestSession(options.auth, request);

    if (!session) {
      reply.code(401);
      reply.header('Cache-Control', NO_STORE_HEADER);
      return {
        authenticated: false
      };
    }

    const minecraftUuid = getSessionMinecraftUuid(session);

    if (!minecraftUuid) {
      reply.code(403);
      reply.header('Cache-Control', NO_STORE_HEADER);
      return {
        authenticated: true,
        authorized: false,
        error: 'MC-ID account is missing a valid Minecraft UUID'
      };
    }

    const snapshot = await options.cosmeticsStore.getSnapshot();
    const player = snapshot.snapshot.players[minecraftUuid];

    if (!player) {
      reply.code(403);
      reply.header('Cache-Control', NO_STORE_HEADER);
      return {
        authenticated: true,
        authorized: false,
        error: 'Player not allowed'
      };
    }

    const responseBody = {
      authenticated: true,
      authorized: true,
      authUser: {
        email: session.user.email,
        emailVerified: session.user.emailVerified,
        id: session.user.id,
        minecraftUsername: getSessionMinecraftUsername(session),
        minecraftUuid,
        name: session.user.name
      },
      player: {
        uuid: minecraftUuid,
        ...player
      }
    };

    reply.header('Cache-Control', NO_STORE_HEADER);
    reply.header('ETag', buildEtag(responseBody));
    return responseBody;
  });

  app.patch('/me/username', async (request, reply) => {
    if (!isTrustedFrontendOrigin(request, options.config)) {
      reply.code(403);
      return {
        error: 'Origin not allowed'
      };
    }

    const session = await getRequestSession(options.auth, request);

    if (!session) {
      reply.code(401);
      return {
        error: 'Unauthorized'
      };
    }

    const minecraftUuid = getSessionMinecraftUuid(session);

    if (!minecraftUuid) {
      reply.code(403);
      return {
        error: 'MC-ID account is missing a valid Minecraft UUID'
      };
    }

    const snapshot = await options.cosmeticsStore.getSnapshot();

    if (!snapshot.snapshot.players[minecraftUuid]) {
      reply.code(403);
      return {
        error: 'Player not allowed'
      };
    }

    const bodyResult = usernameUpdateBodySchema.safeParse(request.body);

    if (!bodyResult.success) {
      reply.code(400);
      return {
        error: 'Invalid request body',
        details: formatZodError(bodyResult.error)
      };
    }

    try {
      const updatedSnapshot = await options.cosmeticsStore.updateUsername(
        minecraftUuid,
        bodyResult.data.username
      );

      if (!updatedSnapshot) {
        reply.code(404);
        return {
          error: 'Player not found'
        };
      }

      const responseBody = {
        version: updatedSnapshot.snapshot.version,
        updatedAt: updatedSnapshot.snapshot.updatedAt,
        player: {
          uuid: minecraftUuid,
          ...updatedSnapshot.player
        }
      };

      reply.header('Cache-Control', NO_STORE_HEADER);
      reply.header('ETag', buildEtag(responseBody));
      return responseBody;
    } catch (error) {
      reply.code(409);
      return {
        error: error instanceof Error ? error.message : 'Unable to update username'
      };
    }
  });
};
