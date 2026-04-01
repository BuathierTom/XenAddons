import Fastify, { type FastifyBaseLogger, type FastifyInstance } from 'fastify';

import { createBetterAuth } from './auth/mc-id.js';
import type { AppAuth } from './auth/types.js';
import type { AppConfig } from './config.js';
import { createApiKeyGuard } from './lib/api-key-auth.js';
import { registerCors } from './lib/cors.js';
import { CosmeticsFileStore } from './lib/file-store.js';
import { authRoutes } from './routes/auth.js';
import { cosmeticsRoutes } from './routes/cosmetics.js';
import { healthRoutes } from './routes/health.js';
import { meRoutes } from './routes/me.js';

export interface CreateAppOptions {
  auth?: AppAuth;
  config: AppConfig;
  logger?: FastifyBaseLogger | boolean;
}

export async function createApp(options: CreateAppOptions): Promise<FastifyInstance> {
  const app = Fastify({
    logger: options.logger ?? true,
    trustProxy: options.config.TRUST_PROXY
  });
  registerCors(app, options.config);

  const cosmeticsStore = new CosmeticsFileStore(options.config.COSMETICS_FILE, app.log);
  await cosmeticsStore.initialize();
  const auth = options.auth ?? createBetterAuth(options.config, cosmeticsStore);

  await app.register(healthRoutes);
  await app.register(authRoutes, {
    auth,
    basePath: options.config.BETTER_AUTH_BASE_PATH
  });
  await app.register(meRoutes, {
    auth,
    config: options.config,
    cosmeticsStore
  });
  await app.register(cosmeticsRoutes, {
    prefix: '/v1/cosmetics',
    cosmeticsStore,
    apiKeyGuard: createApiKeyGuard(options.config.API_KEY)
  });

  return app;
}
