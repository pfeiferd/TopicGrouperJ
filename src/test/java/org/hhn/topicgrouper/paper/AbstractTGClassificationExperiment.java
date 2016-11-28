package org.hhn.topicgrouper.paper;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.hhn.topicgrouper.classify.SupervisedDocumentClassifier;
import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.doc.LabeledDocument;
import org.hhn.topicgrouper.doc.LabelingDocumentProvider;
import org.hhn.topicgrouper.doc.impl.DefaultVocab;
import org.hhn.topicgrouper.doc.impl.LabelingHoldOutSplitter;
import org.hhn.topicgrouper.eval.AbstractTGTester;
import org.hhn.topicgrouper.eval.Reuters21578;
import org.hhn.topicgrouper.tg.TGSolution;
import org.hhn.topicgrouper.tg.TGSolutionListener;
import org.hhn.topicgrouper.tg.TGSolver;
import org.hhn.topicgrouper.tg.impl.TopicGrouperWithTreeSet;

public abstract class AbstractTGClassificationExperiment extends
		AbstractTGTester<String> {
	protected final LabelingDocumentProvider<String, String> testProvider;
	protected final LabelingDocumentProvider<String, String> trainingProvider;

	public AbstractTGClassificationExperiment() throws IOException {
		super(null);

		// Use ModApte split:
		LabelingDocumentProvider<String, String>[] res = new LabelingDocumentProvider[2];
		createTrainingAndTestProvider(res);
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


	protected abstract void createTrainingAndTestProvider(LabelingDocumentProvider<String, String>[] res);
	
	@Override
	protected TGSolver<String> createSolver(
			DocumentProvider<String> documentProvider) {
		return new TopicGrouperWithTreeSet<String>(1, documentProvider, 1);
	}

	@Override
	protected DocumentProvider<String> createDocumentProvider() {
		return trainingProvider;
	}

	@Override
	protected TGSolutionListener<String> createSolutionListener(PrintStream out) {
		return new TGSolutionListener<String>() {
			@Override
			public void beforeInitialization(int maxTopics, int documents) {
			}

			@Override
			public void initalizing(double percentage) {
			}

			@Override
			public void done() {
			}

			@Override
			public void initialized(TGSolution<String> initialSolution) {
			}

			@Override
			public void updatedSolution(int newTopicIndex, int oldTopicIndex,
					double improvement, int t1Size, int t2Size,
					final TGSolution<String> solution) {
				SupervisedDocumentClassifier<String, String> classifier = AbstractTGClassificationExperiment.this
						.createClassifier(solution);
				if (classifier != null) {
					classifier.train(trainingProvider);
					testClassifier(testProvider, classifier, solution.getNumberOfTopics());
				}
			}
		};
	}

	public static void testClassifier(
			LabelingDocumentProvider<String, String> testProvider,
			SupervisedDocumentClassifier<String, String> classifier,
			int topics) {
		int hits = 0;
		int tests = 0;
		Map<String, Integer> counts = new HashMap<String, Integer>();

		for (LabeledDocument<String, String> dt : testProvider
				.getLabeledDocuments()) {
			String label = classifier.classify(dt);
			if (label.equals(dt.getLabel())) {
				Integer v = counts.get(label);
				counts.put(label, v == null ? 1 : v + 1);
				hits++;
			}
			tests++;
		}
		System.out.println(topics + "; "
				+ (((double) hits) / tests) + " " + tests);
		double avg = 0;
		for (String label : counts.keySet()) {
			avg += ((double) counts.get(label))
					/ testProvider.getDocumentsWithLabel(label).size();
		}
		avg = avg / counts.keySet().size();
		System.out.println(avg);
	}

	protected abstract SupervisedDocumentClassifier<String, String> createClassifier(
			TGSolution<String> solution);
}
