package com.GpsTracker.Thinture.partition.controller;

//================================================================================================
//PartitionController.java - COMPLETE MYSQL PARTITION API WITH SCHEDULER INTEGRATION
//================================================================================================

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.GpsTracker.Thinture.partition.dto.PartitionHealthDTO;
import com.GpsTracker.Thinture.partition.dto.PartitionInfoDTO;
import com.GpsTracker.Thinture.partition.dto.PartitionMetricsDTO;
import com.GpsTracker.Thinture.exception.PartitionException;
import com.GpsTracker.Thinture.partition.service.PartitionManagementService;
import com.GpsTracker.Thinture.partition.util.PartitionUtils;
import com.GpsTracker.Thinture.partition.scheduler.PartitionMaintenanceScheduler;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 🎯 Complete MySQL Partition Management API Controller
 * 🚀 Includes full scheduler integration and automation control
 */
@RestController
@RequestMapping("/api/v1/partitions")
@CrossOrigin(origins = "*")
public class PartitionController {

    private static final Logger logger = LoggerFactory.getLogger(PartitionController.class);
    private static final Logger apiLogger = LoggerFactory.getLogger("API." + PartitionController.class.getName());

    @Autowired
    private PartitionManagementService partitionManagementService;
    
    @Autowired
    private PartitionUtils partitionUtils;

    // ✅ NEW: Scheduler integration
    @Autowired
    private PartitionMaintenanceScheduler maintenanceScheduler;

    // ═══════════════════════════════════════════════════════════════════════════════════
    // PARTITION INFORMATION ENDPOINTS
    // ═══════════════════════════════════════════════════════════════════════════════════

