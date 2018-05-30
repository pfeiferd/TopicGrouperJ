package org.hhn.topicgrouper.paper.perplexity.twc;

import java.util.Random;

import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.doc.impl.DefaultVocab;
import org.hhn.topicgrouper.doc.impl.HoldOutSplitter;
import org.hhn.topicgrouper.tg.validation.TGLRPerplexityCalculator;
import org.hhn.topicgrouper.tg.validation.TGPerplexityCalculator;

public class TWCTGLRPerplexityExperiment extends
		TWCTGPerplexityExperiment {
	public TWCTGLRPerplexityExperiment() {
	}
	
	@Override
	protected void optimizeParameters(DocumentProvider<String> trainingDocumentProvider) {
		HoldOutSplitter<String> holdoutSplitter = new HoldOutSplitter<String>(
				new Random(10), trainingDocumentProvider, 0.1, 0, false,
				(DefaultVocab<String>) trainingDocumentProvider.getVocab());
		perplexityCalculator
				.optimizeAlphaConc(1, 250, holdoutSplitter.getHoldOut(), 20);
		// perplexityCalculator.optimizeLambda(0, 1, trainingProvider, solution,
		// 10);
	}
	
	@Override
	protected TGPerplexityCalculator<String> createTGPerplexityCalculator() {
		return new TGLRPerplexityCalculator<String>(new Random(42), false, createDocumentSplitter(), 50);
	}

	public static void main(String[] args){
		new TWCTGLRPerplexityExperiment().run();
	}
}
