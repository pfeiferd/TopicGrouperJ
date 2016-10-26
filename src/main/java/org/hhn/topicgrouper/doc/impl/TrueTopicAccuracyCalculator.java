package org.hhn.topicgrouper.doc.impl;

import org.hhn.topicgrouper.doc.DocumentProvider;

public class TrueTopicAccuracyCalculator<T> {
	public double computeAccuracy(DocumentProvider<T> documentProvider,
			int topics, PwtProvider provider) {
		return computeMaxAccuracyCount(0, documentProvider,
				new int[topics], provider);
	}

	protected double computeMaxAccuracyCount(int pos,
			DocumentProvider<T> documentProvider, int[] perm,
			PwtProvider provider) {
		if (pos == perm.length) {
			return computeAccuracyCountHelp(documentProvider, perm, provider);
		}
		double max = 0;
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
				if (acc > max) {
					max = acc;
				}
			}
		}
		return max;
	}

	protected double computeAccuracyCountHelp(
			DocumentProvider<T> documentProvider, int[] topicAssignments,
			PwtProvider provider) {
		double count = 0;

		for (int j = 0; j < documentProvider.getVocab().getNumberOfWords(); j++) {
			if (documentProvider.getWordFrequency(j) > 0) {
				for (int i = 0; i < topicAssignments.length; i++) {
					if (provider.isCorrectTopic(topicAssignments[i], j)) {
						count += provider.getPwt(i, j);
					}
				}
			}
		}
		//System.out.println(count);

		return count;
	}

	public interface PwtProvider {
		public double getPwt(int topic, int wordIndex);

		public boolean isCorrectTopic(int topic, int index);
	}
}
