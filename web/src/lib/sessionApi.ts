/**
 * Thin pointer for Astro/docs consumers: the secure session editor lives in
 * `web/session-editor` (React) and talks to the Rust `web/rest-api`.
 * Production session load/save must not use bytebin.lucko.me.
 */
export const SESSION_API_DEFAULT_BASE =
  (typeof import.meta !== 'undefined' &&
    // @ts-expect-error optional vite env in Astro
    (import.meta.env?.PUBLIC_SESSION_API_URL as string | undefined)) ||
  'http://127.0.0.1:18787';

/**
 * Secure editor URL: public code in query, secret token in hash only
 * (avoids Referer / access-log leakage of the session secret).
 */
export function sessionEditorPath(code: string, token: string): string {
  const q = new URLSearchParams({ code });
  const h = new URLSearchParams({ token });
  return `/editor/?${q.toString()}#${h.toString()}`;
}