package org.hhn.topicgrouper.classify.impl.vocab;

import gnu.trove.impl.Constants;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.util.Arrays;

import org.hhn.topicgrouper.classify.impl.AbstractTopicBasedNBClassifier;
import org.hhn.topicgrouper.doc.DocumentProvider.Vocab;
import org.hhn.topicgrouper.doc.LabeledDocument;
import org.hhn.topicgrouper.doc.LabelingDocumentProvider;

public class VocabNBClassifier<T, L> extends
		AbstractTopicBasedNBClassifier<T, L> {
	protected final int[] topicIds;

	public VocabNBClassifier(double lambda,
			LabelingDocumentProvider<T, L> documentProvider, int keepWords) {
		super(lambda);
		documentProvider.getVocab();
		topicIds = getBestWords(documentProvider, keepWords);
	}

	protected int[] getBestWords(
			LabelingDocumentProvider<T, L> documentProvider, int maxBest) {
		Vocab<T> vocab = documentProvider.getVocab();
		maxBest = Math.min(maxBest, vocab.getNumberOfWords());

		double[] igList = new double[maxBest];
		int[] bestWords = new int[maxBest];

		Arrays.fill(igList, Integer.MIN_VALUE);
		Arrays.fill(bestWords, -1);

		TObjectIntMap<L> posMap = new TObjectIntHashMap<L>();
		TObjectIntMap<L> negMap = new TObjectIntHashMap<L>();

		for (int i = 0; i < vocab.getNumberOfWords(); i++) {
			double ig = computeInformationGain(i, documentProvider, posMap, negMap);
			int pos = Arrays.binarySearch(igList, i);
			if (pos < 0) {
				pos = -pos + 1;
			}
			if (pos < igList.length) {
				for (int j = pos; j < igList.length - 1; j++) {
					igList[j + 1] = igList[j];
					bestWords[j + 1] = bestWords[j];
				}
				igList[pos] = ig;
				bestWords[pos] = i;
			}
		}

		return bestWords;
	}

	protected double computeInformationGain(int wordIndex,
			LabelingDocumentProvider<T, L> documentProvider,
			TObjectIntMap<L> posMap, TObjectIntMap<L> negMap) {
		posMap.clear();
		negMap.clear();
		double sum = 0;
		int wPosCount = 0;
		int wNegCount = 0;

		for (LabeledDocument<T, L> d : documentProvider.getLabeledDocuments()) {
			if (d.getWordFrequency(wordIndex) > 0) {
				wPosCount++;
				int count = posMap.get(d.getLabel());
				if (count == Constants.DEFAULT_INT_NO_ENTRY_VALUE) {
					count = 0;
				}
				posMap.put(d.getLabel(), count + 1);
			} else {
				wNegCount++;
				int count = negMap.get(d.getLabel());
				if (count == Constants.DEFAULT_INT_NO_ENTRY_VALUE) {
					count = 0;
				}
				negMap.put(d.getLabel(), count + 1);
			}
		}
		for (L label : documentProvider.getAllLabels()) {
			double pLPosW = ((double) posMap.get(label)) / wPosCount;
			sum += pLPosW == 0 ? 0 : pLPosW * Math.log(pLPosW);
			double pLNegW = ((double) negMap.get(label)) / wNegCount;
			sum += pLNegW == 0 ? 0 : pLNegW * Math.log(pLNegW);
		}
		return sum;
	}

	@Override
	protected int[] getTopicIndices() {
		return topicIds;
	}

	@Override
	protected int getTopicIndex(int wordIndex) {
		return wordIndex;
	}
}
