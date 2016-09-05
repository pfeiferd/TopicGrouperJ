package org.hhn.topicgrouper.validation;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.hhn.topicgrouper.doc.Document;
import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.doc.impl.DefaultDocumentProvider;

public class HoldOutSplitter<T> {
	private DefaultDocumentProvider<T> holdOut;
	private DefaultDocumentProvider<T> rest;

	public HoldOutSplitter(Random random, DocumentProvider<T> origDocuments,
			double holdOutRatio, int minGlobalWordFrequency) {
		List<Document<T>> documents = new ArrayList<Document<T>>(
				origDocuments.getDocuments());
		List<Document<T>> holdOutDocuments = new ArrayList<Document<T>>();
		int max = (int) (documents.size() * Math.min(1, holdOutRatio));
		for (int i = 0; i < max; i++) {
			holdOutDocuments.add(documents.remove(random.nextInt(documents
					.size())));
		}
		rest = new DefaultDocumentProvider<T>();
		for (Document<T> d : documents) {
			rest.addDocument(d, minGlobalWordFrequency);
		}
		holdOut = new DefaultDocumentProvider<T>();
		for (Document<T> d : holdOutDocuments) {
			holdOut.addDocument(d, minGlobalWordFrequency);
		}
	}

	public DefaultDocumentProvider<T> getHoldOut() {
		return holdOut;
	}

	public DefaultDocumentProvider<T> getRest() {
		return rest;
	}
}
