export const GITHUB_OWNER = 'aincraft-org';
export const GITHUB_NAME = 'modularjobs';
export const GITHUB_URL = `https://github.com/${GITHUB_OWNER}/${GITHUB_NAME}`;
export const RELEASES_URL = `${GITHUB_URL}/releases`;
export const LATEST_RELEASE_URL = `${RELEASES_URL}/latest`;
export const LATEST_RELEASE_API = `https://api.github.com/repos/${GITHUB_OWNER}/${GITHUB_NAME}/releases/latest`;

/** How long a fetched release stays fresh before the page re-checks GitHub. */
export const REFRESH_INTERVAL_MS = 5 * 60 * 1000;

export interface ReleaseAsset {
  name: string;
  url: string;
  size: number;
  downloads: number;
}

export interface Release {
  name: string;
  tag: string;
  version: string;
  url: string;
  publishedAt: string;
  notes: string;
  assets: ReleaseAsset[];
  /** Paper server jar, when the release publishes one. */
  primary: ReleaseAsset | null;
}

function asAssets(raw: unknown): ReleaseAsset[] {
  if (!Array.isArray(raw)) return [];

  return raw.flatMap((entry) => {
    const asset = entry as Record<string, unknown>;
    const name = typeof asset.name === 'string' ? asset.name : '';
    const url =
      typeof asset.browser_download_url === 'string'
        ? asset.browser_download_url
        : '';
    if (!name || !url) return [];

    return [
      {
        name,
        url,
        size: typeof asset.size === 'number' ? asset.size : 0,
        downloads:
          typeof asset.download_count === 'number' ? asset.download_count : 0,
      },
    ];
  });
}

export function normalizeRelease(raw: unknown): Release | null {
  if (!raw || typeof raw !== 'object') return null;

  const release = raw as Record<string, unknown>;
  const tag = typeof release.tag_name === 'string' ? release.tag_name : '';
  if (!tag) return null;

  const assets = asAssets(release.assets);
  const version = tag.replace(/^v/i, '');
  const jars = assets.filter((asset) => asset.name.endsWith('.jar'));

  return {
    name: typeof release.name === 'string' && release.name ? release.name : tag,
    tag,
    version,
    url: typeof release.html_url === 'string' ? release.html_url : RELEASES_URL,
    publishedAt:
      typeof release.published_at === 'string' ? release.published_at : '',
    notes: toPlainNotes(typeof release.body === 'string' ? release.body : ''),
    assets,
    primary: jars.find((asset) => asset.name.includes('paper')) ?? jars[0] ?? null,
  };
}

export async function fetchLatestRelease(
  init?: RequestInit,
): Promise<Release | null> {
  try {
    const response = await fetch(LATEST_RELEASE_API, {
      ...init,
      headers: {
        Accept: 'application/vnd.github+json',
        ...(init?.headers ?? {}),
      },
    });
    if (!response.ok) return null;
    return normalizeRelease(await response.json());
  } catch {
    return null;
  }
}

/** Markdown release notes flattened to a short plain-text summary. */
export function toPlainNotes(body: string, limit = 320): string {
  const text = body
    .replace(/\r/g, '')
    .replace(/!\[[^\]]*]\([^)]*\)/g, '')
    .replace(/\[([^\]]+)]\([^)]*\)/g, '$1')
    .replace(/^#{1,6}\s*/gm, '')
    .replace(/\*\*([^*]+)\*\*/g, '$1')
    .replace(/https?:\/\/\S+/g, '')
    .split('\n')
    .map((line) =>
      line
        .replace(/^\s*[-*]\s+/, '• ')
        .replace(/\s+/g, ' ')
        .trim()
        // GitHub's generated entries read "<title> by @user in <url>"; the URL is gone.
        .replace(/\s+(in|by|at|to|from|for):?$/i, ''),
    )
    .filter((line) => line && !line.endsWith(':'))
    .join('\n');

  if (text.length <= limit) return text;
  return `${text.slice(0, limit).trimEnd()}…`;
}

export function formatBytes(bytes: number): string {
  if (!bytes || bytes < 0) return '';
  if (bytes < 1024) return `${bytes} B`;

  const units = ['KB', 'MB', 'GB'];
  let value = bytes / 1024;
  let unit = 0;
  while (value >= 1024 && unit < units.length - 1) {
    value /= 1024;
    unit += 1;
  }

  return `${value < 10 ? value.toFixed(1) : Math.round(value)} ${units[unit]}`;
}

export function formatDate(iso: string): string {
  if (!iso) return '';
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) return '';

  return date.toLocaleDateString('en-GB', {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
  });
}

export function formatAssetDetail(asset: ReleaseAsset): string {
  const size = formatBytes(asset.size);
  const downloads =
    asset.downloads > 0
      ? `${asset.downloads.toLocaleString('en-GB')} download${asset.downloads === 1 ? '' : 's'}`
      : '';

  return [size, downloads].filter(Boolean).join(' · ');
}

export function formatRelativeTime(from: number, now = Date.now()): string {
  const seconds = Math.max(0, Math.round((now - from) / 1000));
  if (seconds < 45) return 'just now';

  const minutes = Math.round(seconds / 60);
  if (minutes < 60) return `${minutes} min ago`;

  const hours = Math.round(minutes / 60);
  if (hours < 24) return `${hours} hr ago`;

  const days = Math.round(hours / 24);
  return `${days} day${days === 1 ? '' : 's'} ago`;
}
