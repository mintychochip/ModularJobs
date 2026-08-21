# React Editor + Plugin Homepage Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the Astro `/editor` pages with a single React dashboard editor and redesign the Astro plugin homepage in the same visual system.

**Architecture:** The `web/session-editor` Vite React app becomes the full editor experience and is built into `public/editor` → `dist/editor`. It auto-loads from URL credentials or shows an auth card. The Astro `index.astro` and supporting components are rewritten with Tailwind v4 + daisyUI dark theme. Paper's `EditorService` drops the `/session` path from generated URLs.

**Tech Stack:** Vite 6, React 19, TypeScript, Tailwind CSS v4, daisyUI, Astro, Starlight, Paper Java, Gradle.

## Global Constraints

- Editor must remain fail-closed: `VITE_ALLOWED_API_ORIGINS` required for per-server `?api=` origins.
- Token stays in URL hash; never in query or Referer.
- Token is intentionally sent in `Authorization: Bearer` and `X-Session-Token` headers to the validated API.
- Paper creates sessions; the browser editor only loads and saves.
- `VITE_ALLOW_HTTP_API=true` is for local smoke testing only, never production.
- `npm test` in `web/session-editor` must pass; `./gradlew :paper:compileJava` must pass; `cargo test --lib` in `web/rest-api` must pass.

---

### Task 1: Configure Tailwind v4 + daisyUI in `web/session-editor`

**Files:**
- Modify: `web/session-editor/package.json`
- Modify: `web/session-editor/vite.config.ts`
- Create: `web/session-editor/src/index.css`
- Modify: `web/session-editor/src/main.tsx`

**Interfaces:**
- Produces: `index.css` with `@import "tailwindcss"` and daisyui config.

- [ ] **Step 1: Add Tailwind and daisyUI dependencies**

```bash
cd web/session-editor
npm install -D tailwindcss@^4.1.11 @tailwindcss/vite@^4.1.11 daisyui@^5.0.50
```

- [ ] **Step 2: Update `vite.config.ts`**

```ts
/// <reference types="vitest/config" />
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import tailwindcss from '@tailwindcss/vite';

export default defineConfig({
  plugins: [react(), tailwindcss()],
  base: './',
  server: {
    port: 5174,
  },
  build: {
    outDir: 'dist',
    emptyOutDir: true,
  },
  test: {
    environment: 'jsdom',
    globals: true,
  },
});
```

- [ ] **Step 3: Create `src/index.css`**

```css
@import "tailwindcss";

@plugin "daisyui" {
  themes: dark --default, light;
}

:root {
  color-scheme: dark;
}

body {
  @apply bg-base-100 text-base-content;
}
```

- [ ] **Step 4: Import `index.css` in `src/main.tsx`**

```tsx
import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { SessionEditor } from './SessionEditor';
import './index.css';

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <SessionEditor />
  </StrictMode>,
);
```

- [ ] **Step 5: Build and verify no regressions**

```bash
npm test
npm run build
```

Expected: `dist/assets/index-*.css` includes Tailwind; tests pass.

---

### Task 2: Add `AuthCard` component with manual API base entry

**Files:**
- Create: `web/session-editor/src/AuthCard.tsx`
- Create: `web/session-editor/src/AuthCard.test.tsx`

**Interfaces:**
- Consumes: `validateApiBase` from `apiClient`.
- Produces: `AuthCard` component with `onSubmit(code, token, apiBase)` callback.

- [ ] **Step 1: Install `@testing-library/react`**

```bash
cd web/session-editor
npm install -D @testing-library/react @testing-library/dom
```

- [ ] **Step 2: Write failing test for `AuthCard`**

