package ga;

/**
 * 类名：CostCalculator
 * 功能：成本计算器
 * 说明：计算卡车和无人机的配送成本，包括行驶成本和各种惩罚项
 *      成本计算是适应度函数的核心组成部分
 */
public class CostCalculator {
    /** 问题模型引用，用于获取车辆参数 */
    private final ProblemModel model;

    /**
     * 构造函数：创建成本计算器
     * @param model 问题模型对象
     */
    public CostCalculator(ProblemModel model) {
        this.model = model;
    }

    /**
     * 计算卡车的行驶成本
     * 公式：成本 = 行驶距离 × 单位成本
     * @param distance 行驶距离（单位：km）
     * @return 行驶成本（单位：元）
     */
    public double calculateTruckCost(double distance) {
        return distance * model.getTruckCostPerKm();
    }

    /**
     * 计算无人机的飞行成本
     * 公式：成本 = 飞行距离 × 单位成本
     * @param distance 飞行距离（单位：km）
     * @return 飞行成本（单位：元）
     */
    public double calculateDroneCost(double distance) {
        return distance * model.getDroneCostPerKm();
    }

    /**
     * 计算行驶时间
     * 公式：时间 = 距离 / 速度
     * @param distance 行驶距离（单位：km）
     * @param isDrone 是否为无人机，true表示无人机，false表示卡车
     * @return 行驶时间（单位：min）
     */
    public double calculateTime(double distance, boolean isDrone) {
        // 根据车辆类型选择对应的速度
        double speed = isDrone ? model.getDroneSpeed() : model.getTruckSpeed();
        return distance / speed;
    }

    /**
     * 计算时间窗惩罚
     * 说明：如果车辆在时间窗之前到达，需要等待，惩罚较轻（×2）
     *       如果在时间窗之后到达，延误配送，惩罚较重（×5）
     * @param arrivalTime 到达时间（单位：min）
     * @param customer 客户对象，包含时间窗信息
     * @return 惩罚成本（单位：元），符合时间窗要求返回0
     */
    public double calculateTimeWindowPenalty(double arrivalTime, Customer customer) {
        // 提前到达：等待时间 × 时间窗提前到达惩罚系数
        if (arrivalTime < customer.getStartTimeWindow()) {
            return (customer.getStartTimeWindow() - arrivalTime) * model.getTimeWindowEarly();
        }
        // 延误到达：延误时间 × 时间窗延误惩罚系数
        else if (arrivalTime > customer.getEndTimeWindow()) {
            return (arrivalTime - customer.getEndTimeWindow()) * model.getTimeWindowLate();
        }
        // 符合时间窗要求，无惩罚
        return 0;
    }

    /**
     * 计算续航惩罚（无人机专用）
     * 说明：如果无人机单程飞行距离超过最大续航，需要返回充电
     *       惩罚系数为10
     * @param distance 单程飞行距离（单位：km）
     * @param isDrone 是否为无人机
     * @return 惩罚成本（单位：元），未超续航返回0
     */
    public double calculateRangePenalty(double distance, boolean isDrone) {
        // 只有无人机需要考虑续航限制
        if (isDrone && distance > model.getDroneRange()) {
            // 超出续航的部分 × 无人机续航超限惩罚系数
            return (distance - model.getDroneRange()) * model.getDroneRangePenalty();
        }
        return 0;
    }

    /**
     * 计算载重惩罚（无人机专用）
     * 说明：如果客户需求量超过无人机最大载重，无法配送
     *       惩罚系数为20
     * @param demand 货物需求量（单位：kg）
     * @param isDrone 是否为无人机配送
     * @return 惩罚成本（单位：元），未超载重返回0
     */
    public double calculatePayloadPenalty(double demand, boolean isDrone) {
        // 只有无人机需要考虑载重限制
        if (isDrone && demand > model.getDroneMaxPayload()) {
            // 超出载重的部分 × 无人机载重超限惩罚系数
            return (demand - model.getDroneMaxPayload()) * model.getDronePayloadPenalty();
        }
        return 0;
    }

    /**
     * 计算服务时间成本
     * 说明：到达客户地点后需要花费的装卸货时间
     *       将时间成本转换为货币成本（时间 × 1元/min）
     * @param serviceTime 服务时间（单位：min）
     * @return 服务时间成本（单位：元）
     */
    public double calculateServiceTimeCost(double serviceTime) {
        // 假设每分钟的时间成本为1元
        return serviceTime * 1.0;
    }

