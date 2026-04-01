import Fastify, { type FastifyBaseLogger, type FastifyInstance } from 'fastify';

import type { AppConfig } from './config.js';
import { createApiKeyGuard } from './lib/api-key-auth.js';
import { CosmeticsFileStore } from './lib/file-store.js';
import { cosmeticsRoutes } from './routes/cosmetics.js';
import { healthRoutes } from './routes/health.js';

export interface CreateAppOptions {
  config: AppConfig;
  logger?: FastifyBaseLogger | boolean;
}

export async function createApp(options: CreateAppOptions): Promise<FastifyInstance> {
  const app = Fastify({
    logger: options.logger ?? true
  });

  const cosmeticsStore = new CosmeticsFileStore(options.config.COSMETICS_FILE, app.log);
  await cosmeticsStore.initialize();

  await app.register(healthRoutes);
  await app.register(cosmeticsRoutes, {
    prefix: '/v1/cosmetics',
    cosmeticsStore,
    apiKeyGuard: createApiKeyGuard(options.config.API_KEY)
  });

  return app;
}
