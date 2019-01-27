package org.hhn.topicgrouper.paper.classfication;

import org.hhn.topicgrouper.util.EvalRunner;

public class AllClassificationExperimentsRunner extends EvalRunner {
	@Override
	protected Class<?>[] getMainClasses() {
		return new Class[] { 
				OHSUMEDVocabIGClassificationExperiment.class,
				OHSUMEDVocabDFClassificationExperiment.class,
				OHSUMEDLDAClassificationExperiment.class,
				OHSUMEDLDAClassificationExperimentOpt.class,
				ReutersVocabIGClassificationExperiment.class,
				ReutersVocabDFClassificationExperiment.class,
				ReutersTGNaiveBayesExperiment.class,
				ReutersLDAClassificationExperiment.class,
				ReutersLDAClassificationExperimentOpt.class,
				TwentyNGVocabIGClassificationExperiment.class,
				TwentyNGVocabDFClassificationExperiment.class,
				TwentyNGTGNaiveBayesExperiment.class,
				TwentyNGLDAClassificationExperiment.class,
				TwentyNGLDAClassificationExperimentOpt.class
		};
	}

	public static void main(String[] args) {
		new AllClassificationExperimentsRunner().run(8);
	}
}
