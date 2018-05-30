package org.hhn.topicgrouper.paper.perplexity.twc;

import org.hhn.topicgrouper.util.EvalRunner;

public class AllPerplexityExperimentsRunner extends EvalRunner {
	public static int MAX_EVAL_TOPICS = 20;
	
	@Override
	protected Class<?>[] getMainClasses() {
		return new Class[] { 
				TWCTGPerplexityChangeDocsExp.class,
				TWCLDAPerplexityChangeDocsExp.class,
				TWCTGPerplexityChangeAlphaExp.class,
				TWCTGPerplexityChangeAlphaExp2.class,
				TWCLDAPerplexityChangeAlphaExp.class,
				TWCLDAPerplexityChangeAlphaExp2.class,
				TWCPLSAPerplexityChangeAlphaExp.class,
				TWCPLSAPerplexityChangeAlphaExp2.class,
				TWCUnigramPerplexityChangeAlphaExp.class,
				TWCUnigramPerplexityChangeAlphaExp2.class,
				TWCPerfectPerplexityChangeAlphaExp.class,
				TWCPerfectPerplexityChangeAlphaExp2.class
				/*,
				TWCTGPerplexityExperiment.class,
				TWCTGLRPerplexityExperiment.class,
				TWCLDAPerplexityExperiment.class,
				TWCLDAPerplexityExperimentOpt.class,
				TWCPLSAPerplexityExperiment.class*/
		};
	}

	public static void main(String[] args) {
		new AllPerplexityExperimentsRunner().run(8);
	}
}
