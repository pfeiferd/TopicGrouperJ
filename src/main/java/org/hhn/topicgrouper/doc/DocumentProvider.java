package org.hhn.topicgrouper.doc;

import java.io.Serializable;
import java.util.List;

/**
 * A collection of documents sharing one vocabulary.
 *
 * @param <T> the type of the words
 */
public interface DocumentProvider<T> extends Serializable {
	/**
	 * The total number of word occurrences over all documents.
	 */
	public int getSize();
	/**
	 * The total number of occurrences of the word with the given index over all documents.
	 */
	public int getWordFrequency(int index);
	/**
	 * All documents of this provider.
	 */
	public List<Document<T>> getDocuments();
	/**
	 * The vocabulary mapping words to indices and back.
	 */
	public Vocab<T> getVocab();
	
	/**
	 * A bidirectional mapping between words and consecutive integer indices.
	 *
	 * @param <T> the type of the words
	 */
	public interface Vocab<T> extends Serializable {
		/**
		 * The word for the given index.
		 */
		public T getWord(int index);
		/**
		 * The index of the given word or -1 if the word is unknown.
		 */
		public int getIndex(T word);		
		/**
		 * The number of distinct words in the vocabulary.
		 */
		public int getNumberOfWords();
	}
}