```tsx
import { describe, expect, it, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { AuthCard } from './AuthCard';

describe('AuthCard', () => {
  it('submits code, token, and api base when allowed', () => {
    const onSubmit = vi.fn();
    render(
      <AuthCard
        onSubmit={onSubmit}
        env={{
          VITE_ALLOWED_API_ORIGINS: 'http://localhost:*',
          VITE_ALLOW_HTTP_API: 'true',
        }}
      />,
    );

    fireEvent.change(screen.getByLabelText(/session code/i), { target: { value: 'ABC' } });
    fireEvent.change(screen.getByLabelText(/session token/i), { target: { value: 'SECRET' } });
    fireEvent.change(screen.getByLabelText(/api base/i), { target: { value: 'http://localhost:9999' } });
    fireEvent.click(screen.getByRole('button', { name: /load session/i }));

    expect(onSubmit).toHaveBeenCalledWith('ABC', 'SECRET', 'http://localhost:9999');
  });

  it('rejects an api base outside the allow-list', () => {
    const onSubmit = vi.fn();
    render(
      <AuthCard
        onSubmit={onSubmit}
        env={{ VITE_ALLOWED_API_ORIGINS: 'http://localhost:*' }}
      />,
    );

    fireEvent.change(screen.getByLabelText(/session code/i), { target: { value: 'ABC' } });
    fireEvent.change(screen.getByLabelText(/session token/i), { target: { value: 'SECRET' } });
    fireEvent.change(screen.getByLabelText(/api base/i), { target: { value: 'http://evil.com' } });
    fireEvent.click(screen.getByRole('button', { name: /load session/i }));

    expect(onSubmit).not.toHaveBeenCalled();
    expect(screen.getByRole('alert').textContent).toMatch(/not allowed/);
  });
});
```

- [ ] **Step 3: Implement `AuthCard.tsx`**

```tsx
import { useState } from 'react';
import { validateApiBase } from './apiClient';

export interface AuthCardProps {
  onSubmit: (code: string, token: string, apiBase: string) => void;
  env?: Record<string, string | undefined>;
  initialCode?: string;
  initialToken?: string;
  initialApi?: string;
  error?: string | null;
}

export function AuthCard({
  onSubmit,
  env = import.meta.env as Record<string, string | undefined>,
  initialCode = '',
  initialToken = '',
  initialApi = env.VITE_SESSION_API_URL || env.VITE_API_BASE_URL || '',
  error: externalError,
}: AuthCardProps) {
  const [code, setCode] = useState(initialCode);
  const [token, setToken] = useState(initialToken);
  const [apiBase, setApiBase] = useState(initialApi);
  const [error, setError] = useState<string | null>(externalError ?? null);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    try {
      const trimmedCode = code.trim();
      const trimmedToken = token.trim();
      const trimmedApi = apiBase.trim();
      if (!trimmedCode || !trimmedToken || !trimmedApi) {
        throw new Error('Code, token, and API base are required');
      }
      validateApiBase(trimmedApi, env);
      onSubmit(trimmedCode, trimmedToken, trimmedApi);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Invalid input');
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center p-4">
      <div className="card bg-base-200 shadow-xl w-full max-w-md p-6 space-y-4">
        <h1 className="text-2xl font-bold">ModularJobs Editor</h1>
        <p className="text-sm text-base-content/70">
          Enter your session credentials from <code className="kbd kbd-sm">/jobs editor</code>.
        </p>
        {(error || externalError) && (
          <div className="alert alert-error text-sm" role="alert">{error || externalError}</div>
        )}
        <form onSubmit={handleSubmit} className="space-y-3">
          <label className="form-control w-full">
            <span className="label-text">Session code</span>
            <input
              type="text"
              className="input input-bordered w-full"
              value={code}
              onChange={(e) => setCode(e.target.value)}
              autoComplete="off"
            />
          </label>
          <label className="form-control w-full">
            <span className="label-text">Session token</span>
            <input
              type="password"
              className="input input-bordered w-full"
              value={token}
              onChange={(e) => setToken(e.target.value)}
              autoComplete="off"
            />
          </label>
          <label className="form-control w-full">
            <span className="label-text">API base</span>
            <input
              type="url"
              className="input input-bordered w-full"
              value={apiBase}
              onChange={(e) => setApiBase(e.target.value)}
              placeholder="https://s1-api.modularjobs.com"
            />
          </label>
          <button type="submit" className="btn btn-primary w-full">
            Load session
          </button>
        </form>
      </div>
    </div>
  );
}
```

- [ ] **Step 4: Run tests**

```bash
npm test
```

Expected: `AuthCard` tests pass.

---

### Task 3: Add `useUrlCredentials` hook and safe client creation

**Files:**
- Create: `web/session-editor/src/useUrlCredentials.ts`
- Create: `web/session-editor/src/useUrlCredentials.test.ts`

**Interfaces:**
- Consumes: `readSessionCredentials`, `scrubTokenFromQuery`.
- Produces: `useUrlCredentials` returns `{ code, token, api }` without validating the API base.

- [ ] **Step 1: Write failing test**

