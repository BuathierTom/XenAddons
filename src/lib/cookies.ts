import { createHmac, timingSafeEqual } from 'node:crypto';

import type { FastifyReply, FastifyRequest } from 'fastify';

export type SameSite = 'lax' | 'strict' | 'none';

export interface CookieOptions {
  domain?: string;
  expires?: Date;
  httpOnly?: boolean;
  maxAge?: number;
  path?: string;
  sameSite?: SameSite;
  secure?: boolean;
}

function normalizeSameSite(value: SameSite): string {
  if (value === 'none') {
    return 'None';
  }

  if (value === 'strict') {
    return 'Strict';
  }

  return 'Lax';
}

function mergeSetCookieHeader(reply: FastifyReply, cookieValue: string): void {
  const existingHeader = reply.getHeader('Set-Cookie');

  if (existingHeader === undefined) {
    reply.header('Set-Cookie', cookieValue);
    return;
  }

  if (Array.isArray(existingHeader)) {
    reply.header('Set-Cookie', [...existingHeader.map(String), cookieValue]);
    return;
  }

  reply.header('Set-Cookie', [String(existingHeader), cookieValue]);
}

export function appendVaryHeader(reply: FastifyReply, value: string): void {
  const currentVary = reply.getHeader('Vary');

  if (currentVary === undefined) {
    reply.header('Vary', value);
    return;
  }

  const values = new Set(
    String(currentVary)
      .split(',')
      .map((entry) => entry.trim())
      .filter((entry) => entry.length > 0)
  );
  values.add(value);
  reply.header('Vary', [...values].join(', '));
}

export function serializeCookie(name: string, value: string, options: CookieOptions = {}): string {
  const parts = [`${name}=${encodeURIComponent(value)}`];

  if (options.maxAge !== undefined) {
    parts.push(`Max-Age=${Math.max(0, Math.floor(options.maxAge))}`);
  }

  if (options.domain) {
    parts.push(`Domain=${options.domain}`);
  }

  parts.push(`Path=${options.path ?? '/'}`);

  if (options.expires) {
    parts.push(`Expires=${options.expires.toUTCString()}`);
  }

  if (options.httpOnly) {
    parts.push('HttpOnly');
  }

  if (options.sameSite) {
    parts.push(`SameSite=${normalizeSameSite(options.sameSite)}`);
  }

  if (options.secure) {
    parts.push('Secure');
  }

  return parts.join('; ');
}

export function setCookie(reply: FastifyReply, name: string, value: string, options: CookieOptions = {}): void {
  mergeSetCookieHeader(reply, serializeCookie(name, value, options));
}

export function clearCookie(reply: FastifyReply, name: string, options: CookieOptions = {}): void {
  mergeSetCookieHeader(
    reply,
    serializeCookie(name, '', {
      ...options,
      expires: new Date(0),
      maxAge: 0
    })
  );
}

export function parseCookies(cookieHeader: string | undefined): Record<string, string> {
  if (!cookieHeader) {
    return {};
  }

  return cookieHeader.split(';').reduce<Record<string, string>>((accumulator, part) => {
    const separatorIndex = part.indexOf('=');

    if (separatorIndex <= 0) {
      return accumulator;
    }

    const name = part.slice(0, separatorIndex).trim();
    const value = part.slice(separatorIndex + 1).trim();

    if (name.length === 0) {
      return accumulator;
    }

    try {
      accumulator[name] = decodeURIComponent(value);
    } catch {
      accumulator[name] = value;
    }

    return accumulator;
  }, {});
}

export function getCookie(request: FastifyRequest, name: string): string | null {
  const cookies = parseCookies(request.headers.cookie);
  return cookies[name] ?? null;
}

function createCookieSignature(value: string, secret: string): string {
  return createHmac('sha256', secret).update(value).digest('base64url');
}

export function signCookieValue(value: string, secret: string): string {
  return `${value}.${createCookieSignature(value, secret)}`;
}

export function unsignCookieValue(value: string, secret: string): string | null {
  const separatorIndex = value.lastIndexOf('.');

  if (separatorIndex <= 0) {
    return null;
  }

  const unsignedValue = value.slice(0, separatorIndex);
  const signature = value.slice(separatorIndex + 1);
  const expectedSignature = createCookieSignature(unsignedValue, secret);
  const signatureBuffer = Buffer.from(signature);
  const expectedBuffer = Buffer.from(expectedSignature);

  if (signatureBuffer.length !== expectedBuffer.length) {
    return null;
  }

  if (!timingSafeEqual(signatureBuffer, expectedBuffer)) {
    return null;
  }

  return unsignedValue;
}
