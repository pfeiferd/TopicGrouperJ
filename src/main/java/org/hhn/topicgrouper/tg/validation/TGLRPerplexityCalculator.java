package org.hhn.topicgrouper.tg.validation;

import java.util.Random;

import org.hhn.topicgrouper.doc.DocumentSplitter;
import org.hhn.topicgrouper.validation.BasicPerplexityCalculator;
import org.hhn.topicgrouper.validation.PerplexityCalculatorLeftToRight;

public class TGLRPerplexityCalculator<T> extends TGPerplexityCalculator<T> {
	private final Random random;

	public TGLRPerplexityCalculator(Random random,
			boolean bowFactor, DocumentSplitter<T> documentSplitter, 
			double alphaConc) {
		super(bowFactor, documentSplitter, alphaConc);
		this.random = random;
	}
	
	protected int initParticles() {
		return 20;
	}

	protected BasicPerplexityCalculator<T> createBasicPerplexityCalculator() {
		return new PerplexityCalculatorLeftToRight<T>(random, bowFactor, initParticles());
	}
}