```ts
import { describe, expect, it } from 'vitest';
import { renderHook } from '@testing-library/react';
import { useUrlCredentials } from './useUrlCredentials';

const makeWindow = (search: string, hash: string) =>
  ({
    location: { search, hash },
    history: { replaceState: () => {} },
  }) as unknown as Window;

describe('useUrlCredentials', () => {
  it('reads code, token, and api from the URL without throwing', () => {
    const { result } = renderHook(() =>
      useUrlCredentials(
        { VITE_SESSION_API_URL: 'https://fallback.example.com' },
        makeWindow('?api=https://s1.modularjobs.com&code=ABC', '#token=SECRET'),
      ),
    );
    expect(result.current).toEqual({
      code: 'ABC',
      token: 'SECRET',
      api: 'https://s1.modularjobs.com',
    });
  });

  it('falls back to VITE_SESSION_API_URL', () => {
    const { result } = renderHook(() =>
      useUrlCredentials(
        { VITE_SESSION_API_URL: 'https://fallback.example.com' },
        makeWindow('?code=ABC', '#token=SECRET'),
      ),
    );
    expect(result.current.api).toBe('https://fallback.example.com');
  });
});
```

- [ ] **Step 2: Install `@testing-library/react-hooks` or use renderHook from `@testing-library/react`**

`renderHook` is available in `@testing-library/react` v13+. It is installed in Task 2.

- [ ] **Step 3: Implement `useUrlCredentials.ts`**

```ts
import { useMemo } from 'react';
import { readSessionCredentials, scrubTokenFromQuery } from './sessionCredentials';

export interface UseUrlCredentialsOptions {
  window?: Window;
  env?: Record<string, string | undefined>;
}

export function useUrlCredentials(
  env: Record<string, string | undefined>,
  win: Window | undefined = typeof window !== 'undefined' ? window : undefined,
): { code: string; token: string; api: string } {
  return useMemo(() => {
    if (!win) {
      return { code: '', token: '', api: '' };
    }
    const { code, token } = readSessionCredentials(win.location.search, win.location.hash);
    scrubTokenFromQuery();
    const params = new URLSearchParams(
      win.location.search.startsWith('?') ? win.location.search.slice(1) : win.location.search,
    );
    const api = params.get('api')?.trim() || env.VITE_SESSION_API_URL || env.VITE_API_BASE_URL || '';
    return { code, token, api };
  }, [win, env.VITE_SESSION_API_URL, env.VITE_API_BASE_URL]);
}
```

- [ ] **Step 4: Run tests**

```bash
npm test
```

Expected: `useUrlCredentials` tests pass.

---

### Task 4: Build the `EditorDashboard` and `SessionEditor` shell

**Files:**
- Create: `web/session-editor/src/EditorDashboard.tsx`
- Create: `web/session-editor/src/PayableRow.tsx`
- Modify: `web/session-editor/src/SessionEditor.tsx`
- Create: `web/session-editor/src/SessionEditor.test.tsx`
- Delete: `web/session-editor/src/SessionEditor.css`

**Interfaces:**
- Consumes: `AuthCard`, `useUrlCredentials`, `SessionApiClient`, `createDefaultClient`, `setTaskPayableAmount`, `validateApiBase`.
- Produces: `SessionEditor` that shows `AuthCard` without credentials or `EditorDashboard` with credentials.

- [ ] **Step 1: Write failing `SessionEditor` test**

```tsx
import { describe, expect, it, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { SessionEditor } from './SessionEditor';

beforeEach(() => {
  vi.restoreAllMocks();
});

describe('SessionEditor', () => {
  it('shows auth card without credentials', () => {
    vi.stubGlobal('window', undefined);
    render(<SessionEditor />);
    expect(screen.getByText(/modularjobs editor/i)).toBeInTheDocument();
  });
});
```

- [ ] **Step 2: Implement `PayableRow.tsx`**

```tsx
import type { PayableData } from './types';

interface PayableRowProps {
  payable: PayableData;
  taskIndex: number;
  payableIndex: number;
  onChange: (taskIndex: number, payableIndex: number, amount: string) => void;
}

export function PayableRow({ payable, taskIndex, payableIndex, onChange }: PayableRowProps) {
  return (
    <label className="flex items-center gap-2 text-sm">
      <span className="badge badge-sm badge-neutral">{payable.type}</span>
      <input
        type="text"
        className="input input-xs input-bordered w-24"
        value={payable.amount}
        onChange={(e) => onChange(taskIndex, payableIndex, e.target.value)}
      />
    </label>
  );
}
```

