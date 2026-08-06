#!/usr/bin/env bash
# Point this clone at the versioned hooks under .githooks/
#
# Usage (from repo root or anywhere):
#   ./scripts/install-git-hooks.sh
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
HOOKS_DIR="${ROOT}/.githooks"

if [[ ! -d "${HOOKS_DIR}" ]]; then
  echo "error: hooks directory missing: ${HOOKS_DIR}" >&2
  exit 1
fi

if [[ ! -d "${ROOT}/.git" ]]; then
  echo "error: not a git repository: ${ROOT}" >&2
  exit 1
fi

chmod +x "${HOOKS_DIR}"/* 2>/dev/null || true
git -C "${ROOT}" config core.hooksPath .githooks

echo "Installed git hooks:"
echo "  core.hooksPath = .githooks"
echo "  pre-commit     = ${HOOKS_DIR}/pre-commit"
echo ""
echo "Skip once: SKIP_PRECOMMIT=1 git commit ..."
