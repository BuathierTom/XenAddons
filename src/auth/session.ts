import { randomUUID } from 'node:crypto';

import type { FastifyReply, FastifyRequest } from 'fastify';

import type { AppConfig } from '../config.js';
import {
  clearCookie,
  getCookie,
  setCookie,
  signCookieValue,
  unsignCookieValue
} from '../lib/cookies.js';
import type { AppSession, SessionStore, SessionUser } from './types.js';

const SESSION_COOKIE_NAME = 'xenaddons_session';

interface StoredSession {
  createdAtMs: number;
  expiresAtMs: number;
  id: string;
  user: SessionUser;
}

function toPublicSession(session: StoredSession): AppSession {
  return {
    createdAt: new Date(session.createdAtMs).toISOString(),
    expiresAt: new Date(session.expiresAtMs).toISOString(),
    id: session.id,
    user: session.user
  };
}

function getSessionCookieOptions(config: AppConfig) {
  return {
    httpOnly: true,
    maxAge: config.SESSION_TTL_SECONDS,
    path: '/',
    sameSite: config.SESSION_COOKIE_SAME_SITE,
    secure: config.SESSION_COOKIE_SECURE
  } as const;
}

export class InMemorySessionStore implements SessionStore {
  private readonly sessions = new Map<string, StoredSession>();

  public constructor(private readonly ttlSeconds: number) {}

  public create(user: SessionUser): AppSession {
    this.deleteExpiredSessions();

    const now = Date.now();
    const session: StoredSession = {
      createdAtMs: now,
      expiresAtMs: now + this.ttlSeconds * 1000,
      id: randomUUID(),
      user
    };

    this.sessions.set(session.id, session);
    return toPublicSession(session);
  }

  public delete(sessionId: string): void {
    this.sessions.delete(sessionId);
  }

  public get(sessionId: string): AppSession | null {
    const session = this.sessions.get(sessionId);

    if (!session) {
      return null;
    }

    if (session.expiresAtMs <= Date.now()) {
      this.sessions.delete(sessionId);
      return null;
    }

    return toPublicSession(session);
  }

  private deleteExpiredSessions(): void {
    const now = Date.now();

    for (const [sessionId, session] of this.sessions) {
      if (session.expiresAtMs <= now) {
        this.sessions.delete(sessionId);
      }
    }
  }
}

export function createSessionCookie(
  reply: FastifyReply,
  config: AppConfig,
  sessionId: string
): void {
  setCookie(
    reply,
    SESSION_COOKIE_NAME,
    signCookieValue(sessionId, config.SESSION_SECRET),
    getSessionCookieOptions(config)
  );
}

export function clearSessionCookie(reply: FastifyReply, config: AppConfig): void {
  clearCookie(reply, SESSION_COOKIE_NAME, getSessionCookieOptions(config));
}

export function getRequestSession(
  request: FastifyRequest,
  sessionStore: SessionStore,
  config: AppConfig
): AppSession | null {
  const signedValue = getCookie(request, SESSION_COOKIE_NAME);

  if (!signedValue) {
    return null;
  }

  const sessionId = unsignCookieValue(signedValue, config.SESSION_SECRET);

  if (!sessionId) {
    return null;
  }

  return sessionStore.get(sessionId);
}

export function destroyRequestSession(
  request: FastifyRequest,
  reply: FastifyReply,
  sessionStore: SessionStore,
  config: AppConfig
): void {
  const signedValue = getCookie(request, SESSION_COOKIE_NAME);

  if (signedValue) {
    const sessionId = unsignCookieValue(signedValue, config.SESSION_SECRET);

    if (sessionId) {
      sessionStore.delete(sessionId);
    }
  }

  clearSessionCookie(reply, config);
}

export function isTrustedFrontendOrigin(request: FastifyRequest, config: AppConfig): boolean {
  const origin = request.headers.origin;

  return typeof origin === 'string' && origin === config.FRONTEND_ORIGIN;
}
