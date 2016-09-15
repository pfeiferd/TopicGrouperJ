package org.hhn.topicgrouper.test;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Random;

import javax.swing.UIManager;

import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.doc.impl.InDocumentHoldOutSplitter;
import org.hhn.topicgrouper.eval.AbstractTGTester;
import org.hhn.topicgrouper.eval.TWCLDAPaperDocumentGenerator;
import org.hhn.topicgrouper.tg.TGSolutionListener;
import org.hhn.topicgrouper.tg.TGSolver;
import org.hhn.topicgrouper.tg.report.CSVSolutionReporter;
import org.hhn.topicgrouper.tgimpl.OptimizedTopicGrouper;

public class OptimizedTGTester extends AbstractTGTester<String> {
	private DocumentProvider<String> testDocumentProvider;
	
	public OptimizedTGTester(File outputFile) throws IOException {
		super(outputFile);
	}
	
	protected TGSolutionListener<String, T> createSolutionListener(PrintStream out) {
		CSVSolutionReporter<String> res = new CSVSolutionReporter<String>(out, true);
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
	protected TGSolver<String> createSolver(DocumentProvider<String> documentProvider) {
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
