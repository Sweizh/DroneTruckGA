package ga;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 类名：ProblemModel
 * 功能：问题模型类，用于存储和访问卡车-无人机协同配送问题的所有数据
 * 说明：从Config对象初始化，包含仓库、客户、车辆等所有问题参数
 *      提供getter方法供其他模块访问数据
 */
public class ProblemModel {
    /** 仓库列表（支持多个仓库） */
    private final List<Point> depots;
    /** 仓库Map，key为仓库ID，value为Point对象 */
    private final Map<Integer, Point> depotMap;
    /** 客户点列表 */
    private final List<Customer> customers;
    /** 客户Map，key为客户ID，value为Customer对象 */
    private final Map<Integer, Customer> customerMap;
    /** 卡车数量 */
    private final int truckCount;
    /** 卡车载重容量（单位：kg） */
    private final double truckCapacity;
    /** 卡车行驶速度（单位：km/h） */
    private final double truckSpeed;
    /** 卡车每公里成本（单位：元/km） */
    private final double truckCostPerKm;
    /** 卡车固定成本（单位：元），每辆卡车出车的固定费用 */
    private final double truckFixedCost;
    /** 卡车最大续航里程（单位：km），0或负数表示无限续航 */
    private final double truckRange;
    /** 无人机数量 */
    private final int droneCount;
    /** 无人机最大续航里程（单位：km） */
    private final double droneRange;
    /** 无人机飞行速度（单位：km/h） */
    private final double droneSpeed;
    /** 无人机最大载重（单位：kg） */
    private final double droneMaxPayload;
    /** 无人机每公里成本（单位：元/km） */
    private final double droneCostPerKm;
    /** 无人机充电时间（单位：min） */
    private final double droneRechargeTime;
    /** 无人机充电成本（单位：元） */
    private final double droneRechargeCost;
    /** 单卡车可携无人机数量 */
    private final int dronesPerTruck;
    /** 发射点配置对象 */
    private final Config.LaunchPointConfig launchPointConfig;
    /** 配置对象引用 */
    private final Config config;
    /** 优化目标配置 */
    private String optimizationObjective;
    private double costWeight;
    private double timeWeight;
    /** 惩罚参数配置 */
    private double timeWindowEarly;
    private double timeWindowLate;
    private double droneRangePenalty;
    private double dronePayloadPenalty;
    private double waitTimePenalty;
    private double delayTimePenalty;
    private double truckRangeTimePenalty;
    private double truckRangeDistancePenalty;
    private double distancePenalty;
    private boolean penaltyEnabled;
    /** 归一化参数配置 */
    private double maxCost;
    private double maxTime;

    /**
     * 坐标边界（缓存）
     */
    private Double minX, maxX, minY, maxY;
    
    /** 发射点数量 */
    private int launchPointCount;

