/**
 * Thin pointer for Astro/docs consumers: the secure session editor lives in
 * `web/session-editor` (React) and talks to the Rust `web/session-api`.
 * Production session load/save must not use bytebin.lucko.me.
 */
export const SESSION_API_DEFAULT_BASE =
  (typeof import.meta !== 'undefined' &&
    // @ts-expect-error optional vite env in Astro
    (import.meta.env?.PUBLIC_SESSION_API_URL as string | undefined)) ||
  'http://127.0.0.1:18787';

export function sessionEditorPath(code: string, token: string): string {
  const q = new URLSearchParams({ code, token });
  return `/session-editor/?${q.toString()}`;
}
