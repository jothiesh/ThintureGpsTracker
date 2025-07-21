// ================================================================================================
// SmartPartitionService.java - INTELLIGENT PARTITION MANAGEMENT SERVICE
// ================================================================================================

package com.GpsTracker.Thinture.partition.service;

import com.GpsTracker.Thinture.partition.config.PartitionConfig;
import com.GpsTracker.Thinture.partition.util.PartitionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 🧠 Smart Partition Management Service
 * Coordinates intelligent partition creation, monitoring, and management
 */
@Service
public class SmartPartitionService {
    
    private static final Logger logger = LoggerFactory.getLogger(SmartPartitionService.class);
    private static final Logger smartLogger = LoggerFactory.getLogger("SMART." + SmartPartitionService.class.getName());
    private static final Logger alertLogger = LoggerFactory.getLogger("ALERT." + SmartPartitionService.class.getName());
    private static final Logger performanceLogger = LoggerFactory.getLogger("PERFORMANCE." + SmartPartitionService.class.getName());
    
    @Autowired
    private PartitionUtils partitionUtils;
    
    @Autowired
    private PartitionConfig partitionConfig;
    
    @Autowired
    private PartitionSizeService partitionSizeService;
    
    // Performance tracking
    private final AtomicLong totalOperations = new AtomicLong(0);
    private final AtomicLong successfulOperations = new AtomicLong(0);
    private final AtomicLong failedOperations = new AtomicLong(0);
    private final Map<String, Long> operationTimes = new ConcurrentHashMap<>();
    
    // State tracking
    private volatile LocalDateTime lastMaintenanceRun;
    private volatile boolean maintenanceInProgress = false;
    
    // ═══════════════════════════════════════════════════════════════════════════════════
    // INITIALIZATION
    // ═══════════════════════════════════════════════════════════════════════════════════
    
    @PostConstruct
    public void initializeService() {
        smartLogger.info("🚀 Initializing Smart Partition Service...");
        
        try {
            // Log configuration
            logServiceConfiguration();
            
            // Initial health check
            performInitialHealthCheck();
            
            smartLogger.info("✅ Smart Partition Service initialized successfully");
            
        } catch (Exception e) {
            smartLogger.error("❌ Failed to initialize Smart Partition Service: {}", e.getMessage(), e);
        }
    }
    
    private void logServiceConfiguration() {
        smartLogger.info("⚙️ Smart Partition Service Configuration:");
        smartLogger.info("  Auto-create: {}", partitionConfig.isAutoCreateEnabled());
        smartLogger.info("  Size check: {}", partitionConfig.isSizeCheckEnabled());
        smartLogger.info("  Auto-split: {}", partitionConfig.isAutoSplitEnabled());
        smartLogger.info("  Critical threshold: {}MB", partitionConfig.getCriticalThresholdMB());
        smartLogger.info("  Future months: {}", partitionConfig.getFutureMonthsToCreate());
    }
    
