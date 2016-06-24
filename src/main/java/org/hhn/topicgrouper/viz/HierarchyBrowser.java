package org.hhn.topicgrouper.viz;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Rectangle;
import java.awt.PageAttributes.OriginType;
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
import java.util.ArrayList;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
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
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import org.hhn.topicgrouper.report.store.MapNode;
import org.hhn.topicgrouper.report.store.WordInfo;

public class HierarchyBrowser<T> {
	/*
	 * Filter by topic size, all, number of topics in history (set number),
	 * hierarchy level (set level) delta ratio (set window size, set ratio
	 * level, set min history number)
	 */
	protected enum TableFilters {
		TOPIC_SIZE, ALL, HISTORY_NUMBER, HIERARCHY_LEVEL, DELTA_RATIO;
	};

	private final JFrame frame;
	private final JTree tree;
	private final TreeModel treeModel;

	private final JTable table;
	private final TGTableModel tableModel;

	private MapNode<T> root;
	private List<MapNode<T>> allNodes;
	private final TIntList nodeIndices;
	private final int alphaBase;

	private boolean inSelection = false;
	private int logDeltaChangeWindow = 5;
	private final JCheckBox showFrCheckBox;
	private final JCheckBox frColorCheckBox;
	private final DefaultTableCellRenderer tableCellRenderer;
	private final Color origTableBackground;
	private final Color origTreeBackground;

