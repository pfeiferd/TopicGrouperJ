package org.hhn.topicgrouper.paper.perplexity.twc;

 
public class TWCUnigramPerplexityChangeAlphaExp2 extends
		TWCUnigramPerplexityChangeAlphaExp {
	protected int getSeed() {
		return 43;
	}

	public static void main(String[] args) {
		new TWCUnigramPerplexityChangeAlphaExp2().run(false);
	}
}
