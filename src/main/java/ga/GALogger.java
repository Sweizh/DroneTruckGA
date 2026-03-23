package ga;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.*;

/**
 * GA Logger - English, concise, detailed logging
 */
public class GALogger {
    private static Logger logger = Logger.getLogger("GA");
    private static boolean initialized = false;
    private static Level currentLevel = Level.INFO;
    private static String logFilePath = "";
    private static boolean logEnabled = false; // GUI开关控制详细程度
    private static boolean fileEnabled = false; // GUI开关控制是否生成文件

    /**
     * 启用/禁用详细日志输出
     * true = FINE级别（详细），false = INFO级别（简洁）
     */
    public static void setLogEnabled(boolean enabled) {
        logEnabled = enabled;
        if (enabled) {
            setLevel(Level.FINE);
        } else {
            setLevel(Level.INFO);
        }
    }

    /**
     * 检查日志是否启用
     */
    public static boolean isLogEnabled() {
        return logEnabled;
    }

    /**
     * 设置是否生成日志文件
     * 必须在 restart() 之前调用
     */
    public static void setFileEnabled(boolean enabled) {
        fileEnabled = enabled;
    }

    /**
     * 检查是否生成日志文件
     */
    public static boolean isFileEnabled() {
        return fileEnabled;
    }

    /**
     * Initialize logging system (only once)
     */
    public static void init() {
        if (initialized) return;
        restart();
    }

    /**
      * Restart logging - creates new log file for each GA run
      */
     public static void restart() {
         try {
             // Close existing handlers
             for (Handler h : logger.getHandlers()) {
                 h.close();
                 logger.removeHandler(h);
             }

             logger.setUseParentHandlers(false);
             logger.setLevel(Level.ALL);

             // File handler - only if fileEnabled is true
             if (fileEnabled) {
                 try {
                     String logDir = "logs";
                     File dir = new File(logDir);
                     if (!dir.exists()) dir.mkdirs();

                     SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
                     logFilePath = logDir + "/ga_" + sdf.format(new Date()) + ".log";

                     FileHandler fileHandler = new FileHandler(logFilePath, true);
                     fileHandler.setFormatter(new SimpleFormatter() {
                         private final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

                         @Override
                         public String format(LogRecord record) {
                             return String.format("%s [%s] %s%n",
                                 df.format(new Date(record.getMillis())),
                                 record.getLevel().getName().substring(0, 4),
                                 record.getMessage());
                         }
                     });
                     fileHandler.setLevel(Level.ALL);
                     logger.addHandler(fileHandler);

                 } catch (IOException e) {
                     // 日志文件创建失败，静默处理
                 }
             }


              initialized = true;

          } catch (Exception e) {
              // Logger初始化失败，静默处理
          }
     }

    /**
     * Get log file path
     */
    public static String getLogFilePath() {
        return logFilePath;
    }

    /**
     * Set log level
     */
    public static void setLevel(Level level) {
        currentLevel = level;
        logger.setLevel(level);
        for (Handler h : logger.getHandlers()) {
            h.setLevel(level);
        }
    }

    // === Basic logging methods ===

    public static void severe(String message) { 
        logger.severe(message); 
    }
    public static void warning(String message) { 
        logger.warning(message); 
    }
    public static void info(String message) { 
        logger.info(message); 
    }
    public static void fine(String message) { 
        logger.fine(message); 
    }
    public static void finer(String message) { 
        logger.finer(message); 
    }
    public static void finest(String message) { 
        logger.finest(message); 
    }

    // === GA-specific logging methods ===

    /**
     * Log population statistics
     * Format: [POP] Gen:100 | BestCost:123.45 | BestTime:67.89 | AvgCost:150.00 | AvgTime:80.00 | Fitness:0.85
     */
    public static void logPopulation(int gen, double bestCost, double bestTime,
                                    double avgCost, double avgTime, double fitness) {
        info(String.format("[POP] Gen:%d | BestCost:%.2f | BestTime:%.2f | AvgCost:%.2f | AvgTime:%.2f | Fitness:%.4f",
            gen, bestCost, bestTime, avgCost, avgTime, fitness));
    }

    /**
     * Log convergence progress
     * Format: [CONV] Gen:100 | Prev:123.45 | Curr:120.00 | Improve:3.45 (2.8%)
     */
    public static void logConvergence(int gen, double prevBest, double currBest,
                                      double improvement, double improvementRate) {
        info(String.format("[CONV] Gen:%d | Prev:%.4f | Curr:%.4f | Improve:%.4f (%.2f%%)",
            gen, prevBest, currBest, improvement, improvementRate * 100));
    }

    /**
     * Log chromosome details
     */
    public static void logChromosome(String label, Object chromosome) {
        finer(String.format("[CHROM-%s] %s", label, chromosome));
    }

    /**
     * Log cost breakdown
     * Format: [COST] Total:150.00 = Truck:50.00 + Drone:40.00 + Fixed:50.00 + Penalty:10.00
     */
    public static void logCost(double total, double truck, double drone, double fixed, double penalty) {
        finer(String.format("[COST] Total:%.2f = Truck:%.2f + Drone:%.2f + Fixed:%.2f + Penalty:%.2f",
            total, truck, drone, fixed, penalty));
    }

    /**
     * Log time breakdown
     * Format: [TIME] Total:80.00 = Truck:30.00 + Drone:40.00 + Service:10.00
     */
    public static void logTime(double total, double truck, double drone, double service) {
        finer(String.format("[TIME] Total:%.2f = Truck:%.2f + Drone:%.2f + Service:%.2f",
            total, truck, drone, service));
    }

    /**
     * Log GA operator execution
     */
    public static void logOperator(String op, double rate, boolean executed) {
        finer(String.format("[OP-%s] Rate:%.2f | %s", op, rate, executed ? "EXECUTED" : "SKIPPED"));
    }

    /**
     * Log final result
     */
    public static void logResult(double cost, double time, long elapsedMs) {
        info(String.format("[RESULT] BestCost:%.2f | BestTime:%.2f | Time:%dms", cost, time, elapsedMs));
    }

    /**
     * Log delivery routes
     */
    public static void logRoutes(java.util.List<java.util.List<Integer>> truckRoutes,
                                 java.util.List<java.util.List<Integer>> droneTasks) {
        info("[ROUTES] TruckRoutes: " + truckRoutes);
        info("[ROUTES] DroneTasks: " + droneTasks);
    }

    /**
     * Log initial population chromosomes
     * Format: 1 {customerBlocks}{launchPointAssignment}{launchPointCoords}
     */
    public static void logInitialPopulation(java.util.List<ChromosomeEncoder.Chromosome> population) {
        StringBuilder sb = new StringBuilder();
        sb.append("[INIT-POP] Initial population:\n");
        for (int i = 0; i < population.size(); i++) {
            ChromosomeEncoder.Chromosome chrom = population.get(i);
            sb.append(String.format("%d %s%s%s\n",
                i + 1,
                chrom.customerBlocks,
                chrom.launchPointAssignment,
                chrom.launchPointCoords));
        }
        info(sb.toString());
    }

    /**
     * Log best chromosome of each generation
     * Format: Gen:1 {customerBlocks}{launchPointAssignment}{launchPointCoords}
     */
    public static void logBestChromosome(int gen, ChromosomeEncoder.Chromosome chrom) {
        info(String.format("Gen:%d %s%s%s",
            gen,
            chrom.customerBlocks,
            chrom.launchPointAssignment,
            chrom.launchPointCoords));
    }

    /**
     * Close logger
     */
    public static void close() {
        for (Handler h : logger.getHandlers()) {
            h.close();
        }
    }
}