    /**
     * 构造函数：从Config对象创建问题模型
     * @param config 配置对象，包含所有问题参数
     */
    public ProblemModel(Config config) {
        this.config = config;

        // 初始化仓库列表和Map
        this.depots = new ArrayList<>();
        this.depotMap = new HashMap<>();
        for (Config.Depot d : config.problem.depots) {
            Point depot = new Point(d.id, d.x, d.y, true);
            depots.add(depot);
            depotMap.put(d.id, depot);
        }

        // 初始化客户列表和Map
        this.customers = new ArrayList<>();
        this.customerMap = new HashMap<>();
        for (Config.Customer c : config.problem.customers) {
            Customer customer = new Customer(c.id, c.x, c.y, c.demand,
                    c.depotId, c.timeWindow[0], c.timeWindow[1], c.serviceTime);
            customers.add(customer);
            customerMap.put(c.id, customer);
        }

        // 初始化卡车参数
        this.truckCount = config.vehicles.trucks.count;
        this.truckCapacity = config.vehicles.trucks.capacity;
        this.truckSpeed = config.vehicles.trucks.speed;
        this.truckCostPerKm = config.vehicles.trucks.costPerKm;
        this.truckFixedCost = config.vehicles.trucks.fixedCost;
        this.truckRange = config.vehicles.trucks.range;
        this.dronesPerTruck = config.vehicles.trucks.dronesPerTruck;

        // 初始化无人机参数
        // 总无人机数 = 卡车数量 × 每辆卡车搭载数
        this.droneCount = config.vehicles.trucks.count * config.vehicles.trucks.dronesPerTruck;
        this.droneRange = config.vehicles.drones.range;
        this.droneSpeed = config.vehicles.drones.speed;
        this.droneMaxPayload = config.vehicles.drones.maxPayload;
        this.droneCostPerKm = config.vehicles.drones.costPerKm;
        this.droneRechargeTime = config.vehicles.drones.rechargeTime;
        this.droneRechargeCost = config.vehicles.drones.rechargeCost;

        // 初始化优化目标配置
        if (config.optimization != null) {
            this.optimizationObjective = config.optimization.objective;
            this.costWeight = config.optimization.costWeight;
            this.timeWeight = config.optimization.timeWeight;
        } else {
            this.optimizationObjective = "both";
            this.costWeight = 0.5;
            this.timeWeight = 0.5;
        }

        // 初始化坐标边界
        this.minX = null;
        this.maxX = null;
        this.minY = null;
        this.maxY = null;

        // 初始化发射点配置（从config加载或使用默认值）
        if (config.launchPoint != null) {
            this.launchPointConfig = config.launchPoint;
        } else {
            this.launchPointConfig = new Config.LaunchPointConfig();
        }
        
        // 初始化发射点数量
        this.launchPointCount = calculateLaunchPointCount();
        
        // 初始化惩罚参数配置（从config加载或使用默认值）
        if (config.penalty != null) {
            this.penaltyEnabled = config.penalty.enabled;
            this.timeWindowEarly = config.penalty.timeWindowEarly;
            this.timeWindowLate = config.penalty.timeWindowLate;
            this.droneRangePenalty = config.penalty.droneRangePenalty;
            this.dronePayloadPenalty = config.penalty.dronePayloadPenalty;
            this.waitTimePenalty = config.penalty.waitTimePenalty;
            this.delayTimePenalty = config.penalty.delayTimePenalty;
            this.truckRangeTimePenalty = config.penalty.truckRangeTimePenalty;
            this.truckRangeDistancePenalty = config.penalty.truckRangeDistancePenalty;
            this.distancePenalty = config.penalty.distancePenalty;
        } else {
            // 使用默认值
            this.timeWindowEarly = 2.0;
            this.timeWindowLate = 50.0;
            this.droneRangePenalty = 50.0;
            this.dronePayloadPenalty = 20.0;
            this.waitTimePenalty = 2.0;
            this.delayTimePenalty = 50.0;
            this.truckRangeTimePenalty = 5.0;
            this.truckRangeDistancePenalty = 50.0;
            this.distancePenalty = 10.0;
        }

        // 初始化归一化参数配置（从config加载或使用默认值）
        if (config.normalization != null) {
            this.maxCost = config.normalization.maxCost;
            this.maxTime = config.normalization.maxTime;
        } else {
            // 使用默认值
            this.maxCost = 10000.0;
            this.maxTime = 1000.0;
        }
    }

    /**
     * 获取仓库列表
     * @return 仓库Point对象列表
     */
    public List<Point> getDepots() { return depots; }

    /**
     * 获取仓库数量
     * @return 仓库数量
     */
    public int getDepotCount() { return depots.size(); }

    /**
     * 根据仓库ID获取仓库点
     * @param depotId 仓库ID
     * @return 仓库Point对象，如果未找到返回null
     */
    public Point getDepotById(int depotId) { return depotMap.get(depotId); }

    /**
     * 获取客户点列表
     * @return 客户点List
     */
    public List<Customer> getCustomers() { return customers; }

    /**
     * 获取客户数量
     * @return 客户数量
     */
    public int getCustomerCount() { return customers.size(); }

    /**
     * 获取卡车数量
     * @return 卡车数量
     */
    public int getTruckCount() { return truckCount; }

