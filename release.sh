#!/bin/bash
set -o errexit

SOURCE="${SOURCE:-$(git rev-parse --abbrev-ref HEAD)}"
TIMESTAMP="$(git log -1 --pretty=%cd --date=format:'%Y%m%d%H%M%S')"
COMMIT="$(git rev-parse --short HEAD)"
MAJOR="${MAJOR:-0}"
REVISION="${REVISION:-$MAJOR.$SOURCE.$TIMESTAMP.$COMMIT}"

mvn --define revision="$REVISION" --batch-mode "$@" clean deploy 1>&2

echo "REVISION=$REVISION"
