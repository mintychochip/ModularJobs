#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 1 ]]; then
    echo "usage: $0 YY.M.D.REVISION" >&2
    exit 2
fi

VERSION=$1
PATTERN='^([0-9]{2})\.([1-9][0-9]?)\.([1-9][0-9]?)\.([1-9][0-9]*)$'
if [[ ! "$VERSION" =~ $PATTERN ]]; then
    echo "version must be YY.M.D.REVISION with positive unpadded M, D, and REVISION: $VERSION" >&2
    exit 2
fi

YEAR_SUFFIX=${BASH_REMATCH[1]}
MONTH=${BASH_REMATCH[2]}
DAY=${BASH_REMATCH[3]}
REVISION=${BASH_REMATCH[4]}
YEAR=$((2000 + 10#$YEAR_SUFFIX))
MONTH_NUMBER=$((10#$MONTH))
DAY_NUMBER=$((10#$DAY))
REVISION_NUMBER=$((10#$REVISION))

if (( MONTH_NUMBER > 12 )); then
    echo "version month must be between 1 and 12: $VERSION" >&2
    exit 2
fi

case "$MONTH_NUMBER" in
    2)
        if (( YEAR % 400 == 0 || (YEAR % 4 == 0 && YEAR % 100 != 0) )); then
            MAX_DAY=29
        else
            MAX_DAY=28
        fi
        ;;
    4|6|9|11) MAX_DAY=30 ;;
    *) MAX_DAY=31 ;;
esac

if (( DAY_NUMBER > MAX_DAY )); then
    echo "version day is invalid for its month and year: $VERSION" >&2
    exit 2
fi

if (( REVISION_NUMBER < 1 )); then
    echo "version revision must be at least 1: $VERSION" >&2
    exit 2
fi

printf 'valid release version: %s\n' "$VERSION"
