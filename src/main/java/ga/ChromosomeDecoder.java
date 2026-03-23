package ga;

import java.util.*;

/**
 * 染色体解码器
 * 符合 README 规范的三段式解码流程
 * 
 * 染色体结构：
 * - 段1: customerBlocks[N] - 每个客户所属发射点编号 (1~n)
 * - 段2: launchPointAssignment[K×Mmax] - 每辆卡车的发射点访问顺序
 * - 段3: launchPointCoords[2×n] - 发射点坐标 (x,y)
 * 
 * README 解码流程：
 * 步骤1: 解析客户分块 → 每个发射点服务的客户列表
 * 步骤2: 解析发射点分配 → 每辆卡车的发射点访问顺序
 * 步骤3: 解析发射点坐标 → 发射点位置
 * 步骤4: 生成配送方案
 */
public class ChromosomeDecoder {
    
    private ProblemModel model;
    private int K;
    private int n;
    private int Mmax;
    private int dronesPerTruck;
    
    /**
     * 解码结果
     */
    public static class DecodingResult {
        /** 卡车路线 [卡车idx] = [depotId, -lpId, -lpId, ..., depotId] */
        public List<List<Integer>> truckRoutes;
        
        /** 无人机任务 [无人机idx] = [客户id, ...] */
        public List<List<Integer>> droneTasks;
        
        /** 发射点坐标 [lpIdx] = {x, y} */
        public List<double[]> allLaunchPoints;
        
        /** 无人机→发射点映射 [无人机idx] = lpIdx (0-based)，-1表示未分配 */
        public List<Integer> droneToLaunchPoint;
        
        public DecodingResult() {
            this.truckRoutes = new ArrayList<>();
            this.droneTasks = new ArrayList<>();
            this.allLaunchPoints = new ArrayList<>();
            this.droneToLaunchPoint = new ArrayList<>();
        }
    }
    
    public ChromosomeDecoder(ProblemModel model) {
        this.model = model;
        this.K = model.getTruckCount();
        this.n = model.getLaunchPointCount();
        this.Mmax = (int) Math.ceil((double) n / K);
        this.dronesPerTruck = model.getDronesPerTruck();
    }
    
    /**
     * 解码染色体
     * 
     * @param chrom 三段式染色体
     * @return 解码结果
     */
    public DecodingResult decode(ChromosomeEncoder.Chromosome chrom) {
        DecodingResult result = new DecodingResult();
        
        // ===== 步骤3: 解析发射点坐标 =====
        for (int i = 0; i < n; i++) {
            result.allLaunchPoints.add(new double[]{
                chrom.launchPointCoords.get(i * 2),
                chrom.launchPointCoords.get(i * 2 + 1)
            });
        }
        
        // ===== 步骤1: 解析客户分块 → 每个发射点服务的客户列表 =====
        Map<Integer, List<Integer>> lpCustomers = new HashMap<>();
        for (int lpId = 1; lpId <= n; lpId++) {
            lpCustomers.put(lpId, new ArrayList<>());
        }
        
        List<Customer> customers = model.getCustomers();
        for (int i = 0; i < customers.size(); i++) {
            int lpId = chrom.customerBlocks.get(i);
            if (lpId >= 1 && lpId <= n) {
                lpCustomers.get(lpId).add(customers.get(i).getId());
            }
        }
        
        // ===== 步骤2: 解析发射点分配 → 每辆卡车的发射点访问顺序 =====
        // 从 launchPointAssignment 读取，按 K×Mmax 排列
        // truckLaunchPoints[卡车idx] = [发射点id, ...]
        List<List<Integer>> truckLaunchPoints = new ArrayList<>();
        for (int t = 0; t < K; t++) {
            truckLaunchPoints.add(new ArrayList<>());
            for (int m = 0; m < Mmax; m++) {
                int idx = t * Mmax + m;
                if (idx < chrom.launchPointAssignment.size()) {
                    int lpId = chrom.launchPointAssignment.get(idx);
                    if (lpId > 0) {
                        truckLaunchPoints.get(t).add(lpId);
                    }
                }
            }
        }
        
        // ===== 步骤4: 生成配送方案 =====
        
        // 4.1 构建卡车路线
        int depotId = model.getDepots().get(0).getId();
        for (int t = 0; t < K; t++) {
            List<Integer> route = new ArrayList<>();
            route.add(depotId);
            for (int lpId : truckLaunchPoints.get(t)) {
                route.add(-lpId);  // 负数表示发射点
            }
            route.add(depotId);
            result.truckRoutes.add(route);
        }
        
        // 4.2 构建无人机任务
        int droneCount = K * dronesPerTruck;
        for (int d = 0; d < droneCount; d++) {
            result.droneTasks.add(new ArrayList<>());
            result.droneToLaunchPoint.add(-1);  // 初始化为无效
        }
        
        // 遍历每个发射点，分配客户给无人机
        // 策略：每个发射点的所有客户分配给该卡车的一架无人机
        int[] dronesUsedPerTruck = new int[K];
        
        for (int lpId = 1; lpId <= n; lpId++) {
            List<Integer> custList = lpCustomers.get(lpId);
            if (custList.isEmpty()) continue;
            
            // 计算该发射点归属的卡车
            int truckIdx = (lpId - 1) % K;
            
            // 分配给当前卡车的一个无人机
            int droneIdx = truckIdx * dronesPerTruck + (dronesUsedPerTruck[truckIdx] % dronesPerTruck);
            
            result.droneTasks.get(droneIdx).addAll(custList);
            result.droneToLaunchPoint.set(droneIdx, lpId - 1);  // 0-based索引
            
            dronesUsedPerTruck[truckIdx]++;
        }
        
        return result;
    }
}
