import type { FastifyPluginAsync, FastifyReply, FastifyRequest } from 'fastify';

import type { AppAuth } from '../auth/types.js';
import { sendWebResponse, toWebRequest } from '../lib/fastify-web.js';

interface AuthHandlerRoutesOptions {
  auth: AppAuth;
  basePath: string;
}

export const authRoutes: FastifyPluginAsync<AuthHandlerRoutesOptions> = async (app, options) => {
  const basePath = options.basePath.startsWith('/') ? options.basePath : `/${options.basePath}`;

  const handler = async (request: FastifyRequest, reply: FastifyReply): Promise<void> => {
    const webRequest = toWebRequest(request);
    const webResponse = await options.auth.handler(webRequest);

    await sendWebResponse(reply, webResponse);
  };

  app.route({
    method: ['GET', 'POST', 'PATCH', 'PUT', 'DELETE'],
    url: `${basePath}/*`,
    handler
  });

  app.route({
    method: ['GET', 'POST', 'PATCH', 'PUT', 'DELETE'],
    url: basePath,
    handler
  });
};