    /**
     * 获取卡车载重容量
     * @return 容量（kg）
     */
    public double getTruckCapacity() { return truckCapacity; }

    /**
     * 获取卡车行驶速度
     * @return 速度（km/h）
     */
    public double getTruckSpeed() { return truckSpeed; }

    /**
     * 获取卡车每公里成本
     * @return 成本（元/km）
     */
    public double getTruckCostPerKm() { return truckCostPerKm; }

    /**
     * 获取卡车固定成本
     * @return 固定成本（元）
     */
    public double getTruckFixedCost() { return truckFixedCost; }

    /**
     * 获取卡车续航里程
     * @return 续航里程（km），0或负数表示无限续航
     */
    public double getTruckRange() { return truckRange; }

    /**
     * 获取无人机数量
     * @return 无人机数量
     */
    public int getDroneCount() { return droneCount; }

    /**
     * 获取无人机续航里程
     * @return 续航（km）
     */
    public double getDroneRange() { return droneRange; }

    /**
     * 获取无人机飞行速度
     * @return 速度（km/h）
     */
    public double getDroneSpeed() { return droneSpeed; }

    /**
     * 获取无人机最大载重
     * @return 最大载重（kg）
     */
    public double getDroneMaxPayload() { return droneMaxPayload; }

    /**
     * 获取无人机每公里成本
     * @return 成本（元/km）
     */
    public double getDroneCostPerKm() { return droneCostPerKm; }

    /**
     * 获取无人机充电时间
     * @return 充电时间（min）
     */
    public double getDroneRechargeTime() { return droneRechargeTime; }

    /**
     * 获取无人机充电成本
     * @return 充电成本（元）
     */
    public double getDroneRechargeCost() { return droneRechargeCost; }

    /**
     * 根据客户ID获取客户对象
     * @param id 客户ID
     * @return 客户对象，如果未找到返回null
     */
    public Customer getCustomerById(int id) { return customerMap.get(id); }

    /**
     * 获取单卡车可携无人机数量
     * @return 无人机数量
     */
    public int getDronesPerTruck() { return dronesPerTruck; }

      /**
       * 获取配置对象
       * @return 配置对象
       */
      public Config getConfig() { return config; }

      /**
       * 获取发射点配置
       * @return 发射点配置对象
       */
      public Config.LaunchPointConfig getLaunchPointConfig() { return launchPointConfig; }

    /**
     * 获取X坐标最小值
     * @return 最小X坐标
     */
    public double getMinX() {
        if (minX == null) calculateBounds();
        return minX;
    }

    /**
     * 获取X坐标最大值
     * @return 最大X坐标
     */
    public double getMaxX() {
        if (maxX == null) calculateBounds();
        return maxX;
    }

    /**
     * 获取Y坐标最小值
     * @return 最小Y坐标
     */
    public double getMinY() {
        if (minY == null) calculateBounds();
        return minY;
    }

    /**
     * 获取Y坐标最大值
     * @return 最大Y坐标
     */
    public double getMaxY() {
        if (maxY == null) calculateBounds();
        return maxY;
    }

    /**
     * 计算坐标边界
     */
    private void calculateBounds() {
        minX = Double.MAX_VALUE;
        maxX = Double.MIN_VALUE;
        minY = Double.MAX_VALUE;
        maxY = Double.MIN_VALUE;

        for (Point depot : depots) {
            minX = Math.min(minX, depot.getX());
            maxX = Math.max(maxX, depot.getX());
            minY = Math.min(minY, depot.getY());
            maxY = Math.max(maxY, depot.getY());
        }

        for (Customer customer : customers) {
            minX = Math.min(minX, customer.getX());
            maxX = Math.max(maxX, customer.getX());
            minY = Math.min(minY, customer.getY());
            maxY = Math.max(maxY, customer.getY());
        }
    }

    /**
     * 获取优化目标类型
     * @return 优化目标：cost, time, both
     */
    public String getOptimizationObjective() { return optimizationObjective; }

    /**
     * 获取成本权重
     * @return 成本权重
     */
    public double getCostWeight() { return costWeight; }

