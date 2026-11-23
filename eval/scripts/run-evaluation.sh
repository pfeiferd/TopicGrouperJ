#!/bin/sh
# Builds the library and the evaluation module and runs the experiments.
#
#   scripts/run-evaluation.sh [--quick] [experiments] [--data DIR] [--out DIR]
#
# experiments: perplexity, twc-series, performance, topics, classification, homonyms, or the paper
# names ecir2019, feature-reduction (comma separated, default: all).
# Results (CSV files; the topics experiment also writes .mm, .json.gz and .svg hierarchies) end up
# in eval/results unless --out is given. See scripts/draw-hierarchy.sh for other drawings of the
# hierarchies and scripts/browse-hierarchy.sh for the browser.
# Environment: MVN = Maven command (default: mvn), MVN_OPTS = additional Maven options.
set -eu

HERE="$(cd "$(dirname "$0")/.." && pwd)"
MVN="${MVN:-mvn}"

# Builds api and eval (the parent pom at the project root).
$MVN -q -B ${MVN_OPTS:-} -f "$HERE/../pom.xml" install -DskipTests

cd "$HERE"
exec java -Xmx8g -jar target/topic-grouper-eval.jar --data data --out results "$@"
