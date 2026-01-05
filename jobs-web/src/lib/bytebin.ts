import type { EditorPayload } from './types';

const BYTEBIN_URL = 'https://bytebin.lucko.me';

export async function fetchSession(code: string): Promise<EditorPayload> {
  const response = await fetch(`${BYTEBIN_URL}/${code}`);
  if (!response.ok) {
    if (response.status === 404) {
      throw new Error('Session not found or expired');
    }
    throw new Error(`Failed to fetch session: ${response.status}`);
  }
  return response.json();
}

export async function saveSession(payload: EditorPayload): Promise<string> {
  const response = await fetch(`${BYTEBIN_URL}/post`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  });
  if (!response.ok) {
    throw new Error(`Failed to save session: ${response.status}`);
  }
  const data = await response.json();
  return data.key;
}
