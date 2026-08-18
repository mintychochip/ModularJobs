import { afterAll, afterEach, beforeAll, beforeEach, describe, expect, it, vi } from 'vitest';
import { act, type ComponentProps } from 'react';
import { createRoot, type Root } from 'react-dom/client';
import { SessionApiClient, setTaskPayableAmount } from './apiClient';
import { SessionEditor } from './SessionEditor';
import type { EditorPayload } from './types';

const SAMPLE_PAYLOAD: EditorPayload = {
  version: 1,
  metadata: {
    exportedAt: '2026-08-06T00:00:00Z',
    exportedBy: 'u',
    sessionToken: 'sec',
    serverName: null,
  },
  jobs: {
    'modularjobs:fisherman': {
      displayName: 'Fisherman',
      tasks: [
        {
          actionTypeKey: 'modularjobs:fish',
          contextKey: 'minecraft:cod',
          payables: [{ type: 'modularjobs:economy', amount: '1.0' }],
        },
      ],
    },
  },
  registeredActionTypes: ['modularjobs:fish'],
  registeredPayableTypes: ['modularjobs:economy'],
};

// Allow the local HTTP test origin so manual AuthCard submit can pass
// validateApiBase without needing HTTPS + a production allow-list.
const TEST_ENV: Record<string, string | undefined> = {
  VITE_ALLOW_HTTP_API: 'true',
  VITE_ALLOWED_API_ORIGINS: 'http://localhost:18787',
};

function mount(props: Partial<ComponentProps<typeof SessionEditor>>) {
  const container = document.createElement('div');
  document.body.appendChild(container);
  let root!: Root;
  act(() => {
    root = createRoot(container);
    root.render(<SessionEditor {...props} />);
  });

  const input = (id: string) => document.getElementById(id) as HTMLInputElement;
  const setValue = (el: HTMLInputElement, value: string) => {
    const setter = Object.getOwnPropertyDescriptor(
      window.HTMLInputElement.prototype,
      'value',
    )!.set!;
    act(() => {
      setter.call(el, value);
      el.dispatchEvent(new Event('input', { bubbles: true }));
    });
  };

  const flush = async () => {
    await act(async () => {
      await Promise.resolve();
    });
  };

  return { container, input, setValue, flush };
}

beforeAll(() => {
  (globalThis as Record<string, unknown>).IS_REACT_ACT_ENVIRONMENT = true;
});

afterAll(() => {
  (globalThis as Record<string, unknown>).IS_REACT_ACT_ENVIRONMENT = false;
});

beforeEach(() => {
  for (const [key, value] of Object.entries(TEST_ENV)) {
    vi.stubEnv(key, value);
  }
});

afterEach(() => {
  vi.unstubAllEnvs();
  vi.restoreAllMocks();
  document.body.innerHTML = '';
});

describe('SessionEditor authentication', () => {
  it('renders AuthCard, not the dashboard, before credentials are valid', () => {
    const h = mount({});
    expect(h.input('session-code')).toBeTruthy();
    expect(h.input('session-token')).toBeTruthy();
    expect(h.container.querySelector('table')).toBeNull();
    expect(h.container.textContent ?? '').not.toContain('Save changes');
  });

  it('does not render injected secrets in the DOM after loading', async () => {
    const client = new SessionApiClient('http://localhost:18787');
    vi.spyOn(client, 'fetchSession').mockResolvedValue(SAMPLE_PAYLOAD);
    const h = mount({ client, initialCode: 'CODE-1', initialToken: 'super-secret-token' });
    await h.flush();

    expect(h.container.textContent ?? '').not.toContain('super-secret-token');
  });

  it('stays logged out when logout happens during a pending load', async () => {
    const client = new SessionApiClient('http://localhost:18787');
    let resolveFetch!: (value: EditorPayload) => void;
    vi.spyOn(client, 'fetchSession').mockReturnValue(
      new Promise((resolve) => {
        resolveFetch = resolve;
      }),
    );
    const h = mount({ client, initialCode: 'CODE-1', initialToken: 'tok' });

    // Auto-load started and is still in flight (dashboard loading state shown).
    const logoutBtn = Array.from(h.container.querySelectorAll('button')).find((b) =>
      b.textContent?.includes('Log out'),
    );
    expect(logoutBtn).toBeTruthy();

    act(() => {
      logoutBtn!.click();
    });
    expect(h.input('session-code')).toBeTruthy(); // AuthCard shown after logout

    await act(async () => {
      resolveFetch(SAMPLE_PAYLOAD);
      await Promise.resolve();
    });
    await h.flush();

    // A late response must not restore the authenticated dashboard.
    expect(h.input('session-code')).toBeTruthy();
    expect(h.container.textContent ?? '').not.toContain('Fisherman');
    expect(client.fetchSession).toHaveBeenCalledTimes(1);
  });
});

