package org.hhn.topicgrouper.bin;

import java.io.IOException;
import java.util.Random;

import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.eval.TWCLDAPaperDocumentGenerator;

public class TWCAlphaOptimizationTest  {
	protected final Random random;
	protected final SimpleEMAlphaBetaOptimizer<String> alphaOptimizer;
	
	public TWCAlphaOptimizationTest(Random random) {
		this.random = random;
		alphaOptimizer = new SimpleEMAlphaBetaOptimizer<String>(random);
	}
	
	public void run() {
		alphaOptimizer.run(createDocumentProvider(), 10, 1, 4, 0.0001, 100);
	}
	

	protected DocumentProvider<String> createDocumentProvider() {
		return new TWCLDAPaperDocumentGenerator(random, new double[] { 5, 0.5,
				0.5, 0.5 }, 9000, 100, 100, 30, 30, 0, null, 0.5, 0.8);
	}


	public static void main(String[] args) throws IOException {
		new TWCAlphaOptimizationTest(new Random()).run();
	}
}
