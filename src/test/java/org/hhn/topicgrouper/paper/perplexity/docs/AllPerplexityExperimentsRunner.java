package org.hhn.topicgrouper.paper.perplexity.docs;

import org.hhn.topicgrouper.util.EvalRunner;

public class AllPerplexityExperimentsRunner extends EvalRunner {
	public static int MAX_EVAL_TOPICS = 200;
	
	@Override
	protected Class<?>[] getMainClasses() {
		return new Class[] {
				APExtractPLSAPerplexityExperiment.class,
				APExtractTGPerplexityExperiment.class,
				APExtractLDAPerplexityExperiment.class,
				APExtractLDAPerplexityExperimentOpt.class,
				APExtractTGLRPerplexityExperiment.class,
				APTGPerplexityExperiment.class,
				APLDAPerplexityExperiment.class,
				APLDAPerplexityExperimentOpt.class,
				NIPSTGLRPerplexityExperiment.class,
				NIPSLDAPerplexityExperiment.class,
				NIPSLDAPerplexityExperimentOpt.class
			};
	}

	public static void main(String[] args) {
		new AllPerplexityExperimentsRunner().run(4);
	}
	
}
