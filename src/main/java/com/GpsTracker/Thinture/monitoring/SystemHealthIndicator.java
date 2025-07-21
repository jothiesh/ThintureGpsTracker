package com.GpsTracker.Thinture.monitoring;

import com.GpsTracker.Thinture.service.mqtt.MqttConnectionPool;
import com.GpsTracker.Thinture.service.persistence.BatchPersistenceService;
import com.GpsTracker.Thinture.service.persistence.VehicleDataCache;
import com.GpsTracker.Thinture.service.websocket.WebSocketSessionManager;
import com.GpsTracker.Thinture.service.websocket.WebSocketSessionManager.EnhancedConnectionStatistics;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.ThreadMXBean;
import java.util.HashMap;
import java.util.Map;

@Component
public class SystemHealthIndicator implements HealthIndicator {
    
    @Autowired(required = false)
    private MqttConnectionPool mqttConnectionPool;
    
    @Autowired(required = false)
    private BatchPersistenceService batchPersistenceService;
    
    @Autowired(required = false)
    private VehicleDataCache vehicleDataCache;
    
    @Autowired(required = false)
    private WebSocketSessionManager webSocketSessionManager;
    
    @Autowired
    private DataSource dataSource;
    
    @Autowired
    private GpsMetrics gpsMetrics;
    
    private static final double MEMORY_THRESHOLD_PERCENT = 90.0;
    private static final double CPU_THRESHOLD_PERCENT = 80.0;
    private static final int MIN_DB_CONNECTIONS = 5;
    private static final double MIN_CACHE_HIT_RATIO = 70.0;
    private static final long MAX_BATCH_QUEUE_SIZE = 5000;
    
    @Override
    public Health health() {
        Health.Builder builder = new Health.Builder();
        Map<String, Object> details = new HashMap<>();
        
        boolean isHealthy = true;
        
        // Check MQTT health
        if (mqttConnectionPool != null) {
            Map<String, Object> mqttHealth = checkMqttHealth();
            details.put("mqtt", mqttHealth);
            if (!(boolean) mqttHealth.get("healthy")) {
                isHealthy = false;
            }
        }
        
        // Check database health
        Map<String, Object> dbHealth = checkDatabaseHealth();
        details.put("database", dbHealth);
        if (!(boolean) dbHealth.get("healthy")) {
            isHealthy = false;
        }
        
        // Check memory health
        Map<String, Object> memoryHealth = checkMemoryHealth();
        details.put("memory", memoryHealth);
        if (!(boolean) memoryHealth.get("healthy")) {
            isHealthy = false;
        }
        
        // Check CPU health
        Map<String, Object> cpuHealth = checkCpuHealth();
        details.put("cpu", cpuHealth);
        if (!(boolean) cpuHealth.get("healthy")) {
            isHealthy = false;
        }
        
        // Check batch processing health
        if (batchPersistenceService != null) {
            Map<String, Object> batchHealth = checkBatchHealth();
            details.put("batchProcessing", batchHealth);
            if (!(boolean) batchHealth.get("healthy")) {
                isHealthy = false;
            }
        }
        
        // Check cache health
        if (vehicleDataCache != null) {
            Map<String, Object> cacheHealth = checkCacheHealth();
            details.put("cache", cacheHealth);
            if (!(boolean) cacheHealth.get("healthy")) {
                isHealthy = false;
            }
        }
        
        // Check WebSocket health
        if (webSocketSessionManager != null) {
            Map<String, Object> wsHealth = checkWebSocketHealth();
            details.put("webSocket", wsHealth);
        }
        
        // Add system metrics
        details.put("metrics", getSystemMetrics());
        
        // Set overall health status
        if (isHealthy) {
            builder.up();
        } else {
            builder.down();
        }
        
        return builder.withDetails(details).build();
    }
    
    private Map<String, Object> checkMqttHealth() {
        Map<String, Object> health = new HashMap<>();
        try {
            int totalConnections = mqttConnectionPool.getTotalConnections();
            int activeConnections = mqttConnectionPool.getActiveConnections();
            
            boolean healthy = totalConnections > 0 && activeConnections >= 0;
            
            health.put("healthy", healthy);
            health.put("totalConnections", totalConnections);
            health.put("activeConnections", activeConnections);
            health.put("availableConnections", mqttConnectionPool.getAvailableConnections());
            
            if (!healthy) {
                health.put("error", "No MQTT connections available");
            }
        } catch (Exception e) {
            health.put("healthy", false);
            health.put("error", e.getMessage());
        }
        return health;
    }
    
    private Map<String, Object> checkDatabaseHealth() {
        Map<String, Object> health = new HashMap<>();
        try {
            if (dataSource instanceof HikariDataSource) {
                HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
                HikariPoolMXBean poolMXBean = hikariDataSource.getHikariPoolMXBean();
                
                int totalConnections = poolMXBean.getTotalConnections();
                int activeConnections = poolMXBean.getActiveConnections();
                int idleConnections = poolMXBean.getIdleConnections();
                
                boolean healthy = totalConnections >= MIN_DB_CONNECTIONS && 
                                activeConnections < totalConnections;
                
                health.put("healthy", healthy);
                health.put("totalConnections", totalConnections);
                health.put("activeConnections", activeConnections);
                health.put("idleConnections", idleConnections);
                health.put("threadsAwaitingConnection", poolMXBean.getThreadsAwaitingConnection());
                
                if (!healthy) {
                    health.put("error", "Database connection pool exhausted or too small");
                }
            } else {
                health.put("healthy", true);
                health.put("type", "Non-Hikari datasource");
            }
        } catch (Exception e) {
            health.put("healthy", false);
            health.put("error", e.getMessage());
        }
        return health;
    }
    
