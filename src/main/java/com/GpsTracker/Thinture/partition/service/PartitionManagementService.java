// ================================================================================================
// PartitionManagementService.java - MYSQL SERVICE LAYER
// ================================================================================================

package com.GpsTracker.Thinture.partition.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import com.GpsTracker.Thinture.exception.PartitionException;
import com.GpsTracker.Thinture.partition.dto.PartitionHealthDTO;
import com.GpsTracker.Thinture.partition.dto.PartitionInfoDTO;
import com.GpsTracker.Thinture.partition.dto.PartitionMetricsDTO;
import com.GpsTracker.Thinture.partition.util.PartitionUtils;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 🎯 MYSQL Partition Management Service
 * Business logic for partition operations - same structure as Oracle version
 */
@Service
public class PartitionManagementService {
    
    private static final Logger logger = LoggerFactory.getLogger(PartitionManagementService.class);
    
    @Autowired
    private PartitionUtils partitionUtils;

    /**
     * Get all partition information
     */
    public List<PartitionInfoDTO> getAllPartitions() {
        logger.info("Getting all partition information");
        
        try {
            List<Map<String, Object>> partitions = partitionUtils.getAllPartitionInfo();
            
            return partitions.stream()
                .map(this::mapToPartitionInfoDTO)
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            logger.error("Error getting all partitions: {}", e.getMessage(), e);
            throw new PartitionException("Failed to retrieve partition information", e);
        }
    }

    /**
     * Get specific partition details
     */
    public PartitionInfoDTO getPartitionInfo(String partitionName) {
        logger.info("Getting partition info for: {}", partitionName);
        
        validatePartitionName(partitionName);
        
        try {
            Map<String, Object> details = partitionUtils.getPartitionDetails(partitionName);
            
            if (details.isEmpty()) {
                throw new PartitionException("Partition not found: " + partitionName);
            }
            
            return mapToPartitionInfoDTO(details);
            
        } catch (Exception e) {
            logger.error("Error getting partition info for {}: {}", partitionName, e.getMessage(), e);
            throw new PartitionException("Failed to get partition info for: " + partitionName, e);
        }
    }

    /**
     * Get partition health status
     */
    public PartitionHealthDTO getPartitionHealth(String partitionName) {
        logger.info("Getting partition health for: {}", partitionName);
        
        validatePartitionName(partitionName);
        
        try {
            Map<String, Object> details = partitionUtils.getPartitionDetails(partitionName);
            
            if (details.isEmpty()) {
                throw new PartitionException("Partition not found: " + partitionName);
            }
            
            return mapToPartitionHealthDTO(details);
            
        } catch (Exception e) {
            logger.error("Error getting partition health for {}: {}", partitionName, e.getMessage(), e);
            throw new PartitionException("Failed to get partition health for: " + partitionName, e);
        }
    }

    /**
     * Get partition metrics
     */
    public PartitionMetricsDTO getPartitionMetrics(String partitionName) {
        logger.info("Getting partition metrics for: {}", partitionName);
        
        validatePartitionName(partitionName);
        
        try {
            Map<String, Object> details = partitionUtils.getPartitionDetails(partitionName);
            Map<String, Object> globalMetrics = partitionUtils.getPartitionMetrics();
            
            if (details.isEmpty()) {
                throw new PartitionException("Partition not found: " + partitionName);
            }
            
            return mapToPartitionMetricsDTO(details, globalMetrics);
            
        } catch (Exception e) {
            logger.error("Error getting partition metrics for {}: {}", partitionName, e.getMessage(), e);
            throw new PartitionException("Failed to get partition metrics for: " + partitionName, e);
        }
    }

    /**
     * Create new partition
     */
    public PartitionInfoDTO createPartition(int year, int month) {
        logger.info("Creating partition for {}/{}", year, month);
        
        validatePartitionParams(year, month);
        
        try {
            boolean created = partitionUtils.createMonthlyPartition(year, month);
            
            if (created) {
                String partitionName = partitionUtils.getPartitionName(year, month);
                return getPartitionInfo(partitionName);
            } else {
                throw new PartitionException("Failed to create partition for " + year + "/" + month);
            }
            
        } catch (Exception e) {
            logger.error("Error creating partition for {}/{}: {}", year, month, e.getMessage(), e);
            throw new PartitionException("Failed to create partition for " + year + "/" + month, e);
        }
    }

