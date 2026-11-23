package org.hhn.topicgrouper.tg.viz;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import org.hhn.topicgrouper.tg.paint.DefaultTopicTreePainter;
import org.hhn.topicgrouper.tg.report.TopTopicSearcher;
import org.hhn.topicgrouper.tg.report.store.MapNode;
import org.hhn.topicgrouper.tg.report.store.TopicHierarchy;
import org.hhn.topicgrouper.tg.report.store.TopicHierarchyJsonReader;
import org.hhn.topicgrouper.tg.report.store.TopicHierarchyJsonWriter;
import org.hhn.topicgrouper.tg.report.store.TopicHierarchyObjectReader;
import org.hhn.topicgrouper.tg.report.store.WordInfo;

import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.util.UIScale;

/**
 * Swing application for browsing a recorded topic hierarchy as a tree and a
 * filterable table of topics. It reads the serialized node lists
 * ({@link TopicHierarchyObjectReader}) and the JSON files
 * ({@link TopicHierarchyJsonReader}), converts the former into the latter and
 * exports the hierarchy or a selected subtree as SVG
 * ({@link DefaultTopicTreePainter}). The look and feel is FlatLaf, which also
 * handles HiDPI scaling (<code>-hidpi</code> doubles the scale).
 *
 * @param <T>
 *            the word type
 */
public class HierarchyBrowser<T> {
	/**
	 * The ways the topic table can be filtered: by topic size, all, number of
	 * topics in history (set number), hierarchy level (set level) and delta
	 * ratio (set window size, set ratio level, set min history number).
	 */
	protected enum TableFilters {
		/** No filter, all topics. */
		ALL,
		/** Topics with at least a given size. */
		TOPIC_SIZE,
		/** The topics of the solution with a given number of topics. */
		HISTORY_NUMBER,
		/** The topics at a given level of the hierarchy. */
		HIERARCHY_LEVEL,
		/** Topics whose likelihood improvement ratio within a window exceeds a level. */
		DELTA_RATIO;
	};

	private final JFrame frame;
	private final JTree tree;
	private final TreeModel treeModel;

	private final JTable table;
	private final TGTableModel tableModel;

	private TopicHierarchy<T> hierarchy;
	/**
	 * Indices into the hierarchy's nodes of the topics currently shown in the table.
	 */
	private final TIntList nodeIndices;
	/**
	 * Base value of the color channels used for frequency coloring.
	 */
	/** Colors of the "Color by Topic Frequencies" option, created per hierarchy. */
	private FrequencyColorScale colorScale;

	/**
	 * Whether sizes are doubled for high-DPI displays.
	 */
	private final boolean highDPI;

	private boolean inSelection = false;
	/**
	 * Window size for the log delta change ratio.
	 */
	private int logDeltaChangeWindow = 5;
	private final JCheckBox showFrCheckBox;
	private final JCheckBox showSenseCheckBox;
	/** The words that have senses in the current hierarchy (their types get a sense id in the labels). */
	private final java.util.Set<T> wordsWithSenses = new java.util.HashSet<T>();
	private final JCheckBox frColorCheckBox;
	private final Color origTreeBackground;
	private final JComboBox<TableFilters> tableFilterComboBox;
	private final JSpinner minTopicSizeSpinner;
	private final JSpinner historySpinner;
	private final JSpinner hLevelSpinner;
	private final JPanel settingsPanel;
	private final JPanel filterPanel;
	private final CardLayout filterCards;
	private final JPanel topicSizePanel;
	private final JPanel historyPanel;
	private final JPanel allTopicsPanel;
	private final JPanel hierarchyLevelPanel;
	private final JPanel ratioLevelPanel;
	private final JFormattedTextField ratioField;
	private final JSpinner minTopicIdSpinner;

	private final SpinnerNumberModel spinnerModel2;
	private final SpinnerNumberModel spinnerModel3;
	private final SpinnerNumberModel spinnerModel4;

	private final TopTopicSearcher<T> topicSearcher;
	private File lastDirectory = new File(".");
	/**
	 * Per topic node: its words beyond the top words, most frequent first
	 * (computed once per node, the table asks for them for every cell).
	 */
	private final Map<MapNode<T>, List<WordInfo<T>>> extraWords = new IdentityHashMap<MapNode<T>, List<WordInfo<T>>>();

