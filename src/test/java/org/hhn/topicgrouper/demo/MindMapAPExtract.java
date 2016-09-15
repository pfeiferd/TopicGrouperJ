package org.hhn.topicgrouper.demo;

import java.io.File;
import java.io.IOException;

import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.eval.APParser;

public class MindMapAPExtract extends MindMapDemoReuters21578 {
	public MindMapAPExtract(File file) throws IOException {
		super(file);
	}

	@Override
	protected DocumentProvider<String> createDocumentProvider() {
		return new APParser(true).getCorpusDocumentProvider(new File(
				"src/test/resources/ap-corpus/extract/ap.txt"));
	}

	@Override
	protected File createMindMapFile() {
		return new File("./target/APExtract.mm");
	}

	@Override
	protected File createSerializationFile() {
		return new File("./target/APExtract.ser");
	}

	public static void main(String[] args) throws IOException {
		new MindMapAPExtract(null).run();
	}
}