    private void performInitialHealthCheck() {
        smartLogger.info("🏥 Performing initial partition health check...");
        
        try {
            String currentPartition = partitionUtils.getCurrentPartitionName();
            boolean exists = partitionUtils.partitionExists(currentPartition);
            
            if (!exists) {
                alertLogger.error("🚨 CRITICAL: Current partition {} does not exist!", currentPartition);
                if (partitionConfig.isAutoCreateEnabled()) {
                    smartLogger.info("🔧 Auto-creating missing current partition...");
                    createCurrentPartitionIfNeeded();
                }
            } else {
                smartLogger.info("✅ Current partition {} exists", currentPartition);
            }
            
        } catch (Exception e) {
            smartLogger.error("❌ Initial health check failed: {}", e.getMessage(), e);
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════════
    // MAIN SMART PARTITION OPERATIONS
    // ═══════════════════════════════════════════════════════════════════════════════════
    
    /**
     * Main entry point for smart partition management
     */
    public SmartPartitionResult checkAndCreatePartitions() {
        smartLogger.info("🧠 Starting smart partition check and creation...");
        
        if (maintenanceInProgress) {
            smartLogger.warn("⚠️ Maintenance already in progress, skipping");
            return SmartPartitionResult.skipped("Maintenance already in progress");
        }
        
        long startTime = System.currentTimeMillis();
        totalOperations.incrementAndGet();
        
        try {
            maintenanceInProgress = true;
            SmartPartitionResult result = new SmartPartitionResult();
            
            // Step 1: Check current partition
            handleCurrentPartition(result);
            
            // Step 2: Check size-based requirements
            if (partitionConfig.isSizeCheckEnabled()) {
                handleSizeBasedOperations(result);
            }
            
            // Step 3: Ensure future partitions exist
            if (partitionConfig.isAutoCreateEnabled()) {
                handleFuturePartitions(result);
            }
            
            // Step 4: Performance optimization
            handlePerformanceOptimization(result);
            
            long duration = System.currentTimeMillis() - startTime;
            result.setExecutionTimeMs(duration);
            result.setSuccess(true);
            
            successfulOperations.incrementAndGet();
            lastMaintenanceRun = LocalDateTime.now();
            
            smartLogger.info("✅ Smart partition check completed in {}ms", duration);
            logOperationResult(result);
            
            return result;
            
        } catch (Exception e) {
            failedOperations.incrementAndGet();
            long duration = System.currentTimeMillis() - startTime;
            
            smartLogger.error("❌ Smart partition check failed after {}ms: {}", duration, e.getMessage(), e);
            return SmartPartitionResult.failed(e.getMessage(), duration);
            
        } finally {
            maintenanceInProgress = false;
        }
    }
    
    /**
     * Handle current partition checks and creation
     */
    private void handleCurrentPartition(SmartPartitionResult result) {
        smartLogger.debug("🔍 Checking current partition status...");
        
        try {
            String currentPartition = partitionUtils.getCurrentPartitionName();
            boolean exists = partitionUtils.partitionExists(currentPartition);
            
            if (!exists) {
                alertLogger.error("🚨 CRITICAL: Current partition {} missing!", currentPartition);
                
                if (partitionConfig.isAutoCreateEnabled()) {
                    boolean created = createCurrentPartitionIfNeeded();
                    if (created) {
                        result.addCreatedPartition(currentPartition);
                        smartLogger.info("✅ Auto-created missing current partition: {}", currentPartition);
                    } else {
                        result.addError("Failed to create current partition: " + currentPartition);
                        alertLogger.error("❌ Failed to auto-create current partition: {}", currentPartition);
                    }
                } else {
                    result.addError("Current partition missing and auto-create disabled: " + currentPartition);
                }
            } else {
                smartLogger.debug("✅ Current partition {} exists", currentPartition);
                result.addCheckedPartition(currentPartition);
            }
            
        } catch (Exception e) {
            String error = "Error checking current partition: " + e.getMessage();
            result.addError(error);
            smartLogger.error("❌ {}", error, e);
        }
    }
    
    /**
     * Handle size-based partition operations
     */
    private void handleSizeBasedOperations(SmartPartitionResult result) {
        smartLogger.debug("📏 Checking size-based partition requirements...");
        
        try {
            Map<String, String> oversizedPartitions = partitionSizeService.findOversizedPartitions();
            
            if (oversizedPartitions.isEmpty()) {
                smartLogger.debug("✅ No oversized partitions found");
                return;
            }
            
            smartLogger.warn("⚠️ Found {} oversized partitions", oversizedPartitions.size());
            
            for (Map.Entry<String, String> entry : oversizedPartitions.entrySet()) {
                String partitionName = entry.getKey();
                String thresholdLevel = entry.getValue();
                
                result.addOversizedPartition(partitionName, thresholdLevel);
                
                if ("CRITICAL".equals(thresholdLevel) || "EMERGENCY".equals(thresholdLevel)) {
                    alertLogger.error("🚨 {} partition requires immediate attention: {}", thresholdLevel, partitionName);
                    
                    if (partitionConfig.isAutoSplitEnabled()) {
                        handlePartitionSplit(partitionName, result);
                    } else {
                        result.addRecommendation("Consider splitting partition: " + partitionName);
                    }
                }
            }
            
        } catch (Exception e) {
            String error = "Error in size-based operations: " + e.getMessage();
            result.addError(error);
            smartLogger.error("❌ {}", error, e);
        }
    }
    
    /**
     * Handle future partition creation
     */
    private void handleFuturePartitions(SmartPartitionResult result) {
        smartLogger.debug("📅 Ensuring future partitions exist...");
        
        try {
            int futureMonths = partitionConfig.getFutureMonthsToCreate();
            List<String> createdPartitions = partitionUtils.createFuturePartitions(futureMonths);
            
            if (!createdPartitions.isEmpty()) {
                result.getCreatedPartitions().addAll(createdPartitions);
                smartLogger.info("✅ Created {} future partitions: {}", createdPartitions.size(), createdPartitions);
            } else {
                smartLogger.debug("✅ All required future partitions already exist");
            }
            
        } catch (Exception e) {
            String error = "Error creating future partitions: " + e.getMessage();
            result.addError(error);
            smartLogger.error("❌ {}", error, e);
        }
    }
    
    /**
     * Handle performance optimization
     */
    private void handlePerformanceOptimization(SmartPartitionResult result) {
        smartLogger.debug("⚡ Checking for performance optimization opportunities...");
        
        try {
            // This is a placeholder for future performance optimization logic
            // Could include:
            // - Index optimization
            // - Statistics updates
            // - Compression recommendations
            // - Query performance analysis
            
            smartLogger.debug("✅ Performance optimization check completed");
            
        } catch (Exception e) {
            String error = "Error in performance optimization: " + e.getMessage();
            result.addError(error);
            smartLogger.error("❌ {}", error, e);
        }
    }
    
    /**
     * Handle partition splitting (placeholder for future implementation)
     */
    private void handlePartitionSplit(String partitionName, SmartPartitionResult result) {
        smartLogger.warn("✂️ Partition split requested for: {}", partitionName);
        
        // For now, this is a placeholder
        // Future implementation would:
        // 1. Create new sub-partition
        // 2. Move recent data to new partition
        // 3. Update indexes
        // 4. Update application routing
        
        result.addRecommendation("Partition split recommended for: " + partitionName);
        smartLogger.info("📝 Added split recommendation for partition: {}", partitionName);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════════
    // INDIVIDUAL PARTITION OPERATIONS
    // ═══════════════════════════════════════════════════════════════════════════════════
    
    /**
     * Create current partition if needed
     */
    public boolean createCurrentPartitionIfNeeded() {
        smartLogger.info("🔧 Creating current partition if needed...");
        
        try {
            String currentPartition = partitionUtils.getCurrentPartitionName();
            
            if (partitionUtils.partitionExists(currentPartition)) {
                smartLogger.debug("✅ Current partition {} already exists", currentPartition);
                return true;
            }
            
            smartLogger.info("🔨 Creating current partition: {}", currentPartition);
            boolean created = partitionUtils.createCurrentPartition();
            
            if (created) {
                smartLogger.info("✅ Successfully created current partition: {}", currentPartition);
                return true;
            } else {
                smartLogger.error("❌ Failed to create current partition: {}", currentPartition);
                return false;
            }
            
        } catch (Exception e) {
            smartLogger.error("❌ Error creating current partition: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Check specific partition size and take action if needed
     */
    /**
     * Check specific partition size and take action if needed
     */
    public boolean checkPartitionSize(String partitionName) {
        smartLogger.debug("📏 Checking size for partition: {}", partitionName);

        try {
            if (!partitionUtils.partitionExists(partitionName)) {
                smartLogger.warn("⚠️ Partition does not exist: {}", partitionName);
                return false;
            }

            double sizeMB = partitionSizeService.getPartitionSizeMB(partitionName);
            String thresholdLevel = partitionConfig.getThresholdInfo(sizeMB).getLevel();

            smartLogger.debug("📊 Partition {} size: {:.1f}MB ({})", partitionName, sizeMB, thresholdLevel);

            switch (thresholdLevel) {
                case "EMERGENCY":
                    alertLogger.error("🆘 EMERGENCY: Partition {} at {:.1f}MB requires immediate action!", 
                                     partitionName, sizeMB);
                    return false;

                case "CRITICAL":
                    alertLogger.error("🚨 CRITICAL: Partition {} at {:.1f}MB exceeds critical threshold!", 
                                     partitionName, sizeMB);
                    return false;

                case "WARNING":
                    smartLogger.warn("⚠️ WARNING: Partition {} at {:.1f}MB approaching limits", 
                                    partitionName, sizeMB);
                    return true;

                default:
                    smartLogger.debug("✅ Partition {} within normal size limits", partitionName);
                    return true;
            }

        } catch (Exception e) {
            smartLogger.error("❌ Error checking partition size for {}: {}", partitionName, e.getMessage(), e);
            return false;
        }
    }

    
    // ═══════════════════════════════════════════════════════════════════════════════════
    // ASYNC OPERATIONS
    // ═══════════════════════════════════════════════════════════════════════════════════
    
    /**
     * Perform smart partition check asynchronously
     */
    public CompletableFuture<SmartPartitionResult> checkAndCreatePartitionsAsync() {
        smartLogger.info("🚀 Starting async smart partition check...");
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                return checkAndCreatePartitions();
            } catch (Exception e) {
                smartLogger.error("❌ Async smart partition check failed: {}", e.getMessage(), e);
                return SmartPartitionResult.failed(e.getMessage(), 0);
            }
        });
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════════
    // MONITORING AND REPORTING
    // ═══════════════════════════════════════════════════════════════════════════════════
    
    /**
     * Get service statistics
     */
    public SmartPartitionStatistics getServiceStatistics() {
        smartLogger.debug("📊 Calculating service statistics...");
        
        try {
            SmartPartitionStatistics stats = new SmartPartitionStatistics();
            stats.setTotalOperations(totalOperations.get());
            stats.setSuccessfulOperations(successfulOperations.get());
            stats.setFailedOperations(failedOperations.get());
            stats.setLastMaintenanceRun(lastMaintenanceRun);
            stats.setMaintenanceInProgress(maintenanceInProgress);
            
            if (totalOperations.get() > 0) {
                double successRate = (double) successfulOperations.get() / totalOperations.get() * 100;
                stats.setSuccessRate(successRate);
            }
            
            // Add partition statistics
            PartitionSizeService.PartitionSizeStatistics sizeStats = partitionSizeService.getSizeStatistics();
            stats.setTotalPartitions(sizeStats.getTotalPartitions());
            stats.setTotalSizeMB(sizeStats.getTotalSizeMB());
            
            return stats;
            
        } catch (Exception e) {
            smartLogger.error("❌ Error calculating service statistics: {}", e.getMessage(), e);
            return new SmartPartitionStatistics();
        }
    }
    
    /**
     * Log operation result
     */
    private void logOperationResult(SmartPartitionResult result) {
        if (result.isSuccess()) {
            smartLogger.info("📊 Operation Summary:");
            smartLogger.info("  Created partitions: {}", result.getCreatedPartitions().size());
            smartLogger.info("  Checked partitions: {}", result.getCheckedPartitions().size());
            smartLogger.info("  Oversized partitions: {}", result.getOversizedPartitions().size());
            smartLogger.info("  Recommendations: {}", result.getRecommendations().size());
            smartLogger.info("  Execution time: {}ms", result.getExecutionTimeMs());
            
            if (!result.getErrors().isEmpty()) {
                smartLogger.warn("⚠️ Errors encountered: {}", result.getErrors().size());
                result.getErrors().forEach(error -> smartLogger.warn("  - {}", error));
            }
        }
    }
    
    /**
     * Log current service status
     */
    public void logServiceStatus() {
        smartLogger.info("📊 === SMART PARTITION SERVICE STATUS ===");

        try {
            SmartPartitionStatistics stats = getServiceStatistics();

            smartLogger.info("🔧 Service Statistics:");
            smartLogger.info("  Total Operations: {}", stats.getTotalOperations());

            String successRateFormatted = String.format("%.1f%%", stats.getSuccessRate());
            smartLogger.info("  Success Rate: {}", successRateFormatted);

            smartLogger.info("  Last Maintenance: {}", stats.getLastMaintenanceRun());
            smartLogger.info("  Maintenance In Progress: {}", stats.isMaintenanceInProgress());

            smartLogger.info("📊 Partition Statistics:");
            smartLogger.info("  Total Partitions: {}", stats.getTotalPartitions());

            String totalSizeFormatted = String.format("%.1fMB", stats.getTotalSizeMB());
            smartLogger.info("  Total Size: {}", totalSizeFormatted);

            // Log configuration status
            partitionConfig.logCurrentStatus();

            smartLogger.info("==========================================");

        } catch (Exception e) {
            smartLogger.error("❌ Error logging service status: {}", e.getMessage(), e);
        }
    }


    
    // ═══════════════════════════════════════════════════════════════════════════════════
    // INNER CLASSES
    // ═══════════════════════════════════════════════════════════════════════════════════
    
    /**
     * Smart partition operation result
     */
    public static class SmartPartitionResult {
        private boolean success;
        private String message;
        private long executionTimeMs;
        private List<String> createdPartitions = new ArrayList<>();
        private List<String> checkedPartitions = new ArrayList<>();
        private Map<String, String> oversizedPartitions = new HashMap<>();
        private List<String> errors = new ArrayList<>();
        private List<String> recommendations = new ArrayList<>();
        
        public static SmartPartitionResult failed(String message, long executionTime) {
            SmartPartitionResult result = new SmartPartitionResult();
            result.success = false;
            result.message = message;
            result.executionTimeMs = executionTime;
            return result;
        }
        
        public static SmartPartitionResult skipped(String message) {
            SmartPartitionResult result = new SmartPartitionResult();
            result.success = true;
            result.message = message;
            return result;
        }
        
        // Helper methods
        public void addCreatedPartition(String partition) { createdPartitions.add(partition); }
        public void addCheckedPartition(String partition) { checkedPartitions.add(partition); }
        public void addOversizedPartition(String partition, String level) { oversizedPartitions.put(partition, level); }
        public void addError(String error) { errors.add(error); }
        public void addRecommendation(String recommendation) { recommendations.add(recommendation); }
        
        // Getters and setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public long getExecutionTimeMs() { return executionTimeMs; }
        public void setExecutionTimeMs(long executionTimeMs) { this.executionTimeMs = executionTimeMs; }
        public List<String> getCreatedPartitions() { return createdPartitions; }
        public List<String> getCheckedPartitions() { return checkedPartitions; }
        public Map<String, String> getOversizedPartitions() { return oversizedPartitions; }
        public List<String> getErrors() { return errors; }
        public List<String> getRecommendations() { return recommendations; }
    }
    
    /**
     * Service statistics
     */
    public static class SmartPartitionStatistics {
        private long totalOperations;
        private long successfulOperations;
        private long failedOperations;
        private double successRate;
        private LocalDateTime lastMaintenanceRun;
        private boolean maintenanceInProgress;
        private int totalPartitions;
        private double totalSizeMB;
        
        // Getters and setters
        public long getTotalOperations() { return totalOperations; }
        public void setTotalOperations(long totalOperations) { this.totalOperations = totalOperations; }
        public long getSuccessfulOperations() { return successfulOperations; }
        public void setSuccessfulOperations(long successfulOperations) { this.successfulOperations = successfulOperations; }
        public long getFailedOperations() { return failedOperations; }
        public void setFailedOperations(long failedOperations) { this.failedOperations = failedOperations; }
        public double getSuccessRate() { return successRate; }
        public void setSuccessRate(double successRate) { this.successRate = successRate; }
        public LocalDateTime getLastMaintenanceRun() { return lastMaintenanceRun; }
        public void setLastMaintenanceRun(LocalDateTime lastMaintenanceRun) { this.lastMaintenanceRun = lastMaintenanceRun; }
        public boolean isMaintenanceInProgress() { return maintenanceInProgress; }
        public void setMaintenanceInProgress(boolean maintenanceInProgress) { this.maintenanceInProgress = maintenanceInProgress; }
        public int getTotalPartitions() { return totalPartitions; }
        public void setTotalPartitions(int totalPartitions) { this.totalPartitions = totalPartitions; }
        public double getTotalSizeMB() { return totalSizeMB; }
        public void setTotalSizeMB(double totalSizeMB) { this.totalSizeMB = totalSizeMB; }
    }
}