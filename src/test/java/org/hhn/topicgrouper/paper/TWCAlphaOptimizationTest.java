package org.hhn.topicgrouper.paper;

import java.io.IOException;
import java.util.Random;

import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.eval.TWCLDAPaperDocumentGenerator;
import org.hhn.topicgrouper.lda.impl.SimpleEMAlphaOptimizer;

public class TWCAlphaOptimizationTest  {
	protected final Random random;
	protected final SimpleEMAlphaOptimizer<String> alphaOptimizer;
	
	public TWCAlphaOptimizationTest(Random random) {
		this.random = random;
		alphaOptimizer = new SimpleEMAlphaOptimizer<String>(random);
	}
	
	public void run() {
		alphaOptimizer.run(createDocumentProvider(), 1, 4, 100, 0.001, 100);
	}
	

	protected DocumentProvider<String> createDocumentProvider() {
		return new TWCLDAPaperDocumentGenerator(random, new double[] { 5, 0.5,
				0.5, 0.5 }, 9000, 100, 100, 30, 30, 0, null, 0.5, 0.8);
	}


	public static void main(String[] args) throws IOException {
		new TWCAlphaOptimizationTest(new Random()).run();
	}
}
