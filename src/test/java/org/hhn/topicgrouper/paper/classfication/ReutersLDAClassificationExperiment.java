package org.hhn.topicgrouper.paper.classfication;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Random;

import org.hhn.topicgrouper.classify.SupervisedDocumentClassifier;
import org.hhn.topicgrouper.classify.impl.lda.LDANBClassifier;
import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.doc.LabelingDocumentProvider;
import org.hhn.topicgrouper.lda.impl.LDAGibbsSampler;
import org.hhn.topicgrouper.lda.report.BasicLDAResultReporter;

public class ReutersLDAClassificationExperiment {
	protected final LabelingDocumentProvider<String, String> testProvider;
	protected final LabelingDocumentProvider<String, String> trainingProvider;
	private final PrintStream output;

	public ReutersLDAClassificationExperiment() throws IOException {
		// Use ModApte split:
		// Use ModApte split:
		LabelingDocumentProvider<String, String>[] res = new LabelingDocumentProvider[2];
		ReutersTGNaiveBayesExperiment.createModApteSplit(res);
		testProvider = res[0];
		trainingProvider = res[1];

		output = new PrintStream(new FileOutputStream(new File("./target/"
				+ getClass().getSimpleName() + ".csv")));
		output.println("topics; microAvg; macroAvg");

		// LabelingDocumentProvider<String, String> provider = initProvider();
		// LabelingHoldOutSplitter<String, String> splitter =
		// createHoldOutSplitter(provider);
		// trainingProvider = splitter.getRest();
		// testProvider = splitter.getHoldOut();
	}

	// protected LabelingDocumentProvider<String, String> initProvider() {
	// return new Reuters21578(true).getCorpusDocumentProvider(new File(
	// "src/test/resources/reuters21578"), true, true);
	// }
	//
	// protected LabelingHoldOutSplitter<String, String> createHoldOutSplitter(
	// LabelingDocumentProvider<String, String> provider) {
	// return new LabelingHoldOutSplitter<String, String>(new Random(42),
	// provider, 0.1, 20, 10);
	// }

	public void run(boolean optimizeAlphaBeta) {
		for (int topics = 1; topics <= 9; topics++) {
			runExperiment(topics, optimizeAlphaBeta);
		}
		for (int topics = 10; topics <= 99; topics += 10) {
			runExperiment(topics, optimizeAlphaBeta);
		}
		for (int topics = 100; topics <= 1000; topics += 100) {
			runExperiment(topics, optimizeAlphaBeta);
		}
	}

	protected void runExperiment(final int topics, boolean optimizeAlphaBeta) {
		final LDAGibbsSampler<String> ldaGibbsSampler = createGibbsSampler(
				topics, trainingProvider, optimizeAlphaBeta);
		ldaGibbsSampler.solve(1000, 1000, new BasicLDAResultReporter<String>(
				System.out, 10) {
			@Override
			public void done(LDAGibbsSampler<String> sampler) {
				super.done(sampler);
				SupervisedDocumentClassifier<String, String> classifier = createClassifier(ldaGibbsSampler);
				classifier.train(trainingProvider);
				double microAvg = classifier.test(testProvider, true);
				double macroAvg = classifier.test(testProvider, false);
				System.out.println(topics + "; " + microAvg + "; " + macroAvg);
				output.println(topics + "; " + microAvg + "; " + macroAvg);
			}

			@Override
			public void updatedSolution(LDAGibbsSampler<String> sampler,
					int iteration) {
			}
		});
	}

	protected LDAGibbsSampler<String> createGibbsSampler(int topics,
			DocumentProvider<String> documentProvider, boolean optimizeAlphaBeta) {
		// Initial alpha and beta like in:
		// http://psiexp.ss.uci.edu/research/papers/sciencetopics.pdf
		// and
		// http://stats.stackexchange.com/questions/59684/what-are-typical-values-to-use-for-alpha-and-beta-in-latent-dirichlet-allocation
		LDAGibbsSampler<String> ldaGibbsSampler = new LDAGibbsSampler<String>(
				documentProvider, createAlpha(topics), 0.1, new Random(42));
		ldaGibbsSampler.setUpdateAlphaBeta(optimizeAlphaBeta);
		return ldaGibbsSampler;
	}

	protected double[] createAlpha(int topics) {
		return LDAGibbsSampler.symmetricAlpha(50d / topics, topics);
	}

	protected DocumentProvider<String> createDocumentProvider() {
		return trainingProvider;
	}

	protected SupervisedDocumentClassifier<String, String> createClassifier(
			LDAGibbsSampler<String> ldaGibbsSampler) {
		// Need a minimum smoothing value to make sure the model fails in no
		// case.
		return new LDANBClassifier<String, String>(0.000000000001,
				ldaGibbsSampler, 200, 50);
	}

	public static void main(String[] args) throws IOException {
		new ReutersLDAClassificationExperiment().run(false);
	}
}
