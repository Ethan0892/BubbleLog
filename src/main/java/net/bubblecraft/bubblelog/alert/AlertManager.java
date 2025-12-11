package net.bubblecraft.bubblelog.alert;

import net.bubblecraft.bubblelog.config.ConfigManager;
import net.bubblecraft.bubblelog.monitor.SystemMonitor;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class AlertManager {
    
    private final Path dataDirectory;
    private final Logger logger;
    private final ConfigManager config;
    private final HttpClient httpClient;
    private final DateTimeFormatter dateFormatter;
    
    // Track last alert times to implement cooldown
    private final Map<AlertType, Long> lastAlertTimes = new HashMap<>();
    
    public enum AlertType {
        CPU_HIGH("High CPU Usage"),
        RAM_HIGH("High RAM Usage"),
        DISK_HIGH("High Disk Usage"),
        SYSTEM_CRITICAL("Critical System State");
        
        private final String displayName;
        
        AlertType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    public AlertManager(Path dataDirectory, Logger logger, ConfigManager config) {
        this.dataDirectory = dataDirectory;
        this.logger = logger;
        this.config = config;
        this.httpClient = HttpClient.newHttpClient();
        this.dateFormatter = DateTimeFormatter.ofPattern(config.getDateFormat());
    }
    
    public void checkAndSendAlerts(SystemMonitor.MemoryUsage memUsage, double cpuUsage, 
                                 java.util.List<SystemMonitor.DiskUsage> diskUsages) {
        
        try {
            if (!config.isPerformanceAlertsEnabled()) {
                return;
            }
            
            // Validate input parameters
            if (memUsage == null) {
                logger.debug("MemoryUsage is null, skipping memory alerts");
                memUsage = new SystemMonitor.MemoryUsage(0, 0, 0, 0.0);
            }
            
            if (diskUsages == null) {
                logger.debug("DiskUsages is null, using empty list");
                diskUsages = java.util.Collections.emptyList();
            }
            
            // Validate CPU usage value
            if (Double.isNaN(cpuUsage) || Double.isInfinite(cpuUsage) || cpuUsage < 0) {
                logger.debug("Invalid CPU usage value: {}, skipping CPU alerts", cpuUsage);
                cpuUsage = 0.0;
            }
        
            // Check CPU alerts
            try {
                if (config.isCpuMonitoringEnabled() && cpuUsage * 100 > config.getCpuThreshold()) {
                    sendAlert(AlertType.CPU_HIGH,
                        "CPU usage is %.2f%% (threshold: %.1f%%)".formatted(
                            cpuUsage * 100, config.getCpuThreshold()));
                }
            } catch (Exception e) {
                logger.debug("Error checking CPU alerts", e);
            }
            
            // Check RAM alerts
            try {
                if (config.isRamMonitoringEnabled() && memUsage.getUsagePercent() > config.getRamThreshold()) {
                    sendAlert(AlertType.RAM_HIGH,
                        "RAM usage is %.2f%% (threshold: %.1f%%) - %s/%s".formatted(
                            memUsage.getUsagePercent(), config.getRamThreshold(),
                            formatBytes(memUsage.getUsed()), formatBytes(memUsage.getTotal())));
                }
            } catch (Exception e) {
                logger.debug("Error checking RAM alerts", e);
            }
            
            // Check disk alerts
            try {
                if (config.isDiskMonitoringEnabled()) {
                    for (SystemMonitor.DiskUsage disk : diskUsages) {
                        try {
                            if (disk != null && disk.getUsagePercent() > config.getDiskThreshold()) {
                                sendAlert(AlertType.DISK_HIGH,
                                    "Disk %s usage is %.2f%% (threshold: %.1f%%) - %s/%s".formatted(
                                        disk.getName(), disk.getUsagePercent(), config.getDiskThreshold(),
                                        formatBytes(disk.getUsed()), formatBytes(disk.getTotal())));
                            }
                        } catch (Exception e) {
                            logger.debug("Error checking individual disk alert", e);
                        }
                    }
                }
            } catch (Exception e) {
                logger.debug("Error checking disk alerts", e);
            }
            
            // Check for critical system state (multiple thresholds exceeded)
            try {
                int criticalCount = 0;
                if (cpuUsage * 100 > config.getCpuThreshold()) criticalCount++;
                if (memUsage.getUsagePercent() > config.getRamThreshold()) criticalCount++;
                
                long criticalDisks = diskUsages.stream()
                    .filter(disk -> disk != null)
                    .mapToLong(disk -> {
                        try {
                            return disk.getUsagePercent() > config.getDiskThreshold() ? 1 : 0;
                        } catch (Exception e) {
                            logger.debug("Error checking disk in critical state", e);
                            return 0;
                        }
                    })
                    .sum();
                
                if (criticalCount >= 2 || (criticalCount >= 1 && criticalDisks > 0)) {
                    sendAlert(AlertType.SYSTEM_CRITICAL,
                        "Multiple system resources are under stress! CPU: %.2f%%, RAM: %.2f%%, Disks with issues: %d".formatted(
                            cpuUsage * 100, memUsage.getUsagePercent(), criticalDisks));
                }
            } catch (Exception e) {
                logger.debug("Error checking critical system state", e);
            }
            
        } catch (Exception e) {
            logger.warn("Critical error in alert checking - alerts will continue", e);
        }
    }
    
    private void sendAlert(AlertType alertType, String message) {
        // Check cooldown
        long currentTime = System.currentTimeMillis();
        Long lastAlertTime = lastAlertTimes.get(alertType);
        
        if (lastAlertTime != null && 
            (currentTime - lastAlertTime) < config.getAlertCooldown() * 1000L) {
            return; // Still in cooldown
        }
        
        lastAlertTimes.put(alertType, currentTime);
        
        String timestamp = LocalDateTime.now().format(dateFormatter);
        String fullMessage = "[%s] ALERT - %s: %s".formatted(
            timestamp, alertType.getDisplayName(), message);
        
        // Log to console
        if (config.isAlertToConsoleEnabled()) {
            logger.warn("🚨 PERFORMANCE ALERT: {}", message);
        }
        
        // Log to file
        if (config.isAlertToFileEnabled()) {
            try {
                writeAlertToFile(fullMessage);
            } catch (IOException e) {
                logger.error("Failed to write alert to file", e);
            }
        }
        
        // Send webhook notifications asynchronously
        if (config.isDiscordWebhookEnabled() && !config.getDiscordWebhookUrl().isEmpty()) {
            sendDiscordWebhook(alertType, message).exceptionally(throwable -> {
                logger.debug("Failed to send Discord webhook: {}", throwable.getMessage());
                return null;
            });
        }
        
        if (config.isSlackWebhookEnabled() && !config.getSlackWebhookUrl().isEmpty()) {
            sendSlackWebhook(alertType, message).exceptionally(throwable -> {
                logger.debug("Failed to send Slack webhook: {}", throwable.getMessage());
                return null;
            });
        }
    }
    
    private void writeAlertToFile(String alertMessage) throws IOException {
        Path alertFile = dataDirectory.resolve("logs").resolve("alerts.log");
        
        // Create parent directories if they don't exist
        if (!Files.exists(alertFile.getParent())) {
            Files.createDirectories(alertFile.getParent());
        }
        
        Files.write(alertFile, (alertMessage + System.lineSeparator()).getBytes(),
            StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }
    
    private CompletableFuture<Void> sendDiscordWebhook(AlertType alertType, String message) {
        return CompletableFuture.runAsync(() -> {
            try {
                String emoji = getAlertEmoji(alertType);
                String color = getAlertColor(alertType);
                String timestamp = Instant.now().toString();
                
                // Create a rich embed with server information
                String jsonPayload = """
                    {
                        "content": null,
                        "embeds": [{
                            "title": "%s %s",
                            "description": "%s",
                            "color": %s,
                            "timestamp": "%s",
                            "thumbnail": {
                                "url": "https://i.imgur.com/FTpCNyQ.png"
                            },
                            "fields": [
                                {
                                    "name": "Alert Type",
                                    "value": "%s",
                                    "inline": true
                                },
                                {
                                    "name": "Severity",
                                    "value": "%s",
                                    "inline": true
                                },
                                {
                                    "name": "Server",
                                    "value": "Velocity Proxy",
                                    "inline": true
                                },
                                {
                                    "name": "Timestamp",
                                    "value": "<t:%d:F>",
                                    "inline": false
                                }
                            ],
                            "footer": {
                                "text": "BubbleLog System Monitor • Automatic Alert",
                                "icon_url": "https://i.imgur.com/rNNH9lq.png"
                            }
                        }],
                        "attachments": []
                    }""".formatted(
                    emoji, alertType.getDisplayName(),
                    escapeJson(message),
                    color,
                    timestamp,
                    alertType.getDisplayName(),
                    getSeverityLevel(alertType),
                    System.currentTimeMillis() / 1000
                );
                
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(config.getDiscordWebhookUrl()))
                    .header("Content-Type", "application/json")
                    .header("User-Agent", "BubbleLog/1.0.0 (System Monitor)")
                    .timeout(java.time.Duration.ofSeconds(10))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();
                
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                if (response.statusCode() >= 400) {
                    logger.warn("Discord webhook returned error status: {} - {}", response.statusCode(), response.body());
                } else {
                    logger.debug("Discord webhook sent successfully for alert: {}", alertType.getDisplayName());
                }
                
            } catch (java.net.SocketTimeoutException e) {
                logger.warn("Discord webhook timed out - network may be slow");
                throw new RuntimeException("Discord webhook timeout", e);
            } catch (java.io.IOException e) {
                logger.warn("Discord webhook network error: {}", e.getMessage());
                throw new RuntimeException("Discord webhook network error", e);
            } catch (Exception e) {
                logger.warn("Discord webhook failed: {}", e.getMessage());
                throw new RuntimeException("Discord webhook failed", e);
            }
        });
    }
    
    private CompletableFuture<Void> sendSlackWebhook(AlertType alertType, String message) {
        return CompletableFuture.runAsync(() -> {
            try {
                String emoji = getAlertEmoji(alertType);
                
                String jsonPayload = """
                    {
                        "text": "%s *%s*",
                        "attachments": [{
                            "color": "%s",
                            "fields": [{
                                "title": "Details",
                                "value": "%s",
                                "short": false
                            }],
                            "footer": "BubbleLog System Monitor",
                            "ts": %d
                        }]
                    }""".formatted(emoji, alertType.getDisplayName(),
                    getSlackColor(alertType), message, System.currentTimeMillis() / 1000);
                
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(config.getSlackWebhookUrl()))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();
                
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
            } catch (Exception e) {
                throw new RuntimeException("Slack webhook failed", e);
            }
        });
    }
    
    private String getSeverityLevel(AlertType alertType) {
        return switch (alertType) {
            case CPU_HIGH, RAM_HIGH, DISK_HIGH -> "⚠️ Warning";
            case SYSTEM_CRITICAL -> "🚨 Critical";
        };
    }
    
    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
    
    private String getAlertEmoji(AlertType alertType) {
        return switch (alertType) {
            case CPU_HIGH -> "⚡";
            case RAM_HIGH -> "🧠";
            case DISK_HIGH -> "💾";
            case SYSTEM_CRITICAL -> "🚨";
        };
    }
    
    private String getAlertColor(AlertType alertType) {
        return switch (alertType) {
            case CPU_HIGH, RAM_HIGH, DISK_HIGH -> "16753920"; // Orange
            case SYSTEM_CRITICAL -> "16711680"; // Red
        };
    }
    
    private String getSlackColor(AlertType alertType) {
        return switch (alertType) {
            case CPU_HIGH, RAM_HIGH, DISK_HIGH -> "warning";
            case SYSTEM_CRITICAL -> "danger";
        };
    }
    
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return "%.2f KB".formatted(bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return "%.2f MB".formatted(bytes / (1024.0 * 1024));
        return "%.2f GB".formatted(bytes / (1024.0 * 1024 * 1024));
    }
    
    public void shutdown() {
        // Any cleanup needed
        logger.info("Alert manager shutdown completed");
    }

    // Enhanced Discord Integration - System Status Reports
    public CompletableFuture<Void> sendDiscordStatusReport(SystemMonitor.MemoryUsage memUsage, 
                                                          double cpuUsage, 
                                                          java.util.List<SystemMonitor.DiskUsage> diskUsages,
                                                          int currentPlayers, 
                                                          int maxPlayers) {
        return CompletableFuture.runAsync(() -> {
            try {
                if (!config.isDiscordWebhookEnabled() || config.getDiscordWebhookUrl().isEmpty()) {
                    return;
                }
                
                String timestamp = Instant.now().toString();
                
                // Determine overall system health
                String healthStatus = determineSystemHealth(memUsage, cpuUsage, diskUsages);
                String healthEmoji = getHealthEmoji(healthStatus);
                String healthColor = getHealthColor(healthStatus);
                
                // Build disk usage summary
                StringBuilder diskSummary = new StringBuilder();
                for (SystemMonitor.DiskUsage disk : diskUsages) {
                    if (diskSummary.length() > 0) diskSummary.append("\\n");
                    diskSummary.append("**%s**: %.1f%% (%.1f GB free)".formatted(
                        disk.getName(), disk.getUsagePercent(), disk.getFree() / (1024.0 * 1024 * 1024)));
                }
                if (diskSummary.length() == 0) {
                    diskSummary.append("No disk data available");
                }
                
                String jsonPayload = """
                    {
                        "content": null,
                        "embeds": [{
                            "title": "%s System Status Report",
                            "description": "Current server performance metrics and health status",
                            "color": %s,
                            "timestamp": "%s",
                            "thumbnail": {
                                "url": "https://i.imgur.com/chart-icon.png"
                            },
                            "fields": [
                                {
                                    "name": "🖥️ CPU Usage",
                                    "value": "%.2f%%",
                                    "inline": true
                                },
                                {
                                    "name": "🧠 Memory Usage",
                                    "value": "%.1f%% (%.1f GB / %.1f GB)",
                                    "inline": true
                                },
                                {
                                    "name": "👥 Players Online",
                                    "value": "%d / %d (%.1f%%)",
                                    "inline": true
                                },
                                {
                                    "name": "💾 Disk Usage",
                                    "value": "%s",
                                    "inline": false
                                },
                                {
                                    "name": "🏥 Overall Health",
                                    "value": "%s %s",
                                    "inline": false
                                }
                            ],
                            "footer": {
                                "text": "BubbleLog System Monitor • Status Report",
                                "icon_url": "https://i.imgur.com/rNNH9lq.png"
                            }
                        }]
                    }""".formatted(
                    healthEmoji, healthColor, timestamp,
                    cpuUsage * 100,
                    memUsage.getUsagePercent(),
                    memUsage.getUsed() / (1024.0 * 1024 * 1024),
                    memUsage.getTotal() / (1024.0 * 1024 * 1024),
                    currentPlayers, maxPlayers,
                    maxPlayers > 0 ? (double) currentPlayers / maxPlayers * 100 : 0.0,
                    escapeJson(diskSummary.toString()),
                    healthEmoji, healthStatus
                );
                
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(config.getDiscordWebhookUrl()))
                    .header("Content-Type", "application/json")
                    .header("User-Agent", "BubbleLog/1.0.0 (System Monitor)")
                    .timeout(java.time.Duration.ofSeconds(10))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();
                
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                if (response.statusCode() >= 400) {
                    logger.debug("Discord status report webhook returned error: {} - {}", response.statusCode(), response.body());
                } else {
                    logger.debug("Discord status report sent successfully");
                }
                
            } catch (Exception e) {
                logger.debug("Failed to send Discord status report: {}", e.getMessage());
            }
        });
    }
    
    private String determineSystemHealth(SystemMonitor.MemoryUsage memUsage, double cpuUsage, java.util.List<SystemMonitor.DiskUsage> diskUsages) {
        // Determine overall system health based on thresholds
        boolean cpuHigh = cpuUsage * 100 > config.getCpuThreshold();
        boolean ramHigh = memUsage.getUsagePercent() > config.getRamThreshold();
        boolean diskHigh = diskUsages.stream().anyMatch(disk -> disk.getUsagePercent() > config.getDiskThreshold());
        
        if (cpuHigh && ramHigh) {
            return "Critical";
        } else if (cpuHigh || ramHigh || diskHigh) {
            return "Warning";
        } else if (cpuUsage * 100 > config.getCpuThreshold() * 0.8 || 
                   memUsage.getUsagePercent() > config.getRamThreshold() * 0.8) {
            return "Caution";
        } else {
            return "Healthy";
        }
    }
    
    private String getHealthEmoji(String health) {
        return switch (health) {
            case "Healthy" -> "✅";
            case "Caution" -> "⚠️";
            case "Warning" -> "🟠";
            case "Critical" -> "🔴";
            default -> "❓";
        };
    }
    
    private String getHealthColor(String health) {
        return switch (health) {
            case "Healthy" -> "5763719";   // Green
            case "Caution" -> "16776960";  // Yellow
            case "Warning" -> "16753920";  // Orange
            case "Critical" -> "16711680"; // Red
            default -> "9807270";          // Gray
        };
    }
    
    /**
     * Send a test webhook to verify Discord integration
     */
    public CompletableFuture<Void> sendTestWebhook(String senderName) {
        return CompletableFuture.runAsync(() -> {
            try {
                if (!config.isDiscordWebhookEnabled()) {
                    throw new RuntimeException("Discord webhook is not enabled");
                }
                
                if (config.getDiscordWebhookUrl().isEmpty()) {
                    throw new RuntimeException("Discord webhook URL is not configured");
                }
                
                String timestamp = Instant.now().toString();
                
                String jsonPayload = """
                    {
                        "content": null,
                        "embeds": [{
                            "title": "🧪 Test Webhook",
                            "description": "This is a test message to verify that Discord webhook integration is working correctly.",
                            "color": 3447003,
                            "timestamp": "%s",
                            "thumbnail": {
                                "url": "https://i.imgur.com/FTpCNyQ.png"
                            },
                            "fields": [
                                {
                                    "name": "Initiated by",
                                    "value": "%s",
                                    "inline": true
                                },
                                {
                                    "name": "Test Status",
                                    "value": "✅ Connection Successful",
                                    "inline": true
                                },
                                {
                                    "name": "Server",
                                    "value": "Velocity Proxy",
                                    "inline": true
                                },
                                {
                                    "name": "Note",
                                    "value": "If you can see this message, your Discord webhook is configured correctly!",
                                    "inline": false
                                }
                            ],
                            "footer": {
                                "text": "BubbleLog System Monitor • Test Message",
                                "icon_url": "https://i.imgur.com/rNNH9lq.png"
                            }
                        }],
                        "attachments": []
                    }""".formatted(
                    timestamp,
                    escapeJson(senderName)
                );
                
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(config.getDiscordWebhookUrl()))
                    .header("Content-Type", "application/json")
                    .header("User-Agent", "BubbleLog/1.0.0 (System Monitor)")
                    .timeout(java.time.Duration.ofSeconds(10))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();
                
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                if (response.statusCode() >= 400) {
                    logger.warn("Test Discord webhook returned error status: {} - {}", response.statusCode(), response.body());
                    throw new RuntimeException("Discord webhook returned error status: " + response.statusCode());
                } else {
                    logger.info("Test Discord webhook sent successfully by {}", senderName);
                }
                
            } catch (java.net.SocketTimeoutException e) {
                logger.warn("Test Discord webhook timed out - network may be slow");
                throw new RuntimeException("Discord webhook timeout", e);
            } catch (java.io.IOException e) {
                logger.warn("Test Discord webhook network error: {}", e.getMessage());
                throw new RuntimeException("Discord webhook network error", e);
            } catch (Exception e) {
                logger.warn("Test Discord webhook failed: {}", e.getMessage());
                throw new RuntimeException("Test Discord webhook failed", e);
            }
        });
    }
    
    /**
     * Send a test alert with simulated high resource usage
     */
    public void sendTestAlert(SystemMonitor.MemoryUsage testMemUsage, double testCpuUsage, String senderName) {
        try {
            if (!config.isPerformanceAlertsEnabled()) {
                throw new RuntimeException("Performance alerts are not enabled");
            }
            
            // Override cooldown for test alerts
            long currentTime = System.currentTimeMillis();
            for (AlertType type : AlertType.values()) {
                lastAlertTimes.put(type, currentTime - config.getAlertCooldown() * 1000L - 1000L);
            }
            
            String testMessage = (
                """
                TEST ALERT - Simulated high resource usage (initiated by %s):
                • CPU Usage: %.1f%% (threshold: %.1f%%)
                • RAM Usage: %.1f%% (threshold: %.1f%%)
                • Used Memory: %s / %s
                • Available Memory: %s
                
                This is a test alert to verify that your alert system is working correctly.""").formatted(
                senderName,
                testCpuUsage * 100, config.getCpuThreshold(),
                testMemUsage.getUsagePercent(), config.getRamThreshold(),
                formatBytes(testMemUsage.getUsed()), formatBytes(testMemUsage.getTotal()),
                formatBytes(testMemUsage.getAvailable())
            );
            
            // Send alert through configured channels
            if (config.isDiscordWebhookEnabled() && !config.getDiscordWebhookUrl().isEmpty()) {
                sendDiscordWebhook(AlertType.SYSTEM_CRITICAL, testMessage).exceptionally(throwable -> {
                    logger.warn("Test Discord alert failed", throwable);
                    return null;
                });
            }
            
            if (config.isSlackWebhookEnabled() && !config.getSlackWebhookUrl().isEmpty()) {
                sendSlackWebhook(AlertType.SYSTEM_CRITICAL, testMessage).exceptionally(throwable -> {
                    logger.warn("Test Slack alert failed", throwable);
                    return null;
                });
            }
            
            // Write to file
            try {
                writeAlertToFile("[%s] TEST ALERT: %s".formatted(
                    LocalDateTime.now().format(dateFormatter), testMessage));
            } catch (IOException e) {
                logger.warn("Failed to write test alert to file", e);
            }
            
            logger.info("Test alert sent by {}", senderName);
            
        } catch (Exception e) {
            logger.error("Failed to send test alert", e);
            throw new RuntimeException("Failed to send test alert: " + e.getMessage(), e);
        }
    }
}
