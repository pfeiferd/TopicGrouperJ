package org.hhn.topicgrouper.demo;

import java.io.File;
import java.io.IOException;

import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.eval.APLargeParser;

public class MindMapAPFull extends MindMapDemoReuters21578 {
	public MindMapAPFull(File file) throws IOException {
		super(file);
	}

	@Override
	protected DocumentProvider<String> createDocumentProvider() {
		return new APLargeParser(System.out, true).getCorpusDocumentProvider(new File(
				"src/test/resources/ap-corpus/full"), 16000);
	}

	@Override
	protected File createMindMapFile() {
		return new File("./target/APFull.mm");
	}

	@Override
	protected File createSerializationFile() {
		return new File("./target/APFull.ser");
	}

	public static void main(String[] args) throws IOException {
		new MindMapAPFull(null).run();
	}
}
