package org.hhn.topicgrouper.demo;

import java.io.File;
import java.io.IOException;

import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.tg.TGSolver;
import org.hhn.topicgrouper.tg.impl.LowMemTopicGrouper;

public class MindMapTWC2 extends MindMapTWC {
	public MindMapTWC2(File file) throws IOException {
		super(file);
	}
	
	@Override
	protected TGSolver<String> createSolver(
			DocumentProvider<String> documentProvider) {
		return new LowMemTopicGrouper<String>(1, documentProvider, 1);
	}

	public static void main(String[] args) throws IOException {
		new MindMapTWC2(null).run();
	}
}
