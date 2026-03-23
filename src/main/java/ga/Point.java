package ga;

/**
 * 类名：Point
 * 功能：表示配送网络中的位置点
 * 说明：用于表示客户位置、仓库位置、无人机起飞/降落点
 */
public class Point {
    /** 位置点的唯一标识符 */
    private final int id;
    /** X坐标（单位：km） */
    private final double x;
    /** Y坐标（单位：km） */
    private final double y;
    /** 是否为仓库点，true表示是仓库，false表示是客户点 */
    private final boolean isDepot;

    /**
     * 构造函数：创建普通客户点
     * @param id 客户点的唯一标识
     * @param x X坐标
     * @param y Y坐标
     */
    public Point(int id, double x, double y) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.isDepot = false;  // 默认不是仓库
    }

    /**
     * 构造函数：创建任意位置点
     * @param id 位置点的唯一标识
     * @param x X坐标
     * @param y Y坐标
     * @param isDepot 是否为仓库
     */
    public Point(int id, double x, double y, boolean isDepot) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.isDepot = isDepot;
    }

    /**
     * 获取位置点ID
     * @return 位置点ID
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
     * 判断是否为仓库点
     * @return true表示是仓库，false表示是客户点
     */
    public boolean isDepot() { return isDepot; }

    /**
     * 计算到另一个点的欧几里得距离
     * 使用勾股定理计算两点之间的直线距离
     * @param other 目标位置点
     * @return 两点之间的距离（单位：km）
     */
    public double distanceTo(Point other) {
        // 欧几里得距离公式：√((x1-x2)² + (y1-y2)²)
        return Math.sqrt(Math.pow(this.x - other.getX(), 2)
                       + Math.pow(this.y - other.getY(), 2));
    }

    /**
     * 返回位置点的字符串描述
     * @return 格式：P[id](x,y)[仓] 或 P[id](x,y)
     */
    @Override
    public String toString() {
        return String.format("P%d(%.1f,%.1f)%s", id, x, y, isDepot ? "[仓]" : "");
    }

    /**
     * 判断两个位置点是否相等（根据ID判断）
     * @param o 另一个对象
     * @return ID相等返回true，否则返回false
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Point point = (Point) o;
        return id == point.id;
    }

    /**
     * 获取位置点的哈希码（基于ID）
     * @return 哈希码
     */
    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}
