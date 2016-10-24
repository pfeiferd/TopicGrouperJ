package org.hhn.topicgrouper.doc.impl;

import org.hhn.topicgrouper.doc.DocumentProvider;

public class TrueTopicAccuracyCalculator<T> {
	public double computeAccuracy(DocumentProvider<T> documentProvider,
			int topics, FrequencyProvider provider) {
		int count = computeMaxAccuracyCount(0, documentProvider,
				new int[topics], provider);
		return ((double) count) / documentProvider.getSize();
	}

	protected int computeMaxAccuracyCount(int pos,
			DocumentProvider<T> documentProvider, int[] perm,
			FrequencyProvider provider) {
		if (pos == perm.length) {
			return computeAccuracyCountHelp(documentProvider, perm, provider);
		}
		int max = 0;
		for (int i = 0; i < perm.length; i++) {
			boolean found = false;
			for (int h = 0; h < pos; h++) {
				if (perm[h] == i) {
					found = true;
				}
			}
			if (!found) {
				perm[pos] = i;
				int acc = computeMaxAccuracyCount(pos + 1, documentProvider,
						perm, provider);
				if (acc > max) {
					max = acc;
				}
			}
		}
		return max;
	}

	protected int computeAccuracyCountHelp(
			DocumentProvider<T> documentProvider, int[] topicAssignments,
			FrequencyProvider provider) {
		int count = 0;

		for (int j = 0; j < documentProvider.getVocab().getNumberOfWords(); j++) {
			if (documentProvider.getWordFrequency(j) > 0) {
				for (int i = 0; i < topicAssignments.length; i++) {
					if (provider.isCorrectTopic(topicAssignments[i], j)) {
						count += provider.getFrequency(i, j);
					}
				}
			}
		}

		return count;
	}

	public interface FrequencyProvider {
		public int getFrequency(int topic, int wordIndex);

		public boolean isCorrectTopic(int topic, int index);
	}
}
