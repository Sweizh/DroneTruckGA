package ga;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * 类名：GAOperators
 * 功能：遗传算子类，实现选择、交叉、变异等遗传操作
 * 说明：支持三段式染色体编码的分层遗传算子
 */
public class GAOperators {
    /** 问题模型引用 */
    private final ProblemModel model;
    /** 成本计算器引用 */
    private final CostCalculator costCalculator;
    /** 染色体编码器引用 */
    private final ChromosomeEncoder encoder;
    /** 配置引用 */
    private Config config;
    /** 随机数生成器 */
    private final Random random = new Random();

    /**
     * 构造函数
     */
    public GAOperators(ProblemModel model) {
        this.model = model;
        this.costCalculator = new CostCalculator(model);
        this.encoder = new ChromosomeEncoder(model);
    }

    /**
     * 设置配置
     */
    public void setConfig(Config config) {
        this.config = config;
    }

    /**
     * 锦标赛选择算子
     */
    public ChromosomeEncoder.Chromosome tournamentSelect(
            List<ChromosomeEncoder.Chromosome> population,
            List<Double> fitness,
            int tournamentSize) {
        ChromosomeEncoder.Chromosome best = null;
        double bestFitness = -1;

        for (int i = 0; i < tournamentSize; i++) {
            int idx = random.nextInt(population.size());
            if (fitness.get(idx) > bestFitness) {
                bestFitness = fitness.get(idx);
                best = population.get(idx).copy();
            }
        }
        return best;
    }

    /**
     * 三段式染色体交叉
     * 使用段交叉：完整复制一段，另一段随机选择
     */
    public ChromosomeEncoder.Chromosome crossover(
            ChromosomeEncoder.Chromosome parent1,
            ChromosomeEncoder.Chromosome parent2) {

        ChromosomeEncoder.Chromosome child = new ChromosomeEncoder.Chromosome();

        // 随机选择从哪个父本继承完整段
        int segmentChoice = random.nextInt(3);
        
        // 段1：客户分块
        if (segmentChoice == 0 || random.nextBoolean()) {
            child.customerBlocks = new ArrayList<>(parent1.customerBlocks);
        } else {
            child.customerBlocks = new ArrayList<>(parent2.customerBlocks);
        }
        
        // 段2：发射点分配
        double lpCrossoverRate = (config != null) ? config.genetic.crossoverRate : 0.8;
        if (random.nextDouble() < lpCrossoverRate) {
            child.launchPointAssignment = new ArrayList<>(parent1.launchPointAssignment);
        } else {
            child.launchPointAssignment = new ArrayList<>(parent2.launchPointAssignment);
        }
        
        // 段3：发射点坐标 - BLX-α交叉
        child.launchPointCoords = blxAlphaCrossover(parent1.launchPointCoords, parent2.launchPointCoords);

        // 修复染色体：确保每辆卡车都有发射点
        repairChromosome(child);

        return child;
    }

