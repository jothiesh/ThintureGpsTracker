package com.GpsTracker.Thinture.partition.scheduler;

//================================================================================================
//PartitionMaintenanceScheduler.java - AUTOMATED PARTITION MAINTENANCE TASKS
//================================================================================================



import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.GpsTracker.Thinture.partition.util.PartitionUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
* Scheduled tasks for automatic partition maintenance
*/
@Component
@EnableScheduling
@ConditionalOnProperty(name = "gps.partition.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class PartitionMaintenanceScheduler {

 private static final Logger logger = LoggerFactory.getLogger(PartitionMaintenanceScheduler.class);
 
 @Autowired
 private PartitionUtils partitionUtils;

 // Configuration properties
 @Value("${gps.partition.future-months:3}")
 private int futureMonthsToCreate;
 
 @Value("${gps.partition.retention-months:12}")
 private int retentionMonths;
 
 @Value("${gps.partition.auto-cleanup:true}")
 private boolean autoCleanupEnabled;

 // ═══════════════════════════════════════════════════════════════════════════════════
 // DAILY SCHEDULED TASKS
 // ═══════════════════════════════════════════════════════════════════════════════════

 /**
  * Daily task: Ensure current and future partitions exist
  * Runs every day at 2:00 AM
  */
 @Scheduled(cron = "0 0 2 * * ?")
 public void dailyPartitionCheck() {
     logger.info("Starting daily partition maintenance check...");
     
     try {
         // 1. Ensure current month partition exists
         ensureCurrentPartition();
         
         // 2. Create future partitions if needed
         ensureFuturePartitions();
         
         // 3. Log current status
         logPartitionStatus();
         
         logger.info("Daily partition maintenance completed successfully");
         
     } catch (Exception e) {
         logger.error("Daily partition maintenance failed: {}", e.getMessage(), e);
     }
 }

 /**
  * Daily task: Log partition metrics
  * Runs every day at 6:00 AM
  */
 @Scheduled(cron = "0 0 6 * * ?")
 public void dailyMetricsReport() {
     logger.info("Generating daily partition metrics report...");
     
     try {
         Map<String, Object> metrics = partitionUtils.getPartitionMetrics();
         
         logger.info("=== DAILY PARTITION METRICS ===");
         logger.info("Total Partitions: {}", metrics.get("totalPartitions"));
         logger.info("Total Rows: {}", String.format("%,d", metrics.get("totalRows")));
         logger.info("Total Size: {} MB", metrics.get("totalSizeMB"));
         logger.info("Avg Rows/Partition: {}", String.format("%,d", metrics.get("avgRowsPerPartition")));
         logger.info("Avg Size/Partition: {} MB", metrics.get("avgSizeMBPerPartition"));
         logger.info("===============================");
         
     } catch (Exception e) {
         logger.error("Failed to generate daily metrics report: {}", e.getMessage(), e);
     }
 }

 // ═══════════════════════════════════════════════════════════════════════════════════
 // WEEKLY SCHEDULED TASKS
 // ═══════════════════════════════════════════════════════════════════════════════════

 /**
  * Weekly task: Optimize recent partitions
  * Runs every Sunday at 3:00 AM
  */
 @Scheduled(cron = "0 0 3 * * SUN")
 public void weeklyPartitionOptimization() {
     logger.info("Starting weekly partition optimization...");
     
     try {
         List<Map<String, Object>> partitions = partitionUtils.getAllPartitionInfo();
         int optimizedCount = 0;
         
         // Optimize partitions from last 2 months
         for (Map<String, Object> partition : partitions) {
             String partitionName = (String) partition.get("name");
             
             if (partitionUtils.isRecentPartition(partitionName)) {
                 logger.info("Optimizing partition: {}", partitionName);
                 
                 Map<String, Object> result = partitionUtils.optimizePartition(partitionName);
                 if (!result.containsKey("error")) {
                     optimizedCount++;
                 }
             }
         }
         
         logger.info("Weekly optimization completed: {} partitions optimized", optimizedCount);
         
     } catch (Exception e) {
         logger.error("Weekly partition optimization failed: {}", e.getMessage(), e);
     }
 }

 /**
  * Weekly task: Analyze partition statistics
  * Runs every Sunday at 4:00 AM
  */
 @Scheduled(cron = "0 0 4 * * SUN")
 public void weeklyPartitionAnalysis() {
     logger.info("Starting weekly partition analysis...");
     
     try {
         List<Map<String, Object>> partitions = partitionUtils.getAllPartitionInfo();
         int analyzedCount = 0;
         
         // Analyze recent partitions
         for (Map<String, Object> partition : partitions) {
             String partitionName = (String) partition.get("name");
             
             if (partitionUtils.isRecentPartition(partitionName)) {
                 logger.info("Analyzing partition: {}", partitionName);
                 
                 Map<String, Object> result = partitionUtils.analyzePartition(partitionName);
                 if (!result.containsKey("error")) {
                     analyzedCount++;
                 }
             }
         }
         
         logger.info("Weekly analysis completed: {} partitions analyzed", analyzedCount);
         
     } catch (Exception e) {
         logger.error("Weekly partition analysis failed: {}", e.getMessage(), e);
     }
 }

 // ═══════════════════════════════════════════════════════════════════════════════════
 // MONTHLY SCHEDULED TASKS
 // ═══════════════════════════════════════════════════════════════════════════════════

 /**
  * Monthly task: Create next month's partition
  * Runs on the 1st day of every month at 1:00 AM
  */
 @Scheduled(cron = "0 0 1 1 * ?")
 public void monthlyPartitionCreation() {
     logger.info("Starting monthly partition creation...");
     
     try {
         // Create partitions for next few months
         List<String> createdPartitions = partitionUtils.createFuturePartitions(futureMonthsToCreate);
         
         if (createdPartitions.isEmpty()) {
             logger.info("All required future partitions already exist");
         } else {
             logger.info("Monthly partition creation completed: {} new partitions created", 
                        createdPartitions.size());
             logger.info("Created partitions: {}", createdPartitions);
         }
         
     } catch (Exception e) {
         logger.error("Monthly partition creation failed: {}", e.getMessage(), e);
     }
 }

 /**
  * Monthly task: Cleanup old partitions
  * Runs on the 2nd day of every month at 2:00 AM
  */
 @Scheduled(cron = "0 0 2 2 * ?")
 public void monthlyPartitionCleanup() {
     if (!autoCleanupEnabled) {
         logger.info("Automatic partition cleanup is disabled");
         return;
     }
     
     logger.info("Starting monthly partition cleanup (retention: {} months)...", retentionMonths);
     
     try {
         Map<String, Object> result = partitionUtils.cleanupOldPartitions(retentionMonths);
         
         @SuppressWarnings("unchecked")
         List<String> droppedPartitions = (List<String>) result.get("droppedPartitions");
         
         if (droppedPartitions.isEmpty()) {
             logger.info("No old partitions found for cleanup");
         } else {
             logger.warn("Monthly cleanup completed: {} old partitions dropped", droppedPartitions.size());
             logger.warn("Dropped partitions: {}", droppedPartitions);
         }
         
     } catch (Exception e) {
         logger.error("Monthly partition cleanup failed: {}", e.getMessage(), e);
     }
 }

 /**
  * Monthly task: Generate comprehensive report
  * Runs on the 3rd day of every month at 9:00 AM
  */
 @Scheduled(cron = "0 0 9 3 * ?")
 public void monthlyPartitionReport() {
     logger.info("Generating monthly partition report...");
     
     try {
         List<Map<String, Object>> partitions = partitionUtils.getAllPartitionInfo();
         Map<String, Object> metrics = partitionUtils.getPartitionMetrics();
         
         logger.info("=== MONTHLY PARTITION REPORT ===");
         logger.info("Report Date: {}", LocalDateTime.now());
         logger.info("Total Partitions: {}", metrics.get("totalPartitions"));
         logger.info("Total Rows: {}", String.format("%,d", metrics.get("totalRows")));
         logger.info("Total Size: {} MB", metrics.get("totalSizeMB"));
         logger.info("");
         
         logger.info("Partition Details:");
         for (Map<String, Object> partition : partitions) {
             logger.info("  {} - Rows: {}, Size: {} MB", 
                        partition.get("name"),
                        String.format("%,d", partition.get("rows")),
                        partition.get("sizeMB"));
         }
         
         logger.info("Current Settings:");
         logger.info("  Future Months: {}", futureMonthsToCreate);
         logger.info("  Retention Months: {}", retentionMonths);
         logger.info("  Auto Cleanup: {}", autoCleanupEnabled);
         logger.info("===============================");
         
     } catch (Exception e) {
         logger.error("Failed to generate monthly partition report: {}", e.getMessage(), e);
     }
 }

 // ═══════════════════════════════════════════════════════════════════════════════════
 // UTILITY METHODS
 // ═══════════════════════════════════════════════════════════════════════════════════

 /**
  * Ensure current month partition exists
  */
 private void ensureCurrentPartition() {
     try {
         String currentPartition = partitionUtils.getCurrentPartitionName();
         
         if (!partitionUtils.currentPartitionExists()) {
             logger.info("Creating missing current partition: {}", currentPartition);
             boolean created = partitionUtils.createCurrentPartition();
             
             if (created) {
                 logger.info("✅ Created current partition: {}", currentPartition);
             } else {
                 logger.error("❌ Failed to create current partition: {}", currentPartition);
             }
         } else {
             logger.debug("Current partition {} exists", currentPartition);
         }
         
     } catch (Exception e) {
         logger.error("Error ensuring current partition: {}", e.getMessage(), e);
     }
 }

 /**
  * Ensure future partitions exist
  */
 private void ensureFuturePartitions() {
     try {
         List<String> createdPartitions = partitionUtils.createFuturePartitions(futureMonthsToCreate);
         
         if (!createdPartitions.isEmpty()) {
             logger.info("Created {} future partitions: {}", 
                        createdPartitions.size(), createdPartitions);
         }
         
     } catch (Exception e) {
         logger.error("Error ensuring future partitions: {}", e.getMessage(), e);
     }
 }

 /**
  * Log current partition status
  */
 private void logPartitionStatus() {
     try {
         String currentPartition = partitionUtils.getCurrentPartitionName();
         String nextPartition = partitionUtils.getNextPartitionName();
         
         boolean currentExists = partitionUtils.currentPartitionExists();
         boolean nextExists = partitionUtils.partitionExists(nextPartition);
         
         logger.info("Partition Status - Current: {} ({}), Next: {} ({})",
                    currentPartition, currentExists ? "EXISTS" : "MISSING",
                    nextPartition, nextExists ? "EXISTS" : "MISSING");
                    
     } catch (Exception e) {
         logger.error("Error logging partition status: {}", e.getMessage(), e);
     }
 }

 // ═══════════════════════════════════════════════════════════════════════════════════
 // MANUAL TRIGGER METHODS
 // ═══════════════════════════════════════════════════════════════════════════════════

 /**
  * Manually trigger daily maintenance
  */
 public void triggerDailyMaintenance() {
     logger.info("Manually triggering daily partition maintenance...");
     dailyPartitionCheck();
 }

 /**
  * Manually trigger weekly optimization
  */
 public void triggerWeeklyOptimization() {
     logger.info("Manually triggering weekly partition optimization...");
     weeklyPartitionOptimization();
 }

 /**
  * Manually trigger monthly cleanup
  */
 public void triggerMonthlyCleanup() {
     logger.info("Manually triggering monthly partition cleanup...");
     monthlyPartitionCleanup();
 }

 /**
  * Get scheduler configuration
  */
 public Map<String, Object> getSchedulerConfiguration() {
     return Map.of(
         "futureMonthsToCreate", futureMonthsToCreate,
         "retentionMonths", retentionMonths,
         "autoCleanupEnabled", autoCleanupEnabled,
         "schedulerEnabled", true,
         "nextDailyCheck", "Every day at 2:00 AM",
         "nextWeeklyOptimization", "Every Sunday at 3:00 AM",
         "nextMonthlyCleanup", "2nd day of month at 2:00 AM"
     );
 }
}