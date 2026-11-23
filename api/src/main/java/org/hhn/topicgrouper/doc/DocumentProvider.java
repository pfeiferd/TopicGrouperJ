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
	 *
	 * @return the sum of the sizes of all documents
	 */
	public int getSize();
	/**
	 * The total number of occurrences of the word with the given index over all documents.
	 *
	 * @param index the word index as defined by {@link #getVocab()}
	 * @return the global frequency of the word
	 */
	public int getWordFrequency(int index);
	/**
	 * All documents of this provider.
	 *
	 * @return the documents; the position of a document equals its {@link Document#getIndex()}
	 */
	public List<Document<T>> getDocuments();
	/**
	 * The vocabulary mapping words to indices and back.
	 *
	 * @return the vocabulary
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
		 *
		 * @param index the word index, 0 to {@link #getNumberOfWords()} - 1
		 * @return the word
		 */
		public T getWord(int index);
		/**
		 * The index of the given word or -1 if the word is unknown.
		 *
		 * @param word the word
		 * @return the index of the word or -1
		 */
		public int getIndex(T word);		
		/**
		 * The number of distinct words in the vocabulary.
		 *
		 * @return the number of words
		 */
		public int getNumberOfWords();
	}
}
