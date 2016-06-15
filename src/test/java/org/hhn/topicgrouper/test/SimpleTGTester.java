package org.hhn.topicgrouper.test;

import java.io.File;
import java.io.IOException;

import javax.swing.UIManager;

import org.hhn.topicgrouper.base.DocumentProvider;
import org.hhn.topicgrouper.base.Solver;
import org.hhn.topicgrouper.eval.AbstractTGTester;
import org.hhn.topicgrouper.eval.TWCLDAPaperDocumentGenerator;
import org.hhn.topicgrouper.tgimpl.exp.SimpleTopicGrouper;

public class SimpleTGTester extends AbstractTGTester<String> {
	public SimpleTGTester(File outputFile) throws IOException {
		super(outputFile);
	}
		
	protected DocumentProvider<String> createDocumentProvider() {
		return new TWCLDAPaperDocumentGenerator();
	}
	
	@Override
	protected Solver<String> createSolver(DocumentProvider<String> documentProvider) {
		return new SimpleTopicGrouper<String>(0, 0, documentProvider);
	}

	public static void main(String[] args) throws IOException {
		try {
			// Set System L&F
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		new SimpleTGTester(null /*new File("target/b.txt")*/).run();
	}
}
