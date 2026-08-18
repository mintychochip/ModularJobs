import { describe, expect, it, vi, afterEach } from 'vitest';
import { act } from 'react';
import { createRoot, type Root } from 'react-dom/client';
import type { ReactNode } from 'react';
import { useUrlCredentials } from './useUrlCredentials';

/**
 * Minimal renderHook equivalent using react-dom (no @testing-library/react
 * dependency). Renders a real component tree through `act` so the hook runs
 * exactly as it would inside the editor.
 */
function renderHook<T>(use: () => T): { result: { current: T }; unmount: () => void } {
  const result = { current: undefined as unknown as T };
  const container = document.createElement('div');
  document.body.appendChild(container);
  let root: Root | undefined;

  function Harness(): ReactNode {
    result.current = use();
    return null;
  }

  act(() => {
    root = createRoot(container);
    root.render(<Harness />);
  });

  return {
    result,
    unmount() {
      act(() => root?.unmount());
      document.body.removeChild(container);
    },
  };
}

/** Build a minimal fake Window exposing just the location fields the hook reads. */
function fakeWindow(search: string, hash: string): Window {
  return { location: { search, hash } } as unknown as Window;
}

const env = (overrides: Record<string, string | undefined> = {}) => overrides;

afterEach(() => {
  vi.restoreAllMocks();
});

describe('useUrlCredentials', () => {
  it('reads code and token from the URL hash', () => {
    const win = fakeWindow('?code=queryCode', '#code=hashCode&token=hashToken');
    const { result, unmount } = renderHook(() => useUrlCredentials(env(), win));
    unmount();
    expect(result.current.code).toBe('hashCode');
    expect(result.current.token).toBe('hashToken');
  });

  it('preserves the raw ?api= query value without validation or rewriting', () => {
    // Plain HTTP origin must pass through untouched — this hook is parsing-only
    // and must NOT invoke validateApiBase (which would reject it in prod).
    const win = fakeWindow('?api=http://127.0.0.1:18787/sub/path&code=c1', '');
    const { result, unmount } = renderHook(() => useUrlCredentials(env(), win));
    unmount();
    expect(result.current.api).toBe('http://127.0.0.1:18787/sub/path');
    expect(result.current.code).toBe('c1');
  });

  it('falls back to VITE_SESSION_API_URL then VITE_API_BASE_URL when api is absent', () => {
    const both = fakeWindow('?code=c1', '');
    const r1 = renderHook(() =>
      useUrlCredentials(
        env({
          VITE_SESSION_API_URL: 'https://session.example',
          VITE_API_BASE_URL: 'https://base.example',
        }),
        both,
      ),
    );
    r1.unmount();
    expect(r1.result.current.api).toBe('https://session.example');

    const baseOnly = fakeWindow('', '');
    const r2 = renderHook(() =>
      useUrlCredentials(env({ VITE_API_BASE_URL: 'https://base.example' }), baseOnly),
    );
    r2.unmount();
    expect(r2.result.current.api).toBe('https://base.example');
  });

  it("returns empty api when neither query param nor env fallback is present", () => {
    const win = fakeWindow('?code=c1', '');
    const { result, unmount } = renderHook(() => useUrlCredentials(env(), win));
    unmount();
    expect(result.current.api).toBe('');
  });

  it('never throws for a missing api query param or missing fallback', () => {
    const win = fakeWindow('', '#token=onlyToken');
    const { result, unmount } = renderHook(() => useUrlCredentials(env(), win));
    unmount();
    expect(result.current.token).toBe('onlyToken');
    expect(result.current.api).toBe('');
  });

  it('scrubs a legacy query token from the window', () => {
    const spy = vi
      .spyOn(window.history, 'replaceState')
      .mockImplementation(() => {});

    Object.defineProperty(window, 'location', {
      configurable: true,
      value: {
        ...window.location,
        search: '?code=abc&token=leaked',
        hash: '',
        pathname: '/session-editor/',
      },
    });

    // Pass jsdom's (already mutated) window so parsing and scrubbing agree.
    const { result, unmount } = renderHook(() => useUrlCredentials(env(), window));
    unmount();

    expect(spy).toHaveBeenCalled();
    const next = spy.mock.calls[0][2] as string;
    expect(next).toContain('code=abc');
    expect(next).toMatch(/#.*token=leaked/);
    expect(next.split('#')[0]).not.toContain('token=');
    expect(result.current.token).toBe('leaked');
    expect(result.current.api).toBe('');
  });
});
