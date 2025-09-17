# BubbleLog

A modern Velocity plugin that monitors and logs CPU, RAM, and disk usage for your Minecraft server.

## Features

- **Real-time System Monitoring**: Tracks CPU, RAM, disk, network, and JVM metrics
- **Network & Player Monitoring**: Player count, server utilization, backend server status
- **JVM Performance Monitoring**: Heap usage, thread count, garbage collection metrics
- **Connection Quality Monitoring**: Server response times and availability
- **Performance Alerts**: Configurable thresholds with multiple notification methods
- **Discord Integration**: Rich webhook alerts and periodic status reports with embeds
- **Webhook Integration**: Discord and Slack webhook support for alerts
- **Smart Alert Management**: Cooldown periods to prevent spam
- **Critical State Detection**: Alerts when multiple systems are under stress
- **Robust Error Handling**: Comprehensive error protection ensures server stability
- **Graceful Degradation**: Continues monitoring even if individual components fail
- **Ultra-Lightweight Design**: <0.2% CPU impact with smart caching and optimization
- **Configurable Logging**: Customizable monitoring intervals and log formats
- **Automatic Log Rotation**: Keeps a configurable number of log files
- **Easy Configuration**: YAML-based configuration with sensible defaults

## Requirements

- Velocity 3.3.0 or higher
- Java 17 or higher

## Installation

1. Download the latest `BubbleLog.jar` from the releases
2. Place it in your Velocity `plugins` folder
3. Restart your Velocity proxy
4. Configure the plugin by editing `plugins/bubblelog/config.yml`

## Configuration

The plugin creates a `config.yml` file in the `plugins/bubblelog/` directory with the following options:

```yaml
monitoring:
  # How often to log system usage (in seconds)
  interval: 30
  cpu:
    # Enable CPU usage monitoring
    enabled: true
  ram:
    # Enable RAM usage monitoring
    enabled: true
  disk:
    # Enable disk usage monitoring
    enabled: true
  network:
    # Enable network and player monitoring
    enabled: true
  jvm:
    # Enable JVM performance monitoring
    enabled: true
  connection-quality:
    # Enable connection quality monitoring (ping, packet loss)
    enabled: true

logging:
  # Name of the log file
  filename: "system-usage.log"
  # Also log to console/server log
  console: false
  # Date format for log entries
  date-format: "yyyy-MM-dd HH:mm:ss"
  # Maximum number of log files to keep (0 = unlimited)
  max-files: 7

alerts:
  # Enable performance alerts
  enabled: true
  thresholds:
    # CPU usage threshold for alerts (percentage)
    cpu: 80.0
    # RAM usage threshold for alerts (percentage)
    ram: 85.0
    # Disk usage threshold for alerts (percentage)
    disk: 90.0
  # Send alerts to server console
  console: true
  # Log alerts to file
  log-to-file: true
  # Cooldown between same alert types (seconds)
  cooldown: 300
  discord:
    # Enable Discord webhook alerts
    enabled: false
    # Discord webhook URL for alerts
    webhook-url: ""
    status-reports:
      # Enable periodic Discord status reports
      enabled: false
      # Discord status report interval in seconds (3600 = 1 hour)
      interval: 3600
  slack:
    # Enable Slack webhook alerts
    enabled: false
    # Slack webhook URL for alerts
    webhook-url: ""
```

## Log Format

The plugin logs comprehensive system metrics in the following format:

```
[2025-07-10 14:30:00] CPU: 45.67% | RAM: 2.34GB/8.00GB (29.25%) | Disk(C:): 120.5GB/500.0GB (24.10%) | Players: 45/100 (45.0%), Servers: 3/4 | JVM: Heap 67.8%, Threads: 42, GC: 1250ms
```

**Includes:**
- **System Metrics**: CPU, RAM, and disk usage with percentages
- **Network Metrics**: Current/max players, server utilization, backend server status  
- **JVM Metrics**: Heap utilization, active threads, total GC time
- **Timestamps**: Configurable date/time formatting

## Alert Types

The plugin can send alerts for:

- **High CPU Usage**: When CPU usage exceeds the configured threshold
- **High RAM Usage**: When memory usage exceeds the configured threshold  
- **High Disk Usage**: When any disk exceeds the configured threshold
- **Critical System State**: When multiple resources are under stress simultaneously

