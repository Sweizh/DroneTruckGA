package ga;

import java.util.List;

/**
 * 配置类，用于加载和存储JSON配置文件中的参数
 * 通过Jackson库从config.json文件加载所有配置参数
 */
public class Config {

    // ==================== 问题配置 ====================

    /**
     * 问题配置类：定义配送问题的基本参数
     * 包含仓库列表和客户点列表
     * 
     * 调用位置：
     * - ProblemModel.java: 初始化问题模型
     * - ChromosomeEncoder.java: 获取客户数量用于染色体编码
     * - CostCalculator.java: 计算配送成本和时间
     */
    public static class Problem {
        /** 
         * 仓库列表（支持多个仓库）
         * 
         * 调用位置：
         * - MapPreviewPanel.java: 绘制仓库位置
         * - ChromosomeEncoder.java: 获取仓库坐标
         */
        public List<Depot> depots;
        
        /**
         * 客户点列表
         * 
         * 调用位置：
         * - MapPreviewPanel.java: 绘制客户位置
         * - ChromosomeEncoder.java: 获取客户坐标用于解码
         * - CostCalculator.java: 计算到客户的距离
         * - FitnessFunction.java: 获取客户服务时间
         */
        public List<Customer> customers;
    }

    /**
     * 仓库配置类：定义仓库的坐标位置
     * 
     * 调用位置：
     * - MapPreviewPanel.java: 绘制仓库图标
     * - CostCalculator.java: getPointCoords() 获取仓库坐标
     */
    public static class Depot {
        /** 仓库ID，用于标识不同仓库 */
        public int id;
        
        /** 仓库X坐标（单位：km），地图上的横坐标位置 */
        public double x;
        
        /** 仓库Y坐标（单位：km），地图上的纵坐标位置 */
        public double y;
    }

    /**
     * 客户配置类：定义客户的位置和需求
     * 
     * 调用位置：
     * - MapPreviewPanel.java: 绘制客户图标
     * - ChromosomeEncoder.java: 获取客户坐标和需求量
     * - CostCalculator.java: 计算配送距离和服务时间
     */
    public static class Customer {
        /** 客户ID，从1开始编号 */
        public int id;
        
        /** X坐标（单位：km），客户在地图上的横坐标位置 */
        public double x;
        
        /** Y坐标（单位：km），客户在地图上的纵坐标位置 */
        public double y;
        
        /** 需求量（单位：kg），客户需要的货物重量 */
        public double demand;
        
        /** 所属仓库ID，指定该客户由哪个仓库配送
         * 
         * 调用位置：
         * - ChromosomeEncoder.java: 解码时确定客户归属
         */
        public int depotId;
        
        /** 时间窗数组：[开始时间, 结束时间]（单位：min）
         * 
         * 调用位置：
         * - CostCalculator.java: calculateExtraTimeCost() 计算时间窗惩罚
         */
        public double[] timeWindow;
        
        /** 服务时间（单位：min），到达后需要花费的时间进行装卸货
         * 
         * 调用位置：
         * - CostCalculator.java: calculateTotalTime() 计算总服务时间
         */
        public double serviceTime;
    }

    // ==================== 车辆配置 ====================

    /**
     * 车辆配置类：包含卡车和无人机的配置
     * 
     * 调用位置：
     * - ProblemModel.java: 初始化车辆参数
     * - CostCalculator.java: 获取车辆速度和成本参数
     */
    public static class Vehicles {
        /**
         * 卡车配置
         * 
         * 调用位置：
         * - ProblemModel.java: 获取卡车数量和速度
         * - CostCalculator.java: 计算卡车行驶时间和成本
         * - GAOperators.java: 变异发射点时获取卡车数量
         */
        public Trucks trucks;
        
        /**
         * 无人机配置
         * 
         * 调用位置：
         * - ProblemModel.java: 获取无人机参数
         * - CostCalculator.java: 计算无人机飞行时间和充电时间
         */
        public Drones drones;
    }

    /**
     * 卡车配置类：定义卡车的性能参数
     * 
     * 调用位置：
     * - ProblemModel.java: getTruckCount(), getTruckSpeed() 等
     * - CostCalculator.java: calculateTruckCost(), calculateTruckTime()
     */
    public static class Trucks {
        /** 
         * 卡车数量
         * 
         * 调用位置：
         * - ProblemModel.java: 初始化发射点历史记录
         * - GAOperators.java: gaussianMutate() 遍历每辆卡车
         * - ChromosomeEncoder.java: 解码卡车路线
         */
        public int count;
        
        /** 
         * 最大载重（单位：kg）
         * 
         * 调用位置：
         * - CostCalculator.java: 检查卡车是否超载
         */
        public double capacity;
        
