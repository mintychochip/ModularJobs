import { afterAll, afterEach, beforeAll, describe, expect, it, vi } from 'vitest';
import { act, type ComponentProps } from 'react';
import { createRoot, type Root } from 'react-dom/client';
import { AuthCard } from './AuthCard';

/**
 * Deterministic env: only the exact local HTTPS origin is allowed.
 * Mirrors a hardened production allow-list so tests exercise origin security.
 */
const LOCAL_ENV: Record<string, string | undefined> = {
  VITE_ALLOWED_API_ORIGINS: 'https://localhost:18787',
};

function mount(
  onSubmit: (code: string, token: string, apiBase: string) => void,
  extra: Partial<ComponentProps<typeof AuthCard>> = {},
) {
  const container = document.createElement('div');
  document.body.appendChild(container);
  let root!: Root;
  act(() => {
    root = createRoot(container);
    root.render(<AuthCard env={LOCAL_ENV} onSubmit={onSubmit} {...extra} />);
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

  const submit = () => {
    act(() => {
      document
        .getElementById('auth-card-form')!
        .dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
    });
  };

  return { container, input, setValue, submit };
}

beforeAll(() => {
  // jsdom is not an automatic React act environment; enable act tracking so the
  // harness warns (and behaves) like a React test renderer.
  (globalThis as Record<string, unknown>).IS_REACT_ACT_ENVIRONMENT = true;
});

afterAll(() => {
  (globalThis as Record<string, unknown>).IS_REACT_ACT_ENVIRONMENT = false;
});

afterEach(() => {
  document.body.innerHTML = '';
});

describe('AuthCard', () => {
  it('marks all three credential fields as required', () => {
    const onSubmit = vi.fn();
    const h = mount(onSubmit);

    for (const id of ['session-code', 'session-token', 'api-base']) {
      expect(h.input(id).required).toBe(true);
    }
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it('renders exact labels and button text', () => {
    const h = mount(() => {});
    const text = h.container.textContent ?? '';
    expect(text).toContain('Session code');
    expect(text).toContain('Session token');
    expect(text).toContain('API base');
    expect(h.container.querySelector('button')!.textContent).toBe('Load session');
  });

  it('submits code, token and exact allowed local apiBase', () => {
    const onSubmit = vi.fn();
    const h = mount(onSubmit, {
      initialCode: 'CODE-123',
      initialToken: 'secret-token',
      initialApi: 'https://localhost:18787',
    });

    h.submit();

    expect(onSubmit).toHaveBeenCalledTimes(1);
    expect(onSubmit).toHaveBeenCalledWith(
      'CODE-123',
      'secret-token',
      'https://localhost:18787',
    );
    expect(h.container.querySelector('[role="alert"]')).toBeNull();
  });

  it('rejects an evil API origin with a visible alert and never submits', () => {
    const onSubmit = vi.fn();
    const h = mount(onSubmit, {
      initialCode: 'CODE-123',
      initialToken: 'secret-token',
      initialApi: 'https://localhost:18787',
    });

    h.setValue(h.input('api-base'), 'https://evil.example.com');
    h.submit();

    expect(onSubmit).not.toHaveBeenCalled();
    const alert = h.container.querySelector('[role="alert"]');
    expect(alert).not.toBeNull();
    expect(alert!.textContent).toMatch(/not allowed/i);
  });
});