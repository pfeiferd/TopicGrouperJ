package org.hhn.topicgrouper.test;

import java.io.File;
import java.util.Random;

import org.hhn.topicgrouper.base.DocumentProvider;
import org.hhn.topicgrouper.base.InDocumentHoldOutSplitter;
import org.hhn.topicgrouper.eval.Reuters21578;
import org.hhn.topicgrouper.ldagibbs.GibbsSamplingLDAAdapt;
import org.hhn.topicgrouper.ldagibbs.GibbsSamplingLDAWithPerplexityInDoc;

public class LDAGibbsWithPerplexityReuters21578Tester {
	public static void main(String[] args) throws Exception {
		DocumentProvider<String> documentProvider = new Reuters21578(true)
				.getCorpusDocumentProvider(new File(
						"src/main/resources/reuters21578"),
						new String[] { "earn" }, false, true);

		InDocumentHoldOutSplitter<String> holdoutSplitter = new InDocumentHoldOutSplitter<String>(new Random(42),
				documentProvider, 0.1, 10);

		GibbsSamplingLDAAdapt gibbsSampler = new GibbsSamplingLDAWithPerplexityInDoc(
				holdoutSplitter.getRest(), 30, 1.6, 0.1, 3000, 10, "demo", "", 0,
				holdoutSplitter.getHoldOut(), 50);
		gibbsSampler.inference();
	}
}
