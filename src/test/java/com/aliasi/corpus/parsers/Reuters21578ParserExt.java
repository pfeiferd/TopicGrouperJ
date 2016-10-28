package com.aliasi.corpus.parsers;

import java.io.File;
import java.io.IOException;

import com.aliasi.classify.Classification;
import com.aliasi.corpus.Corpus;
import com.aliasi.io.FileExtensionFilter;

public class Reuters21578ParserExt extends Reuters21578Parser {
    private final boolean mIncludeTestDocuments;
    private final boolean mIncludeTrainingDocuments;
    
	public Reuters21578ParserExt(boolean includeTrainingDocuments, boolean includeTestDocuments) {
		super(TOPICS[0], includeTrainingDocuments, includeTestDocuments);
        mIncludeTrainingDocuments = includeTrainingDocuments;
        mIncludeTestDocuments = includeTestDocuments;
	}

	@Override
	void handleDocument(String text) {
        if (!hasTopics(text)) return;
        if (isTrainingDocument(text) && !mIncludeTrainingDocuments) return;
        if (isTestDocument(text) && !mIncludeTestDocuments) return;
        String topics = extract("TOPICS",text,true);
        String title = extract("TITLE",text,true);
        String dateline = extract("DATELINE",text,true);
        String body = extract("BODY",text,true);
        if (body.endsWith(END_BOILERPLATE_1) || body.endsWith(END_BOILERPLATE_2))
            body = body.substring(0,body.length() - END_BOILERPLATE_1.length());
        StringBuilder sb = new StringBuilder();
        sb.append(title + "\n");
        sb.append(dateline + "\n");
        sb.append(body);
        for (String topic : TOPICS) {
        	int index = topics.indexOf(topic);
        	// Accepts only one label per document:
        	if (index >= 1 && '>' == topics.charAt(index - 1) && '<' == topics.charAt(index + topic.length())) {
        		Classification classification = new Classification(topic);
        		getHandler().handle(sb,classification);        		
        	}        	
        }
	}

	@SuppressWarnings("deprecation")
	@Deprecated
	public static Corpus<com.aliasi.corpus.ClassificationHandler<CharSequence, Classification>> corpus(File directory) throws IOException {
		return new ReutersCorpusExt(directory);
	}

	private static class ReutersCorpusExt
			extends
			Corpus<com.aliasi.corpus.ClassificationHandler<CharSequence, Classification>> {
		private final File mDirectory;

		ReutersCorpusExt(File directory) {
			mDirectory = directory;
		}

		@Override
		public void visitCorpus(
				com.aliasi.corpus.ClassificationHandler<CharSequence, Classification> handler)
				throws IOException {

			visit(handler, true, true);
		}

		@Override
		public void visitTest(
				com.aliasi.corpus.ClassificationHandler<CharSequence, Classification> handler)
				throws IOException {

			visit(handler, false, true);
		}

		@Override
		public void visitTrain(
				com.aliasi.corpus.ClassificationHandler<CharSequence, Classification> handler)
				throws IOException {

			visit(handler, true, false);
		}

		void visit(
				com.aliasi.corpus.ClassificationHandler<CharSequence, Classification> handler,
				boolean includeTrain, boolean includeTest) throws IOException {

			Reuters21578Parser parser = new Reuters21578ParserExt(includeTrain, includeTest);
			parser.setHandler(handler);
			for (File file : mDirectory.listFiles(new FileExtensionFilter(
					".sgm")))
				parser.parse(file);
		}
	}
}
