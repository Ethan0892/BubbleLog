package net.bubblecraft.bubblelog.config;

import org.slf4j.Logger;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigManager {
    
    private final Path dataDirectory;
    private final Logger logger;
    private final Path configPath;
    
    private CommentedConfigurationNode config;
    
    // Default values
    private int monitoringInterval = 30; // seconds
    private String logFileName = "system-usage.log";
    private boolean enableCpuMonitoring = true;
    private boolean enableRamMonitoring = true;
    private boolean enableDiskMonitoring = true;
    private boolean enableNetworkMonitoring = true;
    private boolean enableJvmMonitoring = true;
    private boolean enableConnectionQualityMonitoring = true;
    private boolean logToConsole = false;
    private String dateFormat = "yyyy-MM-dd HH:mm:ss";
    private int maxLogFiles = 7; // Keep 7 days of logs
    
    // Performance alert settings
    private boolean enablePerformanceAlerts = true;
    private double cpuThreshold = 80.0; // Alert when CPU > 80%
    private double ramThreshold = 85.0; // Alert when RAM > 85%
    private double diskThreshold = 90.0; // Alert when Disk > 90%
    private boolean alertToConsole = true;
    private boolean alertToFile = true;
    private int alertCooldown = 300; // 5 minutes between same alerts
    private boolean enableDiscordWebhook = false;
    private String discordWebhookUrl = "";
    private boolean enableDiscordStatusReports = false;
    private int discordStatusReportInterval = 3600; // 1 hour in seconds
    private boolean enableSlackWebhook = false;
    private String slackWebhookUrl = "";
    
    public ConfigManager(Path dataDirectory, Logger logger) {
        this.dataDirectory = dataDirectory;
        this.logger = logger;
        this.configPath = dataDirectory.resolve("config.yml");
    }
    
    public void loadConfig() {
        try {
            // Create data directory if it doesn't exist
            if (!Files.exists(dataDirectory)) {
                Files.createDirectories(dataDirectory);
            }
            
            // Create config file if it doesn't exist
            if (!Files.exists(configPath)) {
                createDefaultConfig();
            }
            
            // Load configuration
            YamlConfigurationLoader loader = YamlConfigurationLoader.builder()
                .path(configPath)
                .build();
            
            config = loader.load();
            
            // Load values
            monitoringInterval = config.node("monitoring", "interval").getInt(30);
            logFileName = config.node("logging", "filename").getString("system-usage.log");
            enableCpuMonitoring = config.node("monitoring", "cpu", "enabled").getBoolean(true);
            enableRamMonitoring = config.node("monitoring", "ram", "enabled").getBoolean(true);
            enableDiskMonitoring = config.node("monitoring", "disk", "enabled").getBoolean(true);
            enableNetworkMonitoring = config.node("monitoring", "network", "enabled").getBoolean(true);
            enableJvmMonitoring = config.node("monitoring", "jvm", "enabled").getBoolean(true);
            enableConnectionQualityMonitoring = config.node("monitoring", "connection-quality", "enabled").getBoolean(true);
            logToConsole = config.node("logging", "console").getBoolean(false);
            dateFormat = config.node("logging", "date-format").getString("yyyy-MM-dd HH:mm:ss");
            maxLogFiles = config.node("logging", "max-files").getInt(7);
            
            // Load alert settings
            enablePerformanceAlerts = config.node("alerts", "enabled").getBoolean(true);
            cpuThreshold = config.node("alerts", "thresholds", "cpu").getDouble(80.0);
            ramThreshold = config.node("alerts", "thresholds", "ram").getDouble(85.0);
            diskThreshold = config.node("alerts", "thresholds", "disk").getDouble(90.0);
            alertToConsole = config.node("alerts", "console").getBoolean(true);
            alertToFile = config.node("alerts", "log-to-file").getBoolean(true);
            alertCooldown = config.node("alerts", "cooldown").getInt(300);
            enableDiscordWebhook = config.node("alerts", "discord", "enabled").getBoolean(false);
            discordWebhookUrl = config.node("alerts", "discord", "webhook-url").getString("");
            enableDiscordStatusReports = config.node("alerts", "discord", "status-reports", "enabled").getBoolean(false);
            discordStatusReportInterval = config.node("alerts", "discord", "status-reports", "interval").getInt(3600);
            enableSlackWebhook = config.node("alerts", "slack", "enabled").getBoolean(false);
            slackWebhookUrl = config.node("alerts", "slack", "webhook-url").getString("");
            
            logger.info("Configuration loaded successfully");
            
        } catch (Exception e) {
            logger.error("Failed to load configuration, using defaults", e);
        }
    }
    
    private void createDefaultConfig() {
        try {
            YamlConfigurationLoader loader = YamlConfigurationLoader.builder()
                .path(configPath)
                .build();
            
            CommentedConfigurationNode root = loader.createNode();
            
            // Monitoring settings
            root.node("monitoring", "interval").set(30)
                .comment("How often to log system usage (in seconds)");
            
            root.node("monitoring", "cpu", "enabled").set(true)
                .comment("Enable CPU usage monitoring");
            
            root.node("monitoring", "ram", "enabled").set(true)
                .comment("Enable RAM usage monitoring");
            
            root.node("monitoring", "disk", "enabled").set(true)
                .comment("Enable disk usage monitoring");
            
            root.node("monitoring", "network", "enabled").set(true)
                .comment("Enable network and player monitoring");
            
            root.node("monitoring", "jvm", "enabled").set(true)
                .comment("Enable JVM performance monitoring");
            
            root.node("monitoring", "connection-quality", "enabled").set(true)
                .comment("Enable connection quality monitoring (ping, packet loss)");
            
            // Logging settings
            root.node("logging", "filename").set("system-usage.log")
                .comment("Name of the log file");
            
            root.node("logging", "console").set(false)
                .comment("Also log to console/server log");
            
            root.node("logging", "date-format").set("yyyy-MM-dd HH:mm:ss")
                .comment("Date format for log entries");
            
            root.node("logging", "max-files").set(7)
                .comment("Maximum number of log files to keep (0 = unlimited)");
            
            // Alert settings
            root.node("alerts", "enabled").set(true)
                .comment("Enable performance alerts");
            
            root.node("alerts", "thresholds", "cpu").set(80.0)
                .comment("CPU usage threshold for alerts (percentage)");
            
            root.node("alerts", "thresholds", "ram").set(85.0)
                .comment("RAM usage threshold for alerts (percentage)");
            
            root.node("alerts", "thresholds", "disk").set(90.0)
                .comment("Disk usage threshold for alerts (percentage)");
            
            root.node("alerts", "console").set(true)
                .comment("Send alerts to server console");
            
            root.node("alerts", "log-to-file").set(true)
                .comment("Log alerts to file");
            
            root.node("alerts", "cooldown").set(300)
                .comment("Cooldown between same alert types (seconds)");
            
            root.node("alerts", "discord", "enabled").set(false)
                .comment("Enable Discord webhook alerts");
            
            root.node("alerts", "discord", "webhook-url").set("")
                .comment("Discord webhook URL for alerts");
            
            root.node("alerts", "discord", "status-reports", "enabled").set(false)
                .comment("Enable periodic Discord status reports");
            
            root.node("alerts", "discord", "status-reports", "interval").set(3600)
                .comment("Discord status report interval in seconds (3600 = 1 hour)");
            
            root.node("alerts", "slack", "enabled").set(false)
                .comment("Enable Slack webhook alerts");
            
            root.node("alerts", "slack", "webhook-url").set("")
                .comment("Slack webhook URL for alerts");
            
            loader.save(root);
            logger.info("Created default configuration file");
            
        } catch (Exception e) {
            logger.error("Failed to create default configuration", e);
        }
    }
    
    // Getters
    public int getMonitoringInterval() {
        return monitoringInterval;
    }
    
    public String getLogFileName() {
        return logFileName;
    }
    
    public boolean isCpuMonitoringEnabled() {
        return enableCpuMonitoring;
    }
    
    public boolean isRamMonitoringEnabled() {
        return enableRamMonitoring;
    }
    
    public boolean isDiskMonitoringEnabled() {
        return enableDiskMonitoring;
    }
    
    public boolean isNetworkMonitoringEnabled() {
        return enableNetworkMonitoring;
    }
    
    public boolean isJvmMonitoringEnabled() {
        return enableJvmMonitoring;
    }
    
    public boolean isConnectionQualityMonitoringEnabled() {
        return enableConnectionQualityMonitoring;
    }
    
    public boolean isConsoleLoggingEnabled() {
        return logToConsole;
    }
    
    public String getDateFormat() {
        return dateFormat;
    }
    
    public int getMaxLogFiles() {
        return maxLogFiles;
    }
    
    // Alert getters
    public boolean isPerformanceAlertsEnabled() {
        return enablePerformanceAlerts;
    }
    
    public double getCpuThreshold() {
        return cpuThreshold;
    }
    
    public double getRamThreshold() {
        return ramThreshold;
    }
    
    public double getDiskThreshold() {
        return diskThreshold;
    }
    
    public boolean isAlertToConsoleEnabled() {
        return alertToConsole;
    }
    
    public boolean isAlertToFileEnabled() {
        return alertToFile;
    }
    
    public int getAlertCooldown() {
        return alertCooldown;
    }
    
    public boolean isDiscordWebhookEnabled() {
        return enableDiscordWebhook;
    }
    
    public String getDiscordWebhookUrl() {
        return discordWebhookUrl;
    }
    
    public boolean isSlackWebhookEnabled() {
        return enableSlackWebhook;
    }
    
    public String getSlackWebhookUrl() {
        return slackWebhookUrl;
    }
    
    public boolean isDiscordStatusReportsEnabled() {
        return enableDiscordStatusReports;
    }
    
    public int getDiscordStatusReportInterval() {
        return discordStatusReportInterval;
    }
}
