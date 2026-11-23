package org.hhn.topicgrouper.eval;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

/**
 * Minimal writer for semicolon separated result files (readable by pgfplots
 * with <code>col sep=semicolon</code>, by R, pandas, spreadsheets, ...).
 * <p>
 * The rows are written to <code>&lt;file&gt;.part</code> and flushed
 * immediately, so partial results survive an aborted run; {@link #close()}
 * renames the part file to the final name. A result file therefore exists
 * only if its experiment has finished, and the experiments skip themselves if
 * their file exists ({@link #skipIfDone(File)}), so an interrupted run can be
 * resumed by starting it again. Delete a result file to repeat its experiment.
 */
public final class Csv implements Closeable {
	private final File file;
	private final File part;
	private final PrintWriter out;
	private boolean closed;

	/**
	 * Creates the part file and writes the header row.
	 *
	 * @param file
	 *            the result file (parent directories are created); the rows
	 *            go to {@link #partFile(File)} until {@link #close()}
	 * @param header
	 *            the column names
	 */
	public Csv(File file, String... header) {
		this.file = file;
		this.part = partFile(file);
		try {
			if (file.getParentFile() != null) {
				file.getParentFile().mkdirs();
			}
			out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(part), "UTF-8"));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		row((Object[]) header);
	}

	/**
	 * The file a result file is written to while its experiment runs.
	 *
	 * @param file
	 *            the result file
	 * @return <code>&lt;file&gt;.part</code>
	 */
	public static File partFile(File file) {
		return new File(file.getPath() + ".part");
	}

	/**
	 * Whether the experiment writing the file has finished.
	 *
	 * @param file
	 *            the result file
	 * @return true if the file exists
	 */
	public static boolean isDone(File file) {
		return file.exists();
	}

	/**
	 * Checks {@link #isDone(File)} and prints a note if the experiment is to be
	 * skipped.
	 *
	 * @param file
	 *            the result file
	 * @return true if the file exists and the experiment is to be skipped
	 */
	public static boolean skipIfDone(File file) {
		if (isDone(file)) {
			System.out.println("Skipping " + file.getName() + ": " + file
					+ " exists (delete it to repeat the experiment)");
			return true;
		}
		return false;
	}

	/**
	 * Writes one row and echoes it to the console.
	 *
	 * @param values
	 *            the cells; doubles are written in plain notation with a dot
	 */
	public void row(Object... values) {
		StringBuilder b = new StringBuilder();
		for (int i = 0; i < values.length; i++) {
			if (i > 0) {
				b.append(';');
			}
			b.append(values[i]);
		}
		out.println(b);
		out.flush();
		System.out.println(b);
	}

	/**
	 * Finishes the file: closes it and renames the part file to the final name
	 * (an existing final file is replaced). Does nothing if the file is already
	 * closed or discarded.
	 */
	@Override
	public void close() {
		if (closed) {
			return;
		}
		closed = true;
		out.close();
		if (file.exists() && !file.delete()) {
			throw new RuntimeException("cannot replace " + file);
		}
		if (!part.renameTo(file)) {
			throw new RuntimeException("cannot rename " + part + " to " + file);
		}
	}

	/**
	 * Closes and deletes the part file, to be called when the experiment
	 * failed. Does nothing if the file is already closed or discarded.
	 */
	public void discard() {
		if (closed) {
			return;
		}
		closed = true;
		out.close();
		part.delete();
	}
}
