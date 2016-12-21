package org.hhn.topicgrouper.paper.classfication;

import java.io.IOException;

import org.hhn.topicgrouper.doc.LabelingDocumentProvider;

public class TwentyNGLDAClassificationExperiment extends ReutersLDAClassificationExperiment {
	public TwentyNGLDAClassificationExperiment() throws IOException {
		super();
	}

	@Override
	protected void createTrainingAndTestProvider(
			LabelingDocumentProvider<String, String>[] res) {
		TwentyNGTGNaiveBayesExperiment.createModApteSplit(res);
	}

	public static void main(String[] args) throws IOException {
		new TwentyNGLDAClassificationExperiment().run(false);
	}	
}
