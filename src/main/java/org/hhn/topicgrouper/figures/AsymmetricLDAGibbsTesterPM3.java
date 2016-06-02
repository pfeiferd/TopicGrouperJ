package org.hhn.topicgrouper.figures;

import java.util.Random;

import org.hhn.topicgrouper.base.DocumentProvider;
import org.hhn.topicgrouper.base.HoldOutSplitter;
import org.hhn.topicgrouper.eval.TWCLDAPaperDocumentGenerator;
import org.hhn.topicgrouper.ldagibbs.GibbsSamplingLDAAdapt;
import org.hhn.topicgrouper.ldagibbs.GibbsSamplingLDAWithPerplexityAlt;

public class AsymmetricLDAGibbsTesterPM3 {
	public static void main(String[] args) throws Exception {
		DocumentProvider<String> documentProvider = new TWCLDAPaperDocumentGenerator();

		HoldOutSplitter<String> splitter = new HoldOutSplitter<String>(
				new Random(42), documentProvider, 0.1, 0);

		GibbsSamplingLDAAdapt gibbsSampler = new GibbsSamplingLDAWithPerplexityAlt(
				splitter.getRest(), new double[] { 5, 0.5, 0.5, 0.5 }, 0.5, 400, 10, "AsymmetricLDAGibbsTesterPM3", "", 0, splitter.getHoldOut());
		gibbsSampler.folderPath = "target/";
		gibbsSampler.inference();
	}
}
