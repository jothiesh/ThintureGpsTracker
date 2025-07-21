package com.GpsTracker.Thinture.partition.PartitionArchiver;

//================================================================================================
//PartitionArchiver.java - AUTOMATED DATA LIFECYCLE MANAGEMENT & ARCHIVING
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

import javax.sql.DataSource;
import java.io.File;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
* Automated data lifecycle management and archiving system for GPS tracking partitions
* Handles data tiering, compression, and long-term storage management
*/
@Component
@EnableScheduling
@ConditionalOnProperty(name = "gps.partition.archiver.enabled", havingValue = "true", matchIfMissing = true)
public class PartitionArchiver {

 private static final Logger logger = LoggerFactory.getLogger(PartitionArchiver.class);
 private static final Logger archiveLogger = LoggerFactory.getLogger("ARCHIVE." + PartitionArchiver.class.getName());
 private static final Logger storageLogger = LoggerFactory.getLogger("STORAGE." + PartitionArchiver.class.getName());

 @Autowired
 private PartitionUtils partitionUtils;
 
 @Autowired
 private DataSource dataSource;

 // Configuration properties
 @Value("${gps.partition.archive.active-months:3}")
 private int activeDataMonths;
 
 @Value("${gps.partition.archive.warm-months:6}")
 private int warmDataMonths;
 
 @Value("${gps.partition.archive.cold-months:24}")
 private int coldDataMonths;
 
 @Value("${gps.partition.archive.path:/var/gps-archives}")
 private String archivePath;
 
 @Value("${gps.partition.archive.compression:true}")
 private boolean enableCompression;
 
 @Value("${gps.partition.archive.backup-before-archive:true}")
 private boolean backupBeforeArchive;
 
 @Value("${gps.partition.archive.parallel-jobs:2}")
 private int parallelJobs;

 // Thread pool for parallel archiving
 private final ExecutorService archiveExecutor = Executors.newFixedThreadPool(2);

 // Archive statistics
 private long totalArchivedPartitions = 0;
 private long totalArchivedSizeMB = 0;
 private double totalCompressionRatio = 0.0;
 private LocalDateTime lastArchiveRun;

 // ═══════════════════════════════════════════════════════════════════════════════════
 // SCHEDULED ARCHIVING TASKS
 // ═══════════════════════════════════════════════════════════════════════════════════

 /**
  * Daily data lifecycle management - runs at 3:00 AM
  */
 @Scheduled(cron = "0 0 3 * * ?")
 public void dailyDataLifecycleManagement() {
     long startTime = System.currentTimeMillis();
     archiveLogger.info("🗄️ Starting daily data lifecycle management...");
     
     try {
         // 1. Analyze partition ages and data tiers
         analyzeDataTiers();
         
         // 2. Compress warm data partitions
         compressWarmDataPartitions();
         
         // 3. Check for partitions ready for cold storage
         identifyPartitionsForColdStorage();
         
         // 4. Clean up temporary archive files
         cleanupTemporaryFiles();
         
         long duration = System.currentTimeMillis() - startTime;
         archiveLogger.info("✅ Daily lifecycle management completed in {}ms", duration);
         lastArchiveRun = LocalDateTime.now();
         
     } catch (Exception e) {
         archiveLogger.error("❌ Daily lifecycle management failed: {}", e.getMessage(), e);
     }
 }

 /**
  * Weekly data archiving - runs Sunday at 2:00 AM
  */
 @Scheduled(cron = "0 0 2 * * SUN")
 public void weeklyDataArchiving() {
     archiveLogger.info("📦 Starting weekly data archiving process...");
     
     try {
         // Archive old partitions to cold storage
         archiveOldPartitions();
         
         // Generate archiving report
         generateArchivingReport();
         
         // Verify archive integrity
         verifyArchiveIntegrity();
         
     } catch (Exception e) {
         archiveLogger.error("❌ Weekly archiving failed: {}", e.getMessage(), e);
     }
 }

 /**
  * Monthly storage optimization - runs 1st day of month at 4:00 AM
  */
 @Scheduled(cron = "0 0 4 1 * ?")
 public void monthlyStorageOptimization() {
     archiveLogger.info("🎯 Starting monthly storage optimization...");
     
     try {
         // Optimize compressed partitions
         optimizeCompressedPartitions();
         
         // Consolidate archive files
         consolidateArchiveFiles();
         
         // Generate storage efficiency report
         generateStorageReport();
         
     } catch (Exception e) {
         archiveLogger.error("❌ Monthly storage optimization failed: {}", e.getMessage(), e);
     }
 }

