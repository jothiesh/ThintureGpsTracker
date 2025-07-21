package com.GpsTracker.Thinture.service.mqtt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 🌟 ENHANCED MQTT Service - Centralized MQTT Operations Hub
 * ✅ Coordinates between all enhanced MQTT components
 * ✅ Provides unified API for MQTT operations
 * ✅ Colorful status monitoring and logging
 * ✅ Performance metrics and health integration
 * ✅ Support for both connection pool and fixed service modes
 * ✅ Real-time statistics and monitoring
 */
@Service
public class MqttService {
    
    private static final Logger logger = LoggerFactory.getLogger(MqttService.class);
    
    // 🎨 ANSI Color codes for beautiful console output
    private static final String RESET = "\u001B[0m";
    private static final String RED = "\u001B[31m";
    private static final String GREEN = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String BLUE = "\u001B[34m";
    private static final String PURPLE = "\u001B[35m";
    private static final String CYAN = "\u001B[36m";
    private static final String WHITE = "\u001B[37m";
    private static final String BRIGHT_RED = "\u001B[91m";
    private static final String BRIGHT_GREEN = "\u001B[92m";
    private static final String BRIGHT_YELLOW = "\u001B[93m";
    private static final String BRIGHT_BLUE = "\u001B[94m";
    private static final String BRIGHT_MAGENTA = "\u001B[95m";
    private static final String BRIGHT_CYAN = "\u001B[96m";
    private static final String BRIGHT_WHITE = "\u001B[97m";
    
    // 🔗 Enhanced component dependencies
    @Autowired
    private MqttConnectionPool connectionPool;
    
    @Autowired
    private MqttHealthMonitor healthMonitor;
    
    @Autowired
    private MqttMessageReceiver messageReceiver;
    
    @Autowired
    private MqttConnectionManager connectionManager;
    
    @Autowired(required = false)
    private FixMqttService fixMqttService;
    
    // 📊 Configuration
    @Value("${mqtt.service.mode:pool}")
    private String serviceMode; // "pool" or "fixed"
    
    @Value("${mqtt.service.name:GPS-MQTT-Service}")
    private String serviceName;
    
    // 📊 Service metrics
    private final AtomicLong serviceStartTime = new AtomicLong(0);
    private final AtomicLong totalPublishAttempts = new AtomicLong(0);
    private final AtomicLong totalPublishSuccesses = new AtomicLong(0);
    private final AtomicLong totalPublishFailures = new AtomicLong(0);
    private final AtomicBoolean serviceHealthy = new AtomicBoolean(true);
    
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    @PostConstruct
    public void init() {
        serviceStartTime.set(System.currentTimeMillis());
        
        logColorful("🌟 INITIALIZING ENHANCED MQTT SERVICE", BRIGHT_CYAN);
        logColorful("⏱️ Service startup time: " + LocalDateTime.now().format(timeFormatter), CYAN);
        
        // 📊 Display service configuration
        displayServiceConfiguration();
        
        // 🔍 Detect and initialize appropriate service mode
        initializeServiceMode();
        
        // 🎨 Display service dashboard
        displayServiceDashboard();
        
        logColorful("✅ Enhanced MQTT Service initialized successfully", BRIGHT_GREEN);
    }
    
    /**
     * 📊 Display service configuration
     */
    private void displayServiceConfiguration() {
        logColorful("", "");
        logColorful("╔════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════", BRIGHT_BLUE);
        logColorful("║ ⚙️ ENHANCED MQTT SERVICE CONFIGURATION", BRIGHT_BLUE);
        logColorful("╠════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════", BRIGHT_BLUE);
        logColorful("║ 🏷️ Service Name: " + serviceName, BLUE);
        logColorful("║ 🔧 Service Mode: " + serviceMode.toUpperCase(), BLUE);
        logColorful("║ 🎯 Target Devices: 5000+", BLUE);
        logColorful("║ 🏗️ Architecture: Enhanced Multi-Component", BLUE);
        logColorful("║ 🔄 Connection Pool: " + (connectionPool != null ? "ENABLED" : "DISABLED"), BLUE);
        logColorful("║ 🔧 Fixed Service: " + (fixMqttService != null ? "ENABLED" : "DISABLED"), BLUE);
        logColorful("║ 🏥 Health Monitor: " + (healthMonitor != null ? "ENABLED" : "DISABLED"), BLUE);
        logColorful("║ 📨 Message Receiver: " + (messageReceiver != null ? "ENABLED" : "DISABLED"), BLUE);
        logColorful("╚════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════", BRIGHT_BLUE);
    }
    
