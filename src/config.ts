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

const rawEnvSchema = z
  .object({
    NODE_ENV: z.enum(['development', 'test', 'production']).default('development'),
    PORT: z.coerce.number().int().min(1).max(65535).default(3000),
    HOST: z.string().min(1).default('0.0.0.0'),
    TRUST_PROXY: booleanSchema.default(false),
    COSMETICS_FILE: z.string().min(1).default('./data/cosmeticPeople.json'),
    API_KEY: z.string().trim().min(1),
    FRONTEND_ORIGIN: z.string().url(),
    SESSION_SECRET: z.string().min(32).optional(),
    BETTER_AUTH_SECRET: z.string().min(32).optional(),
    SESSION_TTL_SECONDS: z.coerce.number().int().min(300).max(60 * 60 * 24 * 30).default(60 * 60 * 24 * 7),
    SESSION_COOKIE_SECURE: booleanSchema.default(false),
    SESSION_COOKIE_SAME_SITE: sameSiteSchema.default('lax'),
    MCID_API_KEY: z.string().min(1),
    MCID_CODE_REQUEST_URL: z
      .string()
      .url()
      .default('https://mc-id.com/api/v1/codes/request'),
    MCID_CODE_VERIFY_URL: z
      .string()
      .url()
      .default('https://mc-id.com/api/v1/codes/verify')
  })
  .superRefine((env, ctx) => {
    if (env.SESSION_COOKIE_SAME_SITE === 'none' && !env.SESSION_COOKIE_SECURE) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        path: ['SESSION_COOKIE_SECURE'],
        message: 'SESSION_COOKIE_SECURE must be true when SESSION_COOKIE_SAME_SITE is none'
      });
    }

    const sessionSecret = env.SESSION_SECRET ?? env.BETTER_AUTH_SECRET;

    if (!sessionSecret) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        path: ['SESSION_SECRET'],
        message: 'SESSION_SECRET is required (or use BETTER_AUTH_SECRET as a deprecated fallback)'
      });
    }
  });

type RawEnv = z.infer<typeof rawEnvSchema>;

export interface AppConfig extends Omit<RawEnv, 'BETTER_AUTH_SECRET'> {
  SESSION_SECRET: string;
}

export function readConfig(env: NodeJS.ProcessEnv = process.env): AppConfig {
  const parsed = rawEnvSchema.parse(env);

  return {
    ...parsed,
    SESSION_SECRET: parsed.SESSION_SECRET ?? parsed.BETTER_AUTH_SECRET!
  };
}
