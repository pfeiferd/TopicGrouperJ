package org.hhn.topicgrouper.paper.classfication;

import java.io.IOException;

public class ReutersVocabDFClassificationExperimentOpt extends ReutersVocabDFClassificationExperiment {
	public ReutersVocabDFClassificationExperimentOpt() throws IOException {
		super();
	}

	public static void main(String[] args) throws IOException {
		new ReutersVocabDFClassificationExperimentOpt().run(true);
	}
}
