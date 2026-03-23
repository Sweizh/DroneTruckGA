package ga;

import java.util.List;

public class FitnessFunction {
    private final ProblemModel model;
    private final CostCalculator costCalculator;
    private final ChromosomeDecoder decoder;

    public static class EvaluationResult {
        public double totalTime;
        public double effectiveTime;

        public EvaluationResult(double totalTime, double effectiveTime) {
            this.totalTime = totalTime;
            this.effectiveTime = effectiveTime;
        }
    }

    public FitnessFunction(ProblemModel model) {
        this.model = model;
        this.costCalculator = new CostCalculator(model);
        this.decoder = new ChromosomeDecoder(model);
    }

    public EvaluationResult evaluate(ChromosomeEncoder.Chromosome chromosome) {
        ChromosomeDecoder.DecodingResult result = decoder.decode(chromosome);
        
        List<List<Integer>> truckRoutes = result.truckRoutes;
        List<List<Integer>> droneTasks = result.droneTasks;
        
        List<Double> allLaunchPoints = new java.util.ArrayList<>();
        for (double[] lp : result.allLaunchPoints) {
            allLaunchPoints.add(lp[0]);
            allLaunchPoints.add(lp[1]);
        }

        double totalTime = costCalculator.calculateTotalTime(
                truckRoutes, droneTasks, result.droneToLaunchPoint, allLaunchPoints);

        double extraTime = costCalculator.calculateExtraTimeCost(
                truckRoutes, droneTasks, result.droneToLaunchPoint, allLaunchPoints);

        double totalLaunchDistance = costCalculator.calculateLaunchPointDistanceSum(
                truckRoutes, droneTasks, result.droneToLaunchPoint, allLaunchPoints);
        double distancePenaltyTime = totalLaunchDistance * model.getDistancePenalty();

        int servedCustomers = 0;
        for (List<Integer> tasks : droneTasks) {
            servedCustomers += tasks.size();
        }
        int unservedCustomers = model.getCustomerCount() - servedCustomers;
        double unservedPenalty = unservedCustomers * model.getMaxTime();

        double penaltyWeight = model.isPenaltyEnabled() ? 1.0 : 0.0;
        double effectiveTime = totalTime + (extraTime + distancePenaltyTime + unservedPenalty) * penaltyWeight;

        return new EvaluationResult(totalTime, effectiveTime);
    }

    public double calculateFitness(ChromosomeEncoder.Chromosome chromosome) {
        EvaluationResult result = evaluate(chromosome);
        return 1.0 / (1.0 + result.effectiveTime);
    }
}
