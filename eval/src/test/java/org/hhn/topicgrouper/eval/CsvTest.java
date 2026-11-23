package org.hhn.topicgrouper.eval;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import junit.framework.TestCase;

/**
 * The CSV writer must keep unfinished results in a part file, rename it on
 * close and let experiments skip themselves once their file exists.
 */
public class CsvTest extends TestCase {
	private File dir;

	@Override
	protected void setUp() throws Exception {
		dir = Files.createTempDirectory("csv").toFile();
	}

	@Override
	protected void tearDown() throws Exception {
		for (File f : dir.listFiles()) {
			f.delete();
		}
		dir.delete();
	}

	public void testPartFileIsRenamedOnClose() throws IOException {
		File file = new File(dir, "result.csv");
		Csv csv = new Csv(file, "a", "b");
		csv.row(1, 2.5);
		assertFalse(file.exists());
		assertTrue(Csv.partFile(file).exists());
		assertFalse(Csv.isDone(file));
		assertFalse(Csv.skipIfDone(file));

		csv.close();
		assertTrue(file.exists());
		assertFalse(Csv.partFile(file).exists());
		assertTrue(Csv.isDone(file));
		assertTrue(Csv.skipIfDone(file));
		List<String> lines = Files.readAllLines(file.toPath());
		assertEquals(2, lines.size());
		assertEquals("a;b", lines.get(0));
		assertEquals("1;2.5", lines.get(1));
		// A second close is harmless.
		csv.close();
		assertTrue(file.exists());
	}

	public void testDiscardDeletesPartFile() {
		File file = new File(dir, "result.csv");
		Csv csv = new Csv(file, "a");
		csv.row("x");
		csv.discard();
		assertFalse(file.exists());
		assertFalse(Csv.partFile(file).exists());
		assertFalse(Csv.isDone(file));
		// Closing after discarding does not resurrect the file.
		csv.close();
		assertFalse(file.exists());
	}

	public void testCloseReplacesExistingFile() throws IOException {
		File file = new File(dir, "result.csv");
		Files.write(file.toPath(), "old".getBytes("UTF-8"));
		Csv csv = new Csv(file, "a");
		csv.row("new");
		csv.close();
		List<String> lines = Files.readAllLines(file.toPath());
		assertEquals("a", lines.get(0));
		assertEquals("new", lines.get(1));
	}
}
