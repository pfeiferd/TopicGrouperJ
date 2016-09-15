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
