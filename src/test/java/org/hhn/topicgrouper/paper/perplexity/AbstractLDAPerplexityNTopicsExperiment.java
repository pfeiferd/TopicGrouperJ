package org.hhn.topicgrouper.paper.perplexity;

import java.util.Random;

import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.lda.impl.LDAGibbsSampler;
import org.hhn.topicgrouper.lda.report.BasicLDAResultReporter;
import org.hhn.topicgrouper.validation.AbstractTopicModeler;
import org.hhn.topicgrouper.validation.BasicPerplexityCalculator;

public abstract class AbstractLDAPerplexityNTopicsExperiment<T> extends
		AbstractTopicModelerPerplexityNTopicsExperiment<T> {
	protected final int foldInIterations;

	public AbstractLDAPerplexityNTopicsExperiment(int maxTopicEval) {
		super(maxTopicEval);
		this.foldInIterations = initFoldInIterations();
	}
	
	protected int initFoldInIterations() {
		return 100;
	}

	@Override
	protected BasicPerplexityCalculator<T> initPerplexityCalculator3() {
		return null;
		// return new PerplexityCalculatorEstimatedTheta<T>(false,
		// createDocumentSplitter(), foldInIterations, 1000);
	}

	@Override
	protected AbstractTopicModeler<T> createTopicModeler(int topics,
			DocumentProvider<T> documentProvider, boolean optimize) {
		// Initial alpha and beta like in:
		// http://psiexp.ss.uci.edu/research/papers/sciencetopics.pdf
		// and
		// http://stats.stackexchange.com/questions/59684/what-are-typical-values-to-use-for-alpha-and-beta-in-latent-dirichlet-allocation
		LDAGibbsSampler<T> ldaGibbsSampler = new LDAGibbsSampler<T>(createRandom(topics), documentProvider, createAlpha(topics), createBeta());
		ldaGibbsSampler.setUpdateAlphaBeta(optimize);
		return ldaGibbsSampler;
	}
	
	protected Random createRandom(int topics) {
		return new Random(42);
	}

	@Override
	protected void trainTopicModeler(AbstractTopicModeler<T> modeler, DocumentProvider<T> documentProvider, 
			boolean optimize) {
		((LDAGibbsSampler<T>) modeler).solve(2000, 1000,
				new BasicLDAResultReporter<T>(System.out, 10));
	}
	
	protected double createBeta() {
		return 0.1;
	}

	protected double[] createAlpha(int topics) {
		return LDAGibbsSampler.symmetricAlpha(50d / topics, topics);
	}
}
