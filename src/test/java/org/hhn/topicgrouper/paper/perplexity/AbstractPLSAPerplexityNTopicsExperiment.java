package org.hhn.topicgrouper.paper.perplexity;

import java.util.Random;

import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.plsa.impl.PLSA;
import org.hhn.topicgrouper.validation.AbstractTopicModeler;
import org.hhn.topicgrouper.validation.BasicPerplexityCalculator;

public abstract class AbstractPLSAPerplexityNTopicsExperiment<T> extends
		AbstractTopicModelerPerplexityNTopicsExperiment<T> {
	private final int trainingIterations;

	public AbstractPLSAPerplexityNTopicsExperiment(int maxTopicEval,
			int trainingIterations) {
		super(maxTopicEval);
		this.trainingIterations = trainingIterations;
	}

	@Override
	protected void printOutputHeader() {
		printStream.println("topics; perplexityMean; perplexityLR");
	}

	@Override
	protected BasicPerplexityCalculator<T> initPerplexityCalculator3() {
		return null;
	}

	@Override
	protected void printResult(int topics, double perplexity1,
			double perplexity2, double perlexity3) {
		printStream.println(topics + "; " + perplexity1 + "; " + perplexity2);
	}

	@Override
	protected AbstractTopicModeler<T> createTopicModeler(int topics,
			DocumentProvider<T> documentProvider, boolean optimize) {
		return new PLSA<T>(createRandom(), documentProvider, topics, 50);
	}
	
	protected Random createRandom() {
		return new Random(42);
	}
	
	@Override
	protected void trainTopicModeler(AbstractTopicModeler<T> modeler, DocumentProvider<T> documentProvider, 
			boolean optimize) {
		((PLSA<T>) modeler).train(trainingIterations);
		if (optimize) {
			calc2.setTopicModeler(modeler);
			calc2.optimizeAlphaConc(1, 250, documentProvider, 10);
		}
	}
}
