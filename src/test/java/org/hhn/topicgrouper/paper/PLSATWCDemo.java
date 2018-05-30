package org.hhn.topicgrouper.paper;

import java.util.Random;

import org.hhn.topicgrouper.eval.TWCLDAPaperDocumentGenerator;
import org.hhn.topicgrouper.lda.report.BasicLDAResultReporter;
import org.hhn.topicgrouper.plsa.impl.PLSA;

public class PLSATWCDemo {
	public PLSATWCDemo() {
		PLSA<String> plsa = new PLSA<String>(new Random(42),
				new TWCLDAPaperDocumentGenerator(new Random(45), new double[] {
						5, 0.5, 0.5, 0.5 }, 6000, 100, 100, 30, 30, 0, null,
						0.5, 0.8), 100, 50);
		plsa.train(100);
		BasicLDAResultReporter.printTopics(System.out, plsa, 40);
	}

	public static void main(String[] args) {
		new PLSATWCDemo();
	}
}