- [ ] **Step 3: Implement `EditorDashboard.tsx`**

```tsx
import { useCallback, useState } from 'react';
import { PayableRow } from './PayableRow';
import { SessionApiClient, setTaskPayableAmount } from './apiClient';
import type { EditorPayload, PayableData, TaskData } from './types';

interface EditorDashboardProps {
  client: SessionApiClient;
  code: string;
  token: string;
}

export function EditorDashboard({ client, code, token }: EditorDashboardProps) {
  const [payload, setPayload] = useState<EditorPayload | null>(null);
  const [selectedJobKey, setSelectedJobKey] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [status, setStatus] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await client.fetchSession(code, token);
      setPayload(data);
      const keys = Object.keys(data.jobs);
      setSelectedJobKey(keys[0] ?? null);
      setStatus('Session loaded');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load');
    } finally {
      setLoading(false);
    }
  }, [client, code, token]);

  const save = useCallback(async () => {
    if (!payload) return;
    setSaving(true);
    setError(null);
    setStatus(null);
    try {
      const saved = await client.saveSession(code, token, payload);
      setPayload(saved.payload);
      setStatus('Saved successfully');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to save');
    } finally {
      setSaving(false);
    }
  }, [client, code, token, payload]);

  const updatePayableAmount = (jobKey: string, taskIndex: number, payableIndex: number, amount: string) => {
    try {
      const next = setTaskPayableAmount(payload!, jobKey, taskIndex, payableIndex, amount);
      setPayload(next);
      setError(null);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Invalid edit');
    }
  };

  const addTask = (jobKey: string) => {
    if (!payload) return;
    const next = { ...payload, jobs: { ...payload.jobs } };
    next.jobs[jobKey] = {
      ...next.jobs[jobKey],
      tasks: [...next.jobs[jobKey].tasks, { actionTypeKey: '', contextKey: '', payables: [] }],
    };
    setPayload(next);
  };

  const deleteTask = (jobKey: string, taskIndex: number) => {
    if (!payload) return;
    const next = { ...payload, jobs: { ...payload.jobs } };
    next.jobs[jobKey] = {
      ...next.jobs[jobKey],
      tasks: next.jobs[jobKey].tasks.filter((_, i) => i !== taskIndex),
    };
    setPayload(next);
  };

  return (
    <div className="min-h-screen flex flex-col">
      <header className="navbar bg-base-200 px-4 py-2 border-b border-base-300">
        <div className="flex-1">
          <h1 className="text-xl font-bold">Secure Session Editor</h1>
        </div>
        <div className="flex-none flex items-center gap-2">
          {loading && <span className="loading loading-dots loading-sm"></span>}
          {status && <span className="badge badge-success">{status}</span>}
          {error && <span className="badge badge-error" role="alert">{error}</span>}
          <button className="btn btn-sm btn-primary" onClick={load} disabled={loading}>Load</button>
          <button className="btn btn-sm btn-accent" onClick={save} disabled={saving}>Save</button>
        </div>
      </header>

      <div className="flex flex-1 overflow-hidden">
        <aside className="w-64 bg-base-200 border-r border-base-300 p-3 overflow-y-auto">
          <h2 className="text-sm font-semibold uppercase opacity-60 mb-2">Jobs</h2>
          <ul className="menu menu-sm bg-base-200 rounded-box">
            {payload && Object.keys(payload.jobs).map((jobKey) => {
              const job = payload.jobs[jobKey];
              return (
                <li key={jobKey}>
                  <button
                    type="button"
                    className={selectedJobKey === jobKey ? 'active' : ''}
                    onClick={() => setSelectedJobKey(jobKey)}
                  >
                    {job.displayName}
                  </button>
                </li>
              );
            })}
          </ul>
        </aside>

        <main className="flex-1 p-4 overflow-y-auto">
          {selectedJobKey && payload && (
            <JobPanel
              jobKey={selectedJobKey}
              job={payload.jobs[selectedJobKey]}
              payload={payload}
              setPayload={setPayload}
              setError={setError}
            />
          )}
        </main>
      </div>
    </div>
  );
}

function JobPanel({
  jobKey,
  job,
  payload,
  setPayload,
  setError,
}: {
  jobKey: string;
  job: { displayName: string; tasks: TaskData[] };
  payload: EditorPayload;
  setPayload: (p: EditorPayload) => void;
  setError: (e: string | null) => void;
}) {
  const updatePayableAmount = (taskIndex: number, payableIndex: number, amount: string) => {
    try {
      const next = setTaskPayableAmount(payload, jobKey, taskIndex, payableIndex, amount);
      setPayload(next);
      setError(null);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Invalid edit');
    }
  };

  const addTask = () => {
    const next = { ...payload, jobs: { ...payload.jobs } };
    next.jobs[jobKey] = {
      ...next.jobs[jobKey],
      tasks: [...next.jobs[jobKey].tasks, { actionTypeKey: '', contextKey: '', payables: [] }],
    };
    setPayload(next);
  };

  const deleteTask = (taskIndex: number) => {
    const next = { ...payload, jobs: { ...payload.jobs } };
    next.jobs[jobKey] = {
      ...next.jobs[jobKey],
      tasks: next.jobs[jobKey].tasks.filter((_, i) => i !== taskIndex),
    };
    setPayload(next);
  };

  return (
    <section className="card bg-base-200 shadow-sm p-4 space-y-4 max-w-4xl">
      <div className="flex items-center justify-between">
        <h2 className="text-2xl font-bold">{job.displayName}</h2>
        <button type="button" className="btn btn-sm btn-secondary" onClick={addTask}>Add task</button>
      </div>

      <div className="overflow-x-auto">
        <table className="table table-zebra">
          <thead>
            <tr>
              <th>Action</th>
              <th>Context</th>
              <th>Payables</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {job.tasks.map((task, taskIndex) => (
              <tr key={taskIndex}>
                <td className="font-mono text-sm">{task.actionTypeKey}</td>
                <td className="font-mono text-sm">{task.contextKey}</td>
                <td>
                  <div className="flex flex-col gap-1">
                    {task.payables.map((payable: PayableData, payableIndex: number) => (
                      <PayableRow
                        key={payableIndex}
                        payable={payable}
                        taskIndex={taskIndex}
                        payableIndex={payableIndex}
                        onChange={updatePayableAmount}
                      />
                    ))}
                  </div>
                </td>
                <td>
                  <button
                    type="button"
                    className="btn btn-xs btn-ghost text-error"
                    onClick={() => deleteTask(taskIndex)}
                  >
                    Delete
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  );
}
```

