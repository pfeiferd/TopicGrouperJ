package org.hhn.topicgrouper.paper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Random;

import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.eval.TWCLDAPaperDocumentGenerator;
import org.hhn.topicgrouper.tg.TGSolution;
import org.hhn.topicgrouper.tg.TGSolutionListener;
import org.hhn.topicgrouper.tg.impl.AbstractTopicGrouper;
import org.hhn.topicgrouper.tg.impl.TopicGrouperWithTreeSet;
import org.hhn.topicgrouper.util.MathExt;

public abstract class DeferredJCComputationDependency<T> {
	public void run(int steps, int avgC) throws IOException {
		PrintStream pw = new PrintStream(new FileOutputStream(new File(
				"./target/" + getFileName() + ".csv")));

		pw.print("nwords;");
		pw.print("ndeferredjccs;");
		pw.println("err;");

		for (int i = 0; i < steps; i++) {
			final double[] tgDeferredJCCs = new double[avgC];
			final double[] nWords = new double[avgC];
			final int[] counter = new int[1];
			for (int j = 0; j < avgC; j++) {
				counter[0] = j;
				DocumentProvider<T> documentProvider = createDocumentProvider(i);
				nWords[j] = documentProvider.getNumberOfWords();
				final AbstractTopicGrouper<T> topicGrouper = new TopicGrouperWithTreeSet<T>(
						1, documentProvider, 1);
				topicGrouper.solve(new TGSolutionListener<T>() {
					@Override
					public void updatedSolution(int newTopicIndex,
							int oldTopicIndex, double improvement, int t1Size,
							int t2Size, final TGSolution<T> solution) {
					}

					@Override
					public void initialized(TGSolution<T> initialSolution) {
					}

					@Override
					public void initalizing(double percentage) {
					}

					@Override
					public void done() {
						tgDeferredJCCs[counter[0]] = topicGrouper
								.getDeferredJCRecomputations();

					}

					@Override
					public void beforeInitialization(int maxTopics,
							int documents) {
					}
				});
			}

			double tgDeferredJCCsAvg = MathExt.avg(tgDeferredJCCs);
			double tgDeferredJCCsStdDev = MathExt.sampleStdDev(
					tgDeferredJCCsAvg, tgDeferredJCCs);
			pw.print(MathExt.avg(nWords));
			pw.print("; ");
			pw.print(tgDeferredJCCsAvg);
			pw.print("; ");
			pw.print(tgDeferredJCCsStdDev);
			pw.println("; ");
		}
		pw.close();
	}

	protected abstract DocumentProvider<T> createDocumentProvider(int step);
	
	protected abstract String getFileName();

	public static void main(String[] args) throws IOException {
		final int[] wordSizesPerTopics = new int[] { 10, 20, 50, 100, 200, 400,
				800 };
		final Random random = new Random(10);

		new DeferredJCComputationDependency<String>() {
			protected org.hhn.topicgrouper.doc.DocumentProvider<String> createDocumentProvider(
					int step) {
				return new TWCLDAPaperDocumentGenerator(random,
						new double[] { 5, 0.5, 0.5, 0.5 }, 6000,
						wordSizesPerTopics[step], wordSizesPerTopics[step], 30,
						30, 0, null, 0.5, 0.8);
			};
			
			@Override
			protected String getFileName() {
				return "DeferredJCComputationDependencyTWC";
			}
		}.run(wordSizesPerTopics.length, 10);
	}
}