    /**
     * 修复染色体：确保每辆卡车都有发射点，且发射点编号在有效范围1~n内
     */
    private void repairChromosome(ChromosomeEncoder.Chromosome chromosome) {
        int K = model.getTruckCount();
        int Mmax = (int) Math.ceil((double) model.getLaunchPointCount() / K);
        int n = chromosome.launchPointCoords.size() / 2;
        
        boolean repaired = false;
        
        // 第一步：修复launchPointAssignment，确保发射点编号在1~n范围内
        for (int i = 0; i < chromosome.launchPointAssignment.size(); i++) {
            int lpId = chromosome.launchPointAssignment.get(i);
            if (lpId < 0 || lpId > n) {
                chromosome.launchPointAssignment.set(i, 0);
                repaired = true;
            }
        }
        
        // 检查每辆卡车是否有发射点
        for (int truckIdx = 0; truckIdx < K; truckIdx++) {
            boolean hasLaunchPoint = false;
            for (int m = 0; m < Mmax; m++) {
                int pos = truckIdx * Mmax + m;
                if (pos < chromosome.launchPointAssignment.size() && 
                    chromosome.launchPointAssignment.get(pos) > 0) {
                    hasLaunchPoint = true;
                    break;
                }
            }
            
            // 如果这辆卡车没有发射点，从其他卡车借一个
            if (!hasLaunchPoint) {
                // 找到有额外发射点的卡车
                for (int otherTruck = 0; otherTruck < K; otherTruck++) {
                    if (otherTruck == truckIdx) continue;
                    for (int m = 0; m < Mmax; m++) {
                        int pos = otherTruck * Mmax + m;
                        if (pos < chromosome.launchPointAssignment.size() && 
                            chromosome.launchPointAssignment.get(pos) > 0) {
                            
                            // 借一个发射点给当前卡车
                            int lpId = chromosome.launchPointAssignment.get(pos);
                            // 找到当前卡车的第一个空位
                            for (int m2 = 0; m2 < Mmax; m2++) {
                                int targetPos = truckIdx * Mmax + m2;
                                if (targetPos < chromosome.launchPointAssignment.size() && 
                                    chromosome.launchPointAssignment.get(targetPos) == 0) {
                                    chromosome.launchPointAssignment.set(targetPos, lpId);
                                    chromosome.launchPointAssignment.set(pos, 0);
                                    hasLaunchPoint = true;
                                    break;
                                }
                            }
                            if (hasLaunchPoint) break;
                        }
                    }
                    if (hasLaunchPoint) break;
                }
                
                // 如果还是没找到，强制分配发射点truckIdx+1
                if (!hasLaunchPoint) {
                    for (int m = 0; m < Mmax; m++) {
                        int pos = truckIdx * Mmax + m;
                        if (pos < chromosome.launchPointAssignment.size() && 
                            chromosome.launchPointAssignment.get(pos) == 0) {
                            chromosome.launchPointAssignment.set(pos, truckIdx + 1);
                            break;
                        }
                    }
                }
                repaired = true;
            }
        }
        
        // ========== 修复逻辑（符合唯一归属信息来源原则）==========
        // 设计原则：第一段 customerBlocks 是唯一的归属信息来源
        // 解码时直接从 customerBlocks 确定每个客户属于哪个发射点
        // 第二段 launchPointAssignment 只决定卡车访问哪些发射点
        // 因此应该用 customerBlocks 来约束 launchPointAssignment，而不是反过来
        
        // 从customerBlocks获取实际被使用的发射点（唯一信息来源）
        Set<Integer> usedLPs = new HashSet<>(chromosome.customerBlocks);
        
        // 修复launchPointAssignment：确保只包含实际有客户的发射点
        for (int i = 0; i < chromosome.launchPointAssignment.size(); i++) {
            int lpId = chromosome.launchPointAssignment.get(i);
            if (lpId > 0 && !usedLPs.contains(lpId)) {
                // 发射点没有客户，随机替换为有客户的发射点
                List<Integer> usedList = new ArrayList<>(usedLPs);
                if (!usedList.isEmpty()) {
                    chromosome.launchPointAssignment.set(i, usedList.get(random.nextInt(usedList.size())));
                    repaired = true;
                }
            }
        }
        
        // 确保customerBlocks中所有值都在有效范围1~n内
        for (int i = 0; i < chromosome.customerBlocks.size(); i++) {
            int blockId = chromosome.customerBlocks.get(i);
            if (blockId < 1 || blockId > n) {
                // 如果超出范围，随机分配一个有效的发射点
                chromosome.customerBlocks.set(i, random.nextInt(n) + 1);
                repaired = true;
            }
        }
    }

    /**
     * BLX-α交叉（用于浮点数坐标）
     * 子代在 [min(parent1,parent2) - α*diff, max(parent1,parent2) + α*diff] 范围内产生
     * α=0.5 表示扩展范围50%，增加探索能力
     */
    private List<Double> blxAlphaCrossover(List<Double> parent1, List<Double> parent2) {
        List<Double> child = new ArrayList<>();
        double alpha = 0.5;  // 扩展系数

        for (int i = 0; i < parent1.size(); i++) {
            double x1 = parent1.get(i);
            double x2 = parent2.get(i);

            double min = Math.min(x1, x2);
            double max = Math.max(x1, x2);
            double diff = max - min;

            // 扩展范围
            double lower = min - alpha * diff;
            double upper = max + alpha * diff;

            // 随机产生子代基因
            double childGene = lower + random.nextDouble() * (upper - lower);
            childGene = Math.round(childGene * 1000.0) / 1000.0;
            child.add(childGene);
        }
        return child;
    }

    /**
       * 三段式染色体变异（兼容旧版本）
      */
      public void mutate(ChromosomeEncoder.Chromosome chromosome, double mutationRate) {
          mutateWithHistory(chromosome, mutationRate, 0, 100);
      }

