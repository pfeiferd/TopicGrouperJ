package org.hhn.topicgrouper.test;

import java.io.File;
import java.util.Random;

import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.doc.impl.InDocumentHoldOutSplitter;
import org.hhn.topicgrouper.eval.APParser;
import org.hhn.topicgrouper.ldagibbs.GibbsSamplingLDAAdapt;
import org.hhn.topicgrouper.ldagibbs.GibbsSamplingLDAWithPerplexityInDoc;

public class LDAGibbsWithPerplexityReuters21578Tester {
	public static void main(String[] args) throws Exception {
		DocumentProvider<String> documentProvider = new APParser(true)
		.getCorpusDocumentProvider(new File(
				"src/test/resources/ap-corpus/extract/ap.txt"));
//		DocumentProvider<String> documentProvider = new Reuters21578(true)
//				.getCorpusDocumentProvider(new File(
//						"src/test/resources/reuters21578"),
//						new String[] { "earn" }, false, true);

		InDocumentHoldOutSplitter<String> holdoutSplitter = new InDocumentHoldOutSplitter<String>(new Random(42),
				documentProvider, 0.1, 10);

		GibbsSamplingLDAAdapt gibbsSampler = new GibbsSamplingLDAWithPerplexityInDoc(
				holdoutSplitter.getRest(), 100, 0.5, 0.1, 1000, 10, "demo", "", 0,
				holdoutSplitter.getHoldOut(), 50);
		gibbsSampler.inference();
	}
}
