package org.hhn.topicgrouper.eval;

import org.hhn.topicgrouper.doc.DocumentProvider;
import java.io.File;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hhn.topicgrouper.eval.ecir2019.PerformanceExperiment;
import org.hhn.topicgrouper.eval.ecir2019.PerplexityExperiment;
import org.hhn.topicgrouper.eval.ecir2019.PerplexityExperiment.Corpus;
import org.hhn.topicgrouper.eval.ecir2019.TopicExamples;
import org.hhn.topicgrouper.eval.ecir2019.TwcSeriesExperiment;
import org.hhn.topicgrouper.eval.featurereduction.ClassificationExperiment;
import org.hhn.topicgrouper.eval.homonyms.HomonymExperiment;

/**
 * The experiments of the papers on the data sets of a data directory, results
 * into an output directory. The ECIR 2019 paper (<code>ecir2019</code>)
 * consists of <code>perplexity</code> (hold-out perplexity over the number of
 * topics on the corpora of the paper), <code>twc-series</code> (perplexity and
 * error rate on the synthetic corpus over LDA's prior and over the number of
 * documents), <code>performance</code> (run time over the number of documents)
 * and <code>topics</code> (top words and mind maps); the feature reduction
 * paper (<code>feature-reduction</code>) of <code>classification</code>; and
 * <code>homonyms</code> of the homonym experiment. Experiments whose data set
 * is missing in the data directory are skipped with a message.
 */
public class Evaluation {
	private static final Map<String, List<String>> PAPERS = new LinkedHashMap<String, List<String>>();
	static {
		PAPERS.put("ecir2019", Arrays.asList("perplexity", "twc-series", "performance", "topics"));
		PAPERS.put("feature-reduction", Arrays.asList("classification"));
		PAPERS.put("homonyms", Arrays.asList("homonyms"));
	}

	private final Datasets data;
	private final Settings settings;
	private final File out;

	/**
	 * Creates the evaluation on the given data sets.
	 *
	 * @param data
	 *            the data sets
	 * @param settings
	 *            experiment size
	 * @param out
	 *            output directory
	 */
	public Evaluation(Datasets data, Settings settings, File out) {
		this.data = data;
		this.settings = settings;
		this.out = out;
	}

	/**
	 * Lists the papers (in order) with the names of their experiments.
	 *
	 * @return the papers and their experiments
	 */
	public static Map<String, List<String>> getPapers() {
		return PAPERS;
	}

	/**
	 * Resolves a paper or experiment name to experiment names.
	 *
	 * @param name
	 *            an experiment or paper name
	 * @return the experiments it stands for, empty if unknown
	 */
	public static List<String> experimentsOf(String name) {
		if (PAPERS.containsKey(name)) {
			return PAPERS.get(name);
		}
		for (List<String> e : PAPERS.values()) {
			if (e.contains(name)) {
				return Arrays.asList(name);
			}
		}
		return Arrays.asList();
	}

	/**
	 * Runs one experiment.
	 *
	 * @param experiment
	 *            its name
	 */
	public void run(String experiment) {
		out.mkdirs();
		if (experiment.equals("perplexity")) {
			runPerplexity();
		} else if (experiment.equals("twc-series")) {
			TwcSeriesExperiment series = new TwcSeriesExperiment(data, settings, out);
			series.runAlpha();
			series.runDocs();
		} else if (experiment.equals("performance")) {
			runPerformance();
		} else if (experiment.equals("topics")) {
			runTopics();
		} else if (experiment.equals("classification")) {
			runClassification();
		} else if (experiment.equals("homonyms")) {
			new HomonymExperiment(data, settings, out).run();
		} else {
			throw new IllegalArgumentException("unknown experiment " + experiment);
		}
	}

