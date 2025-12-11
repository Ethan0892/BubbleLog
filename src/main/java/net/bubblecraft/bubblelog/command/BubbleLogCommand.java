package net.bubblecraft.bubblelog.command;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import net.bubblecraft.bubblelog.BubbleLog;
import net.bubblecraft.bubblelog.config.ConfigManager;
import net.bubblecraft.bubblelog.alert.AlertManager;
import net.bubblecraft.bubblelog.monitor.SystemMonitor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class BubbleLogCommand implements SimpleCommand {
    
    private final BubbleLog plugin;
    private final Logger logger;
    
    public BubbleLogCommand(BubbleLog plugin, Logger logger) {
        this.plugin = plugin;
        this.logger = logger;
    }
    
    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();
        
        if (args.length == 0) {
            sendHelp(source);
            return;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "reload" -> handleReload(source);
            case "validate" -> handleValidate(source);
            case "test" -> {
                if (args.length < 2) {
                    source.sendMessage(Component.text("Usage: /bubblelog test <webhook|alert>", NamedTextColor.RED));
                    return;
                }
                String testType = args[1].toLowerCase();
                switch (testType) {
                    case "webhook" -> handleTestWebhook(source);
                    case "alert" -> handleTestAlert(source);
                    default -> source.sendMessage(Component.text("Invalid test type. Use 'webhook' or 'alert'.", NamedTextColor.RED));
                }
            }
            case "status" -> handleStatus(source);
            case "info" -> handleInfo(source);
            case "env", "environment" -> handleEnvironment(source);
            default -> {
                source.sendMessage(Component.text("Unknown command. Use /bubblelog help for usage.", NamedTextColor.RED));
                sendHelp(source);
            }
        }
    }
    
    private void handleReload(CommandSource source) {
        source.sendMessage(Component.text("🔄 Reloading BubbleLog configuration...", NamedTextColor.YELLOW));
        
        try {
            // Reload configuration
            ConfigManager configManager = plugin.getConfigManager();
            if (configManager != null) {
                configManager.loadConfig();
                source.sendMessage(Component.text("✅ Configuration reloaded successfully!", NamedTextColor.GREEN));
                
                // Log the reload
                logger.info("Configuration reloaded by {}", 
                    source instanceof Player player ? player.getUsername() : "Console");
                
                // Show current monitoring status
                showMonitoringStatus(source, configManager);
                
            } else {
                source.sendMessage(Component.text("❌ Failed to reload configuration: ConfigManager not available", NamedTextColor.RED));
            }
            
        } catch (Exception e) {
            source.sendMessage(Component.text("❌ Failed to reload configuration: " + e.getMessage(), NamedTextColor.RED));
            logger.error("Failed to reload configuration", e);
        }
    }
    
    private void handleValidate(CommandSource source) {
        source.sendMessage(Component.text("🔍 Validating BubbleLog configuration...", NamedTextColor.YELLOW));
        
        try {
            ConfigManager configManager = plugin.getConfigManager();
            if (configManager == null) {
                source.sendMessage(Component.text("❌ ConfigManager not available", NamedTextColor.RED));
                return;
            }
            
            ConfigManager.ValidationResult result = configManager.validateConfig();
            
            if (result.isValid()) {
                source.sendMessage(Component.text("✅ Configuration is valid!", NamedTextColor.GREEN));
            } else {
                source.sendMessage(Component.text("❌ Configuration validation failed", NamedTextColor.RED));
            }
            
            // Show errors
            if (result.hasErrors()) {
                source.sendMessage(Component.text(""));
                source.sendMessage(Component.text("Errors:", NamedTextColor.RED, TextDecoration.BOLD));
                for (String error : result.getErrors()) {
                    source.sendMessage(Component.text("  ❌ " + error, NamedTextColor.RED));
                }
            }
            
            // Show warnings
            if (result.hasWarnings()) {
                source.sendMessage(Component.text(""));
                source.sendMessage(Component.text("Warnings:", NamedTextColor.YELLOW, TextDecoration.BOLD));
                for (String warning : result.getWarnings()) {
                    source.sendMessage(Component.text("  ⚠️ " + warning, NamedTextColor.YELLOW));
                }
            }
            
            logger.info("Configuration validated by {}: {} errors, {} warnings", 
                source instanceof Player player ? player.getUsername() : "Console",
                result.getErrors().size(), result.getWarnings().size());
            
        } catch (Exception e) {
            source.sendMessage(Component.text("❌ Validation failed: " + e.getMessage(), NamedTextColor.RED));
            logger.error("Configuration validation failed", e);
        }
    }
    
    private void handleEnvironment(CommandSource source) {
        try {
            SystemMonitor systemMonitor = plugin.getSystemMonitor();
            if (systemMonitor == null) {
                source.sendMessage(Component.text("❌ System monitor not available", NamedTextColor.RED));
                return;
            }
            
            net.bubblecraft.bubblelog.monitor.HostingEnvironment env = systemMonitor.getHostingEnvironment();
            if (env == null) {
                source.sendMessage(Component.text("❌ Environment info not available", NamedTextColor.RED));
                return;
            }
            
            source.sendMessage(Component.text("🌐 Hosting Environment", NamedTextColor.GOLD, TextDecoration.BOLD));
            source.sendMessage(Component.text(""));
            
            // Capability level
            source.sendMessage(Component.text("📊 Capability Level: ", NamedTextColor.AQUA, TextDecoration.BOLD)
                .append(Component.text(env.getCapabilityLevel().toString(), NamedTextColor.YELLOW)));
            source.sendMessage(Component.text("  " + env.getCapabilityLevel().getDescription(), NamedTextColor.GRAY));
            source.sendMessage(Component.text(""));
            
            // Capabilities
            source.sendMessage(Component.text("🔍 Available Features:", NamedTextColor.AQUA, TextDecoration.BOLD));
            source.sendMessage(createStatusLine("System CPU/RAM Access", env.hasSystemAccess()));
            source.sendMessage(createStatusLine("Disk Access", env.hasDiskAccess()));
            source.sendMessage(createStatusLine("Network Access", env.hasNetworkAccess()));
            source.sendMessage(Component.text(""));
            
            // Environment type
            source.sendMessage(Component.text("📦 Environment Type:", NamedTextColor.AQUA, TextDecoration.BOLD));
            source.sendMessage(Component.text("  Containerized: " + (env.isContainerized() ? "Yes" : "No"), 
                env.isContainerized() ? NamedTextColor.GREEN : NamedTextColor.GRAY));
            source.sendMessage(Component.text("  Shared Hosting: " + (env.isSharedHosting() ? "Likely" : "Unlikely"), 
                env.isSharedHosting() ? NamedTextColor.YELLOW : NamedTextColor.GRAY));
            source.sendMessage(Component.text(""));
            
            // Recommendations
            if (env.shouldUseJVMFallback()) {
                source.sendMessage(Component.text("💡 Recommendations:", NamedTextColor.AQUA, TextDecoration.BOLD));
                source.sendMessage(Component.text("  • Plugin will use JVM-only monitoring", NamedTextColor.YELLOW));
                source.sendMessage(Component.text("  • This is normal for shared hosting environments", NamedTextColor.YELLOW));
                source.sendMessage(Component.text("  • All features will work, but system metrics are limited", NamedTextColor.YELLOW));
            } else {
                source.sendMessage(Component.text("✅ Full monitoring capabilities available!", NamedTextColor.GREEN));
            }
            
        } catch (Exception e) {
            source.sendMessage(Component.text("❌ Failed to get environment info: " + e.getMessage(), NamedTextColor.RED));
            logger.error("Failed to get environment info", e);
        }
    }
    
    private void handleTestWebhook(CommandSource source) {
        source.sendMessage(Component.text("🧪 Testing Discord webhook...", NamedTextColor.YELLOW));
        
        try {
            AlertManager alertManager = plugin.getAlertManager();
            ConfigManager configManager = plugin.getConfigManager();
            
            if (alertManager == null || configManager == null) {
                source.sendMessage(Component.text("❌ AlertManager or ConfigManager not available", NamedTextColor.RED));
                return;
            }
            
            if (!configManager.isDiscordWebhookEnabled()) {
                source.sendMessage(Component.text("❌ Discord webhook is not enabled in configuration", NamedTextColor.RED));
                return;
            }
            
            if (configManager.getDiscordWebhookUrl().isEmpty()) {
                source.sendMessage(Component.text("❌ Discord webhook URL is not configured", NamedTextColor.RED));
                return;
            }
            
            // Send test alert
            CompletableFuture<Void> testFuture = alertManager.sendTestWebhook(
                source instanceof Player player ? player.getUsername() : "Console"
            );
            
            testFuture.thenRun(() -> {
                source.sendMessage(Component.text("✅ Test webhook sent successfully!", NamedTextColor.GREEN));
                logger.info("Test webhook sent by {}", 
                    source instanceof Player player ? player.getUsername() : "Console");
            }).exceptionally(throwable -> {
                source.sendMessage(Component.text("❌ Test webhook failed: " + throwable.getMessage(), NamedTextColor.RED));
                logger.warn("Test webhook failed", throwable);
                return null;
            });
            
        } catch (Exception e) {
            source.sendMessage(Component.text("❌ Test webhook failed: " + e.getMessage(), NamedTextColor.RED));
            logger.error("Test webhook failed", e);
        }
    }
    
    private void handleTestAlert(CommandSource source) {
        source.sendMessage(Component.text("🚨 Sending test alert...", NamedTextColor.YELLOW));
        
        try {
            AlertManager alertManager = plugin.getAlertManager();
            
            if (alertManager == null) {
                source.sendMessage(Component.text("❌ AlertManager not available", NamedTextColor.RED));
                return;
            }
            
            // Send test alert with fake high values
            SystemMonitor.MemoryUsage testMemUsage = new SystemMonitor.MemoryUsage(
                8L * 1024 * 1024 * 1024, // 8GB total
                7L * 1024 * 1024 * 1024, // 7GB used (87.5%)
                1L * 1024 * 1024 * 1024, // 1GB available
                87.5 // 87.5% usage
            );
            
            alertManager.sendTestAlert(testMemUsage, 0.85, // 85% CPU
                source instanceof Player player ? player.getUsername() : "Console");
            
            source.sendMessage(Component.text("✅ Test alert sent! Check your configured alert channels.", NamedTextColor.GREEN));
            logger.info("Test alert sent by {}", 
                source instanceof Player player ? player.getUsername() : "Console");
            
        } catch (Exception e) {
            source.sendMessage(Component.text("❌ Test alert failed: " + e.getMessage(), NamedTextColor.RED));
            logger.error("Test alert failed", e);
        }
    }
    
    private void handleStatus(CommandSource source) {
        try {
            ConfigManager configManager = plugin.getConfigManager();
            
            if (configManager == null) {
                source.sendMessage(Component.text("❌ ConfigManager not available", NamedTextColor.RED));
                return;
            }
            
            source.sendMessage(Component.text("📊 BubbleLog Status", NamedTextColor.GOLD, TextDecoration.BOLD));
            source.sendMessage(Component.text(""));
            
            // Monitoring status
            source.sendMessage(Component.text("🔍 Monitoring:", NamedTextColor.AQUA, TextDecoration.BOLD));
            source.sendMessage(createStatusLine("CPU", configManager.isCpuMonitoringEnabled()));
            source.sendMessage(createStatusLine("RAM", configManager.isRamMonitoringEnabled()));
            source.sendMessage(createStatusLine("Disk", configManager.isDiskMonitoringEnabled()));
            source.sendMessage(createStatusLine("Network", configManager.isNetworkMonitoringEnabled()));
            source.sendMessage(createStatusLine("JVM", configManager.isJvmMonitoringEnabled()));
            source.sendMessage(createStatusLine("Connection Quality", configManager.isConnectionQualityMonitoringEnabled()));
            
            source.sendMessage(Component.text(""));
            
            // Alert status
            source.sendMessage(Component.text("🚨 Alerts:", NamedTextColor.AQUA, TextDecoration.BOLD));
            source.sendMessage(createStatusLine("Performance Alerts", configManager.isPerformanceAlertsEnabled()));
            source.sendMessage(createStatusLine("Discord Webhook", configManager.isDiscordWebhookEnabled()));
            source.sendMessage(createStatusLine("Discord Status Reports", configManager.isDiscordStatusReportsEnabled()));
            source.sendMessage(createStatusLine("Slack Webhook", configManager.isSlackWebhookEnabled()));
            
            source.sendMessage(Component.text(""));
            
            // Configuration details
            source.sendMessage(Component.text("⚙️ Configuration:", NamedTextColor.AQUA, TextDecoration.BOLD));
            source.sendMessage(Component.text("  Monitoring Interval: " + configManager.getMonitoringInterval() + "s", NamedTextColor.GRAY));
            source.sendMessage(Component.text("  CPU Threshold: " + configManager.getCpuThreshold() + "%", NamedTextColor.GRAY));
            source.sendMessage(Component.text("  RAM Threshold: " + configManager.getRamThreshold() + "%", NamedTextColor.GRAY));
            source.sendMessage(Component.text("  Disk Threshold: " + configManager.getDiskThreshold() + "%", NamedTextColor.GRAY));
            source.sendMessage(Component.text("  Alert Cooldown: " + configManager.getAlertCooldown() + "s", NamedTextColor.GRAY));
            
            if (configManager.isDiscordStatusReportsEnabled()) {
                source.sendMessage(Component.text("  Status Report Interval: " + configManager.getDiscordStatusReportInterval() + "s", NamedTextColor.GRAY));
            }
            
        } catch (Exception e) {
            source.sendMessage(Component.text("❌ Failed to get status: " + e.getMessage(), NamedTextColor.RED));
            logger.error("Failed to get status", e);
        }
    }
    
    private void handleInfo(CommandSource source) {
        source.sendMessage(Component.text("📋 BubbleLog Information", NamedTextColor.GOLD, TextDecoration.BOLD));
        source.sendMessage(Component.text(""));
        source.sendMessage(Component.text("Version: 2.0.0", NamedTextColor.GREEN));
        source.sendMessage(Component.text("Author: BubbleCraft", NamedTextColor.GREEN));
        source.sendMessage(Component.text("Description: Advanced system monitoring for Velocity with hosting-friendly design", NamedTextColor.GRAY));
        source.sendMessage(Component.text(""));
        source.sendMessage(Component.text("Features:", NamedTextColor.AQUA, TextDecoration.BOLD));
        source.sendMessage(Component.text("  • CPU, RAM, and Disk monitoring", NamedTextColor.GRAY));
        source.sendMessage(Component.text("  • Network and Player tracking", NamedTextColor.GRAY));
        source.sendMessage(Component.text("  • JVM performance monitoring", NamedTextColor.GRAY));
        source.sendMessage(Component.text("  • Connection quality analysis", NamedTextColor.GRAY));
        source.sendMessage(Component.text("  • Discord webhook integration", NamedTextColor.GRAY));
        source.sendMessage(Component.text("  • Performance alerts and reports", NamedTextColor.GRAY));
        source.sendMessage(Component.text(""));
        source.sendMessage(Component.text("📚 Documentation: https://github.com/bubblecraft/bubblelog", NamedTextColor.BLUE));
    }
    
    private void sendHelp(CommandSource source) {
        source.sendMessage(Component.text("🎮 BubbleLog Commands", NamedTextColor.GOLD, TextDecoration.BOLD));
        source.sendMessage(Component.text(""));
        source.sendMessage(Component.text("/bubblelog reload", NamedTextColor.GREEN)
            .append(Component.text(" - Reload configuration", NamedTextColor.GRAY)));
        source.sendMessage(Component.text("/bubblelog validate", NamedTextColor.GREEN)
            .append(Component.text(" - Validate configuration", NamedTextColor.GRAY)));
        source.sendMessage(Component.text("/bubblelog test webhook", NamedTextColor.GREEN)
            .append(Component.text(" - Test Discord webhook", NamedTextColor.GRAY)));
        source.sendMessage(Component.text("/bubblelog test alert", NamedTextColor.GREEN)
            .append(Component.text(" - Send test alert", NamedTextColor.GRAY)));
        source.sendMessage(Component.text("/bubblelog status", NamedTextColor.GREEN)
            .append(Component.text(" - Show monitoring status", NamedTextColor.GRAY)));
        source.sendMessage(Component.text("/bubblelog env", NamedTextColor.GREEN)
            .append(Component.text(" - Show environment capabilities", NamedTextColor.GRAY)));
        source.sendMessage(Component.text("/bubblelog info", NamedTextColor.GREEN)
            .append(Component.text(" - Show plugin information", NamedTextColor.GRAY)));
        source.sendMessage(Component.text(""));
        source.sendMessage(Component.text("Permission: bubblelog.admin", NamedTextColor.YELLOW));
    }
    
    private void showMonitoringStatus(CommandSource source, ConfigManager config) {
        source.sendMessage(Component.text(""));
        source.sendMessage(Component.text("📊 Current Monitoring Status:", NamedTextColor.AQUA, TextDecoration.BOLD));
        
        List<String> enabled = new ArrayList<>();
        List<String> disabled = new ArrayList<>();
        
        if (config.isCpuMonitoringEnabled()) enabled.add("CPU"); else disabled.add("CPU");
        if (config.isRamMonitoringEnabled()) enabled.add("RAM"); else disabled.add("RAM");
        if (config.isDiskMonitoringEnabled()) enabled.add("Disk"); else disabled.add("Disk");
        if (config.isNetworkMonitoringEnabled()) enabled.add("Network"); else disabled.add("Network");
        if (config.isJvmMonitoringEnabled()) enabled.add("JVM"); else disabled.add("JVM");
        if (config.isConnectionQualityMonitoringEnabled()) enabled.add("Connection Quality"); else disabled.add("Connection Quality");
        
        if (!enabled.isEmpty()) {
            source.sendMessage(Component.text("  ✅ Enabled: " + String.join(", ", enabled), NamedTextColor.GREEN));
        }
        if (!disabled.isEmpty()) {
            source.sendMessage(Component.text("  ❌ Disabled: " + String.join(", ", disabled), NamedTextColor.RED));
        }
        
        source.sendMessage(Component.text("  ⏱️ Interval: " + config.getMonitoringInterval() + " seconds", NamedTextColor.GRAY));
    }
    
    private Component createStatusLine(String name, boolean enabled) {
        return Component.text("  " + (enabled ? "✅" : "❌") + " " + name, 
            enabled ? NamedTextColor.GREEN : NamedTextColor.RED);
    }
    
    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("bubblelog.admin");
    }
    
    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();
        
        if (args.length == 0) {
            return List.of("reload", "validate", "test", "status", "env", "info", "help");
        }
        
        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            return List.of("reload", "validate", "test", "status", "env", "info", "help").stream()
                .filter(cmd -> cmd.startsWith(partial))
                .toList();
        }
        
        if (args.length == 2 && args[0].equalsIgnoreCase("test")) {
            String partial = args[1].toLowerCase();
            return List.of("webhook", "alert").stream()
                .filter(cmd -> cmd.startsWith(partial))
                .toList();
        }
        
        return List.of();
    }
}
