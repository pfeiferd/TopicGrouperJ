package org.hhn.topicgrouper.paper.perplexity.docs;

import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.paper.perplexity.AbstractLDAPerplexityNTopicsExperiment;

public class APLDAPerplexityExperimentOpt extends
		AbstractLDAPerplexityNTopicsExperiment<String> {
	public APLDAPerplexityExperimentOpt() {
		super(AllPerplexityExperimentsRunner.MAX_EVAL_TOPICS);
	}

	@Override
	protected void createTrainingAndTestProvider(DocumentProvider<String>[] res) {
		APTGPerplexityExperiment.createSplit(res);
	}
	
	public static void main(String[] args) {
		new APLDAPerplexityExperimentOpt().run(true);
	}
}
