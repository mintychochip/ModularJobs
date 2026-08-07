import type { CreateSessionResponse, EditorPayload, SessionEnvelope } from './types';

/**
 * Client for the Rust web/rest-api (Postgres-backed secure sessions).
 * Does not use bytebin.lucko.me.
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
   * Create a session. Server always mints the token.
   * When the API sets SESSION_CREATE_SECRET, pass the same value as createSecret
   * (or set VITE_SESSION_CREATE_SECRET for the default client).
   */
  async createSession(
    payload: EditorPayload,
    createSecret?: string,
  ): Promise<CreateSessionResponse> {
    const headers: Record<string, string> = { 'Content-Type': 'application/json' };
    if (createSecret) {
      headers['X-Create-Secret'] = createSecret;
    }
    const response = await fetch(`${this.base}/api/v1/sessions`, {
      method: 'POST',
      headers,
      body: JSON.stringify(payload),
    });
    if (!response.ok) {
      throw new Error(`Failed to create session: ${response.status}`);
    }
    return response.json();
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

/** Resolve API base URL from Vite env or runtime default. */
export function resolveApiBaseUrl(
  env: Record<string, string | undefined> = import.meta.env as Record<string, string | undefined>,
): string {
  return (
    env.VITE_SESSION_API_URL ||
    env.VITE_API_BASE_URL ||
    'http://127.0.0.1:18787'
  );
}

export function createDefaultClient(
  env?: Record<string, string | undefined>,
): SessionApiClient {
  return new SessionApiClient(resolveApiBaseUrl(env));
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
