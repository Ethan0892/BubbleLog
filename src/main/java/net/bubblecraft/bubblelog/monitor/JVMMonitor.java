package net.bubblecraft.bubblelog.monitor;

import net.bubblecraft.bubblelog.config.ConfigManager;
import org.slf4j.Logger;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.ThreadMXBean;
import java.util.HashMap;
import java.util.Map;

/**
 * Monitors JVM-specific metrics for performance analysis
 */
public class JVMMonitor {
    
    private final Logger logger;
    private final ConfigManager config;
    
    public JVMMonitor(Logger logger, ConfigManager config) {
        this.logger = logger;
        this.config = config;
    }
    
    public JVMUsage getJVMUsage() {
        try {
            // Memory usage
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
            MemoryUsage nonHeapUsage = memoryBean.getNonHeapMemoryUsage();
            
            // Thread information
            ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
            int threadCount = threadBean.getThreadCount();
            int daemonThreadCount = threadBean.getDaemonThreadCount();
            
            // Garbage collection stats
            Map<String, GCInfo> gcStats = new HashMap<>();
            for (GarbageCollectorMXBean gcBean : ManagementFactory.getGarbageCollectorMXBeans()) {
                String gcName = gcBean.getName();
                long collectionCount = gcBean.getCollectionCount();
                long collectionTime = gcBean.getCollectionTime();
                
                gcStats.put(gcName, new GCInfo(gcName, collectionCount, collectionTime));
            }
            
            // Runtime information
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long maxMemory = runtime.maxMemory();
            long usedMemory = totalMemory - freeMemory;
            
            double heapUtilization = heapUsage.getMax() > 0 ? 
                (double) heapUsage.getUsed() / heapUsage.getMax() * 100 : 0.0;
            
            return new JVMUsage(
                heapUsage.getUsed(), heapUsage.getMax(), heapUtilization,
                nonHeapUsage.getUsed(), nonHeapUsage.getMax(),
                threadCount, daemonThreadCount,
                gcStats,
                usedMemory, totalMemory, maxMemory
            );
            
        } catch (Exception e) {
            logger.debug("Error getting JVM usage", e);
            return new JVMUsage(0, 0, 0.0, 0, 0, 0, 0, new HashMap<>(), 0, 0, 0);
        }
    }
    
    // Data classes
    public static class JVMUsage {
        private final long heapUsed;
        private final long heapMax;
        private final double heapUtilization;
        private final long nonHeapUsed;
        private final long nonHeapMax;
        private final int threadCount;
        private final int daemonThreadCount;
        private final Map<String, GCInfo> gcStats;
        private final long runtimeUsed;
        private final long runtimeTotal;
        private final long runtimeMax;
        
        public JVMUsage(long heapUsed, long heapMax, double heapUtilization,
                       long nonHeapUsed, long nonHeapMax,
                       int threadCount, int daemonThreadCount,
                       Map<String, GCInfo> gcStats,
                       long runtimeUsed, long runtimeTotal, long runtimeMax) {
            this.heapUsed = heapUsed;
            this.heapMax = heapMax;
            this.heapUtilization = heapUtilization;
            this.nonHeapUsed = nonHeapUsed;
            this.nonHeapMax = nonHeapMax;
            this.threadCount = threadCount;
            this.daemonThreadCount = daemonThreadCount;
            this.gcStats = gcStats;
            this.runtimeUsed = runtimeUsed;
            this.runtimeTotal = runtimeTotal;
            this.runtimeMax = runtimeMax;
        }
        
        // Getters
        public long getHeapUsed() { return heapUsed; }
        public long getHeapMax() { return heapMax; }
        public double getHeapUtilization() { return heapUtilization; }
        public long getNonHeapUsed() { return nonHeapUsed; }
        public long getNonHeapMax() { return nonHeapMax; }
        public int getThreadCount() { return threadCount; }
        public int getDaemonThreadCount() { return daemonThreadCount; }
        public Map<String, GCInfo> getGcStats() { return gcStats; }
        public long getRuntimeUsed() { return runtimeUsed; }
        public long getRuntimeTotal() { return runtimeTotal; }
        public long getRuntimeMax() { return runtimeMax; }
    }
    
    public static class GCInfo {
        private final String name;
        private final long collectionCount;
        private final long collectionTime;
        
        public GCInfo(String name, long collectionCount, long collectionTime) {
            this.name = name;
            this.collectionCount = collectionCount;
            this.collectionTime = collectionTime;
        }
        
        public String getName() { return name; }
        public long getCollectionCount() { return collectionCount; }
        public long getCollectionTime() { return collectionTime; }
    }
}
