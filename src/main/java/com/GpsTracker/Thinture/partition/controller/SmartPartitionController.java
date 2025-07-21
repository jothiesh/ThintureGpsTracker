// ================================================================================================
// SmartPartitionController.java - SMART PARTITION REST API CONTROLLER
// ================================================================================================

package com.GpsTracker.Thinture.partition.controller;

import com.GpsTracker.Thinture.partition.config.PartitionConfig;
import com.GpsTracker.Thinture.partition.service.PartitionSizeService;
import com.GpsTracker.Thinture.partition.service.SmartPartitionService;
import com.GpsTracker.Thinture.partition.strategy.SizeBasedStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import com.GpsTracker.Thinture.partition.dto.PartitionHealthDTO;
import com.GpsTracker.Thinture.partition.dto.PartitionInfoDTO;
import com.GpsTracker.Thinture.partition.dto.PartitionMetricsDTO;
/**
 * 🎮 Smart Partition Management REST API Controller
 * Provides comprehensive REST endpoints for intelligent partition management
 */
@RestController
@RequestMapping("/api/v1/smart-partitions")
@CrossOrigin(origins = "*")
public class SmartPartitionController {
    
    private static final Logger logger = LoggerFactory.getLogger(SmartPartitionController.class);
    private static final Logger apiLogger = LoggerFactory.getLogger("API." + SmartPartitionController.class.getName());
    private static final Logger performanceLogger = LoggerFactory.getLogger("PERFORMANCE." + SmartPartitionController.class.getName());
    private static final Logger auditLogger = LoggerFactory.getLogger("AUDIT." + SmartPartitionController.class.getName());
    
    @Autowired
    private SmartPartitionService smartPartitionService;
    
    @Autowired
    private PartitionSizeService partitionSizeService;
    
    @Autowired
    private SizeBasedStrategy sizeBasedStrategy;
    
    @Autowired
    private PartitionConfig partitionConfig;
    
    // ═══════════════════════════════════════════════════════════════════════════════════
    // INITIALIZATION
    // ═══════════════════════════════════════════════════════════════════════════════════
    
    @PostConstruct
    public void initializeController() {
        apiLogger.info("🚀 Initializing Smart Partition Controller...");
        
        try {
            // Log available endpoints
            logAvailableEndpoints();
            
            apiLogger.info("✅ Smart Partition Controller initialized successfully");
            
        } catch (Exception e) {
            apiLogger.error("❌ Failed to initialize Smart Partition Controller: {}", e.getMessage(), e);
        }
    }
    
