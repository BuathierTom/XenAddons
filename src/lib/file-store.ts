import { readFile, rename, rm, stat, writeFile } from 'node:fs/promises';
import { dirname, resolve } from 'node:path';

import type { FastifyBaseLogger } from 'fastify';
import { ZodError } from 'zod';

import {
  normalizeCosmeticsDocument,
  normalizePlayerEntry,
  type NormalizedPlayer,
  type NormalizedSnapshot
} from '../domain/cosmetics.js';
import { rawCosmeticEntrySchema, rawCosmeticsDocumentSchema } from '../domain/schemas.js';
import { buildEtag } from './etag.js';

export interface CachedCosmeticsSnapshot {
  etag: string;
  mtimeMs: number;
  snapshot: NormalizedSnapshot;
}

export interface UpdatedPlayerSnapshot extends CachedCosmeticsSnapshot {
  player: NormalizedPlayer;
}

function formatZodError(error: ZodError): string {
  return error.issues
    .map((issue) => {
      const path = issue.path.length > 0 ? issue.path.join('.') : '<root>';
      return `${path}: ${issue.message}`;
    })
    .join('; ');
}

function isObjectRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
}

export class CosmeticsFileStore {
  private readonly filePath: string;
  private cache: CachedCosmeticsSnapshot | null = null;
  private failedMtimeMs: number | null = null;
  private mutationQueue: Promise<void> = Promise.resolve();

  public constructor(filePath: string, private readonly logger: FastifyBaseLogger) {
    this.filePath = resolve(filePath);
  }

  public async initialize(): Promise<void> {
    await this.loadSnapshot();
  }

  public async getSnapshot(): Promise<CachedCosmeticsSnapshot> {
    await this.mutationQueue;

    const fileStats = await stat(this.filePath);

    if (this.cache && fileStats.mtimeMs === this.cache.mtimeMs) {
      return this.cache;
    }

    if (this.cache && this.failedMtimeMs !== null && fileStats.mtimeMs === this.failedMtimeMs) {
      return this.cache;
    }

    try {
      return await this.loadSnapshot(fileStats.mtimeMs);
    } catch (error) {
      if (!this.cache) {
        throw error;
      }

      this.failedMtimeMs = fileStats.mtimeMs;
      this.logger.error(
        {
          err: error,
          cosmeticsFile: this.filePath
        },
        'Failed to reload cosmetics file, serving last known snapshot'
      );
      return this.cache;
    }
  }

  public async updateUsername(uuid: string, username: string): Promise<UpdatedPlayerSnapshot | null> {
    const operation = this.mutationQueue.then(() => this.performUpdateUsername(uuid, username));
    this.mutationQueue = operation.then(
      () => undefined,
      () => undefined
    );

    return operation;
  }

  private async loadSnapshot(knownMtimeMs?: number): Promise<CachedCosmeticsSnapshot> {
    const [fileContents, fileStats] = await Promise.all([
      readFile(this.filePath, 'utf8'),
      stat(this.filePath)
    ]);
    const effectiveMtimeMs = knownMtimeMs ?? fileStats.mtimeMs;
    const parsedDocument = JSON.parse(fileContents) as unknown;
    const snapshot = normalizeCosmeticsDocument(parsedDocument, new Date(effectiveMtimeMs), this.logger);
    const etag = buildEtag(snapshot);

    this.failedMtimeMs = null;
    this.cache = {
      etag,
      mtimeMs: effectiveMtimeMs,
      snapshot
    };

    this.logger.info(
      {
        cosmeticsFile: this.filePath,
        updatedAt: snapshot.updatedAt,
        playerCount: Object.keys(snapshot.players).length
      },
      'Loaded cosmetics snapshot'
    );

    return this.cache;
  }

  private async performUpdateUsername(uuid: string, username: string): Promise<UpdatedPlayerSnapshot | null> {
    const rawDocument = await this.readRawDocument();
    const existingEntry = rawDocument[uuid];

    if (existingEntry === undefined) {
      return null;
    }

    if (!isObjectRecord(existingEntry)) {
      throw new Error('Player entry is not a JSON object');
    }

    const nextEntry: Record<string, unknown> = {
      ...existingEntry,
      username
    };
    const parsedEntry = rawCosmeticEntrySchema.safeParse(nextEntry);

    if (!parsedEntry.success) {
      throw new Error(`Updated player entry is invalid: ${formatZodError(parsedEntry.error)}`);
    }

    const normalizedPlayer = normalizePlayerEntry(parsedEntry.data);
    rawDocument[uuid] = nextEntry;

    await this.writeRawDocument(rawDocument);

    const cachedSnapshot = await this.loadSnapshot();
    const snapshotPlayer = cachedSnapshot.snapshot.players[uuid];

    if (!snapshotPlayer) {
      throw new Error('Updated player entry is no longer available in the normalized snapshot');
    }

    return {
      ...cachedSnapshot,
      player: normalizedPlayer
    };
  }

  private async readRawDocument(): Promise<Record<string, unknown>> {
    const fileContents = await readFile(this.filePath, 'utf8');
    const parsedDocument = JSON.parse(fileContents) as unknown;
    const parsedResult = rawCosmeticsDocumentSchema.safeParse(parsedDocument);

    if (!parsedResult.success) {
      throw new Error(`Invalid cosmetics root document: ${formatZodError(parsedResult.error)}`);
    }

    return { ...parsedResult.data };
  }

  private async writeRawDocument(rawDocument: Record<string, unknown>): Promise<void> {
    const serializedDocument = `${JSON.stringify(rawDocument, null, 2)}\n`;
    const temporaryFilePath = resolve(
      dirname(this.filePath),
      `${Date.now()}-${process.pid}-cosmetics.tmp`
    );

    try {
      await writeFile(temporaryFilePath, serializedDocument, 'utf8');
      await rename(temporaryFilePath, this.filePath);
    } catch (error) {
      await rm(temporaryFilePath, { force: true });
      throw error;
    }
  }
}
