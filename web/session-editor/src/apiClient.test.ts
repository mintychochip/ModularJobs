import { describe, expect, it, vi, beforeEach } from 'vitest';
import {
  SessionApiClient,
  resolveApiBaseUrl,
  setTaskPayableAmount,
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
