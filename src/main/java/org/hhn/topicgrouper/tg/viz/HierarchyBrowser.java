package org.hhn.topicgrouper.tg.viz;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;
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
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import org.hhn.topicgrouper.tg.report.TopTopicSearcher;
import org.hhn.topicgrouper.tg.report.store.MapNode;
import org.hhn.topicgrouper.tg.report.store.WordInfo;

/**
 * Swing application for browsing a serialized topic hierarchy (MapNode list) as a tree and a filterable table of topics.
 * @param <T> the word type
 */
public class HierarchyBrowser<T> {
	/*
	 * Filter by topic size, all, number of topics in history (set number),
	 * hierarchy level (set level) delta ratio (set window size, set ratio
	 * level, set min history number)
	 */
	protected enum TableFilters {
		ALL, TOPIC_SIZE, HISTORY_NUMBER, HIERARCHY_LEVEL, DELTA_RATIO;
	};

	private final JFrame frame;
	private final JTree tree;
	private final TreeModel treeModel;

	private final JTable table;
	private final TGTableModel tableModel;

	private MapNode<T> root;
	private List<MapNode<T>> allNodes;
	/**
	 * Indices into allNodes of the topics currently shown in the table.
	 */
	private final TIntList nodeIndices;
	/**
	 * Base value of the color channels used for frequency coloring.
	 */
	private final int alphaBase;

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
	private final JCheckBox frColorCheckBox;
	private final Color origTreeBackground;
	private final JComboBox tableFilterComboBox;
	private final JSpinner minTopicSizeSpinner;
	private final JSpinner historySpinner;
	private final JSpinner hLevelSpinner;
	private final JPanel settingsPanel;
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

