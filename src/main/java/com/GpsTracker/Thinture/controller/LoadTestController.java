package com.GpsTracker.Thinture.controller;

import com.GpsTracker.Thinture.service.mqtt.MqttConnectionPool;
import com.GpsTracker.Thinture.service.mqtt.MqttService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 🧪 Load Test Controller for 500 Device Capacity Testing
 * ✅ Quick capacity verification endpoints
 * ✅ Pool statistics monitoring
 * ✅ Real-time health checking
 * ✅ Force scaling capabilities
 */
@RestController
@RequestMapping("/api/test")
public class LoadTestController {
    
    @Autowired
    private MqttConnectionPool connectionPool;
    
    @Autowired
    private MqttService mqttService;
    
    /**
     * 🧪 Test system capacity for specified number of devices
     */
    @GetMapping("/capacity/{deviceCount}")
    public ResponseEntity<Map<String, Object>> testCapacity(@PathVariable int deviceCount) {
        Map<String, Object> result = new HashMap<>();
        long startTime = System.currentTimeMillis();
        
        try {
            // Log test start
            System.out.println("🧪 Testing capacity for " + deviceCount + " devices");
            
            // Test current pool capacity
            result.put("requestedDevices", deviceCount);
            result.put("currentConnections", connectionPool.getTotalConnections());
            result.put("availableConnections", connectionPool.getAvailableConnections());
            result.put("activeConnections", connectionPool.getActiveConnections());
            result.put("estimatedCapacity", connectionPool.getEstimatedCapacity());
            result.put("currentMessagesProcessed", connectionPool.getTotalMessagesProcessed());
            result.put("connectionFailures", connectionPool.getConnectionFailures());
            
            // Check if scaling is needed
            boolean scalingNeeded = connectionPool.getEstimatedCapacity() < deviceCount;
            result.put("scalingNeeded", scalingNeeded);
            
            if (scalingNeeded) {
                int targetConnections = (deviceCount / 15) + 2; // 15 devices per connection + buffer
                System.out.println("📈 Scaling up to " + targetConnections + " connections");
                
                connectionPool.forceScaleUp(targetConnections);
                
                // Wait for scaling with progress
                for (int i = 0; i < 10; i++) {
                    Thread.sleep(1000);
                    if (connectionPool.getEstimatedCapacity() >= deviceCount) {
                        break;
                    }
                    System.out.println("⏳ Scaling progress: " + connectionPool.getTotalConnections() + 
                                     " connections (" + connectionPool.getEstimatedCapacity() + " capacity)");
                }
                
                result.put("scaledConnections", connectionPool.getTotalConnections());
                result.put("newEstimatedCapacity", connectionPool.getEstimatedCapacity());
            }
            
            // Test pool health
            var poolHealth = connectionPool.getPoolHealth();
            result.put("poolHealthy", poolHealth.isHealthy());
            result.put("canHandle500Devices", poolHealth.canHandle500Devices());
            result.put("connectedClients", poolHealth.getConnectedClients());
            result.put("disconnectedClients", poolHealth.getDisconnectedClients());
            result.put("utilizationRate", Math.round(poolHealth.getUtilizationRate() * 100.0) / 100.0);
            result.put("messagesPerSecond", poolHealth.getMessagesPerSecond());
            
            // Service health check
            result.put("serviceHealthy", mqttService.isHealthy());
            result.put("serviceConnected", mqttService.isConnected());
            
            // Performance assessment
            boolean canHandleLoad = connectionPool.getEstimatedCapacity() >= deviceCount && 
                                  poolHealth.isHealthy() && 
                                  poolHealth.getConnectedClients() >= 10;
            
            result.put("canHandleRequestedLoad", canHandleLoad);
            
            // Recommendations
            if (!canHandleLoad) {
                result.put("recommendation", generateRecommendation(deviceCount, connectionPool, poolHealth));
            }
            
            long duration = System.currentTimeMillis() - startTime;
            result.put("testDurationMs", duration);
            result.put("status", "SUCCESS");
            result.put("timestamp", System.currentTimeMillis());
            
            // Success message
            String message = canHandleLoad ? 
                "✅ System can handle " + deviceCount + " devices" :
                "⚠️ System needs optimization for " + deviceCount + " devices";
            result.put("message", message);
            
            System.out.println(message);
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            result.put("status", "ERROR");
            result.put("error", e.getMessage());
            result.put("timestamp", System.currentTimeMillis());
            
            System.err.println("❌ Capacity test failed: " + e.getMessage());
            return ResponseEntity.status(500).body(result);
        }
    }
    
    /**
     * 📊 Get detailed pool statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getPoolStats() {
        try {
            var stats = connectionPool.getDetailedStats();
            var health = connectionPool.getPoolHealth();
            
            Map<String, Object> result = new HashMap<>();
            
            // Pool statistics
            result.put("totalConnections", stats.getTotalConnections());
            result.put("activeConnections", stats.getActiveConnections());
            result.put("availableConnections", stats.getAvailableConnections());
            result.put("totalMessages", stats.getTotalMessages());
            result.put("totalBytes", stats.getTotalBytes());
            result.put("failures", stats.getFailures());
            result.put("messagesPerSecond", stats.getMessagesPerSecond());
            result.put("estimatedCapacity", stats.getEstimatedCapacity());
            result.put("successRate", Math.round(stats.getSuccessRate() * 100.0) / 100.0);
            
            // Health information
            result.put("healthy", health.isHealthy());
            result.put("canHandle500Devices", health.canHandle500Devices());
            result.put("connectedClients", health.getConnectedClients());
            result.put("disconnectedClients", health.getDisconnectedClients());
            result.put("utilizationRate", Math.round(health.getUtilizationRate() * 100.0) / 100.0);
            
            // Service information
            result.put("serviceStats", mqttService.getServiceStats());
            
            result.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("status", "ERROR");
            error.put("error", e.getMessage());
            error.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.status(500).body(error);
        }
    }
    
    /**
     * 🚀 Force scale up the connection pool
     */
    @GetMapping("/scale-up/{targetConnections}")
    public ResponseEntity<Map<String, Object>> forceScaleUp(@PathVariable int targetConnections) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            int currentConnections = connectionPool.getTotalConnections();
            result.put("currentConnections", currentConnections);
            result.put("targetConnections", targetConnections);
            
