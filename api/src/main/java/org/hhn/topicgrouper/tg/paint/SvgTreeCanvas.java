package org.hhn.topicgrouper.tg.paint;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.Writer;
import java.util.Locale;

/**
 * A {@link TreeCanvas} collecting SVG elements; {@link #write(Writer, Dimension)}
 * emits the complete document once the size of the drawing is known. Text is
 * laid out with the metrics of the Java font and emitted with the matching CSS
 * font family, so the SVG looks like the Swing rendering if the font is
 * available to the viewer. Works headless.
 */
public class SvgTreeCanvas implements TreeCanvas {
	private final StringBuilder body = new StringBuilder();
	private final Graphics2D metricsGraphics;
	private boolean fontsAvailable = true;

	/** Creates an empty canvas. */
	public SvgTreeCanvas() {
		metricsGraphics = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics();
		metricsGraphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
				RenderingHints.VALUE_FRACTIONALMETRICS_ON);
		metricsGraphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
				RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
	}

	/**
	 * The metrics of the Java font; if the AWT font machinery is unavailable (a
	 * server without font libraries) the metrics are approximated from the font
	 * size and the layout is still usable.
	 */
	@Override
	public FontMetrics getFontMetrics(Font font) {
		if (fontsAvailable) {
			try {
				return metricsGraphics.getFontMetrics(font);
			} catch (RuntimeException e) {
				fontsAvailable = false;
			} catch (Error e) {
				fontsAvailable = false;
			}
		}
		return new ApproximateFontMetrics(font);
	}

	/**
	 * Whether the metrics come from the real fonts (false once the AWT font
	 * machinery failed and the approximation is used).
	 *
	 * @return true if the fonts are available
	 */
	public boolean isFontsAvailable() {
		return fontsAvailable;
	}

	/** Metrics estimated from the font size for environments without fonts. */
	@SuppressWarnings("serial")
	protected static class ApproximateFontMetrics extends FontMetrics {
		/**
		 * Creates the metrics.
		 *
		 * @param font
		 *            the font
		 */
		protected ApproximateFontMetrics(Font font) {
			super(font);
		}

		@Override
		public int getAscent() {
			return (int) Math.ceil(getFont().getSize2D() * 0.93);
		}

		@Override
		public int getDescent() {
			return (int) Math.ceil(getFont().getSize2D() * 0.24);
		}

		@Override
		public int getLeading() {
			return 0;
		}

		@Override
		public int getMaxAdvance() {
			return (int) Math.ceil(getFont().getSize2D());
		}

		@Override
		public int charWidth(char ch) {
			double size = getFont().getSize2D();
			double w;
			if (ch == ' ' || ch == 'i' || ch == 'l' || ch == 'j' || ch == '.' || ch == ',' || ch == '\'') {
				w = 0.28;
			} else if (Character.isUpperCase(ch) || ch == 'm' || ch == 'w') {
				w = 0.72;
			} else if (Character.isDigit(ch)) {
				w = 0.56;
			} else {
				w = 0.55;
			}
			if (getFont().isBold()) {
				w *= 1.08;
			}
			return (int) Math.ceil(w * size);
		}

		@Override
		public int stringWidth(String str) {
			int sum = 0;
			for (int i = 0; i < str.length(); i++) {
				sum += charWidth(str.charAt(i));
			}
			return sum;
		}
	}

	@Override
	public void drawLine(int x1, int y1, int x2, int y2, Color color, float width, boolean dashed) {
		body.append("<line x1=\"").append(x1).append("\" y1=\"").append(y1).append("\" x2=\"").append(x2)
				.append("\" y2=\"").append(y2).append("\" stroke=\"").append(rgb(color))
				.append("\" stroke-width=\"").append(number(width)).append('"');
		if (dashed) {
			body.append(" stroke-dasharray=\"").append(number(4 * width)).append(',').append(number(4 * width))
					.append('"');
		}
		body.append("/>\n");
	}

	@Override
	public void fillRect(int x, int y, int width, int height, Color color) {
		body.append("<rect x=\"").append(x).append("\" y=\"").append(y).append("\" width=\"").append(width)
				.append("\" height=\"").append(height).append("\" fill=\"").append(rgb(color)).append("\"/>\n");
	}

	@Override
	public void drawText(int x, int y, String text, Font font, Color color) {
		body.append("<text x=\"").append(x).append("\" y=\"").append(y).append("\" font-family=\"")
				.append(escape(cssFamily(font))).append("\" font-size=\"").append(font.getSize()).append('"');
		if (font.isBold()) {
			body.append(" font-weight=\"bold\"");
		}
		if (font.isItalic()) {
			body.append(" font-style=\"italic\"");
		}
		body.append(" fill=\"").append(rgb(color)).append("\" xml:space=\"preserve\">").append(escape(text))
				.append("</text>\n");
	}

	/**
	 * Writes the SVG document.
	 *
	 * @param out
	 *            the writer (not closed)
	 * @param size
	 *            the size of the drawing, as returned by
	 *            {@link TopicTreePainter#paintTree(TreeCanvas)}
	 * @throws IOException
	 *             on write failure
	 */
	public void write(Writer out, Dimension size) throws IOException {
		out.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
		out.write("<svg xmlns=\"http://www.w3.org/2000/svg\" version=\"1.1\" width=\"" + size.width
				+ "\" height=\"" + size.height + "\" viewBox=\"0 0 " + size.width + " " + size.height + "\">\n");
		out.write(body.toString());
		out.write("</svg>\n");
	}

	/**
	 * The SVG elements collected so far (without the document frame).
	 *
	 * @return the SVG body
	 */
	public String getBody() {
		return body.toString();
	}

	/**
	 * Maps a Java font to a CSS font family; the logical fonts become the generic
	 * families.
	 *
	 * @param font
	 *            the font
	 * @return the family name for the <code>font-family</code> attribute
	 */
	protected String cssFamily(Font font) {
		String family = font.getFamily(Locale.ENGLISH);
		if (Font.SANS_SERIF.equalsIgnoreCase(family) || Font.DIALOG.equalsIgnoreCase(family)
				|| Font.DIALOG_INPUT.equalsIgnoreCase(family)) {
			return "sans-serif";
		}
		if (Font.SERIF.equalsIgnoreCase(family)) {
			return "serif";
		}
		if (Font.MONOSPACED.equalsIgnoreCase(family)) {
			return "monospace";
		}
		return family + ", sans-serif";
	}

	private static String rgb(Color c) {
		return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
	}

	private static String number(float f) {
		return f == Math.rint(f) ? String.valueOf((int) f) : String.format(Locale.US, "%.2f", f);
	}

	private static String escape(String s) {
		StringBuilder sb = new StringBuilder(s.length());
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			switch (c) {
			case '&':
				sb.append("&amp;");
				break;
			case '<':
				sb.append("&lt;");
				break;
			case '>':
				sb.append("&gt;");
				break;
			case '"':
				sb.append("&quot;");
				break;
			default:
				if (c < 0x20 && c != '\t') {
					sb.append(' ');
				} else {
					sb.append(c);
				}
			}
		}
		return sb.toString();
	}
}
