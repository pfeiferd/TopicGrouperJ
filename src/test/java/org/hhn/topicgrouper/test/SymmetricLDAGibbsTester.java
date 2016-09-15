package org.hhn.topicgrouper.test;

import java.util.Random;

import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.eval.TWCLDAPaperDocumentGenerator;
import org.hhn.topicgrouper.lda.impl.LDAGibbsSampler;
import org.hhn.topicgrouper.lda.report.BasicLDAResultReporter;

public class SymmetricLDAGibbsTester {
	public static void main(String[] args) throws Exception {
		DocumentProvider<String> documentProvider = new TWCLDAPaperDocumentGenerator();

		LDAGibbsSampler<String> gibbsSampler = new LDAGibbsSampler<String>(
				documentProvider, new double[] { 5, .5, .5, .5 }, 0.5,
				new Random());
		gibbsSampler.solve(1000, new BasicLDAResultReporter<String>(System.out,
				10));
	}
}
