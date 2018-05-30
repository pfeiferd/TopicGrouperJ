package org.hhn.topicgrouper.paper.perplexity.retail;

import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.paper.perplexity.AbstractLDAPerplexityNTopicsExperiment;

public class USSupermarketLDAPerplexityExperiment extends
		AbstractLDAPerplexityNTopicsExperiment<String> {
	public USSupermarketLDAPerplexityExperiment() {
		super(AllPerplexityExperimentsRunner.MAX_EVAL_TOPICS);
	}
	
	@Override
	protected int initTopicEvalSteps() {
		return 20;
	}

	@Override
	protected void createTrainingAndTestProvider(DocumentProvider<String>[] res) {
		USSupermarketTGPerplexityExperiment.createSplit(res);
	}
	
	public static void main(String[] args) {
		new USSupermarketLDAPerplexityExperiment().run(false);
	}
}
