package org.hhn.topicgrouper.figures;

import java.util.Random;

import org.hhn.topicgrouper.base.DocumentProvider;
import org.hhn.topicgrouper.eval.TWCLDAPaperDocumentGenerator;
import org.hhn.topicgrouper.ldaimpl.LDAGibbsSampler;
import org.hhn.topicgrouper.report.LDAPerplexityResultReporter;
import org.hhn.topicgrouper.validation.InDocumentHoldOutSplitter;
import org.hhn.topicgrouper.validation.LDAPerplexityCalculatorInDoc;

public class AsymmetricLDAGibbsTesterPM2 {
	public static void main(String[] args) throws Exception {
		DocumentProvider<String> documentProvider = new TWCLDAPaperDocumentGenerator(
				new Random(45), new double[] { 5, 0.5, 0.5, 0.5 }, 6000, 100,
				100, 300, 300, 0, null, 0.5, 0.8);

		InDocumentHoldOutSplitter<String> splitter = new InDocumentHoldOutSplitter<String>(
				new Random(42), documentProvider, 0.1, 0);

		LDAGibbsSampler<String> gibbsSampler = new LDAGibbsSampler<String>(
				splitter.getRest(), new double[] { 5, .5, .5, .5 }, 0.5,
				new Random());
		gibbsSampler.solve(1000, new LDAPerplexityResultReporter<String>(
				splitter.getHoldOut(), System.out, 10,
				new LDAPerplexityCalculatorInDoc<String>(false)));
	}
}
