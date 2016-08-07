package org.hhn.topicgrouper.figures;

import java.util.Random;

import org.hhn.topicgrouper.base.DocumentProvider;
import org.hhn.topicgrouper.eval.TWCLDAPaperDocumentGenerator;
import org.hhn.topicgrouper.ldaimpl.LDAGibbsSampler;
import org.hhn.topicgrouper.report.BasicLDAResultReporter;

public class AsymmetricLDAGibbsTester {
	public static void main(String[] args) throws Exception {
		DocumentProvider<String> documentProvider = new TWCLDAPaperDocumentGenerator();

		LDAGibbsSampler<String> gibbsSampler = new LDAGibbsSampler<String>(
				documentProvider, new double[] { 5, .5, .5, .5 }, 0.5,
				new Random());
		gibbsSampler.solve(1000, new BasicLDAResultReporter<String>(System.out,
				10));
	}
}