    /**
     * 🔧 Initialize service mode
     */
    private void initializeServiceMode() {
        logColorful("🔧 Initializing service mode: " + serviceMode.toUpperCase(), BRIGHT_YELLOW);
        
        if ("pool".equalsIgnoreCase(serviceMode)) {
            initializePoolMode();
        } else if ("fixed".equalsIgnoreCase(serviceMode)) {
            initializeFixedMode();
        } else {
            logColorful("⚠️ Unknown service mode: " + serviceMode + ", defaulting to pool mode", YELLOW);
            initializePoolMode();
        }
    }
    
    /**
     * 🏊 Initialize connection pool mode
     */
    private void initializePoolMode() {
        logColorful("🏊 Initializing CONNECTION POOL mode", BRIGHT_GREEN);
        
        if (connectionPool != null) {
            logColorful("✅ Connection pool is available", GREEN);
            logColorful("📊 Initial pool size: " + connectionPool.getTotalConnections(), CYAN);
        } else {
            logColorful("❌ Connection pool is not available!", RED);
            serviceHealthy.set(false);
        }
    }
    
    /**
     * 🔧 Initialize fixed service mode
     */
    private void initializeFixedMode() {
        logColorful("🔧 Initializing FIXED SERVICE mode", BRIGHT_GREEN);
        
        if (fixMqttService != null) {
            logColorful("✅ Fixed MQTT service is available", GREEN);
            var status = fixMqttService.getServiceStatus();
            logColorful("📊 Service status: " + (status.isConnected() ? "CONNECTED" : "DISCONNECTED"), 
                       status.isConnected() ? GREEN : RED);
        } else {
            logColorful("❌ Fixed MQTT service is not available!", RED);
            serviceHealthy.set(false);
        }
    }
    
    /**
     * 🎨 Display service dashboard
     */
    private void displayServiceDashboard() {
        logColorful("", "");
        logColorful("╔════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════", BRIGHT_MAGENTA);
        logColorful("║ 🎯 MQTT SERVICE DASHBOARD", BRIGHT_MAGENTA);
        logColorful("║ 🚀 Ready for GPS device communication", BRIGHT_MAGENTA);
        logColorful("╠════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════", BRIGHT_MAGENTA);
        
        // 🏥 Health status
        boolean isHealthy = healthMonitor != null ? healthMonitor.isHealthy() : true;
        logColorful("║ 🏥 Overall Health: " + (isHealthy ? "🟢 HEALTHY" : "🔴 UNHEALTHY"), 
                   isHealthy ? BRIGHT_GREEN : BRIGHT_RED);
        
        // 🔄 Circuit breaker status
        if (healthMonitor != null) {
            var cbState = healthMonitor.getCircuitBreakerState();
            logColorful("║ 🔄 Circuit Breaker: " + getCircuitBreakerDisplay(cbState), 
                       getCircuitBreakerColor(cbState));
        }
        
        // 📊 Connection status
        if ("pool".equalsIgnoreCase(serviceMode) && connectionPool != null) {
            logColorful("║ 🔗 Connection Pool: " + connectionPool.getTotalConnections() + " total, " + 
                       connectionPool.getActiveConnections() + " active", CYAN);
        } else if (fixMqttService != null) {
            var status = fixMqttService.getServiceStatus();
            logColorful("║ 🔧 Fixed Service: " + (status.isConnected() ? "CONNECTED" : "DISCONNECTED"), 
                       status.isConnected() ? GREEN : RED);
        }
        
        // 📨 Message statistics
        if (messageReceiver != null) {
            logColorful("║ 📨 Messages: " + messageReceiver.getMessageCount() + " total, " + 
                       String.format("%.2f", messageReceiver.getMessagesPerSecond()) + "/sec", PURPLE);
            logColorful("║ 📱 Active Devices: " + messageReceiver.getActiveDevices(), YELLOW);
        }
        
        // 🚀 Service status
        logColorful("║ 🚀 Service Status: " + (serviceHealthy.get() ? "OPERATIONAL" : "DEGRADED"), 
                   serviceHealthy.get() ? BRIGHT_GREEN : BRIGHT_YELLOW);
        
        logColorful("╚════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════", BRIGHT_MAGENTA);
    }
    
