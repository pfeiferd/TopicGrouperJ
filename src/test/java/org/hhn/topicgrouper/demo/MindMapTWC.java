package org.hhn.topicgrouper.demo;

import java.io.File;
import java.io.IOException;
import java.util.Random;

import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.eval.TWCLDAPaperDocumentGenerator;

public class MindMapTWC extends MindMapDemoReuters21578 {
	public MindMapTWC(File file) throws IOException {
		super(file);
	}

	@Override
	protected DocumentProvider<String> createDocumentProvider() {
		return new TWCLDAPaperDocumentGenerator(new Random(42));
//		return new TWCLDAPaperDocumentGenerator(new Random(42), new double[] { 5, 0.5,
//				0.5, 0.5 }, 6000, 10, 10, 30, 30, 0, null, 0.5, 0.8);
	}

	@Override
	protected File createMindMapFile() {
		return new File("./target/TWC.mm");
	}

	@Override
	protected File createSerializationFile() {
		return new File("./target/TWC.ser");
	}

	public static void main(String[] args) throws IOException {
		new MindMapTWC(null).run();
	}
}
