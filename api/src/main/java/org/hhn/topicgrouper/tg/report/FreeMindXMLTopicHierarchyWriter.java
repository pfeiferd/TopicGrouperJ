package org.hhn.topicgrouper.tg.report;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import org.hhn.topicgrouper.tg.report.store.MapNode;
import org.hhn.topicgrouper.tg.report.store.WordInfo;

/**
 * Writes a topic hierarchy of MapNodes as a FreeMind mind map (XML).
 * @param <T> the word type
 */
public class FreeMindXMLTopicHierarchyWriter<T> {
	/**
	 * Whether node backgrounds are colored by word frequency.
	 */
	private final boolean withFrColor;
	/**
	 * Base value of the color channels used for node backgrounds.
	 */
	private final int alphaBase;

	/**
	 * Creates a writer with frequency coloring and the default color base (180).
	 */
	public FreeMindXMLTopicHierarchyWriter() {
		this(true);
	}

	/**
	 * Creates a writer with the default color base (180).
	 * @param withFrColor color nodes by frequency
	 */
	public FreeMindXMLTopicHierarchyWriter(boolean withFrColor) {
		this(withFrColor, 180);
	}

	/**
	 * Creates a writer.
	 * @param withFrColor color nodes by frequency
	 * @param alphaBase base value of the color channels
	 */
	public FreeMindXMLTopicHierarchyWriter(boolean withFrColor, int alphaBase) {
		this.withFrColor = withFrColor;
		this.alphaBase = alphaBase;
	}

	/**
	 * Writes the given root nodes as a complete FreeMind map.
	 * @param xmlFile stream to write to
	 * @param nodes MapNode objects to write as top-level branches
	 * @throws IOException on write failure
	 */
	public void writeToFile(OutputStream xmlFile, Object[] nodes) throws IOException {
		PrintStream pw = new PrintStream(xmlFile);
		pw.println("<map version=\"1.0.1\">");
		double maxFrequency = 0;
		int c = 0;
		for (Object node : nodes) {
			for (WordInfo<T> info : ((MapNode<T>) node).getTopTopicWordInfos()) {
				maxFrequency += info.getFrequency();
				c++;
			}
		}

		for (Object node : nodes) {
			writeNode(pw, (MapNode<T>) node, maxFrequency / c);
		}
		pw.println("</map>");
	}

	/**
	 * Recursively writes a node, its children and a likelihood note as FreeMind XML; nodes without top words are skipped.
	 * @param pw stream to write to
	 * @param node node to write
	 * @param avgMaxFrequency average top-word frequency used to scale the node color
	 */
	public void writeNode(PrintStream pw, MapNode<T> node,
			double avgMaxFrequency) {
		if (node.getTopTopicWordInfos().isEmpty()) {
			return;
		}
		pw.print("<node ");
		if (node.getId() > 0) {
			pw.print("ID=\"");
			pw.print(node.getId());
			pw.print("\" ");
		}
		pw.print("TEXT=\"");
		double avg = printNodeDescription(pw, node);

		pw.print("\"");
		pw.print(" STYLE=\"bubble\"");
		if (withFrColor) {
			pw.print(" BACKGROUND_COLOR=\"");
			pw.print(computerColorCode(avgMaxFrequency, avg));
			pw.print("\"");
		}
		pw.println(">");
//		if (node.isMarked()
//				&& (node.getLeftNode() != null && node.getRightNode() != null
//						&& !node.getLeftNode().isMarked() && !node
//						.getRightNode().isMarked())) {
//			pw.println("<icon BUILTIN=\"stop-sign\"/>");
//			// Check code
//			printNodeDescription(System.out, node);
//		}
		if (node.getLeftNode() != null) {
			writeNode(pw, node.getLeftNode(), avgMaxFrequency);
		}
		if (node.getRightNode() != null) {
			writeNode(pw, node.getRightNode(), avgMaxFrequency);
		}
		pw.print("<richcontent TYPE=\"NOTE\">");
		pw.print(node.getLikelihood());
		pw.print(", ");
		pw.print(node.getDeltaLikelihood());
		pw.println("</richcontent>");
		pw.println("</node>");
	}
	
	private double printNodeDescription(PrintStream pw, MapNode<T> node) {		
		double sumFr = 0;
		int c = 0;

		pw.print("(");
		pw.print(node.getId());
		pw.print(") ");
		
		boolean first = true;
		for (WordInfo<T> info : node.getTopTopicWordInfos()) {
			if (!first) {
				pw.print(", ");
			}
			pw.print(info.getLabel());
//			pw.print("(");
//			pw.print(info.getFrequency());
//			pw.print(")");
			first = false;
			sumFr += info.getFrequency();
			c++;
		}
		return sumFr / c;
	}

	private String computerColorCode(double avgMaxFrequency, double avgFrequency) {
		double ratio = avgFrequency / avgMaxFrequency;
		int diff = (int) ((ratio > 1 ? 1 : ratio) * (255 - alphaBase));
		int b = alphaBase + diff;
		int r = 255 - diff;
		return String.format("#%02x%02x%02x", r, alphaBase, b);
	}
}
