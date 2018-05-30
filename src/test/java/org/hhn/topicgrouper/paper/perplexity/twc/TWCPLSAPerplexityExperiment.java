package org.hhn.topicgrouper.paper.perplexity.twc;

import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.paper.perplexity.AbstractPLSAPerplexityNTopicsExperiment;

public class TWCPLSAPerplexityExperiment extends
		AbstractPLSAPerplexityNTopicsExperiment<String> {
	public TWCPLSAPerplexityExperiment() {
		super(AllPerplexityExperimentsRunner.MAX_EVAL_TOPICS, 100);
	}

	@Override
	protected void createTrainingAndTestProvider(DocumentProvider<String>[] res) {
		TWCTGPerplexityExperiment.createSplit(res);
	}
	
	public static void main(String[] args) {
		new TWCPLSAPerplexityExperiment().run(false);
	}
}
