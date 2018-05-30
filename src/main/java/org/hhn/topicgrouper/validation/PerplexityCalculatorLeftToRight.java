package org.hhn.topicgrouper.validation;

import java.util.Random;

import org.hhn.topicgrouper.doc.Document;
import org.hhn.topicgrouper.doc.DocumentSplitter;
import org.hhn.topicgrouper.doc.impl.DefaultDocumentSplitter;

public class PerplexityCalculatorLeftToRight<T> extends
		BasicPerplexityCalculator<T> {
	protected final Random random;
	protected final int particles;
	private LeftToRightSamplerBase<T> lrpSampler;

	public PerplexityCalculatorLeftToRight(Random random, boolean bowFactor) {
		this(random, bowFactor, 20);
	}
	
	public PerplexityCalculatorLeftToRight(Random random, boolean bowFactor,
			int particles) {
		this(random, bowFactor, new DefaultDocumentSplitter<T>(), particles);
	}

	public PerplexityCalculatorLeftToRight(Random random, boolean bowFactor,
			DocumentSplitter<T> documentSplitter, int particles) {
		super(bowFactor, documentSplitter, 1);
		this.random = random;
		this.particles = particles;
	}
	
	protected boolean useUnbiased() {
		return true;
	}

	@Override
	public void setTopicModeler(AbstractTopicModeler<T> topicModeler) {
		super.setTopicModeler(topicModeler);
		lrpSampler = createLeftToRightParticleSampler(topicModeler);
	}

	@Override
	protected double computeLogProbability(Document<T> refD, Document<T> d,
			AbstractTopicModeler<T> sampler) {
		return lrpSampler.leftToRightDocCompletion(refD, d);
	}

	@Override
	protected void initPtd(Document<T> d, AbstractTopicModeler<T> sampler) {
	}

	@Override
	protected void updatePtd(Document<T> d, AbstractTopicModeler<T> sampler) {
	}

	protected LeftToRightSamplerBase<T> createLeftToRightParticleSampler(
			final AbstractTopicModeler<T> sampler) {
		if (useUnbiased()) {
			return new LeftToRightSequentialSampler<T>(random, particles,
					sampler.getNTopics()) {
				@Override
				protected double getAlpha(int t) {
					return sampler.getAlpha(t);
				}

				@Override
				protected double getAlphaSum() {
					return sampler.getAlphaConc();
				}

				@Override
				protected double getPhi(int t, int wordIndex) {
					return sampler.getPhi(t, wordIndex);
				}
			};
		} else {
			return new LeftToRightParticleSampler<T>(random, particles,
					sampler.getNTopics()) {
				@Override
				protected double getAlpha(int t) {
					return sampler.getAlpha(t);
				}

				@Override
				protected double getAlphaSum() {
					return sampler.getAlphaConc();
				}

				@Override
				protected double getPhi(int t, int wordIndex) {
					return sampler.getPhi(t, wordIndex);
				}
			};
		}
	}
}