- [ ] **Step 4: Implement `SessionEditor.tsx`**

```tsx
import { useEffect, useMemo, useState } from 'react';
import { AuthCard } from './AuthCard';
import { EditorDashboard } from './EditorDashboard';
import { SessionApiClient, validateApiBase } from './apiClient';
import { scrubTokenFromQuery } from './sessionCredentials';
import { useUrlCredentials } from './useUrlCredentials';

export interface SessionEditorProps {
  client?: SessionApiClient;
}

export function SessionEditor({ client: clientProp }: SessionEditorProps) {
  const env = useMemo(() => import.meta.env as Record<string, string | undefined>, []);
  const url = useUrlCredentials(env);

  const [code, setCode] = useState(url.code);
  const [token, setToken] = useState(url.token);
  const [apiBase, setApiBase] = useState(url.api);
  const [client, setClient] = useState<SessionApiClient | null>(clientProp ?? null);
  const [clientError, setClientError] = useState<string | null>(null);

  useEffect(() => {
    if (!code || !token || !apiBase) {
      setClient(clientProp ?? null);
      return;
    }
    try {
      const resolved = validateApiBase(apiBase, env);
      setClient(clientProp ?? new SessionApiClient(resolved));
      setClientError(null);
      scrubTokenFromQuery();
    } catch (err) {
      setClient(null);
      setClientError(err instanceof Error ? err.message : 'Invalid API base');
    }
  }, [code, token, apiBase, clientProp, env]);

  if (!client) {
    return (
      <AuthCard
        initialCode={code}
        initialToken={token}
        initialApi={apiBase}
        error={clientError}
        env={env}
        onSubmit={(newCode, newToken, newApi) => {
          setCode(newCode);
          setToken(newToken);
          setApiBase(newApi);
        }}
      />
    );
  }

  return <EditorDashboard client={client} code={code} token={token} />;
}
```

- [ ] **Step 5: Delete `SessionEditor.css`**

```bash
rm web/session-editor/src/SessionEditor.css
```

- [ ] **Step 6: Run tests and build**

```bash
npm test
npm run build
```

Expected: all tests pass; `dist/` bundle is valid.

