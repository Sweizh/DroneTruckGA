package ga;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * 类名：ChromosomeEncoder
 * 功能：染色体编码器
 * 说明：实现三段式染色体编码
 *      编码格式：[客户分块编码][发射点分配与路径编码][发射点坐标编码]
 */
public class ChromosomeEncoder {
    /** 问题模型引用 */
    private final ProblemModel model;
    /** 随机数生成器 */
    private final Random random = new Random();
    /** 坐标编码精度（小数位数，1000=保留3位小数） */
    private static final int COORD_PRECISION = 1000;
    /** 调试计数器 */
    private static int chromosomeCount = 0;
    
    /**
     * 重置染色体计数器（每次GA运行时调用）
     */
    public static void resetCount() {
        chromosomeCount = 0;
    }
    
    /** 发射点数量 n */
    private int n;
    /** 卡车数量 K */
    private int K;
    /** 每辆卡车最大分配发射点数 M_max = ceil(n/K) */
    private int Mmax;

    /**
     * 三段式染色体结构
     */
    public static class Chromosome {
        /** 第一段：客户分块（每个客户所属发射点编号，1~n） */
        public List<Integer> customerBlocks;
        
        /** 第二段：发射点分配与访问顺序（K × M_max） */
        public List<Integer> launchPointAssignment;
        
        /** 第三段：发射点坐标（2 × n） */
        public List<Double> launchPointCoords;

        public Chromosome() {
            this.customerBlocks = new ArrayList<>();
            this.launchPointAssignment = new ArrayList<>();
            this.launchPointCoords = new ArrayList<>();
        }

        public Chromosome(List<Integer> customerBlocks, List<Integer> launchPointAssignment, List<Double> launchPointCoords) {
            this.customerBlocks = new ArrayList<>(customerBlocks);
            this.launchPointAssignment = new ArrayList<>(launchPointAssignment);
            this.launchPointCoords = new ArrayList<>(launchPointCoords);
        }

        public Chromosome copy() {
            return new Chromosome(this.customerBlocks, this.launchPointAssignment, this.launchPointCoords);
        }

        @Override
        public String toString() {
            return String.format("CustomerBlocks:%s | LaunchPointAssignment:%s | LaunchPointCoords:%s",
                customerBlocks, launchPointAssignment, launchPointCoords);
        }
    }
    
    /**
     * 构造函数
     */
    public ChromosomeEncoder(ProblemModel model) {
        this.model = model;
        this.n = model.getLaunchPointCount();
        this.K = model.getTruckCount();
        this.Mmax = (int) Math.ceil((double) n / K);
    }

    /**
     * 创建基础染色体（使用K-means聚类结果，不加扰动）
     */
    public Chromosome createBaseChromosome() {
        Chromosome chrom = new Chromosome();
        
        List<Customer> customers = model.getCustomers();
        double minX = model.getMinX();
        double maxX = model.getMaxX();
        double minY = model.getMinY();
        double maxY = model.getMaxY();
        
        // ===== 第二段：发射点分配（方案B：轮询分配）=====
        // 1. 先生成所有发射点 [1, 2, ..., n]
        // 2. 轮询分配给K辆卡车（发射点1给卡车1，发射点2给卡车2，...，发射点K给卡车K，发射点K+1给卡车1，...）
        List<List<Integer>> truckLPs = new ArrayList<>();
        for (int t = 0; t < K; t++) {
            truckLPs.add(new ArrayList<>());
        }
        for (int lpId = 1; lpId <= n; lpId++) {
            int truckIdx = (lpId - 1) % K;
            truckLPs.get(truckIdx).add(lpId);
        }
        // 用0填充到Mmax长度
        for (int t = 0; t < K; t++) {
            while (truckLPs.get(t).size() < Mmax) {
                truckLPs.get(t).add(0);
            }
        }
        // 展平为assignment
        List<Integer> assignment = new ArrayList<>();
        for (int t = 0; t < K; t++) {
            assignment.addAll(truckLPs.get(t));
        }
        chrom.launchPointAssignment = assignment;
        
        // ===== 第一段：K-means聚类结果 =====
        List<List<Customer>> clusters = ProblemModel.kmeansClustering(customers, n, 100);
        List<double[]> allCentroids = new ArrayList<>();
        for (List<Customer> cluster : clusters) {
            allCentroids.add(calculateCentroid(cluster));
        }
        while (allCentroids.size() < n) {
            Customer c = customers.get(random.nextInt(customers.size()));
            allCentroids.add(new double[]{c.getX(), c.getY()});
        }
        
        List<Integer> customerBlockIds = new ArrayList<>();
        for (int custIdx = 0; custIdx < customers.size(); custIdx++) {
            customerBlockIds.add(0);
        }
        for (int lpIdx = 0; lpIdx < clusters.size(); lpIdx++) {
            List<Customer> cluster = clusters.get(lpIdx);
            int lpId = lpIdx + 1;
            for (Customer customer : cluster) {
                for (int custIdx = 0; custIdx < customers.size(); custIdx++) {
                    if (customers.get(custIdx).getId() == customer.getId()) {
                        customerBlockIds.set(custIdx, lpId);
                        break;
                    }
                }
            }
        }
        List<Integer> validLpIds = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            if (!clusters.get(i).isEmpty()) {
                validLpIds.add(i + 1);
            }
        }
        for (int i = 0; i < customerBlockIds.size(); i++) {
            if (customerBlockIds.get(i) == 0) {
                customerBlockIds.set(i, validLpIds.get(random.nextInt(validLpIds.size())));
            }
        }
        chrom.customerBlocks = customerBlockIds;
        
