package org.hhn.topicgrouper.paper.performance;

import org.hhn.topicgrouper.util.EvalRunner;

public class AllPerformanceExperimentsRunner extends EvalRunner {
	@Override
	protected Class<?>[] getMainClasses() {
		return new Class[] { 
				APExtractTGPChangeDocsExp.class,
				ReutersTGPChangeDocsExp.class,
				TwentyNGTGPChangeDocsExp.class,
//				APExtractLowMemTGPChangeDocsExp.class,
//				ReutersLowMemTGPChangeDocsExp.class,
//				TwentyNGLowMemTGPChangeDocsExp.class,
		};
	}

	public static void main(String[] args) {
		new AllPerformanceExperimentsRunner().run(1);
	}

}
