package ga.ui;

import ga.Config;
import ga.GARunner;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Point2D;
import java.util.*;
import java.util.List;

public class MapPreviewPanel extends JPanel {
    private Config config;
    private GARunner.GAResult result;
    private GARunner.ProgressInfo realtimeProgress; // 实时进度数据

    private double scale = 1.0;
    private double offsetX = 0;
    private double offsetY = 0;

    private int dragStartX, dragStartY;
    private int selectedCustomerIndex = -1;
    private int selectedDepotIndex = -1;
    private boolean isPanning = false;

    private String currentMode = "仓库管理";

    private static final Color DEPOT_COLOR = new Color(244, 67, 54);
    private static final Color CUSTOMER_COLOR = new Color(33, 150, 243);
    private static final Color LAUNCH_COLOR = new Color(76, 175, 80);
    private static final Color BACKGROUND_COLOR = new Color(250, 250, 250);
    private static final Color GRID_COLOR = new Color(220, 220, 220);

    private ConfigChangeListener changeListener;
    private boolean showRoutes = false;

    public interface ConfigChangeListener {
        void onCustomerPositionChanged(int customerId, double x, double y);
        void onDepotPositionChanged(int depotId, double x, double y);
    }

    public void setConfigChangeListener(ConfigChangeListener listener) {
        this.changeListener = listener;
    }

