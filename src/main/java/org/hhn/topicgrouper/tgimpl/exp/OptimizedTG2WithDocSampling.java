package org.hhn.topicgrouper.tgimpl.exp;

import java.util.BitSet;
import java.util.Random;

import org.hhn.topicgrouper.base.DocumentProvider;
import org.hhn.topicgrouper.tgimpl.OptimizedTopicGrouper2;

public class OptimizedTG2WithDocSampling<T> extends OptimizedTopicGrouper2<T> {
	private final double correctionFactor;
	private final int[] sample;

	public OptimizedTG2WithDocSampling(int minWordFrequency, double lambda,
			DocumentProvider<T> documentProvider, int minTopics,
			double samplingRatio, Random random) {
		super(minWordFrequency, lambda, documentProvider, minTopics);
		int docs = documentProvider.getDocuments().size();
		int samplingTries = (int) (docs * samplingRatio);
		if (samplingTries <= 0) {
			throw new IllegalArgumentException("bad number of sampling tries "
					+ samplingTries);
		}

		correctionFactor = ((double) docs) / samplingTries;

		sample = new int[samplingTries];
		BitSet bitSet = new BitSet(docs);
		for (int i = 0; i < sample.length; i++) {
			// Ensure there is no double sampling.
			int index = bitSet.nextClearBit(random.nextInt(docs));
			if (index == -1 || index == sample.length) {
				index = bitSet.nextClearBit(0);
			}
			bitSet.set(index);
			sample[i] = index;
		}
		//invertedIndex = createInvertedIndex2();
	}

//	@Override
//	protected TIntObjectMap<List<DocIndexAndWordFr>> createInvertedIndex() {
//		return null;
//	}
//
//	protected TIntObjectMap<List<DocIndexAndWordFr>> createInvertedIndex2() {
//		TIntObjectMap<List<DocIndexAndWordFr>> invertedIndex = new TIntObjectHashMap<List<DocIndexAndWordFr>>();
//		for (int j = 0; j < sample.length; j++) {
//			int i = sample[j];
//			Document<T> d = documents.get(i);
//			TIntIterator it = d.getWordIndices().iterator();
//			while (it.hasNext()) {
//				int wordIndex = it.next();
//				List<DocIndexAndWordFr> value = invertedIndex.get(wordIndex);
//				if (value == null) {
//					value = new ArrayList<DocIndexAndWordFr>();
//					invertedIndex.put(wordIndex, value);
//				}
//				DocIndexAndWordFr entry = new DocIndexAndWordFr();
//				entry.docIndex = i;
//				entry.wordFr = d.getWordFrequency(wordIndex);
//				int position = Collections.binarySearch(value, entry);
//				value.add(-position - 1, entry);
//			}
//		}
//		return invertedIndex;
//	}
//
//	@Override
//	protected double computeOneWordTopicLogLikelihood(int wordIndex) {
//		double sum = 0;
//		for (int j = 0; j < sample.length; j++) {
//			int i = sample[j];
//			Document<T> d = documents.get(i);
//			double wordFrPerDoc = d.getWordFrequency(wordIndex);
//			if (wordFrPerDoc > 0 && documentSizes[i] > 0) {
//				sum += onePlusLambdaDivDocSizes[i] * wordFrPerDoc
//						* (Math.log(wordFrPerDoc) - logDocumentSizes[i]);
//			}
//		}
//		return sum;
//	}

	@Override
	protected double computeTwoTopicLogLikelihood(int topic1, int topic2) {
		double sum = 0;
		int[] frTopicPerDocument1 = topicFrequencyPerDocuments[topic1];
		int[] frTopicPerDocument2 = topicFrequencyPerDocuments[topic2];
		for (int j = 0; j < sample.length; j++) {
			int i = sample[j];
			if ((frTopicPerDocument1[i] > 0 || frTopicPerDocument2[i] > 0)
					&& documentSizes[i] > 0) {
				int fr = frTopicPerDocument1[i] + frTopicPerDocument2[i];
				sum += onePlusLambdaDivDocSizes[i] * fr
						* (Math.log(fr) - logDocumentSizes[i]);
			}
		}
		sum *= correctionFactor;

		sum += sumWordFrTimesLogWordFrByTopic[topic1];
		sum += sumWordFrTimesLogWordFrByTopic[topic2];
		int sizeSum = topicSizes[topic1] + topicSizes[topic2];
		sum -= (sizeSum) * Math.log(sizeSum);

		return sum;
	}
}
