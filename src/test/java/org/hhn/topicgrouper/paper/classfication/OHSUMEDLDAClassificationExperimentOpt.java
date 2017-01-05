package org.hhn.topicgrouper.paper.classfication;

import java.io.IOException;

public class OHSUMEDLDAClassificationExperimentOpt extends TwentyNGLDAClassificationExperiment {
	public OHSUMEDLDAClassificationExperimentOpt() throws IOException {
		super();
	}

	public static void main(String[] args) throws IOException {
		new OHSUMEDLDAClassificationExperimentOpt().run(true);
	}
}