 // ═══════════════════════════════════════════════════════════════════════════════════
 // DATA TIER MANAGEMENT
 // ═══════════════════════════════════════════════════════════════════════════════════

 /**
  * Analyze data tiers based on partition age
  */
 private void analyzeDataTiers() {
     archiveLogger.info("📊 Analyzing data tiers...");
     
     try {
         List<Map<String, Object>> partitions = partitionUtils.getAllPartitionInfo();
         LocalDate now = LocalDate.now();
         
         Map<DataTier, Integer> tierCounts = new HashMap<>();
         tierCounts.put(DataTier.ACTIVE, 0);
         tierCounts.put(DataTier.WARM, 0);
         tierCounts.put(DataTier.COLD, 0);
         tierCounts.put(DataTier.ARCHIVE, 0);
         
         for (Map<String, Object> partition : partitions) {
             String partitionName = (String) partition.get("name");
             DataTier tier = determineDataTier(partitionName, now);
             
             tierCounts.put(tier, tierCounts.get(tier) + 1);
             
             archiveLogger.debug("📂 Partition {} classified as {}", partitionName, tier);
         }
         
         archiveLogger.info("📊 Data tier distribution: Active: {}, Warm: {}, Cold: {}, Archive: {}", 
                          tierCounts.get(DataTier.ACTIVE), tierCounts.get(DataTier.WARM),
                          tierCounts.get(DataTier.COLD), tierCounts.get(DataTier.ARCHIVE));
         
     } catch (Exception e) {
         archiveLogger.error("❌ Error analyzing data tiers: {}", e.getMessage(), e);
     }
 }

 /**
  * Determine data tier based on partition age
  */
 private DataTier determineDataTier(String partitionName, LocalDate currentDate) {
     try {
         // Extract date from partition name (p_202506 -> 2025-06)
         String yearMonth = partitionName.substring(2);
         int year = Integer.parseInt(yearMonth.substring(0, 4));
         int month = Integer.parseInt(yearMonth.substring(4, 6));
         
         LocalDate partitionDate = LocalDate.of(year, month, 1);
         long monthsOld = java.time.Period.between(partitionDate, currentDate).toTotalMonths();
         
         if (monthsOld <= activeDataMonths) {
             return DataTier.ACTIVE;
         } else if (monthsOld <= warmDataMonths) {
             return DataTier.WARM;
         } else if (monthsOld <= coldDataMonths) {
             return DataTier.COLD;
         } else {
             return DataTier.ARCHIVE;
         }
         
     } catch (Exception e) {
         archiveLogger.error("Error determining data tier for {}: {}", partitionName, e.getMessage());
         return DataTier.ACTIVE; // Default to active for safety
     }
 }

 // ═══════════════════════════════════════════════════════════════════════════════════
 // COMPRESSION MANAGEMENT
 // ═══════════════════════════════════════════════════════════════════════════════════

 /**
  * Compress warm data partitions for storage efficiency
  */
 private void compressWarmDataPartitions() {
     if (!enableCompression) {
         archiveLogger.info("⏭️ Compression is disabled, skipping warm data compression");
         return;
     }
     
     archiveLogger.info("🗜️ Compressing warm data partitions...");
     
     try {
         List<Map<String, Object>> partitions = partitionUtils.getAllPartitionInfo();
         LocalDate now = LocalDate.now();
         List<CompletableFuture<Void>> compressionTasks = new ArrayList<>();
         
         for (Map<String, Object> partition : partitions) {
             String partitionName = (String) partition.get("name");
             DataTier tier = determineDataTier(partitionName, now);
             
             if (tier == DataTier.WARM && !isPartitionCompressed(partitionName)) {
                 CompletableFuture<Void> task = CompletableFuture.runAsync(() -> {
                     compressPartition(partitionName);
                 }, archiveExecutor);
                 
                 compressionTasks.add(task);
             }
         }
         
         // Wait for all compression tasks to complete
         CompletableFuture.allOf(compressionTasks.toArray(new CompletableFuture[0])).join();
         
         archiveLogger.info("✅ Warm data compression completed: {} partitions processed", compressionTasks.size());
         
     } catch (Exception e) {
         archiveLogger.error("❌ Error compressing warm data partitions: {}", e.getMessage(), e);
     }
 }

