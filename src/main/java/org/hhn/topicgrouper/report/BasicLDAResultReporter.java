package org.hhn.topicgrouper.report;

import java.io.PrintStream;
import java.util.Arrays;

import org.hhn.topicgrouper.base.DocumentProvider;
import org.hhn.topicgrouper.ldaimpl.LDAGibbsSampler;
import org.hhn.topicgrouper.ldaimpl.LDASolutionListener;

public class BasicLDAResultReporter<T> implements LDASolutionListener<T> {
	private final PrintStream pw;
	private long startTime;
	private int topWords;

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
		printTopics(sampler);
	}
	
	protected void printTopics(LDAGibbsSampler<T> sampler) {
		ValueAndIndex[] vi = fillVI(sampler.getTopicFrCount());
		DocumentProvider<T> provider = sampler.getDocumentProvider();
		
		int[][] topicWordCounts = sampler.getTopicWordAssignmentCount();
		for (int i = 0; i < vi.length; i++) {
			pw.print("Topic ");
			pw.print(i);
			pw.print(" (");
			pw.print(vi[i].getValue());
			pw.println("):");
			int[] wordCounts = topicWordCounts[vi[i].getIndex()];
			ValueAndIndex[] viWords = fillVI(wordCounts);
			for (int j = 0; j < topWords; j++) {
				pw.print(provider.getWord(viWords[j].getIndex()));
				pw.print(" (");
				pw.print(viWords[j].getValue());
				pw.print(") ");
			}
			pw.println();
		}
	}
	
	protected ValueAndIndex[] fillVI(int[] values) {
		ValueAndIndex[] result = new ValueAndIndex[values.length];
		for (int i = 0; i < values.length; i++) {
			result[i] = new ValueAndIndex(values[i], i);
		}
		Arrays.sort(result);
		return result;
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
	
	private static class ValueAndIndex implements Comparable<ValueAndIndex>{
		private final int value;
		private final int index;
		
		public ValueAndIndex(int value, int index) {
			this.value = value;
			this.index = index;
		}
		
		public int getValue() {
			return value;
		}
		
		public int getIndex() {
			return index;
		}
		
		@Override
		public int compareTo(ValueAndIndex o) {
			return o.value - value;
		}
	}
}