    /**
     * 获取时间权重
     * @return 时间权重
     */
    public double getTimeWeight() { return timeWeight; }

    /**
     * 获取时间窗提前到达惩罚系数
     * @return 惩罚系数（元/分钟）
     */
    public double getTimeWindowEarly() { return timeWindowEarly; }

    /**
     * 获取时间窗延误到达惩罚系数
     * @return 惩罚系数（元/分钟）
     */
    public double getTimeWindowLate() { return timeWindowLate; }

    /**
     * 获取无人机续航超限惩罚系数
     * @return 惩罚系数（元/km）
     */
    public double getDroneRangePenalty() { return droneRangePenalty; }

    /**
     * 获取无人机载重超限惩罚系数
     * @return 惩罚系数（元/kg）
     */
    public double getDronePayloadPenalty() { return dronePayloadPenalty; }

    /**
     * 获取等待时间惩罚系数
     * @return 惩罚系数（元/分钟）
     */
    public double getWaitTimePenalty() { return waitTimePenalty; }

    /**
     * 获取延误时间惩罚系数
     * @return 惩罚系数（元/分钟）
     */
    public double getDelayTimePenalty() { return delayTimePenalty; }

    /**
     * 获取卡车续航超限时间惩罚系数
     * @return 惩罚系数（元/分钟）
     */
    public double getTruckRangeTimePenalty() { return truckRangeTimePenalty; }

    /**
     * 获取卡车续航超限距离惩罚系数
     * @return 惩罚系数（元/km）
     */
    public double getTruckRangeDistancePenalty() { return truckRangeDistancePenalty; }

    /**
     * 获取距离惩罚系数
     * @return 惩罚系数（元/km）
     */
    public double getDistancePenalty() { return distancePenalty; }

    /**
     * 是否启用惩罚计算
     * @return 是否启用
     */
    public boolean isPenaltyEnabled() { return penaltyEnabled; }

    /**
     * 获取成本归一化最大值
     * @return 最大成本（元）
     */
    public double getMaxCost() { return maxCost; }

    /**
     * 获取时间归一化最大值
     * @return 最大时间（分钟）
     */
    public double getMaxTime() { return maxTime; }
    
    /**
     * 获取发射点数量
     * @return 发射点数量 n
     */
    public int getLaunchPointCount() { return launchPointCount; }
    
    /**
     * 计算发射点数量
     * 使用肘部法则自动确定最优K，或使用手动指定的值
     * @return 发射点数量
     */
    private int calculateLaunchPointCount() {
        int targetN = launchPointConfig.launchPointCount;
        
        if (targetN <= 0) {
            int optimalK = determineOptimalKByElbow();
            return validateAndAdjustLaunchPointCount(optimalK);
        } else {
            return validateAndAdjustLaunchPointCount(targetN);
        }
    }
    
    /**
     * 验证并调整发射点数量，使其符合约束
     * 约束：n >= K 且 n < N/U
     * @param n 原始发射点数量
     * @return 调整后的发射点数量
     */
    private int validateAndAdjustLaunchPointCount(int n) {
        int K = truckCount;
        int N = customers.size();
        int U = dronesPerTruck;
        
        // 计算 n < N/U 的上限 (向上取整)
        int maxN = (int) Math.ceil((double) N / U) - 1;
        if (maxN < K + 1) maxN = K + 1;
        
        // 确保 n >= K
        if (n < K) n = K;
        // 确保 n < N/U
        if (n >= maxN) n = maxN;
        // 确保 n >= 1
        if (n < 1) n = 1;
        
        return n;
    }
    
