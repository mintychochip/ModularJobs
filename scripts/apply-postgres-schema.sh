#!/usr/bin/env bash
# Apply ModularJobs Postgres DDL out-of-band (not from the Java or Rust process).
#
# Usage:
#   ./scripts/apply-postgres-schema.sh
#   DATABASE_URL=postgres://user:pass@host:5432/db ./scripts/apply-postgres-schema.sh
#   ./scripts/apply-postgres-schema.sh postgres://user:pass@host:5432/db
#
# Schema source of truth:
#   paper/src/main/resources/sql/postgres.sql
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SCHEMA="${ROOT}/paper/src/main/resources/sql/postgres.sql"

if [[ ! -f "${SCHEMA}" ]]; then
  echo "error: schema file not found: ${SCHEMA}" >&2
  exit 1
fi

URL="${1:-${DATABASE_URL:-postgres://test:test@127.0.0.1:55432/modularjobs}}"

echo "Applying ModularJobs Postgres schema"
echo "  file: ${SCHEMA}"
echo "  url:  ${URL}"

if command -v psql >/dev/null 2>&1; then
  psql "${URL}" -v ON_ERROR_STOP=1 -f "${SCHEMA}"
elif command -v docker >/dev/null 2>&1; then
  # Fallback: run psql inside a one-shot postgres client container on host network.
  docker run --rm --network host -v "${SCHEMA}:/schema.sql:ro" postgres:16-alpine \
    psql "${URL}" -v ON_ERROR_STOP=1 -f /schema.sql
else
  echo "error: need psql or docker to apply schema" >&2
  exit 1
fi

echo "Schema apply complete."