    /**
     * 计算充电成本（无人机专用）
     * 说明：无人机没电后返回仓库充电的时间和费用
     * @return 充电成本（单位：元）
     */
    public double calculateRechargeCost() {
        // 充电成本 = 充电时间 × 时间成本 + 固定充电费
        double timeCost = model.getDroneRechargeTime() * 1.0;
        double fixedCost = model.getDroneRechargeCost();
        return timeCost + fixedCost;
    }

    /**
     * 计算卡车固定成本
     * 说明：每辆卡车出车都有固定费用（司机工资、车辆损耗等）
     * @param truckCount 使用的卡车数量
     * @return 固定成本（单位：元）
     */
    public double calculateTruckFixedCost(int truckCount) {
        return truckCount * model.getTruckFixedCost();
    }

    /**
     * Calculate total cost
     * @param truckRoutes Truck routes
     * @param droneTasks Drone delivery tasks
     * @param launchPoints Launch point coordinates [x1, y1, x2, y2, ...]
     * @return Total cost (currency unit)
     */
    public double calculateTotalCost(java.util.List<java.util.List<Integer>> truckRoutes,
                                     java.util.List<java.util.List<Integer>> droneTasks,
                                     java.util.List<Double> launchPoints) {
        double totalCost = 0;

        double truckDistance = calculateTruckRouteDistance(truckRoutes, launchPoints);
        totalCost += calculateTruckCost(truckDistance);

        totalCost += calculateTruckFixedCost(model.getTruckCount());

        return totalCost;
    }

    /**
       * Calculate total time
       * Total time = max(Truck time for all trucks) + max(Drone time for all launch points)
       * 所有卡车和无人机同步行动，总时间取最大值
       */
    public double calculateTotalTime(java.util.List<java.util.List<Integer>> truckRoutes,
                                     java.util.List<java.util.List<Integer>> droneTasks,
                                     java.util.List<Integer> droneToLaunchPoint,
                                     java.util.List<Double> launchPoints) {
        double totalTime = 0;
        int truckCount = model.getTruckCount();
        int dronesPerTruck = model.getDronesPerTruck();
        int launchPointCount = launchPoints.size() / 2;

        // 1. 计算每辆卡车的行驶时间，取最大值（同步行动）
        double maxTruckTime = 0;
        for (int truckIdx = 0; truckIdx < truckCount; truckIdx++) {
            java.util.List<Integer> route = truckRoutes.get(truckIdx);
            double truckTravelTime = 0;
            
            for (int i = 0; i < route.size() - 1; i++) {
                int id1 = route.get(i);
                int id2 = route.get(i + 1);
                double[] p1 = getPointCoords(id1, launchPoints);
                double[] p2 = getPointCoords(id2, launchPoints);
                if (p1 != null && p2 != null) {
                    double distance = Math.sqrt(Math.pow(p1[0] - p2[0], 2) + Math.pow(p1[1] - p2[1], 2));
                    truckTravelTime += distance / model.getTruckSpeed() * 60;
                }
            }
            
            if (truckTravelTime > maxTruckTime) {
                maxTruckTime = truckTravelTime;
            }
        }

        // 2. 计算每个发射点的无人机飞行时间，取最大值（同步行动）
        double maxDroneTime = 0;
        int totalTasks = 0;
        
        for (int droneIdx = 0; droneIdx < droneTasks.size(); droneIdx++) {
            java.util.List<Integer> tasks = droneTasks.get(droneIdx);
            if (tasks.isEmpty()) continue;
            
            // 使用映射获取正确的发射点索引
            int lpIdx = droneIdx < droneToLaunchPoint.size() ? droneToLaunchPoint.get(droneIdx) : 0;
            if (lpIdx < 0 || lpIdx >= launchPointCount) lpIdx = 0;
            
            double launchX = launchPoints.get(lpIdx * 2);
            double launchY = launchPoints.get(lpIdx * 2 + 1);
            
            // 每架无人机的配送时间（往返 + 服务时间）
            double droneDeliveryTime = 0;
            
            for (int customerId : tasks) {
                Customer customer = model.getCustomerById(customerId);
                if (customer != null) {
                    double distance = Math.sqrt(Math.pow(customer.getX() - launchX, 2) +
                                               Math.pow(customer.getY() - launchY, 2));
                    double flightTime = distance / model.getDroneSpeed() * 60;
                    double serviceAndFlight = flightTime + customer.getServiceTime();
                    droneDeliveryTime += serviceAndFlight;
                    totalTasks++;
                }
            }
            
            // 取所有无人机的最大时间
            if (droneDeliveryTime > maxDroneTime) {
                maxDroneTime = droneDeliveryTime;
            }
        }

        // 3. 充电时间
        double rechargeTime = 0;
        if (totalTasks > 0) {
            rechargeTime = model.getDroneRechargeTime();
        }
        
        // 总时间 = 卡车时间 + 无人机时间 + 充电时间
        totalTime = maxTruckTime + maxDroneTime + rechargeTime;

        return totalTime;
    }

