package ga.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import ga.Config;

/**
 * 类名：GAGUI
 * 功能：主窗口类
 * 说明：继承JFrame，创建可视化界面主窗口
 */
public class GAGUI extends JFrame {
    private ConfigManager configManager;
    private ConfigPanel configPanel;
    private ResultPanel resultPanel;
    private ChartPanel chartPanel;
    private Config currentConfig;

    /**
     * 构造函数：创建主窗口
     */
    public GAGUI() {
        // 设置窗口标题
        setTitle("卡车-无人机协同配送 GA 参数配置系统");
        // 设置窗口大小
        setSize(1400, 900);
        // 设置窗口关闭行为
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        // 设置窗口居中显示
        setLocationRelativeTo(null);
        // 设置布局管理器
        setLayout(new BorderLayout());

        // 初始化配置管理器
        configManager = new ConfigManager();

        // 加载默认配置
        try {
            currentConfig = configManager.loadDefaultConfig();
        } catch (Exception e) {
            // 如果加载失败，使用默认配置
            currentConfig = createDefaultConfig();
            JOptionPane.showMessageDialog(this,
                    "无法加载配置文件，将使用默认配置",
                    "提示",
                    JOptionPane.INFORMATION_MESSAGE);
        }

        // 初始化界面组件
        initializeComponents();

        // 添加窗口关闭监听器
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                // 关闭窗口前询问是否保存配置
                int result = JOptionPane.showConfirmDialog(GAGUI.this,
                        "是否保存当前配置？",
                        "退出确认",
                        JOptionPane.YES_NO_OPTION);
                if (result == JOptionPane.YES_OPTION) {
                    saveConfig();
                }
            }
        });
    }

    /**
     * 初始化所有界面组件
     */
    private void initializeComponents() {
        // 创建标签页面板
        JTabbedPane tabbedPane = new JTabbedPane();

        // 创建配置面板
        configPanel = new ConfigPanel(this, currentConfig);
        tabbedPane.addTab("参数配置", configPanel);

        // 创建结果面板，传入当前窗口引用
        resultPanel = new ResultPanel(this);
        tabbedPane.addTab("运行结果", resultPanel);

        // 添加到窗口中心
        add(tabbedPane, BorderLayout.CENTER);

        // 创建菜单栏
        createMenuBar();
    }

    /**
     * 创建菜单栏
     */
    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // 文件菜单
        JMenu fileMenu = new JMenu("文件(F)");
        fileMenu.setMnemonic('F');

        JMenuItem newItem = new JMenuItem("新建配置(N)");
        newItem.setMnemonic('N');
        newItem.addActionListener(e -> newConfig());

        JMenuItem openItem = new JMenuItem("打开配置(O)...");
        openItem.setMnemonic('O');
        openItem.addActionListener(e -> openConfig());

        JMenuItem saveItem = new JMenuItem("保存配置(S)");
        saveItem.setMnemonic('S');
        saveItem.addActionListener(e -> saveConfig());

        JMenuItem saveAsItem = new JMenuItem("另存为(A)...");
        saveAsItem.setMnemonic('A');
        saveAsItem.addActionListener(e -> saveConfigAs());

        JMenuItem exitItem = new JMenuItem("退出(X)");
        exitItem.setMnemonic('X');
        exitItem.addActionListener(e -> exitApplication());

        fileMenu.add(newItem);
        fileMenu.add(openItem);
        fileMenu.addSeparator();
        fileMenu.add(saveItem);
        fileMenu.add(saveAsItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);

        // 运行菜单
        JMenu runMenu = new JMenu("运行(R)");
        runMenu.setMnemonic('R');

        JMenuItem runItem = new JMenuItem("运行GA(R)");
        runItem.setMnemonic('R');
        runItem.addActionListener(e -> runGA());

        JMenuItem stopItem = new JMenuItem("停止(S)");
        stopItem.setMnemonic('S');
        stopItem.addActionListener(e -> stopGA());

        runMenu.add(runItem);
        runMenu.add(stopItem);

        // 帮助菜单
        JMenu helpMenu = new JMenu("帮助(H)");
        helpMenu.setMnemonic('H');

        JMenuItem aboutItem = new JMenuItem("关于(A)");
        aboutItem.setMnemonic('A');
        aboutItem.addActionListener(e -> showAbout());

        helpMenu.add(aboutItem);

        // 添加菜单到菜单栏
        menuBar.add(fileMenu);
        menuBar.add(runMenu);
        menuBar.add(helpMenu);

        // 设置菜单栏
        setJMenuBar(menuBar);
    }

    /**
     * 获取当前配置
     * @return 当前配置对象
     */
    public Config getCurrentConfig() {
        // 从配置面板获取最新配置
        return configPanel.getConfig();
    }

    /**
     * 创建默认配置
     * @return 默认配置对象
     */
    private Config createDefaultConfig() {
        Config config = new Config();

        // 创建问题配置
        config.problem = new Config.Problem();
        config.problem.depots = new java.util.ArrayList<>();
        config.problem.depots.add(new Config.Depot());
        config.problem.depots.get(0).id = 0;
        config.problem.depots.get(0).x = 50.0;
        config.problem.depots.get(0).y = 50.0;

        config.problem.customers = new java.util.ArrayList<>();

        // 添加示例客户
        for (int i = 1; i <= 5; i++) {
            Config.Customer customer = new Config.Customer();
            customer.id = i;
            customer.x = 20 + i * 10;
            customer.y = 30 + i * 10;
            customer.demand = 5;
            customer.depotId = 0;
            customer.timeWindow = new double[]{0, 120};
            customer.serviceTime = 5;
            config.problem.customers.add(customer);
        }

        // 创建车辆配置
        config.vehicles = new Config.Vehicles();

        config.vehicles.trucks = new Config.Trucks();
        config.vehicles.trucks.count = 2;
        config.vehicles.trucks.capacity = 100;
        config.vehicles.trucks.speed = 40;

        config.vehicles.drones = new Config.Drones();
        config.vehicles.drones.count = 2;
        config.vehicles.drones.range = 30;
        config.vehicles.drones.speed = 60;
        config.vehicles.drones.maxPayload = 5;
        config.vehicles.drones.rechargeTime = 10;

        // 创建遗传算法配置
        config.genetic = new Config.Genetic();
        config.genetic.populationSize = 100;
        config.genetic.maxGenerations = 500;
        config.genetic.crossoverRate = 0.8;
        config.genetic.mutationRate = 0.15;
        config.genetic.eliteRate = 0.1;
        config.genetic.tournamentSize = 3;

        return config;
    }

    /**
     * 新建配置
     */
    private void newConfig() {
        int result = JOptionPane.showConfirmDialog(this,
                "确定要新建配置吗？当前未保存的更改将会丢失。",
                "新建配置",
                JOptionPane.YES_NO_OPTION);
        if (result == JOptionPane.YES_OPTION) {
            currentConfig = createDefaultConfig();
            configPanel.setConfig(currentConfig);
            resultPanel.clearResults();
        }
    }

    /**
     * 打开配置
     */
    private void openConfig() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("打开配置文件");
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("JSON文件 (*.json)", "json"));

        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                currentConfig = configManager.loadConfig(fileChooser.getSelectedFile().getAbsolutePath());
                configPanel.setConfig(currentConfig);
                JOptionPane.showMessageDialog(this, "配置已加载", "成功", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "加载配置失败: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * 保存配置
     */
    private void saveConfig() {
        try {
            currentConfig = configPanel.getConfig();
            configManager.saveDefaultConfig(currentConfig);
            JOptionPane.showMessageDialog(this, "配置已保存", "成功", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "保存配置失败: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * 另存为配置
     */
    private void saveConfigAs() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("保存配置文件");
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("JSON文件 (*.json)", "json"));

        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            String path = fileChooser.getSelectedFile().getAbsolutePath();
            if (!path.endsWith(".json")) {
                path += ".json";
            }
            try {
                currentConfig = configPanel.getConfig();
                configManager.saveConfig(currentConfig, path);
                JOptionPane.showMessageDialog(this, "配置已保存到: " + path, "成功", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "保存配置失败: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * 退出应用程序
     */
    private void exitApplication() {
        int result = JOptionPane.showConfirmDialog(this,
                "是否保存当前配置？",
                "退出确认",
                JOptionPane.YES_NO_OPTION);
        if (result == JOptionPane.YES_OPTION) {
            saveConfig();
        }
        System.exit(0);
    }

    /**
     * 运行遗传算法
     */
    private void runGA() {
        // 获取当前配置
        Config cfg = configPanel.getConfig();

        // 检查配置有效性
        if (cfg == null || cfg.problem == null) {
            JOptionPane.showMessageDialog(this, "请先配置问题参数！", "配置错误", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (cfg.problem.depots == null || cfg.problem.depots.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请至少配置一个仓库！", "配置错误", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (cfg.problem.customers == null || cfg.problem.customers.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请至少配置一个客户！", "配置错误", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 保存当前配置
        currentConfig = cfg;

        // 在结果面板中运行
        resultPanel.runGA(currentConfig);
    }

    /**
     * 停止遗传算法
     */
    private void stopGA() {
        resultPanel.stopGA();
    }

    /**
     * 显示关于对话框
     */
    private void showAbout() {
        JOptionPane.showMessageDialog(this,
                "卡车-无人机协同配送路径优化系统\n" +
                "基于遗传算法求解\n\n" +
                "功能特点:\n" +
                "- 多仓库支持\n" +
                "- 卡车-无人机协同配送\n" +
                "- 时间窗约束\n" +
                "- 可视化参数配置\n" +
                "- 收敛曲线展示\n" +
                "- 配送路线图展示",
                "关于",
                JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * 主方法：程序入口
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        // 在事件分发线程中创建GUI
        SwingUtilities.invokeLater(() -> {
            // 设置系统外观
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }

            // 创建并显示窗口
            GAGUI gui = new GAGUI();
            gui.setVisible(true);
        });
    }
}