	/**
	 * Creates and shows the browser window.
	 * @param highDPI double pixel sizes for high-DPI displays
	 */
	@SuppressWarnings("serial")
	public HierarchyBrowser(boolean highDPI) {
		this.highDPI = highDPI;
		alphaBase = 200;
		topicSearcher = new TopTopicSearcher<T>();
		frame = new JFrame("TG Result Browser");
		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				System.exit(0);
			}
		});
		frame.setLayout(new BorderLayout());
		frame.add(createMenu(), BorderLayout.NORTH);

		JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		frame.add(splitPane, BorderLayout.CENTER);

		tree = new JTree();
		if (highDPI) {
			tree.setRowHeight(2 * tree.getRowHeight());
		}
		treeModel = createTreeModel();
		tree.setModel(treeModel);
		tree.getSelectionModel().addTreeSelectionListener(
				new TreeSelectionListener() {
					@Override
					public void valueChanged(TreeSelectionEvent e) {
						if (!inSelection) {
							try {
								inSelection = true;
								if (tree.getSelectionPath() != null) {
									@SuppressWarnings("unchecked")
									MapNode<T> node = (MapNode<T>) tree
											.getSelectionPath()
											.getLastPathComponent();
									int index = allNodes.indexOf(node);
									int res = -1;
									for (int i = 0; i < nodeIndices.size(); i++) {
										if (index == nodeIndices.get(i)) {
											res = i;
											break;
										}
									}
									if (res != -1) {
										table.getSelectionModel()
												.clearSelection();
										int viewIndex = table
												.convertRowIndexToView(res);
										table.getSelectionModel()
												.setSelectionInterval(
														viewIndex, viewIndex);
										table.scrollRectToVisible(new Rectangle(
												table.getCellRect(viewIndex, 0,
														true)));
									}
								}
							} finally {
								inSelection = false;
							}
						}
					}
				});
		DefaultTreeCellRenderer treeCellRenderer = new DefaultTreeCellRenderer() {
			public Component getTreeCellRendererComponent(JTree tree,
					Object value, boolean sel, boolean expanded, boolean leaf,
					int row, boolean hasFocus) {
				Component res = super.getTreeCellRendererComponent(tree, value,
						sel, expanded, leaf, row, hasFocus);
				if (frColorCheckBox.isSelected()) {
					@SuppressWarnings("unchecked")
					MapNode<T> node = (MapNode<T>) value;
					setBackgroundNonSelectionColor(computerColor(-node
							.getLikelihood() / node.getTopicFrequency()));
				} else {
					setBackgroundNonSelectionColor(origTreeBackground);
				}
				return res;
			};
		};
		origTreeBackground = treeCellRenderer.getBackground();
		tree.setCellRenderer(treeCellRenderer);

		JScrollPane scrollPane1 = new JScrollPane();
		scrollPane1.setViewportView(tree);

		splitPane.setTopComponent(scrollPane1);

		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());

		splitPane.setBottomComponent(panel);

		settingsPanel = new JPanel();
		BoxLayout boxLayout = new BoxLayout(settingsPanel, BoxLayout.X_AXIS);
		settingsPanel.setLayout(boxLayout);
		int bPixel = getPixel(4);
		settingsPanel.setBorder(BorderFactory.createEmptyBorder(bPixel, bPixel,
				bPixel, bPixel));

		panel.add(settingsPanel, BorderLayout.NORTH);

		tableFilterComboBox = new JComboBox();

		tableFilterComboBox.setModel(new DefaultComboBoxModel(TableFilters
				.values()));
		tableFilterComboBox.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				adjustNodeIndices();
			}
		});
		settingsPanel.add(tableFilterComboBox);
		settingsPanel.add(Box.createHorizontalStrut(getPixel(4)));

		allTopicsPanel = new JPanel();
		settingsPanel.add(allTopicsPanel);

		topicSizePanel = new JPanel();
		BoxLayout topicSizePanelLayout = new BoxLayout(topicSizePanel,
				BoxLayout.X_AXIS);
		topicSizePanel.setLayout(topicSizePanelLayout);

		minTopicSizeSpinner = new JSpinner();
		SpinnerNumberModel spinnerModel = new SpinnerNumberModel();
		spinnerModel.setMinimum(0);
		spinnerModel.setStepSize(1);
		minTopicSizeSpinner.setModel(spinnerModel);
		topicSizePanel.add(new JLabel("Min. Topic Size:"));
		topicSizePanel.add(Box.createHorizontalStrut(getPixel(2)));
		topicSizePanel.add(minTopicSizeSpinner);
		spinnerModel.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				adjustNodeIndices();
			}
		});

		historyPanel = new JPanel();
		BoxLayout historyPanelLayout = new BoxLayout(historyPanel,
				BoxLayout.X_AXIS);
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
		spinnerModel2.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				adjustNodeIndices();
			}
		});

		hierarchyLevelPanel = new JPanel();
		BoxLayout hierarchyLevelPanelLayout = new BoxLayout(
				hierarchyLevelPanel, BoxLayout.X_AXIS);
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
		spinnerModel3.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				adjustNodeIndices();
			}
		});

		ratioLevelPanel = new JPanel();
		BoxLayout rationLevelPanelLayout = new BoxLayout(ratioLevelPanel,
				BoxLayout.X_AXIS);
		ratioLevelPanel.setLayout(rationLevelPanelLayout);

		NumberFormat ratioFormat = NumberFormat
				.getNumberInstance(Locale.ENGLISH);
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

		JScrollPane scrollPane = new JScrollPane();
		table = new JTable();
		table.setAutoCreateRowSorter(true);
		table.setAutoCreateColumnsFromModel(true);
		if (highDPI) {
			table.setRowHeight(2 * table.getRowHeight());
		}
		table.getSelectionModel().addListSelectionListener(
				new ListSelectionListener() {
					@Override
					public void valueChanged(ListSelectionEvent e) {
						if (!inSelection) {
							try {
								inSelection = true;
								if (!e.getValueIsAdjusting()) {
									int tableRow = table.getSelectedRow();
									if (tableRow != -1) {
										int row = table
												.convertRowIndexToModel(tableRow);
										MapNode<T> node = allNodes
												.get(nodeIndices.get(row));
										List<MapNode<T>> pathList = new ArrayList<MapNode<T>>();
										while (node != null) {
											pathList.add(0, node);
											node = node.getParent();
										}
										TreePath path = new TreePath(pathList
												.toArray());
										tree.getSelectionModel()
												.setSelectionPath(path);
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
		//origTableBackground = ((DefaultTableCellRenderer) origRenderer).getBackground();


		scrollPane.setViewportView(table);

		panel.add(scrollPane, BorderLayout.CENTER);

		JPanel viewPanel = new JPanel();
		BoxLayout viewBoxLayout = new BoxLayout(viewPanel, BoxLayout.X_AXIS);
		viewPanel.setLayout(viewBoxLayout);
		viewPanel.setBorder(BorderFactory.createEmptyBorder(bPixel, bPixel,
				bPixel, bPixel));

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
		frColorCheckBox.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				table.repaint();
				tree.repaint();
			}
		});
		viewPanel.add(frColorCheckBox);

		panel.add(viewPanel, BorderLayout.SOUTH);

		frame.pack();
		frame.setVisible(true);
	}

	/**
	 * Scales a pixel size for the display.
	 * @param pixel size in pixels
	 * @return the size, doubled on high-DPI
	 */
	protected int getPixel(int pixel) {
		return highDPI ? 2 * pixel : pixel;
	}

	/**
	 * Notifies the table that its structure and data changed.
	 */
	protected void tableChanged() {
		table.tableChanged(new TableModelEvent(tableModel,
				TableModelEvent.HEADER_ROW));
//		adjustTableCellRenderers();
	}

//	protected void adjustTableCellRenderers() {
//		final TableCellRenderer origRenderer = table.getColumnModel().getColumn(1).getCellRenderer();
//		
//		TableCellRenderer tableCellRenderer = new TableCellRenderer() {
//			@Override
//			public Component getTableCellRendererComponent(JTable table,
//					Object value, boolean isSelected, boolean hasFocus,
//					int row, int column) {
//				Component res = origRenderer.getTableCellRendererComponent(table,
//						value, isSelected, hasFocus, row, column);
//
//				if (!isSelected) {
//					if (frColorCheckBox.isSelected()) {
//						res.setBackground(computerColor((Integer) value,
//								root.getTopicFrequency()));
//					} 
//					else {
//						res.setBackground(origTableBackground);
//					}
//				}
//
//				return res;
//			}
//		};
//
//		table.getColumnModel().getColumn(1).setCellRenderer(tableCellRenderer);
//	}

	/**
	 * Creates the menu bar with the file-open entry.
	 * @return the menu bar
	 */
	protected JMenuBar createMenu() {
		JMenuBar menuBar = new JMenuBar();
		JMenu menu = new JMenu("File");
		JMenuItem openFileItem = new JMenuItem("Open TG File...");
		openFileItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				File file = showOpenFileMenu(frame);
				if (file != null) {
					allNodes = loadFile(file);
					root = allNodes.get(allNodes.size() - 1);
					tree.setModel(null);
					tree.setModel(treeModel);
					for (int i = 0; i < tree.getRowCount(); i++) {
						tree.expandRow(i);
					}
					tableModel.setWithFrequencies(showFrCheckBox.isSelected());
					adjustNodeIndices();
					tableModel.setMaxWords(10 /*root.getTopTopicWordInfos().size()*/);
					spinnerModel2.setMaximum(allNodes.size());
					spinnerModel2.setValue(1);
					spinnerModel3.setMaximum(allNodes.size());
					spinnerModel3.setValue(1);
					spinnerModel4.setMaximum(allNodes.size());
					spinnerModel4.setValue(1);
					tableChanged();
					frame.pack();
				}
			}
		});
		menu.add(openFileItem);

		// JMenuItem exportCSVItem = new JMenuItem("Export as CSV...");
		// exportCSVItem.addActionListener(new ActionListener() {
		// @Override
		// public void actionPerformed(ActionEvent e) {
		// JFileChooser fileChooser = new JFileChooser(".");
		// fileChooser.showDialog(frame, "Save CSV File...");
		// File file = fileChooser.getSelectedFile();
		// if (file != null) {
		// System.out.println("save...");
		// }
		// }
		// });
		// menu.add(exportCSVItem);

		menuBar.add(menu);

		return menuBar;
	}

	/**
	 * Applies the currently selected table filter.
	 */
	protected void adjustNodeIndices() {
		adjustNodeIndices((TableFilters) tableFilterComboBox.getSelectedItem());
	}

	/**
	 * Applies the given filter, shows its settings panel and refreshes the table.
	 * @param filter the filter to apply
	 */
	protected void adjustNodeIndices(TableFilters filter) {
		settingsPanel.remove(2);
		switch (filter) {
		case ALL:
			adjustNodeIndicesByAll();
			settingsPanel.add(allTopicsPanel);
			break;
		case TOPIC_SIZE:
			adjustNodeIndicesByTopicSize((Integer) minTopicSizeSpinner
					.getValue());
			settingsPanel.add(topicSizePanel);
			break;
		case HISTORY_NUMBER:
			settingsPanel.add(historyPanel);
			adjustNodeIndicesByHistory((Integer) spinnerModel2.getValue());
			break;
		case HIERARCHY_LEVEL:
			settingsPanel.add(hierarchyLevelPanel);
			adjustNodeIndicesByHierarchyLevel((Integer) spinnerModel3
					.getValue());
			break;
		case DELTA_RATIO:
			settingsPanel.add(ratioLevelPanel);
			Number number = (Number) ratioField.getValue();
			adjustByRatioLevel(number.doubleValue(),
					(Integer) spinnerModel4.getValue());
			break;
		default:
			throw new IllegalStateException();
		}
		settingsPanel.revalidate();
		tableChanged();
	}

	/**
	 * Selects topics whose log delta change reaches the ratio level, together with their children.
	 * @param ratioLevel minimum delta change ratio
	 * @param maxTopic number of last history steps to examine
	 */
	protected void adjustByRatioLevel(double ratioLevel, int maxTopic) {
		nodeIndices.clear();
		if (allNodes != null) {
			BitSet bitSet = new BitSet(allNodes.size());
			for (int i = allNodes.size() - maxTopic; i < allNodes.size(); i++) {
				MapNode<T> node = allNodes.get(i);
				if (!bitSet.get(node.getId())) {
					double ratio = getLogDeltaChange(i);
					if (ratio >= ratioLevel) {
						MapNode<T> left = node.getLeftNode();
						if (left != null && left.getId() > 0) {
							nodeIndices.add(allNodes.size() - left.getId());
						}
						MapNode<T> right = node.getRightNode();
						if (right != null && right.getId() > 0) {
							nodeIndices.add(allNodes.size() - right.getId());
						}
						// nodeIndices.add(i);
						while (node != null && node.getId() > 0) {
							bitSet.set(node.getId());
							node = node.getParent();
						}
					}
				}
			}
		}
	}

	/**
	 * Selects the given number of best topics from the hierarchy.
	 * @param level number of topics to select
	 */
	protected void adjustNodeIndicesByHierarchyLevel(int level) {
		nodeIndices.clear();
		List<MapNode<T>> res = topicSearcher.getBestTopics(level, root);
		for (MapNode<T> node : res) {
			nodeIndices.add(allNodes.size() - node.getId());
		}
		// collectByLevel(root, 1, level);
	}

	/**
	 * Collects nodes at the target depth of the hierarchy.
	 * @param node current node
	 * @param level depth of the current node
	 * @param targetLevel depth to collect
	 */
	protected void collectByLevel(MapNode<T> node, int level, int targetLevel) {
		if (node == null) {
			return;
		}
		if (level == targetLevel && node.getId() > 0) {
			nodeIndices.add(allNodes.size() - node.getId());
		} else {
			collectByLevel(node.getLeftNode(), level + 1, targetLevel);
			collectByLevel(node.getRightNode(), level + 1, targetLevel);
		}
	}

	/**
	 * Selects the topics of the solution with the given number of topics.
	 * @param topicId number of topics (node id) to reconstruct
	 */
	protected void adjustNodeIndicesByHistory(int topicId) {
		nodeIndices.clear();
		if (allNodes != null) {
			BitSet bitSet = new BitSet(allNodes.size() - topicId);
			for (int i = topicId; i <= allNodes.size(); i++) {
				if (!bitSet.get(i - topicId)) {
					int nodeIndex = allNodes.size() - i;
					if (allNodes.get(nodeIndex).getId() >= 1) {
						nodeIndices.add(nodeIndex);
						markDeps(allNodes.get(nodeIndex), bitSet, topicId);
					}
				}
			}
		}
	}

	/**
	 * Marks the node and all its descendants in the bit set.
	 * @param node current node
	 * @param bitSet set receiving the marks
	 * @param topicId offset subtracted from node ids
	 */
	protected void markDeps(MapNode<T> node, BitSet bitSet, int topicId) {
		if (node == null || node.getId() < 1) {
			return;
		}
		bitSet.set(node.getId() - topicId);
		markDeps(node.getLeftNode(), bitSet, topicId);
		markDeps(node.getRightNode(), bitSet, topicId);
	}

	/**
	 * Selects topics containing at least the given number of words.
	 * @param minTopicSize minimum topic size
	 */
	protected void adjustNodeIndicesByTopicSize(int minTopicSize) {
		nodeIndices.clear();
		if (allNodes != null) {
			for (int i = 0; i < allNodes.size(); i++) {
				int size = allNodes.get(i).getTopicSize();
				if (size >= minTopicSize) {
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
		if (allNodes != null) {
			for (int i = 0; i < allNodes.size(); i++) {
				nodeIndices.add(i);
			}
		}
	}

	/**
	 * Creates a read-only tree model over the MapNode hierarchy.
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

			@SuppressWarnings("unchecked")
			@Override
			public boolean isLeaf(Object node) {
				MapNode<String> n = (MapNode<String>) node;
				return n.getRightNode() == null && n.getLeftNode() == null;
			}

			@Override
			public Object getRoot() {
				return root;
			}

			@SuppressWarnings("unchecked")
			@Override
			public int getIndexOfChild(Object parent, Object child) {
				MapNode<String> n = (MapNode<String>) parent;
				return child.equals(n.getLeftNode()) ? 0 : 1;
			}

			@SuppressWarnings("unchecked")
			@Override
			public int getChildCount(Object parent) {
				MapNode<String> n = (MapNode<String>) parent;
				return n.getLeftNode() != null ? (n.getRightNode() != null ? 2
						: 1) : n.getRightNode() != null ? 1 : 0;
			}

			@SuppressWarnings("unchecked")
			@Override
			public Object getChild(Object parent, int index) {
				MapNode<String> n = (MapNode<String>) parent;
				return index == 0 ? n.getLeftNode() : n.getRightNode();
			}
		};
	}

	/**
	 * Returns the ratio of a node's delta likelihood to the average of the preceding window of nodes.
	 * @param index index into allNodes
	 * @return the ratio, 0 if too few predecessors
	 */
	protected double getLogDeltaChange(int index) {
		if (index < logDeltaChangeWindow) {
			return 0;
		}
		double sum = 0;
		for (int i = 0; i < logDeltaChangeWindow; i++) {
			sum += allNodes.get(index - i - 1).getDeltaLikelihood();
		}
		double avg = sum / logDeltaChangeWindow;
		return allNodes.get(index).getDeltaLikelihood() / avg;
	}

	/**
	 * Shows a file chooser.
	 * @param parent parent frame
	 * @return the selected file, or null
	 */
	protected File showOpenFileMenu(JFrame parent) {
		JFileChooser fileChooser = new JFileChooser(".");
		fileChooser.showDialog(parent, "Choose TG File...");
		return fileChooser.getSelectedFile();
	}

	/**
	 * Loads a serialized MapNode list from a file.
	 * @param file the file
	 * @return the node list
	 * @throws RuntimeException if reading fails
	 */
	@SuppressWarnings("unchecked")
	protected List<MapNode<T>> loadFile(File file) {
		try {
			ObjectInputStream oi = new ObjectInputStream(new FileInputStream(
					file));
			List<MapNode<T>> res = (List<MapNode<T>>) oi.readObject();
			oi.close();
			return res;
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Table model listing the selected topics with their size, frequency and top words.
	 */
	protected class TGTableModel implements TableModel {
		private int maxWords;
		private boolean withFrequencies;
		private List<WordInfo<T>> wordInfoBuffer = new ArrayList<WordInfo<T>>();

		/**
		 * Creates a model that shows frequencies.
		 */
		public TGTableModel() {
			this.withFrequencies = true;
		}

		/**
		 * Sets the number of word columns.
		 * @param maxWords maximum words shown per topic
		 */
		public void setMaxWords(int maxWords) {
			this.maxWords = maxWords;
		}

		/**
		 * Returns the number of word columns.
		 * @return maximum words shown per topic
		 */
		public int getMaxWords() {
			return maxWords;
		}

		/**
		 * Returns whether word frequencies are shown next to the words.
		 * @return true if frequencies are shown
		 */
		public boolean isWithFrequencies() {
			return withFrequencies;
		}

		/**
		 * Sets whether word frequencies are shown.
		 * @param withFrequencies true to show frequencies
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

		private int firstColumns = 2;

		@Override
		public String getColumnName(int columnIndex) {
			if (columnIndex == 0) {
				return "Topic ID";
			}
			if (columnIndex == 1) {
				return "Topic Fr";
			}
			// if (columnIndex == 2) {
			// return "Likelihood Delta";
			// }
			// if (columnIndex == 3) {
			// return "LD Change";
			// }
			// if (columnIndex == 4) {
			// return "Topic Likelihood";
			// }
			int index = (columnIndex - firstColumns)
					/ (isWithFrequencies() ? 2 : 1);
			return ((!isWithFrequencies() || (columnIndex + firstColumns) % 2 == 0) ? "Word "
					: "Fr ")
					+ index;
		}

		@Override
		public Class<?> getColumnClass(int columnIndex) {
			if (columnIndex == 1 || columnIndex == 0) {
				return Integer.class;
			}
			// if (columnIndex == 2 || columnIndex == 3 || columnIndex == 4) {
			// return Double.class;
			// }
			return isWithFrequencies() ? ((columnIndex + firstColumns) % 2 == 0 ? String.class
					: Integer.class)
					: String.class;
		}

		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex) {
			return false;
		}
		
		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			MapNode<T> value = allNodes.get(nodeIndices.get(rowIndex));
			if (columnIndex == 0) {
				return value.getId();
			}
			if (columnIndex == 1) {
				return value.getTopicFrequency();
			}
			// if (columnIndex == 2) {
			// return value.getDeltaLikelihood();
			// }
			// if (columnIndex == 3) {
			// return getLogDeltaChange(nodeIndices.get(rowIndex));
			// }
			// if (columnIndex == 4) {
			// return value.getLikelihood() / value.getTopicFrequency();
			// }
			int index = (columnIndex - firstColumns)
					/ (isWithFrequencies() ? 2 : 1);
			List<WordInfo<T>> wordInfos = value.getTopTopicWordInfos();
			WordInfo<T> info = null;
			if (index < wordInfos.size()) {
				info = wordInfos.get(index);
				return (!isWithFrequencies() || (columnIndex + firstColumns) % 2 == 0) ? info
						.getWord() : info.getFrequency();
			}
			else {
				wordInfoBuffer.clear();
				value.collectWordInfos(wordInfoBuffer);
				wordInfoBuffer.removeAll(wordInfos);
				if (!wordInfoBuffer.isEmpty()) {
					Collections.sort(wordInfoBuffer);
					if (index - wordInfos.size() < wordInfoBuffer.size()) {
						info = wordInfoBuffer.get(index - wordInfos.size());
					}
				}
			}
			if (info == null) {
				return null;
			}
			return (!isWithFrequencies() || (columnIndex + firstColumns) % 2 == 0) ? info
					.getWord() : info.getFrequency();
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

	private Color computerColor(int topicFr, int maxFr) {
		return computerColor(Math.log(topicFr) / Math.log(maxFr));
	}

	private Color computerColor(double ratio /* int topicFr, int maxFr */) {
		// double ratio = Math.log(topicFr) / Math.log(maxFr);
		int diff = (int) ((ratio > 1 ? 1 : ratio) * (255 - alphaBase));
		int b = alphaBase + diff;
		int r = 255 - diff;
		return new Color(r, alphaBase, b);
	}

	/**
	 * Starts the browser.
	 * @param args ignored
	 */
	public static void main(String[] args) {
		try {
			// Set System L&F
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (UnsupportedLookAndFeelException e) {
			throw new RuntimeException(e);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		} catch (InstantiationException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				new HierarchyBrowser<String>(true);
			}
		});
	}
}
