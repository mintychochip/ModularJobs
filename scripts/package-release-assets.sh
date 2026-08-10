#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 4 ]]; then
    echo "usage: $0 VERSION SHADOW_JAR POSTGRES_SQL OUTPUT_DIR" >&2
    exit 2
fi

VERSION=$1
SHADOW_JAR=$2
POSTGRES_SQL=$3
OUTPUT_DIR=$4

if [[ ! "$VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
    echo "version must be semantic x.y.z: $VERSION" >&2
    exit 2
fi

for input in "$SHADOW_JAR" "$POSTGRES_SQL"; do
    if [[ ! -f "$input" ]]; then
        echo "input must be a regular file: $input" >&2
        exit 2
    fi
done

if [[ -e "$OUTPUT_DIR" ]]; then
    if [[ ! -d "$OUTPUT_DIR" ]]; then
        echo "output path exists and is not a directory: $OUTPUT_DIR" >&2
        exit 2
    fi
    if [[ -n "$(find "$OUTPUT_DIR" -mindepth 1 -maxdepth 1 -print -quit)" ]]; then
        echo "output directory must be empty: $OUTPUT_DIR" >&2
        exit 2
    fi
else
    mkdir -p -- "$OUTPUT_DIR"
fi

EXPECTED_VERSION="version: '$VERSION'"
EMBEDDED_DESCRIPTOR="$(unzip -p -- "$SHADOW_JAR" plugin.yml)"
if [[ "$(printf '%s\n' "$EMBEDDED_DESCRIPTOR" | grep -Fxc -- "$EXPECTED_VERSION")" -ne 1 ]]; then
    echo "embedded plugin.yml must contain exactly one $EXPECTED_VERSION line" >&2
    exit 2
fi

JAR_NAME="modularjobs-paper-$VERSION.jar"
SQL_NAME="modularjobs-postgres-$VERSION.sql"
install -m 0644 -- "$SHADOW_JAR" "$OUTPUT_DIR/$JAR_NAME"
install -m 0644 -- "$POSTGRES_SQL" "$OUTPUT_DIR/$SQL_NAME"
(
    cd -- "$OUTPUT_DIR"
    sha256sum "$JAR_NAME" "$SQL_NAME" | sort -k2 > SHA256SUMS
    sha256sum --check SHA256SUMS
)