        /** 
         * 行驶速度（单位：km/h）
         * 用于计算卡车行驶时间 = 距离 / 速度 × 60
         * 
         * 调用位置：
         * - CostCalculator.java: calculateTotalTime() 计算卡车行驶时间
         * - CostCalculator.java: calculateExtraTimeCost() 计算超续航惩罚
         */
        public double speed;
        
        /** 
         * 每公里行驶成本（单位：元/km）
         * 
         * 调用位置：
         * - CostCalculator.java: calculateTruckCost() 计算卡车行驶成本
         */
        public double costPerKm;
        
        /** 
         * 固定成本（单位：元），每辆卡车出车的固定费用
         * 
         * 调用位置：
         * - CostCalculator.java: calculateFixedCost() 计算固定成本
         */
        public double fixedCost;
        
        /** 
         * 单卡车可携无人机数量
         * 总无人机数 = 卡车数量 × 每辆搭载数
         * 
         * 调用位置：
         * - ProblemModel.java: 计算总无人机数
         * - ChromosomeEncoder.java: 解码时分配客户到无人机
         */
        public int dronesPerTruck;
        
        /** 
         * 最大续航里程（单位：km）
         * 超过此距离需要返回仓库充电，设为0或负数表示无限续航
         * 
         * 调用位置：
         * - CostCalculator.java: 检查卡车路线是否超续航
         */
        public double range;
    }

    /**
     * 无人机配置类：定义无人机的性能参数
     * 注意：无人机数量由 卡车数量 × 每辆卡车搭载数 自动计算
     * 
     * 调用位置：
     * - ProblemModel.java: getDroneCount(), getDroneSpeed() 等
     * - CostCalculator.java: calculateDroneCost(), calculateDroneTime()
     */
    public static class Drones {
        /**
         * @deprecated 不再使用，总无人机数 = 卡车数量 × 每辆卡车搭载数
         */
        @Deprecated
        public int count;
        
        /** 
         * 最大续航里程（单位：km），超过此距离需要返回发射点充电
         * 
         * 调用位置：
         * - ChromosomeEncoder.java: planDroneRoute() 检查续航约束
         */
        public double range;
        
        /** 
         * 飞行速度（单位：km/h）
         * 用于计算无人机飞行时间 = 距离 / 速度 × 60
         * 
         * 调用位置：
         * - CostCalculator.java: calculateTotalTime() 计算无人机飞行时间
         * - FitnessFunction.java: distancePenaltyTime 转换
         */
        public double speed;
        
        /** 
         * 最大载重（单位：kg），超过则无法配送
         * 
         * 调用位置：
         * - ChromosomeEncoder.java: planDroneRoute() 检查载重约束
         */
        public double maxPayload;
        
        /** 
         * 每公里飞行成本（单位：元/km）
         * 
         * 调用位置：
         * - CostCalculator.java: calculateDroneCost() 计算无人机配送成本
         */
        public double costPerKm;
        
        /** 
         * 充电时间（单位：min），无人机没电后返回发射点充电的时间
         * 
         * 调用位置：
         * - CostCalculator.java: calculateTotalTime() 计算总充电时间
         */
        public double rechargeTime;
        
        /** 
         * 充电成本（单位：元），每次充电的费用
         * 
         * 调用位置：
         * - CostCalculator.java: calculateDroneCost() 计算充电成本
         */
        public double rechargeCost;
        
        /** 
         * 服务时间（单位：min），无人机服务每位客户的时间
         * 
         * 调用位置：
         * - CostCalculator.java: calculateTotalTime() 计算总服务时间
         */
        public double serviceTime;
    }

    // ==================== 发射点配置 ====================

    /**
     * 发射点配置类：定义发射点的初始和进化参数
     * 发射点是卡车停靠、无人机起飞/降落的地点
     * 
     * 调用位置：
     * - ProblemModel.java: 初始化发射点历史
     * - GAOperators.java: gaussianMutate() 变异发射点位置
     */
    public static class LaunchPointConfig {
        /** 
         * 初始偏移范围（单位：km）
         * 发射点初始位置 = 客户坐标 ± 偏移范围
         * 
         * 调用位置：
         * - ChromosomeEncoder.java: createRandomChromosome() 初始化发射点
         */
        public double initialOffset = 5.0;
        
        /** 
         * 变异范围（单位：km）
         * 发射点变异时探索的最大距离
         * 
         * 调用位置：
         * - GAOperators.java: gaussianMutate() 计算变异步长
         */
        public double mutationRange = 3.0;
        
        /** 
         * 历史方向影响概率（0-1）
         * 使用历史成功方向进行变异的概率
         * 值越大，越倾向于使用历史方向；值越小，越倾向于随机探索
         * 
         * 调用位置：
         * - GAOperators.java: gaussianMutate() 决定是否使用历史方向
         */
        public double historyInfluence = 0.5;
        
