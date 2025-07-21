// ================================================================================================
// PartitionSizeService.java - PARTITION SIZE MONITORING SERVICE
// ================================================================================================

package com.GpsTracker.Thinture.partition.service;

import com.GpsTracker.Thinture.partition.config.PartitionConfig;
import com.GpsTracker.Thinture.partition.util.PartitionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 📏 Partition Size Monitoring Service
 * Handles all partition size-related operations and monitoring
 */
@Service
public class PartitionSizeService {
    
    private static final Logger logger = LoggerFactory.getLogger(PartitionSizeService.class);
    private static final Logger sizeLogger = LoggerFactory.getLogger("SIZE." + PartitionSizeService.class.getName());
    private static final Logger alertLogger = LoggerFactory.getLogger("ALERT." + PartitionSizeService.class.getName());
    
    @Autowired
    private PartitionUtils partitionUtils;
    
    @Autowired
    private PartitionConfig partitionConfig;
    
    // Size monitoring cache
    private final Map<String, PartitionSizeInfo> sizeCache = new ConcurrentHashMap<>();
    private volatile long lastCacheUpdate = 0;
    private static final long CACHE_VALIDITY_MS = 300000; // 5 minutes
    
    // ═══════════════════════════════════════════════════════════════════════════════════
    // INITIALIZATION
    // ═══════════════════════════════════════════════════════════════════════════════════
    
