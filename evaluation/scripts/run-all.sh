#!/bin/sh
# Complete pipeline: download the data, run all experiments, write CSV files.
# Use "--quick" for a scaled down run that takes minutes instead of days.
set -eu
HERE="$(cd "$(dirname "$0")" && pwd)"
"$HERE/download-data.sh"
exec "$HERE/run-evaluation.sh" "$@"
