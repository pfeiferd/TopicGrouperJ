package org.hhn.topicgrouper.eval;

import java.io.File;
import java.io.IOException;
import java.util.Random;

import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.doc.LabelingDocumentProvider;
import org.hhn.topicgrouper.doc.impl.DefaultVocab;
import org.hhn.topicgrouper.doc.impl.HoldOutSplitter;
import org.hhn.topicgrouper.doc.impl.LabelingHoldOutSplitter;
import org.hhn.topicgrouper.data.Reuters21578;
import org.hhn.topicgrouper.data.APLargeParser;
import org.hhn.topicgrouper.data.APParser;
import org.hhn.topicgrouper.data.OHSUMEDParser;
import org.hhn.topicgrouper.data.OnlineRetailParser;
import org.hhn.topicgrouper.data.TaFengGroceryParser;
import org.hhn.topicgrouper.data.USSupermarketParser;

import org.hhn.topicgrouper.data.TWCLDAPaperDocumentGenerator;
import org.hhn.topicgrouper.data.TwentyNGParser;
import org.hhn.topicgrouper.data.UCIParser;

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
	private final File dataDir;
	private final int maxDocuments;

	/**
	 * Creates the data sets of a data directory.
	 *
	 * @param dataDir
	 *            the data directory (see the README for its layout)
	 */
	public Datasets(File dataDir) {
		this(dataDir, 0);
	}

	/**
	 * Data sets reduced to a random subset of their documents (for quick runs).
	 *
	 * @param dataDir
	 *            the data directory (see the README for its layout)
	 * @param maxDocuments
	 *            real corpora are reduced to this many documents (0: all)
	 */
	public Datasets(File dataDir, int maxDocuments) {
		this.dataDir = dataDir;
		this.maxDocuments = maxDocuments;
	}

	/**
	 * The document cap of quick runs.
	 *
	 * @return the number of documents real corpora are reduced to, 0 for all
	 */
	public int getMaxDocuments() {
		return maxDocuments;
	}

	/**
	 * Reduces a corpus to {@link #getMaxDocuments()} random documents.
	 *
	 * @param provider
	 *            the corpus
	 * @return the corpus or a random subset of it
	 */
	private DocumentProvider<String> limit(DocumentProvider<String> provider) {
		int size = provider.getDocuments().size();
		if (maxDocuments <= 0 || size <= maxDocuments) {
			return provider;
		}
		return new HoldOutSplitter<String>(new Random(7), provider, 1 - maxDocuments / (double) size, 0).getRest();
	}

	/**
	 * Reduces a labeled corpus to {@link #getMaxDocuments()} random documents.
	 *
	 * @param provider
	 *            the corpus
	 * @return the corpus or a random subset of it
	 */
	private LabelingDocumentProvider<String, String> limit(LabelingDocumentProvider<String, String> provider) {
		int size = provider.getDocuments().size();
		if (maxDocuments <= 0 || size <= maxDocuments) {
			return provider;
		}
		return new LabelingHoldOutSplitter<String, String>(new Random(7), provider,
				1 - maxDocuments / (double) size, 0, -1).getRest();
	}

	/**
	 * Returns the data directory.
	 *
	 * @return the data directory
	 */
	public File getDataDir() {
		return dataDir;
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
	public Twc twc(int docs, double alpha1, int seed) {
		TWCLDAPaperDocumentGenerator gen = new TWCLDAPaperDocumentGenerator(
				new Random(seed), new double[] { alpha1, 0.5, 0.5, 0.5 }, docs, 100,
				100, 30, 30, 0, null, 0.5, 0.8);
		HoldOutSplitter<String> splitter = new HoldOutSplitter<String>(
				new Random(42), gen, 0.25, 3);
		return new Twc(gen, new Split<DocumentProvider<String>>(splitter.getRest(), splitter.getHoldOut()));
	}

	/**
	 * The synthetic corpus with homonyms: topics are balanced and documents are almost single-topic (Dirichlet 0.1 for
	 * all four) and every homonym is a word that is additionally emitted (with probability
	 * 80% to 100%) whenever one particular word of each of two different topics is
	 * emitted. So a homonym has two senses with equally many documents.
	 * 
	 * @param docs
	 *            number of documents
	 * @param homonyms
	 *            number of homonyms
	 * @param seed
	 *            random seed
	 * @return corpus and split
	 */
	public Twc twcWithHomonyms(int docs, int homonyms, int seed) {
		TWCLDAPaperDocumentGenerator gen = new TWCLDAPaperDocumentGenerator(
				new Random(seed), new double[] { 0.1, 0.1, 0.1, 0.1 }, docs, 100,
				100, 30, 30, homonyms, new double[] { 1, 1, 1 }, 0, 0.2);
		HoldOutSplitter<String> splitter = new HoldOutSplitter<String>(
				new Random(42), gen, 0.25, 3);
		return new Twc(gen, new Split<DocumentProvider<String>>(splitter.getRest(), splitter.getHoldOut()));
	}

	/**
	 * Generates the synthetic corpus with the parameters of the paper (Dirichlet
	 * parameter 5 for the first topic, seed 42).
	 *
	 * @param docs
	 *            number of documents
	 * @return the synthetic corpus with the parameters of the paper
	 */
	public Twc twc(int docs) {
		return twc(docs, 5, 42);
	}

	/**
	 * Checks for <code>nips/docword.nips.txt</code> in the data directory.
	 *
	 * @return whether the NIPS corpus is available
	 */
	public boolean hasNips() {
		return new File(dataDir, "nips/docword.nips.txt").isFile();
	}

	/**
	 * The NIPS papers (UCI bag of words); 10% hold out, words occurring less than
	 * 5 times in the training part are removed.
	 *
	 * @return the split
	 */
	public Split<DocumentProvider<String>> nips() {
		try {
			DocumentProvider<String> provider = limit(new UCIParser(true,
					new File(dataDir, "nips"), "nips", -1).getDocumentProvider());
			HoldOutSplitter<String> splitter = new HoldOutSplitter<String>(
					new Random(42), provider, 0.1, 5);
			return new Split<DocumentProvider<String>>(splitter.getRest(), splitter.getHoldOut());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Checks for the SGML directory <code>reuters21578</code> or the NLTK zip
	 * <code>reuters-nltk/reuters.zip</code> in the data directory.
	 *
	 * @return whether Reuters-21578 is available
	 */
	public boolean hasReuters() {
		return new File(dataDir, "reuters21578").isDirectory() || nltkReuters().isFile();
	}

	/** The NLTK distribution of Reuters-21578 (ApteMod), an alternative to the SGML files. */
	private File nltkReuters() {
		return new File(dataDir, "reuters-nltk/reuters.zip");
	}

	/** Reuters from the SGML files if present, otherwise from the NLTK zip. */
	private LabelingDocumentProvider<String, String> reutersProvider(boolean train, boolean test) {
		File dir = new File(dataDir, "reuters21578");
		if (dir.isDirectory()) {
			return limit(new Reuters21578(true).getCorpusDocumentProvider(dir, train, test));
		}
		return limit(new Reuters21578(true).getNltkDocumentProvider(nltkReuters(), train, test, true));
	}

	/**
	 * Reuters-21578 for perplexity: every ModApte training document once
	 * (ApteMod, from the SGML files or the NLTK zip), 10% held out; words
	 * occurring less than <code>minFrequency</code> times in the training part
	 * are removed.
	 *
	 * @param minFrequency
	 *            minimum word frequency
	 * @return training and test documents
	 */
	public Split<DocumentProvider<String>> reutersPerplexity(int minFrequency) {
		File dir = new File(dataDir, "reuters21578");
		DocumentProvider<String> all;
		if (dir.isDirectory()) {
			all = new Reuters21578(true).getCorpusDocumentProvider(dir, true, false);
		} else {
			all = new Reuters21578(true).getNltkDocumentProvider(nltkReuters(), true, false, false);
		}
		HoldOutSplitter<String> splitter = new HoldOutSplitter<String>(new Random(42), limit(all), 0.1, minFrequency);
		return new Split<DocumentProvider<String>>(splitter.getRest(), splitter.getHoldOut());
	}

	/**
	 * Reuters-21578 with the ModApte split, stop words removed, stemmed. Only the
	 * 10 most frequent categories (as in the paper) and words occurring at least 3
	 * times in the training part are used.
	 *
	 * @return the split
	 */
	public Split<LabelingDocumentProvider<String, String>> reuters() {
		LabelingDocumentProvider<String, String> trainingData = reutersProvider(true, false);
		LabelingDocumentProvider<String, String> training = new LabelingHoldOutSplitter<String, String>(
				new Random(42), trainingData, 0, 3, 10).getRest();
		LabelingDocumentProvider<String, String> testData = reutersProvider(false, true);
		LabelingDocumentProvider<String, String> test = new LabelingHoldOutSplitter<String, String>(
				new Random(42), testData, 1, 0, testData.getAllLabels(),
				(DefaultVocab<String>) training.getVocab()).getHoldOut();
		return new Split<LabelingDocumentProvider<String, String>>(training, test);
	}

	/**
	 * Reuters-21578 training documents without labels, used for the run time
	 * experiments.
	 *
	 * @return the documents
	 */
	public DocumentProvider<String> reutersTraining() {
		return reutersProvider(true, false);
	}

	/**
	 * Checks for the directory <code>20news-18828</code> in the data directory.
	 *
	 * @return whether 20 Newsgroups is available
	 */
	public boolean hasTwentyNG() {
		return new File(dataDir, "20news-18828").isDirectory();
	}

	/**
	 * 20 Newsgroups (duplicates removed version), 25% hold out, words occurring
	 * less than 5 times in the training part are removed.
	 *
	 * @return the split
	 */
	public Split<LabelingDocumentProvider<String, String>> twentyNG() {
		LabelingDocumentProvider<String, String> provider = twentyNGAll();
		LabelingHoldOutSplitter<String, String> splitter = new LabelingHoldOutSplitter<String, String>(
				new Random(42), provider, 0.25, 5, -1);
		return new Split<LabelingDocumentProvider<String, String>>(splitter.getRest(), splitter.getHoldOut());
	}

	/**
	 * Reads all of 20 Newsgroups (stop words removed, stemmed), no split.
	 *
	 * @return all labeled documents of 20 Newsgroups
	 */
	public LabelingDocumentProvider<String, String> twentyNGAll() {
		return limit(new TwentyNGParser(null, true, true)
				.getCorpusDocumentProvider(new File(dataDir, "20news-18828"), 1));
	}

	/** Training and test documents: a random hold out of the given share, rare words removed from the training part. */
	private Split<DocumentProvider<String>> holdOut(DocumentProvider<String> all, double share, int minFrequency) {
		HoldOutSplitter<String> splitter = new HoldOutSplitter<String>(new Random(42), all, share, minFrequency);
		return new Split<DocumentProvider<String>>(splitter.getRest(), splitter.getHoldOut());
	}

	/**
	 * Checks for <code>ap/ap.txt</code> in the data directory.
	 *
	 * @return whether the AP extract (Blei's lda-c) is available: <code>&lt;data&gt;/ap/ap.txt</code>
	 */
	public boolean hasApExtract() {
		return new File(dataDir, "ap/ap.txt").isFile();
	}

	/**
	 * The AP extract (stemmed), no split.
	 *
	 * @param removeStopWords
	 *            whether stop words are removed (the perplexity experiments do, the performance experiment does not)
	 * @return the documents
	 */
	public DocumentProvider<String> apExtract(boolean removeStopWords) {
		return limit(new APParser(removeStopWords, true).getCorpusDocumentProvider(new File(dataDir, "ap/ap.txt")));
	}

	/**
	 * The AP extract for perplexity: 10% held out, words occurring less than 10 times removed.
	 *
	 * @return training and test documents
	 */
	public Split<DocumentProvider<String>> apExtractPerplexity() {
		return holdOut(apExtract(true), 0.1, 10);
	}

	/**
	 * Checks for files named <code>ap*</code> in <code>ap-full</code> of the data directory.
	 *
	 * @return whether the TREC AP corpus is available: the SGML files <code>&lt;data&gt;/ap-full/ap*</code>
	 */
	public boolean hasApFull() {
		File[] files = new File(dataDir, "ap-full").listFiles();
		if (files != null) {
			for (File f : files) {
				if (f.getName().startsWith("ap")) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * The first documents of the TREC AP corpus (stop words removed, stemmed).
	 *
	 * @param maxDocs
	 *            maximum number of documents to read
	 * @return the documents
	 */
	public DocumentProvider<String> apFull(int maxDocs) {
		return limit(new APLargeParser(null, true, true).getCorpusDocumentProvider(new File(dataDir, "ap-full"), maxDocs));
	}

	/**
	 * The TREC AP corpus for perplexity: 10% held out, words occurring less than 5 times removed.
	 *
	 * @param maxDocs
	 *            maximum number of documents to read
	 * @return training and test documents
	 */
	public Split<DocumentProvider<String>> apFullPerplexity(int maxDocs) {
		return holdOut(apFull(maxDocs), 0.1, 5);
	}

	/**
	 * Checks for <code>online-retail/Online Retail.csv</code> in the data directory.
	 *
	 * @return whether the Online Retail data set is available: <code>&lt;data&gt;/online-retail/Online Retail.csv</code>
	 */
	public boolean hasOnlineRetail() {
		return new File(dataDir, "online-retail/Online Retail.csv").isFile();
	}

	/**
	 * Online Retail: one document per invoice, products as words with their quantities.
	 *
	 * @param maxQuantity
	 *            lines with a larger quantity are ignored (25 in the perplexity experiment)
	 * @return the documents
	 */
	public DocumentProvider<String> onlineRetail(int maxQuantity) {
		return limit(new OnlineRetailParser(true, maxQuantity, false)
				.getCorpusDocumentProvider(new File(dataDir, "online-retail/Online Retail.csv")));
	}

	/**
	 * Online Retail for perplexity: 10% held out, products occurring less than 10 times removed.
	 *
	 * @return training and test documents
	 */
	public Split<DocumentProvider<String>> onlineRetailPerplexity() {
		return holdOut(onlineRetail(25), 0.1, 10);
	}

	/**
	 * Checks for <code>tafeng/D01_D02_D11_D12</code> in the data directory.
	 *
	 * @return whether the Ta-Feng data set is available: <code>&lt;data&gt;/tafeng/D01_D02_D11_D12</code>
	 */
	public boolean hasTaFeng() {
		return new File(dataDir, "tafeng/D01_D02_D11_D12").isFile();
	}

	/**
	 * Ta-Feng for perplexity: one document per customer and day, products as words; 10% held out, minimum frequency 20.
	 *
	 * @return training and test documents
	 */
	public Split<DocumentProvider<String>> taFengPerplexity() {
		return holdOut(limit(new TaFengGroceryParser(-1, true, false)
				.getCorpusDocumentProvider(new File(dataDir, "tafeng/D01_D02_D11_D12"))), 0.1, 20);
	}

	/**
	 * Checks for <code>us-supermarket/vocab.uss.csv</code> in the data directory.
	 *
	 * @return whether the US supermarket data set is available: <code>&lt;data&gt;/us-supermarket/vocab.uss.csv</code>
	 */
	public boolean hasUsSupermarket() {
		return new File(dataDir, "us-supermarket/vocab.uss.csv").isFile();
	}

	/**
	 * US supermarket for perplexity: one document per visit; 10% held out, minimum frequency 10.
	 *
	 * @return training and test documents
	 */
	public Split<DocumentProvider<String>> usSupermarketPerplexity() {
		return holdOut(limit(new USSupermarketParser(-1, true)
				.getCorpusDocumentProvider(new File(dataDir, "us-supermarket"), "uss")), 0.1, 10);
	}

	/**
	 * Checks for <code>ohsumed/ohscal.wc.arff</code> in the data directory.
	 *
	 * @return whether OHSUMED (ohscal) is available: <code>&lt;data&gt;/ohsumed/ohscal.wc.arff</code>
	 */
	public boolean hasOhsumed() {
		return new File(dataDir, "ohsumed/ohscal.wc.arff").isFile();
	}

	/**
	 * OHSUMED for classification: 25% held out, words occurring less than 3 times removed.
	 *
	 * @return labeled training and test documents
	 */
	public Split<LabelingDocumentProvider<String, String>> ohsumed() {
		LabelingDocumentProvider<String, String> provider = limit(new OHSUMEDParser()
				.getCorpusDocumentProvider(new File(dataDir, "ohsumed/ohscal.wc.arff")));
		LabelingHoldOutSplitter<String, String> splitter = new LabelingHoldOutSplitter<String, String>(
				new Random(42), provider, 0.25, 3, -1);
		return new Split<LabelingDocumentProvider<String, String>>(splitter.getRest(), splitter.getHoldOut());
	}

	/**
	 * Checks for <code>nytimes/docword.nytimes.txt</code> in the data directory.
	 *
	 * @return whether the UCI New York Times bag of words is available: <code>&lt;data&gt;/nytimes/docword.nytimes.txt</code>
	 */
	public boolean hasNytimes() {
		return new File(dataDir, "nytimes/docword.nytimes.txt").isFile();
	}

	/**
	 * The first documents of the New York Times bag of words.
	 *
	 * @param maxDocs
	 *            maximum number of documents to read
	 * @return the documents
	 */
	public DocumentProvider<String> nytimes(int maxDocs) {
		try {
			return limit(new UCIParser(true, new File(dataDir, "nytimes"), "nytimes", maxDocs).getDocumentProvider());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * The NIPS documents (all), no split.
	 *
	 * @return the documents
	 */
	public DocumentProvider<String> nipsAll() {
		try {
			return limit(new UCIParser(true, new File(dataDir, "nips"), "nips", -1).getDocumentProvider());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
