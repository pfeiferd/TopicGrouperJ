package org.hhn.topicgrouper.classify.impl.vocab;

import gnu.trove.impl.Constants;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.util.Arrays;

import org.hhn.topicgrouper.classify.impl.AbstractTopicBasedNBClassifier;
import org.hhn.topicgrouper.doc.DocumentProvider.Vocab;
import org.hhn.topicgrouper.doc.LabeledDocument;
import org.hhn.topicgrouper.doc.LabelingDocumentProvider;

public class VocabIGNBClassifier<T, L> extends
		AbstractTopicBasedNBClassifier<T, L> {
	protected final int[] topicIds;
	protected final double[] pt;

	public VocabIGNBClassifier(double lambda,
			LabelingDocumentProvider<T, L> documentProvider, int keepWords) {
		super(lambda);
		topicIds = getBestWords(documentProvider, keepWords);
		pt = new double[topicIds.length];
		for (int i = 0; i < pt.length; i++) {
			pt[i] = ((double) documentProvider.getWordFrequency(topicIds[i]))
					/ documentProvider.getSize();
		}
	}

	protected int[] getBestWords(
			LabelingDocumentProvider<T, L> documentProvider, int maxBest) {
		Vocab<T> vocab = documentProvider.getVocab();
		maxBest = Math.min(maxBest, vocab.getNumberOfWords());

		double[] igList = new double[maxBest];
		int[] bestWords = new int[maxBest];

		Arrays.fill(igList, Double.MAX_VALUE);
		Arrays.fill(bestWords, -1);

		TObjectIntMap<L> posMap = new TObjectIntHashMap<L>();
		TObjectIntMap<L> negMap = new TObjectIntHashMap<L>();

		for (int i = 0; i < vocab.getNumberOfWords(); i++) {
			double score = computeScore(i, documentProvider, posMap,
					negMap);
			int pos = Arrays.binarySearch(igList, score);
			if (pos < 0) {
				pos = -pos - 1;
			}
			if (pos < igList.length) {
				for (int j = igList.length - 2; j >= pos; j--) {
					igList[j + 1] = igList[j];
					bestWords[j + 1] = bestWords[j];
				}
				igList[pos] = score;
				bestWords[pos] = i;
			}
		}
//		for (int i = 0; i < bestWords.length; i++) {
//			System.out.print(vocab.getWord(bestWords[i]));
//			System.out.print(" ");
//		}
//		System.out.println();
		return bestWords;
	}

	// The smaller the better...
	protected double computeScore(int wordIndex,
			LabelingDocumentProvider<T, L> documentProvider,
			TObjectIntMap<L> posMap, TObjectIntMap<L> negMap) {
		// Entropy (here same as Information Gain as score.)
		
		posMap.clear();
		negMap.clear();
		int wPosCount = 0;
		int wNegCount = 0;
		int docs = 0;

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
			docs++;
		}
		double sumPos = 0;
		double sumNeg = 0;
		for (L label : documentProvider.getAllLabels()) {
			double pLPosW = ((double) posMap.get(label)) / wPosCount;
			sumPos += pLPosW == 0 ? 0 : pLPosW * Math.log(pLPosW);
			double pLNegW = ((double) negMap.get(label)) / wNegCount;
			sumNeg += pLNegW == 0 ? 0 : pLNegW * Math.log(pLNegW);
		}
		return -(sumPos * wPosCount / docs) - (sumNeg * wNegCount / docs);
	}

	@Override
	protected int[] getTopicIndices() {
		return topicIds;
	}

	@Override
	protected int getTopicIndex(int wordIndex) {
		return wordIndex;
	}
	
	@Override
	protected double[] getPt() {
		return pt;
	}

}
