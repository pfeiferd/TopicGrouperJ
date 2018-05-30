package org.hhn.topicgrouper.paper.performance;

import java.io.File;
import java.util.Random;

import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.doc.LabelingDocumentProvider;
import org.hhn.topicgrouper.eval.TwentyNGParser;

public class TwentyNGTGPChangeDocsExp extends
		TGPerformanceChangeDocsExp<String> {
	public TwentyNGTGPChangeDocsExp() {
		super(5);
	}

	@Override
	protected DocumentProvider<String> createBasicProvider() {
		LabelingDocumentProvider<String, String> provider = new TwentyNGParser(
				null, true, true).getCorpusDocumentProvider(new File(
				"src/test/resources/20news-18828"), 1);
		return provider;
	}

	public static void main(String[] args) {
		new TwentyNGTGPChangeDocsExp().runExperiment(new Random(43), 10, 15);
	}
}
