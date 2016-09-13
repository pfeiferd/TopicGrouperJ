package org.hhn.topicgrouper.paper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Random;

import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.doc.impl.HoldOutSplitter;
import org.hhn.topicgrouper.eval.APParser;
import org.hhn.topicgrouper.eval.TWCLDAPaperDocumentGenerator;
import org.hhn.topicgrouper.lda.impl.LDAGibbsSampler;
import org.hhn.topicgrouper.lda.report.BasicLDAResultReporter;
import org.hhn.topicgrouper.lda.validation.AbstractLDAPerplexityCalculator;
import org.hhn.topicgrouper.lda.validation.LDAPerplexityCalculatorWithFoldIn;
import org.hhn.topicgrouper.tg.TGSolution;
import org.hhn.topicgrouper.tg.TGSolutionListener;
import org.hhn.topicgrouper.tg.impl.AbstractTopicGrouper;
import org.hhn.topicgrouper.tg.impl.TopicGrouperWithTreeSet;
import org.hhn.topicgrouper.util.MathExt;

public class APExtractPerplexityNTopics extends TWCPerplexityErrorRateNDocs {
	protected final DocumentProvider<String> apExtractDocumentProvider;
	protected double[] tgPerplexityPerNTopics;

	public APExtractPerplexityNTopics(Random random) {
		super(random);
		apExtractDocumentProvider = new APParser(false, true)
				.getCorpusDocumentProvider(new File(
						"src/test/resources/ap-corpus/extract/ap.txt"));
	}

	protected int nTopicFromStep(int step) {
		return step + 1;
	}

	@Override
	protected HoldOutSplitter<String> createHoldoutSplitter(int step,
			DocumentProvider<String> documentProvider) {
		return new HoldOutSplitter<String>(random, documentProvider, 0.3333, 1);
	}

	@Override
	protected DocumentProvider<String> createDocumentProvider(int step) {
		return apExtractDocumentProvider;
	}

	@Override
	protected LDAGibbsSampler<String> createGibbsSampler(int step,
			DocumentProvider<String> documentProvider) {
		return new LDAGibbsSampler<String>(documentProvider,
				LDAGibbsSampler.symmetricAlpha(0.5, nTopicFromStep(step)), 0.5,
				random);
	}

	@Override
	protected PrintStream prepareLDAPrintStream() throws IOException {
		PrintStream pw = new PrintStream(new FileOutputStream(new File(
				"./target/APExtractPerplexityNTopicsLDA.csv")));

		pw.print("ntopics;");
		pw.print("perplexity;");
		pw.print("perplexityFoldIn;");

		return pw;
	}

	@Override
	protected PrintStream prepareTGPrintStream() throws IOException {
		PrintStream pw = new PrintStream(new FileOutputStream(new File(
				"./target/APExtractPerplexityNTopicsTG.csv")));
		pw.print("ntopics;");
		pw.print("perplexity;");
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
					tgPerplexityPerNTopics[solution.getNumberOfTopics() - 1] = computeTGPerplexity(
							solution, testDocumentProvider);
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
					tgPerplexityPerNTopics = new double[maxTopics];
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

			perplexity1[step] = calc1.computePerplexity(testDocumentProvider,
					gibbsSampler);

			perplexity2[step] = calc2.computePerplexity(testDocumentProvider,
					gibbsSampler);
		}
	}

	@Override
	protected void aggregateLDAResults(PrintStream pw, int step,
			double[] perplexity1, double[] perplexity2, double[] acc) {
		pw.print(docsFromStep(step));
		pw.print("; ");
		pw.print(MathExt.avg(perplexity1));
		pw.print("; ");
		pw.print(MathExt.sampleStdDev(perplexity1));
		pw.print("; ");
		pw.print(MathExt.avg(perplexity2));
		pw.print("; ");
		pw.print(MathExt.sampleStdDev(perplexity2));
		pw.print("; ");
	}

	@Override
	protected void aggregateTGResults(PrintStream pw, int step,
			double[] tgPerplexity, double[] tgAcc) {
		if (step == 0) {
			for (int i = 0; i < tgPerplexityPerNTopics.length; i++) {
				pw.print(i + 1);
				pw.print("; ");
				pw.print(tgPerplexityPerNTopics[i]);
				pw.print("; ");
			}
		}
	}

	public static void main(String[] args) throws IOException {
		new APExtractPerplexityNTopics(new Random()).run(100, 10, 10);
	}
}
