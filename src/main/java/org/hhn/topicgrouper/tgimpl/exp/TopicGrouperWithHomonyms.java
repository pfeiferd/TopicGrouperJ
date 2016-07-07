package org.hhn.topicgrouper.tgimpl.exp;

import gnu.trove.TIntCollection;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;

import java.util.Collections;
import java.util.List;

import org.hhn.topicgrouper.base.Document;
import org.hhn.topicgrouper.base.DocumentProvider;
import org.hhn.topicgrouper.tgimpl.OptimizedTopicGrouper;

/*
 * This is purely experimental (and doesn't even work well)
 */
public class TopicGrouperWithHomonyms<T> extends OptimizedTopicGrouper<T> {
	private final double hEpsilon;
	private final TIntList homonymList;

	public TopicGrouperWithHomonyms(int minWordFrequency, double lambda,
			double hEpsilon, DocumentProvider<T> documentProvider, int minTopics) {
		super(minWordFrequency, lambda, documentProvider, minTopics);
		this.hEpsilon = hEpsilon;
		homonymList = new TIntArrayList();
	}

	protected boolean handleHomonymicTopic(JoinCandidate jc) {
		if (topics[jc.i].size() == 2 && topics[jc.j].size() <= 2) {
			if (handleHomonymCase(jc.i, jc.j)) {
				return true;
			}		
		}
		if (topics[jc.j].size() == 2 && topics[jc.i].size() <= 2) {
			return handleHomonymCase(jc.j, jc.i);
		}
		return false;
	}
	

	// This could be unfolded 
	protected boolean handleHomonymCase(int tid1, int tid2) {
		int size2 = topics[tid2].size();
		int word0 = topics[tid1].get(0);
		int word1 = topics[tid1].get(1);
		double a = 0;
		double b = 0;
		for (int i = 0; i < size2; i++) {
			a += computeTwoWordLogLikelihoodAlt(word0,
					topics[tid2].get(i));
			b += computeTwoWordLogLikelihoodAlt(word1,
					topics[tid2].get(i));
		}
		a /= size2;
		b /= size2;
		
		double relDiff = Math.abs(a - b) / (Math.abs(a) + Math.abs(b));
		if (relDiff > hEpsilon) {
			System.out.print("Incoherent: ");
			printTopic(topics[tid1]);
			printTopic(topics[tid2]);
			System.out.println();
			if (a < b) {
				toOneWordTopic(tid1, 1); // tid1 1 is homonym
			} else {
				toOneWordTopic(tid1, 0); // tid2 0 is homonym
			}
			return true;
		}
		return false;
	}

	protected void printTopic(TIntList list) {
		System.out.print("{");
		for (int i = 0; i < list.size(); i++) {
			System.out.print(documentProvider.getWord(list.get(i)));
			System.out.print(" ");
		}
		System.out.print("} ");
	}
	
	private int counter = 0;

	protected void toOneWordTopic(int tid, int pos) {
		int wordIndex = topics[tid].get(pos);
		System.out.println("Homonym: " + documentProvider.getWord(wordIndex));
		if (wordIndex < 400) {
			counter++;
		}
		System.out.println("Errors: " + counter);
		topics[tid].removeAt(pos);

		int otherWord = topics[tid].get(0);

		homonymList.add(wordIndex);
		int fr = documentProvider.getWordFrequency(wordIndex);
		topicSizes[tid] -= fr;
		double v = computeOneWordTopicLogLikelihood(otherWord);
		totalLikelihood += v - topicLikelihoods[tid];
		topicLikelihoods[tid] = v;

		for (int j = 0; j < documents.size(); j++) {
			topicFrequencyPerDocuments[tid][j] -= documents.get(j)
					.getWordFrequency(wordIndex);
		}

		sumWordFrTimesLogWordFrByTopic[tid] -= fr * Math.log(fr);
	}

	@Override
	protected TIntCollection getHomonyms() {
		return homonymList;
	}

	protected double computeTwoWordLogLikelihoodAlt(int word1, int word2) {
		double sum = computeTwoWordLogLikelihoodHelp(word1, word2);

		int fr1 = documentProvider.getWordFrequency(word1);
		int fr2 = documentProvider.getWordFrequency(word2);

		sum += fr1 * Math.log(fr1);
		sum += fr2 * Math.log(fr2);
		int sizeSum = fr1 + fr2;
		sum -= (sizeSum) * Math.log(sizeSum);

		return sum;
	}

	protected double computeTwoTopicCoherence(int topic1, int topic2) {
		TIntList c2 = topics[topic2];
		TIntList c1 = topics[topic1];
		double sum = 0;
		for (int i = 0; i < documents.size(); i++) {
			int min = Integer.MAX_VALUE;
			int max = 0;
			Document<T> d = documents.get(i);
			for (int j = 0; j < c1.size(); j++) {
				int fr = d.getWordFrequency(c1.get(j));
				if (fr < min) {
					min = fr;
				}
				if (fr > max) {
					max = fr;
				}
			}
			for (int j = 0; j < c2.size(); j++) {
				int fr = d.getWordFrequency(c2.get(j));
				if (fr < min) {
					min = fr;
				}
				if (fr > max) {
					max = fr;
				}
			}
			sum += Math.log(min + 1) - Math.log(max + 1);
		}
		return sum;
	}

}
