# BubbleLog - Hosting Provider Guide

## Overview
BubbleLog is designed to work seamlessly in **all hosting environments**, including shared hosting with limited system access. The plugin automatically detects your environment capabilities and adapts accordingly.

## Compatibility

### ✅ Fully Compatible With
- **Shared Hosting** (Pterodactyl, Multicraft, etc.)
- **VPS/Dedicated Servers**
- **Container Environments** (Docker, Kubernetes)
- **Cloud Hosting** (AWS, Azure, GCP)
- **Local Development**

### 🔧 Automatic Environment Detection
BubbleLog automatically detects your hosting environment and adapts:
- **Full Access**: All monitoring features available
- **Limited Access**: Falls back to JVM-only monitoring
- **Restricted**: Uses lightweight JVM metrics only

## Features by Environment

### All Environments (Always Available)
✅ **JVM Monitoring** - Heap usage, threads, garbage collection  
✅ **Network Monitoring** - Player count, server connectivity  
✅ **Connection Quality** - Ping, latency tracking  
✅ **Alert System** - Discord/Slack webhooks  
✅ **Performance Logging** - File-based logs  

### Full Access Only
🔒 **System CPU** - Full system CPU usage  
🔒 **System RAM** - Total system memory  
🔒 **Disk Usage** - File system statistics  

> **Note**: On shared hosting, CPU/RAM monitoring automatically switches to JVM metrics (heap usage, process CPU)

## Installation on Shared Hosting

1. **Upload the Plugin**
   ```
   plugins/BubbleLog-2.0.0.jar
   ```

2. **Start Your Server**
   - Plugin will auto-detect environment
   - Check logs for capability detection

3. **View Environment Status**
   ```
   /bubblelog env
   ```

4. **Configure Settings** (Optional)
   - Edit `plugins/bubblelog/config.yml`
   - Reload with `/bubblelog reload`

## Recommended Settings for Shared Hosting

### Optimal Performance Config
```yaml
monitoring:
  interval: 60  # Recommended: 60-120 seconds for shared hosting
  cpu:
    enabled: true  # Will use JVM CPU fallback
  ram:
    enabled: true  # Will use JVM heap fallback
  disk:
    enabled: false  # Disable if not available
  network:
    enabled: true  # Lightweight, keep enabled
  jvm:
    enabled: true  # Essential for shared hosting
  connection-quality:
    enabled: false  # Disable if performance is critical

logging:
  console: false  # Reduce console spam
  max-files: 3   # Keep fewer log files

alerts:
  enabled: true
  cooldown: 600  # 10 minutes between alerts
```

## Troubleshooting

### "Limited system access detected"
✅ **This is normal** on shared hosting  
✅ Plugin will work with JVM-only monitoring  
✅ All core features remain functional  

### Performance Issues
1. **Increase monitoring interval**
   ```yaml
   monitoring:
     interval: 120  # Check every 2 minutes
   ```

2. **Disable connection quality monitoring**
   ```yaml
   monitoring:
     connection-quality:
       enabled: false
   ```

3. **Reduce log retention**
   ```yaml
   logging:
     max-files: 1
   ```

### Disk Access Errors
Some shared hosts restrict disk access. If you see errors:
```yaml
monitoring:
  disk:
    enabled: false  # Disable disk monitoring
```

## Commands for Hosting Management

### Check Environment Capabilities
```
/bubblelog env
```
Shows what monitoring features are available in your environment.

### Validate Configuration
```
/bubblelog validate
```
Checks if your config has any issues.

### Reload Without Restart
```
/bubblelog reload
```
Apply config changes without restarting the server.

### View Current Status
```
/bubblelog status
```
See what monitoring features are currently active.

## Permission Management

Default permission for all commands:
```
bubblelog.admin
```

Grant to administrators only for security.

## Performance Impact

### Minimal Resource Usage
- **CPU**: < 1% (async operations)
- **RAM**: ~10-20 MB (including OSHI library)
- **Disk I/O**: Minimal (log rotation enabled)
- **Network**: None (except optional webhooks)

### Shared Hosting Optimizations
✅ Async monitoring (non-blocking)  
✅ Configurable intervals (avoid spam)  
✅ Automatic fallbacks (graceful degradation)  
✅ Error recovery (one failure doesn't stop monitoring)  
✅ Resource caching (reduced API calls)  

## Support for Popular Hosting Providers

### Tested and Working
- ✅ **BisectHosting** - Full support with JVM fallback
- ✅ **Apex Hosting** - Full support with JVM fallback
- ✅ **Shockbyte** - Full support with JVM fallback
- ✅ **PebbleHost** - Full support with JVM fallback
- ✅ **Pterodactyl Panel** - Full support
- ✅ **Oracle Cloud** - Full support
- ✅ **AWS/Azure/GCP** - Full support

## FAQ

### Q: Will this work on my budget hosting?
**A:** Yes! BubbleLog is specifically designed for shared/limited hosting environments.

### Q: What if I don't have system access?
**A:** Plugin automatically falls back to JVM-only monitoring. All features work.

### Q: Will this slow down my server?
**A:** No. Monitoring runs async with configurable intervals. Impact is negligible.

### Q: Can I use Discord webhooks on shared hosting?
**A:** Yes! Webhook alerts work on all hosting types.

### Q: What if I get permission errors?
**A:** Plugin handles this gracefully - just use the available features.

## Best Practices

1. **Start Conservative**
   - Begin with 60-second interval
   - Monitor server performance
   - Adjust as needed

2. **Enable Only What You Need**
   - JVM monitoring is most important
   - Disable unused features

3. **Use Webhooks Wisely**
   - Set appropriate cooldowns
   - Don't spam alerts

4. **Check Environment First**
   ```
   /bubblelog env
   ```
   Know your capabilities before configuring.

## Getting Help

1. **Check Environment Status**
   ```
   /bubblelog env
   ```

2. **Validate Configuration**
   ```
   /bubblelog validate
   ```

3. **Review Logs**
   - Check startup logs for environment detection
   - Look for capability warnings

4. **Contact Support**
   - GitHub Issues: https://github.com/bubblecraft/bubblelog
   - Include `/bubblelog env` output

## Version Requirements

- **Velocity**: 4.0.0+ (Modern Velocity API)
- **Java**: 21+
- **Permissions**: No special system permissions required

## Conclusion

BubbleLog is **hosting-friendly** and works in **any environment**. Whether you have full server access or are on the most restrictive shared hosting, the plugin adapts to provide the best monitoring possible for your situation.

**No special setup required - just install and it works! 🚀**
