import { z } from 'zod';

export const uuidSchema = z.string().uuid();
export const usernameUpdateBodySchema = z.object({
  username: z.string().trim().min(1)
});

const numericSchema = z.number().finite();

export const rawCosmeticEntrySchema = z
  .object({
    username: z.string().optional(),
    aliases: z.array(z.string()).optional(),
    displayName: z.string().optional(),
    badges: z.array(z.string()).optional(),
    sizeX: numericSchema.optional(),
    sizeY: numericSchema.optional(),
    sizeZ: numericSchema.optional(),
    x: numericSchema.optional(),
    y: numericSchema.optional(),
    z: numericSchema.optional()
  })
  .passthrough();

export const rawCosmeticsDocumentSchema = z.record(z.string(), z.unknown());

export const normalizedScaleSchema = z.object({
  x: numericSchema,
  y: numericSchema,
  z: numericSchema
});

export const normalizedPlayerSchema = z.object({
  username: z.string().nullable(),
  aliases: z.array(z.string()),
  displayName: z.string().nullable(),
  badges: z.array(z.string()),
  scale: normalizedScaleSchema.nullable()
});

export const normalizedSnapshotSchema = z.object({
  version: z.literal(1),
  updatedAt: z.string().datetime(),
  players: z.record(uuidSchema, normalizedPlayerSchema)
});

export type RawCosmeticEntry = z.infer<typeof rawCosmeticEntrySchema>;
export type NormalizedScale = z.infer<typeof normalizedScaleSchema>;
export type NormalizedPlayer = z.infer<typeof normalizedPlayerSchema>;
export type NormalizedSnapshot = z.infer<typeof normalizedSnapshotSchema>;
export type UsernameUpdateBody = z.infer<typeof usernameUpdateBodySchema>;
