import { describe, expect, it } from 'vitest';

import { extractPrimaryMcidAccount } from '../src/auth/mc-id.js';

describe('extractPrimaryMcidAccount', () => {
  it('extracts the primary account and canonicalizes undashed UUIDs', () => {
    const account = extractPrimaryMcidAccount({
      accounts: [
        {
          uuid: 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa',
          username: 'Secondary',
          primary: false
        },
        {
          uuid: '069a79f444e94726a5befca90e38aaf5',
          username: 'Notch',
          primary: true
        }
      ]
    });

    expect(account).toEqual({
      uuid: '069a79f4-44e9-4726-a5be-fca90e38aaf5',
      username: 'Notch'
    });
  });
});
