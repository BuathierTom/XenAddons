import type { FastifyBaseLogger } from 'fastify';
import { ZodError } from 'zod';

import {
  normalizedSnapshotSchema,
  rawCosmeticEntrySchema,
  rawCosmeticsDocumentSchema,
  type NormalizedPlayer,
  type NormalizedScale,
  type NormalizedSnapshot,
  type RawCosmeticEntry
} from './schemas.js';

export type { NormalizedPlayer, NormalizedScale, NormalizedSnapshot } from './schemas.js';

function formatZodError(error: ZodError): string {
  return error.issues
    .map((issue) => {
      const path = issue.path.length > 0 ? issue.path.join('.') : '<root>';
      return `${path}: ${issue.message}`;
    })
    .join('; ');
}

function normalizeScale(entry: RawCosmeticEntry): NormalizedScale | null {
  const usesLegacyScale =
    entry.sizeX !== undefined || entry.sizeY !== undefined || entry.sizeZ !== undefined;
  const usesCompactScale = entry.x !== undefined || entry.y !== undefined || entry.z !== undefined;

  if (!usesLegacyScale && !usesCompactScale) {
    return null;
  }

  if (usesLegacyScale && usesCompactScale) {
    throw new Error('Scale fields cannot mix sizeX/sizeY/sizeZ and x/y/z');
  }

  if (usesLegacyScale) {
    if (entry.sizeX === undefined || entry.sizeY === undefined || entry.sizeZ === undefined) {
      throw new Error('Incomplete legacy scale, expected sizeX, sizeY and sizeZ');
    }

    return {
      x: entry.sizeX,
      y: entry.sizeY,
      z: entry.sizeZ
    };
  }

  if (entry.x === undefined || entry.y === undefined || entry.z === undefined) {
    throw new Error('Incomplete scale, expected x, y and z');
  }

  return {
    x: entry.x,
    y: entry.y,
    z: entry.z
  };
}

export function normalizePlayerEntry(entry: RawCosmeticEntry): NormalizedPlayer {
  return {
    username: entry.username ?? null,
    aliases: entry.aliases ? [...entry.aliases] : [],
    displayName: entry.displayName ?? null,
    scale: normalizeScale(entry)
  };
}

export function normalizeCosmeticsDocument(
  rawDocument: unknown,
  updatedAt: Date,
  logger: Pick<FastifyBaseLogger, 'warn'>
): NormalizedSnapshot {
  const parsedDocument = rawCosmeticsDocumentSchema.safeParse(rawDocument);

  if (!parsedDocument.success) {
    throw new Error(`Invalid cosmetics root document: ${formatZodError(parsedDocument.error)}`);
  }

  const players = Object.entries(parsedDocument.data)
    .sort(([leftUuid], [rightUuid]) => leftUuid.localeCompare(rightUuid))
    .reduce<Record<string, NormalizedPlayer>>((accumulator, [uuid, rawEntry]) => {
      if (!/^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(uuid)) {
        logger.warn({ uuid }, 'Ignoring cosmetics entry with invalid UUID');
        return accumulator;
      }

      const parsedEntry = rawCosmeticEntrySchema.safeParse(rawEntry);

      if (!parsedEntry.success) {
        logger.warn(
          {
            uuid,
            issues: formatZodError(parsedEntry.error)
          },
          'Ignoring cosmetics entry with invalid structure'
        );
        return accumulator;
      }

      try {
        accumulator[uuid] = normalizePlayerEntry(parsedEntry.data);
      } catch (error) {
        logger.warn(
          {
            uuid,
            error: error instanceof Error ? error.message : 'Unknown normalization error'
          },
          'Ignoring cosmetics entry that could not be normalized'
        );
      }

      return accumulator;
    }, {});

  return normalizedSnapshotSchema.parse({
    version: 1,
    updatedAt: updatedAt.toISOString(),
    players
  });
}
