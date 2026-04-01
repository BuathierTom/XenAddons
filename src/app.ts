import Fastify, { type FastifyBaseLogger, type FastifyInstance } from 'fastify';

import { HttpMcidClient } from './auth/mc-id.js';
import { InMemorySessionStore } from './auth/session.js';
import type { McidClient, SessionStore } from './auth/types.js';
import type { AppConfig } from './config.js';
import { createApiKeyGuard } from './lib/api-key-auth.js';
import { registerCors } from './lib/cors.js';
import { CosmeticsFileStore } from './lib/file-store.js';
import { authRoutes } from './routes/auth.js';
import { cosmeticsRoutes } from './routes/cosmetics.js';
import { healthRoutes } from './routes/health.js';
import { meRoutes } from './routes/me.js';

export interface CreateAppOptions {
  config: AppConfig;
  logger?: FastifyBaseLogger | boolean;
  mcidClient?: McidClient;
  sessionStore?: SessionStore;
}

export async function createApp(options: CreateAppOptions): Promise<FastifyInstance> {
  const app = Fastify({
    logger: options.logger ?? true,
    trustProxy: options.config.TRUST_PROXY
  });
  registerCors(app, options.config);

  const cosmeticsStore = new CosmeticsFileStore(options.config.COSMETICS_FILE, app.log);
  await cosmeticsStore.initialize();
  const mcidClient = options.mcidClient ?? new HttpMcidClient(options.config);
  const sessionStore =
    options.sessionStore ?? new InMemorySessionStore(options.config.SESSION_TTL_SECONDS);

  await app.register(healthRoutes);
  await app.register(authRoutes, {
    config: options.config,
    cosmeticsStore,
    mcidClient,
    sessionStore
  });
  await app.register(meRoutes, {
    config: options.config,
    cosmeticsStore,
    sessionStore
  });
  await app.register(cosmeticsRoutes, {
    prefix: '/v1/cosmetics',
    cosmeticsStore,
    apiKeyGuard: createApiKeyGuard(options.config.API_KEY)
  });

  return app;
}
