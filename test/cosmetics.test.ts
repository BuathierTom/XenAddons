import { describe, expect, it, vi } from 'vitest';

import { normalizeCosmeticsDocument } from '../src/domain/cosmetics.js';

describe('normalizeCosmeticsDocument', () => {
  it('normalizes explicit badges and legacy sizeX/sizeY/sizeZ scale', () => {
    const logger = {
      warn: vi.fn()
    };

    const snapshot = normalizeCosmeticsDocument(
      {
        '24c56ab5-b504-4d38-bd06-e5af6686fcaa': {
          username: 'dtXenus',
          aliases: ['dtXenus'],
          displayName: 'FancyName',
          badges: ['france', 'staff'],
          sizeX: 2,
          sizeY: 0.5,
          sizeZ: 1
        }
      },
      new Date('2026-04-01T12:00:00.000Z'),
      logger
    );

    expect(snapshot).toEqual({
      version: 1,
      updatedAt: '2026-04-01T12:00:00.000Z',
      players: {
        '24c56ab5-b504-4d38-bd06-e5af6686fcaa': {
          username: 'dtXenus',
          aliases: ['dtXenus'],
          displayName: 'FancyName',
          badges: ['france', 'staff'],
          scale: {
            x: 2,
            y: 0.5,
            z: 1
          }
        }
      }
    });
    expect(logger.warn).not.toHaveBeenCalled();
  });

  it('extracts legacy badge suffixes and ignores invalid entries', () => {
    const logger = {
      warn: vi.fn()
    };

    const snapshot = normalizeCosmeticsDocument(
      {
        '370b896c-257d-4707-b40a-996be37769a6': {
          username: 'Antorak',
          aliases: ['Antorak'],
          displayName: 'Antorak {capy}',
          x: 1,
          y: 1,
          z: 1
        },
        'not-a-uuid': {
          username: 'Broken'
        }
      },
      new Date('2026-04-01T12:00:00.000Z'),
      logger
    );

    expect(snapshot.players['370b896c-257d-4707-b40a-996be37769a6']).toEqual({
      username: 'Antorak',
      aliases: ['Antorak'],
      displayName: 'Antorak',
      badges: ['capy'],
      scale: {
        x: 1,
        y: 1,
        z: 1
      }
    });
    expect(snapshot.players['not-a-uuid']).toBeUndefined();
    expect(logger.warn).toHaveBeenCalledTimes(1);
  });
});
