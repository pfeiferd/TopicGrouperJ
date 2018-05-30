package org.hhn.topicgrouper.paper.perplexity.retail;

import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.paper.perplexity.AbstractLDAPerplexityNTopicsExperiment;

public class TaFengLDAPerplexityExperiment extends
		AbstractLDAPerplexityNTopicsExperiment<String> {
	public TaFengLDAPerplexityExperiment() {
		super(AllPerplexityExperimentsRunner.MAX_EVAL_TOPICS);
	}
	
	@Override
	protected int initTopicEvalSteps() {
		return 10;
	}

	@Override
	protected void createTrainingAndTestProvider(DocumentProvider<String>[] res) {
		TaFengTGPerplexityExperiment.createSplit(res);
	}
	
	public static void main(String[] args) {
		new TaFengLDAPerplexityExperiment().run(false);
	}
}