    /**
     * 使用肘部法则确定最优发射点数量
     * @return 最优K值
     */
    public int determineOptimalKByElbow() {
        int K = truckCount;
        int N = customers.size();
        int U = dronesPerTruck;
        
        // 计算搜索范围
        int maxPossibleK = (int) Math.ceil((double) N / U) - 1;
        if (maxPossibleK < K + 1) maxPossibleK = K + 1;
        int minK = K;
        int maxK = maxPossibleK;
        
        if (minK >= maxK) {
            return minK;
        }
        
        // 计算不同K值的SSE
        double[] sseValues = new double[maxK - minK + 1];
        int[] kValues = new int[maxK - minK + 1];
        
        for (int k = minK; k <= maxK; k++) {
            List<List<Customer>> clusters = kmeansClustering(customers, k, launchPointConfig.kmeansMaxIterations);
            List<double[]> centroids = new ArrayList<>();
            for (int i = 0; i < clusters.size(); i++) {
                centroids.add(calculateCentroid(clusters.get(i)));
            }
            sseValues[k - minK] = calculateSSE(clusters, centroids);
            kValues[k - minK] = k;
        }
        
        // 找二阶差分最小点（下降率减小最多，即肘点）
        double minCurvature = Double.MAX_VALUE;
        int optimalK = minK;
        
        // 计算一阶差分（相邻SSE的下降量）
        double[] firstDiff = new double[sseValues.length - 1];
        for (int i = 0; i < firstDiff.length; i++) {
            firstDiff[i] = sseValues[i] - sseValues[i + 1]; // 下降量（正值）
        }
        
        // 计算二阶差分（一阶差分的变化率）
        // 肘点特征：二阶差分最小（下降率变化最明显）
        double[] secondDiff = new double[firstDiff.length - 1];
        for (int i = 0; i < secondDiff.length; i++) {
            secondDiff[i] = firstDiff[i] - firstDiff[i + 1];
        }
        
        for (int i = 0; i < secondDiff.length; i++) {
            if (secondDiff[i] < minCurvature) {
                minCurvature = secondDiff[i];
                optimalK = kValues[i + 2]; // i对应firstDiff[i]，对应sseValues[i+1]
            }
        }
        
        return optimalK;
    }
    
    /**
     * K-means 聚类
     * @param customers 客户列表
     * @param k 聚类数量
     * @param maxIterations 最大迭代次数
     * @return 聚类结果，每组是一个客户列表
     */
    public static List<List<Customer>> kmeansClustering(List<Customer> customers, int k, int maxIterations) {
        if (customers.isEmpty() || k <= 0) {
            return new ArrayList<>();
        }
        
        // 初始化聚类中心（使用 K-means++ 初始化）
        List<double[]> centroids = kmeansPlusPlusInit(customers, k);
        
        // 迭代更新聚类中心
        List<List<Customer>> clusters = new ArrayList<>();
        for (int i = 0; i < k; i++) {
            clusters.add(new ArrayList<>());
        }
        
        for (int iter = 0; iter < maxIterations; iter++) {
            // 清空聚类
            for (List<Customer> cluster : clusters) {
                cluster.clear();
            }
            
            // 分配每个客户到最近的聚类中心
            for (Customer customer : customers) {
                int nearestCluster = 0;
                double minDist = Double.MAX_VALUE;
                
                for (int i = 0; i < centroids.size(); i++) {
                    double dist = Math.sqrt(
                        Math.pow(customer.getX() - centroids.get(i)[0], 2) +
                        Math.pow(customer.getY() - centroids.get(i)[1], 2)
                    );
                    if (dist < minDist) {
                        minDist = dist;
                        nearestCluster = i;
                    }
                }
                
                clusters.get(nearestCluster).add(customer);
            }
            
            // 更新聚类中心
            boolean converged = true;
            for (int i = 0; i < k; i++) {
                if (clusters.get(i).isEmpty()) continue;
                
                double[] newCentroid = calculateCentroid(clusters.get(i));
                if (Math.abs(newCentroid[0] - centroids.get(i)[0]) > 0.001 ||
                    Math.abs(newCentroid[1] - centroids.get(i)[1]) > 0.001) {
                    converged = false;
                }
                centroids.set(i, newCentroid);
            }
            
            if (converged) break;
        }
        
        // ========== 空聚类修复逻辑（符合README设计规范）==========
        // README规定：分成n个聚类，每个聚类对应一个发射点，每个聚类中的客户分配给对应的发射点
        // 确保每个聚类都有客户，不出现空聚类、空发射点
        for (int i = 0; i < k; i++) {
            if (clusters.get(i).isEmpty()) {
                // 找到客户最多的聚类，从中移入一个客户
                int maxCluster = 0;
                int maxSize = 0;
                for (int j = 0; j < k; j++) {
                    if (clusters.get(j).size() > maxSize) {
                        maxSize = clusters.get(j).size();
                        maxCluster = j;
                    }
                }
                
                // 从最大聚类中移入一个客户到空聚类
                if (maxSize > 1) {
                    Customer toMove = clusters.get(maxCluster).remove(0);
                    clusters.get(i).add(toMove);
                    // 更新两个聚类的中心点
                    centroids.set(i, calculateCentroid(clusters.get(i)));
                    centroids.set(maxCluster, calculateCentroid(clusters.get(maxCluster)));
                } else if (maxSize == 1) {
                    // 特殊情况：只有一个聚类有客户，将该客户分给空聚类
                    Customer toMove = clusters.get(maxCluster).remove(0);
                    clusters.get(i).add(toMove);
                    centroids.set(i, calculateCentroid(clusters.get(i)));
                    centroids.set(maxCluster, calculateCentroid(clusters.get(maxCluster)));
                }
            }
        }
        
        // 保留所有聚类，确保返回 k 个（此时所有聚类都应该非空）
        return clusters;
    }
    