    /**
     * 📤 Publish message using appropriate service
     */
    public void publish(String topic, String message) {
        totalPublishAttempts.incrementAndGet();
        
        try {
            logColorful("📤 Publishing message to topic: " + topic, BRIGHT_BLUE);
            
            if ("pool".equalsIgnoreCase(serviceMode) && connectionPool != null) {
                // 🏊 Use connection pool
                connectionPool.publish(topic, message);
                logColorful("✅ Message published via connection pool", BRIGHT_GREEN);
            } else if (fixMqttService != null) {
                // 🔧 Use fixed service
                fixMqttService.publishMessage(topic, message);
                logColorful("✅ Message published via fixed service", BRIGHT_GREEN);
            } else {
                throw new IllegalStateException("No MQTT service available for publishing");
            }
            
            totalPublishSuccesses.incrementAndGet();
            
        } catch (Exception e) {
            totalPublishFailures.incrementAndGet();
            logColorful("❌ Failed to publish message: " + e.getMessage(), BRIGHT_RED);
            logger.error("Failed to publish message to topic: " + topic, e);
            throw new RuntimeException("MQTT publish failed", e);
        }
    }
    
    /**
     * 🔗 Check if service is connected
     */
    public boolean isConnected() {
        if ("pool".equalsIgnoreCase(serviceMode) && connectionPool != null) {
            return connectionPool.getTotalConnections() > 0 && 
                   connectionPool.getActiveConnections() >= 0;
        } else if (fixMqttService != null) {
            return fixMqttService.getServiceStatus().isConnected();
        }
        return false;
    }
    
    /**
     * 🏥 Get overall service health
     */
    public boolean isHealthy() {
        if (healthMonitor != null) {
            return healthMonitor.isHealthy() && serviceHealthy.get();
        }
        return serviceHealthy.get();
    }
    
    /**
     * 📊 Get comprehensive service statistics
     */
    public MqttServiceStats getServiceStats() {
        Map<String, Object> stats = new HashMap<>();
        
        // 📊 Basic service stats
        stats.put("service_mode", serviceMode);
        stats.put("service_name", serviceName);
        stats.put("service_healthy", serviceHealthy.get());
        stats.put("uptime", System.currentTimeMillis() - serviceStartTime.get());
        stats.put("publish_attempts", totalPublishAttempts.get());
        stats.put("publish_successes", totalPublishSuccesses.get());
        stats.put("publish_failures", totalPublishFailures.get());
        
        // 🔗 Connection stats
        if ("pool".equalsIgnoreCase(serviceMode) && connectionPool != null) {
            stats.put("total_connections", connectionPool.getTotalConnections());
            stats.put("active_connections", connectionPool.getActiveConnections());
            stats.put("available_connections", connectionPool.getAvailableConnections());
            stats.put("connection_failures", connectionPool.getConnectionFailures());
        } else if (fixMqttService != null) {
            var status = fixMqttService.getServiceStatus();
            stats.put("fixed_service_connected", status.isConnected());
            stats.put("fixed_service_messages", status.getMessagesReceived());
            stats.put("fixed_service_client_id", status.getClientId());
        }
        
        // 📨 Message stats
        if (messageReceiver != null) {
            stats.put("total_messages", messageReceiver.getMessageCount());
            stats.put("messages_per_second", messageReceiver.getMessagesPerSecond());
            stats.put("active_devices", messageReceiver.getActiveDevices());
            stats.put("valid_messages", messageReceiver.getValidMessagesCount());
            stats.put("invalid_messages", messageReceiver.getInvalidMessagesCount());
        }
        
        // 🏥 Health stats
        if (healthMonitor != null) {
            stats.put("health_monitor_healthy", healthMonitor.isHealthy());
            stats.put("circuit_breaker_state", healthMonitor.getCircuitBreakerState().toString());
            stats.put("consecutive_failures", healthMonitor.getConsecutiveFailures());
            stats.put("total_health_checks", healthMonitor.getTotalHealthChecks());
            stats.put("active_alerts", healthMonitor.getActiveAlerts());
        }
        
        return new MqttServiceStats(
            isHealthy(),
            isConnected(),
            serviceMode,
            System.currentTimeMillis() - serviceStartTime.get(),
            stats
        );
    }
    
