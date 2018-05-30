package org.hhn.topicgrouper.paper.perplexity.docs;

import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.paper.perplexity.AbstractLDAPerplexityNTopicsExperiment;

public class APExtractLDAPerplexityExperiment extends
		AbstractLDAPerplexityNTopicsExperiment<String> {
	public APExtractLDAPerplexityExperiment() {
		super(AllPerplexityExperimentsRunner.MAX_EVAL_TOPICS);
	}

	@Override
	protected void createTrainingAndTestProvider(DocumentProvider<String>[] res) {
		APExtractTGPerplexityExperiment.createSplit(res);
	}
	
	public static void main(String[] args) {
		new APExtractLDAPerplexityExperiment().run(false);
	}
}