    /**
     * Optimize partition
     */
    public Map<String, Object> optimizePartition(String partitionName) {
        logger.info("Optimizing partition: {}", partitionName);
        
        validatePartitionName(partitionName);
        
        if (!partitionUtils.partitionExists(partitionName)) {
            throw new PartitionException("Partition not found: " + partitionName);
        }
        
        try {
            return partitionUtils.optimizePartition(partitionName);
            
        } catch (Exception e) {
            logger.error("Error optimizing partition {}: {}", partitionName, e.getMessage(), e);
            throw new PartitionException("Failed to optimize partition: " + partitionName, e);
        }
    }

    /**
     * Analyze partition
     */
    public Map<String, Object> analyzePartition(String partitionName) {
        logger.info("Analyzing partition: {}", partitionName);
        
        validatePartitionName(partitionName);
        
        if (!partitionUtils.partitionExists(partitionName)) {
            throw new PartitionException("Partition not found: " + partitionName);
        }
        
        try {
            return partitionUtils.analyzePartition(partitionName);
            
        } catch (Exception e) {
            logger.error("Error analyzing partition {}: {}", partitionName, e.getMessage(), e);
            throw new PartitionException("Failed to analyze partition: " + partitionName, e);
        }
    }

    /**
     * Drop partition
     */
    public boolean dropPartition(String partitionName, boolean force) {
        logger.warn("Dropping partition: {} (force={})", partitionName, force);
        
        validatePartitionName(partitionName);
        
        if (!partitionUtils.partitionExists(partitionName)) {
            throw new PartitionException("Partition not found: " + partitionName);
        }
        
        // Safety check for recent partitions
        if (!force && partitionUtils.isRecentPartition(partitionName)) {
            throw new PartitionException("Cannot drop recent partition without force=true: " + partitionName);
        }
        
        try {
            return partitionUtils.dropPartition(partitionName);
            
        } catch (Exception e) {
            logger.error("Error dropping partition {}: {}", partitionName, e.getMessage(), e);
            throw new PartitionException("Failed to drop partition: " + partitionName, e);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════════
    // VALIDATION METHODS
    // ═══════════════════════════════════════════════════════════════════════════════════

    private void validatePartitionName(String partitionName) {
        if (!partitionUtils.isValidPartitionName(partitionName)) {
            throw new PartitionException("Invalid partition name format: " + partitionName);
        }
    }

    private void validatePartitionParams(int year, int month) {
        if (year < 2020 || year > 2050) {
            throw new PartitionException("Year must be between 2020 and 2050: " + year);
        }
        if (month < 1 || month > 12) {
            throw new PartitionException("Month must be between 1 and 12: " + month);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════════
    // MAPPING METHODS - Convert MySQL data to DTOs
    // ═══════════════════════════════════════════════════════════════════════════════════

    private PartitionInfoDTO mapToPartitionInfoDTO(Map<String, Object> details) {
        PartitionInfoDTO dto = new PartitionInfoDTO();

        dto.setPartitionName((String) details.get("name"));
        dto.setPartitionDescription((String) details.get("description"));
        dto.setRows((Long) details.get("rows"));
        dto.setSizeMB((Double) details.get("sizeMB"));
        dto.setCreateTime((java.sql.Timestamp) details.get("createTime"));
        dto.setUpdateTime((java.sql.Timestamp) details.get("updateTime"));
        dto.setTableName("vehicle_history");
        dto.setExists(true); 
        dto.setHealthScore(100);
        dto.setStatus("HEALTHY");

        // Extract date range from partition name
        String partitionName = dto.getPartitionName();
        if (partitionName != null && partitionName.startsWith("p_")) {
            try {
                String yearMonth = partitionName.substring(2);
                int year = Integer.parseInt(yearMonth.substring(0, 4));
                int month = Integer.parseInt(yearMonth.substring(4, 6));
                dto.setYear(year);
                dto.setMonth(month);
                dto.setPartitionDescription(yearMonth);
            } catch (Exception e) {
                logger.warn("Could not parse date from partition name: {}", partitionName);
            }
        }

        return dto;
    }


    private PartitionHealthDTO mapToPartitionHealthDTO(Map<String, Object> details) {
        PartitionHealthDTO dto = new PartitionHealthDTO();

        // Basic partition info
        dto.setPartitionName((String) details.get("name"));

        // Set QuickMetrics (nested structure)
        if (dto.getQuickMetrics() != null) {
            dto.getQuickMetrics().setRecordCount((Long) details.get("rows"));
            dto.getQuickMetrics().setSizeMB((Double) details.get("sizeMB"));
        }

        // Calculate health status based on MySQL metrics
        String healthStatus = calculateHealthStatus(
            dto.getQuickMetrics().getSizeMB(), 
            dto.getQuickMetrics().getRecordCount()
        );
        dto.setOverallStatus(healthStatus);

        // Calculate health score (0-100)
        int healthScore = calculateHealthScore(
            dto.getQuickMetrics().getSizeMB(), 
            dto.getQuickMetrics().getRecordCount()
        );
        dto.setHealthScore(healthScore);

        // Last analyzed/check time
        dto.setLastChecked(
            details.get("checkTime") instanceof LocalDateTime
                ? (LocalDateTime) details.get("checkTime")
                : null
        );

        // Optionally set check duration if available
        if (details.get("checkDurationMs") instanceof Long) {
            dto.setCheckDurationMs((Long) details.get("checkDurationMs"));
        }

        return dto;
    }

    private PartitionMetricsDTO mapToPartitionMetricsDTO(Map<String, Object> details, Map<String, Object> globalMetrics) {
        PartitionMetricsDTO dto = new PartitionMetricsDTO();

        dto.setPartitionName((String) details.get("name"));

        // ✅ Set Record Metrics
        dto.getRecordMetrics().setCurrentRecordCount((Long) details.get("rows"));

        // ✅ Set Size Metrics
        Double sizeMB = (Double) details.get("sizeMB");
        dto.getSizeMetrics().setCurrentSizeMB(sizeMB);
        dto.getSizeMetrics().setCompressionRatio(0.0);
        dto.getSizeMetrics().setStorageTier(sizeMB != null && sizeMB > 0 ? "WARM" : "COLD");

        // ✅ Optionally set Performance Metrics
        if (globalMetrics != null) {
            dto.getPerformanceMetrics().setAverageQueryTimeMs((Double) globalMetrics.getOrDefault("averageQueryTime", 0.0));
            dto.getPerformanceMetrics().setSlowQueryPercentage((Double) globalMetrics.getOrDefault("slowQueryPercentage", 0.0));
            dto.getPerformanceMetrics().setTotalQueries((Long) globalMetrics.getOrDefault("totalQueries", 0L));
        }

        // ✅ Add default recommendation
        dto.addRecommendation("STORAGE_OPTIMIZATION", "MEDIUM", "Consider archiving old data to reduce partition size.");

        return dto;
    }

    // ═══════════════════════════════════════════════════════════════════════════════════
    // HEALTH CALCULATION METHODS - MySQL specific
    // ═══════════════════════════════════════════════════════════════════════════════════

    private String calculateHealthStatus(double sizeMB, long recordCount) {
        if (recordCount == 0) {
            return "EMPTY";
        }
        if (sizeMB > 5000) { // 5GB threshold
            return "LARGE";
        }
        if (sizeMB > 2000) { // 2GB threshold
            return "WARNING";
        }
        return "HEALTHY";
    }

    private int calculateHealthScore(double sizeMB, long recordCount) {
        if (recordCount == 0) {
            return 50; // Neutral for empty partitions
        }
        
        if (sizeMB <= 1000) {
            return 100; // Excellent
        } else if (sizeMB <= 2000) {
            return 80;  // Good
        } else if (sizeMB <= 5000) {
            return 60;  // Warning
        } else {
            return 30;  // Poor
        }
    }

    private List<String> generateOptimizationRecommendations(double sizeMB, long recordCount) {
        List<String> recommendations = new java.util.ArrayList<>();
        
        if (recordCount == 0) {
            recommendations.add("Partition is empty - consider dropping if old");
        }
        
        if (sizeMB > 5000) {
            recommendations.add("Partition is very large - consider archiving old data");
            recommendations.add("Consider enabling MySQL compression");
        }
        
        if (sizeMB > 2000) {
            recommendations.add("Monitor partition growth closely");
            recommendations.add("Consider optimizing table structure");
        }
        
        if (recordCount > 50000000) { // 50M records
            recommendations.add("High record count - ensure indexes are optimized");
        }
        
        return recommendations;
    }
}