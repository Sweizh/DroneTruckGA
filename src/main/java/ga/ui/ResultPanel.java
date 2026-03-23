package ga.ui;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import ga.Config;
import ga.GARunner;

public class ResultPanel extends JPanel {
    private Config config;
    private ExecutorService executor;
    private boolean isRunning = false;
    private GAGUI parent;

    private JLabel fitnessLabel;
    private JLabel timeLabel;
    private JLabel chromosomeLabel;
    private JTextArea truckRoutesArea;
    private JTextArea droneTasksArea;

    private ChartPanel chartPanel;
    private ChartPanel.JFreeChartWrapper timeConvergenceChart;
    private ChartPanel.JFreeChartWrapper routeMapChart;

    private JButton runButton;
    private JButton stopButton;

    public ResultPanel() {
        this(null);
    }

    public ResultPanel(GAGUI parent) {
        this.parent = parent;
        setLayout(new BorderLayout());

        JPanel topPanel = new JPanel(new BorderLayout());

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        runButton = new JButton("运行GA");
        stopButton = new JButton("停止");

        runButton.addActionListener(e -> runGAFromParent());
        stopButton.addActionListener(e -> stopGA());

        stopButton.setEnabled(false);
        buttonPanel.add(runButton);
        buttonPanel.add(stopButton);
        topPanel.add(buttonPanel, BorderLayout.NORTH);

        JPanel statsPanel = new JPanel(new GridLayout(1, 4, 10, 10));
        statsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        Font statsFont = new Font("宋体", Font.BOLD, 14);

        fitnessLabel = new JLabel("适应度: -");
        timeLabel = new JLabel("最少时间: -");
        chromosomeLabel = new JLabel("最优染色体: -");

        fitnessLabel.setFont(statsFont);
        timeLabel.setFont(statsFont);
        chromosomeLabel.setFont(statsFont);

        statsPanel.add(fitnessLabel);
        statsPanel.add(timeLabel);
        statsPanel.add(chromosomeLabel);
        statsPanel.add(new JLabel(""));

        topPanel.add(statsPanel, BorderLayout.CENTER);

        JPanel chartContainer = new JPanel(new GridLayout(2, 1, 10, 10));
        chartContainer.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        chartPanel = new ChartPanel();

        JPanel timePanel = new JPanel(new BorderLayout());
        timePanel.setBorder(BorderFactory.createTitledBorder("时间收敛曲线"));
        timeConvergenceChart = chartPanel.initConvergenceChart("时间收敛曲线");
        timePanel.add(timeConvergenceChart.getPanel(), BorderLayout.CENTER);
        chartContainer.add(timePanel);

        JPanel routeMapPanel = new JPanel(new BorderLayout());
        routeMapPanel.setBorder(BorderFactory.createTitledBorder("配送路线图"));
        routeMapChart = chartPanel.initRouteMapChart("配送路线图");
        routeMapPanel.add(routeMapChart.getPanel(), BorderLayout.CENTER);
        chartContainer.add(routeMapPanel);

        JPanel routesPanel = new JPanel(new GridLayout(1, 2, 10, 10));
        routesPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel truckPanel = new JPanel(new BorderLayout());
        truckPanel.setBorder(BorderFactory.createTitledBorder("卡车配送方案"));
        truckRoutesArea = new JTextArea(8, 40);
        truckRoutesArea.setEditable(false);
        JScrollPane truckScroll = new JScrollPane(truckRoutesArea);
        truckPanel.add(truckScroll, BorderLayout.CENTER);
        routesPanel.add(truckPanel);

        JPanel dronePanel = new JPanel(new BorderLayout());
        dronePanel.setBorder(BorderFactory.createTitledBorder("无人机配送方案"));
        droneTasksArea = new JTextArea(8, 40);
        droneTasksArea.setEditable(false);
        JScrollPane droneScroll = new JScrollPane(droneTasksArea);
        dronePanel.add(droneScroll, BorderLayout.CENTER);
        routesPanel.add(dronePanel);

        add(topPanel, BorderLayout.NORTH);
        add(chartContainer, BorderLayout.CENTER);
        add(routesPanel, BorderLayout.SOUTH);

        executor = Executors.newSingleThreadExecutor();
    }

