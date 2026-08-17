#!/usr/bin/env bash
# Apply ModularJobs MySQL DDL out-of-band (not from the Java or Rust process).
#
# Usage:
#   ./scripts/apply-mysql-schema.sh
#   DATABASE_URL=mysql://user:pass@host:3306/db ./scripts/apply-mysql-schema.sh
#   ./scripts/apply-mysql-schema.sh mysql://user:pass@host:3306/db
#
# Schema source of truth:
#   paper/src/main/resources/sql/mysql.sql
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SCHEMA="${ROOT}/paper/src/main/resources/sql/mysql.sql"

if [[ ! -f "${SCHEMA}" ]]; then
  echo "error: schema file not found: ${SCHEMA}" >&2
  exit 1
fi

URL="${1:-${DATABASE_URL:-mysql://test:test@127.0.0.1:3306/modularjobs}}"
if [[ "${URL}" =~ ^mysql://([^:]+):([^@]+)@([^:/]+):([0-9]+)/(.+)$ ]]; then
  DB_USER="${BASH_REMATCH[1]}"
  DB_PASSWORD="${BASH_REMATCH[2]}"
  DB_HOST="${BASH_REMATCH[3]}"
  DB_PORT="${BASH_REMATCH[4]}"
  DB_NAME="${BASH_REMATCH[5]}"
else
  echo "error: URL must match mysql://user:password@host:port/database" >&2
  exit 2
fi

echo "Applying ModularJobs MySQL schema"
echo "  file: ${SCHEMA}"
echo "  url:  ${URL}"

if command -v mysql >/dev/null 2>&1; then
  MYSQL_PWD="${DB_PASSWORD}" mysql --protocol=TCP \
    --host="${DB_HOST}" --port="${DB_PORT}" --user="${DB_USER}" "${DB_NAME}" < "${SCHEMA}"
elif command -v docker >/dev/null 2>&1; then
  docker run --rm --network host \
    -e MYSQL_PWD="${DB_PASSWORD}" \
    -v "${SCHEMA}:/schema.sql:ro" mysql:8 \
    sh -c 'mysql --protocol=TCP --host="$1" --port="$2" --user="$3" "$4" < /schema.sql' \
    sh "${DB_HOST}" "${DB_PORT}" "${DB_USER}" "${DB_NAME}"
else
  echo "error: need mysql or docker to apply schema" >&2
  exit 1
fi

echo "Schema apply complete."
