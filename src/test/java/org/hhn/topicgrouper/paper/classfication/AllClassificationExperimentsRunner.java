package org.hhn.topicgrouper.paper.classfication;

import org.hhn.topicgrouper.util.EvalRunner;

public class AllClassificationExperimentsRunner extends EvalRunner {
	@Override
	protected Class[] getMainClasses() {
		return new Class[] { 
				TwentyNGVocabIGClassificationExperiment.class,
				TwentyNGVocabIGClassificationExperimentOpt.class,
				TwentyNGVocabDFClassificationExperiment.class,
				TwentyNGVocabDFClassificationExperimentOpt.class,
				TwentyNGTGNaiveBayesExperiment.class,
				TwentyNGLDAClassificationExperiment.class,
				TwentyNGLDAClassificationExperimentOpt.class /*,
				ReutersVocabIGClassificationExperiment.class,
				ReutersVocabIGClassificationExperimentOpt.class,
				ReutersVocabDFClassificationExperiment.class,
				ReutersVocabDFClassificationExperimentOpt.class,
				ReutersTGNaiveBayesExperiment.class,
				ReutersLDAClassificationExperiment.class,
				ReutersLDAClassificationExperimentOpt.class */	
		};
	}

	public static void main(String[] args) {
		new AllClassificationExperimentsRunner().run(8);
	}
}
