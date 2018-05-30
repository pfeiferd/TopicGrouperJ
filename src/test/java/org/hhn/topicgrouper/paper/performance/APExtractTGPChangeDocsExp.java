package org.hhn.topicgrouper.paper.performance;

import java.io.File;
import java.util.Random;

import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.eval.APParser;

public class APExtractTGPChangeDocsExp extends
		TGPerformanceChangeDocsExp<String> {
	public APExtractTGPChangeDocsExp() {
		super(10);
	}

	@Override
	protected DocumentProvider<String> createBasicProvider() {
		return new APParser(false, true).getCorpusDocumentProvider(new File(
				"src/test/resources/ap-corpus/extract/ap.txt"));
	}

	public static void main(String[] args) {
		new APExtractTGPChangeDocsExp().runExperiment(new Random(43), 10, 15);
	}
}
