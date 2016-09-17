package org.hhn.topicgrouper.paper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Random;

import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.doc.impl.HoldOutSplitter;
import org.hhn.topicgrouper.eval.APParser;
import org.hhn.topicgrouper.lda.impl.LDAGibbsSampler;
import org.hhn.topicgrouper.lda.report.BasicLDAResultReporter;
import org.hhn.topicgrouper.lda.validation.AbstractLDAPerplexityCalculator;
import org.hhn.topicgrouper.lda.validation.LDAPerplexityCalculatorWithFoldIn;
import org.hhn.topicgrouper.tg.TGSolution;
import org.hhn.topicgrouper.tg.TGSolutionListener;
import org.hhn.topicgrouper.tg.impl.AbstractTopicGrouper;
import org.hhn.topicgrouper.tg.impl.TopicGrouperWithTreeSet;

public class APExtractPerplexityNTopics extends TWCPerplexityErrorRateNDocs {
	protected final DocumentProvider<String> apExtractDocumentProvider;
	protected double[] tgPerplexityPerNTopics;
	protected int maxTopicsToReport;
	protected HoldOutSplitter<String> holdOutSplitter;

	public APExtractPerplexityNTopics(Random random) {
		super(random);
		apExtractDocumentProvider = new APParser(false, true)
				.getCorpusDocumentProvider(new File(
						"src/test/resources/ap-corpus/extract/ap.txt"));
	}

	@Override
	public void run(int gibbsIterations, int steps, int avgC)
			throws IOException {
		tgPerplexityPerNTopics = new double[nTopicFromStep(steps)];
		super.run(gibbsIterations, steps, avgC);
	}

	protected int nTopicFromStep(int step) {
		return (step + 1) * 10;
	}

	@Override
	protected HoldOutSplitter<String> createHoldoutSplitter(int step,
			DocumentProvider<String> documentProvider) {
		// Use always the same hold out splitter at every step.
		if (holdOutSplitter == null) {
			holdOutSplitter = new HoldOutSplitter<String>(random, documentProvider, 0.1, 1);
		}
		return holdOutSplitter;
	}

	@Override
	protected DocumentProvider<String> createDocumentProvider(int step) {
		return apExtractDocumentProvider;
	}

	@Override
	protected LDAGibbsSampler<String> createGibbsSampler(int step,
			DocumentProvider<String> documentProvider) {
		int topics = nTopicFromStep(step);
		return new LDAGibbsSampler<String>(documentProvider,
				LDAGibbsSampler.symmetricAlpha(createAlpha(topics), topics),
				createBeta(topics), random);
	}

	// Like in: http://psiexp.ss.uci.edu/research/papers/sciencetopics.pdf
	// and
	// http://stats.stackexchange.com/questions/59684/what-are-typical-values-to-use-for-alpha-and-beta-in-latent-dirichlet-allocation
	protected double createAlpha(int topics) {
		return 50.d / topics;
	}

	// Like in: http://psiexp.ss.uci.edu/research/papers/sciencetopics.pdf
	// and
	// http://stats.stackexchange.com/questions/59684/what-are-typical-values-to-use-for-alpha-and-beta-in-latent-dirichlet-allocation
	protected double createBeta(int topics) {
		return 0.1;
	}

	@Override
	protected PrintStream prepareLDAPrintStream() throws IOException {
		PrintStream pw = new PrintStream(new FileOutputStream(new File(
				"./target/APExtractPerplexityNTopicsLDA.csv")));

		pw.print("ntopics;");
		pw.print("perplexity;");
		pw.println("perplexityFoldIn;");

		return pw;
	}

	@Override
	protected PrintStream prepareTGPrintStream() throws IOException {
		PrintStream pw = new PrintStream(new FileOutputStream(new File(
				"./target/APExtractPerplexityNTopicsTG.csv")));
		pw.print("ntopics;");
		pw.println("perplexity;");
		return pw;
	}

	@Override
	protected PrintStream prepareTGLikelihoodPrintStream() throws IOException {
		return null;
	}

	@Override
	protected void runTopicGrouper(final PrintStream pw3, final int step,
			final int repeat, final DocumentProvider<String> documentProvider,
			final DocumentProvider<String> testDocumentProvider,
			final double[] tgPerplexity, final double[] tgAcc) {
		if (step == 0 && repeat == 0) {
			AbstractTopicGrouper<String> topicGrouper = new TopicGrouperWithTreeSet<String>(
					1, documentProvider, 1);
			topicGrouper.solve(new TGSolutionListener<String>() {
				@Override
				public void updatedSolution(int newTopicIndex,
						int oldTopicIndex, double improvement, int t1Size,
						int t2Size, final TGSolution<String> solution) {
					int topics = solution.getNumberOfTopics();
					if (topics <= tgPerplexityPerNTopics.length) {
						tgPerplexityPerNTopics[solution.getNumberOfTopics() - 1] = computeTGPerplexity(
								solution, testDocumentProvider);
					}
				}

				@Override
				public void initialized(TGSolution<String> initialSolution) {
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
			});
		}
	}

	@Override
	protected void runLDAGibbsSampler(int step, int repeat,
			int gibbsIterations, DocumentProvider<String> documentProvider,
			DocumentProvider<String> testDocumentProvider,
			double[] perplexity1, double[] perplexity2, double[] acc) {
		if (repeat == 0) {
			final LDAGibbsSampler<String> gibbsSampler = createGibbsSampler(
					step, documentProvider);

			gibbsSampler.solve(gibbsIterations,
					new BasicLDAResultReporter<String>(System.out, 10));
			AbstractLDAPerplexityCalculator<String> calc2 = new LDAPerplexityCalculatorWithFoldIn<String>(
					false, gibbsIterations);

			perplexity1[0] = calc1.computePerplexity(testDocumentProvider,
					gibbsSampler);

			perplexity2[0] = calc2.computePerplexity(testDocumentProvider,
					gibbsSampler);
		}
	}

	@Override
	protected void aggregateLDAResults(PrintStream pw, int step,
			double[] perplexity1, double[] perplexity2, double[] acc) {
		pw.print(nTopicFromStep(step));
		pw.print("; ");
		pw.print(perplexity1[0]);
		pw.print("; ");
		pw.print(perplexity2[0]);
		pw.println("; ");
	}

	@Override
	protected void aggregateTGResults(PrintStream pw, int step,
			double[] tgPerplexity, double[] tgAcc) {
		if (step == 0) {
			for (int i = 0; i < tgPerplexityPerNTopics.length; i++) {
				pw.print(i + 1);
				pw.print("; ");
				pw.print(tgPerplexityPerNTopics[i]);
				pw.println("; ");
			}
		}
	}

	public static void main(String[] args) throws IOException {
		new APExtractPerplexityNTopics(new Random(42)).run(1000, 20, 1);
	}
}
