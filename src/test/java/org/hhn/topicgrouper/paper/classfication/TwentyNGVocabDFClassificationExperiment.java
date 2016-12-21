package org.hhn.topicgrouper.paper.classfication;

import java.io.IOException;

import org.hhn.topicgrouper.doc.LabelingDocumentProvider;

public class TwentyNGVocabDFClassificationExperiment extends ReutersVocabDFClassificationExperiment {
	public TwentyNGVocabDFClassificationExperiment() throws IOException {
		super();
	}

	@Override
	protected void createTrainingAndTestProvider(
			LabelingDocumentProvider<String, String>[] res) {
		TwentyNGTGNaiveBayesExperiment.createSplit(res);
	}
	
	public static void main(String[] args) throws IOException {
		new TwentyNGVocabDFClassificationExperiment().run(false);
	}
}
