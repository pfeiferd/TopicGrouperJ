package org.hhn.topicgrouper.evaluation;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

/**
 * Minimal writer for semicolon separated result files (readable by pgfplots
 * with <code>col sep=semicolon</code>, by R, pandas, spreadsheets, ...). Every row
 * is flushed immediately so partial results survive an aborted run.
 */
public final class Csv implements Closeable {
	private final PrintWriter out;

	/**
	 * @param file
	 *            the file to create (parent directories are created)
	 * @param header
	 *            the column names
	 */
	public Csv(File file, String... header) {
		try {
			if (file.getParentFile() != null) {
				file.getParentFile().mkdirs();
			}
			out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		row((Object[]) header);
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

	@Override
	public void close() {
		out.close();
	}
}