    @PostConstruct
    public void initializeService() {
        sizeLogger.info("🚀 Initializing Partition Size Service...");
        
        try {
            // Initial size cache population
            refreshSizeCache();
            
            sizeLogger.info("✅ Partition Size Service initialized successfully");
            sizeLogger.info("📊 Cached size information for {} partitions", sizeCache.size());
            
        } catch (Exception e) {
            sizeLogger.error("❌ Failed to initialize Partition Size Service: {}", e.getMessage(), e);
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════════
    // CORE SIZE MONITORING METHODS
    // ═══════════════════════════════════════════════════════════════════════════════════
    
    /**
     * Get partition size in MB with caching
     */
    public double getPartitionSizeMB(String partitionName) {
        sizeLogger.debug("📏 Getting size for partition: {}", partitionName);
        
        try {
            // Check cache first
            if (isCacheValid()) {
                PartitionSizeInfo cachedInfo = sizeCache.get(partitionName);
                if (cachedInfo != null) {
                    sizeLogger.debug("💾 Retrieved cached size for {}: {:.2f}MB", partitionName, cachedInfo.getSizeMB());
                    return cachedInfo.getSizeMB();
                }
            }
            
            // Refresh cache if invalid or partition not found
            refreshSizeCache();
            
            // Try cache again after refresh
            PartitionSizeInfo info = sizeCache.get(partitionName);
            if (info != null) {
                sizeLogger.debug("📏 Retrieved fresh size for {}: {:.2f}MB", partitionName, info.getSizeMB());
                return info.getSizeMB();
            }
            
            sizeLogger.warn("⚠️ Partition not found: {}", partitionName);
            return 0.0;
            
        } catch (Exception e) {
            sizeLogger.error("❌ Error getting partition size for {}: {}", partitionName, e.getMessage(), e);
            return 0.0;
        }
    }
    
    /**
     * Get partition size with detailed information
     */
    public PartitionSizeInfo getPartitionSizeInfo(String partitionName) {
        sizeLogger.debug("📊 Getting detailed size info for partition: {}", partitionName);
        
        try {
            // Ensure cache is fresh
            if (!isCacheValid()) {
                refreshSizeCache();
            }
            
            PartitionSizeInfo info = sizeCache.get(partitionName);
            if (info != null) {
                sizeLogger.debug("📊 Retrieved size info for {}: {} rows, {:.2f}MB", 
                               partitionName, info.getRowCount(), info.getSizeMB());
                return info;
            }
            
            sizeLogger.warn("⚠️ Partition size info not found: {}", partitionName);
            return new PartitionSizeInfo(partitionName, 0.0, 0L);
            
        } catch (Exception e) {
            sizeLogger.error("❌ Error getting partition size info for {}: {}", partitionName, e.getMessage(), e);
            return new PartitionSizeInfo(partitionName, 0.0, 0L);
        }
    }
    
    /**
     * Check if partition exceeds specified threshold
     */
    public boolean isPartitionTooLarge(String partitionName, int thresholdMB) {
        sizeLogger.debug("🔍 Checking if partition {} exceeds {}MB threshold", partitionName, thresholdMB);

        try {
            double sizeMB = getPartitionSizeMB(partitionName);
            boolean tooLarge = sizeMB > thresholdMB;

            if (tooLarge) {
                sizeLogger.warn("⚠️ Partition {} size {:.1f}MB exceeds threshold {}MB", 
                               partitionName, sizeMB, thresholdMB);

                // Log threshold level
                String level = partitionConfig.getThresholdInfo(sizeMB).getLevel();
                if ("CRITICAL".equals(level) || "EMERGENCY".equals(level)) {
                    alertLogger.error("🚨 {} THRESHOLD: Partition {} at {:.1f}MB", level, partitionName, sizeMB);
                }
            } else {
                sizeLogger.debug("✅ Partition {} size {:.1f}MB within threshold {}MB", 
                               partitionName, sizeMB, thresholdMB);
            }

            return tooLarge;

        } catch (Exception e) {
            sizeLogger.error("❌ Error checking partition size threshold for {}: {}", partitionName, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Check if partition exceeds warning threshold
     */
    public boolean exceedsWarningThreshold(String partitionName) {
        sizeLogger.debug("⚠️ Checking warning threshold for partition: {}", partitionName);
        return isPartitionTooLarge(partitionName, partitionConfig.getWarningThresholdMB());
    }
    
    /**
     * Check if partition exceeds critical threshold
     */
    public boolean exceedsCriticalThreshold(String partitionName) {
        sizeLogger.debug("🚨 Checking critical threshold for partition: {}", partitionName);
        return isPartitionTooLarge(partitionName, partitionConfig.getCriticalThresholdMB());
    }
    
    /**
     * Check if partition exceeds emergency threshold
     */
    public boolean exceedsEmergencyThreshold(String partitionName) {
        sizeLogger.debug("🆘 Checking emergency threshold for partition: {}", partitionName);
        return isPartitionTooLarge(partitionName, partitionConfig.getEmergencyThresholdMB());
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════════
    // BULK MONITORING OPERATIONS
    // ═══════════════════════════════════════════════════════════════════════════════════
    
    /**
     * Get all partition sizes
     */
    public Map<String, Double> getAllPartitionSizes() {
        sizeLogger.debug("📊 Getting all partition sizes");
        
        try {
            Map<String, Double> sizes = new HashMap<>();
            
            // Ensure cache is fresh
            if (!isCacheValid()) {
                refreshSizeCache();
            }
            
            sizeCache.forEach((name, info) -> sizes.put(name, info.getSizeMB()));
            
            sizeLogger.info("📊 Retrieved sizes for {} partitions", sizes.size());
            return sizes;
            
        } catch (Exception e) {
            sizeLogger.error("❌ Error getting all partition sizes: {}", e.getMessage(), e);
            return new HashMap<>();
        }
    }
    
    /**
     * Find partitions that exceed thresholds
     */
    public Map<String, String> findOversizedPartitions() {
        sizeLogger.info("🔍 Scanning for oversized partitions...");
        
        try {
            Map<String, String> oversizedPartitions = new HashMap<>();
            
            // Ensure cache is fresh
            if (!isCacheValid()) {
                refreshSizeCache();
            }
            
            for (Map.Entry<String, PartitionSizeInfo> entry : sizeCache.entrySet()) {
                String partitionName = entry.getKey();
                double sizeMB = entry.getValue().getSizeMB();

                String thresholdLevel = partitionConfig.getThresholdInfo(sizeMB).getLevel();

                if (!"NORMAL".equals(thresholdLevel)) {
                    oversizedPartitions.put(partitionName, thresholdLevel);
                    sizeLogger.warn("⚠️ Found {} partition: {} at {:.1f}MB", 
                                   thresholdLevel, partitionName, sizeMB);
                }
            }

            
            if (oversizedPartitions.isEmpty()) {
                sizeLogger.info("✅ No oversized partitions found");
            } else {
                sizeLogger.warn("⚠️ Found {} oversized partitions", oversizedPartitions.size());
                
                // Log summary by threshold level
                Map<String, Long> countByLevel = oversizedPartitions.values().stream()
                    .collect(java.util.stream.Collectors.groupingBy(
                        level -> level, 
                        java.util.stream.Collectors.counting()));
                
                countByLevel.forEach((level, count) -> {
                    if ("EMERGENCY".equals(level) || "CRITICAL".equals(level)) {
                        alertLogger.error("🚨 {} {} partitions require immediate attention", count, level);
                    } else {
                        sizeLogger.warn("⚠️ {} {} partitions need monitoring", count, level);
                    }
                });
            }
            
            return oversizedPartitions;
            
        } catch (Exception e) {
            sizeLogger.error("❌ Error finding oversized partitions: {}", e.getMessage(), e);
            return new HashMap<>();
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════════
    // CACHE MANAGEMENT
    // ═══════════════════════════════════════════════════════════════════════════════════
    
    /**
     * Refresh the size cache
     */
    public void refreshSizeCache() {
        sizeLogger.debug("🔄 Refreshing partition size cache...");
        
        try {
            long startTime = System.currentTimeMillis();
            
            List<Map<String, Object>> partitions = partitionUtils.getAllPartitionInfo();
            
            Map<String, PartitionSizeInfo> newCache = new HashMap<>();
            
            for (Map<String, Object> partition : partitions) {
                String name = (String) partition.get("name");
                Double sizeMB = (Double) partition.get("sizeMB");
                Long rows = (Long) partition.get("rows");
                
                if (name != null && sizeMB != null && rows != null) {
                    PartitionSizeInfo info = new PartitionSizeInfo(name, sizeMB, rows);
                    newCache.put(name, info);
                    
                    sizeLogger.debug("💾 Cached size for {}: {:.2f}MB, {} rows", name, sizeMB, rows);
                }
            }
            
            // Replace cache atomically
            sizeCache.clear();
            sizeCache.putAll(newCache);
            lastCacheUpdate = System.currentTimeMillis();
            
            long duration = System.currentTimeMillis() - startTime;
            sizeLogger.info("✅ Size cache refreshed: {} partitions in {}ms", newCache.size(), duration);
            
        } catch (Exception e) {
            sizeLogger.error("❌ Error refreshing size cache: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Check if cache is still valid
     */
    private boolean isCacheValid() {
        boolean valid = (System.currentTimeMillis() - lastCacheUpdate) < CACHE_VALIDITY_MS;
        sizeLogger.debug("💾 Cache validity check: {} (age: {}ms)", 
                        valid ? "VALID" : "EXPIRED", 
                        System.currentTimeMillis() - lastCacheUpdate);
        return valid;
    }
    
    /**
     * Force cache invalidation
     */
    public void invalidateCache() {
        sizeLogger.info("🗑️ Invalidating partition size cache");
        lastCacheUpdate = 0;
        sizeCache.clear();
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════════
    // MONITORING AND ANALYTICS
    // ═══════════════════════════════════════════════════════════════════════════════════
    
    /**
     * Get partition size statistics
     */
    public PartitionSizeStatistics getSizeStatistics() {
        sizeLogger.debug("📈 Calculating partition size statistics");
        
        try {
            if (!isCacheValid()) {
                refreshSizeCache();
            }
            
            if (sizeCache.isEmpty()) {
                sizeLogger.warn("⚠️ No partitions found for statistics calculation");
                return new PartitionSizeStatistics();
            }
            
            double totalSize = 0;
            double minSize = Double.MAX_VALUE;
            double maxSize = 0;
            long totalRows = 0;
            
            for (PartitionSizeInfo info : sizeCache.values()) {
                double size = info.getSizeMB();
                totalSize += size;
                totalRows += info.getRowCount();
                
                if (size < minSize) minSize = size;
                if (size > maxSize) maxSize = size;
            }
            
            double avgSize = totalSize / sizeCache.size();
            
            PartitionSizeStatistics stats = new PartitionSizeStatistics();
            stats.setTotalPartitions(sizeCache.size());
            stats.setTotalSizeMB(totalSize);
            stats.setAverageSizeMB(avgSize);
            stats.setMinSizeMB(minSize == Double.MAX_VALUE ? 0 : minSize);
            stats.setMaxSizeMB(maxSize);
            stats.setTotalRows(totalRows);
            stats.setCalculatedAt(LocalDateTime.now());
            
            sizeLogger.info("📈 Size statistics: {} partitions, {:.1f}MB total, {:.1f}MB avg", 
                           stats.getTotalPartitions(), stats.getTotalSizeMB(), stats.getAverageSizeMB());
            
            return stats;
            
        } catch (Exception e) {
            sizeLogger.error("❌ Error calculating size statistics: {}", e.getMessage(), e);
            return new PartitionSizeStatistics();
        }
    }
    
    /**
     * Log current size status
     */
    public void logSizeStatus() {
        sizeLogger.info("📊 === PARTITION SIZE STATUS ===");
        
        try {
            PartitionSizeStatistics stats = getSizeStatistics();
            Map<String, String> oversized = findOversizedPartitions();
            
            sizeLogger.info("📏 Total Partitions: {}", stats.getTotalPartitions());
            sizeLogger.info("📊 Total Size: {:.1f}MB", stats.getTotalSizeMB());
            sizeLogger.info("📊 Average Size: {:.1f}MB", stats.getAverageSizeMB());
            sizeLogger.info("📊 Size Range: {:.1f}MB - {:.1f}MB", stats.getMinSizeMB(), stats.getMaxSizeMB());
            sizeLogger.info("📊 Total Rows: {}", stats.getTotalRows());
            
            if (!oversized.isEmpty()) {
                sizeLogger.warn("⚠️ Oversized Partitions: {}", oversized.size());
                oversized.forEach((name, level) -> {
                    double size = getPartitionSizeMB(name);
                    sizeLogger.warn("  {} - {} ({:.1f}MB)", name, level, size);
                });
            } else {
                sizeLogger.info("✅ All partitions within normal size limits");
            }
            
            sizeLogger.info("===============================");
            
        } catch (Exception e) {
            sizeLogger.error("❌ Error logging size status: {}", e.getMessage(), e);
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════════
    // INNER CLASSES
    // ═══════════════════════════════════════════════════════════════════════════════════
    
    /**
     * Partition size information holder
     */
    public static class PartitionSizeInfo {
        private final String partitionName;
        private final double sizeMB;
        private final long rowCount;
        private final LocalDateTime lastUpdated;
        
        public PartitionSizeInfo(String partitionName, double sizeMB, long rowCount) {
            this.partitionName = partitionName;
            this.sizeMB = sizeMB;
            this.rowCount = rowCount;
            this.lastUpdated = LocalDateTime.now();
        }
        
        // Getters
        public String getPartitionName() { return partitionName; }
        public double getSizeMB() { return sizeMB; }
        public long getRowCount() { return rowCount; }
        public LocalDateTime getLastUpdated() { return lastUpdated; }
    }
    
    /**
     * Partition size statistics
     */
    public static class PartitionSizeStatistics {
        private int totalPartitions;
        private double totalSizeMB;
        private double averageSizeMB;
        private double minSizeMB;
        private double maxSizeMB;
        private long totalRows;
        private LocalDateTime calculatedAt;
        
        // Getters and setters
        public int getTotalPartitions() { return totalPartitions; }
        public void setTotalPartitions(int totalPartitions) { this.totalPartitions = totalPartitions; }
        
        public double getTotalSizeMB() { return totalSizeMB; }
        public void setTotalSizeMB(double totalSizeMB) { this.totalSizeMB = totalSizeMB; }
        
        public double getAverageSizeMB() { return averageSizeMB; }
        public void setAverageSizeMB(double averageSizeMB) { this.averageSizeMB = averageSizeMB; }
        
        public double getMinSizeMB() { return minSizeMB; }
        public void setMinSizeMB(double minSizeMB) { this.minSizeMB = minSizeMB; }
        
        public double getMaxSizeMB() { return maxSizeMB; }
        public void setMaxSizeMB(double maxSizeMB) { this.maxSizeMB = maxSizeMB; }
        
        public long getTotalRows() { return totalRows; }
        public void setTotalRows(long totalRows) { this.totalRows = totalRows; }
        
        public LocalDateTime getCalculatedAt() { return calculatedAt; }
        public void setCalculatedAt(LocalDateTime calculatedAt) { this.calculatedAt = calculatedAt; }
    }
}