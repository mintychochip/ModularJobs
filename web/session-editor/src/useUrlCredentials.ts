import { readSessionCredentials, scrubTokenFromQuery } from './sessionCredentials';

export type UrlCredentials = {
  code: string;
  token: string;
  api: string;
};

export function useUrlCredentials(
  env: Record<string, string | undefined>,
  win: Window = window,
): UrlCredentials {
  const credentials = readSessionCredentials(win.location.search, win.location.hash);
  scrubTokenFromQuery(win);

  const query = win.location.search.startsWith('?')
    ? win.location.search.slice(1)
    : win.location.search;
  const api = new URLSearchParams(query).get('api');

  return {
    code: credentials.code,
    token: credentials.token,
    api: api ?? env.VITE_SESSION_API_URL ?? env.VITE_API_BASE_URL ?? '',
  };
}
