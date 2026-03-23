package ga.ui;

import ga.Config;
import ga.GALogger;
import ga.GARunner;
import ga.SolomonLoader;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

public class ConfigFormPanel extends JPanel implements MapPreviewPanel.ConfigChangeListener {
    private Config config;
    private CardLayout cardLayout;
    private JPanel cardPanel;
    private MapPreviewPanel mapPanel;

    private JTable customerTable;
    private DefaultTableModel customerTableModel;

    private JTable depotTable;
    private DefaultTableModel depotTableModel;

    private JTextField tfTruckCount, tfTruckCapacity, tfTruckSpeed;
    private JTextField tfDronesPerTruck, tfTruckRange;

    @Deprecated  // 不再使用，总无人机数由 卡车数×每辆搭载数 自动计算
    private JTextField tfDroneCount, tfDroneRange, tfDroneSpeed;
    private JTextField tfDronePayload, tfDroneRechargeTime, tfDroneServiceTime;

    private JSlider slCrossover, slMutation, slElite;
    private JTextField tfPopSize, tfMaxGen, tfCrossover, tfMutation, tfElite, tfLaunchPointCount;
    private JComboBox<Integer> cbTournament;

    // 惩罚参数配置
    private JTextField tfTimeWindowEarly, tfTimeWindowLate;
    private JTextField tfDroneRangePenalty, tfDronePayloadPenalty;
    private JTextField tfWaitTimePenalty, tfDelayTimePenalty;
    private JTextField tfTruckRangeTimePenalty, tfTruckRangeDistancePenalty;
    private JTextField tfDistancePenalty, tfMaxTime;
    private JCheckBox cbPenaltyEnabled;

    // 结果页面组件
    private JLabel resultTimeLabel, resultGenLabel, resultElapsedLabel, resultObjectiveLabel;
    private JEditorPane resultRouteTextArea;
    private JPanel resultTimeChartPanel, resultRouteChartPanel;
    private GARunner.GAResult lastResult;

    // 多次运行结果面板
    private JPanel resultMultiChartPanel, resultBestOverallPanel, resultMultiRouteTextPanel;

    // Solomon 算例导入
    private JComboBox<SolomonLoader.SolomonData> solomonCombo;
    private JButton importSolomonBtn;

