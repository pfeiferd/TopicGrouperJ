package org.hhn.topicgrouper.classify.impl.vocab;

import gnu.trove.impl.Constants;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.util.Arrays;

import org.hhn.topicgrouper.classify.impl.AbstractTopicBasedNBClassifier;
import org.hhn.topicgrouper.doc.DocumentProvider.Vocab;
import org.hhn.topicgrouper.doc.LabeledDocument;
import org.hhn.topicgrouper.doc.LabelingDocumentProvider;

/**
 * Naive Bayes classifier that uses single words as "topics": only the best words
 * are kept as features, ranked by a score where smaller is better (here the conditional
 * entropy of the label given the presence of the word, i.e. information gain).
 *
 * @param <T> the word type of the documents
 * @param <L> the label type
 */
public class VocabIGNBClassifier<T, L> extends
		AbstractTopicBasedNBClassifier<T, L> {
	/**
	 * The indices of the selected words.
	 */
	protected final int[] topicIds;

	/**
	 * Creates the classifier and selects the features.
	 *
	 * @param lambda smoothing weight, see {@link AbstractTopicBasedNBClassifier}
	 * @param documentProvider labeled documents used to rank the words
	 * @param keepWords maximum number of words to keep
	 */
	public VocabIGNBClassifier(double lambda,
			LabelingDocumentProvider<T, L> documentProvider, int keepWords) {
		super(lambda);
		topicIds = getBestWords(documentProvider, keepWords);
//		for (int i = 0; i < topicIds.length; i++) {
//			System.out.println(documentProvider.getVocab().getWord(topicIds[i]));
//		}
	}

	/**
	 * Selects the words with the smallest score.
	 *
	 * @param documentProvider labeled documents used for scoring
	 * @param maxBest maximum number of words to select
	 * @return word indices ordered by increasing score (entries are -1 if fewer words were available)
	 */
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
		return bestWords;
	}

	// The smaller the better...
	/**
	 * Scores a word by the weighted entropy of the label distribution over documents
	 * containing and not containing the word (Laplace smoothed). Smaller is better.
	 *
	 * @param wordIndex the word
	 * @param documentProvider the labeled documents
	 * @param posMap scratch map (label to document count), cleared and reused by this method
	 * @param negMap scratch map (label to document count), cleared and reused by this method
	 * @return the score
	 */
	protected double computeScore(int wordIndex,
			LabelingDocumentProvider<T, L> documentProvider,
			TObjectIntMap<L> posMap, TObjectIntMap<L> negMap) {
		// Entropy (here same as Information Gain as score.)
		int labels = documentProvider.getAllLabels().size();
		
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
			double pLPosW = ((double)(posMap.get(label) + 1)) / (wPosCount + labels); // Laplace smoothing
			sumPos += pLPosW == 0 ? 0 : pLPosW * Math.log(pLPosW);
			double pLNegW = ((double) (negMap.get(label) + 1)) / (wNegCount + labels); // Laplace smoothing
			sumNeg += pLNegW == 0 ? 0 : pLNegW * Math.log(pLNegW);
		}
		return -(sumPos * wPosCount / docs) - (sumNeg * wNegCount / docs);
	}

	/**
	 * @return the indices of the selected words
	 */
	@Override
	protected int[] getTopicIndices() {
		return topicIds;
	}

	/**
	 * @param wordIndex the word index
	 * @return the word index itself if the word was selected, otherwise <code>Integer.MIN_VALUE</code>
	 */
	@Override
	protected int getTopicIndex(int wordIndex) {
		return topicIndicesBack.containsKey(wordIndex) ? wordIndex : Integer.MIN_VALUE;
	}
}