        /** 
         * 历史记录大小
         * 保存的成功变异方向数量
         */
        public int historySize = 20;
        
        /** 
         * 是否启用自适应变异范围
         * 启用后，变异范围随迭代次数增加而减小
         * 
         * 调用位置：
         * - GAOperators.java: gaussianMutate() 自适应调整范围
         */
        public boolean adaptiveRange = true;
        
        /** 
         * 成功阈值
         * 适应度提升多少算"成功"，用于记录历史方向
         * 
         * 调用位置：
         * - GAOperators.java: createNextGeneration() 判断是否记录方向
         */
        public double successThreshold = 0.0001;
        
        /** 
         * 发射点数量
         * 0: 使用肘部法则自动确定
         * >0: 使用手动指定的数量
         * 
         * 调用位置：
         * - ProblemModel.java: 初始化时确定发射点数量
         */
        public int launchPointCount = 0;
        
        /** 
         * 发射点数/卡车数 最小比例
         * 确保 n >= K * minLaunchPointsRatio
         * 
         * 调用位置：
         * - ProblemModel.java: 验证发射点数量约束
         */
        public double minLaunchPointsRatio = 1.0;
        
        /** 
         * K-means 聚类最大迭代次数
         * 
         * 调用位置：
         * - ChromosomeEncoder.java: K-means 聚类
         */
        public int kmeansMaxIterations = 100;
    }

    // ==================== 遗传算法配置 ====================

    /**
     * 遗传算法配置类：定义GA的控制参数
     * 
     * 调用位置：
     * - GARunner.java: 创建下一代种群
     * - GAOperators.java: 执行选择、交叉、变异操作
     */
    public static class Genetic {
        /** 
         * 种群规模：每代包含的个体数量
         * 较大值提高解的质量，但增加计算时间
         * 
         * 调用位置：
         * - GARunner.java: 初始化种群数量
         * - GAOperators.java: 创建下一代时使用
         */
        public int populationSize;
        
        /** 
         * 最大迭代次数：GA运行的代数
         * 较多迭代次数有助于收敛到更优解
         * 
         * 调用位置：
         * - GARunner.java: 控制主循环次数
         */
        public int maxGenerations;
        
        /** 
         * 交叉率：进行交叉操作的概率
         * 通常取0.6-0.9，过高可能破坏优良解
         * 
         * 调用位置：
         * - GAOperators.java: crossover() 判断是否执行交叉
         */
        public double crossoverRate;
        
        /** 
         * 变异率：进行变异操作的概率
         * 通常取0.01-0.2，过高会变成随机搜索
         * 
         * 调用位置：
         * - GAOperators.java: mutate() 判断是否执行变异
         */
        public double mutationRate;
        
        /** 
         * 精英率：每代保留最优个体的比例
         * 通常取0.05-0.2，确保最优解不丢失
         * 
         * 调用位置：
         * - GAOperators.java: createNextGeneration() 保留精英个体
         */
        public double eliteRate;
        
        /** 
         * 锦标赛规模：用于选择操作的每次参与的个体数
         * 较大值增加选择压力，较快收敛
         * 
         * 调用位置：
         * - GAOperators.java: tournamentSelect() 选择父代
         */
        public int tournamentSize;
    }

    // ==================== 惩罚参数配置 ====================

    /**
     * 惩罚参数配置类：定义各种惩罚系数
     * 惩罚用于约束解的可行性，超出约束会增加惩罚值
     * 
     * 调用位置：
     * - CostCalculator.java: calculatePenaltyCost() 计算惩罚
     * - CostCalculator.java: calculateExtraTimeCost() 计算时间惩罚
     * - FitnessFunction.java: 计算有效时间
     */
    public static class PenaltyConfig {
        /** 
         * 是否启用惩罚计算
         * 禁用后，所有约束都不产生惩罚
         */
        public boolean enabled = true;
        
        /** 
         * 时间窗提前到达惩罚系数（单位：元/分钟）
         * 早到等待时间 × 惩罚系数 = 早到惩罚
         * 
         * 调用位置：
         * - CostCalculator.java: calculateExtraTimeCost() 等待时间惩罚
         */
        public double timeWindowEarly = 2.0;
        
        /** 
         * 时间窗延误到达惩罚系数（单位：元/分钟）
         * 延误时间 × 惩罚系数 = 延误惩罚
         * 
         * 调用位置：
         * - CostCalculator.java: calculateExtraTimeCost() 延误时间惩罚
         */
        public double timeWindowLate = 50.0;
        
