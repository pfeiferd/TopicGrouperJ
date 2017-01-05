package org.hhn.topicgrouper.paper.classfication;

import java.io.IOException;

import org.hhn.topicgrouper.doc.LabelingDocumentProvider;

public class OHSUMEDVocabDFClassificationExperiment extends ReutersVocabDFClassificationExperiment {
	public OHSUMEDVocabDFClassificationExperiment() throws IOException {
		super();
	}

	@Override
	protected void createTrainingAndTestProvider(
			LabelingDocumentProvider<String, String>[] res) {
		OHSUMEDTGNaiveBayesExperiment.createSplit(res);
	}
	
	public static void main(String[] args) throws IOException {
		new OHSUMEDVocabDFClassificationExperiment().run(false);
	}
}
