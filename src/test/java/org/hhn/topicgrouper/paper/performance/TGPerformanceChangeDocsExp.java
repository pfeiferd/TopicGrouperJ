package org.hhn.topicgrouper.paper.performance;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Random;

import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.doc.impl.HoldOutSplitter;
import org.hhn.topicgrouper.tg.TGSolution;
import org.hhn.topicgrouper.tg.TGSolutionListener;
import org.hhn.topicgrouper.tg.TGSolver;
import org.hhn.topicgrouper.tg.impl.EHACTopicGrouper;
import org.hhn.topicgrouper.util.MathExt;

public abstract class TGPerformanceChangeDocsExp<T> {
	protected final DocumentProvider<T> provider;
	protected final int minWordFrequency;

	public TGPerformanceChangeDocsExp(int minWordFrequency) {
		this.minWordFrequency = minWordFrequency;
		provider = createBasicProvider();
	}

	protected abstract DocumentProvider<T> createBasicProvider();

	protected DocumentProvider<T> createProvider(Random random, int trie,
			int documents) {
		int size = provider.getDocuments().size();
		if (size < documents) {
			return null;
		}
		HoldOutSplitter<T> splitter = new HoldOutSplitter<T>(random, provider,
				1 - documents / ((double) size), minWordFrequency);
		return splitter.getRest();
	}

	protected int docsForStep(int step) {
		return (int) ((step + 1) * 200);
	}

	protected long runSolver(DocumentProvider<T> documentProvider) {
		long start = System.currentTimeMillis();
		TGSolver<T> topicGrouper = createSolver(documentProvider);

		TGSolutionListener<T> tgSolutionListener = new TGSolutionListener<T>() {
			@Override
			public void updatedSolution(int newTopicIndex, int oldTopicIndex,
					double improvement, int t1Size, int t2Size,
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
			}

			@Override
			public void beforeInitialization(int maxTopics, int documents) {
			}
		};
		topicGrouper.solve(tgSolutionListener);
		return System.currentTimeMillis() - start;
	}

	protected TGSolver<T> createSolver(DocumentProvider<T> documentProvider) {
		return new EHACTopicGrouper<T>(1, documentProvider, 1);
	}

	public void printResult(long[][] timesSec, int[][] docs, int[][] words) {
		try {
			PrintStream printStream = new PrintStream(
					new FileOutputStream(new File("./target/"
							+ getClass().getSimpleName() + ".csv")));
			printStream.println("step; docs; avgTimeSec; stdDevTimeSec; avgDocs; stdDevDocs; avgWords; stdDevWords");
			for (int i = 0; i < timesSec.length; i++) {
				printStream.print(i + 1);
				printStream.print("; ");
				printStream.print(docsForStep(i));
				printStream.print("; ");
				printStream.print(MathExt.avg(timesSec[i]));
				printStream.print("; ");
				printStream.print(MathExt.sampleStdDev(timesSec[i]));
				printStream.print("; ");
				printStream.print(MathExt.avg(docs[i]));
				printStream.print("; ");
				printStream.print(MathExt.sampleStdDev(docs[i]));
				printStream.print("; ");
				printStream.print(MathExt.avg(words[i]));
				printStream.print("; ");
				printStream.print(MathExt.sampleStdDev(words[i]));
				printStream.println();
			}
			printStream.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	protected void runExperiment(Random random, int tries, int maxSteps) {
		final long[][] timesSec = new long[maxSteps][tries];
		final int[][] docs = new int[maxSteps][tries];
		final int[][] words = new int[maxSteps][tries];

		for (int i = 0; i < maxSteps; i++) {
			for (int j = 0; j < tries; j++) {
				DocumentProvider<T> documentProvider = createProvider(random,
						j, docsForStep(i));
				if (documentProvider != null) {
					System.out.println("Try: " + j + " Documents: "
							+ documentProvider.getDocuments().size());
					timesSec[i][j] = runSolver(documentProvider) / 1000;
					docs[i][j] = documentProvider.getDocuments().size();
					words[i][j] = documentProvider.getVocab().getNumberOfWords();
				}
			}
		}
		printResult(timesSec, docs, words);
	}
}
