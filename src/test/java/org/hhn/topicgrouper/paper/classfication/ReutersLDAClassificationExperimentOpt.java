package org.hhn.topicgrouper.paper.classfication;

import java.io.IOException;

public class ReutersLDAClassificationExperimentOpt extends ReutersLDAClassificationExperiment {
	public ReutersLDAClassificationExperimentOpt() throws IOException {
		super();
	}

	public static void main(String[] args) throws IOException {
		new ReutersLDAClassificationExperimentOpt().run(true);
	}
}
