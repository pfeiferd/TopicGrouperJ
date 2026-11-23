#!/bin/sh
# Draws a recorded topic hierarchy (the <corpus>.json.gz of the topics experiment, or a
# serialized .tg node list) as an SVG file.
#
#   scripts/draw-hierarchy.sh <hierarchy file> <svg file> [options]
#
# Options (see org.hhn.topicgrouper.eval.DrawHierarchy): --topics N (stop at the solution with
# N topics), --min-size N, --max-depth N, --top-words N, --font-size N, --cost, --likelihood,
# --no-ids, --no-word-count, --indent, --dashed COST.
#
# Example: scripts/draw-hierarchy.sh results/reuters.json.gz reuters-40.svg --topics 40 --cost
#
# Needs the built module (scripts/run-evaluation.sh or "mvn install" at the project root).
set -eu

HERE="$(cd "$(dirname "$0")/.." && pwd)"
exec java -Djava.awt.headless=true -Xmx4g -cp "$HERE/target/topic-grouper-eval.jar:$HERE/target/lib/*" \
	org.hhn.topicgrouper.eval.DrawHierarchy "$@"