    /**
     * Calculate total truck distance
     */
    private double calculateTruckRouteDistance(java.util.List<java.util.List<Integer>> routes,
                                               java.util.List<Double> launchPoints) {
        double totalDistance = 0;

        for (java.util.List<Integer> route : routes) {
            for (int i = 0; i < route.size() - 1; i++) {
                int id1 = route.get(i);
                int id2 = route.get(i + 1);

                double[] p1 = getPointCoords(id1, launchPoints);
                double[] p2 = getPointCoords(id2, launchPoints);

                if (p1 != null && p2 != null) {
                    totalDistance += Math.sqrt(Math.pow(p1[0] - p2[0], 2) + Math.pow(p1[1] - p2[1], 2));
                }
            }
        }

        return totalDistance;
    }

    /**
     * 计算额外时间成本（转换为分钟，用于时间优化）
     * 早到: (开始时间 - 到达时间) × 2 分钟
     * 晚到: (到达时间 - 结束时间) × 50 分钟
     * 超续航: (往返距离 - 续航) / 速度 × 60 × 50 分钟
     * @return 额外时间（分钟）
     */
    public double calculateExtraTimeCost(
            java.util.List<java.util.List<Integer>> truckRoutes,
            java.util.List<java.util.List<Integer>> droneTasks,
            java.util.List<Integer> droneToLaunchPoint,
            java.util.List<Double> launchPoints) {
        double extraTime = 0;
        if (truckRoutes == null || droneTasks == null || launchPoints == null) {
            return 0;
        }

        int truckCount = model.getTruckCount();
        int dronesPerTruck = model.getDronesPerTruck();
        int launchPointCount = launchPoints.size() / 2;

        // 按发射点分组计算
        for (int droneIdx = 0; droneIdx < droneTasks.size(); droneIdx++) {
            java.util.List<Integer> tasks = droneTasks.get(droneIdx);
            if (tasks.isEmpty()) continue;

            // 使用映射获取正确的发射点索引
            int lpIdx = droneIdx < droneToLaunchPoint.size() ? droneToLaunchPoint.get(droneIdx) : 0;
            if (lpIdx < 0 || lpIdx >= launchPointCount) lpIdx = 0;

            double launchX = launchPoints.get(lpIdx * 2);
            double launchY = launchPoints.get(lpIdx * 2 + 1);

            // 计算对应的卡车索引
            int truckIdx = droneIdx / dronesPerTruck;
            if (truckIdx >= truckCount) truckIdx = truckCount - 1;

            // 计算卡车到达发射点的时间
            double truckArrivalTime = 0;
            java.util.List<Integer> truckRoute = truckRoutes.get(truckIdx);
            double truckDistToLaunch = 0;
            for (int i = 0; i < truckRoute.size() - 1; i++) {
                double[] p1 = getPointCoords(truckRoute.get(i), launchPoints);
                double[] p2 = getPointCoords(truckRoute.get(i + 1), launchPoints);
                if (p1 != null && p2 != null) {
                    double segDist = Math.sqrt(Math.pow(p1[0] - p2[0], 2) + Math.pow(p1[1] - p2[1], 2));
                    if (truckRoute.get(i + 1) < 0 && Math.abs(truckRoute.get(i + 1)) == truckIdx + 1) {
                        truckDistToLaunch = truckDistToLaunch + segDist;
                        break;
                    }
                    truckDistToLaunch += segDist;
                }
            }
            truckArrivalTime = truckDistToLaunch / model.getTruckSpeed() * 60;

            // 每架无人机的最大飞行时间
            double droneMaxFlightTime = 0;

            for (int customerId : tasks) {
                Customer customer = model.getCustomerById(customerId);
                if (customer == null) continue;

                // 距离计算（单程）
                double dist = Math.sqrt(Math.pow(customer.getX() - launchX, 2) +
                                        Math.pow(customer.getY() - launchY, 2));

                // 无人机飞行时间（单程）
                double droneFlightTime = dist / model.getDroneSpeed() * 60;

                if (droneFlightTime > droneMaxFlightTime) {
                    droneMaxFlightTime = droneFlightTime;
                }
            }

            // 计算该无人机客户的时间窗惩罚
            double arrivalTime = truckArrivalTime + droneMaxFlightTime;

            for (int customerId : tasks) {
                Customer customer = model.getCustomerById(customerId);
                if (customer == null) continue;

                // 早到：额外等待时间（设置上限30分钟）× 权重
                if (arrivalTime < customer.getStartTimeWindow()) {
                    double waitTime = customer.getStartTimeWindow() - arrivalTime;
                    extraTime += Math.min(waitTime, 30.0) * model.getTimeWindowEarly();
                }
                // 晚到：额外延误时间（设置上限60分钟）× 权重
                else if (arrivalTime > customer.getEndTimeWindow()) {
                    double delayTime = arrivalTime - customer.getEndTimeWindow();
                    extraTime += Math.min(delayTime, 60.0) * model.getTimeWindowLate();
                }
            }
        }

        // 卡车超续航惩罚（使用实际超限时间）× 权重
        double truckRange = model.getTruckRange();
        if (truckRange > 0) {
            for (int t = 0; t < truckRoutes.size(); t++) {
                java.util.List<Integer> route = truckRoutes.get(t);
                double routeDistance = 0;
                for (int i = 0; i < route.size() - 1; i++) {
                    double[] p1 = getPointCoords(route.get(i), launchPoints);
                    double[] p2 = getPointCoords(route.get(i + 1), launchPoints);
                    if (p1 != null && p2 != null) {
                        routeDistance += Math.sqrt(Math.pow(p1[0] - p2[0], 2) + Math.pow(p1[1] - p2[1], 2));
                    }
                }
                if (routeDistance > truckRange) {
                    double exceedDist = routeDistance - truckRange;
                    double exceedTime = exceedDist / model.getTruckSpeed() * 60;
                    extraTime += Math.min(exceedTime, 60.0) * model.getTruckRangeTimePenalty();
                }
            }
        }

        return extraTime;
    }

