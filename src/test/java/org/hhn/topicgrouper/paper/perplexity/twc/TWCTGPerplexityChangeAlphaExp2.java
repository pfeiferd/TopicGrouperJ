package org.hhn.topicgrouper.paper.perplexity.twc;


public class TWCTGPerplexityChangeAlphaExp2 extends TWCTGPerplexityChangeAlphaExp {	
	protected int getSeed() {
		return 43;
	}

	public static void main(String[] args) {
		new TWCTGPerplexityChangeAlphaExp2().run();
	}
}
