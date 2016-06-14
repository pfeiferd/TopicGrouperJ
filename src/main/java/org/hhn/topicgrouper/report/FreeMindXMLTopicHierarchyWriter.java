package org.hhn.topicgrouper.report;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;

import org.hhn.topicgrouper.report.MindMapSolutionReporter.MapNode;
import org.hhn.topicgrouper.report.MindMapSolutionReporter.WordInfo;

public class FreeMindXMLTopicHierarchyWriter<T> {
	private final boolean withFrColor;
	private final int alphaBase;

	public FreeMindXMLTopicHierarchyWriter() {
		this(true);
	}

	public FreeMindXMLTopicHierarchyWriter(boolean withFrColor) {
		this(withFrColor, 180);
	}

	public FreeMindXMLTopicHierarchyWriter(boolean withFrColor, int alphaBase) {
		this.withFrColor = withFrColor;
		this.alphaBase = alphaBase;
	}

	public void writeToFile(Writer xmlFile, Object[] nodes) throws IOException {
		PrintWriter pw = new PrintWriter(xmlFile);
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

	public void writeNode(PrintWriter pw, MapNode<T> node,
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
		boolean first = true;
		double sumFr = 0;
		int c = 0;
		for (WordInfo<T> info : node.getTopTopicWordInfos()) {
			if (!first) {
				pw.print(", ");
			}
			T word = info.getWord();
			if (word == null) {
				pw.print(info.getWordId());
			} else {
				pw.print(word);
			}
			pw.print("(");
			pw.print(info.getFrequency());
			pw.print(")");
			first = false;
			sumFr += info.getFrequency();
			c++;
		}

		pw.print("\"");
		pw.print(" STYLE=\"bubble\"");
		if (withFrColor) {
			pw.print(" BACKGROUND_COLOR=\"");
			pw.print(computerColorCode(avgMaxFrequency, sumFr / c));
			pw.print("\"");
		}
		pw.println(">");
		if (node.isMarked()) {
			pw.println("<icon BUILTIN=\"flag-yellow\"/>");
		}
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

	private String computerColorCode(double avgMaxFrequency, double avgFrequency) {
		double ratio = avgFrequency / avgMaxFrequency;
		int diff = (int) ((ratio > 1 ? 1 : ratio) * (255 - alphaBase));
		int b = alphaBase + diff;
		int r = 255 - diff;
		return String.format("#%02x%02x%02x", r, alphaBase, b);
	}
}
