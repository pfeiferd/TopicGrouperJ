package org.hhn.topicgrouper.paper.classfication;

import java.io.IOException;

public class OHSUMEDVocabDFClassificationExperimentOpt extends TwentyNGVocabDFClassificationExperiment {
	public OHSUMEDVocabDFClassificationExperimentOpt() throws IOException {
		super();
	}

	@Override
	protected double initialLambda() {
		return 0.3;
	}
	
	public static void main(String[] args) throws IOException {
		new OHSUMEDVocabDFClassificationExperimentOpt().run(true);
	}
}
