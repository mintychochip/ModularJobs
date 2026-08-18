import type { EditorPayload, SessionEnvelope } from './types';

/**
 * Client for the Rust web/rest-api (MySQL-backed secure sessions).
 * Does not use bytebin.lucko.me.
 *
 * Session creation is not exposed in the browser: the Paper server creates
 * the session and hands the editor a code + token + per-server API origin.
 */
export class SessionApiClient {
  constructor(private readonly baseUrl: string) {
    if (!baseUrl) {
      throw new Error('API base URL is required');
    }
  }

  get base(): string {
    return this.baseUrl.replace(/\/$/, '');
  }

  /**
   * Load a session by code + token. Both are required.
   */
  async fetchSession(code: string, token: string): Promise<EditorPayload> {
    if (!code?.trim()) {
      throw new Error('Session code is required');
    }
    if (!token?.trim()) {
      throw new Error('Session token is required');
    }
    const response = await fetch(`${this.base}/api/v1/sessions/${encodeURIComponent(code)}/payload`, {
      headers: {
        Authorization: `Bearer ${token}`,
        'X-Session-Token': token,
      },
    });
    if (!response.ok) {
      if (response.status === 404) {
        throw new Error('Session not found or expired');
      }
      if (response.status === 401 || response.status === 403) {
        throw new Error('Invalid session token');
      }
      throw new Error(`Failed to fetch session: ${response.status}`);
    }
    return response.json();
  }

  /**
   * Save (replace) session payload. Requires matching code + token.
   */
  async saveSession(code: string, token: string, payload: EditorPayload): Promise<SessionEnvelope> {
    if (!code?.trim()) {
      throw new Error('Session code is required');
    }
    if (!token?.trim()) {
      throw new Error('Session token is required');
    }
    // Ensure token embedded in payload matches auth
    const toSave: EditorPayload = {
      ...payload,
      metadata: {
        ...payload.metadata,
        sessionToken: token,
      },
    };
    const response = await fetch(`${this.base}/api/v1/sessions/${encodeURIComponent(code)}`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`,
        'X-Session-Token': token,
      },
      body: JSON.stringify(toSave),
    });
    if (!response.ok) {
      if (response.status === 401 || response.status === 403) {
        throw new Error('Invalid session token');
      }
      throw new Error(`Failed to save session: ${response.status}`);
    }
    return response.json();
  }
}

/** Resolve API base URL from Vite env, optionally overridden by ?api= in the URL. */
export function resolveApiBaseUrl(
  env: Record<string, string | undefined> = import.meta.env as Record<string, string | undefined>,
  search: string = typeof window !== 'undefined' ? window.location.search : '',
): string {
  const params = new URLSearchParams(search.startsWith('?') ? search.slice(1) : search);
  const runtimeApi = params.get('api')?.trim() ?? '';
  if (runtimeApi) {
    return validateApiBase(runtimeApi, env);
  }
  return (
    env.VITE_SESSION_API_URL ||
    env.VITE_API_BASE_URL ||
    'http://127.0.0.1:18787'
  );
}

/**
 * Validate a runtime ?api= URL before we send the secret token to it.
 * - Must be a valid URL.
 * - Must use http: or https: scheme (javascript:, file:, ws:, data:, … rejected).
 * - Must be HTTPS unless VITE_ALLOW_HTTP_API=true.
 * - Origin must match the VITE_ALLOWED_API_ORIGINS allow-list (or the build-time
 *   default origin if no allow-list is configured).
 */
export function validateApiBase(
  apiBase: string,
  env: Record<string, string | undefined>,
): string {
  let url: URL;
  try {
    url = new URL(apiBase);
  } catch {
    throw new Error(`Invalid ?api= URL: ${apiBase}`);
  }
  if (url.protocol !== 'http:' && url.protocol !== 'https:') {
    throw new Error(`?api= URL must use HTTP or HTTPS: ${apiBase}`);
  }

  const allowHttp = env.VITE_ALLOW_HTTP_API === 'true';
  if (url.protocol !== 'https:' && !allowHttp) {
    throw new Error(`?api= URL must use HTTPS in production: ${apiBase}`);
  }

  // Reject URLs carrying credentials, query strings, or fragments: the base URL
  // must be a bare origin+path so the token is never sent to a crafted target
  // and so path/query can never smuggle unexpected destinations.
  if (url.username || url.password) {
    throw new Error(`?api= URL must not contain credentials: ${apiBase}`);
  }
  if (url.search) {
    throw new Error(`?api= URL must not contain a query string: ${apiBase}`);
  }
  if (url.hash) {
    throw new Error(`?api= URL must not contain a fragment: ${apiBase}`);
  }

  // Normalize: keep origin + path only, with no trailing slash.
  const normalize = (u: URL): string => {
    const base = u.origin + u.pathname;
    return base.endsWith('/') ? base.slice(0, -1) : base;
  };

  const allowed = (env.VITE_ALLOWED_API_ORIGINS ?? '').trim();
  const patterns = allowed
    ? allowed.split(',').map((p) => p.trim()).filter(Boolean)
    : [];

  if (patterns.length === 0) {
    // No allow-list: only accept the build-time default origin.
    const defaultBase = env.VITE_SESSION_API_URL || env.VITE_API_BASE_URL;
    const defaultUrl = defaultBase ? new URL(defaultBase) : null;
    if (defaultUrl && url.origin === defaultUrl.origin) {
      return normalize(url);
    }
    throw new Error(
      '?api= URL is not in the allowed API origin list; set VITE_ALLOWED_API_ORIGINS'
    );
  }

  if (!patterns.some((p) => originMatchesPattern(url.origin, p))) {
    throw new Error(`?api= URL origin ${url.origin} is not allowed`);
  }

  return normalize(url);
}

/**
 * Match an origin against a pattern. Supports:
 * - Exact: `https://api.example.com`
 * - Glob (`*` matches any sequence): `https://*.example.com`, `http://localhost:*`
 */
export function originMatchesPattern(origin: string, pattern: string): boolean {
  if (origin === pattern) return true;
  if (!pattern.includes('*')) return false;

  const segments = pattern.split('*');
  let rest = origin;

  for (let i = 0; i < segments.length; i++) {
    const segment = segments[i];
    if (i === 0) {
      if (!rest.startsWith(segment)) return false;
      rest = rest.slice(segment.length);
    } else if (i === segments.length - 1) {
      return rest.endsWith(segment);
    } else {
      const idx = rest.indexOf(segment);
      if (idx === -1) return false;
      rest = rest.slice(idx + segment.length);
    }
  }

  return true;
}

export function createDefaultClient(
  env?: Record<string, string | undefined>,
  search?: string,
): SessionApiClient {
  return new SessionApiClient(resolveApiBaseUrl(env, search));
}

/**
 * Pure helper used by the editor UI and tests: apply a payable amount edit.
 */
export function setTaskPayableAmount(
  payload: EditorPayload,
  jobKey: string,
  taskIndex: number,
  payableIndex: number,
  amount: string,
): EditorPayload {
  const jobs = { ...payload.jobs };
  const job = jobs[jobKey];
  if (!job) {
    throw new Error(`Unknown job: ${jobKey}`);
  }
  const tasks = job.tasks.map((t, i) => {
    if (i !== taskIndex) return t;
    const payables = t.payables.map((p, j) =>
      j === payableIndex ? { ...p, amount } : p,
    );
    return { ...t, payables };
  });
  jobs[jobKey] = { ...job, tasks };
  return { ...payload, jobs };
}