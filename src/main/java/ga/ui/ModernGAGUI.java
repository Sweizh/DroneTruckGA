package ga.ui;

import ga.Config;
import ga.ConfigLoader;
import ga.GALogger;
import ga.GARunner;
import ga.SolomonLoader;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.ArrayList;

public class ModernGAGUI extends JFrame {
    private Config currentConfig;
    private GARunner.GAResult lastResult;
    private GARunner.MultiRunResult lastMultiResult;
    private GASwingWorker gaWorker;
    private MultiRunSwingWorker multiRunWorker;

    private JPanel centerPanel;
    private JSplitPane mainSplit;
    private JSplitPane rightSplit;
    private NavigationPanel navPanel;
    private MapPreviewPanel mapPanel;
    private ConfigFormPanel formPanel;
    private StatusBarPanel statusBar;
    private JButton runButton;
    private JButton repeatButton;
    private JButton stopButton;
    private JButton saveConfigButton;
    private JCheckBox logCheckBox;
    private JTextField tfRepeatCount;
    private volatile boolean isRunning = false;
    private volatile boolean isRepeating = false;

    public ModernGAGUI() {
        initComponents();
        loadDefaultConfig();
    }

    private void initComponents() {
        setTitle("卡车-无人机协同配送路径优化系统");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setPreferredSize(new Dimension(1200, 800));

        navPanel = new NavigationPanel();

        rightSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        rightSplit.setDividerLocation(400);
        rightSplit.setDividerSize(8);

        mapPanel = new MapPreviewPanel();
        rightSplit.setTopComponent(new JPanel());
        rightSplit.setDividerLocation(0.0);
        rightSplit.setDividerSize(0);

        formPanel = new ConfigFormPanel();
        formPanel.setMapPanel(mapPanel);
        rightSplit.setBottomComponent(formPanel);

        mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainSplit.setLeftComponent(navPanel);
        mainSplit.setRightComponent(rightSplit);
        mainSplit.setDividerLocation(200);
        mainSplit.setDividerSize(8);

        centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(mainSplit, BorderLayout.CENTER);

        JPanel southPanel = new JPanel(new BorderLayout());

        JPanel buttonPanel = new JPanel(new BorderLayout());
        JPanel leftButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        JPanel rightButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));

        runButton = new JButton("开始运行");
        repeatButton = new JButton("循环运行");
        stopButton = new JButton("停止");
        stopButton.setEnabled(false);
        tfRepeatCount = new JTextField("5", 5);
        tfRepeatCount.setHorizontalAlignment(JTextField.CENTER);

        // 日志输出开关
        logCheckBox = new JCheckBox("输出日志");
        logCheckBox.setSelected(false);
        logCheckBox.setToolTipText("勾选后生成日志文件");

        // 保存配置按钮
        saveConfigButton = new JButton("保存配置");
        saveConfigButton.setToolTipText("将当前配置保存到config.json文件");

        leftButtonPanel.add(runButton);
        leftButtonPanel.add(repeatButton);
        leftButtonPanel.add(new JLabel("次数:"));
        leftButtonPanel.add(tfRepeatCount);
        leftButtonPanel.add(stopButton);
        leftButtonPanel.add(logCheckBox);

        rightButtonPanel.add(saveConfigButton);

        buttonPanel.add(leftButtonPanel, BorderLayout.WEST);
        buttonPanel.add(rightButtonPanel, BorderLayout.EAST);
        southPanel.add(buttonPanel, BorderLayout.NORTH);

        statusBar = new StatusBarPanel();
        southPanel.add(statusBar, BorderLayout.CENTER);

        add(centerPanel, BorderLayout.CENTER);
        add(southPanel, BorderLayout.SOUTH);

        navPanel.addTreeSelectionListener(e -> {
            String selected = navPanel.getSelectedNode();
            if (selected != null) {
                formPanel.showForm(selected);
                mapPanel.setMode(selected);

                boolean showMap = "仓库管理".equals(selected) || "客户管理".equals(selected);

                if (showMap) {
                    rightSplit.setTopComponent(mapPanel);
                    rightSplit.setDividerLocation(0.4);
                    rightSplit.setDividerSize(8);
                } else {
                    rightSplit.setTopComponent(new JPanel());
                    rightSplit.setDividerLocation(0.0);
                    rightSplit.setDividerSize(0);
                }
            }
        });

        setupEventListeners();

        pack();
        setLocationRelativeTo(null);
    }

    private void setupEventListeners() {
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (gaWorker != null && !gaWorker.isDone()) {
                    gaWorker.cancel(true);
                }
                if (multiRunWorker != null && !multiRunWorker.isDone()) {
                    multiRunWorker.cancel(true);
                }
            }
        });

        runButton.addActionListener(e -> runGA());
        repeatButton.addActionListener(e -> runGARepeat());
        stopButton.addActionListener(e -> stopGA());
        saveConfigButton.addActionListener(e -> saveConfig());
        logCheckBox.addActionListener(e -> GALogger.setFileEnabled(logCheckBox.isSelected()));
    }

    /**
     * 保存当前配置到config.json文件
     */
    private void saveConfig() {
        // 先从表单获取最新配置
        currentConfig = formPanel.getConfig();
        
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            mapper.writerWithDefaultPrettyPrinter().writeValue(new File("config.json"), currentConfig);
            JOptionPane.showMessageDialog(this, "配置已保存到 config.json", "保存成功", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "保存配置失败: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadDefaultConfig() {
        // 优先从config.json加载配置
        try {
            Config loadedConfig = ConfigLoader.load();
            if (loadedConfig != null) {
                currentConfig = loadedConfig;
                
                // 检查是否是旧配置（5个或更少客户），如果是则使用C101算例
                if (loadedConfig.problem != null && loadedConfig.problem.customers != null &&
                    loadedConfig.problem.customers.size() <= 5) {
                    loadDefaultC101(currentConfig);
                }
                
                // 确保launchPoint配置存在（向后兼容旧配置文件）
                if (currentConfig.launchPoint == null) {
                    currentConfig.launchPoint = new Config.LaunchPointConfig();
                }
                
                formPanel.setConfig(currentConfig);
                mapPanel.setConfig(currentConfig);
                updateStatusBar();
                return;
            }
        } catch (Exception e) {
            // config.json不存在或格式错误，使用默认配置
        }
        
        // 如果加载失败，使用默认配置
        currentConfig = createDefaultConfig();
        formPanel.setConfig(currentConfig);
        mapPanel.setConfig(currentConfig);
        updateStatusBar();
    }

    private Config createDefaultConfig() {
        Config config = new Config();

        config.problem = new Config.Problem();
        config.problem.depots = new ArrayList<>();
        config.problem.depots.add(new Config.Depot());
        config.problem.depots.get(0).id = 0;
        config.problem.depots.get(0).x = 50.0;
        config.problem.depots.get(0).y = 50.0;
        config.problem.depots.add(new Config.Depot());
        config.problem.depots.get(1).id = 1;
        config.problem.depots.get(1).x = 150.0;
        config.problem.depots.get(1).y = 150.0;

        config.problem.customers = new ArrayList<>();
        double[][] customerData = {
            {20, 30, 2}, {80, 20, 3}, {30, 80, 2}, {70, 70, 3}, {140, 160, 2}
        };
        for (int i = 0; i < customerData.length; i++) {
            Config.Customer c = new Config.Customer();
            c.id = i + 1;
            c.x = customerData[i][0];
            c.y = customerData[i][1];
            c.demand = customerData[i][2];
            c.depotId = i < 3 ? 0 : 1;
            c.timeWindow = new double[]{0, 120};
            c.serviceTime = 5;
            config.problem.customers.add(c);
        }

        config.vehicles = new Config.Vehicles();
        config.vehicles.trucks = new Config.Trucks();
        config.vehicles.trucks.count = 2;
        config.vehicles.trucks.capacity = 2000;
        config.vehicles.trucks.speed = 40;
        config.vehicles.trucks.dronesPerTruck = 2;
        config.vehicles.trucks.range = 1000;

        config.vehicles.drones = new Config.Drones();
        // count 已废弃，总无人机数由 卡车数×每辆搭载数 自动计算
        config.vehicles.drones.range = 50;
        config.vehicles.drones.speed = 60;
        config.vehicles.drones.maxPayload = 50;
        config.vehicles.drones.rechargeTime = 10;

        config.optimization = new Config.Optimization();
        config.optimization.objective = "time";
        config.optimization.costWeight = 0.5;
        config.optimization.timeWeight = 0.5;

        config.genetic = new Config.Genetic();
        config.genetic.populationSize = 100;
        config.genetic.maxGenerations = 500;
        config.genetic.crossoverRate = 0.8;
        config.genetic.mutationRate = 0.15;
        config.genetic.eliteRate = 0.1;
        config.genetic.tournamentSize = 3;

        // 发射点配置
        config.launchPoint = new Config.LaunchPointConfig();
        config.launchPoint.initialOffset = 5.0;
        config.launchPoint.mutationRange = 30.0;
        config.launchPoint.historyInfluence = 0.5;
        config.launchPoint.historySize = 20;
        config.launchPoint.adaptiveRange = true;
        config.launchPoint.successThreshold = 0.0001;

        // 加载默认C101算例作为初始配置
        loadDefaultC101(config);

        return config;
    }

    private void loadDefaultC101(Config config) {
        try {
            String basePath = getSolomonBasePath();
            File c101File = new File(basePath, "solomon_25/C101.txt");
            if (c101File.exists()) {
                SolomonLoader.SolomonData solomonData = SolomonLoader.load(c101File.getAbsolutePath());
                if (solomonData != null && solomonData.config != null) {
                    // 保留卡车和无人机参数，只替换客户和仓库配置
                    config.problem.depots.clear();
                    for (ga.Config.Depot d : solomonData.config.problem.depots) {
                        config.problem.depots.add(d);
                    }
                    config.problem.customers.clear();
                    for (ga.Config.Customer c : solomonData.config.problem.customers) {
                        config.problem.customers.add(c);
                    }
                    if (solomonData.config.vehicles != null && solomonData.config.vehicles.trucks != null) {
                        config.vehicles.trucks.count = solomonData.config.vehicles.trucks.count;
                        config.vehicles.trucks.capacity = solomonData.config.vehicles.trucks.capacity;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("加载C101默认算例失败: " + e.getMessage());
        }
    }

    private String getSolomonBasePath() {
        try {
            File jarFile = new File(
                ModernGAGUI.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI()
            );
            String jarDir = jarFile.getParentFile().getAbsolutePath();
            File solomonDir = new File(jarDir, "solomon");
            if (solomonDir.exists()) {
                return solomonDir.getAbsolutePath();
            }
            File projectDir = jarFile.getParentFile().getParentFile();
            File solomonInProject = new File(projectDir, "solomon");
            if (solomonInProject.exists()) {
                return solomonInProject.getAbsolutePath();
            }
            return "solomon";
        } catch (Exception e) {
            return "solomon";
        }
    }

    private void updateStatusBar() {
        int depotCount = 0, customerCount = 0, truckCount = 0, droneCount = 0;
        if (currentConfig != null) {
            if (currentConfig.problem != null) {
                if (currentConfig.problem.depots != null) depotCount = currentConfig.problem.depots.size();
                if (currentConfig.problem.customers != null) customerCount = currentConfig.problem.customers.size();
            }
            if (currentConfig.vehicles != null) {
                if (currentConfig.vehicles.trucks != null) truckCount = currentConfig.vehicles.trucks.count;
                if (currentConfig.vehicles.drones != null) droneCount = currentConfig.vehicles.drones.count;
            }
        }
        statusBar.setStatusWithInfo("就绪", depotCount, customerCount, truckCount, droneCount);
    }

    private boolean checkConfig() {
        currentConfig = formPanel.getConfig();
        if (currentConfig == null || currentConfig.problem == null ||
            currentConfig.problem.customers == null || currentConfig.problem.customers.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请先配置客户信息！", "错误", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }

    private void startRunUI() {
        isRunning = true;
        isRepeating = false;
        runButton.setEnabled(false);
        repeatButton.setEnabled(false);
        stopButton.setEnabled(true);
        tfRepeatCount.setEnabled(false);
        statusBar.setRunning(true);
        formPanel.showForm("结果详情");
        mapPanel.setConfig(currentConfig);
        mapPanel.setShowRoutes(true);
        lastResult = null;
        lastMultiResult = null;
    }

    private void runGA() {
        if (!checkConfig()) return;
        startRunUI();

        gaWorker = new GASwingWorker(currentConfig);
        gaWorker.execute();
    }

    private void runGARepeat() {
        int repeatCount;
        try {
            repeatCount = Integer.parseInt(tfRepeatCount.getText());
            if (repeatCount < 1) repeatCount = 1;
            if (repeatCount > 100) repeatCount = 100;
        } catch (NumberFormatException e) {
            repeatCount = 5;
        }
        tfRepeatCount.setText(String.valueOf(repeatCount));

        if (!checkConfig()) return;
        startRunUI();

        multiRunWorker = new MultiRunSwingWorker(currentConfig, repeatCount);
        multiRunWorker.execute();
    }

    private void stopGA() {
        isRunning = false;
        isRepeating = false;
        if (gaWorker != null && !gaWorker.isDone()) {
            gaWorker.cancel(true);
        }
        if (multiRunWorker != null && !multiRunWorker.isDone()) {
            multiRunWorker.cancel(true);
        }
        runButton.setEnabled(true);
        repeatButton.setEnabled(true);
        stopButton.setEnabled(false);
        tfRepeatCount.setEnabled(true);
        statusBar.setStatus("已停止");
        statusBar.setRunning(false);
    }

    private void onRunCompleted() {
        isRunning = false;
        isRepeating = false;
        runButton.setEnabled(true);
        repeatButton.setEnabled(true);
        stopButton.setEnabled(false);
        tfRepeatCount.setEnabled(true);
        statusBar.setRunning(false);
    }

    private class GASwingWorker extends SwingWorker<GARunner.GAResult, GARunner.ProgressInfo> {
        private final Config config;

        public GASwingWorker(Config config) {
            this.config = config;
        }

        @Override
        protected GARunner.GAResult doInBackground() {
            return GARunner.runGA(config, progress -> {
                publish(progress);
            }, () -> !isRunning && !isCancelled());
        }

        @Override
        protected void process(java.util.List<GARunner.ProgressInfo> chunks) {
            if (chunks.isEmpty() || isCancelled()) return;

            GARunner.ProgressInfo progress = chunks.get(chunks.size() - 1);

            int currentGen = progress.generation;
            double time = progress.currentTime;

            statusBar.updateProgress(currentGen, config.genetic.maxGenerations, time);

            if (lastResult == null) {
                lastResult = new GARunner.GAResult();
            }

            lastResult.timeCurve = new ArrayList<>(progress.timeCurve);
            lastResult.bestTime = time;

            formPanel.updateSingleResult(lastResult);
            formPanel.updateRealtimeProgress(progress);
        }

        @Override
        protected void done() {
            try {
                if (isCancelled()) {
                    onRunCompleted();
                    return;
                }

                GARunner.GAResult result = get();

                lastResult = result;
                statusBar.updateResult(result.bestTime);
                mapPanel.setResult(result);
                formPanel.updateSingleResult(result);

            } catch (Exception e) {
                if (!isCancelled()) {
                    JOptionPane.showMessageDialog(ModernGAGUI.this, "运行错误: " + e.getMessage(),
                        "错误", JOptionPane.ERROR_MESSAGE);
                    e.printStackTrace();
                }
            } finally {
                onRunCompleted();
            }
        }
    }

    private class MultiRunSwingWorker extends SwingWorker<GARunner.MultiRunResult, GARunner.ProgressInfo> {
        private final Config config;
        private final int repeatCount;

        public MultiRunSwingWorker(Config config, int repeatCount) {
            this.config = config;
            this.repeatCount = repeatCount;
        }

        @Override
        protected GARunner.MultiRunResult doInBackground() {
            isRepeating = true;

            return GARunner.runGARepeats(config, repeatCount, progress -> {
                publish(progress);
            }, runNum -> {
                SwingUtilities.invokeLater(() -> {
                    statusBar.setStatus("循环: " + runNum + "/" + repeatCount);
                });
            }, () -> !isRunning && !isCancelled());
        }

        @Override
        protected void process(java.util.List<GARunner.ProgressInfo> chunks) {
            if (chunks.isEmpty() || isCancelled()) return;

            GARunner.ProgressInfo progress = chunks.get(chunks.size() - 1);
            int currentGen = progress.generation;
            double time = progress.currentTime;

            statusBar.updateProgress(currentGen, config.genetic.maxGenerations, time);

            if (lastResult == null) {
                lastResult = new GARunner.GAResult();
            }

            lastResult.timeCurve = new ArrayList<>(progress.timeCurve);
            lastResult.bestTime = time;

            formPanel.updateMultiProgress(lastResult);
            mapPanel.setResult(lastResult);
        }

        @Override
        protected void done() {
            try {
                if (isCancelled()) {
                    onRunCompleted();
                    return;
                }

                GARunner.MultiRunResult result = get();
                lastMultiResult = result;
                lastResult = result.bestOverall;

                statusBar.updateResult(result.bestOverall.bestTime);

                mapPanel.setResult(result.bestOverall);
                formPanel.updateMultiResult(result);

            } catch (Exception e) {
                if (!isCancelled()) {
                    JOptionPane.showMessageDialog(ModernGAGUI.this, "运行错误: " + e.getMessage(),
                        "错误", JOptionPane.ERROR_MESSAGE);
                    e.printStackTrace();
                }
            } finally {
                onRunCompleted();
            }
        }
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel("com.formdev.flatlaf.FlatLightLaf");
        } catch (Exception e) {
        }
        SwingUtilities.invokeLater(() -> new ModernGAGUI().setVisible(true));
    }
}