	public HierarchyBrowser(boolean highDPI) {
		alphaBase = 200;
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
									MapNode<T> node = (MapNode<T>) tree
											.getSelectionPath()
											.getLastPathComponent();
									int index = allNodes.indexOf(node);
									for (int i = 0; i < nodeIndices.size(); i++) {
										if (index == nodeIndices.get(i)) {
											index = i;
											break;
										}
									}
									if (index != -1) {
										table.getSelectionModel()
												.clearSelection();
										int viewIndex = table
												.convertRowIndexToView(index);
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
					setBackgroundNonSelectionColor(computerColor(
							((MapNode<T>) value).getTopicFrequency(),
							root.getTopicFrequency()));
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

		JPanel settingsPanel = new JPanel();
		BoxLayout boxLayout = new BoxLayout(settingsPanel, BoxLayout.X_AXIS);
		settingsPanel.setLayout(boxLayout);

		panel.add(settingsPanel, BorderLayout.NORTH);

		JComboBox comboBox = new JComboBox();

		comboBox.setModel(new DefaultComboBoxModel(TableFilters.values()));
		comboBox.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				bindToTable((TableFilters) e.getItem());
			}
		});
		settingsPanel.add(comboBox);

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
									int row = table
											.convertRowIndexToModel(table
													.getSelectedRow());
									MapNode<T> node = allNodes.get(row);
									List<MapNode<T>> pathList = new ArrayList<MapNode<T>>();
									while (node != null) {
										pathList.add(0, node);
										node = node.getParent();
									}
									TreePath path = new TreePath(pathList
											.toArray());
									tree.getSelectionModel().setSelectionPath(
											path);
									tree.scrollPathToVisible(path);
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

		tableCellRenderer = new DefaultTableCellRenderer() {
			@Override
			public Component getTableCellRendererComponent(JTable table,
					Object value, boolean isSelected, boolean hasFocus,
					int row, int column) {
				Component res = super.getTableCellRendererComponent(table,
						value, isSelected, hasFocus, row, column);

				if (frColorCheckBox.isSelected()) {
					res.setBackground(computerColor((Integer) value,
							root.getTopicFrequency()));
				} else {
					res.setBackground(origTableBackground);
				}

				return res;
			}
		};
		origTableBackground = tableCellRenderer.getBackground();

		scrollPane.setViewportView(table);

		panel.add(scrollPane, BorderLayout.CENTER);

		JPanel viewPanel = new JPanel();
		BoxLayout viewBoxLayout = new BoxLayout(viewPanel, BoxLayout.X_AXIS);
		viewPanel.setLayout(viewBoxLayout);

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

	protected void tableChanged() {
		table.tableChanged(new TableModelEvent(tableModel,
				TableModelEvent.HEADER_ROW));
		adjustTableCellRenderers();
	}

	protected void adjustTableCellRenderers() {
		table.getColumnModel().getColumn(1).setCellRenderer(tableCellRenderer);
	}

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
					nodeIndices.clear();
					for (int i = 0; i < allNodes.size(); i++) {
						nodeIndices.add(i);
					}
					tableModel.setMaxWords(root.getTopTopicWordInfos().size());
					tableChanged();
					frame.pack();
				}
			}
		});
		menu.add(openFileItem);

		menuBar.add(menu);

		return menuBar;
	}

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
				MapNode<String> n = (MapNode<String>) node;
				return n.getRightNode() == null && n.getLeftNode() == null;
			}

			@Override
			public Object getRoot() {
				return root;
			}

			@Override
			public int getIndexOfChild(Object parent, Object child) {
				MapNode<String> n = (MapNode<String>) parent;
				return child.equals(n.getLeftNode()) ? 0 : 1;
			}

			@Override
			public int getChildCount(Object parent) {
				MapNode<String> n = (MapNode<String>) parent;
				return n.getLeftNode() != null ? (n.getRightNode() != null ? 2
						: 1) : n.getRightNode() != null ? 1 : 0;
			}

			@Override
			public Object getChild(Object parent, int index) {
				MapNode<String> n = (MapNode<String>) parent;
				return index == 0 ? n.getLeftNode() : n.getRightNode();
			}
		};
	}

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

	protected void bindToTable(TableFilters filter) {

	}

	protected File showOpenFileMenu(JFrame parent) {
		JFileChooser fileChooser = new JFileChooser(".");
		fileChooser.showDialog(parent, "Choose TG File...");
		return fileChooser.getSelectedFile();
	}

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

	protected class TGTableModel implements TableModel {
		private int maxWords;
		private boolean withFrequencies;

		public TGTableModel() {
			this.withFrequencies = true;
		}

		public void setMaxWords(int maxWords) {
			this.maxWords = maxWords;
		}

		public int getMaxWords() {
			return maxWords;
		}

		public boolean isWithFrequencies() {
			return withFrequencies;
		}

		public void setWithFrequencies(boolean withFrequencies) {
			this.withFrequencies = withFrequencies;
		}

		@Override
		public int getRowCount() {
			return nodeIndices.size();
		}

		@Override
		public int getColumnCount() {
			return maxWords * (isWithFrequencies() ? 2 : 1) + 4;
		}

		@Override
		public String getColumnName(int columnIndex) {
			if (columnIndex == 0) {
				return "Topic ID";
			}
			if (columnIndex == 1) {
				return "Topic Fr";
			}
			if (columnIndex == 2) {
				return "Likelihood Delta";
			}
			if (columnIndex == 3) {
				return "LD Change";
			}
			int index = (columnIndex - 4) / (isWithFrequencies() ? 2 : 1);
			return ((!isWithFrequencies() || columnIndex % 2 == 0) ? "Word "
					: "Fr ") + index;
		}

		@Override
		public Class<?> getColumnClass(int columnIndex) {
			if (columnIndex == 0 || columnIndex == 1) {
				return Integer.class;
			}
			if (columnIndex == 2 || columnIndex == 3) {
				return Double.class;
			}
			return isWithFrequencies() ? (columnIndex % 2 == 0 ? String.class
					: Integer.class) : String.class;
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
			if (columnIndex == 2) {
				return value.getDeltaLikelihood();
			}
			if (columnIndex == 3) {
				return getLogDeltaChange(nodeIndices.get(rowIndex));
			}
			int index = (columnIndex - 4) / (isWithFrequencies() ? 2 : 1);
			List<WordInfo<T>> wordInfos = value.getTopTopicWordInfos();
			if (index < wordInfos.size()) {
				WordInfo<T> info = wordInfos.get(index);
				return (!isWithFrequencies() || columnIndex % 2 == 0) ? info
						.getWord() : info.getFrequency();
			}
			return "";
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
		double ratio = Math.log(topicFr) / Math.log(maxFr);
		int diff = (int) ((ratio > 1 ? 1 : ratio) * (255 - alphaBase));
		int b = alphaBase + diff;
		int r = 255 - diff;
		return new Color(r, alphaBase, b);
	}

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
				new HierarchyBrowser(true);
			}
		});
	}
}