---

### Task 5: Update Paper `EditorService` URL generation

**Files:**
- Modify: `paper/src/main/java/net/aincraft/editor/EditorService.java`

**Interfaces:**
- Consumes: `config.webEditorUrl()`, `config.sessionApiUrl()`.
- Produces: `editorUrl` returns `https://modularjobs.com/editor/?api=...&code=...#token=...`.

- [ ] **Step 1: Change `editorUrl` to drop `/session` path**

```java
static String editorUrl(String base, String apiBase, String code, String token) {
    String normalized = base.replaceFirst("/+$", "");
    String encodedApi = encode(apiBase);
    return normalized + "?api=" + encodedApi + "&code=" + encode(code) + "#token=" + encode(token);
}
```

- [ ] **Step 2: Compile Java**

```bash
./gradlew :paper:compileJava
```

Expected: build succeeds.

---

### Task 6: Remove Astro editor pages and update web build pipeline

**Files:**
- Delete: `web/src/pages/editor/index.astro`
- Delete: `web/src/pages/editor/session.astro`
- Modify: `web/package.json`
- Modify: `web/.gitignore`
- Modify: `web/src/lib/sessionApi.ts`
- Modify: `web/README.md`

**Interfaces:**
- Produces: React editor built to `public/editor` → `dist/editor`.

- [ ] **Step 1: Delete Astro editor pages**

```bash
rm -rf web/src/pages/editor
```

- [ ] **Step 2: Update `web/package.json` build scripts**

```json
{
  "scripts": {
    "dev": "npm run build:editor && astro dev",
    "build:editor": "cd session-editor && npm run build && cd .. && rm -rf public/editor && cp -r session-editor/dist public/editor",
    "build": "npm run build:editor && astro build",
    "preview": "astro preview"
  }
}
```

- [ ] **Step 3: Update `web/src/lib/sessionApi.ts`**

```ts
export function sessionEditorPath(code: string, token: string): string {
  const q = new URLSearchParams({ code });
  const h = new URLSearchParams({ token });
  return `/editor/?${q.toString()}#${h.toString()}`;
}
```

- [ ] **Step 4: Update `web/.gitignore`**

Ensure these two lines are present:

```
public/session-editor/
public/editor/
```

- [ ] **Step 5: Update `web/README.md` editor section**

Add a note that the editor is a single React app under `web/session-editor` built into `public/editor`.

- [ ] **Step 6: Build and verify**

```bash
cd web
npm run build
ls -la dist/editor
```

Expected: `dist/editor/index.html` and `dist/editor/assets/` exist.

---

### Task 7: Redesign Astro plugin homepage

**Files:**
- Modify: `web/src/pages/index.astro`
- Create: `web/src/components/Hero.astro`
- Create: `web/src/components/Features.astro`
- Create: `web/src/components/QuickStart.astro`
- Delete: `web/src/components/Body.astro`
- Delete: `web/src/components/Info.astro`
- Delete: `web/src/components/InfoCard.astro`

**Interfaces:**
- Produces: new homepage with hero, feature grid, quick start.

- [ ] **Step 1: Write `Hero.astro`**

```astro
---
---
<section class="hero min-h-[60vh] bg-base-100 border-b border-base-300">
  <div class="hero-content text-center">
    <div class="max-w-2xl">
      <h1 class="text-5xl font-extrabold bg-gradient-to-r from-primary to-secondary bg-clip-text text-transparent">
        ModularJobs
      </h1>
      <p class="py-6 text-lg text-base-content/80">
        Configurable jobs, progression, and payables for Paper servers.
      </p>
      <div class="flex justify-center gap-3">
        <a href="/wiki/" class="btn btn-primary">Get Started</a>
        <a href="https://github.com/aincraft-org/modularjobs" class="btn btn-outline">GitHub</a>
      </div>
    </div>
  </div>
