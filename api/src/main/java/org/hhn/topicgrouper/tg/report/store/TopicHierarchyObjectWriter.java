package org.hhn.topicgrouper.tg.report.store;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;

/**
 * Writes a {@link TopicHierarchy} with Java object serialization (the node list
 * in creation order), the original file format of the hierarchy browser. Note
 * that object serialization recurses through the parent and child links, so
 * very deep hierarchies may exhaust the stack; {@link TopicHierarchyJsonWriter}
 * has no such limit.
 *
 * @param <T>
 *            the word type (must be serializable)
 */
public class TopicHierarchyObjectWriter<T> {
	/** Creates a writer; it has no settings. */
	public TopicHierarchyObjectWriter() {
	}

	/**
	 * Writes a hierarchy.
	 *
	 * @param hierarchy
	 *            the hierarchy
	 * @param file
	 *            the file
	 * @throws IOException
	 *             on write failure
	 */
	public void write(TopicHierarchy<T> hierarchy, File file) throws IOException {
		OutputStream out = new FileOutputStream(file);
		try {
			ObjectOutputStream oo = new ObjectOutputStream(out);
			oo.writeObject(new ArrayList<MapNode<T>>(hierarchy.getNodes()));
			oo.flush();
		} finally {
			out.close();
		}
	}
}
