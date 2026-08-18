import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { SessionApiClient, resolveApiBaseUrl, setTaskPayableAmount, validateApiBase } from './apiClient';
import { AuthCard } from './AuthCard';
import { useUrlCredentials } from './useUrlCredentials';
import type { EditorPayload, TaskData } from './types';

export interface SessionEditorProps {
  client?: SessionApiClient;
  initialCode?: string;
  initialToken?: string;
  initialApi?: string;
}

type Credentials = { code: string; token: string; api: string };

function message(error: unknown): string {
  return error instanceof Error ? error.message : String(error);
}

/**
 * Resolve the effective API base for a set of credentials at the SessionEditor
 * boundary. An empty/whitespace api falls back to the documented resolver
 * default (VITE_SESSION_API_URL / VITE_API_BASE_URL / http://127.0.0.1:18787)
 * instead of failing validateApiBase on an empty string; a non-empty api still
 * goes through strict validation and fails closed. Explicit ?api and manual
 * AuthCard entries are therefore preserved while credential-only deep links get
 * the documented localhost default.
 */
function resolveCredentialsApi(api: string | undefined): string {
  const trimmed = api?.trim() ?? '';
  const env = import.meta.env as Record<string, string | undefined>;
  if (!trimmed) {
    return resolveApiBaseUrl(env, '');
  }
  return validateApiBase(trimmed, env);
}

