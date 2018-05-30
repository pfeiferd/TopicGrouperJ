package org.hhn.topicgrouper.paper.perplexity.docs;

import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.paper.perplexity.AbstractPLSAPerplexityNTopicsExperiment;

public class APExtractPLSAPerplexityExperiment extends
		AbstractPLSAPerplexityNTopicsExperiment<String> {
	public APExtractPLSAPerplexityExperiment() {
		super(AllPerplexityExperimentsRunner.MAX_EVAL_TOPICS, 150);
	}

	@Override
	protected void createTrainingAndTestProvider(DocumentProvider<String>[] res) {
		APExtractTGPerplexityExperiment.createSplit(res);
	}
	
	public static void main(String[] args) {
		new APExtractPLSAPerplexityExperiment().run(false);
	}
}
