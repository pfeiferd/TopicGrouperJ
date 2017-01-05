package org.hhn.topicgrouper.paper.classfication;

import java.io.IOException;

public class OHSUMEDVocabIGClassificationExperimentOpt extends TwentyNGVocabIGClassificationExperiment {
	public OHSUMEDVocabIGClassificationExperimentOpt() throws IOException {
		super();
	}

	@Override
	protected double initialLambda() {
		return 0.3;
	}
	
	public static void main(String[] args) throws IOException {
		new OHSUMEDVocabIGClassificationExperimentOpt().run(true);
	}
}