    private Map<String, Object> checkMemoryHealth() {
        Map<String, Object> health = new HashMap<>();
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        
        long heapUsed = memoryMXBean.getHeapMemoryUsage().getUsed();
        long heapMax = memoryMXBean.getHeapMemoryUsage().getMax();
        double heapUsagePercent = (heapUsed * 100.0) / heapMax;
        
        boolean healthy = heapUsagePercent < MEMORY_THRESHOLD_PERCENT;
        
        health.put("healthy", healthy);
        health.put("heapUsedMB", heapUsed / (1024 * 1024));
        health.put("heapMaxMB", heapMax / (1024 * 1024));
        health.put("heapUsagePercent", String.format("%.2f", heapUsagePercent));
        
        if (!healthy) {
            health.put("error", "Memory usage above threshold: " + MEMORY_THRESHOLD_PERCENT + "%");
        }
        
        return health;
    }
    
    private Map<String, Object> checkCpuHealth() {
        Map<String, Object> health = new HashMap<>();
        
        double cpuLoad = ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage();
        int processors = Runtime.getRuntime().availableProcessors();
        double cpuUsagePercent = (cpuLoad / processors) * 100;
        
        boolean healthy = cpuUsagePercent < CPU_THRESHOLD_PERCENT || cpuLoad < 0;
        
        health.put("healthy", healthy);
        health.put("systemLoadAverage", cpuLoad);
        health.put("availableProcessors", processors);
        health.put("cpuUsagePercent", cpuUsagePercent >= 0 ? String.format("%.2f", cpuUsagePercent) : "N/A");
        
        if (!healthy && cpuLoad >= 0) {
            health.put("error", "CPU usage above threshold: " + CPU_THRESHOLD_PERCENT + "%");
        }
        
        return health;
    }
    
    private Map<String, Object> checkBatchHealth() {
        Map<String, Object> health = new HashMap<>();
        try {
            BatchPersistenceService.BatchStatistics stats = batchPersistenceService.getStatistics();
            
            boolean healthy = stats.getCurrentQueueSize() < MAX_BATCH_QUEUE_SIZE &&
                            stats.getSuccessRate() > 95.0;
            
            health.put("healthy", healthy);
            health.put("currentQueueSize", stats.getCurrentQueueSize());
            health.put("totalRecordsSaved", stats.getTotalRecordsSaved());
            health.put("successRate", String.format("%.2f", stats.getSuccessRate()));
            health.put("failedBatches", stats.getFailedBatches());
            
            if (!healthy) {
                if (stats.getCurrentQueueSize() >= MAX_BATCH_QUEUE_SIZE) {
                    health.put("error", "Batch queue size exceeded threshold");
                } else {
                    health.put("error", "Batch success rate below 95%");
                }
            }
        } catch (Exception e) {
            health.put("healthy", false);
            health.put("error", e.getMessage());
        }
        return health;
    }
    
    private Map<String, Object> checkCacheHealth() {
        Map<String, Object> health = new HashMap<>();
        try {
            VehicleDataCache.CacheStatistics stats = vehicleDataCache.getCacheStatistics();
            
            double hitRate = stats.getHitRate() * 100;
            boolean healthy = hitRate > MIN_CACHE_HIT_RATIO;
            
            health.put("healthy", healthy);
            health.put("totalSize", stats.getTotalCacheSize());
            health.put("hitRate", String.format("%.2f", hitRate));
            health.put("totalHits", stats.getTotalHits());
            health.put("totalMisses", stats.getTotalMisses());
            health.put("evictions", stats.getEvictions());
            
            if (!healthy) {
                health.put("error", "Cache hit rate below threshold: " + MIN_CACHE_HIT_RATIO + "%");
            }
        } catch (Exception e) {
            health.put("healthy", false);
            health.put("error", e.getMessage());
        }
        return health;
    }
    
    private Map<String, Object> checkWebSocketHealth() {
        Map<String, Object> health = new HashMap<>();
        try {
            EnhancedConnectionStatistics stats = webSocketSessionManager.getEnhancedStatistics();
            
            health.put("healthy", true);
            health.put("activeSessions", stats.getActiveSessions());
            health.put("totalConnections", stats.getTotalConnections());
            health.put("totalSubscribers", stats.getTotalSubscribers());
        } catch (Exception e) {
            health.put("healthy", false);
            health.put("error", e.getMessage());
        }
        return health;
    }
    
    private Map<String, Object> getSystemMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        
        metrics.put("messagesReceived", gpsMetrics.getTotalMessagesReceived());
        metrics.put("messagesSaved", gpsMetrics.getTotalMessagesSaved());
        metrics.put("messagesFailed", gpsMetrics.getTotalMessagesFailed());
        metrics.put("messagesPerSecond", String.format("%.2f", gpsMetrics.getMessagesPerSecond()));
        metrics.put("threadCount", threadMXBean.getThreadCount());
        metrics.put("peakThreadCount", threadMXBean.getPeakThreadCount());
        metrics.put("uptime", ManagementFactory.getRuntimeMXBean().getUptime());
        
        return metrics;
    }
}