    /**
     * K-means++ 初始化
     * 确保初始中心点分散，减少空聚类
     */
    private static List<double[]> kmeansPlusPlusInit(List<Customer> customers, int k) {
        List<double[]> centroids = new ArrayList<>();
        java.util.Random random = new java.util.Random();
        
        // 1. 随机选择第一个中心
        Customer first = customers.get(random.nextInt(customers.size()));
        centroids.add(new double[]{first.getX(), first.getY()});
        
        // 2. 选择剩余的 k-1 个中心
        while (centroids.size() < k) {
            double[] dists = new double[customers.size()];
            double totalDist = 0;
            
            // 计算每个客户到最近中心的距离平方 D²
            for (int i = 0; i < customers.size(); i++) {
                Customer c = customers.get(i);
                double minDist = Double.MAX_VALUE;
                for (double[] center : centroids) {
                    double d = Math.sqrt(Math.pow(c.getX() - center[0], 2) + 
                                        Math.pow(c.getY() - center[1], 2));
                    minDist = Math.min(minDist, d);
                }
                dists[i] = minDist * minDist;  // D²
                totalDist += dists[i];
            }
            
            // 按概率选择下一个中心（距离越远越可能被选中）
            double r = random.nextDouble() * totalDist;
            double cumulative = 0;
            for (int i = 0; i < customers.size(); i++) {
                cumulative += dists[i];
                if (cumulative >= r) {
                    Customer c = customers.get(i);
                    centroids.add(new double[]{c.getX(), c.getY()});
                    break;
                }
            }
        }
        
        return centroids;
    }
    
    /**
     * 计算聚类中心
     * @param customers 客户列表
     * @return 中心坐标 [x, y]
     */
    private static double[] calculateCentroid(List<Customer> customers) {
        if (customers.isEmpty()) {
            return new double[]{0, 0};
        }
        
        double sumX = 0, sumY = 0;
        for (Customer c : customers) {
            sumX += c.getX();
            sumY += c.getY();
        }
        
        return new double[]{sumX / customers.size(), sumY / customers.size()};
    }
    
    /**
     * 计算误差平方和（SSE）
     * @param clusters 聚类结果
     * @param centroids 聚类中心
     * @return SSE值
     */
    private static double calculateSSE(List<List<Customer>> clusters, List<double[]> centroids) {
        double sse = 0;
        
        for (int i = 0; i < clusters.size() && i < centroids.size(); i++) {
            for (Customer customer : clusters.get(i)) {
                double dist = Math.sqrt(
                    Math.pow(customer.getX() - centroids.get(i)[0], 2) +
                    Math.pow(customer.getY() - centroids.get(i)[1], 2)
                );
                sse += dist * dist;
            }
        }
        
        return sse;
    }
}
