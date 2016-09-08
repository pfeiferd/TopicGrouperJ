package org.hhn.topicgrouper.eval;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.tg.TGSolutionListener;
import org.hhn.topicgrouper.tg.TGSolver;
import org.hhn.topicgrouper.tg.report.BasicTGSolutionReporter;
import org.hhn.topicgrouper.util.OutputStreamMultiplexer;

public abstract class AbstractTGTester<T> {
	private final PrintStream printStream;

	public AbstractTGTester(File outputFile) throws IOException {
		OutputStreamMultiplexer os = new OutputStreamMultiplexer();
		os.addOutputStream(System.out);
		if (outputFile != null) {
			os.addOutputStream(new FileOutputStream(outputFile));
		}
		printStream = new PrintStream(os);
	}

	public TGSolver<T> run() {
		DocumentProvider<T> provider = createDocumentProvider();
		TGSolver<T> solver = createSolver(provider);
		TGSolutionListener<T> solutionListener = createSolutionListener(printStream);
		startSolving();
		solver.solve(solutionListener);
		done();
		return solver;
	}
	
	protected void startSolving() {		
	}
	
	protected void done() {
	}

	protected TGSolutionListener<T> createSolutionListener(PrintStream out) {
		return new BasicTGSolutionReporter<T>(out, 4, false);
	}

	protected abstract DocumentProvider<T> createDocumentProvider();

	protected abstract TGSolver<T> createSolver(DocumentProvider<T> documentProvider);
}
