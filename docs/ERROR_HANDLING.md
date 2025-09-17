# BubbleLog - Error Handling & Resilience

## Overview

BubbleLog is designed with comprehensive error handling to ensure that any issues with system monitoring **never** affect server performance or functionality. The plugin implements multiple layers of error protection.

## Error Handling Strategy

### 1. **Graceful Degradation**
- If one monitoring component fails, others continue working
- Invalid data is replaced with safe defaults (0 values, "N/A" strings)
- Monitoring continues even if alerts fail

### 2. **Isolated Error Handling**
- Each monitoring function (CPU, RAM, Disk) has individual try-catch blocks
- Alert checking is separated from logging
- Webhook failures don't affect local logging

### 3. **Non-Blocking Operations**
- All file I/O operations are wrapped in error handlers
- Webhook calls are asynchronous and won't block monitoring
- Configuration errors fall back to safe defaults

## Error Scenarios & Responses

### System Monitoring Errors

| Error Type | Response | Impact |
|------------|----------|---------|
| **CPU monitoring fails** | Logs "CPU: N/A", continues with RAM/Disk | None - other monitoring continues |
| **RAM monitoring fails** | Logs "RAM: N/A", continues with CPU/Disk | None - other monitoring continues |
| **Disk monitoring fails** | Logs "Disk: N/A", continues with CPU/RAM | None - other monitoring continues |
| **All monitoring fails** | Logs error, continues attempting next cycle | None - server unaffected |

### Configuration Errors

| Error Type | Response | Impact |
|------------|----------|---------|
| **Config file missing** | Creates default config automatically | None - uses safe defaults |
| **Invalid config values** | Uses built-in defaults | None - monitoring continues |
| **Corrupted config** | Falls back to defaults, logs warning | None - safe fallback |

### File System Errors

| Error Type | Response | Impact |
|------------|----------|---------|
| **Cannot create log directory** | Logs warning, continues monitoring | Monitoring data not saved to file |
| **Cannot write log file** | Logs warning, continues monitoring | Console logging still works |
| **Disk full** | Logs warning, continues monitoring | Monitoring continues |

### Alert System Errors

| Error Type | Response | Impact |
|------------|----------|---------|
| **Alert checking fails** | Logs debug message, continues monitoring | Monitoring unaffected |
| **Discord webhook fails** | Silent failure, continues other alerts | Other alert methods work |
| **Slack webhook fails** | Silent failure, continues other alerts | Other alert methods work |
| **Alert cooldown errors** | Continues without cooldown | May get duplicate alerts |

## Data Validation

### CPU Usage
- Validates against NaN, Infinite, negative values
- Caps maximum at 100%
- Returns 0% on any error

### Memory Usage
- Validates total > 0, available >= 0, used >= 0
- Percentage capped at 100%
- Returns zero values on any error

### Disk Usage
- Validates total > 0, free >= 0, used >= 0
- Filters out invalid disk stores
- Returns empty list on complete failure

## Plugin Lifecycle Protection

### Startup
- Configuration loading errors don't prevent startup
- Component initialization failures are isolated
- Monitoring starts even if alerts fail to initialize

### Runtime
- Individual monitoring cycle errors don't stop the scheduler
- Task continues running even if exceptions occur
- Error logging is minimal to avoid log spam

### Shutdown
- Each component shutdown is individually protected
- Shutdown errors don't affect server shutdown
- Graceful cleanup even if components are in error state

## Performance Considerations

### Error Logging Levels
- **DEBUG**: Minor data validation issues
- **WARN**: Non-critical functionality failures (file writes, webhooks)
- **ERROR**: Critical initialization failures only

### Resource Protection
- No infinite loops or retry mechanisms
- Failed operations return immediately
- Memory usage is bounded (no accumulating errors)

## Monitoring Self-Health

The plugin monitors its own health:
- Tracks initialization success/failure
- Validates data before logging
- Reports component status during startup

## Recovery Mechanisms

### Automatic Recovery
- Next monitoring cycle attempts fresh data collection
- No persistent error states
- Configuration reloading on demand

### Manual Recovery
- Plugin restart will reset all error states
- Configuration file regeneration available
- No manual intervention typically required

## Best Practices Implemented

1. **Fail Fast**: Invalid configurations are detected early
2. **Fail Safe**: Errors never propagate to affect server
3. **Fail Silent**: Debug-level logging for expected failures
4. **Fail Isolated**: Component failures don't cascade
5. **Fail Transparent**: Server admins see normal operation

## Error Prevention

- Input validation on all external data
- Null checks on all objects
- Range validation on numeric values
- Safe defaults for all configurations
- Defensive programming throughout

This comprehensive error handling ensures BubbleLog enhances your server monitoring without ever risking server stability or performance.