describe('SessionEditor loaded dashboard', () => {
  it('auto-loads with injected client and shows jobs, tasks and payable editing', async () => {
    const client = new SessionApiClient('http://localhost:18787');
    vi.spyOn(client, 'fetchSession').mockResolvedValue(SAMPLE_PAYLOAD);
    const h = mount({ client, initialCode: 'CODE-1', initialToken: 'tok' });
    await h.flush();

    // Auto-load from deep-link credentials.
    expect(client.fetchSession).toHaveBeenCalledWith('CODE-1', 'tok');

    const text = () => h.container.textContent ?? '';
    expect(text()).toContain('Fisherman');
    expect((h.container.querySelector('[aria-label="Task 1 context"]') as HTMLInputElement).value).toBe('minecraft:cod');
    expect(text()).toContain('economy');

    // Editable payable amount state reflects an edit.
    const amount = h.container.querySelector('[data-testid="payable-amount-0-0"]') as HTMLInputElement;
    expect(amount.value).toBe('1.0');
    h.setValue(amount, '42.0');
    expect((h.container.querySelector('[data-testid="payable-amount-0-0"]') as HTMLInputElement).value).toBe('42.0');
  });

  it('loads on manual AuthCard submit and exposes save', async () => {
    const client = new SessionApiClient('http://localhost:18787');
    vi.spyOn(client, 'fetchSession').mockResolvedValue(SAMPLE_PAYLOAD);
    vi.spyOn(client, 'saveSession').mockResolvedValue({
      code: 'X',
      expiresAt: '2026-08-08T00:00:00Z',
      payload: SAMPLE_PAYLOAD,
    });
    const h = mount({ client });

    expect(h.input('session-code')).toBeTruthy();
    h.setValue(h.input('session-code'), 'CODE-9');
    h.setValue(h.input('session-token'), 'tok-9');
    h.setValue(h.input('api-base'), 'http://localhost:18787');
    act(() => {
      document
        .getElementById('auth-card-form')!
        .dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
    });
    await h.flush();

    expect(client.fetchSession).toHaveBeenCalledWith('CODE-9', 'tok-9');
    expect(h.container.querySelector('button')!.textContent).not.toBe('Load session');
    const saveBtn = Array.from(h.container.querySelectorAll('button')).find((b) =>
      b.textContent?.startsWith('Save'),
    );
    expect(saveBtn).toBeTruthy();
    expect(h.container.textContent ?? '').toContain('Fisherman');
  });
});

describe('SessionEditor edit pipeline + export surface', () => {
  it('module exports the React component as default and named', async () => {
    const mod = await import('./SessionEditor');
    expect(typeof mod.SessionEditor).toBe('function');
    expect(typeof mod.default).toBe('function');
  });

  it('setTaskPayableAmount keeps sessionToken and applies payable edits', () => {
    const edited = setTaskPayableAmount(SAMPLE_PAYLOAD, 'modularjobs:fisherman', 0, 0, '12.50');
    expect(edited.metadata.sessionToken).toBe('sec');
    expect(edited.jobs['modularjobs:fisherman'].tasks[0].payables[0].amount).toBe('12.50');
  });
});
