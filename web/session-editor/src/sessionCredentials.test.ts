import { describe, expect, it, vi, beforeEach, afterEach } from 'vitest';
import {
  readSessionCredentials,
  scrubTokenFromQuery,
  sessionEditorPath,
} from './sessionCredentials';

describe('readSessionCredentials', () => {
  it('prefers hash token over query token', () => {
    const r = readSessionCredentials('?code=abc&token=queryTok', '#token=hashTok');
    expect(r.code).toBe('abc');
    expect(r.token).toBe('hashTok');
    expect(r.tokenFromQuery).toBe(false);
  });

  it('accepts legacy query token', () => {
    const r = readSessionCredentials('?code=c1&token=secret', '');
    expect(r.code).toBe('c1');
    expect(r.token).toBe('secret');
    expect(r.tokenFromQuery).toBe(true);
  });

  it('reads code from hash when present', () => {
    const r = readSessionCredentials('', '#code=hcode&token=htok');
    expect(r.code).toBe('hcode');
    expect(r.token).toBe('htok');
  });
});

describe('sessionEditorPath', () => {
  it('puts code in query and token only in hash', () => {
    const path = sessionEditorPath('mycode', 'mytoken');
    expect(path).toContain('code=mycode');
    expect(path).toContain('#');
    expect(path).toMatch(/#.*token=mytoken/);
    // token must not appear before the hash
    const beforeHash = path.split('#')[0];
    expect(beforeHash).not.toContain('mytoken');
    expect(beforeHash).not.toContain('token=');
  });
  it('encodes reserved characters without leaking token into query', () => {
    const path = sessionEditorPath('code/1', 'token?1');
    const [query, fragment] = path.split('#');
    expect(query).toContain('code=code%2F1');
    expect(query).not.toContain('token');
    expect(fragment).toBe('token=token%3F1');
  });

});

describe('scrubTokenFromQuery', () => {

  beforeEach(() => {
    vi.spyOn(window.history, 'replaceState').mockImplementation(() => {});
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('moves query token into hash via replaceState', () => {
    // jsdom location is limited; mock read path by setting via history if possible
    Object.defineProperty(window, 'location', {
      configurable: true,
      value: {
        ...window.location,
        search: '?code=abc&token=leaked',
        hash: '',
        pathname: '/session-editor/',
      },
    });

    scrubTokenFromQuery();

    expect(window.history.replaceState).toHaveBeenCalled();
    const next = (window.history.replaceState as ReturnType<typeof vi.fn>).mock
      .calls[0][2] as string;
    expect(next).toContain('code=abc');
    expect(next).toMatch(/#.*token=leaked/);
    expect(next.split('#')[0]).not.toContain('token=');
  });
});