    /**
     * 🔄 Trigger manual health check
     */
    public void triggerHealthCheck() {
        if (healthMonitor != null) {
            logColorful("🏥 Manual health check triggered", BRIGHT_YELLOW);
            healthMonitor.performComprehensiveHealthCheck();
        } else {
            logColorful("⚠️ Health monitor not available", YELLOW);
        }
    }
    
    /**
     * 🔄 Trigger manual reconnection
     */
    public void triggerReconnection() {
        logColorful("🔄 Manual reconnection triggered", BRIGHT_YELLOW);
        
        if ("fixed".equalsIgnoreCase(serviceMode) && fixMqttService != null) {
            fixMqttService.reconnect();
        } else {
            logColorful("⚠️ Manual reconnection not supported in pool mode", YELLOW);
        }
    }
    
    /**
     * 📊 Display real-time service status
     */
    public void displayServiceStatus() {
        logColorful("", "");
        logColorful("╔════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════", BRIGHT_CYAN);
        logColorful("║ 📊 REAL-TIME SERVICE STATUS", BRIGHT_CYAN);
        logColorful("║ 🕒 " + LocalDateTime.now().format(timeFormatter), BRIGHT_CYAN);
        logColorful("╠════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════", BRIGHT_CYAN);
        
        // 🏥 Overall health
        boolean healthy = isHealthy();
        logColorful("║ 🏥 Overall Health: " + (healthy ? "🟢 HEALTHY" : "🔴 UNHEALTHY"), 
                   healthy ? BRIGHT_GREEN : BRIGHT_RED);
        
        // 🔗 Connection status
        boolean connected = isConnected();
        logColorful("║ 🔗 Connection Status: " + (connected ? "🟢 CONNECTED" : "🔴 DISCONNECTED"), 
                   connected ? BRIGHT_GREEN : BRIGHT_RED);
        
        // 📊 Performance metrics
        double publishSuccessRate = totalPublishAttempts.get() > 0 ? 
            (totalPublishSuccesses.get() * 100.0) / totalPublishAttempts.get() : 0.0;
        logColorful("║ 📤 Publish Success Rate: " + String.format("%.2f%%", publishSuccessRate) + 
                   " (" + totalPublishSuccesses.get() + "/" + totalPublishAttempts.get() + ")", PURPLE);
        
        // ⏱️ Uptime
        long uptime = System.currentTimeMillis() - serviceStartTime.get();
        logColorful("║ ⏱️ Service Uptime: " + formatDuration(uptime), CYAN);
        
        // 🔧 Service mode
        logColorful("║ 🔧 Service Mode: " + serviceMode.toUpperCase(), BLUE);
        
        logColorful("╚════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════", BRIGHT_CYAN);
    }
    
    // Utility methods
    private String getCircuitBreakerDisplay(MqttHealthMonitor.CircuitBreakerState state) {
        return switch (state) {
            case CLOSED -> "🟢 CLOSED";
            case OPEN -> "🔴 OPEN";
            case HALF_OPEN -> "🟡 HALF_OPEN";
        };
    }
    
    private String getCircuitBreakerColor(MqttHealthMonitor.CircuitBreakerState state) {
        return switch (state) {
            case CLOSED -> BRIGHT_GREEN;
            case OPEN -> BRIGHT_RED;
            case HALF_OPEN -> BRIGHT_YELLOW;
        };
    }
    
    private String formatDuration(long millis) {
        if (millis < 1000) return millis + "ms";
        if (millis < 60000) return String.format("%.1fs", millis / 1000.0);
        if (millis < 3600000) return String.format("%.1fm", millis / 60000.0);
        if (millis < 86400000) return String.format("%.1fh", millis / 3600000.0);
        return String.format("%.1fd", millis / 86400000.0);
    }
    
    private void logColorful(String message, String color) {
        System.out.println(color + message + RESET);
        logger.info(message.replaceAll("║", "").replaceAll("╔", "").replaceAll("╚", "").replaceAll("╠", "").replaceAll("═", "").trim());
    }
    
