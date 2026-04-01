import 'dotenv/config';

import { createApp } from './app.js';
import { readConfig } from './config.js';

const config = readConfig();
const app = await createApp({ config });

const shutdown = async (signal: string): Promise<void> => {
  app.log.info({ signal }, 'Shutting down HTTP server');

  try {
    await app.close();
    process.exit(0);
  } catch (error) {
    app.log.error({ err: error }, 'Failed to close HTTP server cleanly');
    process.exit(1);
  }
};

for (const signal of ['SIGINT', 'SIGTERM'] as const) {
  process.on(signal, () => {
    void shutdown(signal);
  });
}

try {
  await app.listen({
    host: config.HOST,
    port: config.PORT
  });
} catch (error) {
  app.log.error({ err: error }, 'Failed to start HTTP server');
  process.exit(1);
}
