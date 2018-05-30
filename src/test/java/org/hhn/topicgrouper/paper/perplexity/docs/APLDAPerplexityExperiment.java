package org.hhn.topicgrouper.paper.perplexity.docs;

import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.paper.perplexity.AbstractLDAPerplexityNTopicsExperiment;

public class APLDAPerplexityExperiment extends
		AbstractLDAPerplexityNTopicsExperiment<String> {
	public APLDAPerplexityExperiment() {
		super(AllPerplexityExperimentsRunner.MAX_EVAL_TOPICS);
	}

	@Override
	protected void createTrainingAndTestProvider(DocumentProvider<String>[] res) {
		APTGPerplexityExperiment.createSplit(res);
	}
	
	public static void main(String[] args) {
		new APLDAPerplexityExperiment().run(false);
	}
}