    public void runGA(Config config) {
        if (config == null || config.problem == null) {
            JOptionPane.showMessageDialog(this, "配置无效，请检查配置面板", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (isRunning) {
            JOptionPane.showMessageDialog(this, "遗传算法正在运行中...", "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        this.config = config;
        isRunning = true;
        runButton.setEnabled(false);
        stopButton.setEnabled(true);

        clearResults();

        executor.execute(() -> {
            try {
                GARunner.GAResult result = GARunner.runGA(config, null, null);
                SwingUtilities.invokeLater(() -> updateResults(result));
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this, "运行失败: " + e.getMessage(),
                        "错误", JOptionPane.ERROR_MESSAGE);
                });
            } finally {
                isRunning = false;
                SwingUtilities.invokeLater(() -> {
                    runButton.setEnabled(true);
                    stopButton.setEnabled(false);
                });
            }
        });
    }

    private void internalRunGA() {
        if (this.config == null || this.config.problem == null) {
            JOptionPane.showMessageDialog(this, "请先配置问题参数！", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }
        runGA(this.config);
    }

    private void runGAFromParent() {
        if (parent != null) {
            Config cfg = parent.getCurrentConfig();
            if (cfg != null && cfg.problem != null) {
                this.config = cfg;
                runGA(cfg);
            } else {
                JOptionPane.showMessageDialog(this, "请先配置问题参数！", "提示", JOptionPane.INFORMATION_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(this, "无法获取配置，请重启程序", "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void stopGA() {
        if (isRunning) {
            executor.shutdownNow();
            isRunning = false;
            runButton.setEnabled(true);
            stopButton.setEnabled(false);
            JOptionPane.showMessageDialog(this, "已停止运行", "提示", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    public void clearResults() {
        fitnessLabel.setText("适应度: -");
        timeLabel.setText("最少时间: -");
        chromosomeLabel.setText("最优染色体: -");
        truckRoutesArea.setText("");
        droneTasksArea.setText("");
        if (timeConvergenceChart != null) {
            timeConvergenceChart.clearData();
            timeConvergenceChart.clearAllSeries();
        }
        if (routeMapChart != null) {
            routeMapChart.clearData();
            routeMapChart.clearAllSeries();
        }
    }

    private void updateResults(GARunner.GAResult result) {
        this.lastResult = result;

        fitnessLabel.setText(String.format("适应度: %.6f", result.bestFitness));
        timeLabel.setText(String.format("最少时间: %.2f min", result.bestTime));
        chromosomeLabel.setText("最优染色体: " + result.bestChromosome);

        if (timeConvergenceChart != null && result.timeCurve != null) {
            timeConvergenceChart.clearAllSeries();
            for (int i = 0; i < result.timeCurve.size(); i++) {
                timeConvergenceChart.addSeries("时间").add(i * 10, result.timeCurve.get(i));
            }
        }

        StringBuilder truckText = new StringBuilder();
        for (int i = 0; i < result.truckRoutes.size(); i++) {
            truckText.append(String.format("卡车%d: ", i + 1));
            truckText.append(String.join(" → ", result.truckRoutes.get(i).stream()
                .map(id -> id == 0 ? "仓库" : "客户" + id)
                .toArray(String[]::new)));
            truckText.append("\n");
        }
        truckRoutesArea.setText(truckText.toString());

        StringBuilder droneText = new StringBuilder();
        for (int i = 0; i < result.droneTasks.size(); i++) {
            droneText.append(String.format("无人机%d: ", i + 1));
            if (result.droneTasks.get(i).isEmpty()) {
                droneText.append("无任务");
            } else {
                droneText.append("[");
                droneText.append(result.droneTasks.get(i).stream()
                    .map(id -> "客户" + id)
                    .reduce((a, b) -> a + ", " + b)
                    .orElse(""));
                droneText.append("]");
            }
            droneText.append("\n");
        }
        droneTasksArea.setText(droneText.toString());

        updateRouteMap(result.truckRoutes, result.droneTasks, this.config);
        chartPanel.repaint();
    }

    private void updateRouteMap(List<List<Integer>> truckRoutes,
                                List<List<Integer>> droneTasks,
                                Config config) {
        if (routeMapChart == null || config == null) return;

        routeMapChart.clearAllSeries();

        Map<Integer, double[]> pointMap = new HashMap<>();
        
        if (config.problem != null && config.problem.depots != null) {
            for (Config.Depot depot : config.problem.depots) {
                pointMap.put(depot.id, new double[]{depot.x, depot.y});
            }
        }
        if (config.problem != null && config.problem.customers != null) {
            for (Config.Customer customer : config.problem.customers) {
                pointMap.put(customer.id, new double[]{customer.x, customer.y});
            }
        }

        GARunner.GAResult lastResult = getLastResult();
        if (lastResult != null) {
            // 优先使用解码后的所有发射点
            List<Double> launchPoints = lastResult.allLaunchPoints;
            if (launchPoints == null && lastResult.bestChromosome != null) {
                launchPoints = lastResult.bestChromosome.launchPointCoords;
            }
            if (launchPoints != null) {
                // 构建发射点ID到坐标的映射
                // 发射点 ID = -(1), -(2), -(3), ... -n
                int launchPointCount = launchPoints.size() / 2;
                for (int lpIdx = 0; lpIdx < launchPointCount; lpIdx++) {
                    int lpId = -(lpIdx + 1);
                    pointMap.put(lpId, new double[]{
                        launchPoints.get(lpIdx * 2),
                        launchPoints.get(lpIdx * 2 + 1)
                    });
                }
            }
        }

        if (config.problem != null && config.problem.depots != null) {
            for (Config.Depot depot : config.problem.depots) {
                routeMapChart.addSeries("Warehouse").add(depot.x, depot.y);
            }
        }

        if (config.problem != null && config.problem.customers != null) {
            for (Config.Customer customer : config.problem.customers) {
                routeMapChart.addSeries("Customer").add(customer.x, customer.y);
            }
        }

        // 显示所有发射点（从launchPoints获取，不是只显示K个）
        List<Double> allLaunchPoints = null;
        if (lastResult != null) {
            allLaunchPoints = lastResult.allLaunchPoints;
            if (allLaunchPoints == null && lastResult.bestChromosome != null) {
                allLaunchPoints = lastResult.bestChromosome.launchPointCoords;
            }
        }
        if (allLaunchPoints != null) {
            int lpCount = allLaunchPoints.size() / 2;
            for (int lpIdx = 0; lpIdx < lpCount; lpIdx++) {
                double lx = allLaunchPoints.get(lpIdx * 2);
                double ly = allLaunchPoints.get(lpIdx * 2 + 1);
                routeMapChart.addSeries("LaunchPoint").add(lx, ly);
            }
        }

        Color[] truckColors = new Color[]{
            Color.BLUE, Color.MAGENTA, Color.ORANGE,
            Color.CYAN, Color.PINK, Color.GRAY
        };

        for (int t = 0; t < truckRoutes.size(); t++) {
            List<Integer> route = truckRoutes.get(t);
            routeMapChart.addSeries("Truck" + (t + 1));

            for (Integer nodeId : route) {
                double[] coords = pointMap.get(nodeId);
                if (coords != null) {
                    routeMapChart.addSeries("Truck" + (t + 1)).add(coords[0], coords[1]);
                }
            }
        }

        Color[] droneColors = new Color[]{
            Color.RED, Color.GREEN, Color.DARK_GRAY, Color.YELLOW
        };

        // 获取发射点坐标列表用于无人机显示
        List<Double> droneLaunchPoints = null;
        if (lastResult != null && lastResult.allLaunchPoints != null) {
            droneLaunchPoints = lastResult.allLaunchPoints;
        }
        List<Integer> droneToLP = null;
        if (lastResult != null && lastResult.droneToLaunchPoint != null) {
            droneToLP = lastResult.droneToLaunchPoint;
        }

        for (int d = 0; d < droneTasks.size(); d++) {
            List<Integer> tasks = droneTasks.get(d);
            if (tasks.isEmpty()) continue;

            // 使用droneToLaunchPoint映射获取正确的发射点
            double[] launchPt = null;
            if (droneToLP != null && droneLaunchPoints != null) {
                int lpIdx = d < droneToLP.size() ? droneToLP.get(d) : 0;
                if (lpIdx >= 0 && lpIdx < droneLaunchPoints.size() / 2) {
                    launchPt = new double[]{
                        droneLaunchPoints.get(lpIdx * 2),
                        droneLaunchPoints.get(lpIdx * 2 + 1)
                    };
                }
            }
            // 回退：使用旧方法
            if (launchPt == null) {
                int truckIdx = d / config.vehicles.trucks.dronesPerTruck;
                launchPt = pointMap.get(-(truckIdx + 1));
            }
            if (launchPt == null) continue;

            routeMapChart.addSeries("Drone" + (d + 1));

            for (int custId : tasks) {
                double[] custPt = pointMap.get(custId);
                if (custPt != null) {
                    routeMapChart.addSeries("Drone" + (d + 1)).add(launchPt[0], launchPt[1]);
                    routeMapChart.addSeries("Drone" + (d + 1)).add(custPt[0], custPt[1]);
                    routeMapChart.addSeries("Drone" + (d + 1)).add(custPt[0], custPt[1]);
                    routeMapChart.addSeries("Drone" + (d + 1)).add(launchPt[0], launchPt[1]);
                }
            }
        }
    }

    private GARunner.GAResult getLastResult() {
        return lastResult;
    }

    private GARunner.GAResult lastResult;
}
