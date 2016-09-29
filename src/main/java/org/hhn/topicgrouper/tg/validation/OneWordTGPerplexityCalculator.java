package org.hhn.topicgrouper.tg.validation;

import gnu.trove.TIntCollection;
import gnu.trove.iterator.TIntIterator;

import java.util.Random;

import org.hhn.topicgrouper.doc.Document;
import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.tg.TGSolution;

public class OneWordTGPerplexityCalculator<T> extends TGPerplexityCalculator<T> {
	private final Random random;

	public OneWordTGPerplexityCalculator(Random random) {
		this.random = random;
	}

	@Override
	public double computePerplexity(DocumentProvider<T> testDocumentProvider,
			TGSolution<T> s) {
		double sumA = 0;
		long sumB = 0;
		for (Document<T> d : testDocumentProvider.getDocuments()) {
			int dSize = 0;
			TIntIterator it = d.getWordIndices().iterator();

			// Choose a random word to predict from the test document (uniform).
			int positionOfHeldOutWord = random.nextInt(d.getSize());
			int handledWords = 0;
			int heldOutWordIndex = -1;

			while (it.hasNext()) {
				int index = it.next();
				T word = testDocumentProvider.getWord(index);
				int sIndex = s.getIndex(word);
				if (sIndex >= 0) {
					dSize += d.getWordFrequency(index);
				}
				// Get the index of the word to predict with regard to solution.
				handledWords += d.getWordFrequency(sIndex);
				if (handledWords >= positionOfHeldOutWord
						&& heldOutWordIndex == -1) {
					// Is it a word from the training vocubulary?
					if (sIndex >= 0) {
						heldOutWordIndex = sIndex;
					}
				}
			}
			if (heldOutWordIndex == -1) {
				// No matching word from the training vocubulary found.
				// Restart at the beginning.
				while (it.hasNext()) {
					int index = it.next();
					T word = testDocumentProvider.getWord(index);
					int sIndex = s.getIndex(word);
					if (sIndex >= 0) {
						heldOutWordIndex = sIndex;
						break;
					}
				}
			}
			if (heldOutWordIndex >= 0) {
				int topicIndex = s.getTopicForWord(heldOutWordIndex);
				TIntCollection words = s.getTopics()[topicIndex];
				// Exclude word to predict.
				sumA += computeWordLogProbability(heldOutWordIndex, d, dSize,
						s, words, topicIndex);
				sumB++;
			} else {
				// This means no word from test document occurs in the training
				// vocabulary. Just do nothing and omit this test document.
				// (Should be rare...)
			}
		}
		return Math.exp(-sumA / sumB);
	}

	@Override
	protected int correctTopicFrInDoc(int topicFrInDoc) {
		return (topicFrInDoc - 1) + 1; // Exclude held out word (-1) and add 1
										// for Laplace smoothing.
	}

	protected int correctDocSize(int docSize, int nTopics) {
		return (docSize - 1) + nTopics; // Exclude held out word (-1) and add n
										// topics for Laplace smoothing.
	}
}
