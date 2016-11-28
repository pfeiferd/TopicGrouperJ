package org.hhn.topicgrouper.eval;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.hhn.topicgrouper.doc.Document;
import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.doc.DocumentProvider.Vocab;
import org.hhn.topicgrouper.doc.impl.AbstractDocumentImpl;
import org.hhn.topicgrouper.util.DirichletSampler;
import org.hhn.topicgrouper.util.RandomDirichlet1Dist;

public class TWCLDAPaperDocumentGenerator implements DocumentProvider<String>, Vocab<String> {
	private final List<Document<String>> documentsImmutable;
	private final DirichletSampler dirichlet;
	private final int[] indexToFr;
	private int size;

	public TWCLDAPaperDocumentGenerator() {
		this(new Random(45));
	}

	public TWCLDAPaperDocumentGenerator(Random random) {
		this(random, new double[] { 5, 0.5, 0.5, 0.5 }, 6000, 100, 100,
				30, 30, 0, null, 0.5, 0.8);
	}
	
	public TWCLDAPaperDocumentGenerator(Random random, double[] dirichletAlpha,
			int docs, int minWordsPerTopic, int maxWordsPerTopic,
			int minWordsPerDoc, int maxWordsPerDoc, int homonyms,
			double[] attachHomonymToTopicDistribution, double minAttachProb,
			double maxAttachProb) {
		this(random, dirichletAlpha, docs, computeWordsPerTopic(random,
				dirichletAlpha.length, minWordsPerTopic, maxWordsPerTopic),
				minWordsPerDoc, maxWordsPerDoc, homonyms,
				attachHomonymToTopicDistribution, minAttachProb, maxAttachProb);
	}

	public TWCLDAPaperDocumentGenerator(
			final Random random,
			double[] dirichletAlpha,
			int docs,
			int[] wordsPerTopic,
			int minWordsPerDoc,
			int maxWordsPerDoc,
			int homonyms,
			// Depending on the this distribution it is sampled to how many
			// words of
			// different topics the a homonym will be attached.
			// (at least two)
			// The topics are randomly chosen prior to document generation (so
			// independently of the documents).
			// The word to which a homonym will be attached per selected
			// document is
			// randomly chosen once (and remains fixed)
			double[] attachHomonymToTopicDistribution, double minAttachProb,
			double maxAttachProb) {
		List<Document<String>> documents = new ArrayList<Document<String>>();
		documentsImmutable = Collections.unmodifiableList(documents);

		RandomDirichlet1Dist dirichlet1Dist = new RandomDirichlet1Dist(random);

		double[][] wordPerTopicDist = new double[dirichletAlpha.length][];
		int[] sumWordsUpToTopic = new int[wordsPerTopic.length];

		int allWords = 0;
		for (int i = 0; i < wordPerTopicDist.length; i++) {
			wordPerTopicDist[i] = dirichlet1Dist
					.nextDistribution(wordsPerTopic[i]);
			allWords += wordsPerTopic[i];
			sumWordsUpToTopic[i] = allWords;
		}
		indexToFr = new int[allWords + homonyms];

		dirichlet = new DirichletSampler(random);
		int minMaxWordsPerDocDiff = maxWordsPerDoc - minWordsPerDoc;

		List<Integer> drawn = new ArrayList<Integer>();
		List<Integer> allTopics = new ArrayList<Integer>();
		for (int i = 0; i < dirichletAlpha.length; i++) {
			allTopics.add(i);
		}

		Map<Integer, Object[]> wordToHomonym = new HashMap<Integer, Object[]>();
		for (int i = 0; i < homonyms; i++) {
			allTopics.addAll(drawn);
			drawn.clear();
			int topicsForHomonym = (attachHomonymToTopicDistribution == null ? 0
					: nextDiscrete(attachHomonymToTopicDistribution, random)) + 2;
			for (int j = 0; j < topicsForHomonym; j++) {
				int topicIndex = random.nextInt(allTopics.size());
				drawn.add(allTopics.remove(topicIndex));
			}
			for (int topicIndex : drawn) {
				int wordInTopicIndex = i; // random.nextInt(wordsPerTopic[topicIndex]);
				int wordIndex = wordInTopicIndex
						+ (topicIndex == 0 ? 0
								: sumWordsUpToTopic[topicIndex - 1]);
				double attachProb = minAttachProb
						+ (maxAttachProb - minAttachProb) * random.nextDouble();
				Object[] value = new Object[] { allWords + i, attachProb };
				wordToHomonym.put(wordIndex, value);
			}
		}

		for (int i = 0; i < docs; i++) {
			double[] topicPerDocDist = dirichlet.sample(dirichletAlpha);
			AbstractDocumentImpl<String> document = new AbstractDocumentImpl<String>(i) {
				@Override
				public DocumentProvider<String> getProvider() {
					return TWCLDAPaperDocumentGenerator.this;
				}
			};
			int wordsPerDoc = (minMaxWordsPerDocDiff == 0 ? 0 : random
					.nextInt(minMaxWordsPerDocDiff)) + minWordsPerDoc;

			for (int j = 0; j < wordsPerDoc; j++) {
				int topicIndex = nextDiscrete(topicPerDocDist, random);
				int wordInTopicIndex = nextDiscrete(
						wordPerTopicDist[topicIndex], random);
				int wordIndex = wordInTopicIndex
						+ (topicIndex == 0 ? 0
								: sumWordsUpToTopic[topicIndex - 1]);
				document.addWordOccurrence(wordIndex);
				indexToFr[wordIndex]++;
				size++;
				Object[] homonymInfo = wordToHomonym.get(wordIndex);
				if (homonymInfo != null) {
					if (random.nextDouble() >= (Double) homonymInfo[1]) {
						int homonym = (Integer) homonymInfo[0];
						document.addWordOccurrence(homonym);
						indexToFr[homonym]++;
						size++;
					}
				}
			}
			documents.add(document);
		}
	}

	protected static int[] computeWordsPerTopic(Random random, int topics,
			int minWordsPerTopic, int maxWordsPerTopic) {
		int[] wordsPerTopic = new int[topics];
		int minMaxWordsPerTopicDiff = maxWordsPerTopic - minWordsPerTopic;
		for (int i = 0; i < topics; i++) {
			wordsPerTopic[i] = (minMaxWordsPerTopicDiff == 0 ? 0 : random
					.nextInt(minMaxWordsPerTopicDiff)) + minWordsPerTopic;
		}
		return wordsPerTopic;
	}
	
	@Override
	public Vocab<String> getVocab() {
		return this;
	}

	@Override
	public List<Document<String>> getDocuments() {
		return documentsImmutable;
	}

	@Override
	public int getNumberOfWords() {
		return indexToFr.length;
	}

	@Override
	public String getWord(int index) {
		return String.valueOf(index);
	}

	@Override
	public int getIndex(String word) {
		return Integer.valueOf(word);
	}

	@Override
	public int getWordFrequency(int index) {
		return indexToFr[index];
	}
	
	@Override
	public int getSize() {
		return size;
	}

	public static int nextDiscrete(double[] probs, Random random) {
		double sum = 0.0;
		for (int i = 0; i < probs.length; i++)
			sum += probs[i];

		double r = random.nextDouble() * sum;

		sum = 0.0;
		for (int i = 0; i < probs.length; i++) {
			sum += probs[i];
			if (sum > r)
				return i;
		}
		return probs.length - 1;
	}
}
