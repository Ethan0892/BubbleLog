# Discord Webhook Integration Guide

This guide explains how to set up Discord webhook integration with BubbleLog for performance alerts and system status reports.

## 📋 Table of Contents

1. [Creating a Discord Webhook](#creating-a-discord-webhook)
2. [Configuring BubbleLog](#configuring-bubblelog)
3. [Alert Types](#alert-types)
4. [Status Reports](#status-reports)
5. [Troubleshooting](#troubleshooting)
6. [Security Best Practices](#security-best-practices)

## 🔗 Creating a Discord Webhook

### Step 1: Create or Choose a Discord Channel
1. Open Discord and navigate to your server
2. Either use an existing channel or create a new one (e.g., `#server-alerts`)
3. Right-click on the channel and select **"Edit Channel"**

### Step 2: Create the Webhook
1. In the channel settings, click on **"Integrations"** in the left sidebar
2. Click **"Create Webhook"** or **"View Webhooks"** → **"New Webhook"**
3. Give your webhook a name (e.g., "BubbleLog Monitor")
4. Optionally, upload an avatar for the webhook
5. Copy the **Webhook URL** - you'll need this for configuration

### Step 3: Test the Webhook (Optional)
You can test your webhook using curl or a tool like Postman:
```bash
curl -H "Content-Type: application/json" \
     -X POST \
     -d '{"content": "Test message from BubbleLog!"}' \
     YOUR_WEBHOOK_URL_HERE
```

## ⚙️ Configuring BubbleLog

### Basic Alert Configuration
Edit your `config.yml` file:

```yaml
alerts:
  # Enable performance alerts
  enabled: true
  thresholds:
    cpu: 80.0    # Alert when CPU > 80%
    ram: 85.0    # Alert when RAM > 85%
    disk: 90.0   # Alert when Disk > 90%
  
  # Discord webhook settings
  discord:
    enabled: true
    webhook-url: "https://discord.com/api/webhooks/YOUR_WEBHOOK_URL_HERE"
    status-reports:
      enabled: true
      interval: 3600  # Send status report every hour
```

### Advanced Configuration Options

```yaml
alerts:
  enabled: true
  cooldown: 300  # 5 minutes between same alert types
  console: true  # Also log to server console
  log-to-file: true  # Save alerts to file
  
  discord:
    enabled: true
    webhook-url: "https://discord.com/api/webhooks/YOUR_WEBHOOK_URL_HERE"
    status-reports:
      enabled: true
      interval: 1800  # 30 minutes for high-traffic servers
```

## 🚨 Alert Types

BubbleLog sends different types of alerts with distinct styling:

### ⚡ CPU High Usage Alert
- **Trigger**: CPU usage exceeds configured threshold
- **Color**: Orange (⚠️ Warning)
- **Contains**: Current CPU percentage, threshold value
- **Example**: "CPU usage is 85.23% (threshold: 80.0%)"

### 🧠 RAM High Usage Alert  
- **Trigger**: RAM usage exceeds configured threshold
- **Color**: Orange (⚠️ Warning)
- **Contains**: RAM percentage, used/total memory
- **Example**: "RAM usage is 87.45% (threshold: 85.0%) - 7.0GB/8.0GB"

### 💾 Disk High Usage Alert
- **Trigger**: Any disk exceeds configured threshold
- **Color**: Orange (⚠️ Warning)
- **Contains**: Disk name, usage percentage, available space
- **Example**: "Disk usage high on C:: 92.15% used"

### 🚨 System Critical Alert
- **Trigger**: Multiple systems exceed thresholds simultaneously
- **Color**: Red (🚨 Critical)
- **Contains**: Summary of all critical systems
- **Example**: "Multiple systems critical: CPU 95.2%, RAM 91.8%"

## 📊 Status Reports

When enabled, BubbleLog sends periodic status reports to Discord:

### Status Report Features
- **📈 Current Metrics**: CPU, RAM, Disk, Players
- **🏥 Health Assessment**: Overall system health rating
- **⏰ Timestamps**: Discord-formatted timestamps
- **🎨 Color Coding**: Visual health indicators
- **📊 Rich Formatting**: Professional embed styling

### Health Status Levels
- **✅ Healthy**: All systems operating normally
- **⚠️ Caution**: Systems at 80% of alert thresholds
- **🟠 Warning**: One or more systems exceed thresholds  
- **🔴 Critical**: Multiple critical systems

### Sample Status Report Content
```
✅ System Status Report

🖥️ CPU Usage: 45.32%
🧠 Memory Usage: 65.2% (5.2 GB / 8.0 GB)
👥 Players Online: 85 / 100 (85.0%)
💾 Disk Usage: 
**C:**: 68.5% (15.8 GB free)
**D:**: 45.2% (274.6 GB free)
🏥 Overall Health: ✅ Healthy
```

## 🔧 Troubleshooting

### Common Issues

#### Webhook Not Sending
```yaml
# Check your webhook URL format:
webhook-url: "https://discord.com/api/webhooks/123456789/abcdefghijklmnop"
```
- Ensure the URL starts with `https://discord.com/api/webhooks/`
- Verify the webhook hasn't been deleted in Discord
- Check server logs for error messages

#### 404 Webhook Not Found
- The webhook was deleted or the URL is incorrect
- Recreate the webhook and update the configuration

#### Rate Limiting (429 Error)
- Discord limits webhooks to 30 requests per minute
- Increase alert cooldown periods:
```yaml
alerts:
  cooldown: 600  # 10 minutes instead of 5
```

#### Network Timeouts
- Check your server's internet connection
- Verify firewall allows outbound HTTPS connections
- BubbleLog uses 10-second timeouts for webhook requests

### Debug Logging
Enable debug logging to troubleshoot webhook issues:
```yaml
logging:
  console: true  # Enable console logging
```
Check server logs for messages like:
- `Discord webhook sent successfully`
- `Discord webhook returned error status: 400`
- `Discord webhook timed out`

## 🔒 Security Best Practices

### Webhook URL Security
- **Never share** your webhook URL publicly
- **Regenerate** the webhook if accidentally exposed
- **Use environment variables** for production deployments
- **Restrict channel permissions** to prevent webhook abuse

### Server Permissions
- Create a dedicated channel for BubbleLog alerts
- Limit who can edit the webhook
- Consider using a bot account instead of webhooks for advanced features

### Rate Limiting
- Don't set alert cooldowns too low (minimum 60 seconds recommended)
- For busy servers, consider longer status report intervals
- Monitor Discord webhook usage to avoid rate limits

## 🎨 Customization

### Custom Alert Messages
The webhook messages are automatically formatted, but you can customize thresholds:

```yaml
alerts:
  thresholds:
    cpu: 70.0    # More sensitive CPU alerts
    ram: 90.0    # Less sensitive RAM alerts  
    disk: 95.0   # Very high disk threshold
```

### Status Report Frequency
Adjust based on your monitoring needs:
```yaml
discord:
  status-reports:
    interval: 900   # Every 15 minutes (high monitoring)
    interval: 7200  # Every 2 hours (low monitoring)
    interval: 86400 # Daily status reports
```

## 📱 Mobile Notifications

To receive alerts on mobile:
1. Enable Discord notifications for the alert channel
2. Configure Discord mobile app notification settings
3. Consider using @role mentions for critical alerts (future feature)

## 🔄 Integration with Other Tools

The webhook format is compatible with:
- **Discord bots** that can parse embed data
- **Automation tools** like Zapier or IFTTT
- **Log aggregation** services that support Discord webhooks
- **Monitoring dashboards** that can consume webhook data

## 📈 Monitoring Best Practices

### For Small Servers (< 50 players)
```yaml
alerts:
  cooldown: 300
discord:
  status-reports:
    interval: 3600  # Hourly reports
```

### For Large Servers (> 100 players)  
```yaml
alerts:
  cooldown: 600   # Longer cooldowns
discord:
  status-reports:
    interval: 1800  # More frequent monitoring
```

### For Production Networks
```yaml
alerts:
  cooldown: 180   # Quick response to issues
discord:
  status-reports:
    interval: 900   # High-frequency monitoring
```

Discord webhook integration provides real-time visibility into your Velocity server's performance, helping you maintain optimal server health and player experience.