    // 📊 Service statistics data class
    public static class MqttServiceStats {
        private final boolean healthy;
        private final boolean connected;
        private final String serviceMode;
        private final long uptime;
        private final Map<String, Object> detailedStats;
        
        public MqttServiceStats(boolean healthy, boolean connected, String serviceMode, 
                               long uptime, Map<String, Object> detailedStats) {
            this.healthy = healthy;
            this.connected = connected;
            this.serviceMode = serviceMode;
            this.uptime = uptime;
            this.detailedStats = detailedStats;
        }
        
        // Getters
        public boolean isHealthy() { return healthy; }
        public boolean isConnected() { return connected; }
        public String getServiceMode() { return serviceMode; }
        public long getUptime() { return uptime; }
        public Map<String, Object> getDetailedStats() { return detailedStats; }
        
        // Convenience methods
        public double getPublishSuccessRate() {
            long attempts = (Long) detailedStats.getOrDefault("publish_attempts", 0L);
            long successes = (Long) detailedStats.getOrDefault("publish_successes", 0L);
            return attempts > 0 ? (successes * 100.0) / attempts : 0.0;
        }
        
        public int getTotalConnections() {
            return (Integer) detailedStats.getOrDefault("total_connections", 0);
        }
        
        public long getTotalMessages() {
            return (Long) detailedStats.getOrDefault("total_messages", 0L);
        }
        
        public int getActiveDevices() {
            return (Integer) detailedStats.getOrDefault("active_devices", 0);
        }
    }
    
    
    
 // In MqttService.java - ADD input validation

    /**
     * 📤 Enhanced publish with validation
     */
    public void publishWithValidation(String topic, String message) {
        // Input validation
        if (topic == null || topic.trim().isEmpty()) {
            throw new IllegalArgumentException("Topic cannot be null or empty");
        }
        
        if (message == null) {
            throw new IllegalArgumentException("Message cannot be null");
        }
        
        // Topic validation
        if (topic.length() > 255) {
            throw new IllegalArgumentException("Topic too long: " + topic.length() + " chars (max 255)");
        }
        
        // Message size validation
        byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);
        if (messageBytes.length > 268435456) { // 256MB MQTT limit
            throw new IllegalArgumentException("Message too large: " + messageBytes.length + " bytes");
        }
        
        // Special character validation
        if (topic.contains("#") && !topic.endsWith("#")) {
            throw new IllegalArgumentException("Invalid topic: # must be at the end");
        }
        
        if (topic.contains("+") && topic.contains("++")) {
            throw new IllegalArgumentException("Invalid topic: consecutive + wildcards");
        }
        
        totalPublishAttempts.incrementAndGet();
        
        try {
            logColorful("📤 Publishing validated message to: " + topic, BRIGHT_BLUE);
            logColorful("📊 Message size: " + formatBytes(messageBytes.length), BLUE);
            
            // Use existing publish method
            publish(topic, message);
            
        } catch (Exception e) {
            totalPublishFailures.incrementAndGet();
            logColorful("❌ Validated publish failed: " + e.getMessage(), BRIGHT_RED);
            throw e; // Re-throw for caller handling
        }
    }

    private String formatBytes(int length) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
     * 🔍 Enhanced GPS coordinate validation
     */
    private boolean isValidGpsCoordinatesEnhanced(double latitude, double longitude) {
        // Basic range check
        if (latitude < -90.0 || latitude > 90.0) {
            logColorful("❌ Invalid latitude: " + latitude + " (range: -90 to 90)", RED);
            return false;
        }
        
        if (longitude < -180.0 || longitude > 180.0) {
            logColorful("❌ Invalid longitude: " + longitude + " (range: -180 to 180)", RED);
            return false;
        }
        
        // Check for null island (0,0) - usually invalid for GPS tracking
        if (Math.abs(latitude) < 0.001 && Math.abs(longitude) < 0.001) {
            logColorful("⚠️ Null Island coordinates detected: " + latitude + ", " + longitude, YELLOW);
            return false;
        }
        
        // Check for common test coordinates
        if ((Math.abs(latitude - 12.9716) < 0.0001 && Math.abs(longitude - 77.5946) < 0.0001)) {
            logColorful("⚠️ Test coordinates detected (Bangalore): " + latitude + ", " + longitude, YELLOW);
            // Return true for test coordinates in development
            return true;
        }
        
        return true;
    }
}