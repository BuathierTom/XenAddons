import { z } from 'zod';

import type { AppConfig } from '../config.js';
import type { McidClient, McidRequestedCode, McidVerifiedIdentity } from './types.js';

const requestCodeResponseSchema = z
  .object({
    expiresAt: z.string().datetime().optional(),
    message: z.string().min(1),
    userId: z.string().regex(/^[0-9a-f]{32}$/i)
  })
  .passthrough();

const verifyCodeResponseSchema = z
  .object({
    userId: z.string().regex(/^[0-9a-f]{32}$/i),
    username: z.string().min(1)
  })
  .passthrough();

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
}

function parseJsonSafely(body: string): unknown {
  if (body.trim().length === 0) {
    return null;
  }

  try {
    return JSON.parse(body) as unknown;
  } catch {
    return body;
  }
}

async function readResponseBody(response: Response): Promise<unknown> {
  return parseJsonSafely(await response.text());
}

function extractErrorMessage(body: unknown, fallback: string): string {
  if (isRecord(body) && typeof body.error_description === 'string') {
    return body.error_description;
  }

  if (isRecord(body) && typeof body.message === 'string') {
    return body.message;
  }

  if (isRecord(body) && isRecord(body.data) && typeof body.data.reason === 'string') {
    return body.data.reason;
  }

  if (typeof body === 'string' && body.trim().length > 0) {
    return body;
  }

  return fallback;
}

export class McidApiError extends Error {
  public constructor(
    message: string,
    public readonly statusCode: number,
    public readonly details: unknown
  ) {
    super(message);
  }
}

export function canonicalizeMinecraftUuid(value: string): string | null {
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

export function toCompactMinecraftUuid(value: string): string | null {
  const canonicalUuid = canonicalizeMinecraftUuid(value);

  if (!canonicalUuid) {
    return null;
  }

  return canonicalUuid.replaceAll('-', '');
}

export function getMcidVerificationConfig() {
  return {
    codeLength: 6,
    codeTtlSeconds: 300,
    flow: 'verification_code',
    identifierField: 'identifier'
  } as const;
}

export class HttpMcidClient implements McidClient {
  public constructor(private readonly config: AppConfig) {}

  public async requestCode(input: { uuid: string }): Promise<McidRequestedCode> {
    const compactUuid = toCompactMinecraftUuid(input.uuid);

    if (!compactUuid) {
      throw new McidApiError('Invalid Minecraft UUID', 400, { uuid: input.uuid });
    }

    const response = await fetch(this.config.MCID_CODE_REQUEST_URL, {
      body: JSON.stringify({
        uuid: compactUuid
      }),
      headers: {
        'Content-Type': 'application/json',
        'X-API-Key': this.config.MCID_API_KEY
      },
      method: 'POST'
    });
    const responseBody = await readResponseBody(response);

    if (!response.ok) {
      throw new McidApiError(
        extractErrorMessage(responseBody, 'MC-ID code request failed'),
        response.status,
        responseBody
      );
    }

    const parsedBody = requestCodeResponseSchema.safeParse(responseBody);

    if (!parsedBody.success) {
      throw new McidApiError('MC-ID request-code response is invalid', 502, parsedBody.error.format());
    }

    return {
      expiresAt: parsedBody.data.expiresAt ?? null,
      message: parsedBody.data.message,
      userId: parsedBody.data.userId.toLowerCase()
    };
  }

  public async verifyCode(input: {
    code: string;
    uuid: string;
  }): Promise<McidVerifiedIdentity> {
    const compactUuid = toCompactMinecraftUuid(input.uuid);

    if (!compactUuid) {
      throw new McidApiError('Invalid Minecraft UUID', 400, { uuid: input.uuid });
    }

    const response = await fetch(this.config.MCID_CODE_VERIFY_URL, {
      body: JSON.stringify({
        code: input.code,
        uuid: compactUuid
      }),
      headers: {
        'Content-Type': 'application/json',
        'X-API-Key': this.config.MCID_API_KEY
      },
      method: 'POST'
    });
    const responseBody = await readResponseBody(response);

    if (!response.ok) {
      throw new McidApiError(
        extractErrorMessage(responseBody, 'MC-ID code verification failed'),
        response.status,
        responseBody
      );
    }

    const parsedBody = verifyCodeResponseSchema.safeParse(responseBody);

    if (!parsedBody.success) {
      throw new McidApiError('MC-ID verify-code response is invalid', 502, parsedBody.error.format());
    }

    return {
      userId: parsedBody.data.userId.toLowerCase(),
      username: parsedBody.data.username
    };
  }
}
