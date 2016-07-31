package org.hhn.topicgrouper.report;

import java.io.PrintStream;

import org.hhn.topicgrouper.ldaimpl.LDAGibbsSampler;
import org.hhn.topicgrouper.ldaimpl.LDASolutionListener;

public class BasicLDAResultReporter<T> implements LDASolutionListener<T> {
	private final PrintStream pw;
	private long startTime;

	public BasicLDAResultReporter(PrintStream pw) {
		this.pw = pw;
	}

	@Override
	public void beforeInitialization(LDAGibbsSampler<T> sampler) {
		pw.println("Initializing...");
		startTime = System.currentTimeMillis();
	}

	@Override
	public void done(LDAGibbsSampler<T> sampler) {
		pw.println("Done.");
		pw.print("Require time: ");
		pw.println((System.currentTimeMillis() - startTime) / 1000);
	}
	
	protected void printTopics() {
		
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
}