    private void logAvailableEndpoints() {
        apiLogger.info("📋 Available Smart Partition Endpoints:");
        apiLogger.info("  POST /api/v1/smart-partitions/check-and-create");
        apiLogger.info("  POST /api/v1/smart-partitions/check-sizes");
        apiLogger.info("  GET  /api/v1/smart-partitions/size/{partitionName}");
        apiLogger.info("  GET  /api/v1/smart-partitions/sizes/all");
        apiLogger.info("  GET  /api/v1/smart-partitions/statistics");
        apiLogger.info("  GET  /api/v1/smart-partitions/config");
        apiLogger.info("  POST /api/v1/smart-partitions/async/check-and-create");
        apiLogger.info("  GET  /api/v1/smart-partitions/health");
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════════
    // MAIN SMART PARTITION OPERATIONS
    // ═══════════════════════════════════════════════════════════════════════════════════
    
    /**
     * POST /api/v1/smart-partitions/check-and-create
     * Perform comprehensive smart partition check and creation
     */
    @PostMapping("/check-and-create")
    public ResponseEntity<?> checkAndCreatePartitions() {
        long startTime = System.currentTimeMillis();
        apiLogger.info("🧠 API Request: Smart partition check and create");
        
        try {
            auditLogger.info("AUDIT: Smart partition check initiated by API call");
            
            SmartPartitionService.SmartPartitionResult result = smartPartitionService.checkAndCreatePartitions();
            
            long duration = System.currentTimeMillis() - startTime;
            performanceLogger.info("⚡ Smart partition check completed in {}ms", duration);
            
            Map<String, Object> response = createSuccessResponse(result, duration);
            
            if (result.isSuccess()) {
                auditLogger.info("AUDIT: Smart partition check completed successfully - Created: {}, Checked: {}", 
                               result.getCreatedPartitions().size(), result.getCheckedPartitions().size());
                return ResponseEntity.ok(response);
            } else {
                auditLogger.warn("AUDIT: Smart partition check completed with issues: {}", result.getMessage());
                return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT).body(response);
            }
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            performanceLogger.error("❌ Smart partition check failed after {}ms", duration);
            auditLogger.error("AUDIT: Smart partition check failed: {}", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Smart partition check failed: " + e.getMessage(), duration));
        }
    }
    
    /**
     * POST /api/v1/smart-partitions/check-sizes
     * Check partition sizes and identify issues
     */
    @PostMapping("/check-sizes")
    public ResponseEntity<?> checkPartitionSizes() {
        long startTime = System.currentTimeMillis();
        apiLogger.info("📏 API Request: Partition size check");
        
        try {
            auditLogger.info("AUDIT: Partition size check initiated by API call");
            
            Map<String, String> oversizedPartitions = partitionSizeService.findOversizedPartitions();
            PartitionSizeService.PartitionSizeStatistics statistics = partitionSizeService.getSizeStatistics();
            
            long duration = System.currentTimeMillis() - startTime;
            performanceLogger.info("⚡ Size check completed in {}ms", duration);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("timestamp", LocalDateTime.now());
            response.put("executionTimeMs", duration);
            response.put("oversizedPartitions", oversizedPartitions);
            response.put("statistics", statistics);
            response.put("totalPartitions", statistics.getTotalPartitions());
            response.put("totalSizeMB", statistics.getTotalSizeMB());
            response.put("issuesFound", oversizedPartitions.size());
            
            auditLogger.info("AUDIT: Size check completed - {} partitions checked, {} issues found", 
                           statistics.getTotalPartitions(), oversizedPartitions.size());
            
            if (oversizedPartitions.isEmpty()) {
                return ResponseEntity.ok(response);
            } else {
                response.put("warning", "Found " + oversizedPartitions.size() + " oversized partitions");
                return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
            }
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            performanceLogger.error("❌ Size check failed after {}ms", duration);
            auditLogger.error("AUDIT: Size check failed: {}", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Size check failed: " + e.getMessage(), duration));
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════════
    // SIZE MONITORING ENDPOINTS
    // ═══════════════════════════════════════════════════════════════════════════════════
    
    /**
     * GET /api/v1/smart-partitions/size/{partitionName}
     * Get detailed size information for specific partition
     */
    @GetMapping("/size/{partitionName}")
    public ResponseEntity<?> getPartitionSize(@PathVariable String partitionName) {
        long startTime = System.currentTimeMillis();
        apiLogger.info("📊 API Request: Get size for partition {}", partitionName);
        
        try {
            if (!isValidPartitionName(partitionName)) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Invalid partition name format: " + partitionName, 0));
            }
            
            PartitionSizeService.PartitionSizeInfo sizeInfo = partitionSizeService.getPartitionSizeInfo(partitionName);
            
            long duration = System.currentTimeMillis() - startTime;
            performanceLogger.debug("⚡ Size info retrieved in {}ms", duration);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("timestamp", LocalDateTime.now());
            response.put("executionTimeMs", duration);
            response.put("partitionName", partitionName);
            response.put("sizeMB", sizeInfo.getSizeMB());
            response.put("rowCount", sizeInfo.getRowCount());
            response.put("lastUpdated", sizeInfo.getLastUpdated());
            
            // Add threshold analysis
            String thresholdLevel = partitionConfig.getThresholdLevel(sizeInfo.getSizeMB());
            response.put("thresholdLevel", thresholdLevel);
            response.put("warningThreshold", partitionConfig.getWarningThresholdMB());
            response.put("criticalThreshold", partitionConfig.getCriticalThresholdMB());
            response.put("emergencyThreshold", partitionConfig.getEmergencyThresholdMB());
            
            // Add recommendations
            if (!"NORMAL".equals(thresholdLevel)) {
                response.put("recommendation", generateSizeRecommendation(partitionName, sizeInfo.getSizeMB(), thresholdLevel));
            }
            
            auditLogger.info("AUDIT: Size info retrieved for {} - {:.1f}MB ({})", 
                           partitionName, sizeInfo.getSizeMB(), thresholdLevel);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            performanceLogger.error("❌ Size info retrieval failed after {}ms", duration);
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to get partition size: " + e.getMessage(), duration));
        }
    }
    
    /**
     * GET /api/v1/smart-partitions/sizes/all
     * Get sizes for all partitions
     */
    @GetMapping("/sizes/all")
    public ResponseEntity<?> getAllPartitionSizes() {
        long startTime = System.currentTimeMillis();
        apiLogger.info("📊 API Request: Get all partition sizes");
        
        try {
            Map<String, Double> allSizes = partitionSizeService.getAllPartitionSizes();
            Map<String, String> oversizedPartitions = partitionSizeService.findOversizedPartitions();
            PartitionSizeService.PartitionSizeStatistics statistics = partitionSizeService.getSizeStatistics();
            
            long duration = System.currentTimeMillis() - startTime;
            performanceLogger.info("⚡ All sizes retrieved in {}ms", duration);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("timestamp", LocalDateTime.now());
            response.put("executionTimeMs", duration);
            response.put("partitionSizes", allSizes);
            response.put("oversizedPartitions", oversizedPartitions);
            response.put("statistics", statistics);
            response.put("thresholds", getThresholdConfiguration());
            
            auditLogger.info("AUDIT: All partition sizes retrieved - {} partitions, {:.1f}MB total", 
                           allSizes.size(), statistics.getTotalSizeMB());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            performanceLogger.error("❌ All sizes retrieval failed after {}ms", duration);
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to get all partition sizes: " + e.getMessage(), duration));
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════════
    // STATISTICS AND MONITORING ENDPOINTS
    // ═══════════════════════════════════════════════════════════════════════════════════
    
    /**
     * GET /api/v1/smart-partitions/statistics
     * Get comprehensive partition statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<?> getPartitionStatistics() {
        long startTime = System.currentTimeMillis();
        apiLogger.info("📈 API Request: Get partition statistics");
        
        try {
            SmartPartitionService.SmartPartitionStatistics serviceStats = smartPartitionService.getServiceStatistics();
            PartitionSizeService.PartitionSizeStatistics sizeStats = partitionSizeService.getSizeStatistics();
            Map<String, Object> strategyStats = sizeBasedStrategy.getStrategyStatistics();
            
            long duration = System.currentTimeMillis() - startTime;
            performanceLogger.info("⚡ Statistics retrieved in {}ms", duration);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("timestamp", LocalDateTime.now());
            response.put("executionTimeMs", duration);
            response.put("serviceStatistics", serviceStats);
            response.put("sizeStatistics", sizeStats);
            response.put("strategyStatistics", strategyStats);
            response.put("configuration", getConfigurationSummary());
            
            auditLogger.info("AUDIT: Statistics retrieved - {} operations, {:.1f}% success rate", 
                           serviceStats.getTotalOperations(), serviceStats.getSuccessRate());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            performanceLogger.error("❌ Statistics retrieval failed after {}ms", duration);
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to get statistics: " + e.getMessage(), duration));
        }
    }
    
    /**
     * GET /api/v1/smart-partitions/config
     * Get current configuration
     */
    @GetMapping("/config")
    public ResponseEntity<?> getConfiguration() {
        long startTime = System.currentTimeMillis();
        apiLogger.info("⚙️ API Request: Get configuration");
        
        try {
            Map<String, Object> config = getConfigurationSummary();
            
            long duration = System.currentTimeMillis() - startTime;
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("timestamp", LocalDateTime.now());
            response.put("executionTimeMs", duration);
            response.put("configuration", config);
            
            auditLogger.info("AUDIT: Configuration retrieved");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to get configuration: " + e.getMessage(), duration));
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════════
    // ASYNC OPERATIONS
    // ═══════════════════════════════════════════════════════════════════════════════════
    
    /**
     * POST /api/v1/smart-partitions/async/check-and-create
     * Perform async smart partition check and creation
     */
    @PostMapping("/async/check-and-create")
    public ResponseEntity<?> checkAndCreatePartitionsAsync() {
        long startTime = System.currentTimeMillis();
        apiLogger.info("🚀 API Request: Async smart partition check and create");
        
        try {
            auditLogger.info("AUDIT: Async smart partition check initiated by API call");
            
            CompletableFuture<SmartPartitionService.SmartPartitionResult> future = 
                smartPartitionService.checkAndCreatePartitionsAsync();
            
            long duration = System.currentTimeMillis() - startTime;
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("timestamp", LocalDateTime.now());
            response.put("executionTimeMs", duration);
            response.put("status", "ASYNC_STARTED");
            response.put("message", "Smart partition check started asynchronously");
            
            // You could store the future and provide a way to check status
            // For now, just return immediate response
            
            auditLogger.info("AUDIT: Async smart partition check started");
            
            return ResponseEntity.accepted().body(response);
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            performanceLogger.error("❌ Async smart partition check failed to start after {}ms", duration);
            auditLogger.error("AUDIT: Async smart partition check failed to start: {}", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to start async operation: " + e.getMessage(), duration));
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════════
    // HEALTH AND STATUS ENDPOINTS
    // ═══════════════════════════════════════════════════════════════════════════════════
    
    /**
     * GET /api/v1/smart-partitions/health
     * Get health status of smart partition system
     */
    @GetMapping("/health")
    public ResponseEntity<?> getHealthStatus() {
        long startTime = System.currentTimeMillis();
        apiLogger.info("🏥 API Request: Health check");
        
        try {
            Map<String, Object> health = new HashMap<>();
            
            // Check service health
            SmartPartitionService.SmartPartitionStatistics stats = smartPartitionService.getServiceStatistics();
            boolean serviceHealthy = stats.getSuccessRate() > 80.0; // 80% success rate threshold
            
            // Check configuration
            boolean configHealthy = partitionConfig.isAutoCreateEnabled() && 
                                  partitionConfig.isSizeCheckEnabled();
            
            // Check recent operations
            boolean recentActivity = stats.getLastMaintenanceRun() != null && 
                                   stats.getLastMaintenanceRun().isAfter(LocalDateTime.now().minusHours(24));
            
            long duration = System.currentTimeMillis() - startTime;
            
            health.put("success", true);
            health.put("timestamp", LocalDateTime.now());
            health.put("executionTimeMs", duration);
            health.put("overallHealth", serviceHealthy && configHealthy ? "HEALTHY" : "DEGRADED");
            health.put("serviceHealth", serviceHealthy ? "HEALTHY" : "DEGRADED");
            health.put("configurationHealth", configHealthy ? "HEALTHY" : "DEGRADED");
            health.put("recentActivity", recentActivity);
            health.put("successRate", stats.getSuccessRate());
            health.put("totalOperations", stats.getTotalOperations());
            health.put("lastMaintenance", stats.getLastMaintenanceRun());
            health.put("maintenanceInProgress", stats.isMaintenanceInProgress());
            
            boolean overallHealthy = serviceHealthy && configHealthy;
            
            auditLogger.info("AUDIT: Health check - Overall: {}, Service: {}, Config: {}", 
                           overallHealthy ? "HEALTHY" : "DEGRADED",
                           serviceHealthy ? "HEALTHY" : "DEGRADED",
                           configHealthy ? "HEALTHY" : "DEGRADED");
            
            return overallHealthy ? ResponseEntity.ok(health) : 
                   ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(health);
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            performanceLogger.error("❌ Health check failed after {}ms", duration);
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Health check failed: " + e.getMessage(), duration));
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════════
    // UTILITY METHODS
    // ═══════════════════════════════════════════════════════════════════════════════════
    
    private Map<String, Object> createSuccessResponse(SmartPartitionService.SmartPartitionResult result, long duration) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", result.isSuccess());
        response.put("timestamp", LocalDateTime.now());
        response.put("executionTimeMs", duration);
        response.put("message", result.getMessage());
        response.put("createdPartitions", result.getCreatedPartitions());
        response.put("checkedPartitions", result.getCheckedPartitions());
        response.put("oversizedPartitions", result.getOversizedPartitions());
        response.put("errors", result.getErrors());
        response.put("recommendations", result.getRecommendations());
        
        return response;
    }
    
    private Map<String, Object> createErrorResponse(String message, long duration) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("timestamp", LocalDateTime.now());
        response.put("executionTimeMs", duration);
        response.put("error", message);
        
        return response;
    }
    
    private Map<String, Object> getConfigurationSummary() {
        Map<String, Object> config = new HashMap<>();
        config.put("warningThresholdMB", partitionConfig.getWarningThresholdMB());
        config.put("criticalThresholdMB", partitionConfig.getCriticalThresholdMB());
        config.put("emergencyThresholdMB", partitionConfig.getEmergencyThresholdMB());
        config.put("autoCreateEnabled", partitionConfig.isAutoCreateEnabled());
        config.put("sizeCheckEnabled", partitionConfig.isSizeCheckEnabled());
        config.put("autoSplitEnabled", partitionConfig.isAutoSplitEnabled());
        config.put("futureMonthsToCreate", partitionConfig.getFutureMonthsToCreate());
        config.put("retentionMonths", partitionConfig.getRetentionMonths());
        config.put("alertsEnabled", partitionConfig.isAlertsEnabled());
        
        return config;
    }
    
    private Map<String, Object> getThresholdConfiguration() {
        Map<String, Object> thresholds = new HashMap<>();
        thresholds.put("warning", partitionConfig.getWarningThresholdMB());
        thresholds.put("critical", partitionConfig.getCriticalThresholdMB());
        thresholds.put("emergency", partitionConfig.getEmergencyThresholdMB());
        
        return thresholds;
    }
    
    private String generateSizeRecommendation(String partitionName, double sizeMB, String thresholdLevel) {
        switch (thresholdLevel) {
            case "EMERGENCY":
                return "URGENT: Partition requires immediate splitting or archival. Size: " + sizeMB + "MB";
            case "CRITICAL":
                return "HIGH PRIORITY: Consider splitting partition soon. Size: " + sizeMB + "MB";
            case "WARNING":
                return "MEDIUM PRIORITY: Monitor partition growth closely. Size: " + sizeMB + "MB";
            default:
                return "No action required";
        }
    }
    
    private boolean isValidPartitionName(String partitionName) {
        return partitionName != null && partitionName.matches("p_\\d{6}(?:_[a-z])?");
    }
}