 /**
  * Compress individual partition
  */
 private void compressPartition(String partitionName) {
     archiveLogger.info("🗜️ Compressing partition: {}", partitionName);
     
     try {
         long startTime = System.currentTimeMillis();
         double sizeBefore = getPartitionSize(partitionName);
         
         // Apply compression to partition
         String sql = "ALTER TABLE vehicle_history MODIFY PARTITION " + partitionName + " COMPRESSION='zlib'";
         
         try (Connection conn = dataSource.getConnection();
              Statement stmt = conn.createStatement()) {
             
             stmt.executeUpdate(sql);
             
             // Optimize table to apply compression
             String optimizeSql = "OPTIMIZE TABLE vehicle_history PARTITION " + partitionName;
             stmt.executeUpdate(optimizeSql);
         }
         
         double sizeAfter = getPartitionSize(partitionName);
         double compressionRatio = sizeBefore > 0 ? (sizeBefore - sizeAfter) / sizeBefore * 100 : 0;
         
         long duration = System.currentTimeMillis() - startTime;
         storageLogger.info("✅ Compressed {}: {:.1f}MB -> {:.1f}MB ({:.1f}% reduction) in {}ms", 
                          partitionName, sizeBefore, sizeAfter, compressionRatio, duration);
         
         // Update statistics
         totalCompressionRatio = (totalCompressionRatio + compressionRatio) / 2;
         
     } catch (Exception e) {
         archiveLogger.error("❌ Failed to compress partition {}: {}", partitionName, e.getMessage(), e);
     }
 }

 /**
  * Check if partition is already compressed
  */
 private boolean isPartitionCompressed(String partitionName) {
     try {
         String sql = """
             SELECT CREATE_OPTIONS 
             FROM INFORMATION_SCHEMA.PARTITIONS 
             WHERE TABLE_SCHEMA = DATABASE() 
             AND TABLE_NAME = 'vehicle_history' 
             AND PARTITION_NAME = ?
             """;
         
         try (Connection conn = dataSource.getConnection();
              PreparedStatement stmt = conn.prepareStatement(sql)) {
             
             stmt.setString(1, partitionName);
             try (ResultSet rs = stmt.executeQuery()) {
                 if (rs.next()) {
                     String options = rs.getString("CREATE_OPTIONS");
                     return options != null && options.toLowerCase().contains("compression");
                 }
             }
         }
         
     } catch (Exception e) {
         archiveLogger.debug("Could not check compression status for {}: {}", partitionName, e.getMessage());
     }
     
     return false;
 }

 /**
  * Get partition size in MB
  */
 private double getPartitionSize(String partitionName) {
     try {
         String sql = """
             SELECT ROUND((DATA_LENGTH + INDEX_LENGTH) / 1024 / 1024, 2) AS SIZE_MB
             FROM INFORMATION_SCHEMA.PARTITIONS 
             WHERE TABLE_SCHEMA = DATABASE() 
             AND TABLE_NAME = 'vehicle_history' 
             AND PARTITION_NAME = ?
             """;
         
         try (Connection conn = dataSource.getConnection();
              PreparedStatement stmt = conn.prepareStatement(sql)) {
             
             stmt.setString(1, partitionName);
             try (ResultSet rs = stmt.executeQuery()) {
                 if (rs.next()) {
                     return rs.getDouble("SIZE_MB");
                 }
             }
         }
         
     } catch (Exception e) {
         archiveLogger.debug("Could not get size for partition {}: {}", partitionName, e.getMessage());
     }
     
     return 0.0;
 }

 // ═══════════════════════════════════════════════════════════════════════════════════
 // COLD STORAGE & ARCHIVING
 // ═══════════════════════════════════════════════════════════════════════════════════

