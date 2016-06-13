package org.hhn.topicgrouper.figures;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import javax.swing.UIManager;

import org.hhn.topicgrouper.base.DocumentProvider;
import org.hhn.topicgrouper.base.Solver;
import org.hhn.topicgrouper.base.Solver.SolutionListener;
import org.hhn.topicgrouper.eval.AbstractTGTester;
import org.hhn.topicgrouper.eval.TWCLDAPaperDocumentGenerator;
import org.hhn.topicgrouper.report.BasicSolutionReporter;
import org.hhn.topicgrouper.tgimpl.OptimizedTopicGrouper;

public class OptimizedTGTester2 extends AbstractTGTester<String> {
	public OptimizedTGTester2(File outputFile) throws IOException {
		super(outputFile);
	}
	
	protected SolutionListener<String> createSolutionListener(PrintStream out) {
		return new BasicSolutionReporter<String>(out, 4, true);
	}
		
	protected DocumentProvider<String> createDocumentProvider() {
		return new TWCLDAPaperDocumentGenerator();
	}
	
	@Override
	protected Solver<String> createSolver(DocumentProvider<String> documentProvider) {
		return new OptimizedTopicGrouper<String>(1, 0, documentProvider, 1);
	}

	public static void main(String[] args) throws IOException {
		try {
			// Set System L&F
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		new OptimizedTGTester2(null).run();
	}
}
