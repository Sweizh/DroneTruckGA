package ga;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class GARunner {

    public static class GAResult {
        public ChromosomeEncoder.Chromosome bestChromosome;
        public double bestTime;
        public double bestFitness;
        public List<Double> timeCurve;
        public List<List<Integer>> truckRoutes;
        public List<List<Integer>> droneTasks;
        public long elapsedTime;
        public List<Double> allLaunchPoints; // 解码后的所有发射点
        public List<Integer> droneToLaunchPoint; // 无人机到发射点的映射

        public GAResult() {
            timeCurve = new ArrayList<>();
        }
    }

    public static class ProgressInfo {
        public final int generation;
        public final double bestFitness;
        public final double currentTime;
        public final List<Double> timeCurve;
        public final List<List<Integer>> truckRoutes;
        public final List<List<Integer>> droneTasks;
        public final List<Double> launchPoints;
        public final List<Integer> droneToLaunchPoint;

        public ProgressInfo(int generation, double bestFitness, double currentTime,
                           List<Double> timeCurve,
                           List<List<Integer>> truckRoutes, List<List<Integer>> droneTasks,
                           List<Double> launchPoints, List<Integer> droneToLaunchPoint) {
            this.generation = generation;
            this.bestFitness = bestFitness;
            this.currentTime = currentTime;
            this.timeCurve = timeCurve;
            this.truckRoutes = truckRoutes;
            this.droneTasks = droneTasks;
            this.launchPoints = launchPoints;
            this.droneToLaunchPoint = droneToLaunchPoint;
        }
    }

    public static class MultiRunResult {
        public List<GAResult> runs = new ArrayList<>();
        public GAResult bestOverall;
        public List<Double> avgTimeCurve = new ArrayList<>();
        public List<Double> bestTimeCurve = new ArrayList<>();
        public int totalRuns;
        public int completedRuns;
    }

    public static GAResult runGA(Config config) {
        return runGA(config, null, null);
    }

    public static GAResult runGA(Config config,
                                  Consumer<ProgressInfo> progressConsumer,
                                  java.util.function.BooleanSupplier shouldStop) {

        GALogger.restart();
        ChromosomeEncoder.resetCount();
        long startTime = System.currentTimeMillis();

        ProblemModel model = new ProblemModel(config);
        ChromosomeEncoder encoder = new ChromosomeEncoder(model);
        ChromosomeDecoder decoder = new ChromosomeDecoder(model);
        FitnessFunction fitnessFunc = new FitnessFunction(model);
        GAOperators operators = new GAOperators(model);

        List<ChromosomeEncoder.Chromosome> population = new ArrayList<>();
        
        // 种群初始化：1个基础染色体 + 90%扰动染色体 + 10%完全随机染色体
        int baseCount = 1;
        int populationSize = config.genetic.populationSize;
        int perturbedCount = (int)(populationSize * 0.9);
        int randomCount = populationSize - baseCount - perturbedCount;
        
        // 1. 基础染色体（高质量初始解）
        ChromosomeEncoder.Chromosome base = encoder.createBaseChromosome();
        population.add(base);
        
        // 2. 扰动染色体（基于基础染色体扰动）
        for (int i = 0; i < perturbedCount; i++) {
            ChromosomeEncoder.Chromosome chrom = encoder.applyPerturbation(base);
            population.add(chrom);
        }
        
        // 3. 完全随机染色体（保持多样性）
        for (int i = 0; i < randomCount; i++) {
            ChromosomeEncoder.Chromosome chrom = encoder.createFullyRandomChromosome();
            population.add(chrom);
        }

        GALogger.logInitialPopulation(population);

        List<Double> fitness = new ArrayList<>();
        for (ChromosomeEncoder.Chromosome chrom : population) {
            fitness.add(fitnessFunc.calculateFitness(chrom));
        }

        double bestFitness = 0;
        ChromosomeEncoder.Chromosome bestChromosome = null;
        GAResult result = new GAResult();
        List<Double> timeCurve = new ArrayList<>();

        for (int gen = 0; gen < config.genetic.maxGenerations; gen++) {
            
            if (shouldStop != null && shouldStop.getAsBoolean()) {
                break;
            }

            List<Integer> sortedIndices = new ArrayList<>();
            for (int i = 0; i < population.size(); i++) sortedIndices.add(i);
            Collections.sort(sortedIndices, (i, j) -> Double.compare(fitness.get(j), fitness.get(i)));

            List<ChromosomeEncoder.Chromosome> sortedPopulation = new ArrayList<>();
            for (int idx : sortedIndices) sortedPopulation.add(population.get(idx));
            population = sortedPopulation;

            if (fitness.get(0) > bestFitness) {
                bestFitness = fitness.get(0);
                bestChromosome = population.get(0).copy();
                GALogger.logBestChromosome(gen + 1, bestChromosome);
            }

            FitnessFunction.EvaluationResult currentResult = fitnessFunc.evaluate(population.get(0));
            timeCurve.add(currentResult.effectiveTime);

            if (progressConsumer != null) {
                ChromosomeDecoder.DecodingResult decodeResult = decoder.decode(population.get(0));
                List<List<Integer>> truckRoutes = decodeResult.truckRoutes;
                List<List<Integer>> droneTasks = decodeResult.droneTasks;
                List<Double> launchPoints = new java.util.ArrayList<>();
                for (double[] lp : decodeResult.allLaunchPoints) {
                    launchPoints.add(lp[0]);
                    launchPoints.add(lp[1]);
                }

                ProgressInfo info = new ProgressInfo(
                    gen + 1,
                    bestFitness,
                    currentResult.totalTime,
                    new ArrayList<>(timeCurve),
                    truckRoutes,
                    droneTasks,
                    launchPoints,
                    decodeResult.droneToLaunchPoint
                );
                progressConsumer.accept(info);
            }

            population = operators.createNextGeneration(population, fitness, config, fitnessFunc);

            // 重新计算新一代的适应度
            fitness.clear();
            for (ChromosomeEncoder.Chromosome chrom : population) {
                fitness.add(fitnessFunc.calculateFitness(chrom));
            }
        }

        result.elapsedTime = System.currentTimeMillis() - startTime;
        result.bestChromosome = bestChromosome;
        result.bestFitness = bestFitness;

        FitnessFunction.EvaluationResult finalResult = fitnessFunc.evaluate(bestChromosome);
        result.bestTime = finalResult.totalTime;
        result.timeCurve = timeCurve;
        
        ChromosomeDecoder.DecodingResult decodeResult = decoder.decode(bestChromosome);
        result.truckRoutes = decodeResult.truckRoutes;
        result.droneTasks = decodeResult.droneTasks;
        List<Double> allLaunchPoints = new java.util.ArrayList<>();
        for (double[] lp : decodeResult.allLaunchPoints) {
            allLaunchPoints.add(lp[0]);
            allLaunchPoints.add(lp[1]);
        }
        result.allLaunchPoints = allLaunchPoints;
        result.droneToLaunchPoint = decodeResult.droneToLaunchPoint;

        GALogger.close();

        return result;
    }

    public static MultiRunResult runGARepeats(Config config, int repeatCount,
                                              Consumer<ProgressInfo> progressConsumer,
                                              java.util.function.IntConsumer runProgressConsumer,
                                              java.util.function.BooleanSupplier shouldStop) {
        MultiRunResult multiResult = new MultiRunResult();
        multiResult.totalRuns = repeatCount;
        multiResult.completedRuns = 0;

        double bestOverallFitness = 0;
        GAResult bestOverallResult = null;

        for (int run = 0; run < repeatCount; run++) {
            if (shouldStop != null && shouldStop.getAsBoolean()) {
                break;
            }

            GAResult result = runGA(config, progressConsumer, shouldStop);

            multiResult.runs.add(result);
            multiResult.completedRuns++;

            if (result.bestFitness > bestOverallFitness) {
                bestOverallFitness = result.bestFitness;
                bestOverallResult = result;
            }

            if (runProgressConsumer != null) {
                runProgressConsumer.accept(run + 1);
            }
        }

        multiResult.bestOverall = bestOverallResult;

        int maxGens = 0;
        for (GAResult r : multiResult.runs) {
            if (r.timeCurve.size() > maxGens) maxGens = r.timeCurve.size();
        }

        for (int g = 0; g < maxGens; g++) {
            double avgTime = 0, bestTime = Double.MAX_VALUE;
            int count = 0;

            for (GAResult r : multiResult.runs) {
                if (g < r.timeCurve.size()) {
                    avgTime += r.timeCurve.get(g);
                    if (r.timeCurve.get(g) < bestTime) bestTime = r.timeCurve.get(g);
                    count++;
                }
            }

            if (count > 0) {
                multiResult.avgTimeCurve.add(avgTime / count);
                multiResult.bestTimeCurve.add(bestTime);
            }
        }

        return multiResult;
    }
}