            if (targetConnections <= currentConnections) {
                result.put("status", "NO_ACTION_NEEDED");
                result.put("message", "Already at or above target connections");
                return ResponseEntity.ok(result);
            }
            
            System.out.println("🚀 Force scaling from " + currentConnections + " to " + targetConnections);
            
            connectionPool.forceScaleUp(targetConnections);
            
            // Wait for scaling
            Thread.sleep(5000);
            
            int newConnections = connectionPool.getTotalConnections();
            result.put("newConnections", newConnections);
            result.put("newEstimatedCapacity", connectionPool.getEstimatedCapacity());
            result.put("connectionsAdded", newConnections - currentConnections);
            result.put("status", "SUCCESS");
            result.put("message", "Scaled from " + currentConnections + " to " + newConnections + " connections");
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            result.put("status", "ERROR");
            result.put("error", e.getMessage());
            return ResponseEntity.status(500).body(result);
        }
    }
    
    /**
     * 🏥 Get comprehensive health check
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getHealthCheck() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            var poolHealth = connectionPool.getPoolHealth();
            var serviceStats = mqttService.getServiceStats();
            
            // Overall health
            boolean overallHealthy = poolHealth.isHealthy() && 
                                   serviceStats.isHealthy() && 
                                   serviceStats.isConnected();
            
            result.put("overallHealthy", overallHealthy);
            result.put("status", overallHealthy ? "HEALTHY" : "UNHEALTHY");
            
            // Pool health
            Map<String, Object> poolInfo = new HashMap<>();
            poolInfo.put("healthy", poolHealth.isHealthy());
            poolInfo.put("connectedClients", poolHealth.getConnectedClients());
            poolInfo.put("disconnectedClients", poolHealth.getDisconnectedClients());
            poolInfo.put("utilizationRate", poolHealth.getUtilizationRate());
            poolInfo.put("messagesPerSecond", poolHealth.getMessagesPerSecond());
            poolInfo.put("estimatedCapacity", poolHealth.getEstimatedCapacity());
            poolInfo.put("canHandle500Devices", poolHealth.canHandle500Devices());
            result.put("connectionPool", poolInfo);
            
            // Service health
            Map<String, Object> serviceInfo = new HashMap<>();
            serviceInfo.put("healthy", serviceStats.isHealthy());
            serviceInfo.put("connected", serviceStats.isConnected());
            serviceInfo.put("serviceMode", serviceStats.getServiceMode());
            serviceInfo.put("uptime", serviceStats.getUptime());
            serviceInfo.put("publishSuccessRate", serviceStats.getPublishSuccessRate());
            result.put("mqttService", serviceInfo);
            
            // System recommendations
            if (!overallHealthy) {
                result.put("recommendations", generateHealthRecommendations(poolHealth, serviceStats));
            }
            
            result.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            result.put("status", "ERROR");
            result.put("error", e.getMessage());
            return ResponseEntity.status(500).body(result);
        }
    }
    
    /**
     * 📋 Generate capacity recommendations
     */
    private String generateRecommendation(int requestedDevices, MqttConnectionPool pool, 
                                        MqttConnectionPool.PoolHealth health) {
        if (pool.getEstimatedCapacity() < requestedDevices) {
            int neededConnections = (requestedDevices / 15) + 2;
            return "Need " + neededConnections + " connections for " + requestedDevices + 
                   " devices. Current: " + pool.getTotalConnections();
        }
        
        if (!health.isHealthy()) {
            return "Pool is unhealthy. Check connection failures and broker connectivity.";
        }
        
        if (health.getDisconnectedClients() > 0) {
            return "Reconnect " + health.getDisconnectedClients() + " disconnected clients.";
        }
        
        return "Increase thread pool sizes and database connections for better performance.";
    }
    
    /**
     * 🏥 Generate health recommendations
     */
    private java.util.List<String> generateHealthRecommendations(MqttConnectionPool.PoolHealth poolHealth, 
                                                               MqttService.MqttServiceStats serviceStats) {
        java.util.List<String> recommendations = new java.util.ArrayList<>();
        
        if (!poolHealth.isHealthy()) {
            recommendations.add("Connection pool is unhealthy - check broker connectivity");
        }
        
        if (poolHealth.getDisconnectedClients() > 0) {
            recommendations.add("Reconnect " + poolHealth.getDisconnectedClients() + " disconnected clients");
        }
        
        if (!serviceStats.isConnected()) {
            recommendations.add("MQTT service is disconnected - check network connectivity");
        }
        
        if (poolHealth.getUtilizationRate() > 80.0) {
            recommendations.add("High utilization rate - consider scaling up connections");
        }
        
        if (!poolHealth.canHandle500Devices()) {
            recommendations.add("Scale up to handle 500 devices - need more connections");
        }
        
        if (recommendations.isEmpty()) {
            recommendations.add("System appears healthy but some components are reporting issues");
        }
        
        return recommendations;
    }
}