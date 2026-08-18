#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
PACKAGER="$ROOT/scripts/package-release-assets.sh"
WORK_DIR="$(mktemp -d)"
trap 'rm -rf -- "$WORK_DIR"' EXIT

VALID_JAR="$WORK_DIR/valid.jar"
BAD_JAR="$WORK_DIR/bad-version.jar"
SQL="$WORK_DIR/mysql.sql"
VALID_OUTPUT="$WORK_DIR/valid-output"
NONEMPTY_OUTPUT="$WORK_DIR/nonempty-output"

python3 - "$VALID_JAR" "$BAD_JAR" "$SQL" <<'PY'
from pathlib import Path
import sys
import zipfile

valid_jar, bad_jar, sql = map(Path, sys.argv[1:])
for path, version in ((valid_jar, "26.8.11.1"), (bad_jar, "1.2.0")):
    with zipfile.ZipFile(path, "w", compression=zipfile.ZIP_STORED) as archive:
        archive.writestr("plugin.yml", f"name: ModularJobs\nversion: '{version}'\n")
        archive.writestr("payload.bin", b"release-test-payload")
sql.write_bytes(b"CREATE TABLE release_test (id integer);\n")
PY

for invalid_version in \
    26.8.11 \
    26.13.11.1 \
    26.4.31.1 \
    26.2.29.1 \
    26.8.11.0 \
    26.08.11.1 \
    26.8.11.01; do
    if bash "$PACKAGER" "$invalid_version" "$VALID_JAR" "$SQL" \
        "$WORK_DIR/invalid-$invalid_version"; then
        echo "expected invalid release version to fail: $invalid_version" >&2
        exit 1
    fi
done

if ! bash "$ROOT/scripts/validate-release-version.sh" 24.2.29.1 >/dev/null; then
    echo "expected Gregorian leap-day version to be accepted" >&2
    exit 1
fi

bash "$PACKAGER" 26.8.11.1 "$VALID_JAR" "$SQL" "$VALID_OUTPUT"

python3 - "$VALID_OUTPUT" "$VALID_JAR" "$SQL" <<'PY'
from pathlib import Path
import sys

output, source_jar, source_sql = map(Path, sys.argv[1:])
expected = {
    "modularjobs-paper-26.8.11.1.jar",
    "modularjobs-mysql-26.8.11.1.sql",
    "SHA256SUMS",
}
actual = {path.name for path in output.iterdir() if path.is_file()}
assert actual == expected, actual
assert (output / "modularjobs-paper-26.8.11.1.jar").read_bytes() == source_jar.read_bytes()
assert (output / "modularjobs-mysql-26.8.11.1.sql").read_bytes() == source_sql.read_bytes()
assert all(not line.split()[-1].startswith("/") for line in (output / "SHA256SUMS").read_text().splitlines())
PY
(cd "$VALID_OUTPUT" && sha256sum --check SHA256SUMS)

if bash "$PACKAGER" 26.8.11.1 "$BAD_JAR" "$SQL" "$WORK_DIR/bad-version-output"; then
    echo "expected embedded version mismatch to fail" >&2
    exit 1
fi

if bash "$PACKAGER" 26.8.11 "$VALID_JAR" "$SQL" "$WORK_DIR/bad-revision-output"; then
    echo "expected missing revision to fail" >&2
    exit 1
fi

mkdir -p "$NONEMPTY_OUTPUT"
printf '%s\n' sentinel > "$NONEMPTY_OUTPUT/sentinel"
if bash "$PACKAGER" 26.8.11.1 "$VALID_JAR" "$SQL" "$NONEMPTY_OUTPUT"; then
    echo "expected non-empty output directory to fail" >&2
    exit 1
fi

printf '%s\n' "package release assets tests: PASS"
