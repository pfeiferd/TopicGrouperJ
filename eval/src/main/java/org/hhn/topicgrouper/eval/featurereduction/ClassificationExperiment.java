package org.hhn.topicgrouper.eval.featurereduction;

import java.io.File;
import java.io.IOException;
import java.util.Random;

import org.hhn.topicgrouper.eval.classify.SupervisedDocumentClassifier;
import org.hhn.topicgrouper.eval.classify.impl.AbstractTopicBasedBasicProbClassifier;
import org.hhn.topicgrouper.eval.classify.impl.AbstractTopicBasedTfIdfClassifier;
import org.hhn.topicgrouper.eval.classify.impl.lda.LDABasicProbClassifier;
import org.hhn.topicgrouper.eval.classify.impl.tg.TGBasicProbClassifier;
import org.hhn.topicgrouper.eval.classify.impl.vocab.VocabDFBasicProbClassifier;
import org.hhn.topicgrouper.eval.classify.impl.vocab.VocabIGBasicProbClassifier;
import org.hhn.topicgrouper.doc.LabelingDocumentProvider;
import org.hhn.topicgrouper.eval.Csv;
import org.hhn.topicgrouper.eval.Figures;
import org.hhn.topicgrouper.eval.Settings;
import org.hhn.topicgrouper.eval.Split;
import org.hhn.topicgrouper.eval.TopicGrouperFactory;
import org.hhn.topicgrouper.lda.impl.LDAGibbsSampler;
import org.hhn.topicgrouper.tg.TGSolution;
import org.hhn.topicgrouper.tg.TGSolutionListener;

/**
 * Text classification with topics as features (the paper "Feature reduction
 * using topic modeling"): the accuracy of naive Bayes on the topics of Topic
 * Grouper (<code>tg-nb</code>; <code>tg-nb-smoothed</code> with an optimized
 * smoothing), of a tf-idf / Rocchio classifier on them (<code>tg-tfidf</code>),
 * of naive Bayes on LDA topics with the fixed prior (<code>lda-nb</code>) and
 * with re-estimated alpha and beta (<code>lda-nb-opt</code>), and of naive
 * Bayes on the words selected by information gain (<code>vocab-ig</code>) or
 * document frequency (<code>vocab-df</code>), over the number of topics or
 * words.
 */
public class ClassificationExperiment {
	private final Settings settings;
	private final File out;

	/**
	 * Creates the experiment.
	 *
	 * @param settings
	 *            experiment size
	 * @param out
	 *            output directory
	 */
	public ClassificationExperiment(Settings settings, File out) {
		this.settings = settings;
		this.out = out;
	}

	/**
	 * Writes <code>classification-&lt;name&gt;.csv</code> with the columns
	 * <code>method;topics;microAvg;macroAvg;lambda</code>.
	 *
	 * @param name
	 *            name of the corpus
	 * @param split
	 *            labeled training and test documents
	 * @param withLda
	 *            whether the LDA classifiers are evaluated (slow)
	 */
	public void run(String name, Split<LabelingDocumentProvider<String, String>> split, boolean withLda) {
		File file = new File(out, "classification-" + name + ".csv");
		if (Csv.skipIfDone(file)) {
			return;
		}
		Csv csv = new Csv(file, "method", "topics", "microAvg", "macroAvg", "lambda");
		try {
			runTopicGrouper(split, csv);
			runVocab(split, csv);
			if (withLda) {
				runLDA(split, csv);
			}
		} catch (RuntimeException | Error e) {
			csv.discard();
			throw e;
		}
		csv.close();
		plot(file);
	}

	private void evaluate(Csv csv, String method, int topics, SupervisedDocumentClassifier<String, String> classifier,
			Split<LabelingDocumentProvider<String, String>> split, boolean optimizeLambda) {
		classifier.train(split.train);
		if (optimizeLambda) {
			((AbstractTopicBasedBasicProbClassifier<String, String>) classifier).optimizeLambda(0, 0.5, split.train, 10, true);
		}
		double[] res = classifier.test(split.test, null);
		double lambda = classifier instanceof AbstractTopicBasedBasicProbClassifier
				? ((AbstractTopicBasedBasicProbClassifier<String, String>) classifier).getSmoothingLambda()
				: 0;
		csv.row(method, topics, res[0], res[1], lambda);
	}

	private void runTopicGrouper(final Split<LabelingDocumentProvider<String, String>> split, final Csv csv) {
		new TopicGrouperFactory().create(split.train).solve(new TGSolutionListener<String>() {
			public void updatedSolution(int newTopicIndex, int oldTopicIndex, double improvement, int t1Size,
					int t2Size, final TGSolution<String> solution) {
				int n = solution.getNumberOfTopics();
				if (!settings.isClassificationTopics(n)) {
					return;
				}
				evaluate(csv, "tg-nb", n, new TGBasicProbClassifier<String, String>(0, solution), split, false);
				evaluate(csv, "tg-nb-smoothed", n, new TGBasicProbClassifier<String, String>(0.3, solution), split, true);
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

	private void runVocab(Split<LabelingDocumentProvider<String, String>> split, Csv csv) {
		int words = split.train.getVocab().getNumberOfWords();
		for (int k : settings.classificationTopics) {
			if (k <= words) {
				evaluate(csv, "vocab-ig", k, new VocabIGBasicProbClassifier<String, String>(0, split.train, k), split, false);
				evaluate(csv, "vocab-df", k, new VocabDFBasicProbClassifier<String, String>(0, split.train, k), split, false);
			}
		}
	}

	private void runLDA(Split<LabelingDocumentProvider<String, String>> split, Csv csv) {
		for (int topics : settings.ldaClassificationTopics) {
			for (boolean optimize : new boolean[] { false, true }) {
				LDAGibbsSampler<String> lda = new LDAGibbsSampler<String>(new Random(42), split.train,
						LDAGibbsSampler.symmetricAlpha(50d, topics), 0.1);
				if (optimize) {
					lda.setUpdateAlphaBeta(true);
					lda.setAlphaBetaUpdate(Math.max(1, settings.classLdaBurnIn / 20));
				}
				lda.solve(settings.classLdaBurnIn, settings.classLdaIterations, null);
				evaluate(csv, optimize ? "lda-nb-opt" : "lda-nb", topics, new LDABasicProbClassifier<String, String>(0, lda,
						settings.foldInIterations, settings.foldInSamples), split, false);
			}
		}
	}

	/**
	 * Draws the figures of a finished CSV file into <code>figs</code>.
	 *
	 * @param csv
	 *            the CSV file
	 */
	private void plot(File csv) {
		try {
			new Figures(out).plot(csv);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