export function SessionEditor({ client: clientProp, initialCode, initialToken, initialApi }: SessionEditorProps) {
  const urlCredentials = useUrlCredentials(import.meta.env as Record<string, string | undefined>);
  const initialCredentials = useMemo<Credentials>(() => ({
    code: initialCode ?? urlCredentials.code,
    token: initialToken ?? urlCredentials.token,
    api: initialApi ?? urlCredentials.api,
  }), [initialApi, initialCode, initialToken, urlCredentials.api, urlCredentials.code, urlCredentials.token]);
  const [credentials, setCredentials] = useState<Credentials | null>(
    initialCredentials.code && initialCredentials.token ? initialCredentials : null,
  );
  const [client, setClient] = useState<SessionApiClient | undefined>(clientProp);
  const loadGeneration = useRef(0);
  const autoLoadedFor = useRef<Credentials | null>(null);
  const [payload, setPayload] = useState<EditorPayload | null>(null);
  const [selectedJobKey, setSelectedJobKey] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [status, setStatus] = useState<string | null>(null);

  const load = useCallback(async (nextCredentials: Credentials, nextClient?: SessionApiClient) => {
    const generation = ++loadGeneration.current;
    setLoading(true);
    setError(null);
    setStatus(null);
    try {
      const activeClient = nextClient ?? clientProp ?? client;
      if (!activeClient) throw new Error('API client is not configured');
      const data = await activeClient.fetchSession(nextCredentials.code, nextCredentials.token);
      if (generation !== loadGeneration.current) return;
      setCredentials(nextCredentials);
      if (!clientProp) setClient(activeClient);
      setPayload(data);
      setSelectedJobKey(Object.keys(data.jobs)[0] ?? null);
      setStatus('Session loaded');
    } catch (cause) {
      if (generation !== loadGeneration.current) return;
      setPayload(null);
      setError(message(cause));
    } finally {
      if (generation === loadGeneration.current) {
        setLoading(false);
      }
    }
  }, [client, clientProp]);

  useEffect(() => {
    // Auto-load exactly once per credentials object: deep-link credentials or
    // an injected client for tests/embedding. No token is rendered; the fetch
    // uses the injected client when present. Guarding by reference prevents
    // effect loops (setClient / payload updates) and retry loops on error.
    if (!credentials || payload || autoLoadedFor.current === credentials) return;
    autoLoadedFor.current = credentials;
    if (clientProp) {
      void load(credentials);
      return;
    }
    if (client) {
      void load(credentials);
      return;
    }
    try {
      const activeClient = new SessionApiClient(resolveCredentialsApi(credentials.api));
      void load(credentials, activeClient);
    } catch (cause) {
      setError(message(cause));
    }
  }, [client, clientProp, credentials, load, payload]);

  useEffect(() => {
    if (clientProp && client !== clientProp) setClient(clientProp);
  }, [client, clientProp]);

  const submitAuth = (code: string, token: string, api: string) => {
    try {
      const validated = resolveCredentialsApi(api);
      const next = { code, token, api: validated };
      const activeClient = clientProp ?? new SessionApiClient(validated);
      void load(next, activeClient);
    } catch (cause) {
      setError(message(cause));
    }
  };

  const logout = () => {
    loadGeneration.current += 1; // invalidate any in-flight load
    autoLoadedFor.current = null;
    const url = new URL(window.location.href);
    for (const key of ['code', 'session', 'token', 'sessionToken', 'api']) {
      url.searchParams.delete(key);
    }
    url.hash = '';
    window.history.replaceState(null, '', `${url.pathname}${url.search}`);
    setCredentials(null);
    setClient(clientProp);
    setPayload(null);
    setSelectedJobKey(null);
    setError(null);
    setStatus(null);
    setLoading(false);
  };

  const save = async () => {
    if (!payload || !credentials || !client) return;
    setSaving(true);
    setError(null);
    setStatus(null);
    try {
      const result = await client.saveSession(credentials.code, credentials.token, payload);
      setPayload(result.payload);
      setStatus('Saved successfully');
    } catch (cause) {
      setError(message(cause));
    } finally {
      setSaving(false);
    }
  };

  const jobKeys = useMemo(() => payload ? Object.keys(payload.jobs).sort((a, b) => payload.jobs[a].displayName.localeCompare(payload.jobs[b].displayName)) : [], [payload]);
  const selectedJob = selectedJobKey && payload ? payload.jobs[selectedJobKey] : null;

  const updatePayableAmount = (taskIndex: number, payableIndex: number, amount: string) => {
    if (payload && selectedJobKey) setPayload(setTaskPayableAmount(payload, selectedJobKey, taskIndex, payableIndex, amount));
  };
  const updateTaskField = (taskIndex: number, field: keyof Pick<TaskData, 'actionTypeKey' | 'contextKey'>, value: string) => {
    if (!payload || !selectedJobKey) return;
    const job = payload.jobs[selectedJobKey];
    setPayload({ ...payload, jobs: { ...payload.jobs, [selectedJobKey]: { ...job, tasks: job.tasks.map((task, index) => index === taskIndex ? { ...task, [field]: value } : task) } } });
  };
  const addTask = () => {
    if (!payload || !selectedJobKey) return;
    const job = payload.jobs[selectedJobKey];
    const task: TaskData = { actionTypeKey: payload.registeredActionTypes[0] ?? 'modularjobs:block_break', contextKey: '', payables: [{ type: payload.registeredPayableTypes[0] ?? 'modularjobs:experience', amount: '1.0' }] };
    setPayload({ ...payload, jobs: { ...payload.jobs, [selectedJobKey]: { ...job, tasks: [...job.tasks, task] } } });
  };
  const deleteTask = (taskIndex: number) => {
    if (!payload || !selectedJobKey) return;
    const job = payload.jobs[selectedJobKey];
    setPayload({ ...payload, jobs: { ...payload.jobs, [selectedJobKey]: { ...job, tasks: job.tasks.filter((_, index) => index !== taskIndex) } } });
  };

  if (!credentials || (!payload && !loading)) {
    return <main className="min-h-screen bg-base-300 p-4 text-base-content sm:p-8"><div className="mx-auto max-w-xl pt-8 sm:pt-16"><AuthCard onSubmit={submitAuth} error={error ?? undefined} initialCode={initialCredentials.code} initialToken={initialCredentials.token} initialApi={initialCredentials.api} /></div></main>;
  }

  return (
    <main className="min-h-screen bg-base-300 text-base-content">
      <header className="navbar border-b border-base-content/10 bg-base-200 px-4 sm:px-8">
        <div className="flex-1"><div><h1 className="text-lg font-bold sm:text-xl">Session Editor</h1><p className="text-xs text-base-content/60">ModularJobs configuration</p></div></div>
        <div className="flex gap-2"><button className="btn btn-ghost btn-sm" type="button" onClick={() => credentials && client && void load(credentials)}>Reload</button><button className="btn btn-outline btn-sm" type="button" onClick={logout}>Log out</button></div>
      </header>
      <div className="mx-auto grid max-w-7xl gap-4 p-4 sm:p-6 lg:grid-cols-[18rem_1fr]">
        <aside className="rounded-box bg-base-200 p-3 shadow-xl"><h2 className="mb-3 px-2 text-sm font-semibold uppercase tracking-wide text-base-content/60">Jobs</h2><div className="space-y-1" role="list" aria-label="Jobs">{jobKeys.map((key) => <button key={key} type="button" role="listitem" aria-current={key === selectedJobKey ? 'page' : undefined} className={`btn btn-block justify-start ${key === selectedJobKey ? 'btn-primary' : 'btn-ghost'}`} onClick={() => setSelectedJobKey(key)}>{payload?.jobs[key].displayName}</button>)}{jobKeys.length === 0 && <p className="px-2 text-sm text-base-content/60">No jobs found.</p>}</div></aside>
        <section className="min-w-0">{error && <div className="alert alert-error mb-4" role="alert">{error}<button className="btn btn-sm" type="button" onClick={() => credentials && client && void load(credentials)}>Try again</button></div>}{status && <div className="alert alert-success mb-4" role="status">{status}</div>}{loading && <div className="loading loading-spinner loading-lg" aria-label="Loading session" />}{selectedJob && selectedJobKey && <div className="rounded-box bg-base-200 p-4 shadow-xl sm:p-6"><div className="mb-5 flex flex-wrap items-center justify-between gap-3"><div><h2 className="text-2xl font-bold">{selectedJob.displayName}</h2><p className="text-sm text-base-content/60">{selectedJob.tasks.length} tasks configured</p></div><div className="flex gap-2"><button className="btn btn-ghost" type="button" onClick={addTask}>Add task</button><button className="btn btn-primary" type="button" onClick={() => void save()} disabled={saving}>{saving ? 'Saving…' : 'Save changes'}</button></div></div><div className="overflow-x-auto"><table className="table"><thead><tr><th>Action</th><th>Context</th><th>Payables</th><th><span className="sr-only">Actions</span></th></tr></thead><tbody>{selectedJob.tasks.map((task, taskIndex) => <tr key={`${taskIndex}-${task.actionTypeKey}`}><td><input className="input input-bordered input-sm w-full min-w-48" aria-label={`Task ${taskIndex + 1} action`} value={task.actionTypeKey} onChange={(event) => updateTaskField(taskIndex, 'actionTypeKey', event.target.value)} /></td><td><input className="input input-bordered input-sm w-full min-w-40" aria-label={`Task ${taskIndex + 1} context`} value={task.contextKey} onChange={(event) => updateTaskField(taskIndex, 'contextKey', event.target.value)} /></td><td><div className="space-y-2">{task.payables.map((payable, payableIndex) => <label className="flex items-center gap-2" key={payableIndex}><span className="text-sm text-base-content/70">{payable.type.split(':').pop()}</span><input className="input input-bordered input-sm w-28" aria-label={`${payable.type} amount`} data-testid={`payable-amount-${taskIndex}-${payableIndex}`} value={payable.amount} onChange={(event) => updatePayableAmount(taskIndex, payableIndex, event.target.value)} /></label>)}</div></td><td><button className="btn btn-ghost btn-sm text-error" type="button" onClick={() => deleteTask(taskIndex)}>Delete</button></td></tr>)}</tbody></table></div></div>}</section>
      </div>
    </main>
  );
}

export default SessionEditor;
