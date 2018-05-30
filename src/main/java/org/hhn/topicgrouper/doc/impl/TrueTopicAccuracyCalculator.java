package org.hhn.topicgrouper.doc.impl;

import org.hhn.topicgrouper.doc.DocumentProvider;

public class TrueTopicAccuracyCalculator<T> {
	public enum MeasuringMethod {
		MIN, ABS, SQUARE
	}

	private MeasuringMethod measuringMethod = MeasuringMethod.ABS;

	public void setMeasuringMethod(MeasuringMethod measuringMethod) {
		this.measuringMethod = measuringMethod;
	}

	public MeasuringMethod getMeasuringMethod() {
		return measuringMethod;
	}

	public double computeAccuracy(DocumentProvider<T> documentProvider,
			int topics, PwtProvider<T> provider) {
		// Just a check to ensure we work on useful stuff:
		assertModelSanity(documentProvider, topics, provider);

		return computeAccuracyPure(documentProvider, topics, provider);
	}

	public double computeAccuracyPure(DocumentProvider<T> documentProvider,
			int topics, PwtProvider<T> provider) {
		return computeMaxAccuracyCount(0, documentProvider, new int[topics],
				provider);
	}

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

		double res = count / topicAssignments.length / 2;
		return MeasuringMethod.MIN.equals(measuringMethod) ? res : 1 - res;
	}

	public interface PwtProvider<T> {
		public double getPwtFromModel(int topic, int wordIndex);

		public double getCorrectPwt(int topic, T word);
	}
}
