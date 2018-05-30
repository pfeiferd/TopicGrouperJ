package org.hhn.topicgrouper.paper.perplexity.retail;

import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.paper.perplexity.AbstractLDAPerplexityNTopicsExperiment;

public class OnlineRetailLDAPerplexityExperiment extends
		AbstractLDAPerplexityNTopicsExperiment<String> {
	public OnlineRetailLDAPerplexityExperiment() {
		super(AllPerplexityExperimentsRunner.MAX_EVAL_TOPICS);
	}
	
//	@Override
//	protected int initTopicEvalSteps() {
//		return 20;
//	}

	@Override
	protected void createTrainingAndTestProvider(DocumentProvider<String>[] res) {
		OnlineRetailTGPerplexityExperiment.createSplit(res);
	}
	
	public static void main(String[] args) {
		new OnlineRetailLDAPerplexityExperiment().run(false);
	}
}