    public MapPreviewPanel() {
        setBorder(BorderFactory.createTitledBorder("滚轮缩放地理视图 (0.5x~20x) | 右键拖拽平移"));
        setBackground(BACKGROUND_COLOR);

        addMouseWheelListener(e -> {
            int notches = e.getWheelRotation();
            double oldScale = scale;

            if (notches < 0) {
                scale *= 1.1;
            } else {
                scale *= 0.9;
            }
            scale = Math.max(0.5, Math.min(20.0, scale));

            // 以鼠标为中心缩放
            double mouseX = e.getX();
            double mouseY = e.getY();
            double centerX = getWidth() / 2.0 + offsetX;
            double centerY = getHeight() / 2.0 + offsetY;

            // 计算鼠标位置对应的世界坐标
            double worldX = (mouseX - centerX) / oldScale + 100;
            double worldY = (centerY - mouseY) / oldScale + 100;

            // 调整偏移量，使缩放后鼠标位置对应的世界坐标不变
            offsetX += (mouseX - (centerX + (worldX - 100) * scale - (worldX - 100) * oldScale)) - (mouseX - centerX);
            offsetY += (mouseY - (centerY - (worldY - 100) * scale + (worldY - 100) * oldScale)) - (mouseY - centerY);

            repaint();
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON3) {
                    isPanning = true;
                    dragStartX = e.getX();
                    dragStartY = e.getY();
                } else if (e.getButton() == MouseEvent.BUTTON1) {
                    Point2D worldPos = toWorldPoint(e.getX(), e.getY());
                    if (currentMode != null && currentMode.contains("客户")) {
                        selectedCustomerIndex = findCustomerAt(worldPos);
                        selectedDepotIndex = -1;
                    } else if (currentMode != null && currentMode.contains("仓库")) {
                        selectedDepotIndex = findDepotAt(worldPos);
                        selectedCustomerIndex = -1;
                    }
                    repaint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON3) {
                    isPanning = false;
                }
            }
        });

        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (isPanning) {
                    offsetX += e.getX() - dragStartX;
                    offsetY += e.getY() - dragStartY;
                    dragStartX = e.getX();
                    dragStartY = e.getY();
                    repaint();
                } else if (selectedCustomerIndex >= 0 && currentMode != null && currentMode.contains("客户")) {
                    Point2D worldPos = toWorldPoint(e.getX(), e.getY());
                    if (config != null && config.problem != null &&
                        config.problem.customers != null &&
                        selectedCustomerIndex < config.problem.customers.size()) {
                        Config.Customer cust = config.problem.customers.get(selectedCustomerIndex);
                        double newX = Math.round(worldPos.getX() * 100) / 100.0;
                        double newY = Math.round(worldPos.getY() * 100) / 100.0;
                        cust.x = newX;
                        cust.y = newY;
                        repaint();
                        if (changeListener != null) {
                            changeListener.onCustomerPositionChanged(cust.id, newX, newY);
                        }
                    }
                } else if (selectedDepotIndex >= 0 && currentMode != null && currentMode.contains("仓库")) {
                    Point2D worldPos = toWorldPoint(e.getX(), e.getY());
                    if (config != null && config.problem != null &&
                        config.problem.depots != null &&
                        selectedDepotIndex < config.problem.depots.size()) {
                        Config.Depot depot = config.problem.depots.get(selectedDepotIndex);
                        double newX = Math.round(worldPos.getX() * 100) / 100.0;
                        double newY = Math.round(worldPos.getY() * 100) / 100.0;
                        depot.x = newX;
                        depot.y = newY;
                        repaint();
                        if (changeListener != null) {
                            changeListener.onDepotPositionChanged(depot.id, newX, newY);
                        }
                    }
                }
            }
        });
    }

    public void setConfig(Config config) {
        this.config = config;
        repaint();
    }

    public void setResult(GARunner.GAResult result) {
        this.result = result;
        this.realtimeProgress = null;
        autoFit();
        repaint();
    }

    /**
     * 设置实时进度数据（用于GA运行时实时更新路线图）
     */
    public void setRealtimeProgress(GARunner.ProgressInfo progress) {
        this.realtimeProgress = progress;
        autoFit();
        repaint();
    }

    /**
       * 清除实时数据，停止实时模式
       */
    public void clearRealtime() {
        this.realtimeProgress = null;
        repaint();
    }

    public boolean isRealtimeMode() {
        return realtimeProgress != null;
    }

    /**
     * 自动缩放以适应所有元素到面板大小
     */
    public void autoFit() {
        if (getWidth() <= 0 || getHeight() <= 0) return;

        double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE;
        double minY = Double.MAX_VALUE, maxY = -Double.MAX_VALUE;

        boolean hasPoints = false;

        if (config != null && config.problem != null) {
            if (config.problem.depots != null) {
                for (Config.Depot depot : config.problem.depots) {
                    minX = Math.min(minX, depot.x);
                    maxX = Math.max(maxX, depot.x);
                    minY = Math.min(minY, depot.y);
                    maxY = Math.max(maxY, depot.y);
                    hasPoints = true;
                }
            }
            if (config.problem.customers != null) {
                for (Config.Customer cust : config.problem.customers) {
                    minX = Math.min(minX, cust.x);
                    maxX = Math.max(maxX, cust.x);
                    minY = Math.min(minY, cust.y);
                    maxY = Math.max(maxY, cust.y);
                    hasPoints = true;
                }
            }
        }

        if (realtimeProgress != null && realtimeProgress.launchPoints != null) {
            List<Double> lp = realtimeProgress.launchPoints;
            for (int i = 0; i < lp.size() - 1; i += 2) {
                minX = Math.min(minX, lp.get(i));
                maxX = Math.max(maxX, lp.get(i));
                minY = Math.min(minY, lp.get(i + 1));
                maxY = Math.max(maxY, lp.get(i + 1));
                hasPoints = true;
            }
        }

        if (result != null) {
            List<Double> lp = result.allLaunchPoints;
            if (lp == null && result.bestChromosome != null) {
                lp = result.bestChromosome.launchPointCoords;
            }
            if (lp != null) {
                for (int i = 0; i < lp.size() - 1; i += 2) {
                    minX = Math.min(minX, lp.get(i));
                    maxX = Math.max(maxX, lp.get(i));
                    minY = Math.min(minY, lp.get(i + 1));
                    maxY = Math.max(maxY, lp.get(i + 1));
                    hasPoints = true;
                }
            }
        }

        if (!hasPoints) {
            scale = 1.0;
            offsetX = 0;
            offsetY = 0;
            return;
        }

        double padding = 40;
        double contentWidth = maxX - minX;
        double contentHeight = maxY - minY;

        if (contentWidth <= 0) contentWidth = 100;
        if (contentHeight <= 0) contentHeight = 100;

        double panelWidth = getWidth() - padding * 2;
        double panelHeight = getHeight() - padding * 2;

        scale = Math.min(panelWidth / contentWidth, panelHeight / contentHeight);
        scale = Math.max(0.5, Math.min(20.0, scale));

        double centerX = (minX + maxX) / 2;
        double centerY = (minY + maxY) / 2;

        offsetX = -(centerX - 100) * scale;
        offsetY = (centerY - 100) * scale;

        repaint();
    }

    public void setMode(String mode) {
        this.currentMode = mode != null ? mode : "仓库管理";
        selectedCustomerIndex = -1;
        selectedDepotIndex = -1;
        repaint();
    }

    public void setShowRoutes(boolean show) {
        this.showRoutes = show;
        autoFit();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();
        double centerX = width / 2.0 + offsetX;
        double centerY = height / 2.0 + offsetY;

        drawGrid(g2d, width, height, centerX, centerY);
        drawElements(g2d, centerX, centerY);

        g2d.dispose();
    }

    protected void drawGrid(Graphics2D g2d, int width, int height, double centerX, double centerY) {
        g2d.setColor(GRID_COLOR);
        g2d.setStroke(new BasicStroke(1));

        double gridSize = 20 * scale;
        for (double x = centerX % gridSize; x < width; x += gridSize) {
            g2d.drawLine((int) x, 0, (int) x, height);
        }
        for (double y = centerY % gridSize; y < height; y += gridSize) {
            g2d.drawLine(0, (int) y, width, (int) y);
        }
    }

    protected void drawElements(Graphics2D g2d, double centerX, double centerY) {
        if (config == null) return;

        GARunner.GAResult routeData = showRoutes ? result : null;

        // 实时模式下使用realtimeProgress的数据
        if (realtimeProgress != null) {
            drawRealtimeRoutes(g2d, centerX, centerY);
        } else {
            if (routeData != null) {
                drawTruckRoutes(g2d, centerX, centerY, routeData);
                drawDroneRoutes(g2d, centerX, centerY, routeData);
            }
        }

        if (config.problem != null && config.problem.depots != null) {
            for (int i = 0; i < config.problem.depots.size(); i++) {
                Config.Depot depot = config.problem.depots.get(i);
                Point2D screen = toScreenPoint(depot.x, depot.y, centerX, centerY);
                drawDepot(g2d, screen.getX(), screen.getY(), i);
            }
        }

        if (config.problem != null && config.problem.customers != null) {
            for (Config.Customer cust : config.problem.customers) {
                Point2D screen = toScreenPoint(cust.x, cust.y, centerX, centerY);
                drawCustomer(g2d, screen.getX(), screen.getY(), cust.id);
            }
        }

        // 实时模式下绘制发射点
        if (realtimeProgress != null && realtimeProgress.launchPoints != null) {
            List<Double> launchPoints = realtimeProgress.launchPoints;
            for (int t = 0; t < launchPoints.size() / 2; t++) {
                int idx = t * 2;
                if (idx + 1 < launchPoints.size()) {
                    Point2D screen = toScreenPoint(launchPoints.get(idx), launchPoints.get(idx + 1), centerX, centerY);
                    drawLaunchPoint(g2d, screen.getX(), screen.getY(), t + 1);
                }
            }
        } else if (routeData != null) {
            List<Double> launchPoints = routeData.allLaunchPoints;
            if (launchPoints == null && routeData.bestChromosome != null) {
                launchPoints = routeData.bestChromosome.launchPointCoords;
            }
            if (launchPoints != null) {
                for (int t = 0; t < launchPoints.size() / 2; t++) {
                    int idx = t * 2;
                    if (idx + 1 < launchPoints.size()) {
                        Point2D screen = toScreenPoint(launchPoints.get(idx), launchPoints.get(idx + 1), centerX, centerY);
                        drawLaunchPoint(g2d, screen.getX(), screen.getY(), t + 1);
                    }
                }
            }
        }
    }

    private void drawDepot(Graphics2D g2d, double x, double y, int index) {
        int size = 10;
        g2d.setColor(DEPOT_COLOR);
        g2d.setStroke(new BasicStroke(2));
        g2d.drawRect((int) x - size, (int) y - size, size * 2, size * 2);
        g2d.setColor(Color.WHITE);
        g2d.fillRect((int) x - size, (int) y - size, size * 2, size * 2);
        g2d.setColor(DEPOT_COLOR);
        g2d.setFont(new Font("微软雅黑", Font.BOLD, 11));
        FontMetrics fm = g2d.getFontMetrics();
        String label = String.valueOf(index);
        g2d.drawString(label, (int) x - fm.stringWidth(label) / 2, (int) y + 4);
    }

    private void drawCustomer(Graphics2D g2d, double x, double y, int id) {
        int radius = 10;
        g2d.setColor(CUSTOMER_COLOR);
        g2d.setStroke(new BasicStroke(2));
        g2d.drawOval((int) x - radius, (int) y - radius, radius * 2, radius * 2);
        g2d.setColor(new Color(33, 150, 243, 100));
        g2d.fillOval((int) x - radius, (int) y - radius, radius * 2, radius * 2);
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("微软雅黑", Font.BOLD, 11));
        FontMetrics fm = g2d.getFontMetrics();
        String label = String.valueOf(id);
        g2d.drawString(label, (int) x - fm.stringWidth(label) / 2, (int) y + 4);
    }

    private void drawLaunchPoint(Graphics2D g2d, double x, double y, int truckId) {
        int size = 10;
        g2d.setColor(LAUNCH_COLOR);
        g2d.setStroke(new BasicStroke(2));
        int[] xPoints = {(int) x, (int) x - size, (int) x + size};
        int[] yPoints = {(int) y - size, (int) y + size / 2, (int) y + size / 2};
        g2d.fillPolygon(xPoints, yPoints, 3);
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("微软雅黑", Font.BOLD, 10));
        FontMetrics fm = g2d.getFontMetrics();
        String label = String.valueOf(truckId);
        g2d.drawString(label, (int) x - fm.stringWidth(label) / 2, (int) y + 4);
    }

    private void drawArrowLine(Graphics2D g2d, double x1, double y1, double x2, double y2) {
        g2d.drawLine((int) x1, (int) y1, (int) x2, (int) y2);
        
        double cx = (x1 + x2) / 2.0;
        double cy = (y1 + y2) / 2.0;
        
        double dx = x2 - x1;
        double dy = y2 - y1;
        double angle = Math.atan2(dy, dx);
        
        int arrowLen = 12;
        double arrowAngle = Math.toRadians(25);
        
        double arrowTipX = cx;
        double arrowTipY = cy;
        double arrowBase1X = cx - arrowLen * Math.cos(angle - arrowAngle);
        double arrowBase1Y = cy - arrowLen * Math.sin(angle - arrowAngle);
        double arrowBase2X = cx - arrowLen * Math.cos(angle + arrowAngle);
        double arrowBase2Y = cy - arrowLen * Math.sin(angle + arrowAngle);
        
        Color oldColor = g2d.getColor();
        Stroke oldStroke = g2d.getStroke();
        
        g2d.setColor(new Color(33, 150, 243));
        g2d.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.drawLine((int) arrowTipX, (int) arrowTipY, (int) arrowBase1X, (int) arrowBase1Y);
        g2d.drawLine((int) arrowTipX, (int) arrowTipY, (int) arrowBase2X, (int) arrowBase2Y);
        
        g2d.setStroke(oldStroke);
        g2d.setColor(oldColor);
    }

    private void drawTruckRoutes(Graphics2D g2d, double centerX, double centerY, GARunner.GAResult routeData) {
        if (routeData == null || routeData.truckRoutes == null) return;
        g2d.setColor(new Color(33, 150, 243));
        g2d.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, new float[]{3, 3}, 0));
        for (List<Integer> route : routeData.truckRoutes) {
            if (route.size() < 2) continue;
            boolean hasArrows = route.size() >= 4;
            for (int i = 0; i < route.size() - 1; i++) {
                Point2D p1 = getPointCoords(route.get(i), centerX, centerY, routeData);
                Point2D p2 = getPointCoords(route.get(i + 1), centerX, centerY, routeData);
                if (p1 != null && p2 != null) {
                    if (hasArrows) {
                        drawArrowLine(g2d, p1.getX(), p1.getY(), p2.getX(), p2.getY());
                    } else {
                        g2d.drawLine((int) p1.getX(), (int) p1.getY(), (int) p2.getX(), (int) p2.getY());
                    }
                }
            }
        }
    }

    private void drawDroneRoutes(Graphics2D g2d, double centerX, double centerY, GARunner.GAResult routeData) {
        if (routeData == null || routeData.droneTasks == null) return;
        g2d.setColor(new Color(255, 152, 0));
        g2d.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, new float[]{3, 3}, 0));
        
        List<Double> launchPoints = routeData.allLaunchPoints;
        if (launchPoints == null && routeData.bestChromosome != null) {
            launchPoints = routeData.bestChromosome.launchPointCoords;
        }
        if (launchPoints == null) return;
        
        List<Integer> droneToLaunchPoint = routeData.droneToLaunchPoint;
        
        for (int d = 0; d < routeData.droneTasks.size(); d++) {
            List<Integer> tasks = routeData.droneTasks.get(d);
            if (tasks.isEmpty()) continue;
            
            // 获取该无人机对应的发射点索引
            int lpIdx = -1;
            if (droneToLaunchPoint != null && d < droneToLaunchPoint.size()) {
                lpIdx = droneToLaunchPoint.get(d);
            }
            
            // 如果没有有效的映射，跳过
            if (lpIdx < 0 || lpIdx >= launchPoints.size() / 2) continue;
            
            double lpX = launchPoints.get(lpIdx * 2);
            double lpY = launchPoints.get(lpIdx * 2 + 1);
            Point2D launch = toScreenPoint(lpX, lpY, centerX, centerY);
            
            for (int custId : tasks) {
                Point2D cust = getPointCoords(custId, centerX, centerY, routeData);
                if (cust != null) {
                    // 绘制往返路线：发射点 → 客户 → 发射点
                    g2d.drawLine((int) launch.getX(), (int) launch.getY(), (int) cust.getX(), (int) cust.getY());
                    g2d.drawLine((int) cust.getX(), (int) cust.getY(), (int) launch.getX(), (int) launch.getY());
                    // 在客户处画一个小圆点标记
                    int markerRadius = 4;
                    g2d.setStroke(new BasicStroke(1));
                    g2d.setColor(new Color(255, 152, 0, 150));
                    g2d.fillOval((int) cust.getX() - markerRadius, (int) cust.getY() - markerRadius, markerRadius * 2, markerRadius * 2);
                    g2d.setColor(new Color(255, 152, 0));
                    g2d.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, new float[]{3, 3}, 0));
                }
            }
        }
    }

    /**
     * 绘制实时路线图（使用ProgressInfo数据）
     */
    private void drawRealtimeRoutes(Graphics2D g2d, double centerX, double centerY) {
        if (realtimeProgress == null) return;

        g2d.setColor(new Color(33, 150, 243));
        g2d.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, new float[]{3, 3}, 0));

        // 绘制卡车路线
        if (realtimeProgress.truckRoutes != null) {
            for (List<Integer> route : realtimeProgress.truckRoutes) {
                if (route.size() < 2) continue;
                boolean hasArrows = route.size() >= 4;
                for (int i = 0; i < route.size() - 1; i++) {
                    Point2D p1 = getRealtimePointCoords(route.get(i), centerX, centerY);
                    Point2D p2 = getRealtimePointCoords(route.get(i + 1), centerX, centerY);
                    if (p1 != null && p2 != null) {
                        if (hasArrows) {
                            drawArrowLine(g2d, p1.getX(), p1.getY(), p2.getX(), p2.getY());
                        } else {
                            g2d.drawLine((int) p1.getX(), (int) p1.getY(), (int) p2.getX(), (int) p2.getY());
                        }
                    }
                }
            }
        }

        // 绘制无人机路线
        g2d.setColor(new Color(255, 152, 0));
        g2d.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, new float[]{3, 3}, 0));

        if (realtimeProgress.droneTasks != null && realtimeProgress.launchPoints != null) {
            List<Integer> droneToLaunchPoint = realtimeProgress.droneToLaunchPoint;
            
            for (int d = 0; d < realtimeProgress.droneTasks.size(); d++) {
                List<Integer> tasks = realtimeProgress.droneTasks.get(d);
                if (tasks.isEmpty()) continue;

                // 获取该无人机对应的发射点索引
                int lpIdx = -1;
                if (droneToLaunchPoint != null && d < droneToLaunchPoint.size()) {
                    lpIdx = droneToLaunchPoint.get(d);
                }
                
                // 如果没有有效的映射，跳过
                if (lpIdx < 0 || lpIdx >= realtimeProgress.launchPoints.size() / 2) continue;
                
                double lpX = realtimeProgress.launchPoints.get(lpIdx * 2);
                double lpY = realtimeProgress.launchPoints.get(lpIdx * 2 + 1);
                Point2D launch = toScreenPoint(lpX, lpY, centerX, centerY);

                for (int custId : tasks) {
                    Point2D cust = getRealtimeCustomerCoords(custId, centerX, centerY);
                    if (cust != null) {
                        // 往返路线
                        g2d.drawLine((int) launch.getX(), (int) launch.getY(), (int) cust.getX(), (int) cust.getY());
                        g2d.drawLine((int) cust.getX(), (int) cust.getY(), (int) launch.getX(), (int) launch.getY());
                        // 客户标记
                        int markerRadius = 4;
                        g2d.setStroke(new BasicStroke(1));
                        g2d.setColor(new Color(255, 152, 0, 150));
                        g2d.fillOval((int) cust.getX() - markerRadius, (int) cust.getY() - markerRadius, markerRadius * 2, markerRadius * 2);
                        g2d.setColor(new Color(255, 152, 0));
                        g2d.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, new float[]{3, 3}, 0));
                    }
                }
            }
        }
    }

    /**
     * 获取实时路线图中的点坐标
     */
    private Point2D getRealtimePointCoords(int id, double centerX, double centerY) {
        // id == 0: 仓库
        if (id == 0) {
            if (config.problem != null && config.problem.depots != null && !config.problem.depots.isEmpty()) {
                return toScreenPoint(config.problem.depots.get(0).x, config.problem.depots.get(0).y, centerX, centerY);
            }
        }
        // id < 0: 发射点
        else if (id < 0 && realtimeProgress != null && realtimeProgress.launchPoints != null) {
            int launchIdx = -id - 1;
            List<Double> launchPoints = realtimeProgress.launchPoints;
            if (launchIdx >= 0 && launchIdx * 2 + 1 < launchPoints.size()) {
                return toScreenPoint(launchPoints.get(launchIdx * 2), launchPoints.get(launchIdx * 2 + 1), centerX, centerY);
            }
        }
        return null;
    }

    /**
     * 获取实时路线图中的客户坐标
     */
    private Point2D getRealtimeCustomerCoords(int custId, double centerX, double centerY) {
        if (config.problem != null && config.problem.customers != null) {
            for (Config.Customer cust : config.problem.customers) {
                if (cust.id == custId) {
                    return toScreenPoint(cust.x, cust.y, centerX, centerY);
                }
            }
        }
        return null;
    }

    private Point2D getPointCoords(int id, double centerX, double centerY, GARunner.GAResult routeData) {
        // id == 0: 仓库
        if (id == 0) {
            if (config.problem != null && config.problem.depots != null && !config.problem.depots.isEmpty()) {
                return toScreenPoint(config.problem.depots.get(0).x, config.problem.depots.get(0).y, centerX, centerY);
            }
        }
        // id < 0: 发射点
        else if (id < 0 && routeData != null) {
            // 发射点 ID = -1, -2, -3, ... -n
            // 索引 = -id - 1
            int launchIdx = -id - 1;
            List<Double> launchPoints = routeData.allLaunchPoints;
            if (launchPoints == null && routeData.bestChromosome != null) {
                launchPoints = routeData.bestChromosome.launchPointCoords;
            }
            if (launchPoints != null && launchIdx >= 0 && launchIdx * 2 + 1 < launchPoints.size()) {
                return toScreenPoint(launchPoints.get(launchIdx * 2), launchPoints.get(launchIdx * 2 + 1), centerX, centerY);
            }
        }
        // id > 0: 客户
        else if (config.problem != null && config.problem.customers != null) {
            for (Config.Customer cust : config.problem.customers) {
                if (cust.id == id) {
                    return toScreenPoint(cust.x, cust.y, centerX, centerY);
                }
            }
        }
        return null;
    }

    private Point2D toScreenPoint(double worldX, double worldY, double centerX, double centerY) {
        double screenX = centerX + (worldX - 100) * scale;
        double screenY = centerY - (worldY - 100) * scale;
        return new Point2D.Double(screenX, screenY);
    }

    private Point2D toWorldPoint(int screenX, int screenY) {
        double centerX = getWidth() / 2.0 + offsetX;
        double centerY = getHeight() / 2.0 + offsetY;
        double worldX = (screenX - centerX) / scale + 100;
        double worldY = (centerY - screenY) / scale + 100;
        return new Point2D.Double(worldX, worldY);
    }

    private int findCustomerAt(Point2D worldPos) {
        if (config == null || config.problem == null || config.problem.customers == null) return -1;
        double clickRadius = 15 / scale;
        for (int i = 0; i < config.problem.customers.size(); i++) {
            Config.Customer cust = config.problem.customers.get(i);
            double dx = cust.x - worldPos.getX();
            double dy = cust.y - worldPos.getY();
            if (Math.sqrt(dx * dx + dy * dy) < clickRadius) {
                return i;
            }
        }
        return -1;
    }

    private int findDepotAt(Point2D worldPos) {
        if (config == null || config.problem == null || config.problem.depots == null) return -1;
        double clickRadius = 20 / scale;
        for (int i = 0; i < config.problem.depots.size(); i++) {
            Config.Depot depot = config.problem.depots.get(i);
            double dx = depot.x - worldPos.getX();
            double dy = depot.y - worldPos.getY();
            if (Math.sqrt(dx * dx + dy * dy) < clickRadius) {
                return i;
            }
        }
        return -1;
    }
}