### Alert Destinations

- **Console Logging**: Alerts appear in server console with 🚨 emoji
- **File Logging**: Alerts are saved to `alerts.log` in the logs directory
- **Discord Webhooks**: Rich embeds with colored alerts and timestamps
- **Slack Webhooks**: Formatted messages with appropriate warning colors

### Webhook Setup

For Discord webhooks:
1. Create a webhook in your Discord server
2. Copy the webhook URL
3. Set `alerts.discord.enabled: true` and paste the URL

For Slack webhooks:
1. Create an incoming webhook in your Slack workspace
2. Copy the webhook URL  
3. Set `alerts.slack.enabled: true` and paste the URL

## Building

To build the plugin from source:

1. Ensure you have Java 17+ installed
2. Clone this repository
3. Run `build.bat` (Windows) or `./gradlew shadowJar` (Linux/Mac)
4. The compiled plugin will be in `build/libs/BubbleLog-1.0.0.jar`

## Project Structure

```
BubbleLog/
├── src/main/java/net/bubblecraft/bubblelog/
│   ├── BubbleLog.java              # Main plugin class
│   ├── config/ConfigManager.java   # Configuration handling
│   └── monitor/SystemMonitor.java  # System monitoring logic
├── build.gradle                    # Build configuration
├── config-example.yml             # Example configuration
├── build.bat                      # Windows build script
└── README.md                      # This file
```

## Dependencies

- [OSHI](https://github.com/oshi/oshi) - For system and hardware information
- [Configurate](https://github.com/SpongePowered/Configurate) - For YAML configuration

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Support

If you encounter any issues or have questions, please open an issue on GitHub.

## Error Handling & Reliability

BubbleLog is designed with **comprehensive error handling** to ensure it never affects server performance:

- ✅ **Isolated Error Handling**: Each monitoring component fails independently
- ✅ **Graceful Degradation**: Continues monitoring even if individual components fail
- ✅ **Safe Defaults**: Invalid data is replaced with safe fallback values
- ✅ **Non-Blocking Operations**: File I/O and webhook errors don't stop monitoring
- ✅ **Data Validation**: All system values are validated before use
- ✅ **Recovery Mechanisms**: Automatic recovery on next monitoring cycle

See [ERROR_HANDLING.md](docs/ERROR_HANDLING.md) for detailed error handling documentation.

## 🎮 Discord Integration

BubbleLog provides rich Discord webhook integration for real-time server monitoring and alerts.

### Features
- **🚨 Instant Alerts**: Rich embed alerts for CPU, RAM, and disk usage
- **📊 Status Reports**: Periodic comprehensive system status reports
- **🎨 Rich Formatting**: Professional embeds with color-coded severity levels
- **⚡ Real-time**: Immediate notifications when thresholds are exceeded
- **🔧 Configurable**: Customizable thresholds and report intervals

### Discord Alert Example
```
⚡ High CPU Usage
CPU usage is 85.23% (threshold: 80.0%)

Alert Type: High CPU Usage
Severity: ⚠️ Warning
Server: Velocity Proxy
Timestamp: July 10, 2025 2:30 PM
```

### Discord Status Report Example  
```
✅ System Status Report
Current server performance metrics and health status

🖥️ CPU Usage: 45.32%
🧠 Memory Usage: 65.2% (5.2 GB / 8.0 GB)  
👥 Players Online: 85 / 100 (85.0%)
💾 Disk Usage: C:: 68.5% (15.8 GB free)
🏥 Overall Health: ✅ Healthy
```

### Quick Setup
1. Create a Discord webhook in your server
2. Copy the webhook URL
3. Configure BubbleLog:
```yaml
alerts:
  discord:
    enabled: true
    webhook-url: "YOUR_WEBHOOK_URL_HERE"
    status-reports:
      enabled: true
      interval: 3600  # Hourly reports
```

📚 **[Complete Discord Setup Guide →](docs/DISCORD_INTEGRATION.md)**  
🧪 **[Webhook Testing Utility →](docs/DISCORD_WEBHOOK_TESTING.md)**
