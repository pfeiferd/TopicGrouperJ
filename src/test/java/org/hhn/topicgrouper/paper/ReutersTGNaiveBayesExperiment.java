package org.hhn.topicgrouper.paper;

import gnu.trove.iterator.TIntIterator;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Random;

import org.hhn.topicgrouper.doc.Document;
import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.doc.LabeledDocument;
import org.hhn.topicgrouper.doc.LabelingDocumentProvider;
import org.hhn.topicgrouper.doc.impl.LabelingHoldOutSplitter;
import org.hhn.topicgrouper.eval.AbstractTGTester;
import org.hhn.topicgrouper.eval.Reuters21578;
import org.hhn.topicgrouper.nb.impl.AbstractTopicBasedNBClassifier;
import org.hhn.topicgrouper.tg.TGSolution;
import org.hhn.topicgrouper.tg.TGSolutionListener;
import org.hhn.topicgrouper.tg.TGSolver;
import org.hhn.topicgrouper.tg.impl.TopicGrouperWithTreeSet;

public class ReutersTGNaiveBayesExperiment extends AbstractTGTester<String> {
	private final LabelingDocumentProvider<String, String> testProvider;
	private final LabelingDocumentProvider<String, String> trainingProvider;

	public ReutersTGNaiveBayesExperiment() throws IOException {
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
				final int[] topicsIds = solution.getTopicIds();

				AbstractTopicBasedNBClassifier<String, String> classifier = new AbstractTopicBasedNBClassifier<String, String>() {
					private double lambda = 0.001;
					
					@Override
					protected double[] computePtd(Document<String> d) {
						double[] res = new double[topicsIds.length];
						Arrays.fill(res, lambda);

						TIntIterator it = d.getWordIndices().iterator();
						while (it.hasNext()) {
							int wordIndex = it.next();
							int topicIndex = solution
									.getTopicForWord(wordIndex);
							int k = -1;
							for (int i = 0; i < topicsIds.length; i++) {
								if (topicsIds[i] == topicIndex) {
									k = i;
									break;
								}
							}

							res[k] += d.getWordFrequency(wordIndex);
						}
						for (int i = 0; i < res.length; i++) {
							res[i] /= (d.getSize() + lambda * res.length);
//							if (Double.isNaN(res[i])) {
//								System.out.println("stop");
//							}
						}
						return res;
					}

					@Override
					protected int getNTopics() {
						return solution.getNumberOfTopics();
					}

					@Override
					protected double[] computePt() {
						double[] res = new double[topicsIds.length];
						for (int i = 0; i < topicsIds.length; i++) {
							res[i] = ((double) solution
									.getTopicFrequency(topicsIds[i]))
									/ solution.getSize();
						}
						return res;
					}
				};

				classifier.train(trainingProvider);

				int hits = 0;
				int tests = 0;
				int empty = 0;
				for (LabeledDocument<String, String> dt : testProvider
						.getLabeledDocuments()) {
					// Beware empty docs:
					if (dt.getSize() > 0) {
						String label = classifier.classify(dt);
						if (label.equals(dt.getLabel())) {
							hits++;
						}
						tests++;
					}
					else {
						empty++;
					}
				}
				System.out.println(topicsIds.length + "; "
						+ (((double) hits) / tests) + " " + empty + " " + tests);
			}
		};
	}

	public static void main(String[] args) throws IOException {
		new ReutersTGNaiveBayesExperiment().run();
	}
}
