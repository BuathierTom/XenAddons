import type { FastifyPluginAsync } from 'fastify';
import { ZodError } from 'zod';

import {
  getRequestSession,
  isTrustedFrontendOrigin
} from '../auth/session.js';
import type { SessionStore } from '../auth/types.js';
import type { AppConfig } from '../config.js';
import { cosmeticsWritableUpdateBodySchema } from '../domain/schemas.js';
import type { CosmeticsFileStore } from '../lib/file-store.js';
import { buildEtag } from '../lib/etag.js';

const NO_STORE_HEADER = 'no-store';

interface MeRoutesOptions {
  config: AppConfig;
  cosmeticsStore: CosmeticsFileStore;
  sessionStore: SessionStore;
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
    const session = getRequestSession(request, options.sessionStore, options.config);

    if (!session) {
      reply.code(401);
      reply.header('Cache-Control', NO_STORE_HEADER);
      return {
        authenticated: false
      };
    }

    const snapshot = await options.cosmeticsStore.getSnapshot();
    const player = snapshot.snapshot.players[session.user.minecraftUuid];

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
      authUser: session.user,
      player: {
        uuid: session.user.minecraftUuid,
        ...player
      }
    };

    reply.header('Cache-Control', NO_STORE_HEADER);
    reply.header('ETag', buildEtag(responseBody));
    return responseBody;
  });

  app.patch('/me/cosmetics', async (request, reply) => {
    if (!isTrustedFrontendOrigin(request, options.config)) {
      reply.code(403);
      return {
        error: 'Origin not allowed'
      };
    }

    const session = getRequestSession(request, options.sessionStore, options.config);

    if (!session) {
      reply.code(401);
      return {
        error: 'Unauthorized'
      };
    }

    const snapshot = await options.cosmeticsStore.getSnapshot();

    if (!snapshot.snapshot.players[session.user.minecraftUuid]) {
      reply.code(403);
      return {
        error: 'Player not allowed'
      };
    }

    const bodyResult = cosmeticsWritableUpdateBodySchema.safeParse(request.body);

    if (!bodyResult.success) {
      reply.code(400);
      return {
        error: 'Invalid request body',
        details: formatZodError(bodyResult.error)
      };
    }

    try {
      const updatedSnapshot = await options.cosmeticsStore.updateWritableFields(
        session.user.minecraftUuid,
        bodyResult.data
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
          uuid: session.user.minecraftUuid,
          ...updatedSnapshot.player
        }
      };

      reply.header('Cache-Control', NO_STORE_HEADER);
      reply.header('ETag', buildEtag(responseBody));
      return responseBody;
    } catch (error) {
      reply.code(409);
      return {
        error: error instanceof Error ? error.message : 'Unable to update cosmetics'
      };
    }
  });
};
