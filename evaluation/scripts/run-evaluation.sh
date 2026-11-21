#!/bin/sh
# Builds the library and the evaluation module and runs the experiments.
#
#   scripts/run-evaluation.sh [--quick] [experiments] [--data DIR] [--out DIR]
#
# experiments: perplexity, twc-series, classification, performance (comma separated, default: all).
# Results (CSV files) end up in evaluation/results unless --out is given.
# Environment: MVN = Maven command (default: mvn), MVN_OPTS = additional Maven options.
set -eu

HERE="$(cd "$(dirname "$0")/.." && pwd)"
MVN="${MVN:-mvn}"

# 1. the library (the evaluation depends on it)
$MVN -q -B ${MVN_OPTS:-} -f "$HERE/../pom.xml" install -DskipTests
# 2. the evaluation module
$MVN -q -B ${MVN_OPTS:-} -f "$HERE/pom.xml" package -DskipTests

cd "$HERE"
exec java -Xmx8g -jar target/topic-grouper-evaluation.jar --data data --out results "$@"
