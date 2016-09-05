package org.hhn.topicgrouper.report;

import java.io.PrintStream;
import java.util.Arrays;

import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.lda.impl.LDAGibbsSampler;
import org.hhn.topicgrouper.lda.impl.LDASolutionListener;

public class BasicLDAResultReporter<T> implements LDASolutionListener<T> {
	protected final PrintStream pw;
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
		ValueAndIndex[] vi = new ValueAndIndex[sampler.getNTopics()];
		for (int i = 0; i < vi.length; i++) {
			vi[i] = new ValueAndIndex(sampler.getTopicFrCount(i), i);
		}
		Arrays.sort(vi);
		
		DocumentProvider<T> provider = sampler.getDocumentProvider();
		ValueAndIndex[] viWords = new ValueAndIndex[sampler.getNWords()];
		
		for (int i = 0; i < vi.length; i++) {
			pw.print("Topic ");
			pw.print(i);
			pw.print(" (");
			pw.print(vi[i].getValue());
			pw.println("):");
			for (int j = 0; j < viWords.length; j++) {
				viWords[j] = new ValueAndIndex(sampler.getTopicWordAssignmentCount(vi[i].getIndex(), j), j);
			}
			Arrays.sort(viWords);
			for (int j = 0; j < topWords; j++) {
				pw.print(provider.getWord(viWords[j].getIndex()));
				pw.print(" (");
				pw.print(viWords[j].getValue());
				pw.print(") ");
			}
			pw.println();
		}
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
