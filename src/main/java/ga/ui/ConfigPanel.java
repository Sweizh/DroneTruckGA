package ga.ui;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import ga.Config;

/**
 * 类名：ConfigPanel
 * 功能：参数配置面板
 * 说明：提供仓库配置、客户配置、车辆参数、GA参数的配置界面
 */
public class ConfigPanel extends JPanel {
    private GAGUI parent;
    private Config config;

    // 仓库配置
    private JPanel depotPanel;
    private List<JTextField> depotXFields;
    private List<JTextField> depotYFields;

    // 客户配置
    private JTable customerTable;
    private DefaultTableModel customerTableModel;

    // 车辆参数
    private JTextField truckCountField;
    private JTextField truckCapacityField;
    private JTextField truckSpeedField;
    private JTextField dronesPerTruckField;
    private JTextField droneCountField;
    private JTextField droneRangeField;
    private JTextField droneSpeedField;
    private JTextField dronePayloadField;
    private JTextField droneRechargeTimeField;

    // GA参数
    private JTextField populationSizeField;
    private JTextField maxGenerationsField;
    private JTextField crossoverRateField;
    private JTextField mutationRateField;
    private JTextField eliteRateField;
    private JTextField tournamentSizeField;

    /**
     * 构造函数：创建配置面板
     * @param parent 父窗口
     * @param config 初始配置
     */
    public ConfigPanel(GAGUI parent, Config config) {
        this.parent = parent;
        this.config = config;
        this.depotXFields = new ArrayList<>();
        this.depotYFields = new ArrayList<>();

        // 设置布局
        setLayout(new BorderLayout());

        // 创建标签页面板
        JTabbedPane tabbedPane = new JTabbedPane();

        // 添加仓库配置面板
        tabbedPane.addTab("仓库配置", createDepotPanel());

        // 添加客户配置面板
        tabbedPane.addTab("客户配置", createCustomerPanel());

        // 添加车辆参数面板
        tabbedPane.addTab("车辆参数", createVehiclePanel());

        // 添加GA参数面板
        tabbedPane.addTab("GA参数", createGAPanel());

        // 添加到面板
        add(tabbedPane, BorderLayout.CENTER);
    }

    /**
     * 创建仓库配置面板
     * @return 仓库配置面板
     */
    private JPanel createDepotPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addButton = new JButton("添加仓库");
        JButton removeButton = new JButton("删除仓库");

        addButton.addActionListener(e -> addDepot());
        removeButton.addActionListener(e -> removeDepot());

        buttonPanel.add(addButton);
        buttonPanel.add(removeButton);
        panel.add(buttonPanel, BorderLayout.NORTH);

        // 仓库列表面板
        depotPanel = new JPanel(new GridLayout(0, 1, 5, 5));
        depotPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        // 从配置加载仓库
        loadDepots();

        panel.add(depotPanel, BorderLayout.CENTER);