	private void runPerplexity() {
		PerplexityExperiment e = new PerplexityExperiment(settings, out);
		e.run(new Corpus("twc", data.twc(settings.twcDocs).split).withPlsa());
		if (data.hasNips()) {
			e.run(new Corpus("nips", data.nips()).tgAlpha(4).particles(5));
		} else {
			skip("perplexity-nips", "nips");
		}
		if (data.hasApExtract()) {
			e.run(new Corpus("ap-extract", data.apExtractPerplexity()).tgAlpha(3).withPlsa());
		} else {
			skip("perplexity-ap-extract", "ap");
		}
		if (data.hasApFull()) {
			e.run(new Corpus("ap-full", data.apFullPerplexity(settings.apFullDocs)).tgAlpha(4).particles(5)
					.optimizeShare(0.01));
		} else {
			skip("perplexity-ap-full", "ap-full");
		}
		if (data.hasOnlineRetail()) {
			e.run(new Corpus("online-retail", data.onlineRetailPerplexity()).tgAlpha(4));
		} else {
			skip("perplexity-online-retail", "online-retail");
		}
		if (data.hasTaFeng()) {
			e.run(new Corpus("tafeng", data.taFengPerplexity()).tgAlpha(3));
		} else {
			skip("perplexity-tafeng", "tafeng");
		}
		if (data.hasUsSupermarket()) {
			e.run(new Corpus("us-supermarket", data.usSupermarketPerplexity()).tgAlpha(3).topicStep(20));
		} else {
			skip("perplexity-us-supermarket", "us-supermarket");
		}
	}

	private void runPerformance() {
		PerformanceExperiment e = new PerformanceExperiment(settings, out);
		e.run("twc", data.twc(Math.max(settings.twcDocs, 3000)).split.train, 1);
		if (data.hasApExtract()) {
			e.run("ap-extract", data.apExtract(false), 10);
		} else {
			skip("performance-ap-extract", "ap");
		}
		if (data.hasReuters()) {
			e.run("reuters", data.reutersTraining(), 3);
		} else {
			skip("performance-reuters", "reuters21578");
		}
		if (data.hasTwentyNG()) {
			e.run("20ng", data.twentyNGAll(), 5);
		} else {
			skip("performance-20ng", "20news-18828");
		}
	}

	private void runTopics() {
		TopicExamples e = new TopicExamples(settings, out);
		e.run("twc", data.twc(settings.twcDocs).split.train);
		if (data.hasApExtract()) {
			e.run("ap-extract", data.apExtract(true), 20);
		} else {
			skip("topics-ap-extract", "ap");
		}
		if (data.hasApFull()) {
			e.run("ap-full", data.apFull(settings.apFullDocs), 20);
		} else {
			skip("topics-ap-full", "ap-full");
		}
		if (data.hasReuters()) {
			e.run("reuters", data.reutersTraining());
			// Homonym handling: the sense splitting solver is heap based (memory
			// quadratic in the vocabulary), hence the minimum word frequency.
			DocumentProvider<String> reuters = data.reutersTraining();
			e.run("reuters-sense", reuters, 5, new TopicGrouperFactory().createSenseSplitting(reuters, 5), true);
		} else {
			skip("topics-reuters", "reuters21578");
		}
		if (data.hasNips()) {
			e.run("nips", data.nipsAll(), 5);
		} else {
			skip("topics-nips", "nips");
		}
		if (data.hasNytimes()) {
			e.run("nytimes", data.nytimes(10000), 100);
		} else {
			skip("topics-nytimes", "nytimes");
		}
		if (data.hasOnlineRetail()) {
			e.run("online-retail", data.onlineRetail(1000), 10);
		} else {
			skip("topics-online-retail", "online-retail");
		}
	}

	private void runClassification() {
		ClassificationExperiment e = new ClassificationExperiment(settings, out);
		if (data.hasReuters()) {
			e.run("reuters", data.reuters(), true);
		} else {
			skip("classification-reuters", "reuters21578");
		}
		if (data.hasTwentyNG()) {
			e.run("20ng", data.twentyNG(), true);
		} else {
			skip("classification-20ng", "20news-18828");
		}
		if (data.hasOhsumed()) {
			e.run("ohsumed", data.ohsumed(), true);
		} else {
			skip("classification-ohsumed", "ohsumed");
		}
	}

	private void skip(String experiment, String dir) {
		System.err.println("Skipping " + experiment + ": no data in " + new File(data.getDataDir(), dir)
				+ " (see README.md)");
	}
}
