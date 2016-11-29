package org.hhn.topicgrouper.paper;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.hhn.topicgrouper.classify.SupervisedDocumentClassifier;
import org.hhn.topicgrouper.classify.impl.lda.LDANBClassifier;
import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.doc.LabeledDocument;
import org.hhn.topicgrouper.doc.LabelingDocumentProvider;
import org.hhn.topicgrouper.doc.impl.DefaultVocab;
import org.hhn.topicgrouper.doc.impl.LabelingHoldOutSplitter;
import org.hhn.topicgrouper.eval.Reuters21578;
import org.hhn.topicgrouper.lda.impl.LDAGibbsSampler;
import org.hhn.topicgrouper.lda.report.BasicLDAResultReporter;

public class ReutersLDAClassificationExperiment {
	protected final LabelingDocumentProvider<String, String> testProvider;
	protected final LabelingDocumentProvider<String, String> trainingProvider;

	public ReutersLDAClassificationExperiment() throws IOException {
		// Use ModApte split:
		// Use ModApte split:
		LabelingDocumentProvider<String, String>[] res = new LabelingDocumentProvider[2];
		ReutersTGNaiveBayesExperiment.createModApteSplit(res);
		testProvider = res[0];
		trainingProvider = res[1];

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

	public void run() {
		final int[] t = new int[1];
		for (int topics = 1; topics <= 500; topics += 10) {
			t[0] = topics;
			final LDAGibbsSampler<String> ldaGibbsSampler = new LDAGibbsSampler<String>(
					trainingProvider, topics, 0.1, 0.1, new Random(42));
			ldaGibbsSampler.setUpdateAlphaBeta(true);
			ldaGibbsSampler.solve(200, 200, new BasicLDAResultReporter<String>(
					System.out, 10) {
				@Override
				public void done(LDAGibbsSampler<String> sampler) {
					super.done(sampler);
					SupervisedDocumentClassifier<String, String> classifier = createClassifier(ldaGibbsSampler);
					classifier.train(trainingProvider);
					double microAvg = classifier.test(testProvider, true);
					double macroAvg = classifier.test(testProvider, false);
					System.out
							.println(t[0] + "; " + microAvg + "; " + macroAvg);
				}

				@Override
				public void updatedSolution(LDAGibbsSampler<String> sampler,
						int iteration) {
				}
			});
		}
	}

	protected DocumentProvider<String> createDocumentProvider() {
		return trainingProvider;
	}

	protected SupervisedDocumentClassifier<String, String> createClassifier(
			LDAGibbsSampler<String> ldaGibbsSampler) {
		return new LDANBClassifier<String, String>(0.0001, ldaGibbsSampler, 200);
	}

	public static void main(String[] args) throws IOException {
		new ReutersLDAClassificationExperiment().run();
	}
}
