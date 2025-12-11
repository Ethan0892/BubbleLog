# BubbleLog

> **Modern, Hosting-Friendly System Monitoring for Velocity**

Advanced Velocity monitoring plugin that **automatically adapts to any hosting environment** - from budget shared hosting to dedicated servers.

## 🌟 Key Features

- ✅ **Works on ANY hosting** - Shared, VPS, dedicated, or cloud
- 🎯 **Auto-detects environment** - No configuration needed
- 🔄 **Smart fallbacks** - JVM monitoring when system access is limited
- ⚡ **Async operations** - Zero performance impact
- 📊 **Comprehensive monitoring** - CPU, RAM, Disk, Network, JVM metrics
- 🔔 **Discord/Slack alerts** - Real-time notifications
- 🛠️ **Hot-reload config** - No restart required

## 📋 Requirements

- **Velocity**: 3.4.0+
- **Java**: 21+

## 🚀 Quick Start

1. Download `BubbleLog-2.0.0.jar`
2. Place in `plugins/` folder
3. Start server - **that's it!**

### First Commands
```bash
/bubblelog env       # Check environment capabilities
/bubblelog status    # View monitoring status
/bubblelog reload    # Reload configuration
```

## ⚙️ Configuration

Default config at `plugins/bubblelog/config.yml`:

```yaml
monitoring:
  interval: 30       # Seconds between checks
  cpu: { enabled: true }
  ram: { enabled: true }
  disk: { enabled: true }
  network: { enabled: true }
  jvm: { enabled: true }

alerts:
  enabled: true
  thresholds:
    cpu: 80.0        # Alert at 80% CPU
    ram: 85.0        # Alert at 85% RAM
    disk: 90.0       # Alert at 90% disk
  discord:
    enabled: false
    webhook-url: ""
```

### Shared Hosting Optimization
```yaml
monitoring:
  interval: 60       # Higher interval for shared hosting
  jvm: { enabled: true }    # Always keep enabled
```

## 📊 What Gets Monitored

| Metric | Full Access | Shared Hosting |
|--------|------------|----------------|
| CPU | System CPU | JVM Process CPU |
| RAM | System Memory | JVM Heap |
| Disk | All Disks | Skipped |
| Network | Player stats | Player stats |
| JVM | Full metrics | Full metrics |

## 🎮 Commands

| Command | Description |
|---------|-------------|
| `/bubblelog env` | Show environment capabilities |
| `/bubblelog status` | View monitoring status |
| `/bubblelog reload` | Hot-reload configuration |
| `/bubblelog validate` | Check config validity |
| `/bubblelog test webhook` | Test Discord webhook |

**Permission**: `bubblelog.admin`

## 📚 Documentation

- **[HOSTING_GUIDE.md](docs/HOSTING_GUIDE.md)** - Complete guide for shared hosting
- **[QUICK_START.md](QUICK_START.md)** - 3-step installation
- **[UPGRADE_TO_2.0.md](UPGRADE_TO_2.0.md)** - Changelog and migration

## 🔧 Troubleshooting

### "Limited system access detected"
✅ **Normal on shared hosting!** Plugin works with JVM-only monitoring.

### Performance Issues
Increase monitoring interval:
```yaml
monitoring:
  interval: 120  # Check every 2 minutes
```

### Discord Webhooks
1. Create webhook in Discord server settings
2. Copy webhook URL
3. Add to config:
```yaml
alerts:
  discord:
    enabled: true
    webhook-url: "https://discord.com/api/webhooks/..."
```

## 🏗️ Building

```bash
.\gradlew.bat shadowJar    # Windows
./gradlew shadowJar        # Linux/Mac
```

Output: `build/libs/BubbleLog-2.0.0.jar`

## 📝 License

MIT License - See LICENSE file

## 💬 Support

- **GitHub Issues**: For bug reports
- **Commands**: `/bubblelog env` to check your setup

---

**Made for server administrators running Velocity on any hosting! 🚀**


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