    /**
     * 交换变异
     */
    private void swapMutate(List<Integer> genes) {
        int i = random.nextInt(genes.size());
        int j = random.nextInt(genes.size());
        Collections.swap(genes, i, j);
    }

    /**
      * 高斯变异（用于浮点数坐标）
      * @param values 坐标值列表
      * @param truckIndex 卡车索引（用于获取历史记录）
      * @param currentGen 当前代数（用于自适应范围）
      * @param maxGenerations 最大代数
      * @return 变异方向数组 [[dx1, dy1], [dx2, dy2], ...]
      */
      private List<double[]> gaussianMutate(List<Double> values, int truckIndex, 
                                            int currentGen, int maxGenerations) {
          Config.LaunchPointConfig lpConfig = model.getLaunchPointConfig();
          double baseRange = lpConfig.mutationRange;
          boolean adaptiveRange = lpConfig.adaptiveRange;
          
          // 自适应变异范围：随迭代进行逐渐减小
          double range = baseRange;
          if (adaptiveRange && maxGenerations > 0) {
              double progress = (double) currentGen / maxGenerations;
              range = baseRange * (1.0 - progress * 0.5); // 50%减小
              range = Math.max(range, baseRange * 0.3); // 最小30%
          }
          
          List<double[]> directions = new ArrayList<>();
          double minX = model.getMinX();
          double maxX = model.getMaxX();
          double minY = model.getMinY();
          double maxY = model.getMaxY();
          
          // 边界收紧10%，防止变异后超出边界
          double marginX = (maxX - minX) * 0.1;
          double marginY = (maxY - minY) * 0.1;
          minX += marginX;
          maxX -= marginX;
          minY += marginY;
          maxY -= marginY;
          
          // 变异前先确保所有坐标在边界内
          for (int i = 0; i < values.size(); i += 2) {
              double x = values.get(i);
              double y = values.get(i + 1);
              x = Math.max(minX, Math.min(maxX, x));
              y = Math.max(minY, Math.min(maxY, y));
              values.set(i, x);
              values.set(i + 1, y);
          }
          
          for (int i = 0; i < values.size(); i += 2) {
              double[] direction;
              
              // 随机探索新方向
              double angle = random.nextDouble() * 2 * Math.PI;
              direction = new double[]{Math.cos(angle) * range, Math.sin(angle) * range};
              
              // 应用变异到x坐标
              double newX = values.get(i) + direction[0];
              newX = Math.max(minX, Math.min(maxX, newX));
              values.set(i, Math.round(newX * 1000.0) / 1000.0);
              
              // 应用变异到y坐标
              double newY = values.get(i + 1) + direction[1];
              newY = Math.max(minY, Math.min(maxY, newY));
              values.set(i + 1, Math.round(newY * 1000.0) / 1000.0);
              
              directions.add(direction);
          }
          
          return directions;
      }

