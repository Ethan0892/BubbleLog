# BubbleLog - Quick Start Guide

## Installation

1. Copy `BubbleLog-1.0.0.jar` to your Velocity `plugins` folder
2. Start your Velocity proxy
3. The plugin will automatically create a configuration file at `plugins/bubblelog/config.yml`
4. Logs will be saved to `plugins/bubblelog/logs/system-usage-YYYY-MM-DD.log`

## Configuration

Edit `plugins/bubblelog/config.yml` to customize:

### Monitoring Settings
- **monitoring.interval**: How often to check system usage (seconds)
- **monitoring.cpu.enabled**: Enable/disable CPU monitoring
- **monitoring.ram.enabled**: Enable/disable RAM monitoring  
- **monitoring.disk.enabled**: Enable/disable disk monitoring

### Logging Settings
- **logging.console**: Set to `true` to also log to server console
- **logging.max-files**: Number of log files to keep (automatic cleanup)

### Performance Alerts
- **alerts.enabled**: Enable/disable performance alerts
- **alerts.thresholds.cpu**: CPU usage threshold (default: 80%)
- **alerts.thresholds.ram**: RAM usage threshold (default: 85%)
- **alerts.thresholds.disk**: Disk usage threshold (default: 90%)
- **alerts.console**: Show alerts in server console
- **alerts.log-to-file**: Save alerts to alerts.log
- **alerts.cooldown**: Seconds between duplicate alerts (default: 300)

### Webhook Notifications
- **alerts.discord.enabled**: Enable Discord webhook alerts
- **alerts.discord.webhook-url**: Your Discord webhook URL
- **alerts.slack.enabled**: Enable Slack webhook alerts
- **alerts.slack.webhook-url**: Your Slack webhook URL

## Log Format

Each log entry contains:
```
[2025-07-10 14:30:00] CPU: 45.67% | RAM: 2.34GB/8.00GB (29.25%) | Disk(C:): 120.5GB/500.0GB (24.10%)
```

## Performance

- Very low CPU/memory overhead
- Uses OSHI library for efficient system monitoring
- Configurable monitoring intervals to balance detail vs performance
- Automatic log rotation to prevent disk space issues

## Troubleshooting

- If logs aren't being created, check file permissions on the plugins folder
- If CPU usage shows 0%, try increasing the monitoring interval (some systems need time between readings)
- For high-frequency monitoring (< 10 seconds), consider the performance impact

## Alert Examples

### Console Alert
```
🚨 PERFORMANCE ALERT: CPU usage is 87.45% (threshold: 80.0%)
```

### Discord Webhook
Rich embeds with:
- ⚡ High CPU Usage  
- 🧠 High RAM Usage
- 💾 High Disk Usage
- 🚨 Critical System State

### Alert Log File
```
[2025-07-10 14:30:00] ALERT - High CPU Usage: CPU usage is 87.45% (threshold: 80.0%)
[2025-07-10 14:32:15] ALERT - Critical System State: Multiple system resources are under stress! CPU: 87.45%, RAM: 88.32%, Disks with issues: 1
```
