package org.hhn.topicgrouper.test;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Random;

import javax.swing.UIManager;

import org.hhn.topicgrouper.base.DocumentProvider;
import org.hhn.topicgrouper.base.Solver;
import org.hhn.topicgrouper.base.Solver.SolutionListener;
import org.hhn.topicgrouper.eval.TWCLDAPaperDocumentGenerator;
import org.hhn.topicgrouper.report.BasicSolutionReporter;
import org.hhn.topicgrouper.tgimpl.OptimizedTopicGrouper;
import org.hhn.topicgrouper.validation.InDocumentHoldOutSplitter;

public class OptimizedTGTester extends AbstractTGTester<String> {
	private DocumentProvider<String> testDocumentProvider;
	
	public OptimizedTGTester(File outputFile) throws IOException {
		super(outputFile);
	}
	
	protected SolutionListener<String> createSolutionListener(PrintStream out) {
		BasicSolutionReporter<String> res = new BasicSolutionReporter<String>(out, 4, true);
		res.setTestDocumentProvider(testDocumentProvider);
		return res;
	}
		
	protected DocumentProvider<String> createDocumentProvider() {
		DocumentProvider<String> provider = new TWCLDAPaperDocumentGenerator();
		System.out.println("Number of documents: "
				+ provider.getDocuments().size());
		System.out.println("Vocabulary size of document collection: "
				+ provider.getNumberOfWords());
		
		InDocumentHoldOutSplitter<String> splitter = new InDocumentHoldOutSplitter<String>(new Random(42), provider, 0.1, 0);
		testDocumentProvider =  splitter.getHoldOut();

		return splitter.getRest();
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
		new OptimizedTGTester(null).run();//new File("target/d.txt"));
	}
}
