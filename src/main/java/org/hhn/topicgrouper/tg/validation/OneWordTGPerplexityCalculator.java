package org.hhn.topicgrouper.tg.validation;

import gnu.trove.TIntCollection;
import gnu.trove.iterator.TIntIterator;

import java.util.Random;

import org.hhn.topicgrouper.doc.Document;
import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.tg.TGSolution;

public class OneWordTGPerplexityCalculator<T> extends TGPerplexityCalculator<T> {
	public static final double DEFAULT_LIDSTONE_LAMDA = 0.00000000000001d;
	private final Random random;

	private final double lidstoneLambda;

	public OneWordTGPerplexityCalculator(Random random) {
		this(random, DEFAULT_LIDSTONE_LAMDA);
	}

	public OneWordTGPerplexityCalculator(Random random, double lidstoneLambda) {
		this.random = random;
		this.lidstoneLambda = lidstoneLambda;
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
	protected double correctTopicFrInDoc(int topicFrInDoc) {
		return (topicFrInDoc - 1) + lidstoneLambda; // Exclude held out word (-1) and add 1
										// for Lidstone smoothing.
	}

	protected double correctDocSize(int docSize, int nTopics) {
		return (docSize - 1) + lidstoneLambda * nTopics; // Exclude held out word (-1) and add n
										// topics for Lidstone smoothing.
	}
}
