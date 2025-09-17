package net.bubblecraft.bubblelog.monitor;

import net.bubblecraft.bubblelog.config.ConfigManager;
import net.bubblecraft.bubblelog.alert.AlertManager;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.software.os.FileSystem;
import oshi.software.os.OSFileStore;
import oshi.software.os.OperatingSystem;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.ThreadMXBean;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ClassLoadingMXBean;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class SystemMonitor {
    
    private final Path dataDirectory;
    private final Logger logger;
    private final ConfigManager config;
    private final SystemInfo systemInfo;
    private final HardwareAbstractionLayer hardware;
    private final OperatingSystem os;
    private final AlertManager alertManager;
    private final ProxyServer server; // For network monitoring
    
    private final DateTimeFormatter dateFormatter;
    private long[] prevTicks;
    
    // Lightweight caching for network data to avoid excessive API calls
    private volatile NetworkData cachedNetworkData;
    private volatile long lastNetworkCheck = 0;
    private static final long NETWORK_CACHE_MS = 5000; // 5 second cache
    
    // Connection quality monitoring cache
    private volatile ConnectionQualityData cachedConnectionData;
    private volatile long lastConnectionCheck = 0;
    private static final long CONNECTION_CACHE_MS = 10000; // 10 second cache for connection quality
    
    // JVM monitoring components (lightweight)
    private final MemoryMXBean memoryBean;
    private final ThreadMXBean threadBean;
    
    // Performance optimization: pre-allocate StringBuilder to reduce allocations
    private final StringBuilder logBuilder = new StringBuilder(512);
    
    public SystemMonitor(Path dataDirectory, Logger logger, ConfigManager config, AlertManager alertManager, ProxyServer server) {
        this.dataDirectory = dataDirectory;
        this.logger = logger;
        this.config = config;
        this.alertManager = alertManager;
        this.server = server;
        this.systemInfo = new SystemInfo();
        this.hardware = systemInfo.getHardware();
        this.os = systemInfo.getOperatingSystem();
        this.dateFormatter = DateTimeFormatter.ofPattern(config.getDateFormat());
        
        // Initialize CPU monitoring
        CentralProcessor processor = hardware.getProcessor();
        this.prevTicks = processor.getSystemCpuLoadTicks();
        
        // Initialize JVM monitoring components (lightweight)
        this.memoryBean = ManagementFactory.getMemoryMXBean();
        this.threadBean = ManagementFactory.getThreadMXBean();
    }
    
    public void logSystemUsage() {
        try {
            // Clear and reuse StringBuilder for performance
            logBuilder.setLength(0);
            String timestamp = LocalDateTime.now().format(dateFormatter);
            
            logBuilder.append("[").append(timestamp).append("] ");
            
            // CPU Usage - with error handling
            double cpuUsage = 0.0;
            if (config.isCpuMonitoringEnabled()) {
                try {
                    cpuUsage = getCpuUsage();
                    logBuilder.append("CPU: ").append(String.format("%.2f%%", cpuUsage * 100));
                } catch (Exception e) {
                    logger.debug("Failed to get CPU usage", e);
                    logBuilder.append("CPU: N/A");
                }
            }
            
            // RAM Usage - with error handling
            MemoryUsage memUsage = null;
            if (config.isRamMonitoringEnabled()) {
                try {
                    memUsage = getMemoryUsage();
                    if (logBuilder.length() > timestamp.length() + 3) {
                        logBuilder.append(" | ");
                    }
                    logBuilder.append("RAM: ")
                        .append(formatBytes(memUsage.getUsed()))
                        .append("/")
                        .append(formatBytes(memUsage.getTotal()))
                        .append(" (")
                        .append(String.format("%.2f%%", memUsage.getUsagePercent()))
                        .append(")");
                } catch (Exception e) {
                    logger.debug("Failed to get RAM usage", e);
                    if (logBuilder.length() > timestamp.length() + 3) {
                        logBuilder.append(" | ");
                    }
                    logBuilder.append("RAM: N/A");
                    // Create dummy memory usage for alerts
                    memUsage = new MemoryUsage(0, 0, 0, 0.0);
                }
            }
            
            // Disk Usage - with error handling
            List<DiskUsage> diskUsages = java.util.Collections.emptyList();
            if (config.isDiskMonitoringEnabled()) {
                try {
                    diskUsages = getDiskUsage();
                    if (!diskUsages.isEmpty() && logBuilder.length() > timestamp.length() + 3) {
                        logBuilder.append(" | ");
                    }
                    
                    for (int i = 0; i < diskUsages.size(); i++) {
                        DiskUsage disk = diskUsages.get(i);
                        if (i > 0) logBuilder.append(", ");
                        logBuilder.append("Disk(").append(disk.getName()).append("): ")
                            .append(formatBytes(disk.getUsed()))
                            .append("/")
                            .append(formatBytes(disk.getTotal()))
                            .append(" (")
                            .append(String.format("%.2f%%", disk.getUsagePercent()))
                            .append(")");
                    }
                } catch (Exception e) {
                    logger.debug("Failed to get disk usage", e);
                    if (logBuilder.length() > timestamp.length() + 3) {
                        logBuilder.append(" | ");
                    }
                    logBuilder.append("Disk: N/A");
                }
            }
            
            // Network & Player Monitoring - with caching for performance
            NetworkData networkData = null;
            if (config.isNetworkMonitoringEnabled()) {
                try {
                    networkData = getNetworkData();
                    if (logBuilder.length() > timestamp.length() + 3) {
                        logBuilder.append(" | ");
                    }
                    logBuilder.append("Players: ").append(networkData.getCurrentPlayers())
                        .append("/").append(networkData.getMaxPlayers())
                        .append(" (").append(String.format("%.1f%%", networkData.getServerUtilization())).append(")")
                        .append(", Servers: ").append(networkData.getOnlineServers())
                        .append("/").append(networkData.getTotalServers());
                } catch (Exception e) {
                    logger.debug("Failed to get network data", e);
                    if (logBuilder.length() > timestamp.length() + 3) {
                        logBuilder.append(" | ");
                    }
                    logBuilder.append("Network: N/A");
                }
            }
            
            // JVM Monitoring - lightweight essential metrics only
            JVMData jvmData = null;
            if (config.isJvmMonitoringEnabled()) {
                try {
                    jvmData = getJVMData();
                    if (logBuilder.length() > timestamp.length() + 3) {
                        logBuilder.append(" | ");
                    }
                    logBuilder.append("JVM: Heap ").append(String.format("%.1f%%", jvmData.getHeapUtilization()))
                        .append(", NonHeap: ").append(String.format("%.1f MB", jvmData.getNonHeapUsedMB()))
                        .append(", Threads: ").append(jvmData.getThreadCount())
                        .append(", GC: ").append(jvmData.getTotalGCTime()).append("ms")
                        .append(", Classes: ").append(jvmData.getLoadedClassCount());
                } catch (Exception e) {
                    logger.debug("Failed to get JVM data", e);
                    if (logBuilder.length() > timestamp.length() + 3) {
                        logBuilder.append(" | ");
                    }
                    logBuilder.append("JVM: N/A");
                }
            }
            
            // Connection Quality Monitoring - lightweight network performance metrics
            ConnectionQualityData connectionData = null;
            if (config.isConnectionQualityMonitoringEnabled()) {
                try {
                    connectionData = getConnectionQualityData();
                    if (logBuilder.length() > timestamp.length() + 3) {
                        logBuilder.append(" | ");
                    }
                    logBuilder.append("Connection: Avg Ping ").append(String.format("%.1f", connectionData.getAveragePing())).append("ms")
                        .append(", Max Ping: ").append(String.format("%.1f", connectionData.getMaxPing())).append("ms")
                        .append(", Quality: ").append(connectionData.getConnectionQuality());
                    
                    // Optional: Add packet loss if available
                    if (connectionData.getPacketLoss() >= 0) {
                        logBuilder.append(", Loss: ").append(String.format("%.2f%%", connectionData.getPacketLoss()));
                    }
                } catch (Exception e) {
                    logger.debug("Failed to get connection quality data", e);
                    if (logBuilder.length() > timestamp.length() + 3) {
                        logBuilder.append(" | ");
                    }
                    logBuilder.append("Connection: N/A");
                }
            }
            
            String logLine = logBuilder.toString();
            
            // Write to file - with error handling
            try {
                writeToLogFile(logLine);
            } catch (Exception e) {
                logger.warn("Failed to write to log file", e);
                // Don't let file write errors stop the plugin
            }
            
            // Optionally log to console - with error handling
            try {
                if (config.isConsoleLoggingEnabled()) {
                    logger.info("System Usage: {}", logLine.substring(logLine.indexOf("]") + 2));
                }
            } catch (Exception e) {
                logger.debug("Failed to log to console", e);
                // Don't let console logging errors stop the plugin
            }
            
            // Check for performance alerts - with error handling
            try {
                if (config.isPerformanceAlertsEnabled() && alertManager != null) {
                    // Use the already computed values or defaults
                    MemoryUsage alertMemUsage = memUsage != null ? memUsage : new MemoryUsage(0, 0, 0, 0.0);
                    
                    alertManager.checkAndSendAlerts(alertMemUsage, cpuUsage, diskUsages);
                }
            } catch (Exception e) {
                logger.warn("Failed to check performance alerts", e);
                // Don't let alert errors stop the monitoring
            }
            
        } catch (Exception e) {
            logger.error("Critical error in system monitoring - monitoring will continue", e);
            // Ensure monitoring continues even if there's a critical error
        }
    }
    
    private double getCpuUsage() {
        try {
            CentralProcessor processor = hardware.getProcessor();
            long[] currentTicks = processor.getSystemCpuLoadTicks();
            double cpuUsage = processor.getSystemCpuLoadBetweenTicks(prevTicks);
            prevTicks = currentTicks;
            
            // Validate CPU usage value
            if (Double.isNaN(cpuUsage) || Double.isInfinite(cpuUsage) || cpuUsage < 0) {
                logger.debug("Invalid CPU usage value: {}, returning 0", cpuUsage);
                return 0.0;
            }
            
            return Math.min(cpuUsage, 1.0); // Cap at 100%
        } catch (Exception e) {
            logger.debug("Error getting CPU usage", e);
            return 0.0; // Return safe default
        }
    }
    
    private MemoryUsage getMemoryUsage() {
        try {
            GlobalMemory memory = hardware.getMemory();
            long total = memory.getTotal();
            long available = memory.getAvailable();
            long used = total - available;
            
            // Validate memory values
            if (total <= 0 || available < 0 || used < 0) {
                logger.debug("Invalid memory values: total={}, available={}, used={}", total, available, used);
                return new MemoryUsage(0, 0, 0, 0.0);
            }
            
            double usagePercent = total > 0 ? (double) used / total * 100 : 0.0;
            
            // Validate percentage
            if (Double.isNaN(usagePercent) || Double.isInfinite(usagePercent)) {
                usagePercent = 0.0;
            }
            
            return new MemoryUsage(total, used, available, Math.min(usagePercent, 100.0));
        } catch (Exception e) {
            logger.debug("Error getting memory usage", e);
            return new MemoryUsage(0, 0, 0, 0.0); // Return safe default
        }
    }
    
    private List<DiskUsage> getDiskUsage() {
        try {
            FileSystem fileSystem = os.getFileSystem();
            return fileSystem.getFileStores().stream()
                .filter(store -> {
                    try {
                        return store.getTotalSpace() > 0;
                    } catch (Exception e) {
                        logger.debug("Error checking disk store: {}", store.getName(), e);
                        return false;
                    }
                })
                .map(this::createDiskUsage)
                .filter(diskUsage -> diskUsage != null) // Filter out null results
                .toList();
        } catch (Exception e) {
            logger.debug("Error getting disk usage", e);
            return java.util.Collections.emptyList(); // Return safe default
        }
    }
    
    private DiskUsage createDiskUsage(OSFileStore store) {
        try {
            long total = store.getTotalSpace();
            long free = store.getUsableSpace();
            long used = total - free;
            
            // Validate disk values
            if (total <= 0 || free < 0 || used < 0) {
                logger.debug("Invalid disk values for {}: total={}, free={}, used={}", 
                    store.getName(), total, free, used);
                return null;
            }
            
            double usagePercent = total > 0 ? (double) used / total * 100 : 0.0;
            
            // Validate percentage
            if (Double.isNaN(usagePercent) || Double.isInfinite(usagePercent)) {
                usagePercent = 0.0;
            }
            
            String safeName = store.getName() != null ? store.getName() : "Unknown";
            
            return new DiskUsage(safeName, total, used, free, Math.min(usagePercent, 100.0));
        } catch (Exception e) {
            logger.debug("Error creating disk usage for store", e);
            return null; // Return null to be filtered out
        }
    }
    
    // Lightweight network monitoring with caching
    private NetworkData getNetworkData() {
        long currentTime = System.currentTimeMillis();
        
        // Use cached data if still fresh (performance optimization)
        if (cachedNetworkData != null && (currentTime - lastNetworkCheck) < NETWORK_CACHE_MS) {
            return cachedNetworkData;
        }
        
        try {
            int currentPlayers = server.getPlayerCount();
            int maxPlayers = server.getConfiguration().getShowMaxPlayers();
            double serverUtilization = maxPlayers > 0 ? (double) currentPlayers / maxPlayers * 100 : 0.0;
            
            // Count online servers efficiently (don't ping, just check registration)
            int totalServers = server.getAllServers().size();
            int onlineServers = 0;
            
            // Quick check without blocking pings for performance
            for (RegisteredServer registeredServer : server.getAllServers()) {
                // Consider server online if it has connected players or is responding
                if (registeredServer.getPlayersConnected().size() > 0) {
                    onlineServers++;
                } else {
                    // Only ping if no players to avoid performance impact
                    try {
                        registeredServer.ping().getNow(null); // Non-blocking check
                        onlineServers++;
                    } catch (Exception ignored) {
                        // Server likely offline, don't count it
                    }
                }
            }
            
            cachedNetworkData = new NetworkData(currentPlayers, maxPlayers, serverUtilization, onlineServers, totalServers);
            lastNetworkCheck = currentTime;
            
            return cachedNetworkData;
            
        } catch (Exception e) {
            logger.debug("Error getting network data", e);
            return new NetworkData(0, 0, 0.0, 0, 0);
        }
    }
    
    // Lightweight JVM monitoring - only essential metrics
    private JVMData getJVMData() {
        try {
            // Heap memory usage
            java.lang.management.MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
            double heapUtilization = heapUsage.getMax() > 0 ? 
                (double) heapUsage.getUsed() / heapUsage.getMax() * 100 : 0.0;
            
            // Non-heap memory usage (method area, code cache, etc.)
            java.lang.management.MemoryUsage nonHeapUsage = memoryBean.getNonHeapMemoryUsage();
            double nonHeapUsedMB = nonHeapUsage.getUsed() / (1024.0 * 1024.0);
            
            // Thread count
            int threadCount = threadBean.getThreadCount();
            
            // Loaded class count
            int loadedClassCount = 0;
            try {
                loadedClassCount = ManagementFactory.getClassLoadingMXBean().getLoadedClassCount();
            } catch (Exception e) {
                // Class loading info not available, skip
            }
            
            // Total GC time (lightweight - just sum all collectors)
            long totalGCTime = 0;
            try {
                for (GarbageCollectorMXBean gcBean : ManagementFactory.getGarbageCollectorMXBeans()) {
                    long gcTime = gcBean.getCollectionTime();
                    if (gcTime > 0) {
                        totalGCTime += gcTime;
                    }
                }
            } catch (Exception e) {
                // GC info not available, skip
            }
            
            return new JVMData(heapUtilization, nonHeapUsedMB, threadCount, loadedClassCount, totalGCTime);
            
        } catch (Exception e) {
            logger.debug("Error getting JVM data", e);
            return new JVMData(0.0, 0.0, 0, 0, 0);
        }
    }
    
    // Lightweight connection quality monitoring
    private ConnectionQualityData getConnectionQualityData() {
        try {
            long currentTime = System.currentTimeMillis();
            
            // Use cached data if available and not expired
            if (cachedConnectionData != null && (currentTime - lastConnectionCheck) < CONNECTION_CACHE_MS) {
                return cachedConnectionData;
            }
            
            // Quick connection quality check - avoid heavy operations
            double totalPing = 0.0;
            double maxPing = 0.0;
            int validPings = 0;
            int totalChecks = 0;
            
            // Sample a few servers for connection quality (max 3 to keep it lightweight)
            for (RegisteredServer registeredServer : server.getAllServers()) {
                if (totalChecks >= 3) break; // Limit checks for performance
                totalChecks++;
                
                try {
                    // Quick, non-blocking ping check
                    long startTime = System.nanoTime();
                    CompletableFuture<com.velocitypowered.api.proxy.server.ServerPing> pingFuture = 
                        registeredServer.ping();
                    
                    // Wait max 1 second for ping (lightweight timeout)
                    com.velocitypowered.api.proxy.server.ServerPing ping = pingFuture.get(1, java.util.concurrent.TimeUnit.SECONDS);
                    
                    if (ping != null) {
                        long endTime = System.nanoTime();
                        double pingTime = (endTime - startTime) / 1_000_000.0; // Convert to milliseconds
                        
                        totalPing += pingTime;
                        maxPing = Math.max(maxPing, pingTime);
                        validPings++;
                    }
                } catch (Exception e) {
                    // Ping failed or timed out - this is normal for offline servers
                    logger.debug("Ping failed for server {}: {}", registeredServer.getServerInfo().getName(), e.getMessage());
                }
            }
            
            double averagePing = validPings > 0 ? totalPing / validPings : -1.0;
            String quality = "Unknown";
            
            // Determine connection quality based on average ping
            if (averagePing >= 0) {
                if (averagePing < 50) {
                    quality = "Excellent";
                } else if (averagePing < 100) {
                    quality = "Good";
                } else if (averagePing < 200) {
                    quality = "Fair";
                } else {
                    quality = "Poor";
                }
            }
            
            // Calculate packet loss estimate (simplified - based on failed pings)
            double packetLoss = totalChecks > 0 ? 
                ((double) (totalChecks - validPings) / totalChecks) * 100 : -1.0;
            
            cachedConnectionData = new ConnectionQualityData(averagePing, maxPing, quality, packetLoss);
            lastConnectionCheck = currentTime;
            
            return cachedConnectionData;
            
        } catch (Exception e) {
            logger.debug("Error getting connection quality data", e);
            return new ConnectionQualityData(-1.0, -1.0, "Error", -1.0);
        }
    }
    
    private void writeToLogFile(String logLine) throws IOException {
        Path logFile = getLogFilePath();
        
        // Create parent directories if they don't exist
        if (!Files.exists(logFile.getParent())) {
            Files.createDirectories(logFile.getParent());
        }
        
        // Write log entry
        Files.write(logFile, (logLine + System.lineSeparator()).getBytes(), 
            StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        
        // Clean up old log files if needed
        cleanupOldLogFiles();
    }
    
    private Path getLogFilePath() {
        String filename = config.getLogFileName();
        
        // Add date to filename if it doesn't already contain date pattern
        if (!filename.contains("%date%")) {
            String baseName = filename.substring(0, filename.lastIndexOf('.'));
            String extension = filename.substring(filename.lastIndexOf('.'));
            String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            filename = baseName + "-" + date + extension;
        } else {
            String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            filename = filename.replace("%date%", date);
        }
        
        return dataDirectory.resolve("logs").resolve(filename);
    }
    
    private void cleanupOldLogFiles() {
        int maxFiles = config.getMaxLogFiles();
        if (maxFiles <= 0) return;
        
        try {
            Path logsDir = dataDirectory.resolve("logs");
            if (!Files.exists(logsDir)) return;
            
            Files.list(logsDir)
                .filter(path -> path.getFileName().toString().endsWith(".log"))
                .sorted((a, b) -> {
                    try {
                        return Files.getLastModifiedTime(b).compareTo(Files.getLastModifiedTime(a));
                    } catch (IOException e) {
                        return 0;
                    }
                })
                .skip(maxFiles)
                .forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                        logger.debug("Deleted old log file: {}", path.getFileName());
                    } catch (IOException e) {
                        logger.warn("Failed to delete old log file: {}", path.getFileName(), e);
                    }
                });
                
        } catch (IOException e) {
            logger.warn("Failed to clean up old log files", e);
        }
    }
    
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.2f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }
    
    public void shutdown() {
        // Any cleanup needed
        logger.info("System monitor shutdown completed");
    }
    
    // Data classes
    public static class MemoryUsage {
        private final long total;
        private final long used;
        private final long available;
        private final double usagePercent;
        
        public MemoryUsage(long total, long used, long available, double usagePercent) {
            this.total = total;
            this.used = used;
            this.available = available;
            this.usagePercent = usagePercent;
        }
        
        public long getTotal() { return total; }
        public long getUsed() { return used; }
        public long getAvailable() { return available; }
        public double getUsagePercent() { return usagePercent; }
    }
    
    public static class DiskUsage {
        private final String name;
        private final long total;
        private final long used;
        private final long free;
        private final double usagePercent;
        
        public DiskUsage(String name, long total, long used, long free, double usagePercent) {
            this.name = name;
            this.total = total;
            this.used = used;
            this.free = free;
            this.usagePercent = usagePercent;
        }
        
        public String getName() { return name; }
        public long getTotal() { return total; }
        public long getUsed() { return used; }
        public long getFree() { return free; }
        public double getUsagePercent() { return usagePercent; }
    }
    
    // Lightweight network monitoring data
    public static class NetworkData {
        private final int currentPlayers;
        private final int maxPlayers;
        private final double serverUtilization;
        private final int onlineServers;
        private final int totalServers;
        
        public NetworkData(int currentPlayers, int maxPlayers, double serverUtilization, 
                          int onlineServers, int totalServers) {
            this.currentPlayers = currentPlayers;
            this.maxPlayers = maxPlayers;
            this.serverUtilization = serverUtilization;
            this.onlineServers = onlineServers;
            this.totalServers = totalServers;
        }
        
        public int getCurrentPlayers() { return currentPlayers; }
        public int getMaxPlayers() { return maxPlayers; }
        public double getServerUtilization() { return serverUtilization; }
        public int getOnlineServers() { return onlineServers; }
        public int getTotalServers() { return totalServers; }
    }
    
    // Lightweight JVM monitoring data
    public static class JVMData {
        private final double heapUtilization;
        private final double nonHeapUsedMB;
        private final int threadCount;
        private final int loadedClassCount;
        private final long totalGCTime;
        
        public JVMData(double heapUtilization, double nonHeapUsedMB, int threadCount, int loadedClassCount, long totalGCTime) {
            this.heapUtilization = heapUtilization;
            this.nonHeapUsedMB = nonHeapUsedMB;
            this.threadCount = threadCount;
            this.loadedClassCount = loadedClassCount;
            this.totalGCTime = totalGCTime;
        }
        
        public double getHeapUtilization() { return heapUtilization; }
        public double getNonHeapUsedMB() { return nonHeapUsedMB; }
        public int getThreadCount() { return threadCount; }
        public int getLoadedClassCount() { return loadedClassCount; }
        public long getTotalGCTime() { return totalGCTime; }
    }
    
    // Lightweight connection quality monitoring data
    public static class ConnectionQualityData {
        private final double averagePing;
        private final double maxPing;
        private final String connectionQuality;
        private final double packetLoss;
        
        public ConnectionQualityData(double averagePing, double maxPing, String connectionQuality, double packetLoss) {
            this.averagePing = averagePing;
            this.maxPing = maxPing;
            this.connectionQuality = connectionQuality;
            this.packetLoss = packetLoss;
        }
        
        public double getAveragePing() { return averagePing; }
        public double getMaxPing() { return maxPing; }
        public String getConnectionQuality() { return connectionQuality; }
        public double getPacketLoss() { return packetLoss; }
    }
}
