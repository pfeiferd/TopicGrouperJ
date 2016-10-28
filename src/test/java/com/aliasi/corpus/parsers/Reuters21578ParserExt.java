package com.aliasi.corpus.parsers;

import java.io.File;
import java.io.IOException;

import com.aliasi.classify.Classification;
import com.aliasi.corpus.ClassificationHandler;
import com.aliasi.corpus.Corpus;
import com.aliasi.io.FileExtensionFilter;

public class Reuters21578ParserExt extends Reuters21578Parser {
	private final boolean includeTestDocuments;
	private final boolean includeTrainingDocuments;

	public Reuters21578ParserExt(boolean includeTrainingDocuments,
			boolean includeTestDocuments) {
		super(TOPICS[0], includeTrainingDocuments, includeTestDocuments);
		this.includeTrainingDocuments = includeTrainingDocuments;
		this.includeTestDocuments = includeTestDocuments;
	}

	@Override
	void handleDocument(String text) {
		if (!hasTopics(text)) {
			return;
		}
		if (isTrainingDocument(text) && !includeTrainingDocuments) {
			return;
		}
		if (isTestDocument(text) && !includeTestDocuments) {
			return;
		}
		
		String topics = extract("TOPICS", text, true);
		String title = extract("TITLE", text, true);
		String dateline = extract("DATELINE", text, true);
		String body = extract("BODY", text, true);
		if (body.endsWith(END_BOILERPLATE_1)
				|| body.endsWith(END_BOILERPLATE_2)) {
			body = body
					.substring(0, body.length() - END_BOILERPLATE_1.length());
		}
		
		StringBuilder sb = new StringBuilder();
		sb.append(title);
		sb.append("\n");
		sb.append(dateline);
		sb.append("\n");
		sb.append(body);
		
		for (String topic : TOPICS) {
			int index = topics.indexOf(topic);
			// Accepts only one label per document:
			if (index >= 1 && '>' == topics.charAt(index - 1)
					&& '<' == topics.charAt(index + topic.length())) {
				Classification classification = new Classification(topic);
				getHandler().handle(sb, classification);
			}
		}
	}

	@Deprecated
	public static Corpus<ClassificationHandler<CharSequence, Classification>> corpus(
			File directory) throws IOException {
		return new ReutersCorpusExt(directory);
	}

	private static class ReutersCorpusExt
			extends
			Corpus<ClassificationHandler<CharSequence, Classification>> {
		private final File mDirectory;

		ReutersCorpusExt(File directory) {
			mDirectory = directory;
		}

		@Override
		public void visitCorpus(
				ClassificationHandler<CharSequence, Classification> handler)
				throws IOException {

			visit(handler, true, true);
		}

		@Override
		public void visitTest(
				ClassificationHandler<CharSequence, Classification> handler)
				throws IOException {

			visit(handler, false, true);
		}

		@Override
		public void visitTrain(
				ClassificationHandler<CharSequence, Classification> handler)
				throws IOException {

			visit(handler, true, false);
		}

		void visit(
				ClassificationHandler<CharSequence, Classification> handler,
				boolean includeTrain, boolean includeTest) throws IOException {

			Reuters21578Parser parser = new Reuters21578ParserExt(includeTrain,
					includeTest);
			parser.setHandler(handler);
			for (File file : mDirectory.listFiles(new FileExtensionFilter(
					".sgm")))
				parser.parse(file);
		}
	}
}
