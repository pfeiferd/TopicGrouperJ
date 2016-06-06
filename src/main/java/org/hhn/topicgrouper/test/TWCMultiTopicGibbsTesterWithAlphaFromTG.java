package org.hhn.topicgrouper.test;

import java.util.Random;

import org.hhn.topicgrouper.base.DocumentProvider;
import org.hhn.topicgrouper.base.HoldOutSplitter;
import org.hhn.topicgrouper.eval.TWCLDAPaperDocumentGenerator;
import org.hhn.topicgrouper.ldagibbs.MultiTopicGibbsLDAPerplixityAlt;
import org.hhn.topicgrouper.ldagibbs.MultiTopicGibbsLDAPerplixityAltWithTGAlpha;

public class TWCMultiTopicGibbsTesterWithAlphaFromTG {
	public static void main(String[] args) throws Exception {
		DocumentProvider<String> documentProvider = new TWCLDAPaperDocumentGenerator(
				new Random(45), new double[] { 5, 0.5, 0.5, 0.5 }, 6000, 100,
				100, 30, 30, 0, null, 0.5, 0.8);

		HoldOutSplitter<String> splitter = new HoldOutSplitter<String>(
				new Random(42), documentProvider, 0.1, 0);

		MultiTopicGibbsLDAPerplixityAlt multiTopicGibbsLDAPerplixityAlt = new MultiTopicGibbsLDAPerplixityAltWithTGAlpha(
				splitter.getRest(), splitter.getHoldOut());
		multiTopicGibbsLDAPerplixityAlt.inference(1, 1, 20, 100);
	}
}
