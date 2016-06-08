package org.hhn.topicgrouper.figures;

import org.hhn.topicgrouper.base.DocumentProvider;
import org.hhn.topicgrouper.eval.TWCLDAPaperDocumentGenerator;
import org.hhn.topicgrouper.ldagibbs.GibbsSamplingLDAAdapt;

public class AsymmetricLDAGibbsTester {
	public static void main(String[] args) throws Exception {
		DocumentProvider<String> documentProvider = new TWCLDAPaperDocumentGenerator();

		GibbsSamplingLDAAdapt gibbsSampler = new GibbsSamplingLDAAdapt(
				documentProvider, new double[] { 5, 0.5, 0.5, 0.5 }, 0.5, 1000, 10, "AsymmetricLDAGibbsTester", "", 0);
		gibbsSampler.folderPath = "target/";
		gibbsSampler.inference();
	}
}