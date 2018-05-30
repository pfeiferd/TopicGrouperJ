package org.hhn.topicgrouper.paper.perplexity.twc;


public class TWCPLSAPerplexityChangeAlphaExp2 extends
		TWCPLSAPerplexityChangeAlphaExp {
	public TWCPLSAPerplexityChangeAlphaExp2(int tries) {
		super(10);
	}

	protected int getSeed() {
		return 43;
	}

	public static void main(String[] args) {
		new TWCPLSAPerplexityChangeAlphaExp2(50).run(false);
	}
}
