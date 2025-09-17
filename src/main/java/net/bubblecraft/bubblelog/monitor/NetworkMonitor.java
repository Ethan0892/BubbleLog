package net.bubblecraft.bubblelog.monitor;

import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.bubblecraft.bubblelog.config.ConfigManager;
import org.slf4j.Logger;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Monitors network and server connectivity metrics for Velocity proxy
 */
public class NetworkMonitor {
    
    private final ProxyServer server;
    private final Logger logger;
    private final ConfigManager config;
    
    // Track server response times
    private final Map<String, Long> lastPingTimes = new HashMap<>();
    private final Map<String, Boolean> serverStatus = new HashMap<>();
    
    public NetworkMonitor(ProxyServer server, Logger logger, ConfigManager config) {
        this.server = server;
        this.logger = logger;
        this.config = config;
    }
    
    public NetworkUsage getNetworkUsage() {
        try {
            int totalPlayers = server.getPlayerCount();
            int maxPlayers = server.getConfiguration().getShowMaxPlayers();
            
            // Get backend server statuses
            Map<String, ServerInfo> backendServers = new HashMap<>();
            for (RegisteredServer registeredServer : server.getAllServers()) {
                String serverName = registeredServer.getServerInfo().getName();
                
                // Ping the server asynchronously
                Instant startTime = Instant.now();
                CompletableFuture<Boolean> pingFuture = registeredServer.ping()
                    .thenApply(ping -> {
                        long responseTime = Duration.between(startTime, Instant.now()).toMillis();
                        lastPingTimes.put(serverName, responseTime);
                        serverStatus.put(serverName, true);
                        return true;
                    })
                    .exceptionally(throwable -> {
                        serverStatus.put(serverName, false);
                        lastPingTimes.put(serverName, -1L);
                        return false;
                    });
                
                // Don't wait for ping to complete, use cached data
                boolean isOnline = serverStatus.getOrDefault(serverName, false);
                long pingTime = lastPingTimes.getOrDefault(serverName, -1L);
                int playersOnServer = registeredServer.getPlayersConnected().size();
                
                backendServers.put(serverName, new ServerInfo(serverName, isOnline, pingTime, playersOnServer));
            }
            
            double serverUtilization = maxPlayers > 0 ? (double) totalPlayers / maxPlayers * 100 : 0.0;
            
            return new NetworkUsage(totalPlayers, maxPlayers, serverUtilization, backendServers);
            
        } catch (Exception e) {
            logger.debug("Error getting network usage", e);
            return new NetworkUsage(0, 0, 0.0, new HashMap<>());
        }
    }
    
    // Data classes
    public static class NetworkUsage {
        private final int currentPlayers;
        private final int maxPlayers;
        private final double serverUtilization;
        private final Map<String, ServerInfo> backendServers;
        
        public NetworkUsage(int currentPlayers, int maxPlayers, double serverUtilization, 
                           Map<String, ServerInfo> backendServers) {
            this.currentPlayers = currentPlayers;
            this.maxPlayers = maxPlayers;
            this.serverUtilization = serverUtilization;
            this.backendServers = backendServers;
        }
        
        public int getCurrentPlayers() { return currentPlayers; }
        public int getMaxPlayers() { return maxPlayers; }
        public double getServerUtilization() { return serverUtilization; }
        public Map<String, ServerInfo> getBackendServers() { return backendServers; }
    }
    
    public static class ServerInfo {
        private final String name;
        private final boolean online;
        private final long pingTime;
        private final int playerCount;
        
        public ServerInfo(String name, boolean online, long pingTime, int playerCount) {
            this.name = name;
            this.online = online;
            this.pingTime = pingTime;
            this.playerCount = playerCount;
        }
        
        public String getName() { return name; }
        public boolean isOnline() { return online; }
        public long getPingTime() { return pingTime; }
        public int getPlayerCount() { return playerCount; }
    }
}