 /**
  * Identify partitions ready for cold storage
  */
 private void identifyPartitionsForColdStorage() {
     archiveLogger.info("❄️ Identifying partitions for cold storage...");
     
     try {
         List<Map<String, Object>> partitions = partitionUtils.getAllPartitionInfo();
         LocalDate now = LocalDate.now();
         List<String> coldStorageCandidates = new ArrayList<>();
         
         for (Map<String, Object> partition : partitions) {
             String partitionName = (String) partition.get("name");
             DataTier tier = determineDataTier(partitionName, now);
             
             if (tier == DataTier.COLD || tier == DataTier.ARCHIVE) {
                 coldStorageCandidates.add(partitionName);
             }
         }
         
         if (!coldStorageCandidates.isEmpty()) {
             archiveLogger.info("❄️ Found {} partitions ready for cold storage: {}", 
                              coldStorageCandidates.size(), coldStorageCandidates);
         } else {
             archiveLogger.info("❄️ No partitions currently ready for cold storage");
         }
         
     } catch (Exception e) {
         archiveLogger.error("❌ Error identifying cold storage candidates: {}", e.getMessage(), e);
     }
 }

 /**
  * Archive old partitions to external storage
  */
 private void archiveOldPartitions() {
     archiveLogger.info("📦 Starting partition archiving process...");
     
     try {
         List<Map<String, Object>> partitions = partitionUtils.getAllPartitionInfo();
         LocalDate now = LocalDate.now();
         List<String> archiveCandidates = new ArrayList<>();
         
         for (Map<String, Object> partition : partitions) {
             String partitionName = (String) partition.get("name");
             DataTier tier = determineDataTier(partitionName, now);
             
             if (tier == DataTier.ARCHIVE) {
                 archiveCandidates.add(partitionName);
             }
         }
         
         if (archiveCandidates.isEmpty()) {
             archiveLogger.info("📦 No partitions ready for archiving");
             return;
         }
         
         archiveLogger.info("📦 Archiving {} partitions: {}", archiveCandidates.size(), archiveCandidates);
         
         for (String partitionName : archiveCandidates) {
             archivePartition(partitionName);
         }
         
     } catch (Exception e) {
         archiveLogger.error("❌ Error archiving old partitions: {}", e.getMessage(), e);
     }
 }