        /** 
         * 无人机续航超限惩罚系数（单位：元/km）
         * 超续航距离 × 惩罚系数 = 无人机续航惩罚
         * 
         * 调用位置：
         * - CostCalculator.java: calculatePenaltyCost() 无人机续航惩罚
         */
        public double droneRangePenalty = 50.0;
        
        /** 
         * 无人机载重超限惩罚系数（单位：元/kg）
         * 超载重量 × 惩罚系数 = 无人机载重惩罚
         * 
         * 调用位置：
         * - CostCalculator.java: calculatePenaltyCost() 无人机载重惩罚
         */
        public double dronePayloadPenalty = 20.0;
        
        /** 
         * 等待时间惩罚系数（单位：元/分钟）
         * 无人机等待时间 × 惩罚系数 = 等待惩罚
         * 
         * 调用位置：
         * - CostCalculator.java: calculateExtraTimeCost() 无人机等待惩罚
         */
        public double waitTimePenalty = 2.0;
        
        /** 
         * 延误时间惩罚系数（单位：元/分钟）
         * 无人机延误时间 × 惩罚系数 = 延误惩罚
         * 
         * 调用位置：
         * - CostCalculator.java: calculateExtraTimeCost() 无人机延误惩罚
         */
        public double delayTimePenalty = 50.0;
        
        /** 
         * 卡车续航超限时间惩罚系数（单位：元/分钟）
         * 超续航时间 × 惩罚系数 = 卡车续航惩罚
         * 
         * 调用位置：
         * - CostCalculator.java: calculateExtraTimeCost() 卡车续航惩罚
         */
        public double truckRangeTimePenalty = 5.0;
        
        /** 
         * 卡车续航超限距离惩罚系数（单位：元/km）
         * 超续航距离 × 惩罚系数 = 卡车续航距离惩罚
         * 
         * 调用位置：
         * - CostCalculator.java: calculatePenaltyCost() 卡车续航惩罚
         */
        public double truckRangeDistancePenalty = 50.0;
        
        /** 
         * 距离惩罚系数（单位：元/km）
         * 发射点到客户的距离 × 惩罚系数 = 距离惩罚
         * 用于促使GA优化发射点位置，使其靠近客户群
         * 
         * 调用位置：
         * - CostCalculator.java: calculateDistancePenalty() 计算距离惩罚
         * - FitnessFunction.java: distancePenaltyTime 转换后加入有效时间
         */
        public double distancePenalty = 50.0;
    }

    /**
     * 优化目标配置类：定义优化目标及相关参数
     * 
     * 调用位置：
     * - FitnessFunction.java: 计算综合目标值
     * - ProblemModel.java: 获取优化目标类型
     */
    public static class Optimization {
        /** 
         * 优化目标类型：cost=成本优先, time=时间优先, both=双目标加权
         * 
         * 调用位置：
         * - FitnessFunction.java: 判断优化目标类型
         */
        public String objective;
        
        /** 
         * 成本权重（双目标加权时使用）
         * 
         * 调用位置：
         * - FitnessFunction.java: 计算综合适应度
         */
        public double costWeight;
        
        /** 
         * 时间权重（双目标加权时使用）
         * 
         * 调用位置：
         * - FitnessFunction.java: 计算综合适应度
         */
        public double timeWeight;
    }

    // ==================== 归一化参数配置 ====================

    /**
     * 归一化参数配置类：定义适应度计算的归一化参数
     * 用于将不同量纲的值归一化到0-1范围
     * 
     * 调用位置：
     * - FitnessFunction.java: 归一化处理
     */
    public static class NormalizationConfig {
        /** 
         * 成本归一化最大值（单位：元）
         * 用于将成本归一化到0-1范围
         * 
         * 调用位置：
         * - FitnessFunction.java: 归一化成本值
         */
        public double maxCost = 10000.0;
        
        /** 
         * 时间归一化最大值（单位：分钟）
         * 用于将时间归一化到0-1范围
         * 
         * 调用位置：
         * - FitnessFunction.java: 归一化时间值
         */
        public double maxTime = 10000.0;
    }

    // ==================== 主配置字段 ====================

    /** 问题配置对象 */
    public Problem problem;
    
    /** 车辆配置对象 */
    public Vehicles vehicles;
    
    /** 发射点配置对象 */
    public LaunchPointConfig launchPoint = new LaunchPointConfig();
    
    /** 优化目标配置对象 */
    public Optimization optimization;
    
    /** 遗传算法配置对象 */
    public Genetic genetic;
    
    /** 惩罚参数配置对象 */
    public PenaltyConfig penalty = new PenaltyConfig();
    
    /** 归一化参数配置对象 */
    public NormalizationConfig normalization = new NormalizationConfig();
}