      /**
       * 计算发射点到所有客户的总距离（km）
       * 直接用于距离惩罚
       */
      public double calculateLaunchPointDistanceSum(java.util.List<java.util.List<Integer>> truckRoutes,
                                                    java.util.List<java.util.List<Integer>> droneTasks,
                                                    java.util.List<Integer> droneToLaunchPoint,
                                                    java.util.List<Double> launchPoints) {
          double totalDistance = 0;
          int launchPointCount = launchPoints.size() / 2;

          for (int droneIdx = 0; droneIdx < droneTasks.size(); droneIdx++) {
              java.util.List<Integer> tasks = droneTasks.get(droneIdx);
              if (tasks.isEmpty()) continue;

              // 使用映射获取正确的发射点索引
              int lpIdx = droneIdx < droneToLaunchPoint.size() ? droneToLaunchPoint.get(droneIdx) : 0;
              if (lpIdx < 0 || lpIdx >= launchPointCount) lpIdx = 0;

              double launchX = launchPoints.get(lpIdx * 2);
              double launchY = launchPoints.get(lpIdx * 2 + 1);

              for (int customerId : tasks) {
                  Customer customer = model.getCustomerById(customerId);
                  if (customer == null) continue;

                  double dist = Math.sqrt(Math.pow(customer.getX() - launchX, 2) +
                                         Math.pow(customer.getY() - launchY, 2));
                  totalDistance += dist;
              }
          }

          return totalDistance;
      }

    /**
     * 获取点的坐标
     * @param id 点ID（负数表示发射点）
     * @param launchPoints 发射点坐标列表
     */
    private double[] getPointCoords(int id, java.util.List<Double> launchPoints) {
        if (id < 0) {
            // 发射点（负数ID），从launchPoints获取
            int idx = (-id - 1) * 2;
            if (idx >= 0 && idx < launchPoints.size() - 1) {
                return new double[]{launchPoints.get(idx), launchPoints.get(idx + 1)};
            }
            return null;
        }
        Point depot = model.getDepotById(id);
        if (depot != null) {
            return new double[]{depot.getX(), depot.getY()};
        }
        Customer customer = model.getCustomerById(id);
        if (customer != null) {
            return new double[]{customer.getX(), customer.getY()};
        }
        return null;
    }
}
