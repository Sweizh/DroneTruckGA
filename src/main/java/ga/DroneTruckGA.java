package ga;

import java.util.List;

/**
 * 类名：DroneTruckGA
 * 功能：主程序类
 * 说明：实现卡车-无人机协同配送路径优化的遗传算法
 *      包含：初始化种群 → 评估适应度 → 选择/交叉/变异 → 输出结果
 */
public class DroneTruckGA {

    /**
     * 主函数：程序入口
     * 流程：
     *  1. 加载配置文件
     *  2. 运行GA算法
     *  3. 输出最优解和配送方案
     * @param args 命令行参数（未使用）
     */
    public static void main(String[] args) {
        // ========== 步骤1：打印标题 ==========
        System.out.println("===== 卡车-无人机协同配送路径优化 =====");
        System.out.println("基于遗传算法求解\n");

        // ========== 步骤2：加载配置 ==========
        // 从config.json文件加载所有参数
        Config config = ConfigLoader.load();

        // ========== 步骤3：打印问题信息和算法参数 ==========
        ProblemModel model = new ProblemModel(config);
        System.out.println("问题规模:");
        System.out.printf("  仓库数量: %d\n", model.getDepotCount());
        System.out.printf("  客户数量: %d\n", model.getCustomerCount());
        System.out.printf("  卡车数量: %d, 容量: %.0f, 速度: %.0f\n",
                model.getTruckCount(), model.getTruckCapacity(), model.getTruckSpeed());
        System.out.printf("  无人机数量: %d, 续航: %.0f, 最大载重: %.0f\n",
                model.getDroneCount(), model.getDroneRange(), model.getDroneMaxPayload());
        System.out.printf("\n遗传算法参数:\n");
        System.out.printf("  种群规模: %d\n", config.genetic.populationSize);
        System.out.printf("  最大迭代: %d\n", config.genetic.maxGenerations);
        System.out.printf("  交叉率: %.2f\n", config.genetic.crossoverRate);
        System.out.printf("  变异率: %.2f\n", config.genetic.mutationRate);
        System.out.println();

        // ========== 步骤4：运行GA算法 ==========
        GARunner.GAResult result = GARunner.runGA(config, progress -> {
            // 每50代打印一次进度
            if (progress.generation % 50 == 0) {
                System.out.printf("第%3d代: 最佳适应度=%.6f\n", progress.generation, progress.bestFitness);
            }
        }, null);

        // ========== 步骤5：输出最优解 ==========
        System.out.println("\n===== 最优解 =====");
        System.out.println("最优染色体: " + result.bestChromosome);
        System.out.printf("总时间: %.2f min\n", result.bestTime);
        System.out.printf("适应度: %.6f\n", result.bestFitness);
        System.out.printf("运行时间: %d ms\n", result.elapsedTime);

        // ========== 步骤6：输出配送方案 ==========
        System.out.println("\n仓库信息:");
        for (Point depot : model.getDepots()) {
            System.out.printf("  仓库%d: (%.1f, %.1f)\n", depot.getId(), depot.getX(), depot.getY());
        }

        System.out.println("\n配送方案:");
        // 输出每辆卡车的路线
        for (int t = 0; t < result.truckRoutes.size(); t++) {
            System.out.printf("  卡车%d路线: %s\n", t + 1, result.truckRoutes.get(t));
        }

        // 输出每架无人机的任务
        for (int d = 0; d < result.droneTasks.size(); d++) {
            System.out.printf("  无人机%d任务: %s\n", d + 1, result.droneTasks.get(d));
        }

        // 输出客户信息
        System.out.println("\n客户详情:");
        for (Customer customer : model.getCustomers()) {
            System.out.println("  " + customer);
        }
    }
}
