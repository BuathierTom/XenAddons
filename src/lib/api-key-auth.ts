import { timingSafeEqual } from 'node:crypto';

import type { FastifyReply, FastifyRequest } from 'fastify';

function constantTimeEquals(left: string, right: string): boolean {
  const leftBuffer = Buffer.from(left);
  const rightBuffer = Buffer.from(right);

  if (leftBuffer.length !== rightBuffer.length) {
    return false;
  }

  return timingSafeEqual(leftBuffer, rightBuffer);
}

function readApiKeyHeader(request: FastifyRequest): string | null {
  const headerValue = request.headers['x-api-key'];

  if (typeof headerValue === 'string') {
    return headerValue;
  }

  if (Array.isArray(headerValue) && headerValue.length > 0) {
    return headerValue[0] ?? null;
  }

  return null;
}

export function createApiKeyGuard(expectedApiKey: string) {
  return async (request: FastifyRequest, reply: FastifyReply): Promise<void> => {
    const providedApiKey = readApiKeyHeader(request);

    if (!providedApiKey || !constantTimeEquals(providedApiKey, expectedApiKey)) {
      reply.code(401).send({
        error: 'Unauthorized'
      });
    }
  };
}
