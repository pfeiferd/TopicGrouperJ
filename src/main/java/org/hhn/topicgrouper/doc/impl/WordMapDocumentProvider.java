package org.hhn.topicgrouper.doc.impl;

import gnu.trove.list.TIntList;

import java.util.Collections;
import java.util.List;

import org.hhn.topicgrouper.doc.Document;
import org.hhn.topicgrouper.doc.DocumentProvider;

public class WordMapDocumentProvider<T> implements DocumentProvider<T> {

	private static final long serialVersionUID = -7108768570880922450L;

	private List<Document<T>> immutableList;
	protected List<Document<T>> entries;
	protected TIntList indexToFr;
	protected DefaultVocab<T> vocab;
	protected int size;

	protected WordMapDocumentProvider() {
		//no-args constructor for serialization
	}

	public WordMapDocumentProvider(DefaultVocab<T> vocab, List<Document<T>> entries,
			TIntList indexToFr) {
		this.vocab = vocab == null ? new DefaultVocab<T>() : vocab;
		this.entries = entries;
		this.immutableList = Collections.unmodifiableList(entries);
		this.indexToFr = indexToFr;
		this.size = 0;
	}
	
	
	@Override
	public List<Document<T>> getDocuments() {
		return immutableList;
	}

	@Override
	public int getWordFrequency(int index) {
		return indexToFr.get(index);
	}
	
	@Override
	public int getSize() {
		return size;
	}
	
	@Override
	public Vocab<T> getVocab() {
		return vocab;
	}
}
