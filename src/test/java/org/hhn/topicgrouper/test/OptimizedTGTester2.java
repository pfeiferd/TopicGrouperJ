package org.hhn.topicgrouper.test;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Random;

import javax.swing.UIManager;

import org.hhn.topicgrouper.base.DocumentProvider;
import org.hhn.topicgrouper.base.Solution;
import org.hhn.topicgrouper.base.Solver;
import org.hhn.topicgrouper.base.Solver.SolutionListener;
import org.hhn.topicgrouper.eval.AbstractTGTester;
import org.hhn.topicgrouper.eval.TWCLDAPaperDocumentGenerator;
import org.hhn.topicgrouper.tgimpl.OptimizedTopicGrouper;

public class OptimizedTGTester2 extends AbstractTGTester<String> {
	public OptimizedTGTester2(File outputFile) throws IOException {
		super(outputFile);
	}

	protected SolutionListener<String> createSolutionListener(PrintStream out) {
		// return new BasicSolutionReporter<String>(out, 4, true);
		return new SolutionListener<String>() {

			@Override
			public void updatedSolution(int newTopicIndex, int oldTopicIndex,
					double improvement, int t1Size, int t2Size,
					Solution<String> solution) {
				// TODO Auto-generated method stub

			}

			@Override
			public void initialized(Solution<String> initialSolution) {
				// TODO Auto-generated method stub

			}

			@Override
			public void initalizing(double percentage) {
				// TODO Auto-generated method stub

			}

			@Override
			public void beforeInitialization(int maxTopics, int documents) {
				// TODO Auto-generated method stub

			}
			
			@Override
			public void done() {
				// TODO Auto-generated method stub
				
			}
		};
	}

	protected DocumentProvider<String> createDocumentProvider() {
		return new TWCLDAPaperDocumentGenerator(new Random(45), new double[] {
				5, 0.5, 0.5, 0.5 }, 6000, 100, 100, 30, 30, 0, null, 0.5, 0.8);
	}

	@Override
	protected Solver<String> createSolver(
			DocumentProvider<String> documentProvider) {
		// return new OptimizedTG2WithDocSampling<String>(1, 0,
		// documentProvider,
		// 1, 0.1, new Random(22));
		return new OptimizedTopicGrouper<String>(1, 0, documentProvider, 1);
	}

	public static void main(String[] args) throws IOException {
		try {
			// Set System L&F
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		long start = System.currentTimeMillis();
		new OptimizedTGTester2(null).run();
		System.out.println(System.currentTimeMillis() - start);
	}
}
