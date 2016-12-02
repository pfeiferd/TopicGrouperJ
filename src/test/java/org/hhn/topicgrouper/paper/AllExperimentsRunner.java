package org.hhn.topicgrouper.paper;

import org.hhn.topicgrouper.paper.classfication.ReutersLDAClassificationExperiment;
import org.hhn.topicgrouper.paper.classfication.ReutersLDAClassificationExperimentOpt;
import org.hhn.topicgrouper.util.EvalRunner;

public class AllExperimentsRunner extends EvalRunner {
	@Override
	protected Class[] getMainClasses() {
		return new Class[] { /* ReutersTGNaiveBayesExperiment.class */
				ReutersLDAClassificationExperiment.class,
				ReutersLDAClassificationExperimentOpt.class };
	}

	public static void main(String[] args) {
		new AllExperimentsRunner().run(8);
	}
}
