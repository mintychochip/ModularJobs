/**
 * Read session code + token from the URL.
 *
 * Prefer the hash fragment for the secret token so it is not sent to the
 * server/proxy in the request line or leaked via Referer:
 *   /session-editor/?code=ABC#token=SECRET
 *
 * Legacy `?token=` / `?sessionToken=` query params are still accepted, then
 * optionally scrubbed via {@link scrubTokenFromQuery}.
 */
export function readSessionCredentials(
  search: string = typeof window !== 'undefined' ? window.location.search : '',
  hash: string = typeof window !== 'undefined' ? window.location.hash : '',
): { code: string; token: string; tokenFromQuery: boolean } {
  const params = new URLSearchParams(search.startsWith('?') ? search.slice(1) : search);
  const hashParams = new URLSearchParams(
    hash.startsWith('#') ? hash.slice(1) : hash,
  );

  const code =
    hashParams.get('code') ??
    params.get('code') ??
    params.get('session') ??
    '';

  const hashToken = hashParams.get('token') ?? hashParams.get('sessionToken') ?? '';
  const queryToken = params.get('token') ?? params.get('sessionToken') ?? '';
  const token = hashToken || queryToken;

  return {
    code,
    token,
    tokenFromQuery: Boolean(queryToken) && !hashToken,
  };
}

/**
 * Build a safe editor path: public code in query, secret token in hash only.
 */
export function sessionEditorPath(code: string, token: string): string {
  const q = new URLSearchParams({ code });
  const h = new URLSearchParams({ token });
  return `?${q.toString()}#${h.toString()}`;
}

/**
 * If the token is only present on the query string, rewrite the URL so the
 * token moves into the hash and is removed from search (history + Referer hygiene).
 * No-op when there is no window or nothing to scrub.
 */
export function scrubTokenFromQuery(): void {
  if (typeof window === 'undefined' || !window.history?.replaceState) {
    return;
  }
  const { code, token, tokenFromQuery } = readSessionCredentials(
    window.location.search,
    window.location.hash,
  );
  if (!tokenFromQuery || !token) {
    return;
  }
  const params = new URLSearchParams(window.location.search.slice(1));
  params.delete('token');
  params.delete('sessionToken');
  if (code && !params.get('code') && !params.get('session')) {
    params.set('code', code);
  }
  const search = params.toString() ? `?${params.toString()}` : '';
  const hashParams = new URLSearchParams(
    window.location.hash.startsWith('#')
      ? window.location.hash.slice(1)
      : window.location.hash,
  );
  hashParams.set('token', token);
  const hash = `#${hashParams.toString()}`;
  const next = `${window.location.pathname}${search}${hash}`;
  window.history.replaceState(null, '', next);
}
