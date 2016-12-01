package org.hhn.topicgrouper.paper;

import org.hhn.topicgrouper.util.EvalRunner;

public class AllExperimentsRunner extends EvalRunner {
	@Override
	protected Class[] getMainClasses() {
		return new Class[] { ReutersTGNaiveBayesExperiment.class };
	}
	
	public static void main(String[] args) {
		new AllExperimentsRunner().run(8);
	}
}
