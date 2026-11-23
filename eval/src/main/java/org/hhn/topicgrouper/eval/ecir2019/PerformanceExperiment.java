package org.hhn.topicgrouper.eval.ecir2019;

import java.io.File;
import java.io.IOException;
import java.util.Random;

import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.doc.impl.HoldOutSplitter;
import org.hhn.topicgrouper.eval.Csv;
import org.hhn.topicgrouper.eval.Figures;
import org.hhn.topicgrouper.eval.Settings;
import org.hhn.topicgrouper.tg.TGSolution;
import org.hhn.topicgrouper.tg.TGSolutionListener;
import org.hhn.topicgrouper.tg.TGSolver;
import org.hhn.topicgrouper.tg.impl.EHACTopicGrouper;
import org.hhn.topicgrouper.tg.impl.LowMemTopicGrouper;
import org.hhn.topicgrouper.util.MathExt;

/**
 * Run time of the Topic Grouper solvers over the number of documents (the
 * extended version of the ECIR 2019 paper): random subsets of 200, 400, ...
 * documents of a corpus, several tries each, solved down to one topic with the
 * heap based (<code>ehac</code>) and the memory efficient (<code>lowmem</code>)
 * solver, single threaded and (<code>ehac-mt</code>, <code>lowmem-mt</code>)
 * with one thread per available processor.
 */
public class PerformanceExperiment {
	private static final int DOCS_PER_STEP = 200;

	private final Settings settings;
	private final File out;

	/**
	 * Creates the experiment.
	 *
	 * @param settings
	 *            experiment size
	 * @param out
	 *            output directory
	 */
	public PerformanceExperiment(Settings settings, File out) {
		this.settings = settings;
		this.out = out;
	}

	/**
	 * Writes <code>performance-&lt;name&gt;.csv</code> with the columns
	 * <code>solver;docs;avgSeconds;stdDevSeconds;avgWords</code>.
	 *
	 * @param name
	 *            name of the corpus
	 * @param provider
	 *            the documents
	 * @param minWordFrequency
	 *            words occurring less often in a subset are removed from it
	 */
	public void run(String name, DocumentProvider<String> provider, int minWordFrequency) {
		File file = new File(out, "performance-" + name + ".csv");
		if (Csv.skipIfDone(file)) {
			return;
		}
		Csv csv = new Csv(file, "solver", "docs", "avgSeconds", "stdDevSeconds", "avgWords");
		try {
			for (String solver : new String[] { "ehac", "lowmem", "ehac-mt", "lowmem-mt" }) {
				Random random = new Random(43);
				for (int step = 0; step < settings.performanceSteps; step++) {
					int docs = (step + 1) * DOCS_PER_STEP;
					if (docs > provider.getDocuments().size()) {
						break;
					}
					double[] seconds = new double[settings.performanceTries];
					double[] words = new double[settings.performanceTries];
					for (int t = 0; t < settings.performanceTries; t++) {
						DocumentProvider<String> subset = new HoldOutSplitter<String>(random, provider,
								1 - docs / (double) provider.getDocuments().size(), minWordFrequency).getRest();
						words[t] = subset.getVocab().getNumberOfWords();
						seconds[t] = time(solver, subset);
					}
					csv.row(solver, docs, MathExt.avg(seconds),
							seconds.length > 1 ? MathExt.sampleStdDev(seconds) : 0, MathExt.avg(words));
				}
			}
		} catch (RuntimeException | Error e) {
			csv.discard();
			throw e;
		}
		csv.close();
		plot(file);
	}

	private double time(String solver, DocumentProvider<String> docs) {
		int threads = solver.endsWith("-mt") ? Runtime.getRuntime().availableProcessors() : 1;
		TGSolver<String> tg = solver.startsWith("ehac") ? new EHACTopicGrouper<String>(1, docs, 1, threads)
				: new LowMemTopicGrouper<String>(1, docs, 1, threads);
		long start = System.nanoTime();
		tg.solve(new TGSolutionListener<String>() {
			public void updatedSolution(int newTopicIndex, int oldTopicIndex, double improvement,
					int t1Size, int t2Size, TGSolution<String> solution) {
			}

			public void beforeInitialization(int maxTopics, int documents) {
			}

			public void initalizing(double percentage) {
			}

			public void initialized(TGSolution<String> initialSolution) {
			}

			public void done() {
			}
		});
		return (System.nanoTime() - start) / 1e9;
	}

	/**
	 * Draws the figures of a finished CSV file into <code>figs</code>.
	 *
	 * @param csv
	 *            the CSV file
	 */
	private void plot(File csv) {
		try {
			new Figures(out).plot(csv);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