    /**
     * GET /api/v1/partitions/list
     * Get all partitions with basic information
     */
    @GetMapping("/list")
    public ResponseEntity<?> getAllPartitions() {
        apiLogger.info("📋 API Request: Get all partitions");
        
        try {
            List<PartitionInfoDTO> partitions = partitionManagementService.getAllPartitions();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("timestamp", LocalDateTime.now());
            response.put("count", partitions.size());
            response.put("partitions", partitions);
            response.put("databaseType", "MYSQL");
            
            apiLogger.info("✅ Retrieved {} partitions", partitions.size());
            return ResponseEntity.ok(response);
            
        } catch (PartitionException e) {
            apiLogger.error("❌ Partition error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            apiLogger.error("❌ Unexpected error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Internal server error"));
        }
    }

    /**
     * GET /api/v1/partitions/{partitionName}/info
     * Get detailed information about a specific partition
     */
    @GetMapping("/{partitionName}/info")
    public ResponseEntity<?> getPartitionInfo(@PathVariable String partitionName) {
        apiLogger.info("📊 API Request: Get partition info for {}", partitionName);
        
        try {
            PartitionInfoDTO partitionInfo = partitionManagementService.getPartitionInfo(partitionName);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("timestamp", LocalDateTime.now());
            response.put("partition", partitionInfo);
            
            apiLogger.info("✅ Retrieved partition info for {}", partitionName);
            return ResponseEntity.ok(response);
            
        } catch (PartitionException e) {
            apiLogger.error("❌ Partition error for {}: {}", partitionName, e.getMessage());
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            apiLogger.error("❌ Unexpected error for {}: {}", partitionName, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Internal server error"));
        }
    }

    /**
     * GET /api/v1/partitions/{partitionName}/health
     * Get health status of a specific partition
     */
    @GetMapping("/{partitionName}/health")
    public ResponseEntity<?> getPartitionHealth(@PathVariable String partitionName) {
        apiLogger.info("🏥 API Request: Get partition health for {}", partitionName);
        
        try {
            PartitionHealthDTO partitionHealth = partitionManagementService.getPartitionHealth(partitionName);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("timestamp", LocalDateTime.now());
            response.put("health", partitionHealth);
            
            // Add status code based on health
            HttpStatus status = getHttpStatusFromHealth(partitionHealth.getHealthStatus());
            
            apiLogger.info("✅ Retrieved partition health for {} - Status: {}", 
                         partitionName, partitionHealth.getHealthStatus());
            return ResponseEntity.status(status).body(response);
            
        } catch (PartitionException e) {
            apiLogger.error("❌ Partition error for {}: {}", partitionName, e.getMessage());
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            apiLogger.error("❌ Unexpected error for {}: {}", partitionName, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Internal server error"));
        }
    }

    /**
     * GET /api/v1/partitions/{partitionName}/metrics
     * Get comprehensive metrics for a specific partition
     */
    @GetMapping("/{partitionName}/metrics")
    public ResponseEntity<?> getPartitionMetrics(@PathVariable String partitionName) {
        apiLogger.info("📈 API Request: Get partition metrics for {}", partitionName);
        
        try {
            PartitionMetricsDTO partitionMetrics = partitionManagementService.getPartitionMetrics(partitionName);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("timestamp", LocalDateTime.now());
            response.put("metrics", partitionMetrics);
            
            apiLogger.info("✅ Retrieved partition metrics for {}", partitionName);
            return ResponseEntity.ok(response);
            
        } catch (PartitionException e) {
            apiLogger.error("❌ Partition error for {}: {}", partitionName, e.getMessage());
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            apiLogger.error("❌ Unexpected error for {}: {}", partitionName, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Internal server error"));
        }
    }

    /**
     * GET /api/v1/partitions/status
     * Get overall partition system status
     */
    @GetMapping("/status")
    public ResponseEntity<?> getPartitionStatus() {
        apiLogger.info("📊 API Request: Get partition system status");
        
        try {
            List<PartitionInfoDTO> allPartitions = partitionManagementService.getAllPartitions();
            Map<String, Object> metrics = partitionUtils.getPartitionMetrics();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("timestamp", LocalDateTime.now());
            response.put("partitionCount", allPartitions.size());
            response.put("systemMetrics", metrics);
            response.put("databaseType", "MYSQL");
            response.put("tablePartitioned", partitionUtils.isTablePartitioned());
            
            // Add current partition status
            String currentPartition = partitionUtils.getCurrentPartitionName();
            response.put("currentPartition", currentPartition);
            response.put("currentPartitionExists", partitionUtils.partitionExists(currentPartition));
            
            // ✅ NEW: Add scheduler status
            response.put("schedulerConfig", maintenanceScheduler.getSchedulerConfiguration());
            
            apiLogger.info("✅ Retrieved system status - {} partitions", allPartitions.size());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            apiLogger.error("❌ Error getting system status: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to get system status"));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════════
    // PARTITION CREATION ENDPOINTS
    // ═══════════════════════════════════════════════════════════════════════════════════

    /**
     * POST /api/v1/partitions/create
     * Create a new partition
     */
    @PostMapping("/create")
    public ResponseEntity<?> createPartition(@RequestParam int year, @RequestParam int month) {
        apiLogger.info("🔨 API Request: Create partition for {}/{}", year, month);
        
        try {
            PartitionInfoDTO createdPartition = partitionManagementService.createPartition(year, month);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("timestamp", LocalDateTime.now());
            response.put("partition", createdPartition);
            response.put("message", "Partition created successfully");
            
            apiLogger.info("✅ Created partition for {}/{}: {}", year, month, createdPartition.getPartitionName());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (PartitionException e) {
            apiLogger.error("❌ Partition creation error for {}/{}: {}", year, month, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            apiLogger.error("❌ Unexpected error creating partition for {}/{}: {}", year, month, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to create partition"));
        }
    }

    /**
     * POST /api/v1/partitions/create-current
     * Create current month partition if missing
     */
    @PostMapping("/create-current")
    public ResponseEntity<?> createCurrentPartition() {
        apiLogger.info("🔨 API Request: Create current month partition");
        
        try {
            String currentPartition = partitionUtils.getCurrentPartitionName();
            
            if (partitionUtils.partitionExists(currentPartition)) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("timestamp", LocalDateTime.now());
                response.put("message", "Current partition already exists: " + currentPartition);
                response.put("partitionName", currentPartition);
                
                return ResponseEntity.ok(response);
            }
            
            boolean created = partitionUtils.createCurrentPartition();
            
            if (created) {
                PartitionInfoDTO partitionInfo = partitionManagementService.getPartitionInfo(currentPartition);
                
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("timestamp", LocalDateTime.now());
                response.put("partition", partitionInfo);
                response.put("message", "Current partition created successfully");
                
                apiLogger.info("✅ Created current partition: {}", currentPartition);
                return ResponseEntity.status(HttpStatus.CREATED).body(response);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(createErrorResponse("Failed to create current partition"));
            }
            
        } catch (Exception e) {
            apiLogger.error("❌ Error creating current partition: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to create current partition"));
        }
    }

    /**
     * POST /api/v1/partitions/create-future
     * Create future partitions
     */
    @PostMapping("/create-future")
    public ResponseEntity<?> createFuturePartitions(@RequestParam(defaultValue = "3") int months) {
        apiLogger.info("🔨 API Request: Create {} future partitions", months);
        
        try {
            List<String> createdPartitions = partitionUtils.createFuturePartitions(months);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("timestamp", LocalDateTime.now());
            response.put("requestedMonths", months);
            response.put("createdPartitions", createdPartitions);
            response.put("createdCount", createdPartitions.size());
            response.put("message", "Future partitions created successfully");
            
            apiLogger.info("✅ Created {} future partitions: {}", createdPartitions.size(), createdPartitions);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (Exception e) {
            apiLogger.error("❌ Error creating future partitions: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to create future partitions"));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════════
    // PARTITION MAINTENANCE ENDPOINTS
    // ═══════════════════════════════════════════════════════════════════════════════════

    /**
     * POST /api/v1/partitions/{partitionName}/optimize
     * Optimize a specific partition
     */
    @PostMapping("/{partitionName}/optimize")
    public ResponseEntity<?> optimizePartition(@PathVariable String partitionName) {
        apiLogger.info("⚡ API Request: Optimize partition {}", partitionName);
        
        try {
            Map<String, Object> result = partitionManagementService.optimizePartition(partitionName);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("timestamp", LocalDateTime.now());
            response.put("partitionName", partitionName);
            response.put("result", result);
            response.put("message", "Partition optimized successfully");
            
            apiLogger.info("✅ Optimized partition: {}", partitionName);
            return ResponseEntity.ok(response);
            
        } catch (PartitionException e) {
            apiLogger.error("❌ Partition optimization error for {}: {}", partitionName, e.getMessage());
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            apiLogger.error("❌ Unexpected error optimizing {}: {}", partitionName, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to optimize partition"));
        }
    }

    /**
     * POST /api/v1/partitions/{partitionName}/analyze
     * Analyze a specific partition
     */
    @PostMapping("/{partitionName}/analyze")
    public ResponseEntity<?> analyzePartition(@PathVariable String partitionName) {
        apiLogger.info("🔍 API Request: Analyze partition {}", partitionName);
        
        try {
            Map<String, Object> result = partitionManagementService.analyzePartition(partitionName);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("timestamp", LocalDateTime.now());
            response.put("partitionName", partitionName);
            response.put("result", result);
            response.put("message", "Partition analyzed successfully");
            
            apiLogger.info("✅ Analyzed partition: {}", partitionName);
            return ResponseEntity.ok(response);
            
        } catch (PartitionException e) {
            apiLogger.error("❌ Partition analysis error for {}: {}", partitionName, e.getMessage());
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            apiLogger.error("❌ Unexpected error analyzing {}: {}", partitionName, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to analyze partition"));
        }
    }

    /**
     * POST /api/v1/partitions/maintenance
     * Run automated maintenance tasks
     */
    @PostMapping("/maintenance")
    public ResponseEntity<?> runMaintenance() {
        apiLogger.info("🔧 API Request: Run automated maintenance");
        
        try {
            Map<String, Object> result = partitionUtils.runAutomatedMaintenance();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("timestamp", LocalDateTime.now());
            response.put("maintenanceResult", result);
            response.put("message", "Automated maintenance completed successfully");
            
            apiLogger.info("✅ Automated maintenance completed");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            apiLogger.error("❌ Error running automated maintenance: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to run automated maintenance"));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════════
    // PARTITION DELETION ENDPOINTS
    // ═══════════════════════════════════════════════════════════════════════════════════

    /**
     * DELETE /api/v1/partitions/{partitionName}
     * Drop a specific partition
     */
    @DeleteMapping("/{partitionName}")
    public ResponseEntity<?> dropPartition(@PathVariable String partitionName,
                                         @RequestParam(defaultValue = "false") boolean force) {
        apiLogger.warn("🗑️ API Request: Drop partition {} (force={})", partitionName, force);
        
        try {
            boolean dropped = partitionManagementService.dropPartition(partitionName, force);
            
            if (dropped) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("timestamp", LocalDateTime.now());
                response.put("partitionName", partitionName);
                response.put("message", "Partition dropped successfully");
                
                apiLogger.warn("✅ Dropped partition: {}", partitionName);
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(createErrorResponse("Failed to drop partition"));
            }
            
        } catch (PartitionException e) {
            apiLogger.error("❌ Partition drop error for {}: {}", partitionName, e.getMessage());
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            apiLogger.error("❌ Unexpected error dropping {}: {}", partitionName, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to drop partition"));
        }
    }

    /**
     * POST /api/v1/partitions/cleanup
     * Cleanup old partitions based on retention policy
     */
    @PostMapping("/cleanup")
    public ResponseEntity<?> cleanupOldPartitions(@RequestParam(defaultValue = "12") int retentionMonths) {
        apiLogger.info("🧹 API Request: Cleanup partitions older than {} months", retentionMonths);
        
        try {
            Map<String, Object> result = partitionUtils.cleanupOldPartitions(retentionMonths);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("timestamp", LocalDateTime.now());
            response.put("cleanupResult", result);
            response.put("message", "Partition cleanup completed successfully");
            
            @SuppressWarnings("unchecked")
            List<String> droppedPartitions = (List<String>) result.get("droppedPartitions");
            
            apiLogger.info("✅ Cleanup completed: dropped {} partitions", droppedPartitions.size());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            apiLogger.error("❌ Error during partition cleanup: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to cleanup partitions"));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════════
    // ✅ NEW: SCHEDULER MANAGEMENT ENDPOINTS
    // ═══════════════════════════════════════════════════════════════════════════════════

    /**
     * GET /api/v1/partitions/scheduler/config
     * Get scheduler configuration and status
     */
    @GetMapping("/scheduler/config")
    public ResponseEntity<?> getSchedulerConfig() {
        apiLogger.info("⏰ API Request: Get scheduler configuration");
        
        try {
            Map<String, Object> config = maintenanceScheduler.getSchedulerConfiguration();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("timestamp", LocalDateTime.now());
            response.put("schedulerConfig", config);
            response.put("message", "Scheduler configuration retrieved successfully");
            
            apiLogger.info("✅ Retrieved scheduler configuration");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            apiLogger.error("❌ Error getting scheduler config: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to get scheduler configuration"));
        }
    }

    /**
     * POST /api/v1/partitions/scheduler/trigger/daily
     * Manually trigger daily maintenance
     */
    @PostMapping("/scheduler/trigger/daily")
    public ResponseEntity<?> triggerDailyMaintenance() {
        apiLogger.info("⏰ API Request: Trigger daily maintenance");
        
        try {
            maintenanceScheduler.triggerDailyMaintenance();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("timestamp", LocalDateTime.now());
            response.put("triggerType", "DAILY_MAINTENANCE");
            response.put("message", "Daily maintenance triggered successfully");
            response.put("description", "Ensures current and future partitions exist");
            
            apiLogger.info("✅ Daily maintenance triggered manually");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            apiLogger.error("❌ Error triggering daily maintenance: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to trigger daily maintenance"));
        }
    }

    /**
     * POST /api/v1/partitions/scheduler/trigger/weekly
     * Manually trigger weekly optimization
     */
    @PostMapping("/scheduler/trigger/weekly")
    public ResponseEntity<?> triggerWeeklyOptimization() {
        apiLogger.info("⏰ API Request: Trigger weekly optimization");
        
        try {
            maintenanceScheduler.triggerWeeklyOptimization();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("timestamp", LocalDateTime.now());
            response.put("triggerType", "WEEKLY_OPTIMIZATION");
            response.put("message", "Weekly optimization triggered successfully");
            response.put("description", "Optimizes recent partitions for better performance");
            
            apiLogger.info("✅ Weekly optimization triggered manually");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            apiLogger.error("❌ Error triggering weekly optimization: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to trigger weekly optimization"));
        }
    }

    /**
     * POST /api/v1/partitions/scheduler/trigger/cleanup
     * Manually trigger monthly cleanup
     */
    @PostMapping("/scheduler/trigger/cleanup")
    public ResponseEntity<?> triggerMonthlyCleanup() {
        apiLogger.info("⏰ API Request: Trigger monthly cleanup");
        
        try {
            maintenanceScheduler.triggerMonthlyCleanup();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("timestamp", LocalDateTime.now());
            response.put("triggerType", "MONTHLY_CLEANUP");
            response.put("message", "Monthly cleanup triggered successfully");
            response.put("description", "Removes old partitions based on retention policy");
            
            apiLogger.info("✅ Monthly cleanup triggered manually");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            apiLogger.error("❌ Error triggering monthly cleanup: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to trigger monthly cleanup"));
        }
    }

    /**
     * GET /api/v1/partitions/scheduler/status
     * Get detailed scheduler status and next run times
     */
    @GetMapping("/scheduler/status")
    public ResponseEntity<?> getSchedulerStatus() {
        apiLogger.info("⏰ API Request: Get scheduler status");
        
        try {
            Map<String, Object> config = maintenanceScheduler.getSchedulerConfiguration();
            
            // Build detailed status information
            Map<String, Object> scheduleInfo = new HashMap<>();
            scheduleInfo.put("dailyCheck", Map.of(
                "description", "Ensures current and future partitions exist",
                "schedule", "Every day at 2:00 AM",
                "enabled", true
            ));
            scheduleInfo.put("dailyMetrics", Map.of(
                "description", "Generates daily partition metrics report",
                "schedule", "Every day at 6:00 AM",
                "enabled", true
            ));
            scheduleInfo.put("weeklyOptimization", Map.of(
                "description", "Optimizes recent partitions",
                "schedule", "Every Sunday at 3:00 AM",
                "enabled", true
            ));
            scheduleInfo.put("weeklyAnalysis", Map.of(
                "description", "Analyzes partition statistics",
                "schedule", "Every Sunday at 4:00 AM",
                "enabled", true
            ));
            scheduleInfo.put("monthlyCreation", Map.of(
                "description", "Creates next month's partitions",
                "schedule", "1st day of every month at 1:00 AM",
                "enabled", true
            ));
            scheduleInfo.put("monthlyCleanup", Map.of(
                "description", "Removes old partitions",
                "schedule", "2nd day of every month at 2:00 AM",
                "enabled", config.get("autoCleanupEnabled")
            ));
            scheduleInfo.put("monthlyReport", Map.of(
                "description", "Generates comprehensive monthly report",
                "schedule", "3rd day of every month at 9:00 AM",
                "enabled", true
            ));
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("timestamp", LocalDateTime.now());
            response.put("schedulerConfig", config);
            response.put("scheduleDetails", scheduleInfo);
            response.put("message", "Scheduler status retrieved successfully");
            
            apiLogger.info("✅ Retrieved detailed scheduler status");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            apiLogger.error("❌ Error getting scheduler status: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to get scheduler status"));
        }
    }

    /**
     * POST /api/v1/partitions/scheduler/trigger/all
     * Trigger all maintenance tasks (daily, weekly, monthly) - USE WITH CAUTION
     */
    @PostMapping("/scheduler/trigger/all")
    public ResponseEntity<?> triggerAllMaintenance(@RequestParam(defaultValue = "false") boolean confirmAll) {
        apiLogger.warn("⚠️ API Request: Trigger ALL maintenance tasks (confirm={})", confirmAll);
        
        if (!confirmAll) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("timestamp", LocalDateTime.now());
            response.put("error", "Confirmation required");
            response.put("message", "Add parameter ?confirmAll=true to execute all maintenance tasks");
            response.put("warning", "This will run daily, weekly, and monthly maintenance all at once");
            
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
        
        try {
            Map<String, Object> results = new HashMap<>();
            
            // Execute all maintenance tasks
            maintenanceScheduler.triggerDailyMaintenance();
            results.put("dailyMaintenance", "Completed");
            
            maintenanceScheduler.triggerWeeklyOptimization();
            results.put("weeklyOptimization", "Completed");
            
            maintenanceScheduler.triggerMonthlyCleanup();
            results.put("monthlyCleanup", "Completed");
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("timestamp", LocalDateTime.now());
            response.put("triggerType", "ALL_MAINTENANCE");
            response.put("results", results);
            response.put("message", "All maintenance tasks triggered successfully");
            response.put("warning", "This was a comprehensive maintenance run");
            
            apiLogger.warn("✅ ALL maintenance tasks triggered manually - COMPREHENSIVE RUN");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            apiLogger.error("❌ Error triggering all maintenance: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to trigger all maintenance tasks"));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════════
    // UTILITY ENDPOINTS
    // ═══════════════════════════════════════════════════════════════════════════════════

    /**
     * GET /api/v1/partitions/test-connection
     * Test database connection
     */
    @GetMapping("/test-connection")
    public ResponseEntity<?> testConnection() {
        apiLogger.info("🔌 API Request: Test database connection");
        
        try {
            Map<String, Object> result = partitionUtils.testConnection();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("timestamp", LocalDateTime.now());
            response.put("connectionTest", result);
            
            boolean connected = (Boolean) result.get("connected");
            
            if (connected) {
                apiLogger.info("✅ Database connection test successful");
                return ResponseEntity.ok(response);
            } else {
                apiLogger.error("❌ Database connection test failed");
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
            }
            
        } catch (Exception e) {
            apiLogger.error("❌ Error testing connection: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to test connection"));
        }
    }

    /**
     * GET /api/v1/partitions/database-info
     * Get database information
     */
    @GetMapping("/database-info")
    public ResponseEntity<?> getDatabaseInfo() {
        apiLogger.info("ℹ️ API Request: Get database information");
        
        try {
            Map<String, Object> result = partitionUtils.getDatabaseInfo();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("timestamp", LocalDateTime.now());
            response.put("databaseInfo", result);
            
            apiLogger.info("✅ Retrieved database information");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            apiLogger.error("❌ Error getting database info: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to get database information"));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════════
    // UTILITY METHODS
    // ═══════════════════════════════════════════════════════════════════════════════════

    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("timestamp", LocalDateTime.now());
        response.put("error", message);
        response.put("databaseType", "MYSQL");
        return response;
    }

    private HttpStatus getHttpStatusFromHealth(String healthStatus) {
        return switch (healthStatus.toUpperCase()) {
            case "CRITICAL" -> HttpStatus.SERVICE_UNAVAILABLE;
            case "WARNING", "LARGE" -> HttpStatus.ACCEPTED;
            case "EMPTY" -> HttpStatus.NO_CONTENT;
            default -> HttpStatus.OK;
        };
    }
}