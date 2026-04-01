import { describe, expect, it } from 'vitest';

import {
  canonicalizeMinecraftUuid,
  getMcidVerificationConfig,
  toCompactMinecraftUuid
} from '../src/auth/mc-id.js';

describe('MC-ID helpers', () => {
  it('canonicalizes and compacts Minecraft UUIDs', () => {
    expect(canonicalizeMinecraftUuid('069a79f444e94726a5befca90e38aaf5')).toBe(
      '069a79f4-44e9-4726-a5be-fca90e38aaf5'
    );
    expect(toCompactMinecraftUuid('069a79f4-44e9-4726-a5be-fca90e38aaf5')).toBe(
      '069a79f444e94726a5befca90e38aaf5'
    );
  });

  it('returns the expected verification-code flow config', () => {
    expect(getMcidVerificationConfig()).toEqual({
      codeLength: 6,
      codeTtlSeconds: 300,
      flow: 'verification_code',
      identifierField: 'identifier'
    });
  });
});