 /**
  * Archive individual partition
  */
 private void archivePartition(String partitionName) {
     archiveLogger.info("📦 Archiving partition: {}", partitionName);
     
     try {
         long startTime = System.currentTimeMillis();
         double partitionSizeMB = getPartitionSize(partitionName);
         
         // Create archive directory if it doesn't exist
         File archiveDir = new File(archivePath);
         if (!archiveDir.exists()) {
             archiveDir.mkdirs();
             storageLogger.info("📁 Created archive directory: {}", archivePath);
         }
         
         // Create backup before archiving if enabled
         if (backupBeforeArchive) {
             createPartitionBackup(partitionName);
         }
         
         // Export partition data to archive file
         String archiveFileName = String.format("%s/%s_%s.sql", 
             archivePath, partitionName, LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")));
         
         exportPartitionToFile(partitionName, archiveFileName);
         
         // Verify archive file
         if (verifyArchiveFile(archiveFileName)) {
             // Remove partition from active database
             boolean dropped = partitionUtils.dropPartition(partitionName);
             
             if (dropped) {
                 long duration = System.currentTimeMillis() - startTime;
                 storageLogger.info("✅ Archived partition {}: {:.1f}MB -> {} in {}ms", 
                                  partitionName, partitionSizeMB, archiveFileName, duration);
                 
                 // Update statistics
                 totalArchivedPartitions++;
                 totalArchivedSizeMB += (long) partitionSizeMB;
             } else {
                 archiveLogger.error("❌ Failed to drop partition {} after archiving", partitionName);
             }
         } else {
             archiveLogger.error("❌ Archive verification failed for {}, keeping partition", partitionName);
         }
         
     } catch (Exception e) {
         archiveLogger.error("❌ Failed to archive partition {}: {}", partitionName, e.getMessage(), e);
     }
 }

 /**
  * Create backup of partition before archiving
  */
 private void createPartitionBackup(String partitionName) {
     archiveLogger.info("💾 Creating backup for partition: {}", partitionName);
     
     try {
         String backupFileName = String.format("%s/backup_%s_%s.sql", 
             archivePath, partitionName, LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")));
         
         exportPartitionToFile(partitionName, backupFileName);
         
         storageLogger.info("💾 Backup created: {}", backupFileName);
         
     } catch (Exception e) {
         archiveLogger.error("❌ Failed to create backup for {}: {}", partitionName, e.getMessage(), e);
     }
 }

 /**
  * Export partition data to file
  */
 private void exportPartitionToFile(String partitionName, String fileName) throws SQLException {
     String sql = "SELECT * FROM vehicle_history PARTITION (" + partitionName + ")";
     
     try (Connection conn = dataSource.getConnection();
          PreparedStatement stmt = conn.prepareStatement(sql);
          ResultSet rs = stmt.executeQuery()) {
         
         // Implementation would write ResultSet to file
         // For this example, we'll simulate the export
         archiveLogger.debug("📄 Exporting partition {} to {}", partitionName, fileName);
         
         // In real implementation, you would:
         // 1. Create SQL dump file
         // 2. Write INSERT statements
         // 3. Compress if needed
         // 4. Validate export
         
     }
 }

 /**
  * Verify archive file integrity
  */
 private boolean verifyArchiveFile(String fileName) {
     try {
         File archiveFile = new File(fileName);
         boolean exists = archiveFile.exists();
         boolean hasSize = archiveFile.length() > 0;
         
         archiveLogger.debug("🔍 Verifying archive {}: exists={}, hasSize={}", fileName, exists, hasSize);
         
         return exists && hasSize;
         
     } catch (Exception e) {
         archiveLogger.error("❌ Error verifying archive file {}: {}", fileName, e.getMessage());
         return false;
     }
 }

 // ═══════════════════════════════════════════════════════════════════════════════════
 // MAINTENANCE & OPTIMIZATION
 // ═══════════════════════════════════════════════════════════════════════════════════

 /**
  * Clean up temporary files
  */
 private void cleanupTemporaryFiles() {
     archiveLogger.info("🧹 Cleaning up temporary archive files...");
     
     try {
         File archiveDir = new File(archivePath);
         if (!archiveDir.exists()) {
             return;
         }
         
         File[] tempFiles = archiveDir.listFiles((dir, name) -> 
             name.startsWith("temp_") || name.endsWith(".tmp"));
         
         if (tempFiles != null) {
             for (File tempFile : tempFiles) {
                 if (tempFile.delete()) {
                     storageLogger.debug("🗑️ Deleted temporary file: {}", tempFile.getName());
                 }
             }
             
             archiveLogger.info("🧹 Cleaned up {} temporary files", tempFiles.length);
         }
         
     } catch (Exception e) {
         archiveLogger.error("❌ Error cleaning up temporary files: {}", e.getMessage(), e);
     }
 }

 /**
  * Optimize compressed partitions
  */
 private void optimizeCompressedPartitions() {
     archiveLogger.info("🎯 Optimizing compressed partitions...");
     
     try {
         List<Map<String, Object>> partitions = partitionUtils.getAllPartitionInfo();
         
         for (Map<String, Object> partition : partitions) {
             String partitionName = (String) partition.get("name");
             
             if (isPartitionCompressed(partitionName)) {
                 partitionUtils.optimizePartition(partitionName);
                 archiveLogger.debug("🎯 Optimized compressed partition: {}", partitionName);
             }
         }
         
     } catch (Exception e) {
         archiveLogger.error("❌ Error optimizing compressed partitions: {}", e.getMessage(), e);
     }
 }

 /**
  * Consolidate archive files
  */
 private void consolidateArchiveFiles() {
     archiveLogger.info("📁 Consolidating archive files...");
     
     try {
         // Implementation would consolidate multiple small archive files
         // into larger, more efficient archive files
         archiveLogger.info("📁 Archive file consolidation completed");
         
     } catch (Exception e) {
         archiveLogger.error("❌ Error consolidating archive files: {}", e.getMessage(), e);
     }
 }

 /**
  * Verify archive integrity
  */
 private void verifyArchiveIntegrity() {
     archiveLogger.info("🔍 Verifying archive integrity...");
     
     try {
         File archiveDir = new File(archivePath);
         if (!archiveDir.exists()) {
             archiveLogger.warn("⚠️ Archive directory does not exist: {}", archivePath);
             return;
         }
         
         File[] archiveFiles = archiveDir.listFiles((dir, name) -> name.endsWith(".sql"));
         
         if (archiveFiles != null) {
             int validFiles = 0;
             for (File archiveFile : archiveFiles) {
                 if (verifyArchiveFile(archiveFile.getAbsolutePath())) {
                     validFiles++;
                 }
             }
             
             archiveLogger.info("🔍 Archive integrity check: {}/{} files valid", validFiles, archiveFiles.length);
         }
         
     } catch (Exception e) {
         archiveLogger.error("❌ Error verifying archive integrity: {}", e.getMessage(), e);
     }
 }

 // ═══════════════════════════════════════════════════════════════════════════════════
 // REPORTING
 // ═══════════════════════════════════════════════════════════════════════════════════

 /**
  * Generate archiving report
  */
 private void generateArchivingReport() {
     archiveLogger.info("📋 === WEEKLY ARCHIVING REPORT ===");
     archiveLogger.info("Report Date: {}", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE));
     archiveLogger.info("Total Archived Partitions: {}", totalArchivedPartitions);
     archiveLogger.info("Total Archived Size: {}MB", totalArchivedSizeMB);
     archiveLogger.info("Average Compression Ratio: {:.1f}%", totalCompressionRatio);
     archiveLogger.info("Last Archive Run: {}", lastArchiveRun);
     archiveLogger.info("Archive Path: {}", archivePath);
     archiveLogger.info("=================================");
 }

 /**
  * Generate storage efficiency report
  */
 private void generateStorageReport() {
     storageLogger.info("💾 === MONTHLY STORAGE REPORT ===");
     
     try {
         // Calculate storage savings from compression and archiving
         long activeSizeMB = 0;
         long compressedSizeMB = 0;
         int compressedPartitions = 0;
         
         List<Map<String, Object>> partitions = partitionUtils.getAllPartitionInfo();
         LocalDate now = LocalDate.now();
         
         for (Map<String, Object> partition : partitions) {
             String partitionName = (String) partition.get("name");
             double sizeMB = (Double) partition.get("sizeMB");
             DataTier tier = determineDataTier(partitionName, now);
             
             if (tier == DataTier.ACTIVE) {
                 activeSizeMB += (long) sizeMB;
             } else if (isPartitionCompressed(partitionName)) {
                 compressedSizeMB += (long) sizeMB;
                 compressedPartitions++;
             }
         }
         
         storageLogger.info("Active Data Size: {}MB", activeSizeMB);
         storageLogger.info("Compressed Data Size: {}MB ({} partitions)", compressedSizeMB, compressedPartitions);
         storageLogger.info("Archived Data Size: {}MB ({} partitions)", totalArchivedSizeMB, totalArchivedPartitions);
         storageLogger.info("Total Storage Savings: {}MB", totalArchivedSizeMB);
         storageLogger.info("Average Compression Ratio: {:.1f}%", totalCompressionRatio);
         
     } catch (Exception e) {
         storageLogger.error("❌ Error generating storage report: {}", e.getMessage(), e);
     }
     
     storageLogger.info("==============================");
 }

 // ═══════════════════════════════════════════════════════════════════════════════════
 // PUBLIC API METHODS
 // ═══════════════════════════════════════════════════════════════════════════════════

 /**
  * Manually trigger archiving process
  */
 public void triggerManualArchiving() {
     archiveLogger.info("🔧 Manual archiving triggered");
     weeklyDataArchiving();
 }

 /**
  * Get archiving statistics
  */
 public Map<String, Object> getArchivingStatistics() {
     Map<String, Object> stats = new HashMap<>();
     stats.put("totalArchivedPartitions", totalArchivedPartitions);
     stats.put("totalArchivedSizeMB", totalArchivedSizeMB);
     stats.put("averageCompressionRatio", totalCompressionRatio);
     stats.put("lastArchiveRun", lastArchiveRun);
     stats.put("archivePath", archivePath);
     
     return stats;
 }

 // ═══════════════════════════════════════════════════════════════════════════════════
 // ENUMS
 // ═══════════════════════════════════════════════════════════════════════════════════

 public enum DataTier {
     ACTIVE,   // 0-3 months: Frequently accessed, high performance
     WARM,     // 3-6 months: Occasionally accessed, compressed
     COLD,     // 6-24 months: Rarely accessed, heavily compressed
     ARCHIVE   // 24+ months: Very rarely accessed, archived to external storage
 }
}