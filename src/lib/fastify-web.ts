import type { FastifyReply, FastifyRequest } from 'fastify';

type RequestBody = string | ArrayBuffer | ArrayBufferView;

function toHeadersInit(headersObject: FastifyRequest['headers']): Headers {
  const headers = new Headers();

  for (const [name, value] of Object.entries(headersObject)) {
    if (value === undefined) {
      continue;
    }

    if (Array.isArray(value)) {
      for (const entry of value) {
        headers.append(name, entry);
      }

      continue;
    }

    headers.append(name, value);
  }

  return headers;
}

function getRequestOrigin(request: FastifyRequest): string {
  const protocol = request.protocol;
  const hostHeader = request.headers['x-forwarded-host'] ?? request.headers.host;
  const host = Array.isArray(hostHeader) ? hostHeader[0] : hostHeader;

  if (!host) {
    throw new Error('Missing Host header');
  }

  return `${protocol}://${host}`;
}

function getRequestBody(request: FastifyRequest): RequestBody | undefined {
  if (request.method === 'GET' || request.method === 'HEAD') {
    return undefined;
  }

  if (request.body === undefined || request.body === null) {
    return undefined;
  }

  if (
    typeof request.body === 'string' ||
    request.body instanceof ArrayBuffer ||
    ArrayBuffer.isView(request.body)
  ) {
    return request.body as RequestBody;
  }

  return JSON.stringify(request.body);
}

export function toWebRequest(request: FastifyRequest): Request {
  const requestInit: RequestInit = {
    method: request.method,
    headers: toHeadersInit(request.headers)
  };
  const body = getRequestBody(request);

  if (body !== undefined) {
    requestInit.body = body as NonNullable<RequestInit['body']>;
  }

  return new Request(new URL(request.raw.url ?? request.url, getRequestOrigin(request)), requestInit);
}

export async function sendWebResponse(reply: FastifyReply, response: Response): Promise<void> {
  for (const [name, value] of response.headers.entries()) {
    if (name.toLowerCase() === 'set-cookie') {
      continue;
    }

    reply.header(name, value);
  }

  const setCookieHeaders = response.headers.getSetCookie();

  if (setCookieHeaders.length > 0) {
    reply.header('set-cookie', setCookieHeaders);
  }

  reply.code(response.status);

  if (response.body === null) {
    reply.send();
    return;
  }

  const body = Buffer.from(await response.arrayBuffer());

  reply.send(body);
}
