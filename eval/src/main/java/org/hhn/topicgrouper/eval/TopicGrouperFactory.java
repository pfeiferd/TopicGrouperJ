package org.hhn.topicgrouper.eval;

import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.tg.TGSolver;
import org.hhn.topicgrouper.tg.impl.EHACTopicGrouper;
import org.hhn.topicgrouper.tg.impl.LowMemTopicGrouper;
import org.hhn.topicgrouper.tg.impl.SenseSplittingTopicGrouper;

/**
 * Chooses the Topic Grouper solver for a corpus: the fast heap based one up to
 * a vocabulary size limit (its memory is quadratic in the vocabulary), the
 * memory efficient one beyond.
 */
public class TopicGrouperFactory {
	/** Default vocabulary size up to which the heap based solver is used. */
	public static final int DEFAULT_MAX_WORDS_FOR_EHAC = 2500;

	private final int maxWordsForEhac;
	private final int threads;

	/** Uses {@link #DEFAULT_MAX_WORDS_FOR_EHAC} and one thread per available processor. */
	public TopicGrouperFactory() {
		this(DEFAULT_MAX_WORDS_FOR_EHAC);
	}

	/**
	 * Uses the given limit and one thread per available processor.
	 *
	 * @param maxWordsForEhac
	 *            vocabulary size up to which the heap based solver is used
	 */
	public TopicGrouperFactory(int maxWordsForEhac) {
		this(maxWordsForEhac, Runtime.getRuntime().availableProcessors());
	}

	/**
	 * Uses the given limit and number of threads.
	 *
	 * @param maxWordsForEhac
	 *            vocabulary size up to which the heap based solver is used
	 * @param threads
	 *            the number of threads the solvers use
	 */
	public TopicGrouperFactory(int maxWordsForEhac, int threads) {
		this.maxWordsForEhac = maxWordsForEhac;
		this.threads = threads;
	}

	/**
	 * The number of threads the created solvers use.
	 *
	 * @return the number of threads
	 */
	public int getThreads() {
		return threads;
	}

	/**
	 * Creates the solver for a corpus (the choice depends on the size of the
	 * corpus vocabulary, not on the minimum word frequency).
	 *
	 * @param train
	 *            the training documents
	 * @param minWordFrequency
	 *            only words occurring at least this often are grouped
	 * @return a solver that runs down to one topic
	 */
	public TGSolver<String> create(DocumentProvider<String> train, int minWordFrequency) {
		if (train.getVocab().getNumberOfWords() <= maxWordsForEhac) {
			return new EHACTopicGrouper<String>(minWordFrequency, train, 1, threads);
		}
		return new LowMemTopicGrouper<String>(minWordFrequency, train, 1, threads);
	}

	/**
	 * Creates the sense splitting solver (homonym handling, see
	 * <code>doc/topic-grouper.md</code>). It is heap based like the fast
	 * solver, so its memory is quadratic in the number of grouped words; choose
	 * the minimum word frequency accordingly.
	 *
	 * @param train
	 *            the training documents
	 * @param minWordFrequency
	 *            only words occurring at least this often are grouped
	 * @return a solver that runs down to one topic with the default minimum lift
	 */
	public TGSolver<String> createSenseSplitting(DocumentProvider<String> train, int minWordFrequency) {
		return new SenseSplittingTopicGrouper<String>(minWordFrequency, train, 1,
				SenseSplittingTopicGrouper.DEFAULT_MIN_SENSE_LIFT, threads);
	}

	/**
	 * Creates the solver for a corpus with minimum word frequency 1.
	 *
	 * @param train
	 *            the training documents
	 * @return a solver over all words of the corpus
	 */
	public TGSolver<String> create(DocumentProvider<String> train) {
		return create(train, 1);
	}
}
