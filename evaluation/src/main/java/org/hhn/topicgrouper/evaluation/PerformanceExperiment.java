package org.hhn.topicgrouper.evaluation;

import java.io.File;
import java.util.Random;

import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.doc.impl.HoldOutSplitter;
import org.hhn.topicgrouper.tg.TGSolution;
import org.hhn.topicgrouper.tg.TGSolutionListener;
import org.hhn.topicgrouper.tg.TGSolver;
import org.hhn.topicgrouper.tg.impl.EHACTopicGrouper;
import org.hhn.topicgrouper.tg.impl.LowMemTopicGrouper;
import org.hhn.topicgrouper.util.MathExt;

/**
 * Run time of Topic Grouper depending on the number of training documents, for
 * the heap based solver (EHAC) and the low memory solver.
 */
public final class PerformanceExperiment {
	private static final int DOCS_PER_STEP = 200;

	private PerformanceExperiment() {
	}

	/**
	 * Writes <code>performance-&lt;name&gt;.csv</code> with the columns
	 * <code>solver;docs;avgSeconds;stdDevSeconds;avgWords</code>.
	 * 
	 * @param name
	 *            name of the data set
	 * @param provider
	 *            all documents; random subsets of growing size are drawn from it
	 * @param minWordFrequency
	 *            words occurring less often in a subset are ignored
	 * @param s
	 *            experiment size
	 * @param out
	 *            result file
	 */
	public static void run(String name, DocumentProvider<String> provider,
			int minWordFrequency, Settings s, File out) {
		Csv csv = new Csv(out, "solver", "docs", "avgSeconds", "stdDevSeconds", "avgWords");
		try {
			for (String solver : new String[] { "ehac", "lowmem" }) {
				Random random = new Random(43);
				for (int step = 0; step < s.performanceSteps; step++) {
					int docs = (step + 1) * DOCS_PER_STEP;
					if (docs > provider.getDocuments().size()) {
						break;
					}
					double[] seconds = new double[s.performanceTries];
					double[] words = new double[s.performanceTries];
					for (int t = 0; t < s.performanceTries; t++) {
						DocumentProvider<String> subset = new HoldOutSplitter<String>(
								random, provider,
								1 - docs / (double) provider.getDocuments().size(),
								minWordFrequency).getRest();
						words[t] = subset.getVocab().getNumberOfWords();
						seconds[t] = time(solver, subset, minWordFrequency);
					}
					csv.row(solver, docs, MathExt.avg(seconds),
							seconds.length > 1 ? MathExt.sampleStdDev(seconds) : 0, MathExt.avg(words));
				}
			}
		} finally {
			csv.close();
		}
	}

	private static double time(String solver, DocumentProvider<String> docs, int minWordFrequency) {
		TGSolver<String> tg = solver.equals("ehac")
				? new EHACTopicGrouper<String>(1, docs, 1)
				: new LowMemTopicGrouper<String>(1, docs, 1);
		long start = System.nanoTime();
		tg.solve(new TGSolutionListener<String>() {
			public void updatedSolution(int newTopicIndex, int oldTopicIndex,
					double improvement, int t1Size, int t2Size, TGSolution<String> solution) {
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
}
