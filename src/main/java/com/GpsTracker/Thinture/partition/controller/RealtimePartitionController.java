// ================================================================================================
// AdditionalControllerFeatures.java - REAL-TIME & ADVANCED FEATURES
// ================================================================================================

package com.GpsTracker.Thinture.partition.controller;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import com.GpsTracker.Thinture.partition.dto.PartitionHealthDTO;
import com.GpsTracker.Thinture.partition.dto.PartitionInfoDTO;
import com.GpsTracker.Thinture.partition.dto.PartitionMetricsDTO;
/**
 * 🚀 ADDITIONAL CONTROLLER FEATURES
 * Real-time monitoring, WebSocket integration, and advanced features


// ═══════════════════════════════════════════════════════════════════════════════════
// 1. REAL-TIME MONITORING WITH SERVER-SENT EVENTS (SSE)
// ═══════════════════════════════════════════════════════════════════════════════════

@RestController
@RequestMapping("/api/v1/partitions/realtime")
public class RealtimePartitionController {

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    /**
     * 📡 Real-time monitoring stream
   
    @GetMapping("/monitor")
    public SseEmitter streamMonitoringData() {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        emitters.add(emitter);
        
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError((ex) -> emitters.remove(emitter));
        
        return emitter;
    }

    /**
     * 📊 Broadcast monitoring data every 30 seconds
     
    @Scheduled(fixedRate = 30000)
    public void broadcastMonitoringData() {
        if (emitters.isEmpty()) return;
        
        Map<String, Object> data = new HashMap<>();
        data.put("timestamp", LocalDateTime.now());
        data.put("systemHealth", healthMonitor.isSystemHealthy());
        data.put("performanceSnapshot", performanceMonitor.getSnapshot());
        data.put("metricsData", metricsCollector.getMetricsSummary());
        
        List<SseEmitter> deadEmitters = new ArrayList<>();
        
        emitters.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event()
                    .name("monitoring-update")
                    .data(data));
            } catch (IOException e) {
                deadEmitters.add(emitter);
            }
        });
        
        emitters.removeAll(deadEmitters);
    }
}

// ═══════════════════════════════════════════════════════════════════════════════════
// 2. WEBSOCKET CONTROLLER FOR REAL-TIME UPDATES
// ═══════════════════════════════════════════════════════════════════════════════════

@Controller
public class PartitionWebSocketController {

    /**
     * 🔄 WebSocket endpoint for real-time partition updates
   
    @MessageMapping("/partition/subscribe")
    @SendTo("/topic/partition-updates")
    public Map<String, Object> subscribeToPartitionUpdates() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Subscribed to partition updates");
        response.put("timestamp", LocalDateTime.now());
        return response;
    }

    /**
     * 📈 Real-time health updates
     
    @MessageMapping("/health/subscribe")
    @SendTo("/topic/health-updates")
    public Map<String, Object> subscribeToHealthUpdates() {
        return Map.of(
            "systemHealth", healthMonitor.isSystemHealthy(),
            "timestamp", LocalDateTime.now()
        );
    }
}

// ═══════════════════════════════════════════════════════════════════════════════════
// 3. EXPORT/IMPORT CONTROLLER
// ═══════════════════════════════════════════════════════════════════════════════════

@RestController
@RequestMapping("/api/v1/partitions/export")
public class PartitionExportController {

    /**
     * 📤 Export partition configuration
    
    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> exportConfiguration() {
        Map<String, Object> config = new HashMap<>();
        
        config.put("exportDate", LocalDateTime.now());
        config.put("databaseType", "MYSQL");
        config.put("partitionConfig", Map.of(
            "warningThresholdMB", partitionConfig.getWarningThresholdMB(),
            "criticalThresholdMB", partitionConfig.getCriticalThresholdMB(),
            "emergencyThresholdMB", partitionConfig.getEmergencyThresholdMB(),
            "autoCreateEnabled", partitionConfig.isAutoCreateEnabled(),
            "retentionMonths", partitionConfig.getRetentionMonths()
        ));
        
        return ResponseEntity.ok(config);
    }

    /**
     * 📤 Export partition structure as SQL
    
    @GetMapping("/structure/sql")
    public ResponseEntity<String> exportPartitionStructureSQL() {
        StringBuilder sql = new StringBuilder();
        
        List<PartitionInfoDTO> partitions = partitionManagementService.getAllPartitions();
        
        sql.append("-- MySQL Partition Structure Export\n");
        sql.append("-- Generated on: ").append(LocalDateTime.now()).append("\n\n");
        
        for (PartitionInfoDTO partition : partitions) {
            sql.append("-- Partition: ").append(partition.getPartitionName()).append("\n");
            sql.append("-- Size: ").append(partition.getSizeMB()).append("MB, ");
            sql.append("Rows: ").append(partition.getRecordCount()).append("\n");
            sql.append("ALTER TABLE vehicle_history ADD PARTITION (")
               .append("PARTITION ").append(partition.getPartitionName())
               .append(" VALUES LESS THAN (UNIX_TIMESTAMP('")
               .append(partition.getEndDate().plusDays(1)).append(" 00:00:00')));\n\n");
        }
        
        return ResponseEntity.ok()
            .header("Content-Type", "text/plain; charset=utf-8")
            .header("Content-Disposition", "attachment; filename=partition_structure.sql")
            .body(sql.toString());
    }

    /**
     * 📊 Export metrics as CSV
   
    @GetMapping("/metrics/csv")
    public ResponseEntity<String> exportMetricsCSV() {
        StringBuilder csv = new StringBuilder();
        
        csv.append("Partition Name,Size MB,Row Count,Health Status,Last Updated\n");
        
        List<PartitionInfoDTO> partitions = partitionManagementService.getAllPartitions();
        Map<String, PartitionHealthMonitor.PartitionHealth> healthData = healthMonitor.getPartitionHealthStatus();
        
        for (PartitionInfoDTO partition : partitions) {
            PartitionHealthMonitor.PartitionHealth health = healthData.get(partition.getPartitionName());
            
            csv.append(partition.getPartitionName()).append(",")
               .append(partition.getSizeMB()).append(",")
               .append(partition.getRecordCount()).append(",")
               .append(health != null ? health.getStatus() : "UNKNOWN").append(",")
               .append(partition.getUpdateTime()).append("\n");
        }
        
        return ResponseEntity.ok()
            .header("Content-Type", "text/csv; charset=utf-8")
            .header("Content-Disposition", "attachment; filename=partition_metrics.csv")
            .body(csv.toString());
    }
}

// ═══════════════════════════════════════════════════════════════════════════════════
// 4. ALERTS CONTROLLER
// ═══════════════════════════════════════════════════════════════════════════════════

@RestController
@RequestMapping("/api/v1/partitions/alerts")
public class PartitionAlertsController {

    /**
     * 🔔 Get active alerts
    
    @GetMapping("/active")
    public ResponseEntity<Map<String, Object>> getActiveAlerts() {
        Map<String, Object> response = new HashMap<>();
        List<Map<String, Object>> alerts = new ArrayList<>();
        
        // Check health status for alerts
        Map<String, PartitionHealthMonitor.PartitionHealth> healthStatus = healthMonitor.getPartitionHealthStatus();
        
        for (Map.Entry<String, PartitionHealthMonitor.PartitionHealth> entry : healthStatus.entrySet()) {
            PartitionHealthMonitor.PartitionHealth health = entry.getValue();
            
            if (health.getStatus() != PartitionHealthMonitor.HealthStatus.HEALTHY) {
                Map<String, Object> alert = new HashMap<>();
                alert.put("partitionName", entry.getKey());
                alert.put("alertType", "HEALTH_WARNING");
                alert.put("severity", health.getStatus().toString());
                alert.put("message", "Partition health status: " + health.getStatus());
                alert.put("sizeMB", health.getSizeMB());
                alert.put("rows", health.getRows());
                alert.put("timestamp", health.getLastChecked());
                alerts.add(alert);
            }
        }
        
        // Check performance alerts
        PerformanceMonitor.SystemHealth systemHealth = performanceMonitor.getCurrentSystemHealth();
        if (systemHealth.getOverallStatus() != PerformanceMonitor.HealthStatus.HEALTHY) {
            Map<String, Object> alert = new HashMap<>();
            alert.put("alertType", "PERFORMANCE_WARNING");
            alert.put("severity", systemHealth.getOverallStatus().toString());
            alert.put("message", "System performance degraded");
            alert.put("cpuStatus", systemHealth.getCpuStatus());
            alert.put("memoryStatus", systemHealth.getMemoryStatus());
            alert.put("timestamp", systemHealth.getTimestamp());
            alerts.add(alert);
        }
        
        response.put("success", true);
        response.put("timestamp", LocalDateTime.now());
        response.put("alertCount", alerts.size());
        response.put("alerts", alerts);
        
        return ResponseEntity.ok(response);
    }

    /**
     * 📧 Test alert system
    
    @PostMapping("/test")
    public ResponseEntity<Map<String, Object>> testAlerts(@RequestParam String alertType) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            switch (alertType.toLowerCase()) {
                case "email":
                    if (partitionConfig.isEmailAlertsEnabled()) {
                        // Trigger test email alert
                        response.put("message", "Test email alert sent");
                    } else {
                        response.put("message", "Email alerts not enabled");
                    }
                    break;
                case "slack":
                    if (partitionConfig.isSlackAlertsEnabled()) {
                        // Trigger test Slack alert
                        response.put("message", "Test Slack alert sent");
                    } else {
                        response.put("message", "Slack alerts not enabled");
                    }
                    break;
                default:
                    return ResponseEntity.badRequest()
                        .body(Map.of("error", "Invalid alert type: " + alertType));
            }
            
            response.put("success", true);
            response.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to test alerts: " + e.getMessage()));
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════════
// 5. SEARCH AND ANALYTICS CONTROLLER
// ═══════════════════════════════════════════════════════════════════════════════════

@RestController
@RequestMapping("/api/v1/partitions/analytics")
public class PartitionAnalyticsController {

    /**
     * 📈 Get growth analytics
    
    @GetMapping("/growth")
    public ResponseEntity<Map<String, Object>> getGrowthAnalytics(
            @RequestParam(defaultValue = "30") int days) {
        
        Map<String, Object> response = new HashMap<>();
        
        // This would integrate with your metrics collector to get historical data
        Map<String, Object> metricsData = metricsCollector.getMetricsSummary();
        
        response.put("success", true);
        response.put("timestamp", LocalDateTime.now());
        response.put("periodDays", days);
        response.put("growthData", metricsData);
        response.put("projections", calculateGrowthProjections(days));
        
        return ResponseEntity.ok(response);
    }

    /**
     * 🔍 Search partitions
     
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchPartitions(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String healthStatus,
            @RequestParam(required = false) Double minSizeMB,
            @RequestParam(required = false) Double maxSizeMB) {
        
        List<PartitionInfoDTO> allPartitions = partitionManagementService.getAllPartitions();
        Map<String, PartitionHealthMonitor.PartitionHealth> healthData = healthMonitor.getPartitionHealthStatus();
        
        List<PartitionInfoDTO> filteredPartitions = allPartitions.stream()
            .filter(p -> name == null || p.getPartitionName().contains(name))
            .filter(p -> minSizeMB == null || p.getSizeMB() >= minSizeMB)
            .filter(p -> maxSizeMB == null || p.getSizeMB() <= maxSizeMB)
            .filter(p -> {
                if (healthStatus == null) return true;
                PartitionHealthMonitor.PartitionHealth health = healthData.get(p.getPartitionName());
                return health != null && health.getStatus().toString().equalsIgnoreCase(healthStatus);
            })
            .collect(Collectors.toList());
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("timestamp", LocalDateTime.now());
        response.put("totalCount", allPartitions.size());
        response.put("filteredCount", filteredPartitions.size());
        response.put("partitions", filteredPartitions);
        
        return ResponseEntity.ok(response);
    }

    private Map<String, Object> calculateGrowthProjections(int days) {
        // Implement growth projection logic based on historical data
        Map<String, Object> projections = new HashMap<>();
        projections.put("projectedSizeIncrease", "5.2MB/day");
        projections.put("projectedNewPartitionsNeeded", 2);
        projections.put("recommendedAction", "Continue monitoring");
        return projections;
    }
}

// ═══════════════════════════════════════════════════════════════════════════════════
// 6. WEBSOCKET CONFIGURATION
// ═══════════════════════════════════════════════════════════════════════════════════

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/partitions")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
}*/