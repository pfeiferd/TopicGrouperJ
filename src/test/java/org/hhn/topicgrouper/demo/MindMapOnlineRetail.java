package org.hhn.topicgrouper.demo;

import java.io.File;
import java.io.IOException;
import java.util.Random;

import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.doc.impl.HoldOutSplitter;
import org.hhn.topicgrouper.eval.OnlineRetailParser;

public class MindMapOnlineRetail extends MindMapDemoReuters21578 {
	public MindMapOnlineRetail(File file) throws IOException {
		super(file);
	}

	@Override
	protected DocumentProvider<String> createDocumentProvider() {
		OnlineRetailParser parser = new OnlineRetailParser();
		DocumentProvider<String> provider = parser.getCorpusDocumentProvider(
				new File("src/test/resources/OnlineRetail/Online Retail.csv"),
				true, 1000, false);
		HoldOutSplitter<String> holdOutSplitter = new HoldOutSplitter<String>(
				new Random(), provider, 0, 10);
		provider = holdOutSplitter.getRest();
		System.out.println(provider.getVocab().getNumberOfWords());

		return provider;
	}

	@Override
	protected File createMindMapFile() {
		return new File("./target/OR.mm");
	}

	@Override
	protected File createSerializationFile() {
		return new File("./target/OR.ser");
	}

	public static void main(String[] args) throws IOException {
		new MindMapOnlineRetail(null).run();
	}
}
