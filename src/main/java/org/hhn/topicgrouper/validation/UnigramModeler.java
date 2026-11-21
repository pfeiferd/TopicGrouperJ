package org.hhn.topicgrouper.validation;

import org.hhn.topicgrouper.doc.DocumentProvider;

/**
 * Baseline model in which every topic is the unigram distribution of the training corpus.
 *
 * @param <T> the type of the words
 */
public class UnigramModeler<T> extends AbstractTopicModelerWithProvider<T> {
	/**
	 * @param provider the training documents
	 * @param nTopics the number of topics
	 */
	public UnigramModeler(DocumentProvider<T> provider,
			int nTopics) {
		super(null, provider, nTopics);
		for (int i = 0; i < nTopics; i++) {
			for (int j = 0; j < nWords; j++) {
				phi[i][j] = getWordProb(j);
			}
		}
	}
	
	@Override
	public double getAlpha(int i) {
		return getAlphaConc() / nTopics;
	}

	@Override
	public double getAlphaConc() {
		return 1;
	}

	@Override
	public void setAlphaConc(double alphaConc) {
	}
}
