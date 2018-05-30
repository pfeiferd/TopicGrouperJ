package org.hhn.topicgrouper.paper.performance;

import java.util.Random;

import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.tg.TGSolver;
import org.hhn.topicgrouper.tg.impl.LowMemTopicGrouper;

public class TwentyNGLowMemTGPChangeDocsExp extends TwentyNGTGPChangeDocsExp {
	public TwentyNGLowMemTGPChangeDocsExp() {
	}

	@Override
	protected TGSolver<String> createSolver(
			DocumentProvider<String> documentProvider) {
		return new LowMemTopicGrouper<String>(1, documentProvider, 1);
	}

	public static void main(String[] args) {
		new TwentyNGLowMemTGPChangeDocsExp().runExperiment(new Random(43), 10,
				15);
	}
}
