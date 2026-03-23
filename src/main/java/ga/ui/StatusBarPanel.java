package ga.ui;

import ga.Config;

import javax.swing.*;
import java.awt.*;

public class StatusBarPanel extends JPanel {
    private JLabel statusLabel;
    private JLabel genLabel;
    private JLabel timeLabel;
    private JProgressBar progressBar;

    public StatusBarPanel() {
        setLayout(new FlowLayout(FlowLayout.LEFT, 10, 5));
        setBorder(BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));
        setPreferredSize(new Dimension(0, 30));

        Font font = new Font("微软雅黑", Font.PLAIN, 12);

        statusLabel = new JLabel("就绪");
        statusLabel.setFont(font);
        add(statusLabel);

        add(new JSeparator(SwingConstants.VERTICAL));

        genLabel = new JLabel("迭代: 0/0");
        genLabel.setFont(font);
        add(genLabel);

        progressBar = new JProgressBar(0, 100);
        progressBar.setValue(0);
        progressBar.setStringPainted(true);
        progressBar.setPreferredSize(new Dimension(120, 18));
        add(progressBar);

        timeLabel = new JLabel("时间: -");
        timeLabel.setFont(font);
        add(timeLabel);
    }

    public void setStatus(String status) {
        statusLabel.setText(status);
    }

    public void setStatusWithInfo(String status, int depotCount, int customerCount, int truckCount, int droneCount) {
        statusLabel.setText(status);
    }

    public void setRunning(boolean running) {
        if (running) {
            statusLabel.setText("运行中...");
        }
    }

    public void updateProgress(int current, int total, double time) {
        if (total > 0) {
            int percent = (int) ((current * 100.0) / total);
            progressBar.setValue(percent);
            progressBar.setString(current + "/" + total);
        }
        genLabel.setText("迭代: " + current + "/" + total);
        timeLabel.setText("时间: " + String.format("%.2f", time) + " min");
    }

    public void updateResult(double time) {
        progressBar.setValue(100);
        progressBar.setString("完成");
        timeLabel.setText("时间: " + String.format("%.2f", time) + " min");
        statusLabel.setText("完成");
    }

    public JLabel getGenLabel() {
        return genLabel;
    }
}
