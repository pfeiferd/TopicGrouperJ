package org.hhn.topicgrouper.paper.jcupdates;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Random;

import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.doc.impl.HoldOutSplitter;
import org.hhn.topicgrouper.eval.APParser;
import org.hhn.topicgrouper.eval.Reuters21578;
import org.hhn.topicgrouper.eval.TWCLDAPaperDocumentGenerator;
import org.hhn.topicgrouper.tg.TGSolution;
import org.hhn.topicgrouper.tg.TGSolutionListener;
import org.hhn.topicgrouper.tg.impl.AbstractTopicGrouper;
import org.hhn.topicgrouper.tg.impl.TopicGrouperWithTreeSet;
import org.hhn.topicgrouper.util.MathExt;

public class JCUpdatesByVocabSize {
	public void runAll() throws IOException {
		final Random random = new Random(10);

		final int[] wordSizesPerTopics = new int[] { 10, 20, 50, 100, 200, 400,
				800 };

		JCCsRunner<String> r1 = new JCCsRunner<String>() {
			protected org.hhn.topicgrouper.doc.DocumentProvider<String> createDocumentProvider(
					int step) {
				return new TWCLDAPaperDocumentGenerator(random, new double[] {
						5, 0.5, 0.5, 0.5 }, 6000, wordSizesPerTopics[step],
						wordSizesPerTopics[step], 30, 30, 0, null, 0.5, 0.8);
			};

			@Override
			protected String getFileName() {
				return "JCUpdatesTWC";
			}
		};
		r1.run(wordSizesPerTopics.length, 10, true);
		r1.run(wordSizesPerTopics.length, 10, false);

		final double[] holdOutRatios = new double[] { 0.01, 0.05, 0.1, 0.2,
				0.5, 1 };
		final DocumentProvider<String> reutersDocumentProvider = new Reuters21578(
				false).getCorpusDocumentProvider(new File(
				"src/test/resources/reuters21578"), false, true);
		JCCsRunner<String> r2 = new JCCsRunner<String>() {
			protected org.hhn.topicgrouper.doc.DocumentProvider<String> createDocumentProvider(
					int step) {
				return new HoldOutSplitter<String>(random,
						reutersDocumentProvider, holdOutRatios[step], 1)
						.getHoldOut();
			}

			@Override
			protected void aggregateResults(PrintStream pw, int[] nWords,
					int[] tgDeferredJCCs, long[] durationMs, int[] nDocs) {
				aggregateResultsNone(pw, nWords, tgDeferredJCCs, durationMs,
						nDocs);
			}

			@Override
			protected String getFileName() {
				return "JCUpdatesReuters";
			}
		};
		r2.run(holdOutRatios.length, 10, true);
		r2.run(holdOutRatios.length, 10, false);

		final DocumentProvider<String> apExtractDocumentProvider = new APParser(
				true, true).getCorpusDocumentProvider(new File(
				"src/test/resources/ap-corpus/extract/ap.txt"));
		JCCsRunner<String> r3 = new JCCsRunner<String>() {
			protected org.hhn.topicgrouper.doc.DocumentProvider<String> createDocumentProvider(
					int step) {
				return new HoldOutSplitter<String>(random,
						apExtractDocumentProvider, holdOutRatios[step], 1)
						.getHoldOut();
			}

			@Override
			protected void aggregateResults(PrintStream pw, int[] nWords,
					int[] tgDeferredJCCs, long[] durationMs, int[] nDocs) {
				aggregateResultsNone(pw, nWords, tgDeferredJCCs, durationMs,
						nDocs);
			}

			@Override
			protected String getFileName() {
				return "JCUpdatesAPExtract";
			}
		};
		r3.run(holdOutRatios.length, 10, true);
		r3.run(holdOutRatios.length, 10, false);
	}

