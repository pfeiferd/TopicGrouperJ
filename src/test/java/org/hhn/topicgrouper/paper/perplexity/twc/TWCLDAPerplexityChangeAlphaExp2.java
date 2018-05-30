package org.hhn.topicgrouper.paper.perplexity.twc;


public class TWCLDAPerplexityChangeAlphaExp2 extends TWCLDAPerplexityChangeAlphaExp {
	public TWCLDAPerplexityChangeAlphaExp2(int tries, int maxSteps, int seed) {
		super(tries, maxSteps, seed);
	}

	public static void main(String[] args) {
		new TWCLDAPerplexityChangeAlphaExp2(50, 10, 43).run();
	}
}
