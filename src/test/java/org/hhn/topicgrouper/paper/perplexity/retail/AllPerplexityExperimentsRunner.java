package org.hhn.topicgrouper.paper.perplexity.retail;

import org.hhn.topicgrouper.util.EvalRunner;

public class AllPerplexityExperimentsRunner extends EvalRunner {
	public static int MAX_EVAL_TOPICS = 200;

	@Override
	protected Class<?>[] getMainClasses() {
		return new Class[] { 
				/* TaFengTGPerplexityExperiment.class, 
				TaFengTGLRPerplexityExperiment.class,
				TaFengLDAPerplexityExperiment.class,
				TaFengLDAPerplexityExperimentOpt.class, */
				
				/* OnlineRetailTGPerplexityExperiment.class, */
//				OnlineRetailTGLRPerplexityExperiment.class,
				OnlineRetailLDAPerplexityExperiment.class,
				OnlineRetailLDAPerplexityExperimentOpt.class, 
				
//				
//				/* USSupermarketTGPerplexityExperiment.class, */
//				USSupermarketTGLRPerplexityExperiment.class,
//				USSupermarketLDAPerplexityExperiment.class,
//				USSupermarketLDAPerplexityExperimentOpt.class
				};
	}

	public static void main(String[] args) {
		new AllPerplexityExperimentsRunner().run(8);
	}
}
