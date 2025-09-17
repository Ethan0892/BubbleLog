# Sample Log Output

This document shows examples of BubbleLog's monitoring output with all features enabled.

## Basic System Monitoring
```
[2025-07-10 14:30:00] CPU: 45.32% | RAM: 2.1GB/8.0GB (26.25%) | Disk(C:): 15.2GB/50.0GB (30.40%)
[2025-07-10 14:30:30] CPU: 38.75% | RAM: 2.3GB/8.0GB (28.75%) | Disk(C:): 15.2GB/50.0GB (30.40%)
[2025-07-10 14:31:00] CPU: 42.18% | RAM: 2.2GB/8.0GB (27.50%) | Disk(C:): 15.2GB/50.0GB (30.40%)
```

## With Network & Player Monitoring
```
[2025-07-10 14:30:00] CPU: 45.32% | RAM: 2.1GB/8.0GB (26.25%) | Disk(C:): 15.2GB/50.0GB (30.40%) | Players: 85/100 (85.0%), Servers: 3/4
[2025-07-10 14:30:30] CPU: 38.75% | RAM: 2.3GB/8.0GB (28.75%) | Disk(C:): 15.2GB/50.0GB (30.40%) | Players: 87/100 (87.0%), Servers: 4/4
[2025-07-10 14:31:00] CPU: 42.18% | RAM: 2.2GB/8.0GB (27.50%) | Disk(C:): 15.2GB/50.0GB (30.40%) | Players: 82/100 (82.0%), Servers: 4/4
```

## With JVM Monitoring
```
[2025-07-10 14:30:00] CPU: 45.32% | RAM: 2.1GB/8.0GB (26.25%) | Disk(C:): 15.2GB/50.0GB (30.40%) | Players: 85/100 (85.0%), Servers: 3/4 | JVM: Heap 68.5%, NonHeap: 125.3 MB, Threads: 42, GC: 2,451ms, Classes: 8,234
[2025-07-10 14:30:30] CPU: 38.75% | RAM: 2.3GB/8.0GB (28.75%) | Disk(C:): 15.2GB/50.0GB (30.40%) | Players: 87/100 (87.0%), Servers: 4/4 | JVM: Heap 71.2%, NonHeap: 126.1 MB, Threads: 45, GC: 2,467ms, Classes: 8,234
[2025-07-10 14:31:00] CPU: 42.18% | RAM: 2.2GB/8.0GB (27.50%) | Disk(C:): 15.2GB/50.0GB (30.40%) | Players: 82/100 (82.0%), Servers: 4/4 | JVM: Heap 65.8%, NonHeap: 127.2 MB, Threads: 43, GC: 2,489ms, Classes: 8,235
```

## Full Monitoring (All Features Enabled)
```
[2025-07-10 14:30:00] CPU: 45.32% | RAM: 2.1GB/8.0GB (26.25%) | Disk(C:): 15.2GB/50.0GB (30.40%) | Players: 85/100 (85.0%), Servers: 3/4 | JVM: Heap 68.5%, NonHeap: 125.3 MB, Threads: 42, GC: 2,451ms, Classes: 8,234 | Connection: Avg Ping 25.3ms, Max Ping: 45.2ms, Quality: Excellent, Loss: 0.00%
[2025-07-10 14:30:30] CPU: 38.75% | RAM: 2.3GB/8.0GB (28.75%) | Disk(C:): 15.2GB/50.0GB (30.40%) | Players: 87/100 (87.0%), Servers: 4/4 | JVM: Heap 71.2%, NonHeap: 126.1 MB, Threads: 45, GC: 2,467ms, Classes: 8,234 | Connection: Avg Ping 23.7ms, Max Ping: 41.8ms, Quality: Excellent, Loss: 0.00%
[2025-07-10 14:31:00] CPU: 42.18% | RAM: 2.2GB/8.0GB (27.50%) | Disk(C:): 15.2GB/50.0GB (30.40%) | Players: 82/100 (82.0%), Servers: 4/4 | JVM: Heap 65.8%, NonHeap: 127.2 MB, Threads: 43, GC: 2,489ms, Classes: 8,235 | Connection: Avg Ping 31.2ms, Max Ping: 67.4ms, Quality: Good, Loss: 0.00%
```

## Performance Alert Examples
```
[2025-07-10 14:35:15] CPU: 85.67% | RAM: 6.8GB/8.0GB (85.00%) | Disk(C:): 15.2GB/50.0GB (30.40%) | Players: 95/100 (95.0%), Servers: 4/4 | JVM: Heap 89.3%, NonHeap: 142.7 MB, Threads: 67, GC: 3,821ms, Classes: 8,241 | Connection: Avg Ping 45.6ms, Max Ping: 89.3ms, Quality: Good, Loss: 1.25%
[ALERT] CPU usage high: 85.67% (threshold: 80.00%)
[ALERT] RAM usage high: 85.00% (threshold: 85.00%)
[ALERT] JVM heap usage critical: 89.3%
```

## Error Handling Examples
```
[2025-07-10 14:30:00] CPU: 45.32% | RAM: 2.1GB/8.0GB (26.25%) | Disk: N/A | Players: 85/100 (85.0%), Servers: 3/4 | JVM: N/A | Connection: N/A
[2025-07-10 14:30:30] CPU: N/A | RAM: N/A | Disk(C:): 15.2GB/50.0GB (30.40%) | Players: N/A | JVM: Heap 71.2%, NonHeap: 126.1 MB, Threads: 45, GC: 2,467ms, Classes: 8,234 | Connection: Avg Ping 23.7ms, Max Ping: 41.8ms, Quality: Excellent
```

## Connection Quality Ratings

| Average Ping | Quality Rating | Description |
|-------------|---------------|-------------|
| < 50ms | Excellent | Optimal for gaming |
| 50-100ms | Good | Acceptable for most players |
| 100-200ms | Fair | Noticeable lag for competitive play |
| > 200ms | Poor | Significant lag issues |

## JVM Metrics Explanation

- **Heap**: Percentage of maximum heap memory in use
- **NonHeap**: Non-heap memory usage in MB (method area, code cache, etc.)
- **Threads**: Current number of active threads
- **GC**: Total garbage collection time since server start
- **Classes**: Number of currently loaded classes

## Log File Rotation

BubbleLog automatically creates daily log files:
```
logs/
├── system-usage-2025-07-10.log
├── system-usage-2025-07-09.log
├── system-usage-2025-07-08.log
└── ...
```

Old files are automatically deleted based on the `max-files` configuration.
