import type { FastifyRequest } from 'fastify';

import type { AppConfig } from '../config.js';
import type { AppAuth, AppAuthSession } from './types.js';

function appendHeader(headers: Headers, name: string, value: string | string[]): void {
  if (Array.isArray(value)) {
    for (const entry of value) {
      headers.append(name, entry);
    }

    return;
  }

  headers.append(name, value);
}

export function toWebHeaders(headersObject: FastifyRequest['headers']): Headers {
  const headers = new Headers();

  for (const [name, value] of Object.entries(headersObject)) {
    if (value === undefined) {
      continue;
    }

    appendHeader(headers, name, value);
  }

  return headers;
}

export async function getRequestSession(
  auth: AppAuth,
  request: FastifyRequest
): Promise<AppAuthSession | null> {
  return auth.api.getSession({
    headers: toWebHeaders(request.headers)
  });
}

export function isTrustedFrontendOrigin(request: FastifyRequest, config: AppConfig): boolean {
  const origin = request.headers.origin;

  return typeof origin === 'string' && origin === config.FRONTEND_ORIGIN;
}

export function getSessionMinecraftUuid(session: AppAuthSession): string | null {
  const value = session.user.minecraftUuid;
  return typeof value === 'string' && value.length > 0 ? value : null;
}

export function getSessionMinecraftUsername(session: AppAuthSession): string | null {
  const value = session.user.minecraftUsername;
  return typeof value === 'string' && value.length > 0 ? value : null;
}
