package net.bubblecraft.bubblelog.monitor;

import org.slf4j.Logger;
import oshi.SystemInfo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Detects hosting environment capabilities and restrictions.
 * Helps the plugin adapt to shared hosting environments with limited system access.
 */
public class HostingEnvironment {
    
    private final Logger logger;
    private final boolean hasSystemAccess;
    private final boolean hasDiskAccess;
    private final boolean hasNetworkAccess;
    private final boolean isContainerized;
    private final boolean isSharedHosting;
    private final CapabilityLevel capabilityLevel;
    
    public enum CapabilityLevel {
        FULL("Full system access - all features available"),
        LIMITED("Limited access - some features restricted"),
        MINIMAL("Minimal access - only basic features available"),
        RESTRICTED("Restricted environment - only JVM metrics available");
        
        private final String description;
        
        CapabilityLevel(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    public HostingEnvironment(Logger logger) {
        this.logger = logger;
        
        // Detect system access capabilities
        this.hasSystemAccess = checkSystemAccess();
        this.hasDiskAccess = checkDiskAccess();
        this.hasNetworkAccess = checkNetworkAccess();
        this.isContainerized = detectContainer();
        this.isSharedHosting = detectSharedHosting();
        this.capabilityLevel = determineCapabilityLevel();
        
        logEnvironmentInfo();
    }
    
    private boolean checkSystemAccess() {
        try {
            SystemInfo systemInfo = new SystemInfo();
            systemInfo.getHardware().getProcessor().getProcessorIdentifier();
            return true;
        } catch (SecurityException | UnsatisfiedLinkError e) {
            logger.warn("Limited system access detected: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            logger.debug("System access check failed", e);
            return false;
        }
    }
    
    private boolean checkDiskAccess() {
        try {
            SystemInfo systemInfo = new SystemInfo();
            systemInfo.getOperatingSystem().getFileSystem().getFileStores();
            return true;
        } catch (SecurityException | UnsatisfiedLinkError e) {
            logger.debug("Limited disk access detected: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            logger.debug("Disk access check failed", e);
            return false;
        }
    }
    
    private boolean checkNetworkAccess() {
        try {
            SystemInfo systemInfo = new SystemInfo();
            systemInfo.getHardware().getNetworkIFs();
            return true;
        } catch (SecurityException | UnsatisfiedLinkError e) {
            logger.debug("Limited network access detected: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            logger.debug("Network access check failed", e);
            return false;
        }
    }
    
    private boolean detectContainer() {
        try {
            // Check for common container indicators
            Path cgroupPath = Paths.get("/proc/1/cgroup");
            if (Files.exists(cgroupPath)) {
                String content = Files.readString(cgroupPath);
                if (content.contains("docker") || content.contains("lxc") || 
                    content.contains("kubepods") || content.contains("containerd")) {
                    return true;
                }
            }
            
            // Check for .dockerenv
            if (Files.exists(Paths.get("/.dockerenv"))) {
                return true;
            }
            
            // Check environment variables
            String container = System.getenv("container");
            if (container != null && !container.isEmpty()) {
                return true;
            }
            
        } catch (IOException | SecurityException e) {
            logger.debug("Container detection check failed", e);
        }
        
        return false;
    }
    
    private boolean detectSharedHosting() {
        // Indicators of shared hosting environment
        if (!hasSystemAccess || !hasDiskAccess) {
            return true;
        }
        
        // Check if running with very limited permissions
        try {
            String userHome = System.getProperty("user.home");
            
            // Common shared hosting patterns
            if (userHome != null && (userHome.contains("/home/") || 
                                     userHome.contains("minecraft") ||
                                     userHome.contains("gameserver"))) {
                return true;
            }
        } catch (SecurityException e) {
            return true; // If we can't even read properties, it's very restricted
        }
        
        return false;
    }
    
    private CapabilityLevel determineCapabilityLevel() {
        if (hasSystemAccess && hasDiskAccess && hasNetworkAccess) {
            return CapabilityLevel.FULL;
        } else if (hasSystemAccess && hasDiskAccess) {
            return CapabilityLevel.LIMITED;
        } else if (hasSystemAccess || hasDiskAccess) {
            return CapabilityLevel.MINIMAL;
        } else {
            return CapabilityLevel.RESTRICTED;
        }
    }
    
    private void logEnvironmentInfo() {
        logger.info("=== Hosting Environment Detection ===");
        logger.info("Capability Level: {} - {}", capabilityLevel, capabilityLevel.getDescription());
        logger.info("System Access: {}", hasSystemAccess ? "✓" : "✗");
        logger.info("Disk Access: {}", hasDiskAccess ? "✓" : "✗");
        logger.info("Network Access: {}", hasNetworkAccess ? "✓" : "✗");
        logger.info("Containerized: {}", isContainerized ? "Yes" : "No");
        logger.info("Shared Hosting: {}", isSharedHosting ? "Likely" : "Unlikely");
        logger.info("=====================================");
        
        if (capabilityLevel == CapabilityLevel.RESTRICTED || capabilityLevel == CapabilityLevel.MINIMAL) {
            logger.warn("Running in restricted environment - some monitoring features will be limited to JVM metrics only");
            logger.warn("This is normal for shared hosting providers and will not affect plugin functionality");
        }
    }
    
    public boolean hasSystemAccess() {
        return hasSystemAccess;
    }
    
    public boolean hasDiskAccess() {
        return hasDiskAccess;
    }
    
    public boolean hasNetworkAccess() {
        return hasNetworkAccess;
    }
    
    public boolean isContainerized() {
        return isContainerized;
    }
    
    public boolean isSharedHosting() {
        return isSharedHosting;
    }
    
    public CapabilityLevel getCapabilityLevel() {
        return capabilityLevel;
    }
    
    public boolean canMonitorCPU() {
        return hasSystemAccess;
    }
    
    public boolean canMonitorRAM() {
        return hasSystemAccess;
    }
    
    public boolean canMonitorDisk() {
        return hasDiskAccess;
    }
    
    public boolean canMonitorNetwork() {
        return hasNetworkAccess;
    }
    
    public boolean shouldUseJVMFallback() {
        return capabilityLevel == CapabilityLevel.RESTRICTED || capabilityLevel == CapabilityLevel.MINIMAL;
    }
}
