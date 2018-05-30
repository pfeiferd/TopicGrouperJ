package org.hhn.topicgrouper.classify.impl.tg;

import org.hhn.topicgrouper.classify.impl.AbstractTopicBasedNBClassifier;
import org.hhn.topicgrouper.tg.TGSolution;

public class TGNBClassifier<T, L> extends
		AbstractTopicBasedNBClassifier<T, L> {
	protected final TGSolution<T> solution;
	protected final int[] topicIds;
	protected final double[] pt;

	public TGNBClassifier(double lambda, TGSolution<T> solution) {
		super(lambda);
		this.solution = solution;
		topicIds = solution.getTopicIds();
		pt = new double[topicIds.length];
		for (int i = 0; i < pt.length; i++) {
			pt[i] = ((double) solution.getTopicFrequency(topicIds[i]))
					/ solution.getSize();
		}
	}

	@Override
	protected int[] getTopicIndices() {
		return topicIds;
	}

	@Override
	protected int getTopicIndex(int wordIndex) {
		return solution.getTopicForWord(wordIndex);
	}

	@Override
	protected double[] getPt() {
		return pt;
	}
}