    public ConfigFormPanel() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("参数配置"));

        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);

        initForms();
        add(cardPanel, BorderLayout.CENTER);
    }

    private void initForms() {
        cardPanel.add(createWelcomePanel(), "welcome");
        cardPanel.add(createDepotPanel(), "仓库管理");
        cardPanel.add(createCustomerPanel(), "客户管理");
        cardPanel.add(createTruckPanel(), "卡车参数");
        cardPanel.add(createDronePanel(), "无人机参数");
        cardPanel.add(createGAPanel(), "GA参数");
        cardPanel.add(createPenaltyPanel(), "惩罚参数");
        cardPanel.add(createResultPanel(), "结果详情");
    }

    private JPanel createWelcomePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JLabel label = new JLabel("<html><center><h2>欢迎使用 Truck-Drone GA 优化器</h2><p>从左侧导航选择一个配置项进行编辑</p></center></html>", SwingConstants.CENTER);
        label.setFont(new Font("微软雅黑", Font.PLAIN, 16));
        panel.add(label, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createDepotPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addBtn = new JButton("添加仓库");
        JButton removeBtn = new JButton("删除");
        JButton clearBtn = new JButton("清空");

        addBtn.addActionListener(e -> {
            int id = depotTableModel.getRowCount() + 1;
            depotTableModel.addRow(new Object[]{id, 50.0, 50.0});
            syncDepotToConfig();
            if (mapPanel != null) mapPanel.setConfig(config);
        });
        removeBtn.addActionListener(e -> {
            int row = depotTable.getSelectedRow();
            if (row >= 0) {
                depotTableModel.removeRow(row);
                for (int i = row; i < depotTableModel.getRowCount(); i++) {
                    depotTableModel.setValueAt(i + 1, i, 0);
                }
                syncDepotToConfig();
                if (mapPanel != null) mapPanel.setConfig(config);
            }
        });
        clearBtn.addActionListener(e -> {
            depotTableModel.setRowCount(0);
            syncDepotToConfig();
            if (mapPanel != null) mapPanel.setConfig(config);
        });

        btnPanel.add(addBtn);
        btnPanel.add(removeBtn);
        btnPanel.add(clearBtn);
        panel.add(btnPanel, BorderLayout.NORTH);

        String[] depotCols = {"ID", "X坐标", "Y坐标"};
        depotTableModel = new DefaultTableModel(depotCols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return col != 0;
            }
        };
        depotTable = new JTable(depotTableModel);
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        depotTable.setDefaultRenderer(Object.class, centerRenderer);
        depotTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        depotTable.getColumnModel().getColumn(0).setMaxWidth(50);
        depotTable.getColumnModel().getColumn(1).setPreferredWidth(80);
        depotTable.getColumnModel().getColumn(2).setPreferredWidth(80);
        panel.add(new JScrollPane(depotTable), BorderLayout.CENTER);

        return panel;
    }

    private JPanel createCustomerPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel topPanel = new JPanel(new BorderLayout());

        JPanel solomonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        solomonPanel.add(new JLabel("导入Solomon算例:"));
        solomonCombo = new JComboBox<>();
        solomonCombo.setPreferredSize(new Dimension(200, 25));
        solomonCombo.addItem(null);
        solomonPanel.add(solomonCombo);

        importSolomonBtn = new JButton("导入");
        importSolomonBtn.setEnabled(false);
        importSolomonBtn.addActionListener(e -> {
            SolomonLoader.SolomonData selected = (SolomonLoader.SolomonData) solomonCombo.getSelectedItem();
            if (selected != null) {
                applySolomonData(selected);
            }
        });
        solomonPanel.add(importSolomonBtn);

        JLabel loadingLabel = new JLabel("扫描中...");
        loadingLabel.setForeground(Color.GRAY);
        solomonPanel.add(loadingLabel);

        // 添加按钮到同一栏右边
        JButton addBtn = new JButton("添加客户");
        JButton removeBtn = new JButton("删除");
        JButton clearBtn = new JButton("清空");
        solomonPanel.add(addBtn);
        solomonPanel.add(removeBtn);
        solomonPanel.add(clearBtn);

        addBtn.addActionListener(e -> {
            int id = customerTableModel.getRowCount() + 1;
            customerTableModel.addRow(new Object[]{id, 50.0, 50.0, 2.0, 0, 120, 5});
            syncCustomerToConfig();
            if (mapPanel != null) mapPanel.setConfig(config);
        });
        removeBtn.addActionListener(e -> {
            int row = customerTable.getSelectedRow();
            if (row >= 0) {
                customerTableModel.removeRow(row);
                for (int i = row; i < customerTableModel.getRowCount(); i++) {
                    customerTableModel.setValueAt(i + 1, i, 0);
                }
                syncCustomerToConfig();
                if (mapPanel != null) mapPanel.setConfig(config);
            }
        });
        clearBtn.addActionListener(e -> {
            customerTableModel.setRowCount(0);
            syncCustomerToConfig();
            if (mapPanel != null) mapPanel.setConfig(config);
        });

        new SwingWorker<List<SolomonLoader.SolomonData>, Void>() {
            @Override
            protected List<SolomonLoader.SolomonData> doInBackground() {
                return SolomonLoader.getAvailableInstances();
            }

            @Override
            protected void done() {
                try {
                    List<SolomonLoader.SolomonData> instances = get();
                    for (SolomonLoader.SolomonData data : instances) {
                        solomonCombo.addItem(data);
                    }
                    loadingLabel.setText("已加载 " + instances.size() + " 个算例");
                    loadingLabel.setForeground(new Color(0, 128, 0));
                    if (instances.size() > 0) {
                        importSolomonBtn.setEnabled(true);
                    }
                } catch (Exception ex) {
                    loadingLabel.setText("加载失败");
                    loadingLabel.setForeground(Color.RED);
                }
            }
        }.execute();

        panel.add(solomonPanel, BorderLayout.NORTH);

        String[] cols = {"ID", "X坐标", "Y坐标", "需求量", "时间窗开始", "时间窗结束", "服务时间"};
        customerTableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return col != 0;
            }

            @Override
            public Object getValueAt(int row, int col) {
                Object value = super.getValueAt(row, col);
                if (col == 4 || col == 5) {
                    if (value instanceof Number) {
                        return SolomonLoader.formatMinutes(((Number) value).intValue());
                    }
                } else if (col == 6) {
                    if (value instanceof Number) {
                        int seconds = (int) (Double.parseDouble(value.toString()) * 60);
                        return SolomonLoader.formatServiceTime(seconds);
                    }
                }
                return value;
            }
        };
        customerTable = new JTable(customerTableModel);

        customerTable.getColumnModel().getColumn(4).setCellEditor(new DefaultCellEditor(new JTextField()) {
            @Override
            public Component getTableCellEditorComponent(JTable table, Object value,
                    boolean isSelected, int row, int column) {
                String text = "";
                if (value != null) {
                    String str = value.toString();
                    if (str.contains("小时") || str.contains("分") || str.contains("秒")) {
                        text = String.valueOf(SolomonLoader.parseTime(str, false));
                    } else {
                        text = str;
                    }
                }
                ((JTextField) editorComponent).setText(text);
                return editorComponent;
            }

            @Override
            public Object getCellEditorValue() {
                String text = ((JTextField) editorComponent).getText();
                return SolomonLoader.parseTime(text, false);
            }
        });

        customerTable.getColumnModel().getColumn(5).setCellEditor(new DefaultCellEditor(new JTextField()) {
            @Override
            public Component getTableCellEditorComponent(JTable table, Object value,
                    boolean isSelected, int row, int column) {
                String text = "";
                if (value != null) {
                    String str = value.toString();
                    if (str.contains("小时") || str.contains("分") || str.contains("秒")) {
                        text = String.valueOf(SolomonLoader.parseTime(str, false));
                    } else {
                        text = str;
                    }
                }
                ((JTextField) editorComponent).setText(text);
                return editorComponent;
            }

            @Override
            public Object getCellEditorValue() {
                String text = ((JTextField) editorComponent).getText();
                return SolomonLoader.parseTime(text, false);
            }
        });

        customerTable.getColumnModel().getColumn(6).setCellEditor(new DefaultCellEditor(new JTextField()) {
            @Override
            public Component getTableCellEditorComponent(JTable table, Object value,
                    boolean isSelected, int row, int column) {
                String text = "";
                if (value != null) {
                    String str = value.toString();
                    if (str.contains("小时") || str.contains("分") || str.contains("秒")) {
                        int seconds = (int) (Double.parseDouble(str.replaceAll("[^0-9.]", "")) * 60);
                        text = String.valueOf(seconds);
                    } else {
                        text = str;
                    }
                }
                ((JTextField) editorComponent).setText(text);
                return editorComponent;
            }

            @Override
            public Object getCellEditorValue() {
                String text = ((JTextField) editorComponent).getText();
                int seconds = SolomonLoader.parseTime(text, true);
                return seconds / 60.0;
            }
        });

        DefaultTableCellRenderer customerRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                if (column == 4 || column == 5) {
                    if (value instanceof Number) {
                        value = SolomonLoader.formatMinutes(((Number) value).intValue());
                    }
                } else if (column == 6) {
                    if (value instanceof Number) {
                        int seconds = (int) (Double.parseDouble(value.toString()) * 60);
                        value = SolomonLoader.formatServiceTime(seconds);
                    }
                } else if (column == 3) {
                    // 超载检测：需求量列显示红色背景
                    if (value instanceof Number) {
                        double demand = Double.parseDouble(value.toString());
                        double droneMaxPayload = 10;
                        // 实时从 tfDronePayload 获取最新值
                        try {
                            droneMaxPayload = Double.parseDouble(tfDronePayload.getText());
                        } catch (Exception ex) {
                        }
                        if (demand > droneMaxPayload) {
                            setBackground(new Color(255, 200, 200));
                            setForeground(Color.RED);
                        } else {
                            setBackground(table.getBackground());
                            setForeground(table.getForeground());
                        }
                    }
                }
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setHorizontalAlignment(SwingConstants.CENTER);
                if (column != 3) {
                    setBackground(table.getBackground());
                    setForeground(table.getForeground());
                }
                return this;
            }
        };
        customerTable.setDefaultRenderer(Object.class, customerRenderer);
        customerTable.getColumnModel().getColumn(0).setPreferredWidth(40);
        customerTable.getColumnModel().getColumn(0).setMaxWidth(40);
        panel.add(new JScrollPane(customerTable), BorderLayout.CENTER);

        return panel;
    }

    private JPanel createTruckPanel() {
        JPanel panel = new JPanel(new GridLayout(0, 2, 10, 15));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        tfTruckCount = createNumberField(panel, "数量:", "2");
        tfTruckCapacity = createNumberField(panel, "载重(kg):", "2000");
        tfTruckSpeed = createNumberField(panel, "速度(km/h):", "40");
        tfDronesPerTruck = createNumberField(panel, "每辆卡车搭载无人机:", "2");
        tfTruckRange = createNumberField(panel, "续航(km):", "1000");

        return panel;
    }

    private JPanel createDronePanel() {
        JPanel panel = new JPanel(new GridLayout(0, 2, 10, 15));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        // 无人机参数（每辆卡车的无人机参数，总数=卡车数×每辆搭载数）
        tfDroneRange = createNumberField(panel, "续航(km):", "50");
        tfDroneSpeed = createNumberField(panel, "速度(km/h):", "60");
        tfDronePayload = createNumberField(panel, "最大载重(kg):", "50");
        // 载重修改时刷新客户表格的超载检测
        tfDronePayload.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void changedUpdate(javax.swing.event.DocumentEvent e) { refreshCustomerTable(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { refreshCustomerTable(); }
            public void insertUpdate(javax.swing.event.DocumentEvent e) { refreshCustomerTable(); }
        });
        tfDroneRechargeTime = createNumberField(panel, "充电时间(min):", "10");
        tfDroneServiceTime = createNumberField(panel, "服务时间(min):", "1.5");

        return panel;
    }

    private JPanel createPenaltyPanel() {
        JPanel panel = new JPanel(new GridLayout(0, 2, 10, 15));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        panel.add(new JLabel("<html><b>启用设置</b></html>"));
        cbPenaltyEnabled = new JCheckBox("启用惩罚计算", true);
        panel.add(cbPenaltyEnabled);

        panel.add(new JLabel("<html><b>时间窗惩罚</b></html>"));
        panel.add(new JLabel());

        tfTimeWindowEarly = createNumberField(panel, "提前到达权重:", "2.0");
        tfTimeWindowLate = createNumberField(panel, "延误到达权重:", "50.0");

        panel.add(new JLabel("<html><b>无人机惩罚</b></html>"));
        panel.add(new JLabel());

        tfDroneRangePenalty = createNumberField(panel, "续航超限权重:", "50.0");
        tfDronePayloadPenalty = createNumberField(panel, "载重超限权重:", "20.0");
        tfWaitTimePenalty = createNumberField(panel, "等待时间权重:", "2.0");
        tfDelayTimePenalty = createNumberField(panel, "延误时间权重:", "50.0");

        panel.add(new JLabel("<html><b>卡车惩罚</b></html>"));
        panel.add(new JLabel());

        tfTruckRangeTimePenalty = createNumberField(panel, "续航超限-时间权重:", "5.0");
        tfTruckRangeDistancePenalty = createNumberField(panel, "续航超限-距离权重:", "50.0");

        panel.add(new JLabel("<html><b>其他惩罚</b></html>"));
        panel.add(new JLabel());

        tfDistancePenalty = createNumberField(panel, "距离惩罚权重:", "10.0");

        panel.add(new JLabel("<html><b>归一化参数</b></html>"));
        panel.add(new JLabel());

        tfMaxTime = createNumberField(panel, "最大时间(分钟):", "10000");

        return panel;
    }

    private JPanel createGAPanel() {
        JPanel panel = new JPanel(new GridLayout(0, 2, 10, 15));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        tfPopSize = createNumberField(panel, "种群规模:", "100");
        tfMaxGen = createNumberField(panel, "最大迭代次数:", "500");

        slCrossover = new JSlider(0, 100, 80);
        JPanel crPanel = new JPanel(new BorderLayout());
        crPanel.add(new JLabel("交叉率:"), BorderLayout.WEST);
        crPanel.add(slCrossover, BorderLayout.CENTER);
        tfCrossover = new JTextField("0.8");
        crPanel.add(tfCrossover, BorderLayout.EAST);
        panel.add(crPanel);

        slMutation = new JSlider(0, 50, 15);
        JPanel mrPanel = new JPanel(new BorderLayout());
        mrPanel.add(new JLabel("变异率:"), BorderLayout.WEST);
        mrPanel.add(slMutation, BorderLayout.CENTER);
        tfMutation = new JTextField("0.15");
        mrPanel.add(tfMutation, BorderLayout.EAST);
        panel.add(mrPanel);

        slElite = new JSlider(0, 30, 10);
        JPanel erPanel = new JPanel(new BorderLayout());
        erPanel.add(new JLabel("精英率:"), BorderLayout.WEST);
        erPanel.add(slElite, BorderLayout.CENTER);
        tfElite = new JTextField("0.1");
        erPanel.add(tfElite, BorderLayout.EAST);
        panel.add(erPanel);

        panel.add(new JLabel("锦标赛规模:"));
        cbTournament = new JComboBox<>(new Integer[]{2, 3, 4, 5, 6, 7, 8, 9, 10});
        cbTournament.setSelectedIndex(1);
        panel.add(cbTournament);

        slCrossover.addChangeListener(e -> tfCrossover.setText(String.valueOf(slCrossover.getValue() / 100.0)));
        slMutation.addChangeListener(e -> tfMutation.setText(String.valueOf(slMutation.getValue() / 100.0)));
        slElite.addChangeListener(e -> tfElite.setText(String.valueOf(slElite.getValue() / 100.0)));

        tfLaunchPointCount = createNumberField(panel, "发射点数量(0=自动):", "0");

        return panel;
    }

    private JPanel createResultPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel statsPanel = new JPanel(new GridLayout(1, 4, 10, 10));
        statsPanel.setBorder(BorderFactory.createTitledBorder("统计信息"));
        Font labelFont = new Font("微软雅黑", Font.BOLD, 14);

        resultTimeLabel = new JLabel("最优时间: -");
        resultTimeLabel.setFont(labelFont);
        resultTimeLabel.setForeground(new Color(33, 150, 243));
        statsPanel.add(resultTimeLabel);

        resultGenLabel = new JLabel("迭代次数: -");
        resultGenLabel.setFont(labelFont);
        statsPanel.add(resultGenLabel);

        resultElapsedLabel = new JLabel("运行时间: -");
        resultElapsedLabel.setFont(labelFont);
        statsPanel.add(resultElapsedLabel);

        resultObjectiveLabel = new JLabel("优化目标: -");
        resultObjectiveLabel.setFont(labelFont);
        statsPanel.add(resultObjectiveLabel);

        JTabbedPane singleTabbedPane = new JTabbedPane();

        resultTimeChartPanel = new JPanel(new BorderLayout());
        resultTimeChartPanel.add(new JLabel("<html><center><h3>时间收敛曲线</h3><p>点击开始运行查看结果</p></center></html>", SwingConstants.CENTER), BorderLayout.CENTER);
        singleTabbedPane.addTab("时间收敛曲线", resultTimeChartPanel);

        resultRouteChartPanel = new JPanel(new BorderLayout());
        resultRouteChartPanel.add(new JLabel("<html><center><h3>配送路线图</h3><p>点击开始运行查看结果</p></center></html>", SwingConstants.CENTER), BorderLayout.CENTER);
        singleTabbedPane.addTab("路线图", resultRouteChartPanel);

        resultRouteTextArea = new JEditorPane();
        resultRouteTextArea.setContentType("text/html; charset=UTF-8");
        resultRouteTextArea.setEditable(false);
        resultRouteTextArea.setBackground(new Color(250, 250, 250));
        JScrollPane routeScroll = new JScrollPane(resultRouteTextArea);
        routeScroll.setBorder(BorderFactory.createTitledBorder("配送方案"));
        singleTabbedPane.addTab("配送方案", routeScroll);

        // 默认选择"路线图"标签页
        singleTabbedPane.setSelectedIndex(1);

        JTabbedPane multiTabbedPane = new JTabbedPane();

        resultMultiChartPanel = new JPanel(new BorderLayout());
        resultMultiChartPanel.add(new JLabel("<html><center><h3>叠加显示</h3><p>多次运行结果叠加</p></center></html>", SwingConstants.CENTER), BorderLayout.CENTER);
        multiTabbedPane.addTab("叠加显示", resultMultiChartPanel);

        resultBestOverallPanel = new JPanel(new BorderLayout());
        resultBestOverallPanel.add(new JLabel("<html><center><h3>历史最优路线图</h3><p>多次运行中的最优解路线</p></center></html>", SwingConstants.CENTER), BorderLayout.CENTER);
        multiTabbedPane.addTab("路线图", resultBestOverallPanel);

        resultMultiRouteTextPanel = new JPanel(new BorderLayout());
        resultMultiRouteTextPanel.add(new JLabel("<html><center><h3>历史最优配送方案</h3><p>多次运行中的最优解方案</p></center></html>", SwingConstants.CENTER), BorderLayout.CENTER);
        multiTabbedPane.addTab("配送方案", resultMultiRouteTextPanel);

        JTabbedPane mainTabbedPane = new JTabbedPane();
        mainTabbedPane.addTab("单次运行", singleTabbedPane);
        mainTabbedPane.addTab("多次运行", multiTabbedPane);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setTopComponent(statsPanel);
        splitPane.setBottomComponent(mainTabbedPane);
        splitPane.setDividerLocation(60);

        panel.add(splitPane, BorderLayout.CENTER);

        return panel;
    }

    public void updateResults(GARunner.GAResult result) {
        this.lastResult = result;
        if (result == null) return;

        resultTimeLabel.setText("最优时间: " + String.format("%.2f 分钟", result.bestTime));
        resultGenLabel.setText("迭代次数: " + result.timeCurve.size());
        resultElapsedLabel.setText("运行时间: " + result.elapsedTime + " 毫秒");

        resultObjectiveLabel.setText("优化目标: 时间优先");

        if (result.timeCurve.size() > 1) {
            JPanel chart = createChartPanel("时间收敛曲线", result.timeCurve, new Color(33, 150, 243), "分钟");
            resultTimeChartPanel.removeAll();
            resultTimeChartPanel.add(chart, BorderLayout.CENTER);
        }

        if (config != null && result != null) {
            if (mapPanel != null) {
                mapPanel.setConfig(config);
                mapPanel.setResult(result);
                mapPanel.setShowRoutes(true);
                mapPanel.setMode("结果详情");
                SwingUtilities.invokeLater(() -> mapPanel.autoFit());
                resultRouteChartPanel.removeAll();
                resultRouteChartPanel.add(mapPanel, BorderLayout.CENTER);
            } else {
                MapPreviewPanel routeMap = new MapPreviewPanel();
                routeMap.setConfig(config);
                routeMap.setResult(result);
                routeMap.setShowRoutes(true);
                routeMap.setMode("结果详情");
                resultRouteChartPanel.removeAll();
                resultRouteChartPanel.add(routeMap, BorderLayout.CENTER);
            }
        }

        // 更新配送方案文本
        StringBuilder sb = new StringBuilder();
        
        if (result.bestChromosome != null && result.truckRoutes != null && result.droneTasks != null) {
            sb.append(buildTimelineString(result));
        } else {
            sb.append("无有效的配送方案数据\n");
        }

        String htmlContent = "<html><body><pre style='font-family: \"Microsoft YaHei\", \"Segoe UI Emoji\", sans-serif; font-size: 13px;'>" 
            + escapeHtml(sb.toString()) + "</pre></body></html>";
        resultRouteTextArea.setText(htmlContent);
        resultRouteTextArea.setCaretPosition(0);
    }
    
    private String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }

    /**
     * 构建时间轴配送方案字符串
     */
    private String buildTimelineString(GARunner.GAResult result) {
        StringBuilder sb = new StringBuilder();

        int truckCount = result.truckRoutes.size();
        int dronesPerTruck = config != null && config.vehicles != null && config.vehicles.trucks != null ?
            config.vehicles.trucks.dronesPerTruck : 2;

        double truckSpeed = config != null && config.vehicles != null && config.vehicles.trucks != null ?
            config.vehicles.trucks.speed : 40;
        double droneSpeed = config != null && config.vehicles != null && config.vehicles.drones != null ?
            config.vehicles.drones.speed : 60;
        double droneRechargeTime = config != null && config.vehicles != null && config.vehicles.drones != null ?
            config.vehicles.drones.rechargeTime : 10;

        List<Double> launchPoints = result.allLaunchPoints;
        if (launchPoints == null && result.bestChromosome != null) {
            launchPoints = result.bestChromosome.launchPointCoords;
        }
        List<Config.Depot> depots = config != null && config.problem != null ? config.problem.depots : new ArrayList<>();
        
        // 获取仓库坐标
        double depotX = 50.0, depotY = 50.0;
        if (depots != null && !depots.isEmpty()) {
            depotX = depots.get(0).x;
            depotY = depots.get(0).y;
        }

        // 为每辆卡车生成时间轴
        if (launchPoints != null && result.truckRoutes != null) {
            for (int t = 0; t < truckCount; t++) {
                List<Integer> route = result.truckRoutes.get(t);
                if (route == null || route.isEmpty()) continue;

                // 卡车部分标题
                sb.append(String.format("【卡车 %d】（搭载 %d 架无人机）\n", t + 1, dronesPerTruck));
                sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
                
                double currentTime = 0;
                double currentX = depotX;
                double currentY = depotY;
                
                // 卡车出发
                sb.append(String.format("0:00   🏭 仓库 (%.1f, %.1f) 出发\n", depotX, depotY));
                
                // 遍历路线上的每个点
                for (int i = 1; i < route.size(); i++) {
                    int pointId = route.get(i);
                    
                    // 获取当前点坐标
                    double targetX, targetY;
                    if (pointId == 0) {
                        // 返回仓库
                        targetX = depotX;
                        targetY = depotY;
                    } else if (pointId < 0) {
                        // 发射点
                        int launchIdx = -pointId - 1;
                        if (launchIdx >= 0 && launchIdx * 2 + 1 < launchPoints.size()) {
                            targetX = launchPoints.get(launchIdx * 2);
                            targetY = launchPoints.get(launchIdx * 2 + 1);
                        } else {
                            continue;
                        }
                    } else {
                        continue;
                    }
                    
                    // 计算到达时间
                    double dist = Math.sqrt(Math.pow(targetX - currentX, 2) + Math.pow(targetY - currentY, 2));
                    double travelTime = dist / truckSpeed * 60;
                    currentTime += travelTime;
                    
                    if (pointId < 0) {
                        // 到达发射点
                        sb.append(String.format("%s   🚀 发射点 (%.1f, %.1f) 到达\n", 
                            formatTime(currentTime), targetX, targetY));
                        sb.append("\n");
                        
                        // 无人机服务客户
                        int launchIdx = -pointId - 1;
                        
                        // 使用 droneToLaunchPoint 映射判断无人机是否属于当前发射点
                        List<Integer> droneToLP = result.droneToLaunchPoint;
                        
                        for (int d = 0; d < dronesPerTruck; d++) {
                            int droneIdx = t * dronesPerTruck + d;
                            
                            // 检查该无人机是否属于当前发射点
                            int droneLaunchIdx = -1;
                            if (droneToLP != null && droneIdx < droneToLP.size()) {
                                droneLaunchIdx = droneToLP.get(droneIdx);
                            }
                            
                            // 如果该无人机不属于当前发射点，跳过
                            if (droneLaunchIdx != launchIdx) continue;
                            
                            List<Integer> tasks = droneIdx < result.droneTasks.size() ? 
                                result.droneTasks.get(droneIdx) : new ArrayList<>();
                            
                            if (!tasks.isEmpty()) {
                                sb.append(String.format("      【无人机 %d】\n", droneIdx + 1));
                                sb.append("      ───────────────────────────────────────────────────────────────────────\n");
                                
                                for (int custId : tasks) {
                                    Config.Customer customer = getCustomerById(custId);
                                    if (customer == null) continue;
                                    
                                    double distToCust = Math.sqrt(
                                        Math.pow(customer.x - targetX, 2) +
                                        Math.pow(customer.y - targetY, 2));
                                    double flightTime = distToCust / droneSpeed * 60;
                                    
                                    sb.append(String.format("      %s   → 飞往客户%d (%.1f km)\n",
                                        formatTime(currentTime), custId, distToCust));
                                    currentTime += flightTime;
                                    
                                    sb.append(String.format("      %s   📦 客户%d 配送完成 (服务 %.0f 分钟)\n",
                                        formatTime(currentTime), custId, customer.serviceTime));
                                    currentTime += customer.serviceTime;
                                    
                                    sb.append(String.format("      %s   ← 返回发射点 (%.1f km)\n",
                                        formatTime(currentTime), distToCust));
                                    currentTime += flightTime;
                                    
                                    sb.append(String.format("      %s   🔋 充电完成\n",
                                        formatTime(currentTime)));
                                    currentTime += droneRechargeTime;
                                }
                                sb.append("\n");
                            }
                        }
                    } else {
                        // 返回仓库
                        sb.append(String.format("%s   🏭 返回仓库 (%.1f, %.1f)\n",
                            formatTime(currentTime), targetX, targetY));
                    }
                    
                    currentX = targetX;
                    currentY = targetY;
                }
                sb.append("\n");
            }
        }

        // 总结
        sb.append("【配送总结】\n");
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        int totalServed = 0;
        int totalCustomers = config != null && config.problem != null &&
            config.problem.customers != null ? config.problem.customers.size() : 0;
        for (List<Integer> tasks : result.droneTasks) {
            totalServed += tasks.size();
        }
        sb.append(String.format("   服务客户数: %d / %d\n", totalServed, totalCustomers));
        sb.append(String.format("   总耗时: %.0f 分钟\n", result.bestTime));

        return sb.toString();
    }

    /**
     * 格式化时间（分钟）为 H:MM 或 MM:SS 格式
     */
    private String formatTime(double minutes) {
        int totalSeconds = (int) Math.round(minutes * 60);
        int hours = totalSeconds / 3600;
        int mins = (totalSeconds % 3600) / 60;
        int secs = totalSeconds % 60;

        if (hours > 0) {
            if (secs > 0) {
                return String.format("%d:%02d:%02d", hours, mins, secs);
            } else {
                return String.format("%d:%02d", hours, mins);
            }
        } else {
            if (secs > 0) {
                return String.format("%d:%02d", mins, secs);
            } else {
                return String.format("%d:00", mins);
            }
        }
    }

    /**
     * 根据客户ID获取客户对象
     */
    private Config.Customer getCustomerById(int custId) {
        if (config != null && config.problem != null && config.problem.customers != null) {
            for (Config.Customer c : config.problem.customers) {
                if (c.id == custId) return c;
            }
        }
        return null;
    }

    public void updateSingleResult(GARunner.GAResult result) {
        updateResults(result);
    }

    public void updateMultiProgress(GARunner.GAResult result) {
        if (result == null) return;

        resultTimeLabel.setText("当前时间: " + String.format("%.2f 分钟", result.bestTime));

        if (result.timeCurve.size() > 1) {
            JPanel chart = createChartPanel("当前运行时间", result.timeCurve, new Color(33, 150, 243), "分钟");
            resultMultiChartPanel.removeAll();
            resultMultiChartPanel.add(chart, BorderLayout.CENTER);
        }
    }

    public void updateMultiResult(GARunner.MultiRunResult multiResult) {
        if (multiResult == null || multiResult.bestOverall == null) return;

        GARunner.GAResult best = multiResult.bestOverall;

        resultTimeLabel.setText("最优时间: " + String.format("%.2f 分钟", best.bestTime));
        resultGenLabel.setText("完成次数: " + multiResult.completedRuns + "/" + multiResult.totalRuns);
        resultElapsedLabel.setText("最优运行时间: " + best.elapsedTime + " 毫秒");

        resultObjectiveLabel.setText("优化目标: 时间优先");

        if (multiResult.runs != null && multiResult.runs.size() > 1) {
            JPanel stackedChart = createStackedCurveChart("叠加显示 - 时间", multiResult.runs, "分钟");
            resultMultiChartPanel.removeAll();
            resultMultiChartPanel.add(stackedChart, BorderLayout.CENTER);
        }

        if (multiResult.bestTimeCurve.size() > 1) {
            JPanel bestChart = createChartPanel("最优曲线 - 时间", multiResult.bestTimeCurve, new Color(255, 87, 34), "分钟");
            resultTimeChartPanel.removeAll();
            resultTimeChartPanel.add(bestChart, BorderLayout.CENTER);
        }

        // 设置历史最优路线图
        resultBestOverallPanel.removeAll();
        MapPreviewPanel routeMap = new MapPreviewPanel();
        routeMap.setConfig(config);
        routeMap.setResult(best);
        routeMap.setShowRoutes(true);
        routeMap.setMode("结果详情");
        resultBestOverallPanel.add(routeMap, BorderLayout.CENTER);

        // 设置历史最优配送方案
        resultMultiRouteTextPanel.removeAll();
        StringBuilder sb = new StringBuilder();
        sb.append("========== 历史最优 ==========\n\n");
        sb.append("最优时间: ").append(String.format("%.2f 分钟", best.bestTime)).append("\n");
        sb.append("运行时间: ").append(best.elapsedTime).append(" 毫秒\n\n");

        if (best.bestChromosome != null && best.truckRoutes != null && best.droneTasks != null) {
            sb.append(buildTimelineString(best));
        } else {
            sb.append("无有效的配送方案数据\n");
        }
        JEditorPane textArea = new JEditorPane();
        textArea.setContentType("text/html; charset=UTF-8");
        textArea.setEditable(false);
        textArea.setBackground(new Color(250, 250, 250));
        String htmlContent2 = "<html><body><pre style='font-family: \"Microsoft YaHei\", \"Segoe UI Emoji\", sans-serif; font-size: 12px;'>" 
            + escapeHtml(sb.toString()) + "</pre></body></html>";
        textArea.setText(htmlContent2);
        resultMultiRouteTextPanel.add(new JScrollPane(textArea), BorderLayout.CENTER);
    }

    private JPanel createStackedCurveChart(String title, java.util.List<GARunner.GAResult> runs, String yAxisLabel) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        if (runs == null || runs.size() < 2) {
            panel.add(new JLabel("需要至少2次运行才能显示叠加曲线", SwingConstants.CENTER), BorderLayout.CENTER);
            return panel;
        }

        XYSeriesCollection dataset = new XYSeriesCollection();
        Color[] colors = {
            new Color(76, 175, 80), new Color(33, 150, 243), new Color(255, 87, 34),
            new Color(156, 39, 176), new Color(255, 193, 7), new Color(0, 188, 212)
        };

        for (int i = 0; i < runs.size(); i++) {
            GARunner.GAResult result = runs.get(i);
            java.util.List<Double> curveData = result.timeCurve;
            if (curveData != null && curveData.size() > 1) {
                XYSeries series = new XYSeries("运行" + (i + 1));
                for (int j = 0; j < curveData.size(); j++) {
                    series.add(j, curveData.get(j));
                }
                dataset.addSeries(series);
            }
        }

        JFreeChart chart = ChartFactory.createXYLineChart(
            title, "迭代次数", yAxisLabel, dataset,
            PlotOrientation.VERTICAL, true, false, false
        );

        Font chineseFont = new Font("微软雅黑", Font.PLAIN, 12);
        chart.getTitle().setFont(chineseFont);
        XYPlot plot = chart.getXYPlot();
        plot.getDomainAxis().setLabelFont(chineseFont);
        plot.getRangeAxis().setLabelFont(chineseFont);

        ChartPanel chartPanel = new ChartPanel(chart) {
            private int panStartX, panStartY;
            private double startDomainLower, startDomainUpper;
            private double startRangeLower, startRangeUpper;
            private boolean isPanning = false;

            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                if (javax.swing.SwingUtilities.isRightMouseButton(e)) {
                    isPanning = true;
                    panStartX = e.getX();
                    panStartY = e.getY();
                    XYPlot plot = getChart().getXYPlot();
                    startDomainLower = plot.getDomainAxis().getLowerBound();
                    startDomainUpper = plot.getDomainAxis().getUpperBound();
                    startRangeLower = plot.getRangeAxis().getLowerBound();
                    startRangeUpper = plot.getRangeAxis().getUpperBound();
                } else {
                    super.mousePressed(e);
                }
            }

            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {
                if (isPanning) {
                    isPanning = false;
                } else {
                    super.mouseReleased(e);
                }
            }

            @Override
            public void mouseDragged(java.awt.event.MouseEvent e) {
                if (isPanning) {
                    XYPlot plot = getChart().getXYPlot();
                    double domainRange = startDomainUpper - startDomainLower;
                    double rangeRange = startRangeUpper - startRangeLower;

                    double domainShift = (panStartX - e.getX()) * domainRange / getWidth();
                    double rangeShift = (e.getY() - panStartY) * rangeRange / getHeight();

                    double newDomainLower = Math.max(0, startDomainLower + domainShift);
                    double newRangeLower = startRangeLower + rangeShift;

                    if (newDomainLower >= 0) {
                        plot.getDomainAxis().setLowerBound(newDomainLower);
                        plot.getDomainAxis().setUpperBound(newDomainLower + domainRange);
                    }
                    plot.getRangeAxis().setLowerBound(newRangeLower);
                    plot.getRangeAxis().setUpperBound(newRangeLower + rangeRange);
                } else {
                    super.mouseDragged(e);
                }
            }
        };
        chartPanel.setMouseWheelEnabled(true);
        chartPanel.setPopupMenu(null);

        panel.add(chartPanel, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createCombinedChartPanel(GARunner.GAResult result) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        XYSeriesCollection dataset = new XYSeriesCollection();

        XYSeries timeSeries = new XYSeries("时间");
        for (int i = 0; i < result.timeCurve.size(); i++) {
            timeSeries.add(i, result.timeCurve.get(i));
        }
        dataset.addSeries(timeSeries);

        JFreeChart chart = ChartFactory.createXYLineChart(
            "时间收敛曲线", "迭代次数", "分钟", dataset,
            PlotOrientation.VERTICAL, true, false, false
        );

        Font chineseFont = new Font("微软雅黑", Font.PLAIN, 12);
        chart.getTitle().setFont(chineseFont);
        XYPlot plot = chart.getXYPlot();
        plot.getDomainAxis().setLabelFont(chineseFont);

        ChartPanel chartPanel = new ChartPanel(chart) {
            private int panStartX, panStartY;
            private double startDomainLower, startDomainUpper;
            private double startRangeLower, startRangeUpper;
            private boolean isPanning = false;

            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                if (javax.swing.SwingUtilities.isRightMouseButton(e)) {
                    isPanning = true;
                    panStartX = e.getX();
                    panStartY = e.getY();
                    XYPlot plot = getChart().getXYPlot();
                    startDomainLower = plot.getDomainAxis().getLowerBound();
                    startDomainUpper = plot.getDomainAxis().getUpperBound();
                    startRangeLower = plot.getRangeAxis().getLowerBound();
                    startRangeUpper = plot.getRangeAxis().getUpperBound();
                } else {
                    super.mousePressed(e);
                }
            }

            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {
                if (isPanning) {
                    isPanning = false;
                } else {
                    super.mouseReleased(e);
                }
            }

            @Override
            public void mouseDragged(java.awt.event.MouseEvent e) {
                if (isPanning) {
                    XYPlot plot = getChart().getXYPlot();
                    double domainRange = startDomainUpper - startDomainLower;
                    double rangeRange = startRangeUpper - startRangeLower;

                    double domainShift = (panStartX - e.getX()) * domainRange / getWidth();
                    double rangeShift = (e.getY() - panStartY) * rangeRange / getHeight();

                    double newDomainLower = Math.max(0, startDomainLower + domainShift);
                    double newRangeLower = startRangeLower + rangeShift;

                    if (newDomainLower >= 0) {
                        plot.getDomainAxis().setLowerBound(newDomainLower);
                        plot.getDomainAxis().setUpperBound(newDomainLower + domainRange);
                    }
                    plot.getRangeAxis().setLowerBound(newRangeLower);
                    plot.getRangeAxis().setUpperBound(newRangeLower + rangeRange);
                } else {
                    super.mouseDragged(e);
                }
            }
        };
        chartPanel.setMouseWheelEnabled(true);
        chartPanel.setPopupMenu(null);

        panel.add(chartPanel, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createChartPanel(String title, java.util.List<Double> data, Color color, String yAxisLabel) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        if (data == null || data.size() < 2) {
            panel.add(new JLabel("数据不足", SwingConstants.CENTER), BorderLayout.CENTER);
            return panel;
        }

        XYSeriesCollection dataset = new XYSeriesCollection();
        XYSeries series = new XYSeries(title);
        for (int i = 0; i < data.size(); i++) {
            series.add(i, data.get(i));
        }
        dataset.addSeries(series);

        JFreeChart chart = ChartFactory.createXYLineChart(
            title, "迭代次数", yAxisLabel, dataset,
            PlotOrientation.VERTICAL, true, false, false
        );

        Font chineseFont = new Font("微软雅黑", Font.PLAIN, 12);
        chart.getTitle().setFont(chineseFont);
        chart.getLegend().setItemFont(chineseFont);

        XYPlot plot = chart.getXYPlot();
        plot.getDomainAxis().setLabelFont(chineseFont);
        plot.getRangeAxis().setLabelFont(chineseFont);
        plot.getDomainAxis().setTickLabelFont(chineseFont);
        plot.getRangeAxis().setTickLabelFont(chineseFont);
        plot.getRenderer().setSeriesPaint(0, color);

        plot.setDomainGridlinesVisible(true);
        plot.setRangeGridlinesVisible(true);
        plot.setDomainGridlineStroke(new BasicStroke(0.5f));
        plot.setRangeGridlineStroke(new BasicStroke(0.5f));
        plot.setDomainGridlinePaint(new Color(200, 200, 200));
        plot.setRangeGridlinePaint(new Color(200, 200, 200));

        NumberAxis domainAxis = (NumberAxis) plot.getDomainAxis();
        domainAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());

        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        double minVal = Double.MAX_VALUE;
        double maxVal = Double.MIN_VALUE;
        for (Double val : data) {
            if (val != null) {
                minVal = Math.min(minVal, val);
                maxVal = Math.max(maxVal, val);
            }
        }
        if (minVal != Double.MAX_VALUE) {
            double padding = (maxVal - minVal) * 0.1;
            if (padding == 0) padding = 1;
            rangeAxis.setRange(minVal - padding, maxVal + padding);
        }

        chart.getRenderingHints().add(new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON));
        chart.getRenderingHints().add(new RenderingHints(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON));

        panel.add(new ChartPanel(chart) {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(580, 380);
            }
        }, BorderLayout.CENTER);

        return panel;
    }

    private String formatRoute(java.util.List<Integer> route) {
        if (route == null || route.isEmpty()) return "空";

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < route.size(); i++) {
            int id = route.get(i);
            if (id < 0) {
                sb.append("发射点" + (-id));
            } else if (id == 0) {
                sb.append("仓库");
            } else {
                sb.append("客户" + id);
            }
            if (i < route.size() - 1) {
                sb.append(" -> ");
            }
        }
        return sb.toString();
    }

    private JPanel createRouteMapPanel(GARunner.GAResult result) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        if (config == null || config.problem == null) {
            panel.add(new JLabel("暂无数据，请先运行算法", SwingConstants.CENTER), BorderLayout.CENTER);
            return panel;
        }

        if (result == null || result.bestChromosome == null) {
            panel.add(new JLabel("请先运行算法获取最优解", SwingConstants.CENTER), BorderLayout.CENTER);
            return panel;
        }

        JPanel mapWrapper = new JPanel(new BorderLayout());
        mapWrapper.setBorder(BorderFactory.createTitledBorder(""));

        if (mapPanel != null) {
            mapPanel.setConfig(config);
            mapPanel.setResult(result);
            mapPanel.setShowRoutes(true);
            mapPanel.setMode("结果详情");
            SwingUtilities.invokeLater(() -> mapPanel.autoFit());
            mapWrapper.add(mapPanel, BorderLayout.CENTER);
        } else {
            MapPreviewPanel routeMap = new MapPreviewPanel();
            routeMap.setConfig(config);
            routeMap.setResult(result);
            routeMap.setShowRoutes(true);
            routeMap.setMode("结果详情");
            SwingUtilities.invokeLater(() -> routeMap.autoFit());
            mapWrapper.add(routeMap, BorderLayout.CENTER);
        }

        JScrollPane scrollPane = new JScrollPane(mapWrapper);
        scrollPane.setPreferredSize(new Dimension(0, 450));
        scrollPane.getViewport().setBackground(Color.WHITE);

        JLabel tipLabel = new JLabel("滚轮缩放 | 右键拖拽平移", SwingConstants.RIGHT);
        tipLabel.setFont(new Font("微软雅黑", Font.PLAIN, 11));
        tipLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 10));

        JPanel tipPanel = new JPanel(new BorderLayout());
        tipPanel.add(scrollPane, BorderLayout.CENTER);
        tipPanel.add(tipLabel, BorderLayout.SOUTH);

        panel.add(tipPanel, BorderLayout.CENTER);

        return panel;
    }

    private JTextField createNumberField(JPanel parent, String label, String defaultVal) {
        parent.add(new JLabel(label));
        JTextField field = new JTextField(defaultVal);
        parent.add(field);
        return field;
    }

    public void setConfig(Config config) {
        this.config = config;
        loadConfig();
    }

    public void setMapPanel(MapPreviewPanel mapPanel) {
        this.mapPanel = mapPanel;
        if (mapPanel != null) {
            mapPanel.setConfigChangeListener(this);
        }
        setupTableListeners();
    }

    public Config getConfig() {
        saveToConfig();
        return config;
    }

    private void setupTableListeners() {
        customerTableModel.addTableModelListener(e -> {
            if (e.getType() == TableModelEvent.UPDATE && mapPanel != null) {
                int row = e.getFirstRow();
                int col = e.getColumn();
                if (col == 1 || col == 2) {
                    try {
                        Integer id = (Integer) customerTableModel.getValueAt(row, 0);
                        Double x = Double.parseDouble(customerTableModel.getValueAt(row, 1).toString());
                        Double y = Double.parseDouble(customerTableModel.getValueAt(row, 2).toString());
                        if (config != null && config.problem != null && config.problem.customers != null) {
                            for (Config.Customer c : config.problem.customers) {
                                if (c.id == id) {
                                    c.x = x;
                                    c.y = y;
                                    break;
                                }
                            }
                            mapPanel.setConfig(config);
                        }
                    } catch (Exception ex) {
                    }
                }
                // 修改需求量列时，重新渲染表格以更新超载检测
                if (col == 3) {
                    customerTable.repaint();
                }
            }
        });

        depotTableModel.addTableModelListener(e -> {
            if (e.getType() == TableModelEvent.UPDATE && mapPanel != null) {
                int row = e.getFirstRow();
                int col = e.getColumn();
                if (col == 1 || col == 2) {
                    try {
                        Integer id = (Integer) depotTableModel.getValueAt(row, 0);
                        Double x = Double.parseDouble(depotTableModel.getValueAt(row, 1).toString());
                        Double y = Double.parseDouble(depotTableModel.getValueAt(row, 2).toString());
                        if (config != null && config.problem != null && config.problem.depots != null) {
                            for (Config.Depot d : config.problem.depots) {
                                if (d.id == id) {
                                    d.x = x;
                                    d.y = y;
                                    break;
                                }
                            }
                            mapPanel.setConfig(config);
                        }
                    } catch (Exception ex) {
                    }
                }
            }
        });
    }

    private void loadConfig() {
        if (config == null || config.problem == null) return;

        depotTableModel.setRowCount(0);
        if (config.problem.depots != null) {
            for (Config.Depot d : config.problem.depots) {
                depotTableModel.addRow(new Object[]{d.id, d.x, d.y});
            }
        }

        customerTableModel.setRowCount(0);
        if (config.problem.customers != null) {
            for (Config.Customer c : config.problem.customers) {
                customerTableModel.addRow(new Object[]{
                    c.id, c.x, c.y, c.demand,
                    c.timeWindow != null ? c.timeWindow[0] : 0,
                    c.timeWindow != null ? c.timeWindow[1] : 120,
                    c.serviceTime
                });
            }
        }

        if (config.vehicles != null && config.vehicles.trucks != null) {
            tfTruckCount.setText(String.valueOf(config.vehicles.trucks.count));
            tfTruckCapacity.setText(String.valueOf(config.vehicles.trucks.capacity));
            tfTruckSpeed.setText(String.valueOf(config.vehicles.trucks.speed));
            tfDronesPerTruck.setText(String.valueOf(config.vehicles.trucks.dronesPerTruck));
            tfTruckRange.setText(String.valueOf(config.vehicles.trucks.range));
        }

        if (config.vehicles != null && config.vehicles.drones != null) {
            // tfDroneCount 已废弃，总无人机数由 卡车数×每辆搭载数 自动计算
            tfDroneRange.setText(String.valueOf(config.vehicles.drones.range));
            tfDroneSpeed.setText(String.valueOf(config.vehicles.drones.speed));
            tfDronePayload.setText(String.valueOf(config.vehicles.drones.maxPayload));
            tfDroneRechargeTime.setText(String.valueOf(config.vehicles.drones.rechargeTime));
            tfDroneServiceTime.setText(String.valueOf(config.vehicles.drones.serviceTime));
        }

        if (config.genetic != null) {
            tfPopSize.setText(String.valueOf(config.genetic.populationSize));
            tfMaxGen.setText(String.valueOf(config.genetic.maxGenerations));
            slCrossover.setValue((int)(config.genetic.crossoverRate * 100));
            slMutation.setValue((int)(config.genetic.mutationRate * 100));
            slElite.setValue((int)(config.genetic.eliteRate * 100));
            tfCrossover.setText(String.valueOf(config.genetic.crossoverRate));
            tfMutation.setText(String.valueOf(config.genetic.mutationRate));
            tfElite.setText(String.valueOf(config.genetic.eliteRate));
            cbTournament.setSelectedItem(config.genetic.tournamentSize);
        }

        if (config.launchPoint != null) {
            tfLaunchPointCount.setText(String.valueOf(config.launchPoint.launchPointCount));
        }

        // 加载惩罚参数
        if (config.penalty != null) {
            cbPenaltyEnabled.setSelected(config.penalty.enabled);
            tfTimeWindowEarly.setText(String.valueOf(config.penalty.timeWindowEarly));
            tfTimeWindowLate.setText(String.valueOf(config.penalty.timeWindowLate));
            tfDroneRangePenalty.setText(String.valueOf(config.penalty.droneRangePenalty));
            tfDronePayloadPenalty.setText(String.valueOf(config.penalty.dronePayloadPenalty));
            tfWaitTimePenalty.setText(String.valueOf(config.penalty.waitTimePenalty));
            tfDelayTimePenalty.setText(String.valueOf(config.penalty.delayTimePenalty));
            tfTruckRangeTimePenalty.setText(String.valueOf(config.penalty.truckRangeTimePenalty));
            tfTruckRangeDistancePenalty.setText(String.valueOf(config.penalty.truckRangeDistancePenalty));
            tfDistancePenalty.setText(String.valueOf(config.penalty.distancePenalty));
        }

        if (config.normalization != null) {
            tfMaxTime.setText(String.valueOf(config.normalization.maxTime));
        }
    }

    private void syncDepotToConfig() {
        if (config == null) config = new Config();
        if (config.problem == null) config.problem = new Config.Problem();
        if (config.problem.depots == null) config.problem.depots = new java.util.ArrayList<>();
        config.problem.depots.clear();
        for (int i = 0; i < depotTableModel.getRowCount(); i++) {
            Config.Depot d = new Config.Depot();
            d.id = i;
            d.x = Double.parseDouble(depotTableModel.getValueAt(i, 1).toString());
            d.y = Double.parseDouble(depotTableModel.getValueAt(i, 2).toString());
            config.problem.depots.add(d);
        }
    }

    private void syncCustomerToConfig() {
        if (config == null) config = new Config();
        if (config.problem == null) config.problem = new Config.Problem();
        if (config.problem.customers == null) config.problem.customers = new java.util.ArrayList<>();
        config.problem.customers.clear();
        for (int i = 0; i < customerTableModel.getRowCount(); i++) {
            Config.Customer c = new Config.Customer();
            c.id = i + 1;
            c.x = Double.parseDouble(customerTableModel.getValueAt(i, 1).toString());
            c.y = Double.parseDouble(customerTableModel.getValueAt(i, 2).toString());
            c.demand = Double.parseDouble(customerTableModel.getValueAt(i, 3).toString());

            Object twStartObj = customerTableModel.getValueAt(i, 4);
            Object twEndObj = customerTableModel.getValueAt(i, 5);
            Object serviceObj = customerTableModel.getValueAt(i, 6);

            String twStartStr = twStartObj != null ? twStartObj.toString() : "0";
            String twEndStr = twEndObj != null ? twEndObj.toString() : "120";
            String serviceStr = serviceObj != null ? serviceObj.toString() : "5";

            c.timeWindow = new double[]{
                SolomonLoader.parseTime(twStartStr, false),
                SolomonLoader.parseTime(twEndStr, false)
            };

            if (serviceStr.contains("秒") || serviceStr.contains("分")) {
                int seconds = SolomonLoader.parseTime(serviceStr, true);
                c.serviceTime = seconds / 60.0;
            } else {
                c.serviceTime = Double.parseDouble(serviceStr);
            }

            config.problem.customers.add(c);
        }
    }

    private void saveToConfig() {
        if (config == null || config.problem == null) return;

        config.problem.depots.clear();
        for (int i = 0; i < depotTableModel.getRowCount(); i++) {
            Config.Depot d = new Config.Depot();
            d.id = i;
            d.x = Double.parseDouble(depotTableModel.getValueAt(i, 1).toString());
            d.y = Double.parseDouble(depotTableModel.getValueAt(i, 2).toString());
            config.problem.depots.add(d);
        }

        config.problem.customers.clear();
        for (int i = 0; i < customerTableModel.getRowCount(); i++) {
            Config.Customer c = new Config.Customer();
            c.id = i + 1;
            c.x = Double.parseDouble(customerTableModel.getValueAt(i, 1).toString());
            c.y = Double.parseDouble(customerTableModel.getValueAt(i, 2).toString());
            c.demand = Double.parseDouble(customerTableModel.getValueAt(i, 3).toString());

            Object twStartObj = customerTableModel.getValueAt(i, 4);
            Object twEndObj = customerTableModel.getValueAt(i, 5);
            Object serviceObj = customerTableModel.getValueAt(i, 6);

            String twStartStr = twStartObj != null ? twStartObj.toString() : "0";
            String twEndStr = twEndObj != null ? twEndObj.toString() : "120";
            String serviceStr = serviceObj != null ? serviceObj.toString() : "5";

            c.timeWindow = new double[]{
                SolomonLoader.parseTime(twStartStr, false),
                SolomonLoader.parseTime(twEndStr, false)
            };

            if (serviceStr.contains("秒") || serviceStr.contains("分")) {
                int seconds = SolomonLoader.parseTime(serviceStr, true);
                c.serviceTime = seconds / 60.0;
            } else {
                c.serviceTime = Double.parseDouble(serviceStr);
            }

            config.problem.customers.add(c);
        }

        try {
            config.vehicles.trucks.count = Integer.parseInt(tfTruckCount.getText());
            config.vehicles.trucks.capacity = Double.parseDouble(tfTruckCapacity.getText());
            config.vehicles.trucks.speed = Double.parseDouble(tfTruckSpeed.getText());
            config.vehicles.trucks.dronesPerTruck = Integer.parseInt(tfDronesPerTruck.getText());
            config.vehicles.trucks.range = Double.parseDouble(tfTruckRange.getText());
        } catch (Exception e) {
            config.vehicles.trucks.count = 2;
            config.vehicles.trucks.capacity = 100;
            config.vehicles.trucks.speed = 40;
            config.vehicles.trucks.range = 0;
            config.vehicles.trucks.dronesPerTruck = 2;
        }

        try {
            // tfDroneCount 已废弃，总无人机数由 卡车数×每辆搭载数 自动计算
            config.vehicles.drones.range = Double.parseDouble(tfDroneRange.getText());
            config.vehicles.drones.speed = Double.parseDouble(tfDroneSpeed.getText());
            config.vehicles.drones.maxPayload = Double.parseDouble(tfDronePayload.getText());
            config.vehicles.drones.rechargeTime = Double.parseDouble(tfDroneRechargeTime.getText());
            config.vehicles.drones.serviceTime = Double.parseDouble(tfDroneServiceTime.getText());
        } catch (Exception e) {
            // tfDroneCount 已废弃，总无人机数由 卡车数×每辆搭载数 自动计算
            config.vehicles.drones.range = 50;
            config.vehicles.drones.speed = 60;
            config.vehicles.drones.maxPayload = 50;
            config.vehicles.drones.rechargeTime = 10;
            config.vehicles.drones.serviceTime = 1.5;
        }

        try {
            config.genetic.populationSize = Integer.parseInt(tfPopSize.getText());
            config.genetic.maxGenerations = Integer.parseInt(tfMaxGen.getText());
            config.genetic.crossoverRate = Double.parseDouble(tfCrossover.getText());
            config.genetic.mutationRate = Double.parseDouble(tfMutation.getText());
            config.genetic.eliteRate = Double.parseDouble(tfElite.getText());
            config.genetic.tournamentSize = (Integer) cbTournament.getSelectedItem();
        } catch (Exception e) {
            config.genetic.populationSize = 100;
            config.genetic.maxGenerations = 500;
            config.genetic.crossoverRate = 0.8;
            config.genetic.mutationRate = 0.15;
            config.genetic.eliteRate = 0.1;
            config.genetic.tournamentSize = 3;
        }

        if (config.launchPoint == null) {
            config.launchPoint = new Config.LaunchPointConfig();
        }
        try {
            config.launchPoint.launchPointCount = Integer.parseInt(tfLaunchPointCount.getText());
        } catch (Exception e) {
            config.launchPoint.launchPointCount = 0;
        }

        // 确保penalty配置存在
        if (config.penalty == null) {
            config.penalty = new Config.PenaltyConfig();
        }

        // 保存惩罚参数
        try {
            config.penalty.enabled = cbPenaltyEnabled.isSelected();
            config.penalty.timeWindowEarly = Double.parseDouble(tfTimeWindowEarly.getText());
            config.penalty.timeWindowLate = Double.parseDouble(tfTimeWindowLate.getText());
            config.penalty.droneRangePenalty = Double.parseDouble(tfDroneRangePenalty.getText());
            config.penalty.dronePayloadPenalty = Double.parseDouble(tfDronePayloadPenalty.getText());
            config.penalty.waitTimePenalty = Double.parseDouble(tfWaitTimePenalty.getText());
            config.penalty.delayTimePenalty = Double.parseDouble(tfDelayTimePenalty.getText());
            config.penalty.truckRangeTimePenalty = Double.parseDouble(tfTruckRangeTimePenalty.getText());
            config.penalty.truckRangeDistancePenalty = Double.parseDouble(tfTruckRangeDistancePenalty.getText());
            config.penalty.distancePenalty = Double.parseDouble(tfDistancePenalty.getText());
        } catch (Exception e) {
            if (config.penalty == null) config.penalty = new Config.PenaltyConfig();
            config.penalty.enabled = true;
            config.penalty.timeWindowEarly = 2.0;
            config.penalty.timeWindowLate = 50.0;
            config.penalty.droneRangePenalty = 50.0;
            config.penalty.dronePayloadPenalty = 20.0;
            config.penalty.waitTimePenalty = 2.0;
            config.penalty.delayTimePenalty = 50.0;
            config.penalty.truckRangeTimePenalty = 5.0;
            config.penalty.truckRangeDistancePenalty = 50.0;
            config.penalty.distancePenalty = 10.0;
        }

        try {
            if (config.normalization == null) {
                config.normalization = new Config.NormalizationConfig();
            }
            config.normalization.maxTime = Double.parseDouble(tfMaxTime.getText());
        } catch (Exception e) {
            if (config.normalization == null) {
                config.normalization = new Config.NormalizationConfig();
            }
            config.normalization.maxTime = 10000.0;
        }
    }

    public void showForm(String nodeName) {
        cardLayout.show(cardPanel, nodeName);
    }
    
    public void updateRealtimeProgress(GARunner.ProgressInfo progress) {
        if (mapPanel != null && config != null) {
            mapPanel.setConfig(config);
            mapPanel.clearRealtime();
            mapPanel.setRealtimeProgress(progress);
        }
    }

    @Override
    public void onCustomerPositionChanged(int customerId, double x, double y) {
        for (int i = 0; i < customerTableModel.getRowCount(); i++) {
            Integer id = (Integer) customerTableModel.getValueAt(i, 0);
            if (id != null && id == customerId) {
                customerTableModel.setValueAt(Math.round(x * 100) / 100.0, i, 1);
                customerTableModel.setValueAt(Math.round(y * 100) / 100.0, i, 2);
                break;
            }
        }
    }

    @Override
    public void onDepotPositionChanged(int depotId, double x, double y) {
        for (int i = 0; i < depotTableModel.getRowCount(); i++) {
            Integer id = (Integer) depotTableModel.getValueAt(i, 0);
            if (id != null && id == depotId) {
                depotTableModel.setValueAt(Math.round(x * 100) / 100.0, i, 1);
                depotTableModel.setValueAt(Math.round(y * 100) / 100.0, i, 2);
                break;
            }
        }
    }

    private void ensureConfigInitialized() {
        if (config == null) {
            config = new Config();
        }
        if (config.problem == null) {
            config.problem = new Config.Problem();
        }
        if (config.problem.depots == null) {
            config.problem.depots = new ArrayList<>();
        }
        if (config.problem.customers == null) {
            config.problem.customers = new ArrayList<>();
        }
        if (config.vehicles == null) {
            config.vehicles = new Config.Vehicles();
        }
        if (config.vehicles.trucks == null) {
            config.vehicles.trucks = new Config.Trucks();
            config.vehicles.trucks.count = 2;
            config.vehicles.trucks.capacity = 2000;
            config.vehicles.trucks.speed = 40;
            config.vehicles.trucks.dronesPerTruck = 2;
            config.vehicles.trucks.range = 1000;
        }
        if (config.vehicles.drones == null) {
            config.vehicles.drones = new Config.Drones();
            // count 已废弃，总无人机数由 卡车数×每辆搭载数 自动计算
            config.vehicles.drones.range = 50;
            config.vehicles.drones.speed = 60;
            config.vehicles.drones.maxPayload = 50;
            config.vehicles.drones.rechargeTime = 10;
        }
        if (config.optimization == null) {
            config.optimization = new Config.Optimization();
            config.optimization.objective = "both";
            config.optimization.costWeight = 0.5;
            config.optimization.timeWeight = 0.5;
        }
        if (config.genetic == null) {
            config.genetic = new Config.Genetic();
            config.genetic.populationSize = 100;
            config.genetic.maxGenerations = 500;
            config.genetic.crossoverRate = 0.8;
            config.genetic.mutationRate = 0.15;
            config.genetic.eliteRate = 0.1;
            config.genetic.tournamentSize = 3;
        }
    }

    public void applySolomonData(SolomonLoader.SolomonData data) {
        if (data == null || data.config == null) return;

        ensureConfigInitialized();

        config.problem.depots.clear();
        for (Config.Depot d : data.config.problem.depots) {
            config.problem.depots.add(d);
        }

        config.problem.customers.clear();
        for (Config.Customer c : data.config.problem.customers) {
            config.problem.customers.add(c);
        }

        if (data.config.vehicles != null && data.config.vehicles.trucks != null) {
            config.vehicles.trucks.count = data.config.vehicles.trucks.count;
            config.vehicles.trucks.capacity = data.config.vehicles.trucks.capacity;
        }

        loadConfig();

        if (mapPanel != null) {
            mapPanel.setConfig(config);
        }

        JOptionPane.showMessageDialog(this,
            "已成功导入算例 " + data.name + "\n" +
            "仓库: " + config.problem.depots.size() + " 个\n" +
            "客户: " + config.problem.customers.size() + " 个",
            "导入成功",
            JOptionPane.INFORMATION_MESSAGE);
    }

    private void refreshCustomerTable() {
        if (customerTable != null) {
            customerTable.repaint();
        }
    }
}
