package org.hhn.topicgrouper.test;

import java.io.File;
import java.util.Random;

import org.hhn.topicgrouper.base.DocumentProvider;
import org.hhn.topicgrouper.eval.APParser;
import org.hhn.topicgrouper.ldagibbs.MultiTopicGibbsLDAPerplixityAlt;
import org.hhn.topicgrouper.ldagibbs.MultiTopicGibbsLDAPerplixityAltWithTGAlpha;
import org.hhn.topicgrouper.validation.HoldOutSplitter;

public class TWCMultiTopicGibbsTesterWithAlphaFromTG {
	public static void main(String[] args) throws Exception {
		DocumentProvider<String> documentProvider = new APParser(true)
				.getCorpusDocumentProvider(new File(
						"src/test/resources/ap-corpus/extract/ap.txt"));
		// DocumentProvider<String> documentProvider = new
		// TWCLDAPaperDocumentGenerator(
		// new Random(45), new double[] { 5, 0.5, 0.5, 0.5 }, 6000, 100,
		// 100, 30, 30, 0, null, 0.5, 0.8);

		HoldOutSplitter<String> splitter = new HoldOutSplitter<String>(
				new Random(42), documentProvider, 0.1, 10);

		MultiTopicGibbsLDAPerplixityAlt multiTopicGibbsLDAPerplixityAlt = new MultiTopicGibbsLDAPerplixityAltWithTGAlpha(
				splitter.getRest(), splitter.getHoldOut());
		multiTopicGibbsLDAPerplixityAlt.inference(10, 10, 20, 500);
	}
}