	public static abstract class JCCsRunner<T> {
		public void run(int steps, int avgC, boolean deferredUpdates)
				throws IOException {
			if (!new File("./target/jcupdates").exists()) {
				new File("./target/jcupdates").mkdir();
			}
			
			PrintStream pw = new PrintStream(new FileOutputStream(new File(
					"./target/jcupdates/" + getFileName()
							+ (deferredUpdates ? "_deferredUpdates" : "")
							+ ".csv")));

			pw.println("nwords;njcupdates;err;durationms;durationms_err;ndocs;");

			for (int i = 0; i < steps; i++) {
				System.out.print("Step: ");
				System.out.println(i);
				final int[] jvUpdates = new int[avgC];
				final int[] nWords = new int[avgC];
				final long[] durationMs = new long[avgC];
				final int[] nDocs = new int[avgC];
				final int[] counter = new int[1];
				for (int j = 0; j < avgC; j++) {
					System.out.print("Avg run: ");
					System.out.println(j);
					counter[0] = j;
					DocumentProvider<T> documentProvider = createDocumentProvider(i);
					nWords[j] = documentProvider.getVocab().getNumberOfWords();
					nDocs[j] = documentProvider.getDocuments().size();
					final AbstractTopicGrouper<T> topicGrouper = new TopicGrouperWithTreeSet<T>(
							1, documentProvider, 1);
					if (!deferredUpdates) {
						topicGrouper.setDeferJCUpdates(false);
					}
					long startTime = System.currentTimeMillis();
					topicGrouper.solve(new TGSolutionListener<T>() {
						@Override
						public void updatedSolution(int newTopicIndex,
								int oldTopicIndex, double improvement,
								int t1Size, int t2Size,
								final TGSolution<T> solution) {
						}

						@Override
						public void initialized(TGSolution<T> initialSolution) {
						}

						@Override
						public void initalizing(double percentage) {
						}

						@Override
						public void done() {
							jvUpdates[counter[0]] = topicGrouper.getJCUpdates();

						}

						@Override
						public void beforeInitialization(int maxTopics,
								int documents) {
							nWords[counter[0]] = maxTopics;
							System.out.print("Number of Words: ");
							System.out.println(maxTopics);
						}
					});
					durationMs[j] = System.currentTimeMillis() - startTime;
				}
				aggregateResults(pw, nWords, jvUpdates, durationMs, nDocs);
			}
			pw.close();
		}

		protected void aggregateResultsNone(PrintStream pw, int[] nWords,
				int[] tgDeferredJCCs, long[] durationMs, int[] nDocs) {
			for (int i = 0; i < nWords.length; i++) {
				pw.print(nWords[i]);
				pw.print("; ");
				pw.print(tgDeferredJCCs[i]);
				pw.print("; ");
				pw.print(0);
				pw.print("; ");
				pw.print(durationMs[i]);
				pw.print("; ");
				pw.print(0);
				pw.print("; ");
				pw.print(nDocs[i]);
				pw.println("; ");
			}
		}

		protected void aggregateResultsAvg(PrintStream pw, int[] nWords,
				int[] tgDeferredJCCs, long[] durationMs, int[] nDocs) {
			double tgDeferredJCCsAvg = MathExt.avg(tgDeferredJCCs);
			double tgDeferredJCCsStdDev = MathExt.sampleStdDev(
					tgDeferredJCCsAvg, tgDeferredJCCs);
			pw.print(MathExt.avg(nWords));
			pw.print("; ");
			pw.print(tgDeferredJCCsAvg);
			pw.print("; ");
			pw.print(tgDeferredJCCsStdDev);
			pw.print("; ");
			pw.print(MathExt.avg(durationMs));
			pw.print("; ");
			pw.print(MathExt.sampleStdDev(durationMs));
			pw.print("; ");
			pw.print(MathExt.avg(nDocs));
			pw.println("; ");
		}

		protected void aggregateResults(PrintStream pw, int[] nWords,
				int[] tgDeferredJCCs, long[] durationMs, int[] nDocs) {
			aggregateResultsAvg(pw, nWords, tgDeferredJCCs, durationMs, nDocs);
		}

		protected abstract DocumentProvider<T> createDocumentProvider(int step);

		protected abstract String getFileName();
	}

	public static void main(String[] args) throws IOException {
		new JCUpdatesByVocabSize().runAll();
	}
}
