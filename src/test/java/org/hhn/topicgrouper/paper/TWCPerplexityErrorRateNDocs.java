package org.hhn.topicgrouper.paper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Random;

import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.doc.impl.HoldOutSplitter;
import org.hhn.topicgrouper.eval.TWCLDAPaperDocumentGenerator;
import org.hhn.topicgrouper.lda.impl.LDAGibbsSampler;
import org.hhn.topicgrouper.tg.TGSolution;
import org.hhn.topicgrouper.tg.TGSolutionListener;
import org.hhn.topicgrouper.tg.impl.AbstractTopicGrouper;
import org.hhn.topicgrouper.tg.impl.TopicGrouperWithTreeSet;
import org.hhn.topicgrouper.util.MathExt;

public class TWCPerplexityErrorRateNDocs extends TWCPerplexityErrorRateLDAGibbs {
	public TWCPerplexityErrorRateNDocs(Random random) {
		super(random);
	}

	protected int docsFromStep(int step) {
		return 100 * (step + 1) + 3000;
	}

	@Override
	protected HoldOutSplitter<String> createHoldoutSplitter(int step,
			DocumentProvider<String> documentProvider) {
		return new HoldOutSplitter<String>(random, documentProvider, 3000, 1);
	}

	@Override
	protected DocumentProvider<String> createDocumentProvider(int step) {
		return new TWCLDAPaperDocumentGenerator(random, new double[] { 5, 0.5,
				0.5, 0.5 }, docsFromStep(step), 100, 100, 30, 30, 0, null, 0.5,
				0.8);
	}

	@Override
	protected LDAGibbsSampler<String> createGibbsSampler(int step,
			DocumentProvider<String> documentProvider) {
		return new LDAGibbsSampler<String>(documentProvider, new double[] { 5,
				0.5, 0.5, 0.5 }, 0.5, random);
	}

	@Override
	protected PrintStream prepareLDAPrintStream() throws IOException {
		PrintStream pw = new PrintStream(new FileOutputStream(new File(
				"./target/TWCPerplexityErrorRateNDocsLDA.csv")));

		pw.print("ndocs;");
		pw.print("perplexity;");
		pw.print("perplexity_stddev;");
		pw.print("perplexityFoldIn;");
		pw.print("perplexityFoldIn_stddev;");
		pw.print("err;");
		pw.println("err_stddev;");
		return pw;
	}

	@Override
	protected PrintStream prepareTGPrintStream() throws IOException {
		PrintStream pw = new PrintStream(new FileOutputStream(new File(
				"./target/TWCPerplexityErrorRateNDocsTG.csv")));
		pw.print("ndocs;");
		pw.print("perplexity;");
		pw.print("perplexity_stddev;");
		pw.print("err;");
		pw.println("err_stddev;");
		return pw;
	}

	@Override
	protected PrintStream prepareTGLikelihoodPrintStream() throws IOException {
		return null;
	}

	@Override
	protected void runTopicGrouper(final PrintStream pw3, final int step,
			final DocumentProvider<String> documentProvider,
			final DocumentProvider<String> testDocumentProvider,
			final double[] tgPerplexity, final double[] tgAcc) {
		AbstractTopicGrouper<String> topicGrouper = new TopicGrouperWithTreeSet<String>(
				1, documentProvider, 1);
		topicGrouper.solve(new TGSolutionListener<String>() {
			@Override
			public void updatedSolution(int newTopicIndex, int oldTopicIndex,
					double improvement, int t1Size, int t2Size,
					final TGSolution<String> solution) {
				if (solution.getNumberOfTopics() == 4) {
					tgAcc[step] = computeTGAccuracy(solution, documentProvider);
					tgPerplexity[step] = computeTGPerplexity(solution,
							testDocumentProvider);
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
		pw.print(1 - MathExt.avg(acc));
		pw.print("; ");
		pw.print(MathExt.sampleStdDev(acc));
		pw.println("; ");
	}

	@Override
	protected void aggregateTGResults(PrintStream pw, int step,
			double[] tgPerplexity, double[] tgAcc) {
		double tgPerplexityAvg = MathExt.avg(tgPerplexity);
		double tgAccAvg = MathExt.avg(tgAcc);

		pw.print(docsFromStep(step));
		pw.print("; ");
		pw.print(tgPerplexityAvg);
		pw.print("; ");
		pw.print(MathExt.sampleStdDev(tgPerplexityAvg, tgPerplexity));
		pw.print("; ");
		pw.print(1.0 - tgAccAvg);
		pw.print("; ");
		pw.print(MathExt.sampleStdDev(tgAccAvg, tgAcc));
		pw.println("; ");
	}

	public static void main(String[] args) throws IOException {
		new TWCPerplexityErrorRateNDocs(new Random()).run(100, 10, 10);
	}
}
