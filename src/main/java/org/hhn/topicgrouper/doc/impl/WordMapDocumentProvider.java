package org.hhn.topicgrouper.doc.impl;

import gnu.trove.list.TIntList;

import java.util.Collections;
import java.util.List;

import org.hhn.topicgrouper.doc.Document;
import org.hhn.topicgrouper.doc.DocumentProvider;

/**
 * Document provider storing its documents in a list and the global word frequencies in an index-based list.
 *
 * @param <T> the word type
 */
public class WordMapDocumentProvider<T> implements DocumentProvider<T> {

	private static final long serialVersionUID = -7108768570880922450L;

	/**
	 * Unmodifiable view of {@link #entries}.
	 */
	private List<Document<T>> immutableList;
	/**
	 * The documents.
	 */
	protected List<Document<T>> entries;
	/**
	 * Global frequency per word index.
	 */
	protected TIntList indexToFr;
	/**
	 * The vocabulary.
	 */
	protected DefaultVocab<T> vocab;
	/**
	 * Total number of word occurrences in all documents.
	 */
	protected int size;

	/**
	 * Constructor for serialization only.
	 */
	protected WordMapDocumentProvider() {
		//no-args constructor for serialization
	}

	/**
	 * Creates the provider.
	 *
	 * @param vocab the vocabulary, <code>null</code> for a new empty one
	 * @param entries the (initially usually empty) backing list of documents
	 * @param indexToFr the backing list of global word frequencies
	 */
	public WordMapDocumentProvider(DefaultVocab<T> vocab, List<Document<T>> entries,
			TIntList indexToFr) {
		this.vocab = vocab == null ? new DefaultVocab<T>() : vocab;
		this.entries = entries;
		this.immutableList = Collections.unmodifiableList(entries);
		this.indexToFr = indexToFr;
		this.size = 0;
	}
	
	
	/**
	 * @return an unmodifiable view of the documents
	 */
	@Override
	public List<Document<T>> getDocuments() {
		return immutableList;
	}

	/**
	 * @param index the word index
	 * @return the global frequency of the word
	 */
	@Override
	public int getWordFrequency(int index) {
		return indexToFr.get(index);
	}
	
	/**
	 * @return the total number of word occurrences in all documents
	 */
	@Override
	public int getSize() {
		return size;
	}
	
	/**
	 * @return the vocabulary
	 */
	@Override
	public Vocab<T> getVocab() {
		return vocab;
	}
}
