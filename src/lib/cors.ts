import type { FastifyInstance, FastifyReply, FastifyRequest } from 'fastify';

import type { AppConfig } from '../config.js';
import { appendVaryHeader } from './cookies.js';

const ALLOW_METHODS = 'GET,POST,PATCH,OPTIONS';
const ALLOW_HEADERS = 'Content-Type, X-CSRF-Token, X-API-Key';
const EXPOSE_HEADERS = 'ETag';

function getOriginHeader(request: FastifyRequest): string | null {
  const origin = request.headers.origin;
  return typeof origin === 'string' ? origin : null;
}

function isAllowedOrigin(origin: string | null, config: AppConfig): boolean {
  return origin === config.FRONTEND_ORIGIN;
}

function applyCorsHeaders(reply: FastifyReply, origin: string): void {
  reply.header('Access-Control-Allow-Origin', origin);
  reply.header('Access-Control-Allow-Credentials', 'true');
  reply.header('Access-Control-Expose-Headers', EXPOSE_HEADERS);
  appendVaryHeader(reply, 'Origin');
}

export function registerCors(app: FastifyInstance, config: AppConfig): void {
  app.addHook('onRequest', async (request, reply) => {
    if (request.method !== 'OPTIONS') {
      return;
    }

    const origin = getOriginHeader(request);

    if (origin === null || !isAllowedOrigin(origin, config)) {
      reply.code(403).send({
        error: 'Origin not allowed'
      });
      return;
    }

    applyCorsHeaders(reply, origin);
    reply.header('Access-Control-Allow-Methods', ALLOW_METHODS);
    reply.header('Access-Control-Allow-Headers', ALLOW_HEADERS);
    reply.header('Access-Control-Max-Age', '600');
    reply.code(204).send();
  });

  app.addHook('onSend', async (request, reply, payload) => {
    const origin = getOriginHeader(request);

    if (origin !== null && isAllowedOrigin(origin, config)) {
      applyCorsHeaders(reply, origin);
    }

    return payload;
  });
}
