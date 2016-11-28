package org.hhn.topicgrouper.classify.impl.lda;

import java.util.Arrays;

import org.hhn.topicgrouper.classify.impl.AbstractTopicBasedNBClassifier;
import org.hhn.topicgrouper.doc.Document;
import org.hhn.topicgrouper.lda.impl.LDAGibbsSampler;

public class LDANBClassifier<T, L> extends AbstractTopicBasedNBClassifier<T, L> {
	private final LDAGibbsSampler<T> ldaGibbsSampler;
	private final int foldInterations;

	public LDANBClassifier(double lambda, LDAGibbsSampler<T> ldaGibbsSampler,
			int foldIterations) {
		super(lambda);
		this.ldaGibbsSampler = ldaGibbsSampler;
		this.foldInterations = foldIterations;
	}

	@Override
	protected void computeTopicFrequency(Document<T> d, double[] v, boolean add) {
		if (!add) {
			Arrays.fill(v, 0);
		}
		for (int i = 0; i < v.length; i++) {
			// We are just getting the current sample state from the Gibbs sampler.
			// To be exact, we would have to average over the examples.
			// TODO: Really wanna do this?
			v[i] += ldaGibbsSampler.getDocumentTopicAssignmentCount(
					d.getIndex(), i);
		}
	}

	@Override
	protected void computeTopicFrequencyTest(Document<T> d, double[] v,
			boolean add) {
		// We are just getting the current fold in sample from the Gibbs sampler.
		// To be exact, we would have to average over the examples.
		// TODO: Really wanna do this?
		LDAGibbsSampler<T>.FoldInStore store = ldaGibbsSampler.foldIn(
				foldInterations, d);
		int[] h = store.getDTopicAssignmentCounts();

		if (!add) {
			Arrays.fill(v, 0);
		}
		for (int i = 0; i < v.length; i++) {
			v[i] = h[i];
		}
	}

	@Override
	protected int getNTopics() {
		return ldaGibbsSampler.getNTopics();
	}

	@Override
	protected int[] getTopicIndices() {
		return new int[0];
	}

	@Override
	protected int getTopicIndex(int wordIndex) {
		throw new IllegalStateException("should not get called");
	}
}
