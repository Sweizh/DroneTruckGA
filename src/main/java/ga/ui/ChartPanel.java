package ga.ui;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

/**
 * 类名：ChartPanel
 * 功能：图表面板
 * 说明：使用JFreeChart绘制图表，包括收敛曲线和路线图
 */
public class ChartPanel extends JPanel {
    private JFreeChartWrapper convergenceChart;
    private JFreeChartWrapper routeMapChart;

    /**
     * 构造函数：创建图表面板
     */
    public ChartPanel() {
        setLayout(new GridLayout(2, 2, 10, 10));
        setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));
    }

    /**
     * 初始化收敛曲线图表
     * @param title 图表标题
     * @return JFreeChartWrapper对象
     */
    public JFreeChartWrapper initConvergenceChart(String title) {
        JFreeChartWrapper chart = new JFreeChartWrapper(title, "迭代次数", "成本");
        chart.setLineColor(java.awt.Color.BLUE);
        return chart;
    }

    /**
     * 初始化路线图图表
     * @param title 图表标题
     * @return JFreeChartWrapper对象
     */
    public JFreeChartWrapper initRouteMapChart(String title) {
        JFreeChartWrapper chart = new JFreeChartWrapper(title, "X", "Y");
        chart.setShowLines(true);
        chart.setShowShapes(true);
        return chart;
    }

    /**
     * 内部类：JFreeChart包装器
     * 用于简化JFreeChart的使用
     */
    public static class JFreeChartWrapper {
        private org.jfree.chart.JFreeChart chart;
        private org.jfree.chart.plot.XYPlot plot;
        private XYSeriesCollection dataset;
        private List<XYSeries> seriesList;
        private String xAxisLabel;
        private String yAxisLabel;
        private java.awt.Color lineColor = java.awt.Color.BLUE;
        private java.awt.Color pointColor = java.awt.Color.RED;
        private boolean showLines = true;
        private boolean showShapes = true;
        private int seriesCounter = 0;

        /**
         * 构造函数：创建图表包装器
         * @param title 图表标题
         * @param xAxisLabel X轴标签
         * @param yAxisLabel Y轴标签
         */
        public JFreeChartWrapper(String title, String xAxisLabel, String yAxisLabel) {
            this.xAxisLabel = xAxisLabel;
            this.yAxisLabel = yAxisLabel;
            this.seriesList = new ArrayList<>();

            dataset = new XYSeriesCollection();

            plot = new org.jfree.chart.plot.XYPlot(
                dataset,
                new org.jfree.chart.axis.NumberAxis(xAxisLabel),
                new org.jfree.chart.axis.NumberAxis(yAxisLabel),
                new org.jfree.chart.renderer.xy.XYLineAndShapeRenderer(showLines, showShapes)
            );

            chart = new org.jfree.chart.JFreeChart(title,
                org.jfree.chart.JFreeChart.DEFAULT_TITLE_FONT,
                plot,
                true);
        }

        /**
         * 添加新数据系列
         * @param seriesName 系列名称
         * @return 创建的系列
         */
        public XYSeries addSeries(String seriesName) {
            XYSeries series = new XYSeries(seriesName);
            seriesList.add(series);
            dataset.addSeries(series);
            
            // 设置新系列的颜色
            int index = seriesList.size() - 1;
            Color[] colors = getDefaultColors();
            Color color = colors[index % colors.length];
            
            if (plot.getRenderer() instanceof XYLineAndShapeRenderer) {
                XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRenderer();
                renderer.setSeriesPaint(index, color);
                renderer.setSeriesLinesVisible(index, true);
                renderer.setSeriesShapesVisible(index, true);
            }
            
            return series;
        }
        
        private Color[] getDefaultColors() {
            return new Color[]{
                Color.RED, Color.BLUE, Color.GREEN, Color.MAGENTA,
                Color.ORANGE, Color.CYAN, Color.PINK, Color.GRAY,
                Color.BLACK, Color.DARK_GRAY, Color.YELLOW
            };
        }

        /**
         * 设置线条颜色
         * @param color 颜色
         */
        public void setLineColor(java.awt.Color color) {
            this.lineColor = color;
            if (plot.getRenderer() instanceof XYLineAndShapeRenderer) {
                ((XYLineAndShapeRenderer) plot.getRenderer()).setSeriesPaint(0, color);
            }
        }

        /**
         * 设置数据点颜色
         * @param color 颜色
         */
        public void setPointColor(java.awt.Color color) {
            this.pointColor = color;
        }

        /**
         * 设置是否显示线条
         * @param show 是否显示
         */
        public void setShowLines(boolean show) {
            this.showLines = show;
            if (plot.getRenderer() instanceof XYLineAndShapeRenderer) {
                XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRenderer();
                for (int i = 0; i < seriesList.size(); i++) {
                    renderer.setSeriesLinesVisible(i, show);
                }
            }
        }

        /**
         * 设置是否显示形状
         * @param show 是否显示
         */
        public void setShowShapes(boolean show) {
            this.showShapes = show;
            if (plot.getRenderer() instanceof XYLineAndShapeRenderer) {
                XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRenderer();
                for (int i = 0; i < seriesList.size(); i++) {
                    renderer.setSeriesShapesVisible(i, show);
                }
            }
        }

        /**
         * 添加数据点到第一个系列
         * @param x X坐标
         * @param y Y坐标
         */
        public void addData(double x, double y) {
            if (!seriesList.isEmpty()) {
                seriesList.get(0).add(x, y);
            }
        }

        /**
         * 添加数据点到指定系列
         * @param seriesIndex 系列索引
         * @param x X坐标
         * @param y Y坐标
         */
        public void addDataToSeries(int seriesIndex, double x, double y) {
            if (seriesIndex >= 0 && seriesIndex < seriesList.size()) {
                seriesList.get(seriesIndex).add(x, y);
            }
        }

        /**
         * 获取JFreeChart对象
         * @return JFreeChart对象
         */
        public org.jfree.chart.JFreeChart getChart() {
            return chart;
        }

        /**
         * 获取图表面板
         * @return 图表面板
         */
        public JPanel getPanel() {
            ChartPanel2D panel = new ChartPanel2D(chart);
            return panel;
        }

        /**
         * 清除所有数据
         */
        public void clearData() {
            for (XYSeries series : seriesList) {
                series.clear();
            }
        }

        /**
         * 清除所有系列
         */
        public void clearAllSeries() {
            seriesList.clear();
            dataset.removeAllSeries();
        }
    }

    /**
     * 内部类：2D图表面板
     * 使用JFreeChart的原生ChartPanel
     */
    private static class ChartPanel2D extends JPanel {
        private JFreeChart chart;
        private org.jfree.chart.ChartPanel chartPanel;

        public ChartPanel2D(JFreeChart chart) {
            this.chart = chart;
            setLayout(new BorderLayout());
            chartPanel = new org.jfree.chart.ChartPanel(chart);
            chartPanel.setPreferredSize(new java.awt.Dimension(500, 300));
            add(chartPanel, BorderLayout.CENTER);
        }
    }
}
