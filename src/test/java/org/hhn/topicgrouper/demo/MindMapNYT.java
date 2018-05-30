package org.hhn.topicgrouper.demo;

import java.io.File;
import java.io.IOException;
import java.util.Random;

import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.doc.impl.HoldOutSplitter;
import org.hhn.topicgrouper.eval.UCIParser;

public class MindMapNYT extends MindMapDemoReuters21578 {
	public MindMapNYT(File file) throws IOException {
		super(file);
	}

	@Override
	protected DocumentProvider<String> createDocumentProvider() {
		try {
			DocumentProvider<String> provider = new UCIParser(true, new File("./src/test/resources/nyt"),
					"nytimes", 10000).getDocumentProvider();
			HoldOutSplitter<String> holdOutSplitter = new HoldOutSplitter<String>(new Random(), provider, 0, 100);
			provider = holdOutSplitter.getRest();
			System.out.println(provider.getVocab().getNumberOfWords());
			
			return provider;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		//
		// return new APParser(true, true).getCorpusDocumentProvider(new File(
		// "src/test/resources/ap-corpus/extract/ap.txt"));
	}

	@Override
	protected File createMindMapFile() {
		return new File("./target/nyt.mm");
	}

	@Override
	protected File createSerializationFile() {
		return new File("./target/nyt.ser");
	}

	public static void main(String[] args) throws IOException {
		new MindMapNYT(null).run();
	}
}
