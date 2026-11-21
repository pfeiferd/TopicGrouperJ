package org.hhn.topicgrouper.evaluation;

import java.io.File;
import java.io.IOException;
import java.util.Random;

import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.doc.LabelingDocumentProvider;
import org.hhn.topicgrouper.doc.impl.DefaultVocab;
import org.hhn.topicgrouper.doc.impl.HoldOutSplitter;
import org.hhn.topicgrouper.doc.impl.LabelingHoldOutSplitter;
import org.hhn.topicgrouper.evaluation.data.Reuters21578;
import org.hhn.topicgrouper.evaluation.data.TWCLDAPaperDocumentGenerator;
import org.hhn.topicgrouper.evaluation.data.TwentyNGParser;
import org.hhn.topicgrouper.evaluation.data.UCIParser;

/**
 * Loads the (publicly available) corpora of the paper and creates the same
 * training / test splits as the original experiments.
 * <p>
 * Expected layout of the data directory (created by
 * <code>scripts/download-data.sh</code>):
 * 
 * <pre>
 * data/nips/vocab.nips.txt, data/nips/docword.nips.txt     (UCI bag of words)
 * data/reuters21578/*.sgm                                  (Reuters-21578 Distribution 1.0)
 * data/20news-18828/&lt;group&gt;/&lt;message&gt;                  (20 Newsgroups, duplicates removed)
 * </pre>
 */
public final class Datasets {
	private Datasets() {
	}

	/** The synthetic "Topic Word Corpus" and the model that generated it. */
	public static final class Twc {
		/** The generator, knows the true word-topic distributions. */
		public final TWCLDAPaperDocumentGenerator generator;
		/** The 75% / 25% split of the generated documents. */
		public final Split<DocumentProvider<String>> split;

		Twc(TWCLDAPaperDocumentGenerator generator, Split<DocumentProvider<String>> split) {
			this.generator = generator;
			this.split = split;
		}
	}

	/**
	 * Generates the synthetic corpus of the paper: 4 topics with 100 words each,
	 * documents of 30 words, topic proportions drawn from a Dirichlet.
	 * 
	 * @param docs
	 *            number of documents
	 * @param alpha1
	 *            Dirichlet parameter of the first topic (the paper's default is 5, the others are 0.5)
	 * @param seed
	 *            random seed
	 * @return corpus and split, words with a frequency below 3 in the training part are removed
	 */
	public static Twc twc(int docs, double alpha1, int seed) {
		TWCLDAPaperDocumentGenerator gen = new TWCLDAPaperDocumentGenerator(
				new Random(seed), new double[] { alpha1, 0.5, 0.5, 0.5 }, docs, 100,
				100, 30, 30, 0, null, 0.5, 0.8);
		HoldOutSplitter<String> splitter = new HoldOutSplitter<String>(
				new Random(42), gen, 0.25, 3);
		return new Twc(gen, new Split<DocumentProvider<String>>(splitter.getRest(), splitter.getHoldOut()));
	}

	/** @return the synthetic corpus with the parameters of the paper */
	public static Twc twc(int docs) {
		return twc(docs, 5, 42);
	}

	/**
	 * @param dataDir
	 *            data directory
	 * @return whether the NIPS corpus is available
	 */
	public static boolean hasNips(File dataDir) {
		return new File(dataDir, "nips/docword.nips.txt").isFile();
	}

	/**
	 * The NIPS papers (UCI bag of words); 10% hold out, words occurring less than
	 * 5 times in the training part are removed.
	 * 
	 * @param dataDir
	 *            data directory
	 * @return the split
	 */
	public static Split<DocumentProvider<String>> nips(File dataDir) {
		try {
			DocumentProvider<String> provider = new UCIParser(true,
					new File(dataDir, "nips"), "nips", -1).getDocumentProvider();
			HoldOutSplitter<String> splitter = new HoldOutSplitter<String>(
					new Random(42), provider, 0.1, 5);
			return new Split<DocumentProvider<String>>(splitter.getRest(), splitter.getHoldOut());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * @param dataDir
	 *            data directory
	 * @return whether Reuters-21578 is available
	 */
	public static boolean hasReuters(File dataDir) {
		return new File(dataDir, "reuters21578").isDirectory();
	}

	/**
	 * Reuters-21578 with the ModApte split, stop words removed, stemmed. Only the
	 * 10 most frequent categories (as in the paper) and words occurring at least 3
	 * times in the training part are used.
	 * 
	 * @param dataDir
	 *            data directory
	 * @return the split
	 */
	public static Split<LabelingDocumentProvider<String, String>> reuters(File dataDir) {
		File dir = new File(dataDir, "reuters21578");
		Reuters21578 reuters = new Reuters21578(true);
		LabelingDocumentProvider<String, String> trainingData = reuters
				.getCorpusDocumentProvider(dir, true, false);
		LabelingDocumentProvider<String, String> training = new LabelingHoldOutSplitter<String, String>(
				new Random(42), trainingData, 0, 3, 10).getRest();
		LabelingDocumentProvider<String, String> testData = reuters
				.getCorpusDocumentProvider(dir, false, true);
		LabelingDocumentProvider<String, String> test = new LabelingHoldOutSplitter<String, String>(
				new Random(42), testData, 1, 0, testData.getAllLabels(),
				(DefaultVocab<String>) training.getVocab()).getHoldOut();
		return new Split<LabelingDocumentProvider<String, String>>(training, test);
	}

	/**
	 * Reuters-21578 training documents without labels, used for the run time
	 * experiments.
	 * 
	 * @param dataDir
	 *            data directory
	 * @return the documents
	 */
	public static DocumentProvider<String> reutersTraining(File dataDir) {
		return new Reuters21578(true).getCorpusDocumentProvider(new File(dataDir, "reuters21578"), true, false);
	}

	/**
	 * @param dataDir
	 *            data directory
	 * @return whether 20 Newsgroups is available
	 */
	public static boolean hasTwentyNG(File dataDir) {
		return new File(dataDir, "20news-18828").isDirectory();
	}

	/**
	 * 20 Newsgroups (duplicates removed version), 25% hold out, words occurring
	 * less than 5 times in the training part are removed.
	 * 
	 * @param dataDir
	 *            data directory
	 * @return the split
	 */
	public static Split<LabelingDocumentProvider<String, String>> twentyNG(File dataDir) {
		LabelingDocumentProvider<String, String> provider = twentyNGAll(dataDir);
		LabelingHoldOutSplitter<String, String> splitter = new LabelingHoldOutSplitter<String, String>(
				new Random(42), provider, 0.25, 5, -1);
		return new Split<LabelingDocumentProvider<String, String>>(splitter.getRest(), splitter.getHoldOut());
	}

	/**
	 * @param dataDir
	 *            data directory
	 * @return all labeled documents of 20 Newsgroups
	 */
	public static LabelingDocumentProvider<String, String> twentyNGAll(File dataDir) {
		return new TwentyNGParser(null, true, true)
				.getCorpusDocumentProvider(new File(dataDir, "20news-18828"), 1);
	}
}