      /**
       * 变异染色体（三段式编码）
       * 暂时不调用历史方向引导
       */
      public List<double[]> mutateWithHistory(ChromosomeEncoder.Chromosome chromosome, 
                                              double mutationRate,
                                              int currentGen,
                                               int maxGenerations) {
          List<double[]> allDirections = new ArrayList<>();
          
          // 获取卡车数量和每辆卡车最大发射点数
          int K = model.getTruckCount();
          int Mmax = (int) Math.ceil((double) model.getLaunchPointCount() / K);
          int n = chromosome.launchPointCoords.size() / 2;
          
          // 第一段：客户分块变异 - 混合策略
          if (random.nextDouble() < mutationRate) {
              if (random.nextDouble() < 0.7) {
                  // 70%概率：交换变异（保持每个发射点有客户）
                  swapMutate(chromosome.customerBlocks);
              } else {
                  // 30%概率：随机重置为当前卡车负责的发射点
                  Set<Integer> truckLPs = new HashSet<>();
                  for (int lpId : chromosome.launchPointAssignment) {
                      if (lpId > 0) truckLPs.add(lpId);
                  }
                  List<Integer> validLPs = new ArrayList<>(truckLPs);
                  if (!validLPs.isEmpty()) {
                      int i = random.nextInt(chromosome.customerBlocks.size());
                      chromosome.customerBlocks.set(i, validLPs.get(random.nextInt(validLPs.size())));
                  }
              }
          }
          
           // ========== 第二段：发射点分配变异 - 卡车间发射点交换 ==========
           if (random.nextDouble() < mutationRate) {
               List<Integer> trucksWithLP = new ArrayList<>();
               for (int t = 0; t < K; t++) {
                   boolean hasLP = false;
                   for (int m = 0; m < Mmax; m++) {
                       int pos = t * Mmax + m;
                       if (pos < chromosome.launchPointAssignment.size() 
                           && chromosome.launchPointAssignment.get(pos) > 0) {
                           hasLP = true;
                           break;
                       }
                   }
                   if (hasLP) {
                       trucksWithLP.add(t);
                   }
               }
               
               if (trucksWithLP.size() >= 2) {
                   int idx1 = random.nextInt(trucksWithLP.size());
                   int idx2 = random.nextInt(trucksWithLP.size());
                   while (idx2 == idx1) {
                       idx2 = random.nextInt(trucksWithLP.size());
                   }
                   int k1 = trucksWithLP.get(idx1);
                   int k2 = trucksWithLP.get(idx2);
                   
                   List<Integer> positions1 = new ArrayList<>();
                   List<Integer> positions2 = new ArrayList<>();
                   for (int m = 0; m < Mmax; m++) {
                       int pos1 = k1 * Mmax + m;
                       int pos2 = k2 * Mmax + m;
                       if (pos1 < chromosome.launchPointAssignment.size() 
                           && chromosome.launchPointAssignment.get(pos1) > 0) {
                           positions1.add(pos1);
                       }
                       if (pos2 < chromosome.launchPointAssignment.size() 
                           && chromosome.launchPointAssignment.get(pos2) > 0) {
                           positions2.add(pos2);
                       }
                   }
                   
                   if (!positions1.isEmpty() && !positions2.isEmpty()) {
                       int pos1 = positions1.get(random.nextInt(positions1.size()));
                       int pos2 = positions2.get(random.nextInt(positions2.size()));
                       
                       int lp1 = chromosome.launchPointAssignment.get(pos1);
                       int lp2 = chromosome.launchPointAssignment.get(pos2);
                       
                       chromosome.launchPointAssignment.set(pos1, lp2);
                       chromosome.launchPointAssignment.set(pos2, lp1);
                   }
               }
           }
          
            // 第三段：发射点坐标变异 - 高斯变异
           if (random.nextDouble() < mutationRate) {
               gaussianMutate(chromosome.launchPointCoords, 0, currentGen, maxGenerations);
           }
          
          // 修复染色体：确保每辆卡车都有发射点
          repairChromosome(chromosome);
            
         return allDirections;
     }

    /**
     * 创建下一代种群
     * 自动记录成功变异方向到历史
     */
    public List<ChromosomeEncoder.Chromosome> createNextGeneration(
            List<ChromosomeEncoder.Chromosome> population,
            List<Double> fitness,
            Config config,
            FitnessFunction fitnessFunc) {

        int popSize = config.genetic.populationSize;
        int eliteCount = (int) (popSize * config.genetic.eliteRate);
        double successThreshold = config.launchPoint != null ? 
            config.launchPoint.successThreshold : 0.0001;

        List<ChromosomeEncoder.Chromosome> nextGen = new ArrayList<>();

        // 精英保留
        for (int i = 0; i < eliteCount; i++) {
            ChromosomeEncoder.Chromosome elite = population.get(i).copy();
            nextGen.add(elite);
        }

        // 生成剩余个体
        this.config = config;
        int childIndex = 0;
        int tournamentSize = config.genetic.tournamentSize;
        while (nextGen.size() < popSize) {
            // 锦标赛选择父代
            ChromosomeEncoder.Chromosome parent = tournamentSelect(population, fitness, tournamentSize);

            ChromosomeEncoder.Chromosome child;
            if (random.nextDouble() < config.genetic.crossoverRate) {
                // 锦标赛选择第二个父代
                ChromosomeEncoder.Chromosome parent2 = tournamentSelect(population, fitness, tournamentSize);
                child = crossover(parent, parent2);
            } else {
                child = parent.copy();
            }

            // 变异
            this.mutateWithHistory(child, config.genetic.mutationRate, nextGen.size(), popSize);

            // 评估子代适应度
            double childFitness = fitnessFunc.calculateFitness(child);

            nextGen.add(child);
            childIndex++;
        }

        return nextGen;
    }

    /**
     * 兼容旧版本（不记录历史）
     */
    public List<ChromosomeEncoder.Chromosome> createNextGeneration(
            List<ChromosomeEncoder.Chromosome> population,
            List<Double> fitness,
            Config config) {
        return createNextGeneration(population, fitness, config, null);
    }

}
