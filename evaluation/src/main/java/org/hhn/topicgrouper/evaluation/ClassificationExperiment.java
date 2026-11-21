package org.hhn.topicgrouper.evaluation;

import java.io.File;
import java.util.Random;

import org.hhn.topicgrouper.classify.SupervisedDocumentClassifier;
import org.hhn.topicgrouper.classify.impl.AbstractTopicBasedNBClassifier;
import org.hhn.topicgrouper.classify.impl.AbstractTopicBasedTfIdfClassifier;
import org.hhn.topicgrouper.classify.impl.lda.LDANBClassifier;
import org.hhn.topicgrouper.classify.impl.tg.TGNBClassifier;
import org.hhn.topicgrouper.classify.impl.vocab.VocabDFNBClassifier;
import org.hhn.topicgrouper.classify.impl.vocab.VocabIGNBClassifier;
import org.hhn.topicgrouper.doc.LabelingDocumentProvider;
import org.hhn.topicgrouper.lda.impl.LDAGibbsSampler;
import org.hhn.topicgrouper.tg.TGSolution;
import org.hhn.topicgrouper.tg.TGSolutionListener;

/**
 * Text classification with topics as features: the words are replaced by
 * their topics (found by Topic Grouper or LDA) or by a selected subset of the
 * vocabulary (information gain / document frequency baselines), and a naive
 * Bayes or tf-idf classifier is trained on top.
 */
public final class ClassificationExperiment {
	private ClassificationExperiment() {
	}

	/**
	 * Runs all classifiers and writes <code>classification-&lt;name&gt;.csv</code>
	 * with the columns <code>method;topics;microAvg;macroAvg;lambda</code>.
	 * 
	 * @param split
	 *            training and test documents
	 * @param s
	 *            experiment size
	 * @param out
	 *            result file
	 * @param withLda
	 *            whether the (slow) LDA features are evaluated
	 */
	public static void run(final Split<LabelingDocumentProvider<String, String>> split,
			final Settings s, File out, boolean withLda) {
		final Csv csv = new Csv(out, "method", "topics", "microAvg", "macroAvg", "lambda");
		try {
			runTG(split, s, csv);
			runVocab(split, s, csv);
			if (withLda) {
				runLDA(split, s, csv);
			}
		} finally {
			csv.close();
		}
	}

	private static void evaluate(Csv csv, String method, int topics,
			SupervisedDocumentClassifier<String, String> classifier,
			Split<LabelingDocumentProvider<String, String>> split, boolean optimizeLambda) {
		classifier.train(split.train);
		if (optimizeLambda) {
			((AbstractTopicBasedNBClassifier<String, String>) classifier)
					.optimizeLambda(0, 0.5, split.train, 10, true);
		}
		double[] res = classifier.test(split.test, null);
		double lambda = classifier instanceof AbstractTopicBasedNBClassifier
				? ((AbstractTopicBasedNBClassifier<String, String>) classifier).getSmoothingLambda()
				: 0;
		csv.row(method, topics, res[0], res[1], lambda);
	}

	private static void runTG(final Split<LabelingDocumentProvider<String, String>> split,
			final Settings s, final Csv csv) {
		PerplexityExperiment.createSolver(split.train).solve(new TGSolutionListener<String>() {
			public void updatedSolution(int newTopicIndex, int oldTopicIndex,
					double improvement, int t1Size, int t2Size, final TGSolution<String> solution) {
				int n = solution.getNumberOfTopics();
				if (!Settings.contains(s.classificationTopics, n)) {
					return;
				}
				evaluate(csv, "tg-nb", n, new TGNBClassifier<String, String>(0, solution), split, false);
				evaluate(csv, "tg-nb-smoothed", n, new TGNBClassifier<String, String>(0.3, solution), split, true);
				final int[] topicIds = solution.getTopicIds();
				evaluate(csv, "tg-tfidf", n, new AbstractTopicBasedTfIdfClassifier<String, String>() {
					@Override
					protected int getTopicIndex(int wordIndex) {
						return solution.getTopicForWord(wordIndex);
					}

					@Override
					protected int[] getTopicIndices() {
						return topicIds;
					}
				}, split, false);
			}

			public void beforeInitialization(int maxTopics, int documents) {
			}

			public void initalizing(double percentage) {
			}

			public void initialized(TGSolution<String> initialSolution) {
			}

			public void done() {
			}
		});
	}

	private static void runVocab(Split<LabelingDocumentProvider<String, String>> split, Settings s, Csv csv) {
		int words = split.train.getVocab().getNumberOfWords();
		for (int k : s.classificationTopics) {
			if (k <= words) {
				evaluate(csv, "vocab-ig", k, new VocabIGNBClassifier<String, String>(0, split.train, k), split, false);
				evaluate(csv, "vocab-df", k, new VocabDFNBClassifier<String, String>(0, split.train, k), split, false);
			}
		}
	}

	private static void runLDA(Split<LabelingDocumentProvider<String, String>> split, Settings s, Csv csv) {
		for (int topics : s.ldaClassificationTopics) {
			LDAGibbsSampler<String> lda = new LDAGibbsSampler<String>(new Random(42),
					split.train, LDAGibbsSampler.symmetricAlpha(50d, topics), 0.1);
			lda.solve(s.classLdaBurnIn, s.classLdaIterations, null);
			evaluate(csv, "lda-nb", topics,
					new LDANBClassifier<String, String>(0, lda, s.foldInIterations, s.foldInSamples), split, false);
		}
	}
}
