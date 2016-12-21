package org.hhn.topicgrouper.paper.classfication;

import org.hhn.topicgrouper.paper.classfication.ReutersLDAClassificationExperiment;
import org.hhn.topicgrouper.paper.classfication.ReutersLDAClassificationExperimentOpt;
import org.hhn.topicgrouper.paper.classfication.ReutersTGNaiveBayesExperiment;
import org.hhn.topicgrouper.paper.classfication.ReutersVocabDFClassificationExperiment;
import org.hhn.topicgrouper.paper.classfication.ReutersVocabDFClassificationExperimentOpt;
import org.hhn.topicgrouper.paper.classfication.ReutersVocabIGClassificationExperiment;
import org.hhn.topicgrouper.paper.classfication.ReutersVocabIGClassificationExperimentOpt;
import org.hhn.topicgrouper.util.EvalRunner;

public class AllClassificationExperimentsRunner extends EvalRunner {
	@Override
	protected Class[] getMainClasses() {
		return new Class[] { ReutersVocabIGClassificationExperiment.class,
				ReutersVocabIGClassificationExperimentOpt.class,
				ReutersVocabDFClassificationExperiment.class,
				ReutersVocabDFClassificationExperimentOpt.class,
				ReutersTGNaiveBayesExperiment.class,
				ReutersLDAClassificationExperiment.class,
				ReutersLDAClassificationExperimentOpt.class };
	}

	public static void main(String[] args) {
		new AllClassificationExperimentsRunner().run(8);
	}
}
