package org.hhn.topicgrouper.tg.report.store;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.List;

/**
 * Reads a {@link TopicHierarchy} from a file written by
 * {@link TopicHierarchyObjectWriter} (Java object serialization of the node
 * list), the original file format of the hierarchy browser.
 * {@link TopicHierarchyJsonReader} reads the JSON alternative.
 *
 * @param <T>
 *            the word type
 */
public class TopicHierarchyObjectReader<T> {
	/** Creates a reader; it has no settings. */
	public TopicHierarchyObjectReader() {
	}

	/**
	 * Reads a hierarchy.
	 *
	 * @param file
	 *            the file
	 * @return the hierarchy
	 * @throws IOException
	 *             on read failure or if the file holds something else
	 */
	@SuppressWarnings("unchecked")
	public TopicHierarchy<T> read(File file) throws IOException {
		InputStream in = new FileInputStream(file);
		try {
			ObjectInputStream oi = new ObjectInputStream(in);
			Object o = oi.readObject();
			if (!(o instanceof List)) {
				throw new IOException("not a topic hierarchy: " + file);
			}
			return new TopicHierarchy<T>((List<MapNode<T>>) o);
		} catch (ClassNotFoundException e) {
			throw new IOException(e);
		} finally {
			in.close();
		}
	}
}
