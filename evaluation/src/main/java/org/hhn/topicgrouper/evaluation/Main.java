package org.hhn.topicgrouper.evaluation;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Command line entry point of the evaluation.
 * 
 * <pre>
 * java -jar topic-grouper-evaluation.jar [experiments] [--data DIR] [--out DIR] [--quick]
 * </pre>
 * 
 * Experiments (comma separated, default <code>all</code>): <code>perplexity</code>
 * (twc, nips), <code>twc-series</code> (corpus size / alpha series on the synthetic
 * corpus), <code>classification</code> (reuters, 20ng), <code>performance</code>
 * (twc, reuters, 20ng). Experiments whose data set is missing in the data
 * directory are skipped with a message. Results are written as CSV files into the
 * output directory.
 */
public final class Main {
	private Main() {
	}

	/**
	 * @param args
	 *            see class description
	 */
	public static void main(String[] args) {
		File data = new File("data");
		File out = new File("results");
		Settings s = Settings.full();
		Set<String> experiments = new HashSet<String>();
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("--data")) {
				data = new File(args[++i]);
			} else if (args[i].equals("--out")) {
				out = new File(args[++i]);
			} else if (args[i].equals("--quick")) {
				s = Settings.quick();
			} else if (args[i].startsWith("--")) {
				usage("unknown option " + args[i]);
				return;
			} else {
				experiments.addAll(Arrays.asList(args[i].split(",")));
			}
		}
		if (experiments.isEmpty() || experiments.contains("all")) {
			experiments.addAll(Arrays.asList("perplexity", "twc-series", "classification", "performance"));
		}
		experiments.remove("all");
		for (String e : experiments) {
			if (!Arrays.asList("perplexity", "twc-series", "classification", "performance").contains(e)) {
				usage("unknown experiment " + e);
				return;
			}
		}
		out.mkdirs();

		if (experiments.contains("perplexity")) {
			PerplexityExperiment.run("twc", Datasets.twc(s.twcDocs).split, s,
					new File(out, "perplexity-twc.csv"), true);
			if (Datasets.hasNips(data)) {
				PerplexityExperiment.run("nips", Datasets.nips(data), s,
						new File(out, "perplexity-nips.csv"), false);
			} else {
				skip("perplexity-nips", "nips");
			}
		}
		if (experiments.contains("twc-series")) {
			TwcSeriesExperiment.runDocs(s, out);
			TwcSeriesExperiment.runAlpha(s, out);
		}
		if (experiments.contains("classification")) {
			if (Datasets.hasReuters(data)) {
				ClassificationExperiment.run(Datasets.reuters(data), s,
						new File(out, "classification-reuters.csv"), true);
			} else {
				skip("classification-reuters", "reuters21578");
			}
			if (Datasets.hasTwentyNG(data)) {
				ClassificationExperiment.run(Datasets.twentyNG(data), s,
						new File(out, "classification-20ng.csv"), true);
			} else {
				skip("classification-20ng", "20news-18828");
			}
		}
		if (experiments.contains("performance")) {
			PerformanceExperiment.run("twc", Datasets.twc(Math.max(s.twcDocs, 3000)).split.train, 1, s,
					new File(out, "performance-twc.csv"));
			if (Datasets.hasReuters(data)) {
				PerformanceExperiment.run("reuters", Datasets.reutersTraining(data), 3, s,
						new File(out, "performance-reuters.csv"));
			} else {
				skip("performance-reuters", "reuters21578");
			}
			if (Datasets.hasTwentyNG(data)) {
				PerformanceExperiment.run("20ng", Datasets.twentyNGAll(data), 5, s,
						new File(out, "performance-20ng.csv"));
			} else {
				skip("performance-20ng", "20news-18828");
			}
		}
	}

	private static void skip(String experiment, String dir) {
		System.err.println("Skipping " + experiment + ": no data in <data>/" + dir
				+ " (run scripts/download-data.sh)");
	}

	private static void usage(String error) {
		System.err.println(error);
		System.err.println("usage: [perplexity|twc-series|classification|performance|all][,...] [--data DIR] [--out DIR] [--quick]");
	}
}
