package org.hhn.topicgrouper.validation;

import org.hhn.topicgrouper.doc.DocumentProvider;

public class UnigramModeler<T> extends AbstractTopicModelerWithProvider<T> {
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
