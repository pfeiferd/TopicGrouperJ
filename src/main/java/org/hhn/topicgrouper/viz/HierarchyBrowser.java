package org.hhn.topicgrouper.viz;

import java.awt.BorderLayout;
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

import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import org.hhn.topicgrouper.report.MindMapSolutionReporter;

public class HierarchyBrowser {
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

	private MindMapSolutionReporter.MapNode<String> root;
	private final JTable table;

	public HierarchyBrowser(boolean highDPI) {
		frame = new JFrame();
		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				System.exit(0);
			}
		});
		frame.setLayout(new BorderLayout());
		frame.add(createMenu(), BorderLayout.NORTH);

		tree = new JTree();
		if (highDPI) {
			tree.setRowHeight(2 * tree.getRowHeight());
		}
		JScrollPane scrollPane1 = new JScrollPane();
		scrollPane1.setViewportView(tree);

		frame.add(scrollPane1, BorderLayout.CENTER);

		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());

		frame.add(panel, BorderLayout.SOUTH);

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
		table = new JTable(3, 4);
		if (highDPI) {
			table.setRowHeight(2 * table.getRowHeight());
		}
		
		scrollPane.setViewportView(table);

		panel.add(scrollPane, BorderLayout.CENTER);

		frame.pack();
		frame.setVisible(true);
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
					root = loadFile(file);
					tree.setModel(bindToTreeModel());
				}
			}
		});
		menu.add(openFileItem);
		menuBar.add(menu);

		return menuBar;
	}

	protected TreeModel bindToTreeModel() {
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
				MindMapSolutionReporter.MapNode<String> n = (MindMapSolutionReporter.MapNode<String>) node;
				return n.getRightNode() != null && n.getLeftNode() != null;
			}

			@Override
			public Object getRoot() {
				return root;
			}

			@Override
			public int getIndexOfChild(Object parent, Object child) {
				MindMapSolutionReporter.MapNode<String> n = (MindMapSolutionReporter.MapNode<String>) parent;
				return child.equals(n.getLeftNode()) ? 0 : 1;
			}

			@Override
			public int getChildCount(Object parent) {
				MindMapSolutionReporter.MapNode<String> n = (MindMapSolutionReporter.MapNode<String>) parent;
				return n.getLeftNode() != null ? (n.getRightNode() != null ? 2
						: 1) : n.getRightNode() != null ? 1 : 0;
			}

			@Override
			public Object getChild(Object parent, int index) {
				MindMapSolutionReporter.MapNode<String> n = (MindMapSolutionReporter.MapNode<String>) parent;
				return index == 0 ? n.getLeftNode() : n.getRightNode();
			}
		};
	}

	protected void bindToTable(TableFilters filter) {

	}

	protected File showOpenFileMenu(JFrame parent) {
		JFileChooser fileChooser = new JFileChooser(".");
		fileChooser.showDialog(parent, "Choose TG File...");
		return fileChooser.getSelectedFile();
	}

	@SuppressWarnings("unchecked")
	protected MindMapSolutionReporter.MapNode<String> loadFile(File file) {
		try {
			ObjectInputStream oi = new ObjectInputStream(new FileInputStream(
					file));
			MindMapSolutionReporter.MapNode<String> res = (MindMapSolutionReporter.MapNode<String>) oi
					.readObject();
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
