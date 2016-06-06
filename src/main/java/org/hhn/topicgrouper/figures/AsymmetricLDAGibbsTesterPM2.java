package org.hhn.topicgrouper.figures;

import java.util.Random;

import org.hhn.topicgrouper.base.DocumentProvider;
import org.hhn.topicgrouper.base.InDocumentHoldOutSplitter;
import org.hhn.topicgrouper.eval.TWCLDAPaperDocumentGenerator;
import org.hhn.topicgrouper.ldagibbs.BasicGibbsSolutionReporter;
import org.hhn.topicgrouper.ldagibbs.GibbsSamplingLDAAdapt;
import org.hhn.topicgrouper.ldagibbs.GibbsSamplingLDAWithPerplexityInDoc;

public class AsymmetricLDAGibbsTesterPM2 {
	public static void main(String[] args) throws Exception {
		DocumentProvider<String> documentProvider = new TWCLDAPaperDocumentGenerator(
				new Random(45), new double[] { 5, 0.5, 0.5, 0.5 }, 6000, 100,
				100, 300, 300, 0, null, 0.5, 0.8);

		InDocumentHoldOutSplitter<String> splitter = new InDocumentHoldOutSplitter<String>(
				new Random(42), documentProvider, 0.1, 0);

		GibbsSamplingLDAAdapt gibbsSampler = new GibbsSamplingLDAWithPerplexityInDoc(
				new BasicGibbsSolutionReporter(System.out), splitter.getRest(),
				new double[] { 5, 0.5, 0.5, 0.5 }, 0.5, 400, 10,
				"AsymmetricLDAGibbsTesterPM2", "", 0, splitter.getHoldOut(), 2);
		gibbsSampler.folderPath = "target/";
		gibbsSampler.inference();
	}
}
