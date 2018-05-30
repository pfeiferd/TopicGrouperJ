package org.hhn.topicgrouper.paper.performance;

import java.io.File;
import java.util.Random;

import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.doc.LabelingDocumentProvider;
import org.hhn.topicgrouper.eval.Reuters21578;

public class ReutersTGPChangeDocsExp extends
		TGPerformanceChangeDocsExp<String> {
	public ReutersTGPChangeDocsExp() {
		super(3);
	}

	@Override
	protected DocumentProvider<String> createBasicProvider() {
		Reuters21578 reuters = new Reuters21578(true); // Excluding stop words.
		LabelingDocumentProvider<String, String> trainingData = reuters
				.getCorpusDocumentProvider(new File(
						"src/test/resources/reuters21578"), true, false);
		return trainingData;
	}

	public static void main(String[] args) {
		new ReutersTGPChangeDocsExp().runExperiment(new Random(43), 10, 15);
	}
}
