package org.hhn.topicgrouper.eval.classify.impl;

import gnu.trove.iterator.TIntIterator;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;

import java.util.List;

import org.hhn.topicgrouper.eval.classify.SupervisedDocumentClassifier;
import org.hhn.topicgrouper.doc.Document;
import org.hhn.topicgrouper.doc.LabeledDocument;
import org.hhn.topicgrouper.doc.LabelingDocumentProvider;

/**
 * Base class for classifiers that represent a document as a vector of topic frequencies
 * (a topic being a group of words, e.g. a TopicGrouper topic, an LDA topic or a single
 * selected word). Subclasses define which topics exist and which topic a word belongs to.
 * Provides the shared evaluation in {@link #test(LabelingDocumentProvider, double[])}.
 *
 * @param <T> the word type of the documents
 * @param <L> the label type
 */
public abstract class AbstractTopicBasedClassifier<T, L> implements
		SupervisedDocumentClassifier<T, L> {
	/**
	 * Maps a topic id (as returned by {@link #getTopicIndices()}) back to its position in {@link #topicIndices}.
	 */
	protected final TIntIntMap topicIndicesBack;
	/**
	 * The topic ids of the current model; the position in this array is the topic's vector dimension.
	 */
	protected int[] topicIndices;

	/**
	 * Creates a classifier with no topics; {@link #updateTopicIndices()} must be called when training.
	 */
	public AbstractTopicBasedClassifier() {
		topicIndicesBack = new TIntIntHashMap();
	}

	/**
	 * Re-reads the topic ids from {@link #getTopicIndices()} and rebuilds the reverse index.
	 */
	protected void updateTopicIndices() {
		topicIndices = getTopicIndices();
		topicIndicesBack.clear();
		for (int i = 0; i < topicIndices.length; i++) {
			this.topicIndicesBack.put(topicIndices[i], i);
		}
	}

	/**
	 * The number of topics of the current model.
	 *
	 * @return the number of topics, i.e. the dimension of the topic frequency vectors
	 */
	protected int getNTopics() {
		return topicIndices.length;
	}

	/**
	 * Adds the topic frequencies of a document to a vector: the frequency of each word is added
	 * to the dimension of the topic the word belongs to. Words without topic are ignored.
	 *
	 * @param d the document
	 * @param v vector of length {@link #getNTopics()} that is incremented (not cleared)
	 */
	protected void computeTopicFrequency(Document<T> d, double[] v) {
		TIntIterator it = d.getWordIndices().iterator();
		while (it.hasNext()) {
			int wordIndex = it.next();
			int fr = d.getWordFrequency(wordIndex);
			int ti = getTopicIndex(wordIndex);
			if (ti != Integer.MIN_VALUE) {
				v[this.topicIndicesBack.get(ti)] += fr;
			}
		}
	}

	/**
	 * Computes the topic frequency vector of a (test) document.
	 *
	 * @param d the document
	 * @return a new vector of length {@link #getNTopics()}
	 */
	protected double[] computeTopicFrequencyTest(Document<T> d) {
		double[] ftd = new double[getNTopics()];
		computeTopicFrequency(d, ftd);
		return ftd;
	}

	/**
	 * Counts the documents that contain at least one word of the given topic.
	 *
	 * @param ds the documents
	 * @param topicIndex the topic id (not the vector position)
	 * @return the number of documents containing the topic
	 */
	protected int computeDocumentFrequency(List<Document<T>> ds, int topicIndex) {
		int df = 0;
		for (Document<T> d : ds) {
			TIntIterator it = d.getWordIndices().iterator();
			while (it.hasNext()) {
				int wordIndex = it.next();
				int fr = d.getWordFrequency(wordIndex);
				int ti = getTopicIndex(wordIndex);
				if (ti != Integer.MIN_VALUE) {
					if (fr > 0 && ti == topicIndex) {
						df++;
						break;
					}
				}
			}
		}
		return df;
	}

	/**
	 * Classifies all documents of the test provider and compares with their true labels.
	 *
	 * @param testProvider provider of the labeled test documents
	 * @param res array of length at least 2 to store the results in, or <code>null</code>
	 * @return the array with <code>res[0]</code> = micro-averaged accuracy (over documents) and
	 * <code>res[1]</code> = macro-averaged accuracy (average of the per-label accuracies, labels without documents are skipped)
	 */
	@Override
	public double[] test(LabelingDocumentProvider<T, L> testProvider,
			double[] res) {
		int hits = 0;
		int tests = 0;
		int usedLabels = 0;
		double macroAvg = 0;
		if (res == null) {
			res = new double[2];
		}

		for (L label : testProvider.getAllLabels()) {
			int labelHits = 0;
			int labelTests = 0;
			for (LabeledDocument<T, L> dt : testProvider
					.getDocumentsWithLabel(label)) {
				L clabel = classify(dt);
				if (clabel != null && clabel.equals(label)) {
					labelHits++;
					hits++;
				}
				labelTests++;
				tests++;
			}
			usedLabels += labelTests == 0 ? 0 : 1;
			macroAvg += labelTests == 0 ? 0 : ((double) labelHits) / labelTests;
		}

		res[0] = ((double) hits) / tests;
		res[1] = macroAvg / usedLabels;
		return res;
	}

	/**
	 * Supplies the topics of the underlying model (e.g. the TopicGrouper topics of a chosen
	 * solution, the LDA topics or the selected vocabulary words).
	 *
	 * @return the ids of all topics of the current model
	 */
	protected abstract int[] getTopicIndices();

	/**
	 * Maps a word to its topic.
	 *
	 * @param wordIndex the word index in the vocabulary
	 * @return the topic id of the word or <code>Integer.MIN_VALUE</code> if the word has no topic
	 */
	protected abstract int getTopicIndex(int wordIndex);
}