        // ===== 第三段：聚类中心坐标 =====
        List<Double> coords = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            double x = allCentroids.get(i)[0];
            double y = allCentroids.get(i)[1];
            x = Math.max(minX, Math.min(maxX, x));
            y = Math.max(minY, Math.min(maxY, y));
            coords.add((double) Math.round(x * COORD_PRECISION) / COORD_PRECISION);
            coords.add((double) Math.round(y * COORD_PRECISION) / COORD_PRECISION);
        }
        chrom.launchPointCoords = coords;
        
        return chrom;
    }
    
    /**
     * 对基础染色体应用扰动
     */
    public Chromosome applyPerturbation(Chromosome base) {
        Chromosome chrom = base.copy();
        
        List<Customer> customers = model.getCustomers();
        int swapCount = Math.max(1, (int)(customers.size() * 0.05));
        for (int i = 0; i < swapCount; i++) {
            int c1 = random.nextInt(customers.size());
            int c2 = random.nextInt(customers.size());
            int temp = chrom.customerBlocks.get(c1);
            chrom.customerBlocks.set(c1, chrom.customerBlocks.get(c2));
            chrom.customerBlocks.set(c2, temp);
        }
        
        // 发射点分配：只打乱每辆卡车内部访问顺序，不破坏分配结构
        List<List<Integer>> truckLPs = new ArrayList<>();
        for (int t = 0; t < K; t++) {
            List<Integer> lps = new ArrayList<>();
            for (int m = 0; m < Mmax; m++) {
                int pos = t * Mmax + m;
                if (pos < chrom.launchPointAssignment.size()) {
                    int lpId = chrom.launchPointAssignment.get(pos);
                    lps.add(lpId);
                }
            }
            truckLPs.add(lps);
        }
        // 打乱每辆卡车内部的访问顺序
        for (int t = 0; t < K; t++) {
            Collections.shuffle(truckLPs.get(t), random);
        }
        // 重新组装
        chrom.launchPointAssignment.clear();
        for (int t = 0; t < K; t++) {
            List<Integer> lps = truckLPs.get(t);
            for (int m = 0; m < Mmax; m++) {
                if (m < lps.size()) {
                    chrom.launchPointAssignment.add(lps.get(m));
                } else {
                    chrom.launchPointAssignment.add(0);
                }
            }
        }
        
        double minX = model.getMinX();
        double maxX = model.getMaxX();
        double minY = model.getMinY();
        double maxY = model.getMaxY();
        
        double perturbRange = 5.0;
        for (int i = 0; i < chrom.launchPointCoords.size(); i++) {
            double perturb = (random.nextDouble() - 0.5) * perturbRange * 2;
            double newValue = chrom.launchPointCoords.get(i) + perturb;
            newValue = Math.max(minX, Math.min(maxX, newValue));
            chrom.launchPointCoords.set(i, (double) Math.round(newValue * COORD_PRECISION) / COORD_PRECISION);
        }
        
        return chrom;
    }
    
    /**
     * 创建完全随机的染色体
     */
    public Chromosome createFullyRandomChromosome() {
        Chromosome chrom = new Chromosome();
        
        List<Customer> customers = model.getCustomers();
        double minX = model.getMinX();
        double maxX = model.getMaxX();
        double minY = model.getMinY();
        double maxY = model.getMaxY();
        
        // ===== 第二段：发射点分配（方案B：轮询分配，打乱后再分配）=====
        List<Integer> allLPs = new ArrayList<>();
        for (int i = 1; i <= n; i++) {
            allLPs.add(i);
        }
        Collections.shuffle(allLPs, random);
        
        List<List<Integer>> truckLPs = new ArrayList<>();
        for (int t = 0; t < K; t++) {
            truckLPs.add(new ArrayList<>());
        }
        
        // 轮询分配所有发射点
        for (int lpIdx = 0; lpIdx < n; lpIdx++) {
            int truckIdx = lpIdx % K;
            truckLPs.get(truckIdx).add(allLPs.get(lpIdx));
        }
        
        // 用0填充到Mmax长度
        for (int t = 0; t < K; t++) {
            while (truckLPs.get(t).size() < Mmax) {
                truckLPs.get(t).add(0);
            }
        }
        
        List<Integer> assignment = new ArrayList<>();
        for (int t = 0; t < K; t++) {
            assignment.addAll(truckLPs.get(t));
        }
        chrom.launchPointAssignment = assignment;
        
        // ===== 第一段：K-means聚类结果 =====
        List<List<Customer>> clusters = ProblemModel.kmeansClustering(customers, n, 100);
        List<double[]> allCentroids = new ArrayList<>();
        for (List<Customer> cluster : clusters) {
            allCentroids.add(calculateCentroid(cluster));
        }
        while (allCentroids.size() < n) {
            Customer c = customers.get(random.nextInt(customers.size()));
            allCentroids.add(new double[]{c.getX(), c.getY()});
        }
        
        List<Integer> customerBlockIds = new ArrayList<>();
        for (int custIdx = 0; custIdx < customers.size(); custIdx++) {
            customerBlockIds.add(0);
        }
        for (int lpIdx = 0; lpIdx < clusters.size(); lpIdx++) {
            List<Customer> cluster = clusters.get(lpIdx);
            int lpId = lpIdx + 1;
            for (Customer customer : cluster) {
                for (int custIdx = 0; custIdx < customers.size(); custIdx++) {
                    if (customers.get(custIdx).getId() == customer.getId()) {
                        customerBlockIds.set(custIdx, lpId);
                        break;
                    }
                }
            }
        }
        List<Integer> validLpIds = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            if (!clusters.get(i).isEmpty()) {
                validLpIds.add(i + 1);
            }
        }
        for (int i = 0; i < customerBlockIds.size(); i++) {
            if (customerBlockIds.get(i) == 0) {
                customerBlockIds.set(i, validLpIds.get(random.nextInt(validLpIds.size())));
            }
        }
        chrom.customerBlocks = customerBlockIds;
        
        // ===== 第三段：随机坐标 =====
        List<Double> coords = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            coords.add((double) Math.round((minX + random.nextDouble() * (maxX - minX)) * COORD_PRECISION) / COORD_PRECISION);
            coords.add((double) Math.round((minY + random.nextDouble() * (maxY - minY)) * COORD_PRECISION) / COORD_PRECISION);
        }
        chrom.launchPointCoords = coords;
        
        chromosomeCount++;
        return chrom;
    }
    
    /**
     * 创建随机染色体
     */
    public Chromosome createRandomChromosome() {
        Chromosome chrom = new Chromosome();
        
        List<Customer> customers = model.getCustomers();
        double minX = model.getMinX();
        double maxX = model.getMaxX();
        double minY = model.getMinY();
        double maxY = model.getMaxY();
        
        // ===== 第二段：发射点分配（方案B：轮询分配）=====
        List<Integer> allLPs = new ArrayList<>();
        for (int i = 1; i <= n; i++) {
            allLPs.add(i);
        }
        Collections.shuffle(allLPs, random);
        
        List<List<Integer>> truckLPs = new ArrayList<>();
        for (int t = 0; t < K; t++) {
            truckLPs.add(new ArrayList<>());
        }
        
        // 轮询分配所有发射点
        for (int lpIdx = 0; lpIdx < n; lpIdx++) {
            int truckIdx = lpIdx % K;
            truckLPs.get(truckIdx).add(allLPs.get(lpIdx));
        }
        
        // 用0填充到Mmax长度
        for (int t = 0; t < K; t++) {
            while (truckLPs.get(t).size() < Mmax) {
                truckLPs.get(t).add(0);
            }
        }
        
        List<Integer> assignment = new ArrayList<>();
        for (int t = 0; t < K; t++) {
            assignment.addAll(truckLPs.get(t));
        }
        chrom.launchPointAssignment = assignment;
        
        // ===== 第一段：客户分块 =====
        // K-means聚类获取所有候选发射点坐标（根据README：分成n个聚类，每个聚类对应一个发射点）
        List<List<Customer>> clusters = ProblemModel.kmeansClustering(customers, n, 100);
        List<double[]> allCentroids = new ArrayList<>();
        
        // 验证聚类结果符合README规范
        boolean allClustersNonEmpty = true;
        for (List<Customer> cluster : clusters) {
            if (cluster.isEmpty()) {
                allClustersNonEmpty = false;
                break;
            }
            allCentroids.add(calculateCentroid(cluster));
        }
        
        // 如果聚类数量少于n，添加额外中心点
        while (allCentroids.size() < n) {
            Customer c = customers.get(random.nextInt(customers.size()));
            allCentroids.add(new double[]{c.getX(), c.getY()});
        }
        
        // 直接继承K-means聚类结果作为客户分配
        // 每个聚类中的客户分配给对应的发射点
        List<Integer> customerBlockIds = new ArrayList<>();
        
        // 确保customerBlockIds的大小与客户数量一致
        for (int custIdx = 0; custIdx < customers.size(); custIdx++) {
            customerBlockIds.add(0); // 先占位
        }
        
        // 遍历每个聚类，将聚类中的客户分配到对应的发射点编号
        for (int lpIdx = 0; lpIdx < clusters.size(); lpIdx++) {
            List<Customer> cluster = clusters.get(lpIdx);
            int lpId = lpIdx + 1; // 发射点编号从1开始
            
            for (Customer customer : cluster) {
                // 找到客户在customers列表中的索引
                for (int custIdx = 0; custIdx < customers.size(); custIdx++) {
                    if (customers.get(custIdx).getId() == customer.getId()) {
                        customerBlockIds.set(custIdx, lpId);
                        break;
                    }
                }
            }
        }
        
        // 确保所有客户都被分配（如果某个客户没有被分配，随机分配到一个有客户的发射点）
        List<Integer> validLpIds = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            if (!clusters.get(i).isEmpty()) {
                validLpIds.add(i + 1);
            }
        }
        for (int i = 0; i < customerBlockIds.size(); i++) {
            if (customerBlockIds.get(i) == 0) {
                customerBlockIds.set(i, validLpIds.get(random.nextInt(validLpIds.size())));
            }
        }
        chrom.customerBlocks = customerBlockIds;
        
        // ===== 第三段：发射点坐标 =====
        List<Double> coords = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            double x = allCentroids.get(i)[0];
            double y = allCentroids.get(i)[1];
            x = Math.max(minX, Math.min(maxX, x));
            y = Math.max(minY, Math.min(maxY, y));
            coords.add((double) Math.round(x * COORD_PRECISION) / COORD_PRECISION);
            coords.add((double) Math.round(y * COORD_PRECISION) / COORD_PRECISION);
        }
        chrom.launchPointCoords = coords;
        
        chromosomeCount++;
        
        return chrom;
    }
    
    /**
     * 计算客户列表的中心点
     */
    private double[] calculateCentroid(List<Customer> customers) {
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
     * 验证染色体有效性（三段式编码）
     */
    public void validateChromosome(Chromosome chromosome) {
        // 验证第一段：客户分块
        if (chromosome.customerBlocks.size() != model.getCustomerCount()) {
            throw new IllegalArgumentException("客户分块段长度必须等于客户数量");
        }

        // 验证客户分块编号范围
        for (int blockId : chromosome.customerBlocks) {
            if (blockId < 1 || blockId > n) {
                throw new IllegalArgumentException("客户分块编号必须在1~" + n + "范围内");
            }
        }

        // 验证第二段：发射点分配长度
        int expectedAssignmentLen = K * Mmax;
        if (chromosome.launchPointAssignment.size() != expectedAssignmentLen) {
            throw new IllegalArgumentException("发射点分配段长度必须等于" + expectedAssignmentLen);
        }

        // 验证发射点分配编号范围
        for (int lpId : chromosome.launchPointAssignment) {
            if (lpId < 0 || lpId > n) {
                throw new IllegalArgumentException("发射点分配编号必须在0~" + n + "范围内");
            }
        }

        // 验证第三段：发射点坐标数量
        int expectedCoords = n * 2;
        if (chromosome.launchPointCoords.size() != expectedCoords) {
            throw new IllegalArgumentException("发射点坐标数量必须等于" + expectedCoords);
        }
    }

    /**
     * 获取染色体总长度
     */
    public int getChromosomeLength() {
        return model.getCustomerCount()  // 第一段：客户分块
             + K * Mmax                  // 第二段：发射点分配
             + n * 2;                    // 第三段：发射点坐标
    }

    /**
     * 将染色体转换为整数列表（供GAOperators使用）
     */
    public List<Integer> toIntegerList(Chromosome chromosome) {
        List<Integer> result = new ArrayList<>();

        // 第一段：客户分块
        result.addAll(chromosome.customerBlocks);

        // 第二段：发射点分配（转换为整数）
        result.addAll(chromosome.launchPointAssignment);

        // 第三段：发射点坐标（转换为整数编码）
        for (Double coord : chromosome.launchPointCoords) {
            result.add((int) (coord * COORD_PRECISION));
        }

        return result;
    }

    /**
     * 按距离发射点由近到远排序客户
     */
    private List<Integer> sortCustomersByDistance(List<Integer> customers, double[] launchPoint) {
        List<Integer> sorted = new ArrayList<>(customers);
        Collections.sort(sorted, (c1, c2) -> {
            Customer cust1 = model.getCustomerById(c1);
            Customer cust2 = model.getCustomerById(c2);
            if (cust1 == null || cust2 == null) return 0;

            double dist1 = Math.sqrt(Math.pow(cust1.getX() - launchPoint[0], 2) +
                                        Math.pow(cust1.getY() - launchPoint[1], 2));
            double dist2 = Math.sqrt(Math.pow(cust2.getX() - launchPoint[0], 2) +
                                        Math.pow(cust2.getY() - launchPoint[1], 2));
            return Double.compare(dist1, dist2);
        });
        return sorted;
    }

    /**
     * 完整解码染色体（三段式编码）
     * 
    /**
     * 从整数列表创建染色体
     */
    public Chromosome fromIntegerList(List<Integer> intList) {
        Chromosome chrom = new Chromosome();

        int customerCount = model.getCustomerCount();

        // 第一段：客户分块
        for (int i = 0; i < customerCount; i++) {
            chrom.customerBlocks.add(intList.get(i));
        }

        // 第二段：发射点分配
        int assignmentLen = K * Mmax;
        for (int i = 0; i < assignmentLen; i++) {
            int idx = customerCount + i;
            chrom.launchPointAssignment.add(intList.get(idx));
        }

        // 第三段：发射点坐标
        for (int i = 0; i < n * 2; i++) {
            int idx = customerCount + assignmentLen + i;
            chrom.launchPointCoords.add(intList.get(idx) / (double) COORD_PRECISION);
        }

        return chrom;
    }
}
