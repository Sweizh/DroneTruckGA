package ga;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class SolomonLoader {

    public static class SolomonData {
        public String name;
        public String scale;
        public String description;
        public Config config;

        @Override
        public String toString() {
            return name + " (" + scale + "客户) - " + description;
        }
    }

    private static List<SolomonData> cachedInstances = null;

    public static List<SolomonData> getAvailableInstances() {
        if (cachedInstances != null) {
            return new ArrayList<>(cachedInstances);
        }

        List<SolomonData> instances = new ArrayList<>();
        String basePath = getBasePath();

        String[][] scales = {
            {"solomon_25", "25"},
            {"solomon_50", "50"},
            {"solomon_100", "100"}
        };

        for (String[] scaleInfo : scales) {
            String dirName = scaleInfo[0];
            String scale = scaleInfo[1];
            File dir = new File(basePath, dirName);

            if (dir.exists() && dir.isDirectory()) {
                File[] files = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".txt"));
                if (files != null) {
                    Arrays.sort(files);
                    for (File file : files) {
                        try {
                            SolomonData data = loadFile(file.getAbsolutePath());
                            if (data != null) {
                                data.scale = scale;
                                instances.add(data);
                            }
                        } catch (Exception e) {
                            System.err.println("Failed to load: " + file.getName());
                        }
                    }
                }
            }
        }

        cachedInstances = new ArrayList<>(instances);
        return instances;
    }

    public static SolomonData load(String filePath) {
        return loadFile(filePath);
    }

    private static SolomonData loadFile(String filePath) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(filePath), StandardCharsets.UTF_8))) {

            SolomonData data = new SolomonData();
            data.config = new Config();
            data.config.problem = new Config.Problem();
            data.config.problem.depots = new ArrayList<>();
            data.config.problem.customers = new ArrayList<>();
            data.config.vehicles = new Config.Vehicles();
            data.config.vehicles.trucks = new Config.Trucks();
            data.config.vehicles.drones = new Config.Drones();

            List<String> customerLines = new ArrayList<>();
            int vehicleCount = 0;
            double vehicleCapacity = 0;

            String line;
            boolean inCustomerSection = false;

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                if (line.isEmpty()) continue;

                if (data.name == null && !line.startsWith("VEHICLE") && !line.startsWith("CUSTOMER")) {
                    data.name = line;
                    continue;
                }

                if (line.startsWith("VEHICLE")) {
                    inCustomerSection = false;
                    continue;
                }

                if (line.startsWith("CUSTOMER")) {
                    inCustomerSection = true;
                    continue;
                }

                if (inCustomerSection) {
                    if (line.contains("XCOORD") || line.contains("CUST NO")) {
                        continue;
                    }
                    customerLines.add(line);
                } else if (line.matches("\\s*\\d+\\s+\\d+.*")) {
                    String[] parts = line.trim().split("\\s+");
                    if (parts.length >= 2) {
                        try {
                            vehicleCount = Integer.parseInt(parts[0].trim());
                            vehicleCapacity = Double.parseDouble(parts[1].trim());
                        } catch (NumberFormatException e) {
                        }
                    }
                }
            }

            if (customerLines.isEmpty()) {
                return null;
            }

            data.config.vehicles.trucks.count = vehicleCount;
            data.config.vehicles.trucks.capacity = vehicleCapacity;

            Config.Depot depot = null;

            for (String custLine : customerLines) {
                String[] parts = custLine.trim().split("\\s+");
                if (parts.length < 7) continue;

                int id = Integer.parseInt(parts[0].trim());
                double x = Double.parseDouble(parts[1].trim());
                double y = Double.parseDouble(parts[2].trim());
                double demand = Double.parseDouble(parts[3].trim());
                double readyTime = Double.parseDouble(parts[4].trim());
                double dueDate = Double.parseDouble(parts[5].trim());
                double serviceTime = Double.parseDouble(parts[6].trim());

                if (id == 0) {
                    depot = new Config.Depot();
                    depot.id = 0;
                    depot.x = x;
                    depot.y = y;
                    data.config.problem.depots.add(depot);
                } else {
                    Config.Customer customer = new Config.Customer();
                    customer.id = id;
                    customer.x = x;
                    customer.y = y;
                    customer.demand = demand;
                    customer.depotId = depot != null ? depot.id : 0;
                    customer.timeWindow = new double[]{readyTime, dueDate};
                    customer.serviceTime = serviceTime / 60.0;
                    data.config.problem.customers.add(customer);
                }
            }

            data.description = generateDescription(data, vehicleCapacity);

            return data;

        } catch (Exception e) {
            System.err.println("Error loading Solomon file: " + filePath);
            e.printStackTrace();
            return null;
        }
    }

    private static String generateDescription(SolomonData data, double capacity) {
        StringBuilder sb = new StringBuilder();

        String type = "未知";
        if (data.name != null && data.name.length() >= 2) {
            char c = data.name.charAt(0);
            if (c == 'C') type = "聚类";
            else if (c == 'R') type = "随机";
            else if (c == 'R' && data.name.length() >= 2 && data.name.charAt(1) == 'C') type = "混合";
        }

        sb.append(type);
        sb.append(", 容量: ").append((int) capacity).append("kg");
        sb.append(", 车辆: ").append(data.config.vehicles.trucks.count);

        return sb.toString();
    }

    private static String getBasePath() {
        try {
            File jarFile = new File(
                SolomonLoader.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI()
            );
            String jarDir = jarFile.getParentFile().getAbsolutePath();
            File solomonDir = new File(jarDir, "solomon");
            if (solomonDir.exists()) {
                return solomonDir.getAbsolutePath();
            }
            File projectDir = jarFile.getParentFile().getParentFile();
            File solomonInProject = new File(projectDir, "solomon");
            if (solomonInProject.exists()) {
                return solomonInProject.getAbsolutePath();
            }
            return "solomon";
        } catch (Exception e) {
            return "solomon";
        }
    }

    public static String formatMinutes(int minutes) {
        if (minutes < 0) minutes = 0;
        int hours = minutes / 60;
        int mins = minutes % 60;
        if (hours > 0) {
            return hours + "小时" + (mins > 0 ? mins + "分" : "");
        } else {
            return mins + "分";
        }
    }

    public static String formatServiceTime(int seconds) {
        if (seconds < 0) seconds = 0;
        int mins = seconds / 60;
        int secs = seconds % 60;
        if (mins > 0) {
            return mins + "分" + (secs > 0 ? secs + "秒" : "");
        } else {
            return secs + "秒";
        }
    }

    public static int parseTime(String timeStr, boolean isServiceTime) {
        if (timeStr == null || timeStr.trim().isEmpty()) {
            return 0;
        }

        timeStr = timeStr.trim();

        try {
            if (timeStr.matches("\\d+")) {
                return Integer.parseInt(timeStr);
            }

            int totalMinutes = 0;
            int hours = 0, mins = 0, secs = 0;

            java.util.regex.Pattern hourPattern = java.util.regex.Pattern.compile("(\\d+)\\s*小时");
            java.util.regex.Pattern minPattern = java.util.regex.Pattern.compile("(\\d+)\\s*分");
            java.util.regex.Pattern secPattern = java.util.regex.Pattern.compile("(\\d+)\\s*秒");

            java.util.regex.Matcher m = hourPattern.matcher(timeStr);
            if (m.find()) {
                hours = Integer.parseInt(m.group(1));
            }
            m = minPattern.matcher(timeStr);
            if (m.find()) {
                mins = Integer.parseInt(m.group(1));
            }
            m = secPattern.matcher(timeStr);
            if (m.find()) {
                secs = Integer.parseInt(m.group(1));
            }

            if (isServiceTime) {
                return hours * 3600 + mins * 60 + secs;
            } else {
                return hours * 60 + mins;
            }
        } catch (Exception e) {
            return 0;
        }
    }

    public static void clearCache() {
        cachedInstances = null;
    }
}
