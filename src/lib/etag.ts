import { createHash } from 'node:crypto';

export function buildEtag(payload: unknown): string {
  const serializedPayload = JSON.stringify(payload);
  const digest = createHash('sha1').update(serializedPayload).digest('base64url');

  return `"${digest}"`;
}

export function ifNoneMatchIncludes(headerValue: string | string[], etag: string): boolean {
  const normalizedHeader = Array.isArray(headerValue) ? headerValue.join(',') : headerValue;

  return normalizedHeader
    .split(',')
    .map((candidate) => candidate.trim())
    .some((candidate) => {
      if (candidate === '*') {
        return true;
      }

      const strippedCandidate = candidate.startsWith('W/') ? candidate.slice(2) : candidate;
      const strippedEtag = etag.startsWith('W/') ? etag.slice(2) : etag;

      return strippedCandidate === strippedEtag;
    });
}
