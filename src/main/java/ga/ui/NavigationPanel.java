package ga.ui;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;

public class NavigationPanel extends JPanel {
    private JTree tree;
    private DefaultMutableTreeNode root;

    public NavigationPanel() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("导航"));
        setPreferredSize(new Dimension(180, 0));

        initTree();
    }

    private void initTree() {
        root = new DefaultMutableTreeNode("卡车-无人机配送优化系统");

        DefaultMutableTreeNode problemNode = new DefaultMutableTreeNode("问题配置");
        problemNode.add(new DefaultMutableTreeNode("仓库管理"));
        problemNode.add(new DefaultMutableTreeNode("客户管理"));
        problemNode.add(new DefaultMutableTreeNode("卡车参数"));
        problemNode.add(new DefaultMutableTreeNode("无人机参数"));
        root.add(problemNode);

        DefaultMutableTreeNode algoNode = new DefaultMutableTreeNode("算法配置");
        algoNode.add(new DefaultMutableTreeNode("GA参数"));
        algoNode.add(new DefaultMutableTreeNode("惩罚参数"));
        root.add(algoNode);

        DefaultMutableTreeNode resultNode = new DefaultMutableTreeNode("结果详情");
        root.add(resultNode);

        tree = new JTree(root);
        tree.setRootVisible(false);
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.setExpandsSelectedPaths(true);

        // 默认展开所有节点
        for (int i = 0; i < tree.getRowCount(); i++) {
            tree.expandRow(i);
        }

        Font font = new Font("微软雅黑", Font.PLAIN, 13);
        tree.setFont(font);

        JScrollPane scrollPane = new JScrollPane(tree);
        scrollPane.setBorder(null);
        add(scrollPane, BorderLayout.CENTER);
    }

    public void addTreeSelectionListener(javax.swing.event.TreeSelectionListener listener) {
        tree.addTreeSelectionListener(listener);
    }

    public String getSelectedNode() {
        TreePath path = tree.getSelectionPath();
        if (path != null) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
            return node.getUserObject().toString();
        }
        return null;
    }

    public void setRunAction(Runnable action) {
    }

    public void setStopAction(Runnable action) {
    }

    public void setResultAction(Runnable action) {
    }

    public void setRunning(boolean running) {
    }

    public void enableResult() {
    }
}
