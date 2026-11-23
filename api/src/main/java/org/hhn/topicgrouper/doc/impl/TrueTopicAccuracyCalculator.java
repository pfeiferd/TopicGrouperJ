package org.hhn.topicgrouper.doc.impl;

import org.hhn.topicgrouper.doc.DocumentProvider;

/**
 * Measures how well learned topic-word distributions match known true ones (e.g. of synthetic data).
 * Since topics are unordered, all assignments of model topics to true topics are tried and the best one is used
 * (factorial running time in the number of topics).
 *
 * @param <T> the word type
 */
public class TrueTopicAccuracyCalculator<T> {
	/**
	 * How the difference between model and true word probabilities is measured.
	 */
	public enum MeasuringMethod {
		/**
		 * Overlap of the two distributions, i.e. the sum over the words of the minimum of both probabilities.
		 */
		MIN,
		/**
		 * Sum over the words of the absolute difference of both probabilities.
		 */
		ABS,
		/**
		 * Sum over the words of the squared difference of both probabilities.
		 */
		SQUARE
	}

	private MeasuringMethod measuringMethod = MeasuringMethod.ABS;

	/**
	 * Creates a calculator using {@link MeasuringMethod#ABS}.
	 */
	public TrueTopicAccuracyCalculator() {
	}

	/**
	 * Sets the measuring method.
	 *
	 * @param measuringMethod the method used by subsequent computations (default ABS)
	 */
	public void setMeasuringMethod(MeasuringMethod measuringMethod) {
		this.measuringMethod = measuringMethod;
	}

	/**
	 * The measuring method.
	 *
	 * @return the current measuring method
	 */
	public MeasuringMethod getMeasuringMethod() {
		return measuringMethod;
	}

	/**
	 * Computes the accuracy after checking that the model's distributions are valid.
	 *
	 * @param documentProvider provides the vocabulary
	 * @param topics number of topics
	 * @param provider access to the model and true word probabilities
	 * @return the accuracy of the best topic assignment, 1 meaning identical
	 * @throws IllegalArgumentException if the model probabilities are inconsistent
	 */
	public double computeAccuracy(DocumentProvider<T> documentProvider,
			int topics, PwtProvider<T> provider) {
		// Just a check to ensure we work on useful stuff:
		assertModelSanity(documentProvider, topics, provider);

		return computeAccuracyPure(documentProvider, topics, provider);
	}

	/**
	 * Computes the accuracy without checking the model.
	 *
	 * @param documentProvider provides the vocabulary
	 * @param topics number of topics
	 * @param provider access to the model and true word probabilities
	 * @return the accuracy of the best topic assignment
	 */
	public double computeAccuracyPure(DocumentProvider<T> documentProvider,
			int topics, PwtProvider<T> provider) {
		return computeMaxAccuracyCount(0, documentProvider, new int[topics],
				provider);
	}

	/**
	 * Checks that all probabilities are in [0, 1] and that each model topic sums to 1.
	 *
	 * @param documentProvider provides the vocabulary
	 * @param topics number of topics
	 * @param provider access to the model and true word probabilities
	 * @throws IllegalArgumentException if the model is inconsistent
	 */
	protected void assertModelSanity(DocumentProvider<T> documentProvider,
			int topics, PwtProvider<T> provider) {
		for (int i = 0; i < topics; i++) {
			double sumModel = 0;
			double sumCorrect = 0;
			for (int j = 0; j < documentProvider.getVocab().getNumberOfWords(); j++) {
				double pwt = provider.getPwtFromModel(i, j);
				if (pwt < 0 || pwt > 1) {
					throw new IllegalArgumentException("Inconsistent model");
				}
				sumModel += pwt;
				double pwtc = provider.getCorrectPwt(i, documentProvider
						.getVocab().getWord(j));
				if (pwtc < 0 || pwtc > 1) {
					throw new IllegalArgumentException("Inconsistent model");
				}
				sumCorrect += pwtc;
			}
			if (sumModel <= 0.999 || sumModel > 1.00001) {
				throw new IllegalArgumentException("Inconsistent model");
			}
			// if (sumCorrect <= 0.999 || sumCorrect > 1.00001) {
			// throw new IllegalArgumentException("Inconsistent test data");
			// }
		}
	}

	/**
	 * Recursively enumerates all permutations of topics and returns the best accuracy.
	 *
	 * @param pos the position in the permutation to fill next
	 * @param documentProvider provides the vocabulary
	 * @param perm the permutation under construction (model topic to true topic)
	 * @param provider access to the model and true word probabilities
	 * @return the best accuracy over all completions
	 */
	protected double computeMaxAccuracyCount(int pos,
			DocumentProvider<T> documentProvider, int[] perm,
			PwtProvider<T> provider) {
		if (pos == perm.length) {
			return computeAccuracyCountHelp(documentProvider, perm, provider);
		}
		double best = 0;
		for (int i = 0; i < perm.length; i++) {
			boolean found = false;
			for (int h = 0; h < pos; h++) {
				if (perm[h] == i) {
					found = true;
				}
			}
			if (!found) {
				perm[pos] = i;
				double acc = computeMaxAccuracyCount(pos + 1, documentProvider,
						perm, provider);
				if (acc > best) {
					best = acc;
				}
			}
		}
		return best;
	}

	/**
	 * Computes the accuracy for a fixed assignment of model topics to true topics.
	 *
	 * @param documentProvider provides the vocabulary
	 * @param topicAssignments model topic i is compared to true topic <code>topicAssignments[i]</code>
	 * @param provider access to the model and true word probabilities
	 * @return the accuracy (for ABS and SQUARE one minus the normalized difference, for MIN the normalized overlap)
	 */
	protected double computeAccuracyCountHelp(
			DocumentProvider<T> documentProvider, int[] topicAssignments,
			PwtProvider<T> provider) {
		double count = 0;

		for (int i = 0; i < topicAssignments.length; i++) {
			for (int j = 0; j < documentProvider.getVocab().getNumberOfWords(); j++) {
				double a = provider.getCorrectPwt(topicAssignments[i],
						documentProvider.getVocab().getWord(j));
				double b = provider.getPwtFromModel(i, j);
				switch (measuringMethod) {
				case ABS:
					count += Math.abs(a - b);
					break;
				case SQUARE:
					double h = a - b;
					count += h * h;
					break;
				case MIN:
					count += Math.min(a, b);
					break;
				default:
					throw new IllegalStateException(
							"Undefined measuring method");
				}
			}
		}

		// Per topic, the absolute difference and the squared difference are at most 2,
		// the overlap (sum of minima) is at most 1.
		double res = count / topicAssignments.length;
		return MeasuringMethod.MIN.equals(measuringMethod) ? res : 1 - res / 2;
	}

	/**
	 * Supplies model and true word probabilities per topic.
	 *
	 * @param <T> the word type
	 */
	public interface PwtProvider<T> {
		/**
		 * The word probability according to the learned model.
		 *
		 * @param topic the model topic
		 * @param wordIndex the word index in the vocabulary
		 * @return P(word | topic) according to the learned model
		 */
		public double getPwtFromModel(int topic, int wordIndex);

		/**
		 * The true word probability, e.g. the one used to generate synthetic data.
		 *
		 * @param topic the true topic
		 * @param word the word
		 * @return the true P(word | topic)
		 */
		public double getCorrectPwt(int topic, T word);
	}
}
