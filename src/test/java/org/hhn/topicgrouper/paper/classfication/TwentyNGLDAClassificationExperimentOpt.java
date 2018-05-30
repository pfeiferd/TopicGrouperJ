package org.hhn.topicgrouper.paper.classfication;

import java.io.IOException;

public class TwentyNGLDAClassificationExperimentOpt extends TwentyNGLDAClassificationExperiment {
	public TwentyNGLDAClassificationExperimentOpt() throws IOException {
		super();
	}

	public static void main(String[] args) throws IOException {
		new TwentyNGLDAClassificationExperimentOpt().run(true);
	}
}
