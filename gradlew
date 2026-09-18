#!/bin/sh
# AI Workstation CI Gradle launcher. GitHub Actions installs the exact pinned Gradle version (9.3.1) before invoking this script.
set -eu
if ! command -v gradle >/dev/null 2>&1; then
  echo "ERROR: Gradle 9.3.1 must be installed by the CI workflow before using this launcher." >&2
  exit 127
fi
exec gradle "$@"