</section>
```

- [ ] **Step 2: Write `Features.astro`**

```astro
---
const features = [
  { title: 'Jobs & Tasks', body: 'Define block break, crafting, killing, and custom action tasks.' },
  { title: 'Payables', body: 'Attach experience, money, items, or custom rewards.' },
  { title: 'Boosts', body: 'Timed and item-based multipliers for players and groups.' },
  { title: 'Upgrade Trees', body: 'Skill-point unlocks and node-based progression.' },
  { title: 'MySQL 8', body: 'Shared operator-managed persistence for Paper and REST API.' },
  { title: 'Secure Editor', body: 'Browser sessions with per-server REST APIs and token-in-fragment URLs.' },
];
---
<section class="py-12 px-4 max-w-6xl mx-auto">
  <h2 class="text-3xl font-bold mb-8 text-center">Features</h2>
  <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
    {features.map((f) => (
      <div class="card bg-base-200 shadow-sm p-6">
        <h3 class="text-xl font-semibold mb-2 text-primary">{f.title}</h3>
        <p class="text-base-content/70">{f.body}</p>
      </div>
    ))}
  </div>
</section>
```

- [ ] **Step 3: Write `QuickStart.astro`**

```astro
---
---
<section class="py-12 px-4 bg-base-200 border-y border-base-300">
  <div class="max-w-4xl mx-auto space-y-6">
    <h2 class="text-3xl font-bold text-center">Quick start</h2>
    <div class="grid grid-cols-1 md:grid-cols-3 gap-4 text-center">
      <div class="card bg-base-100 p-4">
        <div class="text-2xl font-bold text-primary">1</div>
        <p>Apply the MySQL 8 schema out of band.</p>
      </div>
      <div class="card bg-base-100 p-4">
        <div class="text-2xl font-bold text-primary">2</div>
        <p>Drop the plugin jar into your Paper server.</p>
      </div>
      <div class="card bg-base-100 p-4">
        <div class="text-2xl font-bold text-primary">3</div>
        <p>Run <code class="kbd kbd-sm">/jobs editor</code> and open the secure web editor.</p>
      </div>
    </div>
  </div>
</section>
```

- [ ] **Step 4: Rewrite `index.astro`**

```astro
---
import '../styles/global.css';
import Base from '../layouts/Base.astro';
import Hero from '../components/Hero.astro';
import Features from '../components/Features.astro';
import QuickStart from '../components/QuickStart.astro';
---

<Base lang="en" theme="dark">
  <main class="bg-base-100 text-base-content">
    <Hero />
    <Features />
    <QuickStart />
  </main>
</Base>
```

- [ ] **Step 5: Delete obsolete components**

```bash
rm web/src/components/Body.astro
rm web/src/components/Info.astro
rm web/src/components/InfoCard.astro
```

- [ ] **Step 6: Build and verify**

```bash
cd web
npm run build
```

Expected: `dist/index.html` contains the new homepage.

---

### Task 8: Full build, verify, and smoke test

**Files:**
- Run: all modules.

- [ ] **Step 1: Run editor unit tests**

```bash
cd web/session-editor
npm test
```

Expected: all tests pass.

- [ ] **Step 2: Compile Paper Java**

```bash
cd /home/jlo/dev/modularjobs
./gradlew :paper:compileJava
```

Expected: build succeeds.

- [ ] **Step 3: Run REST API library tests**

```bash
cd web/rest-api
cargo test --lib
```

Expected: tests pass.

- [ ] **Step 4: Full web build**

```bash
cd /home/jlo/dev/modularjobs/web
npm run build
```

Expected: `dist/editor/`, `dist/index.html`, and `dist/wiki/` all present.

- [ ] **Step 5: Browser smoke test**

Start a static preview:

```bash
python3 -m http.server 4321 --directory dist
```

Open `http://localhost:4321/editor/?api=http://localhost:9999&code=TEST#token=SECRET` with a mock API on `localhost:9999` and confirm the editor loads and saves. Open `http://localhost:4321/` and confirm the homepage renders.

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "feat: migrate editor to React dashboard and redesign plugin homepage"
```

---

## Self-review

**Spec coverage:**
- Single React app for editor: Tasks 2, 3, 4, 6.
- Dashboard style with sidebar: Task 4.
- Plugin homepage redesign: Task 7.
- Shared visual system: Tasks 1, 4, 7.
- Paper URL update: Task 5.
- Build integration: Task 6.
- Security invariants preserved: token in hash, API validation in `AuthCard`/`SessionEditor`, headers unchanged.

**Placeholder scan:**
- No TBDs or unspecified code blocks.
- `VITE_ALLOWED_API_ORIGINS` is given as an example pattern that the operator must replace with their own domain.

**Type consistency:**
- `AuthCard` `onSubmit` signature matches `SessionEditor` usage.
- `EditorDashboard` accepts `SessionApiClient`, `code`, `token`.
- `editorUrl` keeps the same Java signature but drops the `/session` path.