        return panel;
    }

    /**
     * 创建客户配置面板
     * @return 客户配置面板
     */
    private JPanel createCustomerPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addButton = new JButton("添加客户");
        JButton removeButton = new JButton("删除客户");
        JButton clearButton = new JButton("清空客户");

        addButton.addActionListener(e -> addCustomer());
        removeButton.addActionListener(e -> removeCustomer());
        clearButton.addActionListener(e -> clearCustomers());

        buttonPanel.add(addButton);
        buttonPanel.add(removeButton);
        buttonPanel.add(clearButton);

        panel.add(buttonPanel, BorderLayout.NORTH);

        // 客户表格
        String[] columns = {"ID", "X坐标", "Y坐标", "需求量", "所属仓库", "时间窗开始", "时间窗结束", "服务时间"};
        customerTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return true;
            }
        };
        customerTable = new JTable(customerTableModel);

        // 设置列宽
        customerTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        customerTable.getColumnModel().getColumn(1).setPreferredWidth(80);
        customerTable.getColumnModel().getColumn(2).setPreferredWidth(80);
        customerTable.getColumnModel().getColumn(3).setPreferredWidth(80);
        customerTable.getColumnModel().getColumn(4).setPreferredWidth(80);
        customerTable.getColumnModel().getColumn(5).setPreferredWidth(80);
        customerTable.getColumnModel().getColumn(6).setPreferredWidth(80);
        customerTable.getColumnModel().getColumn(7).setPreferredWidth(80);

        JScrollPane scrollPane = new JScrollPane(customerTable);
        scrollPane.setPreferredSize(new Dimension(800, 300));

        panel.add(scrollPane, BorderLayout.CENTER);

        // 从配置加载客户
        loadCustomers();

        return panel;
    }

    /**
     * 创建车辆参数面板
     * @return 车辆参数面板
     */
    private JPanel createVehiclePanel() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 20, 20));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 卡车参数
        JPanel truckPanel = new JPanel(new GridLayout(0, 2, 5, 10));
        truckPanel.setBorder(BorderFactory.createTitledBorder("卡车参数"));

        truckPanel.add(new JLabel("数量:"));
        truckCountField = new JTextField("2", 10);
        truckPanel.add(truckCountField);

        truckPanel.add(new JLabel("容量(kg):"));
        truckCapacityField = new JTextField("100", 10);
        truckPanel.add(truckCapacityField);

        truckPanel.add(new JLabel("速度(km/h):"));
        truckSpeedField = new JTextField("40", 10);
        truckPanel.add(truckSpeedField);

        truckPanel.add(new JLabel("可携无人机:"));
        dronesPerTruckField = new JTextField("2", 10);
        truckPanel.add(dronesPerTruckField);

        // 无人机参数
        JPanel dronePanel = new JPanel(new GridLayout(0, 2, 5, 10));
        dronePanel.setBorder(BorderFactory.createTitledBorder("无人机参数"));

        dronePanel.add(new JLabel("数量:"));
        droneCountField = new JTextField("2", 10);
        dronePanel.add(droneCountField);

        dronePanel.add(new JLabel("续航(km):"));
        droneRangeField = new JTextField("30", 10);
        dronePanel.add(droneRangeField);

        dronePanel.add(new JLabel("速度(km/h):"));
        droneSpeedField = new JTextField("60", 10);
        dronePanel.add(droneSpeedField);

        dronePanel.add(new JLabel("最大载重(kg):"));
        dronePayloadField = new JTextField("5", 10);
        dronePanel.add(dronePayloadField);

        dronePanel.add(new JLabel("充电时间(min):"));
        droneRechargeTimeField = new JTextField("10", 10);
        dronePanel.add(droneRechargeTimeField);

        panel.add(truckPanel);
        panel.add(dronePanel);

        // 从配置加载车辆参数
        loadVehicleParams();

        return panel;
    }

    /**
     * 创建分组参数面板
     * @return 分组参数面板
     */
    /**
      * 创建GA参数面板
     * @return GA参数面板
     */
    private JPanel createGAPanel() {
        JPanel panel = new JPanel(new GridLayout(0, 2, 10, 15));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        panel.add(new JLabel("种群规模:"));
        populationSizeField = new JTextField("100", 15);
        panel.add(populationSizeField);

        panel.add(new JLabel("最大迭代次数:"));
        maxGenerationsField = new JTextField("500", 15);
        panel.add(maxGenerationsField);

        panel.add(new JLabel("交叉率(0-1):"));
        crossoverRateField = new JTextField("0.8", 15);
        panel.add(crossoverRateField);

        panel.add(new JLabel("变异率(0-1):"));
        mutationRateField = new JTextField("0.15", 15);
        panel.add(mutationRateField);

        panel.add(new JLabel("精英率(0-1):"));
        eliteRateField = new JTextField("0.1", 15);
        panel.add(eliteRateField);

        panel.add(new JLabel("锦标赛规模:"));
        tournamentSizeField = new JTextField("3", 15);
        panel.add(tournamentSizeField);

        // 从配置加载GA参数
        loadGAParams();

        return panel;
    }

    /**
     * 加载仓库配置到界面
     */
    private void loadDepots() {
        depotPanel.removeAll();
        depotXFields.clear();
        depotYFields.clear();

        if (config.problem != null && config.problem.depots != null) {
            for (int i = 0; i < config.problem.depots.size(); i++) {
                Config.Depot depot = config.problem.depots.get(i);

                JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT));
                row.add(new JLabel("仓库" + depot.id + ":"));

                JTextField xField = new JTextField(String.valueOf(depot.x), 8);
                JTextField yField = new JTextField(String.valueOf(depot.y), 8);

                row.add(new JLabel("X:"));
                row.add(xField);
                row.add(new JLabel("Y:"));
                row.add(yField);

                depotXFields.add(xField);
                depotYFields.add(yField);

                depotPanel.add(row);
            }
        }

        depotPanel.revalidate();
        depotPanel.repaint();
    }

    /**
     * 加载客户配置到表格
     */
    private void loadCustomers() {
        customerTableModel.setRowCount(0);

        if (config.problem != null && config.problem.customers != null) {
            for (Config.Customer customer : config.problem.customers) {
                Object[] row = {
                    customer.id,
                    customer.x,
                    customer.y,
                    customer.demand,
                    customer.depotId,
                    customer.timeWindow != null ? customer.timeWindow[0] : 0,
                    customer.timeWindow != null ? customer.timeWindow[1] : 120,
                    customer.serviceTime
                };
                customerTableModel.addRow(row);
            }
        }
    }

    /**
     * 加载车辆参数到界面
     */
    private void loadVehicleParams() {
        if (config.vehicles != null && config.vehicles.trucks != null) {
            truckCountField.setText(String.valueOf(config.vehicles.trucks.count));
            truckCapacityField.setText(String.valueOf(config.vehicles.trucks.capacity));
            truckSpeedField.setText(String.valueOf(config.vehicles.trucks.speed));
            dronesPerTruckField.setText(String.valueOf(config.vehicles.trucks.dronesPerTruck));
        }

        if (config.vehicles != null && config.vehicles.drones != null) {
            droneCountField.setText(String.valueOf(config.vehicles.drones.count));
            droneRangeField.setText(String.valueOf(config.vehicles.drones.range));
            droneSpeedField.setText(String.valueOf(config.vehicles.drones.speed));
            dronePayloadField.setText(String.valueOf(config.vehicles.drones.maxPayload));
            droneRechargeTimeField.setText(String.valueOf(config.vehicles.drones.rechargeTime));
        }
    }

    /**
      * 加载GA参数到界面
      */
     private void loadGAParams() {
         if (config.genetic != null) {
             populationSizeField.setText(String.valueOf(config.genetic.populationSize));
             maxGenerationsField.setText(String.valueOf(config.genetic.maxGenerations));
             crossoverRateField.setText(String.valueOf(config.genetic.crossoverRate));
             mutationRateField.setText(String.valueOf(config.genetic.mutationRate));
             eliteRateField.setText(String.valueOf(config.genetic.eliteRate));
             tournamentSizeField.setText(String.valueOf(config.genetic.tournamentSize));
         }
     }

    /**
     * 添加仓库
     */
    private void addDepot() {
        int newId = depotXFields.size();
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT));
        row.add(new JLabel("仓库" + newId + ":"));

        JTextField xField = new JTextField("50", 8);
        JTextField yField = new JTextField("50", 8);

        row.add(new JLabel("X:"));
        row.add(xField);
        row.add(new JLabel("Y:"));
        row.add(yField);

        depotXFields.add(xField);
        depotYFields.add(yField);

        depotPanel.add(row);
        depotPanel.revalidate();
        depotPanel.repaint();
    }

    /**
     * 删除仓库
     */
    private void removeDepot() {
        if (depotXFields.size() > 1) {
            depotXFields.remove(depotXFields.size() - 1);
            depotYFields.remove(depotYFields.size() - 1);
            depotPanel.remove(depotPanel.getComponentCount() - 1);
            depotPanel.revalidate();
            depotPanel.repaint();
        } else {
            JOptionPane.showMessageDialog(this, "至少保留一个仓库", "提示", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /**
     * 添加客户
     */
    private void addCustomer() {
        int newId = customerTableModel.getRowCount() + 1;
        Object[] row = {
            newId,
            50.0,
            50.0,
            5.0,
            0,
            0,
            120,
            5
        };
        customerTableModel.addRow(row);
    }

    /**
     * 删除客户
     */
    private void removeCustomer() {
        int selectedRow = customerTable.getSelectedRow();
        if (selectedRow >= 0) {
            customerTableModel.removeRow(selectedRow);
            // 更新ID
            for (int i = 0; i < customerTableModel.getRowCount(); i++) {
                customerTableModel.setValueAt(i + 1, i, 0);
            }
        } else {
            JOptionPane.showMessageDialog(this, "请选择要删除的客户", "提示", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /**
     * 清空客户
     */
    private void clearCustomers() {
        int result = JOptionPane.showConfirmDialog(this,
                "确定要清空所有客户吗？",
                "确认",
                JOptionPane.YES_NO_OPTION);
        if (result == JOptionPane.YES_OPTION) {
            customerTableModel.setRowCount(0);
        }
    }

    /**
     * 设置配置
     * @param config 配置对象
     */
    public void setConfig(Config config) {
        this.config = config;
        loadDepots();
        loadCustomers();
        loadVehicleParams();
        loadGAParams();
    }

    /**
     * 获取当前配置
     * @return 配置对象
     */
    public Config getConfig() {
        // 确保config对象不为null
        if (config == null) {
            config = new Config();
        }

        // 保存仓库配置
        if (config.problem == null) {
            config.problem = new Config.Problem();
        }
        if (config.problem.depots == null) {
            config.problem.depots = new ArrayList<>();
        } else {
            config.problem.depots.clear();
        }

        for (int i = 0; i < depotXFields.size(); i++) {
            Config.Depot depot = new Config.Depot();
            depot.id = i;
            try {
                depot.x = Double.parseDouble(depotXFields.get(i).getText());
                depot.y = Double.parseDouble(depotYFields.get(i).getText());
            } catch (NumberFormatException e) {
                depot.x = 50.0;
                depot.y = 50.0;
            }
            config.problem.depots.add(depot);
        }

        // 保存客户配置
        if (config.problem.customers == null) {
            config.problem.customers = new ArrayList<>();
        } else {
            config.problem.customers.clear();
        }

        for (int i = 0; i < customerTableModel.getRowCount(); i++) {
            Config.Customer customer = new Config.Customer();
            try {
                customer.id = Integer.parseInt(customerTableModel.getValueAt(i, 0).toString());
                customer.x = Double.parseDouble(customerTableModel.getValueAt(i, 1).toString());
                customer.y = Double.parseDouble(customerTableModel.getValueAt(i, 2).toString());
                customer.demand = Double.parseDouble(customerTableModel.getValueAt(i, 3).toString());
                customer.depotId = Integer.parseInt(customerTableModel.getValueAt(i, 4).toString());
                customer.timeWindow = new double[]{
                    Double.parseDouble(customerTableModel.getValueAt(i, 5).toString()),
                    Double.parseDouble(customerTableModel.getValueAt(i, 6).toString())
                };
                customer.serviceTime = Double.parseDouble(customerTableModel.getValueAt(i, 7).toString());
            } catch (Exception e) {
                // 使用默认值
                customer.id = i + 1;
                customer.x = 50.0;
                customer.y = 50.0;
                customer.demand = 5.0;
                customer.depotId = 0;
                customer.timeWindow = new double[]{0, 120};
                customer.serviceTime = 5.0;
            }
            config.problem.customers.add(customer);
        }

        // 保存车辆参数
        if (config.vehicles == null) {
            config.vehicles = new Config.Vehicles();
        }

        if (config.vehicles.trucks == null) {
            config.vehicles.trucks = new Config.Trucks();
        }
        try {
            config.vehicles.trucks.count = Integer.parseInt(truckCountField.getText());
            config.vehicles.trucks.capacity = Double.parseDouble(truckCapacityField.getText());
            config.vehicles.trucks.speed = Double.parseDouble(truckSpeedField.getText());
            config.vehicles.trucks.dronesPerTruck = Integer.parseInt(dronesPerTruckField.getText());
        } catch (NumberFormatException e) {
            config.vehicles.trucks.count = 2;
            config.vehicles.trucks.capacity = 100;
            config.vehicles.trucks.speed = 40;
            config.vehicles.trucks.dronesPerTruck = 2;
        }

        if (config.vehicles.drones == null) {
            config.vehicles.drones = new Config.Drones();
        }
        try {
            config.vehicles.drones.count = Integer.parseInt(droneCountField.getText());
            config.vehicles.drones.range = Double.parseDouble(droneRangeField.getText());
            config.vehicles.drones.speed = Double.parseDouble(droneSpeedField.getText());
            config.vehicles.drones.maxPayload = Double.parseDouble(dronePayloadField.getText());
            config.vehicles.drones.rechargeTime = Double.parseDouble(droneRechargeTimeField.getText());
        } catch (NumberFormatException e) {
            config.vehicles.drones.count = 2;
            config.vehicles.drones.range = 30;
            config.vehicles.drones.speed = 60;
            config.vehicles.drones.maxPayload = 5;
            config.vehicles.drones.rechargeTime = 10;
        }

        // 保存GA参数
        if (config.genetic == null) {
            config.genetic = new Config.Genetic();
        }
        try {
            config.genetic.populationSize = Integer.parseInt(populationSizeField.getText());
            config.genetic.maxGenerations = Integer.parseInt(maxGenerationsField.getText());
            config.genetic.crossoverRate = Double.parseDouble(crossoverRateField.getText());
            config.genetic.mutationRate = Double.parseDouble(mutationRateField.getText());
            config.genetic.eliteRate = Double.parseDouble(eliteRateField.getText());
            config.genetic.tournamentSize = Integer.parseInt(tournamentSizeField.getText());
        } catch (NumberFormatException e) {
            config.genetic.populationSize = 100;
            config.genetic.maxGenerations = 500;
            config.genetic.crossoverRate = 0.8;
            config.genetic.mutationRate = 0.15;
            config.genetic.eliteRate = 0.1;
            config.genetic.tournamentSize = 3;
        }

        return config;
    }
}
