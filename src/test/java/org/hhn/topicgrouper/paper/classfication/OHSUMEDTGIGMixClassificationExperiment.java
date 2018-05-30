package org.hhn.topicgrouper.paper.classfication;

import java.io.IOException;

import org.hhn.topicgrouper.doc.LabelingDocumentProvider;

public class OHSUMEDTGIGMixClassificationExperiment extends
		ReutersTGIGMixClassificationExperiment {
	public OHSUMEDTGIGMixClassificationExperiment(Class<?> clazz)
			throws IOException {
		super(clazz);
	}
	
	@Override
	protected void createTrainingAndTestProvider(
			LabelingDocumentProvider<String, String>[] res) {
		OHSUMEDTGNaiveBayesExperiment.createSplit(res);
	}	

	public static void main(String[] args) throws IOException {
		new OHSUMEDTGNaiveBayesExperiment().run();
		new OHSUMEDTGIGMixClassificationExperiment(
				OHSUMEDTGNaiveBayesExperiment.class).run(false);
	}
}
