import { z } from 'zod';

export const uuidSchema = z.string().uuid();
export const strictMinecraftUuidSchema = z.string().regex(/^[0-9a-f]{32}$/i);
export const mcidCodeRequestBodySchema = z.object({
  identifier: z.string().trim().min(1)
});
export const mcidCodeVerifyBodySchema = z.object({
  code: z.string().trim().regex(/^\d{6}$/),
  uuid: z.union([uuidSchema, strictMinecraftUuidSchema])
});

const numericSchema = z.number().finite();

export const rawCosmeticEntrySchema = z
  .object({
    username: z.string().optional(),
    aliases: z.array(z.string()).optional(),
    displayName: z.string().optional(),
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

export const cosmeticsWritableUpdateBodySchema = z
  .object({
    displayName: z.string().min(1).nullable().optional(),
    scale: normalizedScaleSchema.nullable().optional()
  })
  .strict()
  .refine((value) => value.displayName !== undefined || value.scale !== undefined, {
    message: 'At least one writable field must be provided'
  });

export const normalizedPlayerSchema = z.object({
  username: z.string().nullable(),
  aliases: z.array(z.string()),
  displayName: z.string().nullable(),
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
export type CosmeticsWritableUpdateBody = z.infer<typeof cosmeticsWritableUpdateBodySchema>;
export type McidCodeRequestBody = z.infer<typeof mcidCodeRequestBodySchema>;
export type McidCodeVerifyBody = z.infer<typeof mcidCodeVerifyBodySchema>;
