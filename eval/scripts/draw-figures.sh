#!/bin/sh
# Redraws the figures of the papers (SVG) from the CSV files of a result directory into its
# "figs" subdirectory. The experiments draw them anyway when they finish; use this after
# editing or merging CSV files, or to redraw with a newer version of the plotting code.
#
#   scripts/draw-figures.sh [results directory]      (default: results)
#
# Copy the "figs" directory next to doc/topic-grouper.md and doc/feature-reduction.md to update
# the figures referenced there. Needs the built module.
set -eu

HERE="$(cd "$(dirname "$0")/.." && pwd)"
exec java -Djava.awt.headless=true -cp "$HERE/target/topic-grouper-eval.jar:$HERE/target/lib/*" \
	org.hhn.topicgrouper.eval.Figures "$@"
