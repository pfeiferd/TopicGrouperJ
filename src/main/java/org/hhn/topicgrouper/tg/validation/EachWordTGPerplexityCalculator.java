package org.hhn.topicgrouper.tg.validation;

import org.hhn.topicgrouper.doc.Document;
import org.hhn.topicgrouper.tg.TGSolution;


public class EachWordTGPerplexityCalculator<T> extends TGPerplexityCalculator<T> {
	public static final double DEFAULT_LIDSTONE_LAMDA = 0.00000000000001d;

	private final double lidstoneLambda;

	public EachWordTGPerplexityCalculator(double lidstoneLambda) {
		super(false);
		this.lidstoneLambda = lidstoneLambda;
	}
	
	@Override
	protected int correctAddendForB(int dSize) {
		return 1;
	}
	
	@Override
	public double computeLogProbability(Document<T> d, int dSize,
			TGSolution<T> s) {
		return super.computeLogProbability(d, dSize, s) / dSize;
	}

	@Override
	protected double correctTopicFrInDoc(int topicFrInDoc) {
		return (topicFrInDoc - 1) + lidstoneLambda; // Exclude held out word (-1) and add 1
										// for Lidstone smoothing.
	}

	protected double correctDocSize(int docSize, int nTopics) {
		return (docSize - 1) + lidstoneLambda * nTopics; // Exclude held out word (-1) and add n
										// topics for Lidstone smoothing.
	}
}
