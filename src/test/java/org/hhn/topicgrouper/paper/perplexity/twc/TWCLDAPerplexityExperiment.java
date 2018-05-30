package org.hhn.topicgrouper.paper.perplexity.twc;

import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.paper.perplexity.AbstractLDAPerplexityNTopicsExperiment;

public class TWCLDAPerplexityExperiment extends
		AbstractLDAPerplexityNTopicsExperiment<String> {
	public TWCLDAPerplexityExperiment() {
		this(AllPerplexityExperimentsRunner.MAX_EVAL_TOPICS);
	}
	
	public TWCLDAPerplexityExperiment(int maxTopicEval) {
		super(maxTopicEval);
	}

	@Override
	protected void createTrainingAndTestProvider(DocumentProvider<String>[] res) {
		TWCTGPerplexityExperiment.createSplit(res);
	}
	
	public static void main(String[] args) {
		new TWCLDAPerplexityExperiment().run(false);
	}
}
