import type {
  FastifyPluginAsync,
  FastifyReply,
  FastifyRequest
} from 'fastify';
import { ZodError } from 'zod';

import { buildEtag, ifNoneMatchIncludes } from '../lib/etag.js';
import type { CosmeticsFileStore } from '../lib/file-store.js';
import { usernameUpdateBodySchema, uuidSchema } from '../domain/schemas.js';

const CACHE_CONTROL_HEADER = 'public, max-age=60, stale-while-revalidate=300';
const NO_STORE_HEADER = 'no-store';

interface CosmeticsRoutesOptions {
  cosmeticsStore: CosmeticsFileStore;
  apiKeyGuard: (request: FastifyRequest, reply: FastifyReply) => Promise<void>;
}

function applyCacheHeaders(reply: FastifyReply, etag: string): void {
  reply.header('Cache-Control', CACHE_CONTROL_HEADER);
  reply.header('ETag', etag);
}

function replyIfNotModified(request: FastifyRequest, reply: FastifyReply, etag: string): boolean {
  const ifNoneMatch = request.headers['if-none-match'];

  if (!ifNoneMatch || !ifNoneMatchIncludes(ifNoneMatch, etag)) {
    return false;
  }

  applyCacheHeaders(reply, etag);
  reply.code(304).send();
  return true;
}

function formatZodError(error: ZodError): string {
  return error.issues
    .map((issue) => {
      const path = issue.path.length > 0 ? issue.path.join('.') : '<root>';
      return `${path}: ${issue.message}`;
    })
    .join('; ');
}

export const cosmeticsRoutes: FastifyPluginAsync<CosmeticsRoutesOptions> = async (app, options) => {
  app.addHook('onRequest', options.apiKeyGuard);

  app.get('/', async (request, reply) => {
    const { etag, snapshot } = await options.cosmeticsStore.getSnapshot();

    if (replyIfNotModified(request, reply, etag)) {
      return;
    }

    applyCacheHeaders(reply, etag);
    return snapshot;
  });

  app.get('/:uuid', async (request, reply) => {
    const paramsResult = uuidSchema.safeParse((request.params as { uuid?: string }).uuid);

    if (!paramsResult.success) {
      reply.code(400);
      return {
        error: 'Invalid UUID'
      };
    }

    const uuid = paramsResult.data;
    const { snapshot } = await options.cosmeticsStore.getSnapshot();
    const player = snapshot.players[uuid];

    if (!player) {
      reply.code(404);
      return {
        error: 'Player not found'
      };
    }

    const responseBody = {
      version: snapshot.version,
      updatedAt: snapshot.updatedAt,
      player: {
        uuid,
        ...player
      }
    };
    const etag = buildEtag(responseBody);

    if (replyIfNotModified(request, reply, etag)) {
      return;
    }

    applyCacheHeaders(reply, etag);
    return responseBody;
  });

  app.patch('/:uuid/username', async (request, reply) => {
    const paramsResult = uuidSchema.safeParse((request.params as { uuid?: string }).uuid);

    if (!paramsResult.success) {
      reply.code(400);
      return {
        error: 'Invalid UUID'
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
        paramsResult.data,
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
          uuid: paramsResult.data,
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
