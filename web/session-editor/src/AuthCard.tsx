import { FormEvent, useState } from 'react';
import { resolveApiBaseUrl, validateApiBase } from './apiClient';

export interface AuthCardProps {
  onSubmit: (code: string, token: string, apiBase: string) => void;
  env?: Record<string, string | undefined>;
  initialCode?: string;
  initialToken?: string;
  initialApi?: string;
  error?: string;
}

export function AuthCard({
  onSubmit,
  env,
  initialCode = '',
  initialToken = '',
  initialApi,
  error,
}: AuthCardProps) {
  const runtimeEnv = env ?? (import.meta.env as Record<string, string | undefined>);
  const [code, setCode] = useState(initialCode);
  const [token, setToken] = useState(initialToken);
  const [apiBase, setApiBase] = useState(
    initialApi?.trim() ? initialApi : resolveApiBaseUrl(runtimeEnv),
  );
  const [validationError, setValidationError] = useState<string | null>(null);

  const submit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setValidationError(null);
    try {
      const validatedApiBase = validateApiBase(apiBase, runtimeEnv);
      onSubmit(code, token, validatedApiBase);
    } catch (cause) {
      setValidationError(cause instanceof Error ? cause.message : 'Invalid API base');
    }
  };

  return (
    <section className="card bg-base-200 shadow-xl" aria-labelledby="auth-card-title">
      <div className="card-body">
        <h1 id="auth-card-title" className="card-title">Load session</h1>
        {(error || validationError) && (
          <div className="alert alert-error" role="alert">
            {validationError ?? error}
          </div>
        )}
        <form id="auth-card-form" className="space-y-4" onSubmit={submit}>
          <label className="form-control w-full" htmlFor="session-code">
            <span className="label-text">Session code</span>
            <input
              id="session-code"
              className="input input-bordered w-full"
              value={code}
              onChange={(event) => setCode(event.target.value)}
              required
            />
          </label>
          <label className="form-control w-full" htmlFor="session-token">
            <span className="label-text">Session token</span>
            <input
              id="session-token"
              className="input input-bordered w-full"
              type="password"
              value={token}
              onChange={(event) => setToken(event.target.value)}
              required
            />
          </label>
          <label className="form-control w-full" htmlFor="api-base">
            <span className="label-text">API base</span>
            <input
              id="api-base"
              className="input input-bordered w-full"
              type="url"
              value={apiBase}
              onChange={(event) => setApiBase(event.target.value)}
              required
            />
          </label>
          <button className="btn btn-primary" type="submit">Load session</button>
        </form>
      </div>
    </section>
  );
}

export default AuthCard;
