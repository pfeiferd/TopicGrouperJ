package org.hhn.topicgrouper.paper.classfication;

import java.io.IOException;

import org.hhn.topicgrouper.doc.LabelingDocumentProvider;

public class OHSUMEDVocabIGClassificationExperiment extends ReutersVocabIGClassificationExperiment {
	public OHSUMEDVocabIGClassificationExperiment() throws IOException {
		super();
	}

	@Override
	protected void createTrainingAndTestProvider(
			LabelingDocumentProvider<String, String>[] res) {
		OHSUMEDTGNaiveBayesExperiment.createSplit(res);
	}
	
	public static void main(String[] args) throws IOException {
		new OHSUMEDVocabIGClassificationExperiment().run(false);
	}
}
