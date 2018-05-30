package org.hhn.topicgrouper.lda.report;

import java.io.PrintStream;
import java.util.Arrays;

import org.hhn.topicgrouper.lda.impl.LDAGibbsSampler;
import org.hhn.topicgrouper.lda.impl.LDASolutionListener;
import org.hhn.topicgrouper.validation.AbstractTopicModeler;

public class BasicLDAResultReporter<T> implements LDASolutionListener<T> {
	protected final PrintStream pw;
	protected final int topWords;
	private long startTime;

	public BasicLDAResultReporter(PrintStream pw, int topWords) {
		this.pw = pw;
		this.topWords = topWords;
	}

	@Override
	public void beforeInitialization(LDAGibbsSampler<T> sampler) {
		pw.println("Initializing...");
		startTime = System.currentTimeMillis();
	}

	@Override
	public void done(LDAGibbsSampler<T> sampler) {
		pw.println("Done.");
		pw.print("Required time: ");
		pw.print((System.currentTimeMillis() - startTime) / 1000);
		pw.println(" secs.");
		printTopics(pw, sampler, topWords);
	}
	
	@Override
	public void initalizing(LDAGibbsSampler<T> sampler, int document) {
	}

	@Override
	public void initialized(LDAGibbsSampler<T> sampler) {
		pw.println("Initializition done.");
		pw.println("Starting training...");
	}

	@Override
	public void updatedSolution(LDAGibbsSampler<T> sampler, int iteration) {
		pw.print("Iteration ");
		pw.println(iteration);
	}
	
	
	public static <T> void printTopics(PrintStream pw, AbstractTopicModeler<T> sampler, int topWords) {
		ValueAndIndex[] vi = new ValueAndIndex[sampler.getNTopics()];
		for (int i = 0; i < vi.length; i++) {
			vi[i] = new ValueAndIndex(sampler.getTopicProb(i), i);
		}
		Arrays.sort(vi);
		
		ValueAndIndex[] viWords = new ValueAndIndex[sampler.getNWords()];
		
		for (int i = 0; i < vi.length; i++) {
			pw.print("Topic ");
			pw.print(i);
			pw.print(" (");
			pw.print(vi[i].getValue());
			pw.print(") (Internal Index ");
			pw.print(vi[i].getIndex());
			pw.println(");");
			for (int j = 0; j < viWords.length; j++) {
				viWords[j] = new ValueAndIndex(sampler.getPhi(vi[i].getIndex(), j), j);
			}
			Arrays.sort(viWords);
			for (int j = 0; j < topWords; j++) {
				pw.print(sampler.getVocab().getWord(viWords[j].getIndex()));
				pw.print(" (");
				pw.print(viWords[j].getValue());
				pw.print(") ");
			}
			pw.println();
		}
	}
	
	private static class ValueAndIndex implements Comparable<ValueAndIndex>{
		private final double value;
		private final int index;
		
		public ValueAndIndex(double value, int index) {
			this.value = value;
			this.index = index;
		}
		
		public double getValue() {
			return value;
		}
		
		public int getIndex() {
			return index;
		}
		
		@Override
		public int compareTo(ValueAndIndex o) {
			return (int) Math.signum(o.value - value);
		}
	}
}
