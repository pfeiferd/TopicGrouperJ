package org.hhn.topicgrouper.paper.perplexity.docs;

import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.paper.perplexity.AbstractLDAPerplexityNTopicsExperiment;

public class NIPSLDAPerplexityExperimentOpt extends
		AbstractLDAPerplexityNTopicsExperiment<String> {
	public NIPSLDAPerplexityExperimentOpt() {
		super(AllPerplexityExperimentsRunner.MAX_EVAL_TOPICS);
	}

	@Override
	protected void createTrainingAndTestProvider(DocumentProvider<String>[] res) {
		NIPSTGLRPerplexityExperiment.createSplit(res);
	}
	
	public static void main(String[] args) {
		new NIPSLDAPerplexityExperimentOpt().run(true);
	}
}
