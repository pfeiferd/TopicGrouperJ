package org.hhn.topicgrouper.paper.perplexity.twc;

public class TWCPerfectPerplexityChangeAlphaExp2 extends
		TWCPerfectPerplexityChangeAlphaExp {
	protected int getSeed() {
		return 43;
	}

	public static void main(String[] args) {
		new TWCPerfectPerplexityChangeAlphaExp2().run(false);
	}
}
