package org.hhn.topicgrouper.paper;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Random;

import org.hhn.topicgrouper.classify.impl.AbstractTopicBasedTfIdfClassifier;
import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.doc.LabeledDocument;
import org.hhn.topicgrouper.doc.LabelingDocumentProvider;
import org.hhn.topicgrouper.doc.impl.LabelingHoldOutSplitter;
import org.hhn.topicgrouper.eval.AbstractTGTester;
import org.hhn.topicgrouper.eval.Reuters21578;
import org.hhn.topicgrouper.tg.TGSolution;
import org.hhn.topicgrouper.tg.TGSolutionListener;
import org.hhn.topicgrouper.tg.TGSolver;
import org.hhn.topicgrouper.tg.impl.TopicGrouperWithTreeSet;

public class ReutersTGTfIdfExperiment extends AbstractTGTester<String> {
	private final LabelingDocumentProvider<String, String> testProvider;
	private final LabelingDocumentProvider<String, String> trainingProvider;

	public ReutersTGTfIdfExperiment() throws IOException {
		super(null);
		LabelingDocumentProvider<String, String> provider = new Reuters21578(
				true).getCorpusDocumentProvider(new File(
				"src/test/resources/reuters21578"), true, true);
		LabelingHoldOutSplitter<String, String> splitter = new LabelingHoldOutSplitter<String, String>(
				new Random(42), provider, 0.1, 20, 10);
		testProvider = splitter.getHoldOut();
		trainingProvider = splitter.getRest();
	}

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
				int nt = solution.getNumberOfTopics();
				if (nt % 100 == 0 || nt < 300) {
					final int[] topicsIds = solution.getTopicIds();

					AbstractTopicBasedTfIdfClassifier<String, String> classifier = new AbstractTopicBasedTfIdfClassifier<String, String>() {
						@Override
						protected int getTopicIndex(int wordIndex) {
							return solution.getTopicForWord(wordIndex);
						}

						@Override
						protected int[] getTopicIndices() {
							return topicsIds;
						}
					};

					classifier.train(trainingProvider);

					int hits = 0;
					int tests = 0;
					for (LabeledDocument<String, String> dt : testProvider
							.getLabeledDocuments()) {
						String label = classifier.classify(dt);
						if (label.equals(dt.getLabel())) {
							hits++;
						}
						tests++;
					}
					System.out.println(topicsIds.length + "; "
							+ (((double) hits) / tests) + ";");
				}
			}
		};
	}

	public static void main(String[] args) throws IOException {
		new ReutersTGTfIdfExperiment().run();
	}
}
