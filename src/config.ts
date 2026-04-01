import { z } from 'zod';

const booleanSchema = z.preprocess((value) => {
  if (typeof value === 'string') {
    if (value === 'true') {
      return true;
    }

    if (value === 'false') {
      return false;
    }
  }

  return value;
}, z.boolean());

const sameSiteSchema = z.enum(['lax', 'strict', 'none']);

const envSchema = z
  .object({
    NODE_ENV: z.enum(['development', 'test', 'production']).default('development'),
    PORT: z.coerce.number().int().min(1).max(65535).default(3000),
    HOST: z.string().min(1).default('0.0.0.0'),
    TRUST_PROXY: booleanSchema.default(false),
    COSMETICS_FILE: z.string().min(1).default('./data/cosmeticPeople.json'),
    API_KEY: z.string().trim().min(1),
    FRONTEND_ORIGIN: z.string().url(),
    BETTER_AUTH_URL: z.string().url(),
    BETTER_AUTH_SECRET: z.string().min(32),
    BETTER_AUTH_BASE_PATH: z.string().min(1).default('/api/auth'),
    SESSION_TTL_SECONDS: z.coerce.number().int().min(300).max(60 * 60 * 24 * 30).default(60 * 60 * 24 * 7),
    SESSION_COOKIE_SECURE: booleanSchema.default(false),
    SESSION_COOKIE_SAME_SITE: sameSiteSchema.default('lax'),
    MCID_CLIENT_ID: z.string().min(1),
    MCID_CLIENT_SECRET: z.string().min(1),
    MCID_DISCOVERY_URL: z
      .string()
      .url()
      .default('https://mc-id.com/.well-known/openid-configuration')
  })
  .superRefine((env, ctx) => {
    if (env.SESSION_COOKIE_SAME_SITE === 'none' && !env.SESSION_COOKIE_SECURE) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        path: ['SESSION_COOKIE_SECURE'],
        message: 'SESSION_COOKIE_SECURE must be true when SESSION_COOKIE_SAME_SITE is none'
      });
    }

    const authBasePath = env.BETTER_AUTH_BASE_PATH.startsWith('/')
      ? env.BETTER_AUTH_BASE_PATH
      : `/${env.BETTER_AUTH_BASE_PATH}`;

    if (!authBasePath.startsWith('/')) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        path: ['BETTER_AUTH_BASE_PATH'],
        message: 'BETTER_AUTH_BASE_PATH must start with /'
      });
    }
  });

export type AppConfig = z.infer<typeof envSchema>;

export function readConfig(env: NodeJS.ProcessEnv = process.env): AppConfig {
  return envSchema.parse(env);
}
