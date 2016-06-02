package org.hhn.topicgrouper.test;

import org.hhn.topicgrouper.base.DocumentProvider;
import org.hhn.topicgrouper.eval.TWCLDAPaperDocumentGenerator;
import org.hhn.topicgrouper.ldagibbs.GibbsSamplingLDAAdapt;

public class SymmetricLDAGibbsTester {
	public static void main(String[] args) throws Exception {
		DocumentProvider<String> documentProvider = new TWCLDAPaperDocumentGenerator();

		GibbsSamplingLDAAdapt gibbsSampler = new GibbsSamplingLDAAdapt(
				documentProvider, new double[] { 1.5, 1.5, 1.5, 1.5 }, 0.1, 1000, 100, "demo", "", 0);
		gibbsSampler.inference();
	}
}
