import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  SessionApiClient,
  createDefaultClient,
  setTaskPayableAmount,
} from './apiClient';
import {
  readSessionCredentials,
  scrubTokenFromQuery,
} from './sessionCredentials';
import type { EditorPayload, TaskData } from './types';
import './SessionEditor.css';

export interface SessionEditorProps {
  client?: SessionApiClient;
  initialCode?: string;
  initialToken?: string;
}

export function SessionEditor({
  client: clientProp,
  initialCode,
  initialToken,
}: SessionEditorProps) {
  const client = useMemo(() => clientProp ?? createDefaultClient(), [clientProp]);
  const [code, setCode] = useState(initialCode ?? '');
  const [token, setToken] = useState(initialToken ?? '');
  const [payload, setPayload] = useState<EditorPayload | null>(null);
  const [selectedJobKey, setSelectedJobKey] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [status, setStatus] = useState<string | null>(null);

  useEffect(() => {
    if (initialCode != null || initialToken != null) return;
    scrubTokenFromQuery();
    const q = readSessionCredentials();
    if (q.code) setCode(q.code);
    if (q.token) setToken(q.token);
  }, [initialCode, initialToken]);

  const jobKeys = useMemo(() => {
    if (!payload) return [];
    return Object.keys(payload.jobs).sort((a, b) =>
      payload.jobs[a].displayName.localeCompare(payload.jobs[b].displayName),
    );
  }, [payload]);

  const selectedJob = selectedJobKey && payload ? payload.jobs[selectedJobKey] : null;

  const load = useCallback(async () => {
    setError(null);
    setStatus(null);
    setLoading(true);
    try {
      const data = await client.fetchSession(code, token);
      setPayload(data);
      const keys = Object.keys(data.jobs);
      setSelectedJobKey(keys[0] ?? null);
      setStatus('Session loaded');
    } catch (e) {
      setPayload(null);
      setError(e instanceof Error ? e.message : String(e));
    } finally {
      setLoading(false);
    }
  }, [client, code, token]);

  useEffect(() => {
    scrubTokenFromQuery();
    const q = readSessionCredentials();
    if ((initialCode || q.code) && (initialToken || q.token)) {
      // Auto-load when code+token present on mount
      void (async () => {
        const c = initialCode ?? q.code;
        const t = initialToken ?? q.token;
        if (!c || !t) return;
        setLoading(true);
        try {
          const data = await client.fetchSession(c, t);
          setPayload(data);
          setSelectedJobKey(Object.keys(data.jobs)[0] ?? null);
          setStatus('Session loaded');
        } catch (e) {
          setError(e instanceof Error ? e.message : String(e));
        } finally {
          setLoading(false);
        }
      })();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const save = useCallback(async () => {
    if (!payload) return;
    setError(null);
    setStatus(null);
    setSaving(true);
    try {
      const result = await client.saveSession(code, token, payload);
      setPayload(result.payload);
      setStatus('Saved successfully');
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    } finally {
      setSaving(false);
    }
  }, [client, code, token, payload]);

  const updatePayableAmount = (
    taskIndex: number,
    payableIndex: number,
    amount: string,
  ) => {
    if (!payload || !selectedJobKey) return;
    setPayload(
      setTaskPayableAmount(payload, selectedJobKey, taskIndex, payableIndex, amount),
    );
  };

  const updateTaskField = (
    taskIndex: number,
    field: keyof Pick<TaskData, 'actionTypeKey' | 'contextKey'>,
    value: string,
  ) => {
    if (!payload || !selectedJobKey) return;
    const job = payload.jobs[selectedJobKey];
    const tasks = job.tasks.map((t, i) =>
      i === taskIndex ? { ...t, [field]: value } : t,
    );
    setPayload({
      ...payload,
      jobs: {
        ...payload.jobs,
        [selectedJobKey]: { ...job, tasks },
      },
    });
  };

  const addTask = () => {
    if (!payload || !selectedJobKey) return;
    const job = payload.jobs[selectedJobKey];
    const newTask: TaskData = {
      actionTypeKey: payload.registeredActionTypes[0] || 'modularjobs:block_break',
      contextKey: '',
      payables: [
        {
          type: payload.registeredPayableTypes[0] || 'modularjobs:experience',
          amount: '1.0',
        },
      ],
    };
    setPayload({
      ...payload,
      jobs: {
        ...payload.jobs,
        [selectedJobKey]: { ...job, tasks: [...job.tasks, newTask] },
      },
    });
  };

  const deleteTask = (taskIndex: number) => {
    if (!payload || !selectedJobKey) return;
    const job = payload.jobs[selectedJobKey];
    setPayload({
      ...payload,
      jobs: {
        ...payload.jobs,
        [selectedJobKey]: {
          ...job,
          tasks: job.tasks.filter((_, i) => i !== taskIndex),
        },
      },
    });
  };

  return (
    <div className="session-editor">
      <header className="session-editor__header">
        <h1>Secure Session Editor</h1>
        <p className="session-editor__sub">
          Load and save job task configs via the ModularJobs session API (not bytebin).
        </p>
      </header>

      <section className="session-editor__auth card">
        <label>
          Session code
          <input
            data-testid="session-code"
            value={code}
            onChange={(e) => setCode(e.target.value)}
            placeholder="paste session code"
            autoComplete="off"
          />
        </label>
        <label>
          Session token
          <input
            data-testid="session-token"
            type="password"
            value={token}
            onChange={(e) => setToken(e.target.value)}
            placeholder="session token"
            autoComplete="off"
          />
        </label>
        <div className="session-editor__actions">
          <button
            data-testid="load-btn"
            type="button"
            onClick={() => void load()}
            disabled={loading || !code || !token}
          >
            {loading ? 'Loading…' : 'Load'}
          </button>
          <button
            data-testid="save-btn"
            type="button"
            onClick={() => void save()}
            disabled={saving || !payload || !code || !token}
          >
            {saving ? 'Saving…' : 'Save'}
          </button>
        </div>
      </section>

      {error && (
        <div className="session-editor__error" role="alert" data-testid="error">
          {error}
        </div>
      )}
      {status && (
        <div className="session-editor__status" data-testid="status">
          {status}
        </div>
      )}

      {payload && (
        <>
          <nav className="session-editor__tabs" aria-label="Jobs">
            {jobKeys.map((key) => (
              <button
                key={key}
                type="button"
                className={key === selectedJobKey ? 'active' : ''}
                onClick={() => setSelectedJobKey(key)}
              >
                {payload.jobs[key].displayName}
              </button>
            ))}
          </nav>

          {selectedJob && selectedJobKey && (
            <section className="session-editor__table card">
              <div className="session-editor__table-head">
                <h2>{selectedJob.displayName}</h2>
                <button type="button" onClick={addTask}>
                  Add task
                </button>
              </div>
              <table>
                <thead>
                  <tr>
                    <th>Action</th>
                    <th>Context</th>
                    <th>Payables</th>
                    <th />
                  </tr>
                </thead>
                <tbody>
                  {selectedJob.tasks.map((task, ti) => (
                    <tr key={`${ti}-${task.actionTypeKey}-${task.contextKey}`}>
                      <td>
                        <input
                          value={task.actionTypeKey}
                          onChange={(e) =>
                            updateTaskField(ti, 'actionTypeKey', e.target.value)
                          }
                        />
                      </td>
                      <td>
                        <input
                          value={task.contextKey}
                          onChange={(e) =>
                            updateTaskField(ti, 'contextKey', e.target.value)
                          }
                        />
                      </td>
                      <td>
                        <div className="payables">
                          {task.payables.map((p, pi) => (
                            <label key={pi} className="payable-row">
                              <span>{p.type.split(':').pop()}</span>
                              <input
                                data-testid={`payable-amount-${ti}-${pi}`}
                                value={p.amount}
                                onChange={(e) =>
                                  updatePayableAmount(ti, pi, e.target.value)
                                }
                              />
                            </label>
                          ))}
                        </div>
                      </td>
                      <td>
                        <button type="button" onClick={() => deleteTask(ti)}>
                          Delete
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </section>
          )}
        </>
      )}
    </div>
  );
}

export default SessionEditor;
