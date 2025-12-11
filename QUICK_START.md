# 🚀 Quick Start - Shared Hosting

> **TL;DR**: Just install the plugin. It automatically detects and adapts to your environment!

## Installation (3 Steps)

### 1. Download & Upload
- Get `BubbleLog-2.0.0.jar`
- Upload to `plugins/` folder

### 2. Start Server
- Plugin auto-configures
- No setup needed!

### 3. Verify (Optional)
```
/bubblelog env
```

**That's it!** ✅

## What You'll See

### On Shared Hosting
```
[BubbleLog] === Hosting Environment Detection ===
[BubbleLog] Capability Level: LIMITED - JVM-based monitoring
[BubbleLog] System Access: ✗
[BubbleLog] Disk Access: ✗
[BubbleLog] JVM Metrics: ✓
[BubbleLog] Running in restricted environment - using JVM-only mode
[BubbleLog] This is normal for shared hosting!
```

### On VPS/Dedicated
```
[BubbleLog] === Hosting Environment Detection ===
[BubbleLog] Capability Level: FULL - All features available
[BubbleLog] System Access: ✓
[BubbleLog] Disk Access: ✓
[BubbleLog] Full monitoring capabilities available!
```

## Common Questions

### Q: "Limited access detected" - is this bad?
**A:** No! It's completely normal and expected on shared hosting. Plugin still works perfectly.

### Q: Do I need to configure anything?
**A:** Nope! Default config works great. Customize only if you want to.

### Q: Will this slow my server?
**A:** No. Runs async with < 1% CPU impact.

### Q: Where are the logs?
**A:** `plugins/bubblelog/logs/system-usage-YYYY-MM-DD.log`

### Q: Can I use Discord webhooks?
**A:** Yes! Works on all hosting types.

## Optional Tweaks

### Reduce Resource Usage
```yaml
monitoring:
  interval: 120  # Check every 2 minutes
  connection-quality:
    enabled: false
```

### Enable Discord Alerts
```yaml
alerts:
  discord:
    enabled: true
    webhook-url: "https://discord.com/api/webhooks/YOUR_WEBHOOK"
```

## Commands Cheat Sheet

```bash
/bubblelog env       # Check what features you have
/bubblelog status    # See current monitoring
/bubblelog reload    # Reload config (no restart!)
/bubblelog validate  # Check config is valid
```

## Troubleshooting

### Plugin not working?
1. Check Java version: `java -version` (need 21+)
2. Check Velocity version: `velocity version` (need 4.0.0+)
3. Check logs for errors

### Want more features?
Your host may be very restricted. This is normal!
- JVM monitoring always works
- Network monitoring always works
- System metrics may be limited

### Still need help?
1. Run `/bubblelog env` 
2. Run `/bubblelog validate`
3. Check `logs/latest.log`
4. Open GitHub issue with output

## That's All!

BubbleLog is designed to **just work** on shared hosting.

No complex setup. No special permissions. No headaches.

Install → Start → Done! 🎉

---

**Pro Tip**: Run `/bubblelog env` to see exactly what your hosting supports!
