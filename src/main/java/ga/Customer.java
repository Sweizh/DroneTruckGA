package ga;

/**
 * 类名：Customer
 * 功能：表示配送网络中的客户点
 * 说明：包含客户的位置坐标、需求量、时间窗要求、所属仓库、服务时间
 *      用于构建VRP（车辆路径问题）中的客户节点
 */
public class Customer {
    /** 客户点的唯一标识符 */
    private final int id;
    /** X坐标（单位：km） */
    private final double x;
    /** Y坐标（单位：km） */
    private final double y;
    /** 需求量（单位：kg），表示该客户需要的货物重量 */
    private final double demand;
    /** 所属仓库ID，指定该客户由哪个仓库负责配送 */
    private final int depotId;
    /** 时间窗开始时间（单位：min），配送车辆必须在此时间之后到达 */
    private final double startTimeWindow;
    /** 时间窗结束时间（单位：min），配送车辆必须在此时间之前到达 */
    private final double endTimeWindow;
    /** 服务时间（单位：min），到达客户地点后需要花费的装卸货时间 */
    private final double serviceTime;

    /**
     * 构造函数：创建客户点（支持多仓库和服务时间）
     * @param id 客户ID（唯一标识）
     * @param x X坐标（km）
     * @param y Y坐标（km）
     * @param demand 需求量（kg）
     * @param depotId 所属仓库ID
     * @param startTW 时间窗开始时间（min）
     * @param endTW 时间窗结束时间（min）
     * @param serviceTime 服务时间（min）
     */
    public Customer(int id, double x, double y, double demand, int depotId,
                   double startTW, double endTW, double serviceTime) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.demand = demand;
        this.depotId = depotId;
        this.startTimeWindow = startTW;
        this.endTimeWindow = endTW;
        this.serviceTime = serviceTime;
    }

    /**
     * 获取客户ID
     * @return 客户ID
     */
    public int getId() { return id; }

    /**
     * 获取X坐标
     * @return X坐标（km）
     */
    public double getX() { return x; }

    /**
     * 获取Y坐标
     * @return Y坐标（km）
     */
    public double getY() { return y; }

    /**
     * 获取需求量
     * @return 需求量（kg）
     */
    public double getDemand() { return demand; }

    /**
     * 获取所属仓库ID
     * @return 仓库ID
     */
    public int getDepotId() { return depotId; }

    /**
     * 获取时间窗开始时间
     * @return 时间窗开始时间（min）
     */
    public double getStartTimeWindow() { return startTimeWindow; }

    /**
     * 获取时间窗结束时间
     * @return 时间窗结束时间（min）
     */
    public double getEndTimeWindow() { return endTimeWindow; }

    /**
     * 获取服务时间
     * @return 服务时间（min）
     */
    public double getServiceTime() { return serviceTime; }

    /**
     * 将客户点转换为Point对象
     * 用于距离计算等场景
     * @return Point对象
     */
    public Point toPoint() {
        return new Point(id, x, y);
    }

    /**
     * 返回客户点的字符串描述
     * @return 格式：客户[id](x,y) 需求:demand 仓库:[depotId] 时间窗:[start,end] 服务时间:serviceTime
     */
    @Override
    public String toString() {
        return String.format("客户%d(%.1f,%.1f) 需求:%.1f 仓库:%d 时间窗:[%.0f,%.0f] 服务:%.0fmin",
                id, x, y, demand, depotId, startTimeWindow, endTimeWindow, serviceTime);
    }
}
