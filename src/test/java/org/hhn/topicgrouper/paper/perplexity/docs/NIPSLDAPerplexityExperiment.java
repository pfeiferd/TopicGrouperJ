package org.hhn.topicgrouper.paper.perplexity.docs;

import java.io.IOException;

import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.paper.perplexity.AbstractLDAPerplexityNTopicsExperiment;

public class NIPSLDAPerplexityExperiment extends
		AbstractLDAPerplexityNTopicsExperiment<String> {
	public NIPSLDAPerplexityExperiment() {
		super(AllPerplexityExperimentsRunner.MAX_EVAL_TOPICS);
	}

	@Override
	protected void createTrainingAndTestProvider(DocumentProvider<String>[] res) {
		NIPSTGLRPerplexityExperiment.createSplit(res);
	}
	
	public static void main(String[] args) {
		new NIPSLDAPerplexityExperiment().run(false);
	}
}
