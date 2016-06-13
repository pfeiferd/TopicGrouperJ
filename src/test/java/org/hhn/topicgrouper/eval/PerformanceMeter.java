package org.hhn.topicgrouper.eval;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Random;

import org.hhn.topicgrouper.base.DocumentProvider;
import org.hhn.topicgrouper.base.Solution;
import org.hhn.topicgrouper.base.Solver;
import org.hhn.topicgrouper.base.Solver.SolutionListener;
import org.hhn.topicgrouper.tgimpl.OptimizedTopicGrouper;
import org.hhn.topicgrouper.util.OutputStreamMultiplexer;

public class PerformanceMeter {
	private final Random random;

	private long startTime;
	private long endTime;
	private long maxTopics;

	public PerformanceMeter() {
		random = new Random(42);
	}

	public void measure(PrintStream out) throws IOException {
		out.println("docs; words per doc; topics; words per topic; max topics; millis");
		int topics = 10;
		int wordsPerTopic = 10;
		int wordsPerDoc = 100;
		int docs;

//		for (docs = 100; docs <= 6000; docs += 100) {
//			test(out, docs, wordsPerDoc, topics, wordsPerTopic);
//		}
//		out.println();
		
//		docs = 1000;		
//		for (wordsPerDoc = 100; wordsPerDoc <= 6000; wordsPerDoc += 100) {
//			test(out, docs, wordsPerDoc, topics, wordsPerTopic);
//		}
//		out.println();
		
//		wordsPerDoc = 100;
		docs = 10000;
//		for (wordsPerTopic = 10; wordsPerTopic <= 200; wordsPerTopic += 10) {
//			test(out, docs, wordsPerDoc, topics, wordsPerTopic);			
//		}
//		out.println();
		
		wordsPerTopic = 10;
		for (topics = 10; topics <= 200; topics += 10) {
			test(out, docs, wordsPerDoc, topics, wordsPerTopic);						
		}
	}

	protected void test(PrintStream out, int docs, int wordsPerDoc, int topics,
			int wordsPerTopic) throws IOException {
		AbstractTGTester<String> tester = createTGTester(docs, wordsPerDoc,
				topics, wordsPerTopic);
		out.print(docs);
		out.print("; ");
		out.print(wordsPerDoc);
		out.print("; ");
		out.print(topics);
		out.print("; ");
		out.print(wordsPerTopic);
		out.print("; ");
		tester.run();
		out.print(maxTopics);
		out.print("; ");
		out.println(endTime - startTime);
	}

	protected AbstractTGTester<String> createTGTester(final int docs,
			final int wordsPerDoc, final int topics, final int wordsPerTopic)
			throws IOException {
		return new AbstractTGTester<String>(null) {
			@Override
			protected DocumentProvider<String> createDocumentProvider() {
				double[] alpha = new double[topics];
				for (int i = 0; i < alpha.length; i++) {
					alpha[i] = 50.d / topics;
				}
				return new TWCLDAPaperDocumentGenerator(random, alpha, docs,
						wordsPerTopic, wordsPerTopic, wordsPerDoc, wordsPerDoc,
						0, null, 0d, 0d);
			}

			@Override
			protected Solver<String> createSolver(
					DocumentProvider<String> documentProvider) {
				return new OptimizedTopicGrouper<String>(1, 0,
						documentProvider, 1);
			}

			@Override
			protected SolutionListener<String> createSolutionListener(
					PrintStream out) {
				return new SolutionListener<String>() {
					@Override
					public void updatedSolution(int newTopicIndex,
							int oldTopicIndex, double improvement, int t1Size,
							int t2Size, Solution<String> solution) {
					}

					@Override
					public void initialized(Solution<String> initialSolution) {
					}

					@Override
					public void initalizing(double percentage) {
					}

					@Override
					public void done() {
						endTime = System.currentTimeMillis();
					}

					@Override
					public void beforeInitialization(int maxTopics,
							int documents) {
						startTime = System.currentTimeMillis();
						PerformanceMeter.this.maxTopics = maxTopics;
					}
				};
			}
		};
	}

	public static void main(String[] args) throws IOException {
		File file = new File("./target/performanc.csv");
		FileOutputStream out = new FileOutputStream(file);
		OutputStreamMultiplexer multiplexer = new OutputStreamMultiplexer();
		multiplexer.addOutputStream(out);
		multiplexer.addOutputStream(System.out);
		new PerformanceMeter().measure(new PrintStream(multiplexer));
	}
}