	/**
	 * Creates and shows the browser window.
	 *
	 * @param highDPI
	 *            kept for compatibility; scaling is done by FlatLaf (see
	 *            {@link #main(String[])}), this flag only doubles the row heights
	 */
	@SuppressWarnings("serial")
	public HierarchyBrowser(boolean highDPI) {
		this.highDPI = highDPI;
		topicSearcher = new TopTopicSearcher<T>();
		frame = new JFrame("TG Result Browser");
		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				System.exit(0);
			}
		});
		frame.setLayout(new BorderLayout());
		frame.setJMenuBar(createMenu());

		JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		frame.add(splitPane, BorderLayout.CENTER);

		tree = new JTree();
		if (highDPI) {
			tree.setRowHeight(2 * tree.getRowHeight());
		}
		treeModel = createTreeModel();
		tree.setModel(treeModel);
		tree.getSelectionModel().addTreeSelectionListener(new TreeSelectionListener() {
			@Override
			public void valueChanged(TreeSelectionEvent e) {
				if (!inSelection) {
					try {
						inSelection = true;
						MapNode<T> node = getSelectedTreeNode();
						if (node != null) {
							int index = hierarchy.indexOf(node);
							int res = -1;
							for (int i = 0; i < nodeIndices.size(); i++) {
								if (index == nodeIndices.get(i)) {
									res = i;
									break;
								}
							}
							if (res != -1) {
								table.getSelectionModel().clearSelection();
								int viewIndex = table.convertRowIndexToView(res);
								table.getSelectionModel().setSelectionInterval(viewIndex, viewIndex);
								table.scrollRectToVisible(new Rectangle(table.getCellRect(viewIndex, 0, true)));
							}
						}
					} finally {
						inSelection = false;
					}
				}
			}
		});
		DefaultTreeCellRenderer treeCellRenderer = new DefaultTreeCellRenderer() {
			private Font plain;
			private Font italic;

			public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded,
					boolean leaf, int row, boolean hasFocus) {
				Component res = super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
				if (plain == null || !plain.equals(tree.getFont())) {
					plain = tree.getFont();
					italic = plain.deriveFont(Font.ITALIC);
				}
				if (value instanceof MapNode) {
					@SuppressWarnings("unchecked")
					MapNode<T> node = (MapNode<T>) value;
					setFont(node.isSenseLeaf() ? italic : plain);
					setText(nodeText(node));
					if (frColorCheckBox.isSelected() && colorScale != null) {
						setBackgroundNonSelectionColor(colorScale.colorOf(node.getTopicFrequency()));
					} else {
						setBackgroundNonSelectionColor(origTreeBackground);
					}
				} else {
					setFont(plain);
					setBackgroundNonSelectionColor(origTreeBackground);
				}
				return res;
			};
		};
		origTreeBackground = treeCellRenderer.getBackground();
		tree.setCellRenderer(treeCellRenderer);

		JScrollPane scrollPane1 = new JScrollPane();
		scrollPane1.setViewportView(tree);
		scrollPane1.setPreferredSize(new Dimension(getPixel(600), getPixel(250)));

		splitPane.setTopComponent(scrollPane1);

		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());

		splitPane.setBottomComponent(panel);

		settingsPanel = new JPanel();
		BoxLayout boxLayout = new BoxLayout(settingsPanel, BoxLayout.X_AXIS);
		settingsPanel.setLayout(boxLayout);
		int bPixel = getPixel(4);
		settingsPanel.setBorder(BorderFactory.createEmptyBorder(bPixel, bPixel, bPixel, bPixel));

		panel.add(settingsPanel, BorderLayout.NORTH);

		tableFilterComboBox = new JComboBox<TableFilters>();

		tableFilterComboBox.setModel(new DefaultComboBoxModel<TableFilters>(TableFilters.values()));
		tableFilterComboBox.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				adjustNodeIndices();
			}
		});
		tableFilterComboBox.setMaximumSize(tableFilterComboBox.getPreferredSize());
		settingsPanel.add(tableFilterComboBox);
		settingsPanel.add(Box.createHorizontalStrut(getPixel(4)));

		filterCards = new CardLayout();
		filterPanel = new JPanel(filterCards);
		settingsPanel.add(filterPanel);
		settingsPanel.add(Box.createHorizontalGlue());

		allTopicsPanel = new JPanel();
		filterPanel.add(allTopicsPanel, TableFilters.ALL.name());

		topicSizePanel = new JPanel();
		BoxLayout topicSizePanelLayout = new BoxLayout(topicSizePanel, BoxLayout.X_AXIS);
		topicSizePanel.setLayout(topicSizePanelLayout);

		minTopicSizeSpinner = new JSpinner();
		SpinnerNumberModel spinnerModel = new SpinnerNumberModel();
		spinnerModel.setMinimum(0);
		spinnerModel.setStepSize(1);
		minTopicSizeSpinner.setModel(spinnerModel);
		topicSizePanel.add(new JLabel("Min. Topic Size:"));
		topicSizePanel.add(Box.createHorizontalStrut(getPixel(2)));
		topicSizePanel.add(minTopicSizeSpinner);
		filterPanel.add(topicSizePanel, TableFilters.TOPIC_SIZE.name());
		spinnerModel.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				adjustNodeIndices();
			}
		});

		historyPanel = new JPanel();
		BoxLayout historyPanelLayout = new BoxLayout(historyPanel, BoxLayout.X_AXIS);
		historyPanel.setLayout(historyPanelLayout);

		historySpinner = new JSpinner();
		spinnerModel2 = new SpinnerNumberModel();
		spinnerModel2.setMinimum(1);
		spinnerModel2.setMaximum(1);
		spinnerModel2.setStepSize(1);
		spinnerModel2.setValue(1);
		historySpinner.setModel(spinnerModel2);
		historyPanel.add(new JLabel("Number of Topics:"));
		historyPanel.add(Box.createHorizontalStrut(getPixel(2)));
		historyPanel.add(historySpinner);
		filterPanel.add(historyPanel, TableFilters.HISTORY_NUMBER.name());
		spinnerModel2.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				adjustNodeIndices();
			}
		});

		hierarchyLevelPanel = new JPanel();
		BoxLayout hierarchyLevelPanelLayout = new BoxLayout(hierarchyLevelPanel, BoxLayout.X_AXIS);
		hierarchyLevelPanel.setLayout(hierarchyLevelPanelLayout);

		hLevelSpinner = new JSpinner();
		spinnerModel3 = new SpinnerNumberModel();
		spinnerModel3.setMinimum(1);
		spinnerModel3.setMaximum(1);
		spinnerModel3.setStepSize(1);
		spinnerModel3.setValue(1);
		hLevelSpinner.setModel(spinnerModel3);
		hierarchyLevelPanel.add(new JLabel("Hierarchy Level:"));
		hierarchyLevelPanel.add(Box.createHorizontalStrut(getPixel(2)));
		hierarchyLevelPanel.add(hLevelSpinner);
		filterPanel.add(hierarchyLevelPanel, TableFilters.HIERARCHY_LEVEL.name());
		spinnerModel3.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				adjustNodeIndices();
			}
		});

		ratioLevelPanel = new JPanel();
		BoxLayout rationLevelPanelLayout = new BoxLayout(ratioLevelPanel, BoxLayout.X_AXIS);
		ratioLevelPanel.setLayout(rationLevelPanelLayout);

		NumberFormat ratioFormat = NumberFormat.getNumberInstance(Locale.ENGLISH);
		ratioFormat.setMinimumFractionDigits(6);
		ratioField = new JFormattedTextField(ratioFormat) {
			public void commitEdit() throws java.text.ParseException {
				super.commitEdit();
				adjustNodeIndices();
			}
		};

		ratioField.setValue(1d);
		ratioField.setColumns(10);
		Dimension d = ratioField.getPreferredSize();
		ratioField.setMaximumSize(d);

		ratioLevelPanel.add(new JLabel("LD Change Level:"));
		ratioLevelPanel.add(Box.createHorizontalStrut(getPixel(2)));
		ratioLevelPanel.add(ratioField);

		minTopicIdSpinner = new JSpinner();
		spinnerModel4 = new SpinnerNumberModel();
		spinnerModel4.setMinimum(1);
		spinnerModel4.setMaximum(1);
		spinnerModel4.setStepSize(1);
		spinnerModel4.setValue(1);
		spinnerModel4.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				adjustNodeIndices();
			}
		});

		minTopicIdSpinner.setModel(spinnerModel4);
		ratioLevelPanel.add(Box.createHorizontalStrut(getPixel(4)));
		ratioLevelPanel.add(new JLabel("Max. Topic ID:"));
		ratioLevelPanel.add(Box.createHorizontalStrut(getPixel(2)));
		ratioLevelPanel.add(minTopicIdSpinner);
		filterPanel.add(ratioLevelPanel, TableFilters.DELTA_RATIO.name());
		for (JSpinner sp : new JSpinner[] { minTopicSizeSpinner, historySpinner, hLevelSpinner, minTopicIdSpinner }) {
			Dimension ps = sp.getPreferredSize();
			ps.width = Math.max(ps.width, getPixel(90));
			sp.setPreferredSize(ps);
			sp.setMaximumSize(ps);
		}

		JScrollPane scrollPane = new JScrollPane();
		table = new JTable() {
			@Override
			public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
				Component c = super.prepareRenderer(renderer, row, column);
				if (!isRowSelected(row)) {
					if (frColorCheckBox.isSelected() && colorScale != null && hierarchy != null) {
						MapNode<T> node = hierarchy.get(nodeIndices.get(convertRowIndexToModel(row)));
						c.setBackground(colorScale.colorOf(node.getTopicFrequency()));
					} else {
						c.setBackground(getBackground());
					}
				}
				return c;
			}
		};
		table.setAutoCreateRowSorter(true);
		table.setAutoCreateColumnsFromModel(true);
		if (highDPI) {
			table.setRowHeight(2 * table.getRowHeight());
		}
		table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				if (!inSelection) {
					try {
						inSelection = true;
						if (!e.getValueIsAdjusting()) {
							int tableRow = table.getSelectedRow();
							if (tableRow != -1) {
								int row = table.convertRowIndexToModel(tableRow);
								MapNode<T> node = hierarchy.get(nodeIndices.get(row));
								TreePath path = createPath(node);
								tree.getSelectionModel().setSelectionPath(path);
								tree.scrollPathToVisible(path);
							}
						}
					} finally {
						inSelection = false;
					}
				}
			}
		});
		nodeIndices = new TIntArrayList();
		tableModel = new TGTableModel();
		table.setModel(tableModel);

		scrollPane.setViewportView(table);
		scrollPane.setPreferredSize(new Dimension(getPixel(600), getPixel(250)));

		panel.add(scrollPane, BorderLayout.CENTER);

		JPanel viewPanel = new JPanel();
		BoxLayout viewBoxLayout = new BoxLayout(viewPanel, BoxLayout.X_AXIS);
		viewPanel.setLayout(viewBoxLayout);
		viewPanel.setBorder(BorderFactory.createEmptyBorder(bPixel, bPixel, bPixel, bPixel));

		showFrCheckBox = new JCheckBox("Show Frequencies");
		showFrCheckBox.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				tableModel.setWithFrequencies(showFrCheckBox.isSelected());
				tableChanged();
			}
		});
		viewPanel.add(showFrCheckBox);

		frColorCheckBox = new JCheckBox("Color by Topic Frequencies");
		frColorCheckBox.setToolTipText("Background from light blue (rare) to light red (the whole corpus), logarithmic in the topic frequency");
		frColorCheckBox.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				table.repaint();
				tree.repaint();
			}
		});
		viewPanel.add(frColorCheckBox);

		showSenseCheckBox = new JCheckBox("Show Sense Ids", true);
		showSenseCheckBox.setToolTipText("Label the types of a word that has senses: word'0 is the remainder, word'1, word'2, ... the senses");
		showSenseCheckBox.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				table.repaint();
				tree.repaint();
			}
		});
		viewPanel.add(showSenseCheckBox);

		panel.add(viewPanel, BorderLayout.SOUTH);

		frame.pack();
		frame.setVisible(true);
	}

	/**
	 * The frame of the browser.
	 *
	 * @return the frame
	 */
	public JFrame getFrame() {
		return frame;
	}

	/**
	 * The hierarchy shown.
	 *
	 * @return the hierarchy, <code>null</code> if none was loaded yet
	 */
	public TopicHierarchy<T> getHierarchy() {
		return hierarchy;
	}

	/**
	 * Shows a hierarchy.
	 *
	 * @param hierarchy
	 *            the hierarchy
	 */
	public void setHierarchy(TopicHierarchy<T> hierarchy) {
		this.hierarchy = hierarchy;
		extraWords.clear();
		wordsWithSenses.clear();
		for (MapNode<T> node : hierarchy.getNodes()) {
			if (node.isSenseLeaf()) {
				wordsWithSenses.add(node.getWordInfo().getWord());
			}
		}
		int maxFrequency = 0;
		for (MapNode<T> root : hierarchy.getRoots()) {
			maxFrequency += root.getTopicFrequency();
		}
		colorScale = new FrequencyColorScale(maxFrequency);
		tree.setModel(null);
		tree.setModel(treeModel);
		expandUpperLevels(3);
		tableModel.setWithFrequencies(showFrCheckBox.isSelected());
		tableModel.setMaxWords(10);
		spinnerModel2.setMaximum(Math.max(1, hierarchy.getMaxTopics()));
		spinnerModel2.setValue(1);
		spinnerModel3.setMaximum(Math.max(1, hierarchy.size()));
		spinnerModel3.setValue(1);
		spinnerModel4.setMaximum(Math.max(1, hierarchy.size()));
		spinnerModel4.setValue(1);
		adjustNodeIndices();
		tableChanged();
	}

	/**
	 * The label of a word type: the word, and if the word has senses and the
	 * option is on, the sense id (<code>word'0</code> for the remainder,
	 * <code>word'1</code>, ... for the senses).
	 *
	 * @param info
	 *            the word type
	 * @return the label
	 */
	protected String label(WordInfo<T> info) {
		String word = info.getWord() == null ? String.valueOf(info.getWordId()) : info.getWord().toString();
		if (showSenseCheckBox.isSelected() && wordsWithSenses.contains(info.getWord())) {
			return word + "'" + info.getSense();
		}
		return word;
	}

	/**
	 * The text of a tree node: the labelled word of a leaf, or the id and the
	 * top words of a topic, each with its frequency.
	 *
	 * @param node
	 *            the node
	 * @return the text
	 */
	protected String nodeText(MapNode<T> node) {
		StringBuilder sb = new StringBuilder();
		if (!node.isLeaf()) {
			sb.append(node.getId()).append(": ");
		}
		boolean first = true;
		for (WordInfo<T> info : node.getTopTopicWordInfos()) {
			if (!first) {
				sb.append(", ");
			}
			first = false;
			sb.append(label(info)).append(" (").append(info.getFrequency()).append(')');
		}
		if (first) {
			sb.append(node.getId());
		}
		return sb.toString();
	}

	/**
	 * Expands the tree down to the given depth (expanding all rows of a
	 * hierarchy with tens of thousands of nodes is slow and useless).
	 *
	 * @param depth
	 *            the number of levels below the root to expand
	 */
	protected void expandUpperLevels(int depth) {
		Object root = treeModel.getRoot();
		if (root == null) {
			return;
		}
		List<TreePath> level = new ArrayList<TreePath>();
		level.add(new TreePath(root));
		for (int d = 0; d < depth && !level.isEmpty(); d++) {
			List<TreePath> next = new ArrayList<TreePath>();
			for (TreePath path : level) {
				Object node = path.getLastPathComponent();
				if (!treeModel.isLeaf(node)) {
					tree.expandPath(path);
					for (int i = 0; i < treeModel.getChildCount(node); i++) {
						next.add(path.pathByAddingChild(treeModel.getChild(node, i)));
					}
				}
			}
			level = next;
		}
	}

	/**
	 * Scales a pixel size for the display (FlatLaf's user scale factor).
	 *
	 * @param pixel
	 *            size in pixels
	 * @return the scaled size
	 */
	protected int getPixel(int pixel) {
		return UIScale.scale(pixel);
	}

	/**
	 * Notifies the table that its structure and data changed.
	 */
	protected void tableChanged() {
		table.tableChanged(new TableModelEvent(tableModel, TableModelEvent.HEADER_ROW));
	}

	/**
	 * The node selected in the tree.
	 *
	 * @return the node, <code>null</code> if none or the virtual root is selected
	 */
	@SuppressWarnings("unchecked")
	protected MapNode<T> getSelectedTreeNode() {
		TreePath path = tree.getSelectionPath();
		if (path != null && path.getLastPathComponent() instanceof MapNode) {
			return (MapNode<T>) path.getLastPathComponent();
		}
		return null;
	}

	/**
	 * The tree path of a node.
	 *
	 * @param node
	 *            the node
	 * @return the path from the tree's root
	 */
	protected TreePath createPath(MapNode<T> node) {
		List<Object> pathList = new ArrayList<Object>();
		while (node != null) {
			pathList.add(0, node);
			node = node.getParent();
		}
		if (treeModel.getRoot() != pathList.get(0)) {
			pathList.add(0, treeModel.getRoot());
		}
		return new TreePath(pathList.toArray());
	}

	/**
	 * Creates the menu bar.
	 *
	 * @return the menu bar
	 */
	protected JMenuBar createMenu() {
		JMenuBar menuBar = new JMenuBar();
		JMenu menu = new JMenu("File");
		JMenuItem openFileItem = new JMenuItem("Open TG File...");
		openFileItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				File file = chooseFile("Open TG File", false, new FileNameExtensionFilter("TG files (*.tg)", "tg"));
				if (file != null) {
					try {
						setHierarchy(new TopicHierarchyObjectReader<T>().read(file));
					} catch (IOException ex) {
						showError("Cannot read " + file, ex);
					}
				}
			}
		});
		menu.add(openFileItem);

		JMenuItem openJsonItem = new JMenuItem("Open JSON File...");
		openJsonItem.addActionListener(new ActionListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void actionPerformed(ActionEvent e) {
				File file = chooseFile("Open JSON File", false, new FileNameExtensionFilter(
						"JSON files (*.json, *.json.gz)", "json", "gz"));
				if (file != null) {
					try {
						setHierarchy((TopicHierarchy<T>) new TopicHierarchyJsonReader().read(file));
					} catch (IOException ex) {
						showError("Cannot read " + file, ex);
					}
				}
			}
		});
		menu.add(openJsonItem);

		JMenuItem saveJsonItem = new JMenuItem("Save as JSON...");
		saveJsonItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (hierarchy == null) {
					return;
				}
				File file = chooseFile("Save JSON File", true, new FileNameExtensionFilter(
						"JSON files (*.json, *.json.gz)", "json", "gz"));
				if (file != null) {
					try {
						new TopicHierarchyJsonWriter<T>().write(hierarchy, file);
					} catch (IOException ex) {
						showError("Cannot write " + file, ex);
					}
				}
			}
		});
		menu.add(saveJsonItem);

		JMenuItem exportSvgItem = new JMenuItem("Export SVG...");
		exportSvgItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (hierarchy != null) {
					exportSvg();
				}
			}
		});
		menu.add(exportSvgItem);

		menu.addSeparator();
		JMenuItem exitItem = new JMenuItem("Exit");
		exitItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				System.exit(0);
			}
		});
		menu.add(exitItem);

		menuBar.add(menu);

		return menuBar;
	}

	/**
	 * Asks for the SVG parameters and writes the selected subtree (or the whole
	 * hierarchy) as SVG.
	 */
	protected void exportSvg() {
		MapNode<T> selected = getSelectedTreeNode();
		JPanel panel = new JPanel(new GridLayout(0, 2, getPixel(4), getPixel(4)));
		SpinnerNumberModel topicsModel = new SpinnerNumberModel(0, 0, Math.max(1, hierarchy.getMaxTopics()), 1);
		SpinnerNumberModel minSizeModel = new SpinnerNumberModel(1, 1, Integer.MAX_VALUE, 1);
		SpinnerNumberModel topWordsModel = new SpinnerNumberModel(5, 1, 100, 1);
		SpinnerNumberModel fontSizeModel = new SpinnerNumberModel(12, 4, 72, 1);
		JCheckBox costBox = new JCheckBox("Show join cost", false);
		JCheckBox indentBox = new JCheckBox("Indent by join cost", false);
		JCheckBox subtreeBox = new JCheckBox("Selected subtree only", selected != null);
		subtreeBox.setEnabled(selected != null);
		panel.add(new JLabel("Number of topics (0 = all):"));
		panel.add(new JSpinner(topicsModel));
		panel.add(new JLabel("Min. topic size:"));
		panel.add(new JSpinner(minSizeModel));
		panel.add(new JLabel("Top words per topic:"));
		panel.add(new JSpinner(topWordsModel));
		panel.add(new JLabel("Font size:"));
		panel.add(new JSpinner(fontSizeModel));
		panel.add(costBox);
		panel.add(indentBox);
		panel.add(subtreeBox);
		int res = JOptionPane.showConfirmDialog(frame, panel, "Export SVG", JOptionPane.OK_CANCEL_OPTION,
				JOptionPane.PLAIN_MESSAGE);
		if (res != JOptionPane.OK_OPTION) {
			return;
		}
		File file = chooseFile("Export SVG", true, new FileNameExtensionFilter("SVG files (*.svg)", "svg"));
		if (file == null) {
			return;
		}
		List<MapNode<T>> roots = subtreeBox.isSelected() && selected != null ? Collections.singletonList(selected)
				: hierarchy.getRoots();
		DefaultTopicTreePainter<T> painter = new DefaultTopicTreePainter<T>(hierarchy, roots);
		painter.setTopics(topicsModel.getNumber().intValue()).setMinTopicSize(minSizeModel.getNumber().intValue())
				.setTopWords(topWordsModel.getNumber().intValue())
				.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, fontSizeModel.getNumber().intValue()))
				.setLabelParts(true, true, costBox.isSelected(), false)
				.setNodeIndent(indentBox.isSelected() ? 3 : 0, true);
		try {
			painter.writeSvg(file);
		} catch (IOException ex) {
			showError("Cannot write " + file, ex);
		}
	}

	/**
	 * Shows an error dialog.
	 *
	 * @param message
	 *            the message
	 * @param ex
	 *            the cause
	 */
	protected void showError(String message, Exception ex) {
		JOptionPane.showMessageDialog(frame, message + ":\n" + ex, "Error", JOptionPane.ERROR_MESSAGE);
	}

	/**
	 * Applies the currently selected table filter.
	 */
	protected void adjustNodeIndices() {
		adjustNodeIndices((TableFilters) tableFilterComboBox.getSelectedItem());
	}

	/**
	 * Applies the given filter, shows its settings panel and refreshes the table.
	 *
	 * @param filter
	 *            the filter to apply
	 */
	protected void adjustNodeIndices(TableFilters filter) {
		filterCards.show(filterPanel, filter.name());
		switch (filter) {
		case ALL:
			adjustNodeIndicesByAll();
			break;
		case TOPIC_SIZE:
			adjustNodeIndicesByTopicSize((Integer) minTopicSizeSpinner.getValue());
			break;
		case HISTORY_NUMBER:
			adjustNodeIndicesByHistory((Integer) spinnerModel2.getValue());
			break;
		case HIERARCHY_LEVEL:
			adjustNodeIndicesByHierarchyLevel((Integer) spinnerModel3.getValue());
			break;
		case DELTA_RATIO:
			Number number = (Number) ratioField.getValue();
			adjustByRatioLevel(number.doubleValue(), (Integer) spinnerModel4.getValue());
			break;
		default:
			throw new IllegalStateException();
		}
		settingsPanel.revalidate();
		settingsPanel.repaint();
		tableChanged();
	}

	/**
	 * Selects topics whose log delta change reaches the ratio level, together with their children.
	 *
	 * @param ratioLevel
	 *            minimum delta change ratio
	 * @param maxTopic
	 *            number of last history steps to examine
	 */
	protected void adjustByRatioLevel(double ratioLevel, int maxTopic) {
		nodeIndices.clear();
		if (hierarchy != null) {
			BitSet seen = new BitSet(hierarchy.size());
			for (int i = Math.max(0, hierarchy.size() - maxTopic); i < hierarchy.size(); i++) {
				MapNode<T> node = hierarchy.get(i);
				if (!node.isLeaf() && !seen.get(i)) {
					double ratio = getLogDeltaChange(i);
					if (ratio >= ratioLevel) {
						MapNode<T> left = node.getLeftNode();
						if (left != null && !left.isLeaf()) {
							nodeIndices.add(hierarchy.indexOf(left));
						}
						MapNode<T> right = node.getRightNode();
						if (right != null && !right.isLeaf()) {
							nodeIndices.add(hierarchy.indexOf(right));
						}
						while (node != null && !node.isLeaf()) {
							seen.set(hierarchy.indexOf(node));
							node = node.getParent();
						}
					}
				}
			}
		}
	}

	/**
	 * Selects the given number of best topics from the hierarchy.
	 *
	 * @param level
	 *            number of topics to select
	 */
	protected void adjustNodeIndicesByHierarchyLevel(int level) {
		nodeIndices.clear();
		if (hierarchy != null) {
			for (MapNode<T> root : hierarchy.getRoots()) {
				for (MapNode<T> node : topicSearcher.getBestTopics(level, root)) {
					nodeIndices.add(hierarchy.indexOf(node));
				}
			}
		}
	}

	/**
	 * Selects the topics of the solution with the given number of topics.
	 *
	 * @param topics
	 *            number of topics
	 */
	protected void adjustNodeIndicesByHistory(int topics) {
		nodeIndices.clear();
		if (hierarchy != null && topics >= 1 && topics <= hierarchy.getMaxTopics()) {
			for (MapNode<T> node : hierarchy.getTopicsAt(topics)) {
				nodeIndices.add(hierarchy.indexOf(node));
			}
		}
	}

	/**
	 * Selects topics containing at least the given number of words.
	 *
	 * @param minTopicSize
	 *            minimum topic size
	 */
	protected void adjustNodeIndicesByTopicSize(int minTopicSize) {
		nodeIndices.clear();
		if (hierarchy != null) {
			for (int i = 0; i < hierarchy.size(); i++) {
				if (hierarchy.getTopicSize(hierarchy.get(i)) >= minTopicSize) {
					nodeIndices.add(i);
				}
			}
		}
	}

	/**
	 * Selects all nodes.
	 */
	protected void adjustNodeIndicesByAll() {
		nodeIndices.clear();
		if (hierarchy != null) {
			for (int i = 0; i < hierarchy.size(); i++) {
				nodeIndices.add(i);
			}
		}
	}

	/**
	 * Creates a read-only tree model over the hierarchy. The root of the tree is
	 * the root of the hierarchy, or the hierarchy itself (shown as "Topics") if
	 * the solver stopped with several topics.
	 *
	 * @return the tree model
	 */
	protected TreeModel createTreeModel() {
		return new TreeModel() {
			@Override
			public void valueForPathChanged(TreePath path, Object newValue) {
				// Ignore on purpose.
			}

			@Override
			public void removeTreeModelListener(TreeModelListener l) {
				// Ignore on purpose.
			}

			@Override
			public void addTreeModelListener(TreeModelListener l) {
				// Ignore on purpose.
			}

			@Override
			public boolean isLeaf(Object node) {
				return getChildCount(node) == 0;
			}

			@Override
			public Object getRoot() {
				if (hierarchy == null) {
					return null;
				}
				return hierarchy.getRoots().size() == 1 ? hierarchy.getRoot() : new VirtualRoot();
			}

			@Override
			public int getIndexOfChild(Object parent, Object child) {
				return children(parent).indexOf(child);
			}

			@Override
			public int getChildCount(Object parent) {
				return children(parent).size();
			}

			@Override
			public Object getChild(Object parent, int index) {
				return children(parent).get(index);
			}

			@SuppressWarnings("unchecked")
			private List<MapNode<T>> children(Object parent) {
				if (parent instanceof MapNode) {
					return hierarchy.children((MapNode<T>) parent);
				}
				return hierarchy.getRoots();
			}
		};
	}

	/** The tree root standing for several top-level topics. */
	private class VirtualRoot {
		@Override
		public String toString() {
			return "Topics";
		}

		@Override
		public boolean equals(Object obj) {
			return obj instanceof HierarchyBrowser.VirtualRoot;
		}

		@Override
		public int hashCode() {
			return 1;
		}
	}

	/**
	 * Returns the ratio of a node's delta likelihood to the average of the preceding window of nodes.
	 *
	 * @param index
	 *            index into the hierarchy's nodes
	 * @return the ratio, 0 if too few predecessors
	 */
	protected double getLogDeltaChange(int index) {
		if (index < logDeltaChangeWindow) {
			return 0;
		}
		double sum = 0;
		for (int i = 0; i < logDeltaChangeWindow; i++) {
			sum += hierarchy.get(index - i - 1).getDeltaLikelihood();
		}
		double avg = sum / logDeltaChangeWindow;
		return hierarchy.get(index).getDeltaLikelihood() / avg;
	}

	/**
	 * Shows a file chooser.
	 *
	 * @param title
	 *            the dialog title
	 * @param save
	 *            true for a save dialog
	 * @param filter
	 *            the file filter
	 * @return the selected file, or null
	 */
	protected File chooseFile(String title, boolean save, FileNameExtensionFilter filter) {
		JFileChooser fileChooser = new JFileChooser(lastDirectory);
		fileChooser.setDialogTitle(title);
		fileChooser.addChoosableFileFilter(filter);
		fileChooser.setFileFilter(filter);
		int res = save ? fileChooser.showSaveDialog(frame) : fileChooser.showOpenDialog(frame);
		if (res != JFileChooser.APPROVE_OPTION) {
			return null;
		}
		File file = fileChooser.getSelectedFile();
		if (file != null) {
			lastDirectory = file.getParentFile() == null ? lastDirectory : file.getParentFile();
			if (save && filter.getExtensions().length == 1 && !file.getName().contains(".")) {
				file = new File(file.getParentFile(), file.getName() + "." + filter.getExtensions()[0]);
			}
		}
		return file;
	}

	/**
	 * Table model listing the selected topics with their size, frequency and top words.
	 */
	protected class TGTableModel implements TableModel {
		private int maxWords;
		private boolean withFrequencies;
		private int firstColumns = 2;

		/**
		 * Creates a model that shows frequencies.
		 */
		public TGTableModel() {
			this.withFrequencies = true;
		}

		/**
		 * Sets the number of word columns.
		 *
		 * @param maxWords
		 *            maximum words shown per topic
		 */
		public void setMaxWords(int maxWords) {
			if (maxWords != this.maxWords) {
				extraWords.clear();
			}
			this.maxWords = maxWords;
		}

		/**
		 * Returns the number of word columns.
		 *
		 * @return maximum words shown per topic
		 */
		public int getMaxWords() {
			return maxWords;
		}

		/**
		 * Returns whether word frequencies are shown next to the words.
		 *
		 * @return true if frequencies are shown
		 */
		public boolean isWithFrequencies() {
			return withFrequencies;
		}

		/**
		 * Sets whether word frequencies are shown.
		 *
		 * @param withFrequencies
		 *            true to show frequencies
		 */
		public void setWithFrequencies(boolean withFrequencies) {
			this.withFrequencies = withFrequencies;
		}

		@Override
		public int getRowCount() {
			return nodeIndices.size();
		}

		@Override
		public int getColumnCount() {
			return maxWords * (isWithFrequencies() ? 2 : 1) + firstColumns;
		}

		@Override
		public String getColumnName(int columnIndex) {
			if (columnIndex == 0) {
				return "Topic ID";
			}
			if (columnIndex == 1) {
				return "Topic Fr";
			}
			int index = (columnIndex - firstColumns) / (isWithFrequencies() ? 2 : 1);
			return ((!isWithFrequencies() || (columnIndex + firstColumns) % 2 == 0) ? "Word " : "Fr ") + index;
		}

		@Override
		public Class<?> getColumnClass(int columnIndex) {
			if (columnIndex == 1 || columnIndex == 0) {
				return Integer.class;
			}
			return isWithFrequencies() ? ((columnIndex + firstColumns) % 2 == 0 ? String.class : Integer.class)
					: String.class;
		}

		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex) {
			return false;
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			MapNode<T> value = hierarchy.get(nodeIndices.get(rowIndex));
			if (columnIndex == 0) {
				return value.getId();
			}
			if (columnIndex == 1) {
				return value.getTopicFrequency();
			}
			int index = (columnIndex - firstColumns) / (isWithFrequencies() ? 2 : 1);
			List<WordInfo<T>> wordInfos = value.getTopTopicWordInfos();
			WordInfo<T> info = null;
			if (index < wordInfos.size()) {
				info = wordInfos.get(index);
			} else {
				List<WordInfo<T>> extra = extraWords(value);
				if (index - wordInfos.size() < extra.size()) {
					info = extra.get(index - wordInfos.size());
				}
			}
			if (info == null) {
				return null;
			}
			return (!isWithFrequencies() || (columnIndex + firstColumns) % 2 == 0) ? label(info) : info
					.getFrequency();
		}

		/**
		 * The words of a topic beyond its top words, most frequent first; computed
		 * once per node and limited to the number of word columns.
		 */
		private List<WordInfo<T>> extraWords(MapNode<T> node) {
			List<WordInfo<T>> extra = extraWords.get(node);
			if (extra == null) {
				List<WordInfo<T>> top = node.getTopTopicWordInfos();
				List<MapNode<T>> leaves = new ArrayList<MapNode<T>>();
				hierarchy.collectLeaves(node, leaves);
				extra = new ArrayList<WordInfo<T>>();
				for (MapNode<T> leaf : leaves) {
					WordInfo<T> info = leaf.getWordInfo();
					if (info != null && !top.contains(info)) {
						extra.add(info);
					}
				}
				Collections.sort(extra);
				if (extra.size() > maxWords) {
					extra = new ArrayList<WordInfo<T>>(extra.subList(0, maxWords));
				}
				extraWords.put(node, extra);
			}
			return extra;
		}

		@Override
		public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void addTableModelListener(TableModelListener l) {
			// Ignore on purpose.
		}

		@Override
		public void removeTableModelListener(TableModelListener l) {
			// Ignore on purpose.
		}
	}

	/**
	 * Starts the browser with the FlatLaf light look and feel. FlatLaf scales
	 * the user interface with the system settings (macOS Retina, Windows and
	 * Linux scale factors); <code>-hidpi</code> doubles that scale
	 * (<code>flatlaf.uiScale=2</code>).
	 *
	 * @param args
	 *            optional: <code>-hidpi</code> to double the scale, and a
	 *            hierarchy file (<code>.json</code>, <code>.json.gz</code> or a
	 *            serialized node list) to open
	 */
	public static void main(String[] args) {
		boolean hiDpi = false;
		String file = null;
		for (String arg : args) {
			if ("-hidpi".equals(arg)) {
				hiDpi = true;
			} else {
				file = arg;
			}
		}
		if (hiDpi && System.getProperty("flatlaf.uiScale") == null) {
			System.setProperty("flatlaf.uiScale", "2");
		}
		System.setProperty("apple.laf.useScreenMenuBar", "true");
		System.setProperty("apple.awt.application.name", "TG Result Browser");
		if (!FlatLightLaf.setup()) {
			try {
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			} catch (Exception e) {
				// Keep the default look and feel.
			}
		}
		final File toOpen = file == null ? null : new File(file);
		SwingUtilities.invokeLater(new Runnable() {
			@SuppressWarnings("unchecked")
			@Override
			public void run() {
				HierarchyBrowser<String> browser = new HierarchyBrowser<String>(false);
				if (toOpen != null) {
					try {
						String name = toOpen.getName();
						if (name.endsWith(".json") || name.endsWith(".json.gz")) {
							browser.setHierarchy(new TopicHierarchyJsonReader().read(toOpen));
						} else {
							browser.setHierarchy(new TopicHierarchyObjectReader<String>().read(toOpen));
						}
					} catch (IOException e) {
						browser.showError("Cannot read " + toOpen, e);
					}
				}
			}
		});
	}
}
