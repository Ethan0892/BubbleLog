package net.bubblecraft.bubblelog;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import net.bubblecraft.bubblelog.config.ConfigManager;
import net.bubblecraft.bubblelog.monitor.SystemMonitor;
import net.bubblecraft.bubblelog.alert.AlertManager;
import net.bubblecraft.bubblelog.command.BubbleLogCommand;
import org.slf4j.Logger;

import java.nio.file.Path;
import com.velocitypowered.api.scheduler.ScheduledTask;
import java.util.concurrent.TimeUnit;

@Plugin(
    id = "bubblelog",
    name = "BubbleLog",
    version = "1.0.0",
    description = "CPU/RAM monitoring plugin for Velocity",
    authors = {"BubbleCraft"}
)
public class BubbleLog {
    
    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;
    private ConfigManager configManager;
    private SystemMonitor systemMonitor;
    private AlertManager alertManager;
    private ScheduledTask monitoringTask;
    
    @Inject
    public BubbleLog(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }
    
    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        logger.info("BubbleLog is starting up...");
        
        try {
            // Initialize configuration
            configManager = new ConfigManager(dataDirectory, logger);
            configManager.loadConfig();
            
            logger.info("Configuration loaded successfully");
            
        } catch (Exception e) {
            logger.error("Failed to load configuration - using defaults", e);
            // Create a minimal config manager with defaults
            configManager = new ConfigManager(dataDirectory, logger);
        }
        
        try {
            // Initialize alert manager
            alertManager = new AlertManager(dataDirectory, logger, configManager);
            
            // Initialize system monitor with proxy server reference
            systemMonitor = new SystemMonitor(dataDirectory, logger, configManager, alertManager, server);
            
            logger.info("System components initialized successfully");
            
        } catch (Exception e) {
            logger.error("Failed to initialize system components", e);
            // Try to initialize without alerting
            try {
                systemMonitor = new SystemMonitor(dataDirectory, logger, configManager, null, server);
                logger.warn("System monitor initialized without alerting functionality");
            } catch (Exception e2) {
                logger.error("Critical failure - could not initialize system monitor", e2);
                return; // Don't start monitoring if we can't initialize
            }
        }
        
        try {
            // Start monitoring task
            startMonitoring();
            
            // Register commands
            registerCommands();
            
            logger.info("BubbleLog has been enabled successfully!");
            logger.info("Monitoring interval: {} seconds", configManager.getMonitoringInterval());
            logger.info("Log file: {}", configManager.getLogFileName());
            if (configManager.isPerformanceAlertsEnabled()) {
                logger.info("Performance alerts: ENABLED");
            } else {
                logger.info("Performance alerts: DISABLED");
            }
            
        } catch (Exception e) {
            logger.error("Failed to start monitoring - plugin will remain inactive", e);
        }
    }
    
    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        logger.info("BubbleLog is shutting down...");
        
        try {
            if (monitoringTask != null) {
                monitoringTask.cancel();
                logger.debug("Monitoring task cancelled");
            }
        } catch (Exception e) {
            logger.warn("Error cancelling monitoring task", e);
        }
        
        try {
            if (systemMonitor != null) {
                systemMonitor.shutdown();
                logger.debug("System monitor shutdown completed");
            }
        } catch (Exception e) {
            logger.warn("Error shutting down system monitor", e);
        }
        
        try {
            if (alertManager != null) {
                alertManager.shutdown();
                logger.debug("Alert manager shutdown completed");
            }
        } catch (Exception e) {
            logger.warn("Error shutting down alert manager", e);
        }
        
        logger.info("BubbleLog has been disabled.");
    }
    
    private void startMonitoring() {
        try {
            int interval = configManager.getMonitoringInterval();
            
            // Validate interval
            if (interval <= 0) {
                logger.warn("Invalid monitoring interval: {}, using default of 30 seconds", interval);
                interval = 30;
            }
            
            monitoringTask = server.getScheduler().buildTask(this, () -> {
                try {
                    if (systemMonitor != null) {
                        systemMonitor.logSystemUsage();
                    }
                } catch (Exception e) {
                    logger.warn("Error during system monitoring - monitoring will continue", e);
                    // Don't let individual monitoring errors stop the scheduler
                }
            })
                .repeat(interval, TimeUnit.SECONDS)
                .schedule();
            
            logger.info("System monitoring started with interval of {} seconds", interval);
            
        } catch (Exception e) {
            logger.error("Failed to start monitoring task", e);
            throw e; // Re-throw to be handled by caller
        }
    }
    
    private void registerCommands() {
        try {
            // Register the main BubbleLog command
            BubbleLogCommand bubbleLogCommand = new BubbleLogCommand(this, logger);
            
            CommandMeta commandMeta = server.getCommandManager()
                .metaBuilder("bubblelog")
                .aliases("bl", "log")
                .plugin(this)
                .build();
            
            server.getCommandManager().register(commandMeta, bubbleLogCommand);
            
            logger.info("Commands registered successfully");
            
        } catch (Exception e) {
            logger.error("Failed to register commands", e);
        }
    }
    
    public ProxyServer getServer() {
        return server;
    }
    
    public Logger getLogger() {
        return logger;
    }
    
    public Path getDataDirectory() {
        return dataDirectory;
    }
    
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public AlertManager getAlertManager() {
        return alertManager;
    }
    
    public SystemMonitor getSystemMonitor() {
        return systemMonitor;
    }
}
