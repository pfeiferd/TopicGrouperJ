#!/bin/sh
# Opens the Swing hierarchy browser (FlatLaf) on a recorded topic hierarchy: a <corpus>.json.gz
# of the topics experiment or a serialized .tg node list. The browser converts to JSON and
# exports SVG drawings.
#
#   scripts/browse-hierarchy.sh [-hidpi] [hierarchy file]
#
# Needs the built module (scripts/run-evaluation.sh or "mvn install" at the project root).
set -eu

HERE="$(cd "$(dirname "$0")/.." && pwd)"
exec java -Xmx4g -cp "$HERE/target/topic-grouper-eval.jar:$HERE/target/lib/*" \
	org.hhn.topicgrouper.tg.viz.HierarchyBrowser "$@"
