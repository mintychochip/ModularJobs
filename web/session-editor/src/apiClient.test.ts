import { describe, expect, it, vi, beforeEach } from 'vitest';
import {
  SessionApiClient,
  originMatchesPattern,
  resolveApiBaseUrl,
  setTaskPayableAmount,
  validateApiBase,
} from './apiClient';
import type { EditorPayload } from './types';

function samplePayload(): EditorPayload {
  return {
    version: 1,
    metadata: {
      exportedAt: '2026-08-06T00:00:00Z',
      exportedBy: 'tester',
      sessionToken: 'tok-abc',
      serverName: 'dev',
    },
    jobs: {
      'modularjobs:miner': {
        displayName: 'Miner',
        tasks: [
          {
            actionTypeKey: 'modularjobs:block_break',
            contextKey: 'minecraft:stone',
            payables: [{ type: 'modularjobs:experience', amount: '2.5' }],
          },
        ],
      },
    },
    registeredActionTypes: ['modularjobs:block_break'],
    registeredPayableTypes: ['modularjobs:experience'],
  };
}

describe('resolveApiBaseUrl', () => {
  it('does not default to bytebin', () => {
    const url = resolveApiBaseUrl({});
    expect(url).not.toContain('bytebin');
    expect(url).toMatch(/^https?:\/\//);
  });

  it('prefers VITE_SESSION_API_URL', () => {
    expect(
      resolveApiBaseUrl({ VITE_SESSION_API_URL: 'http://api.example:9000' }),
    ).toBe('http://api.example:9000');
  });

  it('uses ?api= when allowed', () => {
    const env = {
      VITE_SESSION_API_URL: 'http://api.example:9000',
      VITE_ALLOWED_API_ORIGINS: 'https://*.modularjobs.com',
    };
    expect(resolveApiBaseUrl(env, '?api=https://s1.modularjobs.com')).toBe(
      'https://s1.modularjobs.com',
    );
  });

  it('rejects ?api= with wrong origin', () => {
    const env = {
      VITE_ALLOWED_API_ORIGINS: 'https://*.modularjobs.com',
    };
    expect(() =>
      resolveApiBaseUrl(env, '?api=https://evil.com'),
    ).toThrow(/not allowed/);
  });

  it('rejects non-HTTPS ?api= in production', () => {
    const env = {
      VITE_ALLOWED_API_ORIGINS: 'http://*',
    };
    expect(() =>
      resolveApiBaseUrl(env, '?api=http://api.modularjobs.com'),
    ).toThrow(/must use HTTPS/);
  });

  it('allows non-HTTPS ?api= with VITE_ALLOW_HTTP_API', () => {
    const env = {
      VITE_ALLOWED_API_ORIGINS: 'http://localhost:*',
      VITE_ALLOW_HTTP_API: 'true',
    };
    expect(
      resolveApiBaseUrl(env, '?api=http://localhost:18787'),
    ).toBe('http://localhost:18787');
  });
});

describe('validateApiBase', () => {
  it('accepts exact allowed origin', () => {
    expect(
      validateApiBase('https://s1.modularjobs.com', {
        VITE_ALLOWED_API_ORIGINS: 'https://s1.modularjobs.com',
      }),
    ).toBe('https://s1.modularjobs.com');
  });

  it('rejects unlisted origin', () => {
    expect(() =>
      validateApiBase('https://evil.com', {
        VITE_ALLOWED_API_ORIGINS: 'https://*.modularjobs.com',
      }),
    ).toThrow(/not allowed/);
  });

  it('rejects non-web schemes even when allow-listed', () => {
    expect(() =>
      validateApiBase('javascript:alert(1)', {
        VITE_ALLOWED_API_ORIGINS: '*',
        VITE_ALLOW_HTTP_API: 'true',
      }),
    ).toThrow(/must use HTTP or HTTPS/);
    expect(() =>
      validateApiBase('file:///etc/passwd', {
        VITE_ALLOWED_API_ORIGINS: '*',
        VITE_ALLOW_HTTP_API: 'true',
      }),
    ).toThrow(/must use HTTP or HTTPS/);
    expect(() =>
      validateApiBase('ws://localhost:18787', {
        VITE_ALLOWED_API_ORIGINS: '*',
        VITE_ALLOW_HTTP_API: 'true',
      }),
    ).toThrow(/must use HTTP or HTTPS/);
    expect(() =>
      validateApiBase('data:text/html;base64,PHNjcmlwdD4=', {
        VITE_ALLOWED_API_ORIGINS: '*',
        VITE_ALLOW_HTTP_API: 'true',
      }),
    ).toThrow(/must use HTTP or HTTPS/);
  });

  it('rejects non-web schemes despite VITE_ALLOW_HTTP_API', () => {
    expect(() =>
      validateApiBase('javascript:alert(1)', {
        VITE_ALLOWED_API_ORIGINS: 'javascript:*',
        VITE_ALLOW_HTTP_API: 'true',
      }),
    ).toThrow(/must use HTTP or HTTPS/);
  });

  it('accepts http with VITE_ALLOW_HTTP_API and matching allow-list', () => {
    expect(
      validateApiBase('http://localhost:18787', {
        VITE_ALLOWED_API_ORIGINS: 'http://localhost:*',
        VITE_ALLOW_HTTP_API: 'true',
      }),
    ).toBe('http://localhost:18787');
  });

  it('rejects ?api= with URL credentials', () => {
    expect(() =>
      validateApiBase('https://user:pass@s1.modularjobs.com', {
        VITE_ALLOWED_API_ORIGINS: 'https://*.modularjobs.com',
      }),
    ).toThrow(/credentials/);
  });

  it('rejects ?api= with a query string', () => {
    expect(() =>
      validateApiBase('https://s1.modularjobs.com?token=steal', {
        VITE_ALLOWED_API_ORIGINS: 'https://*.modularjobs.com',
      }),
    ).toThrow(/query string/);
  });

  it('rejects ?api= with a fragment', () => {
    expect(() =>
      validateApiBase('https://s1.modularjobs.com#secret', {
        VITE_ALLOWED_API_ORIGINS: 'https://*.modularjobs.com',
      }),
    ).toThrow(/fragment/);
  });

  it('rejects credentials/query/fragment even with HTTPS disabled', () => {
    expect(() =>
      validateApiBase('http://user:pass@localhost:18787?x=1#y', {
        VITE_ALLOWED_API_ORIGINS: 'http://localhost:*',
        VITE_ALLOW_HTTP_API: 'true',
      }),
    ).toThrow(/credentials/);
  });

  it('normalizes to origin + path without trailing slash', () => {
    expect(
      validateApiBase('https://s1.modularjobs.com/api/v2/', {
        VITE_ALLOWED_API_ORIGINS: 'https://*.modularjobs.com',
      }),
    ).toBe('https://s1.modularjobs.com/api/v2');
    expect(
      validateApiBase('https://s1.modularjobs.com/', {
        VITE_ALLOWED_API_ORIGINS: 'https://*.modularjobs.com',
      }),
    ).toBe('https://s1.modularjobs.com');
  });
});

describe('originMatchesPattern', () => {
  it('matches exact origin', () => {
    expect(originMatchesPattern('https://s1.modularjobs.com', 'https://s1.modularjobs.com')).toBe(true);
  });

  it('matches subdomain wildcard', () => {
    expect(originMatchesPattern('https://s1-api.modularjobs.com', 'https://*.modularjobs.com')).toBe(true);
  });

  it('matches port wildcard', () => {
    expect(originMatchesPattern('http://localhost:18787', 'http://localhost:*')).toBe(true);
  });

  it('rejects origin outside wildcard', () => {
    expect(originMatchesPattern('https://evil.modularjobs.com.evil.com', 'https://*.modularjobs.com')).toBe(false);
    expect(originMatchesPattern('https://modularjobs.com', 'https://*.modularjobs.com')).toBe(false);
  });

  it('rejects exact mismatch', () => {
    expect(originMatchesPattern('https://s2.modularjobs.com', 'https://s1.modularjobs.com')).toBe(false);
  });
});

describe('setTaskPayableAmount', () => {
  it('updates only the targeted payable amount', () => {
    const next = setTaskPayableAmount(samplePayload(), 'modularjobs:miner', 0, 0, '42.125');
    expect(next.jobs['modularjobs:miner'].tasks[0].payables[0].amount).toBe('42.125');
    expect(next.jobs['modularjobs:miner'].tasks[0].actionTypeKey).toBe(
      'modularjobs:block_break',
    );
  });
});

describe('SessionApiClient', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it('requires session code and token for load', async () => {
    const client = new SessionApiClient('http://127.0.0.1:18787');
    await expect(client.fetchSession('', 'tok')).rejects.toThrow(/code/i);
    await expect(client.fetchSession('abc', '')).rejects.toThrow(/token/i);
  });

  it('requires session code and token for save', async () => {
    const client = new SessionApiClient('http://127.0.0.1:18787');
    const payload = samplePayload();
    await expect(client.saveSession('', 'tok', payload)).rejects.toThrow(/code/i);
    await expect(client.saveSession('abc', '', payload)).rejects.toThrow(/token/i);
  });

  it('load → edit amount → save sends edited amount and uses API base (not bytebin)', async () => {
    const calls: { url: string; init?: RequestInit }[] = [];
    const payload = samplePayload();

    vi.stubGlobal(
      'fetch',
      vi.fn(async (url: string, init?: RequestInit) => {
        calls.push({ url: String(url), init });
        if (String(url).includes('/payload') && (!init || !init.method || init.method === 'GET')) {
          return {
            ok: true,
            status: 200,
            json: async () => payload,
          };
        }
        if (init?.method === 'PUT') {
          const body = JSON.parse(String(init.body));
          return {
            ok: true,
            status: 200,
            json: async () => ({
              code: 'sess1',
              payload: body,
              expiresAt: '2026-08-07T00:00:00Z',
            }),
          };
        }
        return { ok: false, status: 500, json: async () => ({}) };
      }),
    );

    const client = new SessionApiClient('http://rest-api.test:18787');
    const loaded = await client.fetchSession('sess1', 'tok-abc');
    expect(loaded.jobs['modularjobs:miner'].tasks[0].payables[0].amount).toBe('2.5');

    const edited = setTaskPayableAmount(loaded, 'modularjobs:miner', 0, 0, '99.125');
    const saved = await client.saveSession('sess1', 'tok-abc', edited);

    expect(saved.payload.jobs['modularjobs:miner'].tasks[0].payables[0].amount).toBe(
      '99.125',
    );
    expect(calls[0].url).toContain('http://rest-api.test:18787/api/v1/sessions/sess1/payload');
    expect(calls[0].url).not.toContain('bytebin');
    expect(calls[1].url).toBe('http://rest-api.test:18787/api/v1/sessions/sess1');
    expect(calls[1].init?.method).toBe('PUT');

    const saveBody = JSON.parse(String(calls[1].init?.body));
    expect(saveBody.jobs['modularjobs:miner'].tasks[0].payables[0].amount).toBe('99.125');
    expect(saveBody.metadata.sessionToken).toBe('tok-abc');

    const auth = (calls[1].init?.headers as Record<string, string>).Authorization;
    expect(auth).toBe('Bearer tok-abc');
  });

  it('rejects wrong-token responses from API', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(async () => ({
        ok: false,
        status: 401,
        json: async () => ({ error: 'invalid session token' }),
      })),
    );
    const client = new SessionApiClient('http://rest-api.test:18787');
    await expect(client.fetchSession('x', 'bad')).rejects.toThrow(/token/i);
  });
});