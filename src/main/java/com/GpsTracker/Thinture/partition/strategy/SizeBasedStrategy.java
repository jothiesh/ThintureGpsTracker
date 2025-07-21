// ================================================================================================
// SizeBasedStrategy.java - INTELLIGENT SIZE-BASED PARTITION STRATEGY
// ================================================================================================

package com.GpsTracker.Thinture.partition.strategy;

import com.GpsTracker.Thinture.partition.config.PartitionConfig;
import com.GpsTracker.Thinture.partition.service.PartitionSizeService;
import com.GpsTracker.Thinture.partition.util.PartitionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 🧠 Size-Based Partition Strategy
 * Implements intelligent partition creation and management based on data size
 */
@Component
public class SizeBasedStrategy {
    
    private static final Logger logger = LoggerFactory.getLogger(SizeBasedStrategy.class);
    private static final Logger strategyLogger = LoggerFactory.getLogger("STRATEGY." + SizeBasedStrategy.class.getName());
    private static final Logger alertLogger = LoggerFactory.getLogger("ALERT." + SizeBasedStrategy.class.getName());
    private static final Logger decisionLogger = LoggerFactory.getLogger("DECISION." + SizeBasedStrategy.class.getName());
    
    @Autowired
    private PartitionConfig partitionConfig;
    
    @Autowired
    private PartitionSizeService partitionSizeService;
    
    @Autowired
    private PartitionUtils partitionUtils;
    
    // Partition naming patterns
    private static final Pattern PARTITION_PATTERN = Pattern.compile("p_(\\d{4})(\\d{2})(?:_([a-z]))?");
    private static final DateTimeFormatter PARTITION_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMM");
    
    // Strategy configuration
    private static final double SIZE_PROJECTION_FACTOR = 1.2; // 20% growth buffer
    private static final int MAX_SUB_PARTITIONS = 26; // a-z
    
    // ═══════════════════════════════════════════════════════════════════════════════════
    // INITIALIZATION
    // ═══════════════════════════════════════════════════════════════════════════════════
    
    @PostConstruct
    public void initializeStrategy() {
        strategyLogger.info("🚀 Initializing Size-Based Partition Strategy...");
        
        try {
            // Log strategy configuration
            logStrategyConfiguration();
            
            // Validate strategy settings
            validateStrategySettings();
            
            strategyLogger.info("✅ Size-Based Partition Strategy initialized successfully");
            
        } catch (Exception e) {
            strategyLogger.error("❌ Failed to initialize Size-Based Partition Strategy: {}", e.getMessage(), e);
        }
    }
    
    private void logStrategyConfiguration() {
        strategyLogger.info("⚙️ Strategy Configuration:");
        strategyLogger.info("  Critical threshold: {}MB", partitionConfig.getCriticalThresholdMB());
        strategyLogger.info("  Emergency threshold: {}MB", partitionConfig.getEmergencyThresholdMB());
        strategyLogger.info("  Auto-split enabled: {}", partitionConfig.isAutoSplitEnabled());
        strategyLogger.info("  Size projection factor: {}x", SIZE_PROJECTION_FACTOR);
        strategyLogger.info("  Max sub-partitions: {}", MAX_SUB_PARTITIONS);
    }
    
    private void validateStrategySettings() {
        if (partitionConfig.getCriticalThresholdMB() <= partitionConfig.getWarningThresholdMB()) {
            throw new IllegalStateException("Critical threshold must be greater than warning threshold");
        }
        
        if (partitionConfig.getEmergencyThresholdMB() <= partitionConfig.getCriticalThresholdMB()) {
            throw new IllegalStateException("Emergency threshold must be greater than critical threshold");
        }
        
        strategyLogger.debug("✅ Strategy settings validation completed");
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════════
    // CORE STRATEGY METHODS
    // ═══════════════════════════════════════════════════════════════════════════════════
    
    /**
     * Determine if a new partition should be created based on size
     */
    public boolean shouldCreateNewPartition(String currentPartition) {
        strategyLogger.debug("🔍 Evaluating partition creation need for: {}", currentPartition);
        
        try {
            if (!partitionUtils.partitionExists(currentPartition)) {
                strategyLogger.warn("⚠️ Partition does not exist: {}", currentPartition);
                return true; // Always create if missing
            }
            
            double currentSizeMB = partitionSizeService.getPartitionSizeMB(currentPartition);
            
            // Decision tree based on thresholds
            PartitionDecision decision = evaluatePartitionDecision(currentPartition, currentSizeMB);
            
            logDecision(currentPartition, currentSizeMB, decision);
            
            return decision.shouldCreateNew();
            
        } catch (Exception e) {
            strategyLogger.error("❌ Error evaluating partition creation for {}: {}", currentPartition, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Determine if a partition should be split
     */
    public boolean shouldSplitPartition(String partitionName) {
        strategyLogger.debug("✂️ Evaluating partition split need for: {}", partitionName);
        
        try {
            if (!partitionConfig.isAutoSplitEnabled()) {
                strategyLogger.debug("✂️ Auto-split disabled, skipping split evaluation");
                return false;
            }
            
            double sizeMB = partitionSizeService.getPartitionSizeMB(partitionName);
            
            // Split if exceeds critical threshold and is not already split
            boolean exceedsCritical = sizeMB > partitionConfig.getCriticalThresholdMB();
            boolean isAlreadySplit = isPartitionSplit(partitionName);
            boolean canSplit = canPartitionBeSplit(partitionName);
            
            boolean shouldSplit = exceedsCritical && !isAlreadySplit && canSplit;
            
            strategyLogger.debug("✂️ Split evaluation for {}: size={:.1f}MB, exceeds={}, split={}, canSplit={}, result={}", 
                               partitionName, sizeMB, exceedsCritical, isAlreadySplit, canSplit, shouldSplit);
            
            if (shouldSplit) {
                alertLogger.warn("✂️ Partition {} ({:.1f}MB) should be split", partitionName, sizeMB);
            }
            
            return shouldSplit;
            
        } catch (Exception e) {
            strategyLogger.error("❌ Error evaluating partition split for {}: {}", partitionName, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Generate next partition name based on current partition
     */
    public String generateNextPartitionName(String currentPartition) {
        strategyLogger.debug("📝 Generating next partition name for: {}", currentPartition);
        
        try {
            Matcher matcher = PARTITION_PATTERN.matcher(currentPartition);
            
            if (!matcher.matches()) {
                strategyLogger.error("❌ Invalid partition name format: {}", currentPartition);
                throw new IllegalArgumentException("Invalid partition name format: " + currentPartition);
            }
            
            String year = matcher.group(1);
            String month = matcher.group(2);
            String suffix = matcher.group(3);
            
            String nextName;
            
            if (suffix == null) {
                // First split: p_202507 → p_202507_b (keeping original as _a)
                nextName = String.format("p_%s%s_b", year, month);
                strategyLogger.debug("📝 Generated first split partition: {}", nextName);
            } else {
                // Subsequent splits: p_202507_b → p_202507_c
                char nextSuffix = (char) (suffix.charAt(0) + 1);
                if (nextSuffix > 'z') {
                    strategyLogger.error("❌ Maximum sub-partitions reached for: {}", currentPartition);
                    throw new IllegalStateException("Maximum sub-partitions reached for: " + currentPartition);
                }
                nextName = String.format("p_%s%s_%c", year, month, nextSuffix);
                strategyLogger.debug("📝 Generated subsequent split partition: {}", nextName);
            }
            
            return nextName;
            
        } catch (Exception e) {
            strategyLogger.error("❌ Error generating next partition name for {}: {}", currentPartition, e.getMessage(), e);
            throw new RuntimeException("Failed to generate next partition name", e);
        }
    }
    
    /**
     * Generate split partition names for a given partition
     */
    public List<String> generateSplitPartitionNames(String originalPartition) {
        strategyLogger.debug("📝 Generating split partition names for: {}", originalPartition);
        
        try {
            Matcher matcher = PARTITION_PATTERN.matcher(originalPartition);
            
            if (!matcher.matches()) {
                throw new IllegalArgumentException("Invalid partition name format: " + originalPartition);
            }
            
            String year = matcher.group(1);
            String month = matcher.group(2);
            
            List<String> splitNames = new ArrayList<>();
            splitNames.add(String.format("p_%s%s_a", year, month)); // Rename original
            splitNames.add(String.format("p_%s%s_b", year, month)); // New partition
            
            strategyLogger.debug("📝 Generated split partition names: {}", splitNames);
            return splitNames;
            
        } catch (Exception e) {
            strategyLogger.error("❌ Error generating split partition names for {}: {}", originalPartition, e.getMessage(), e);
            throw new RuntimeException("Failed to generate split partition names", e);
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════════
    // DECISION LOGIC
    // ═══════════════════════════════════════════════════════════════════════════════════
    
    /**
     * Evaluate comprehensive partition decision
     */
    private PartitionDecision evaluatePartitionDecision(String partitionName, double sizeMB) {
        decisionLogger.debug("🤔 Evaluating decision for {} at {:.1f}MB", partitionName, sizeMB);
        
        PartitionDecision decision = new PartitionDecision(partitionName, sizeMB);
        
        // Size-based evaluation
        if (sizeMB > partitionConfig.getEmergencyThresholdMB()) {
            decision.setAction(PartitionAction.EMERGENCY_SPLIT);
            decision.setPriority(Priority.CRITICAL);
            decision.setReason("Exceeds emergency threshold");
        } else if (sizeMB > partitionConfig.getCriticalThresholdMB()) {
            decision.setAction(PartitionAction.SPLIT_REQUIRED);
            decision.setPriority(Priority.HIGH);
            decision.setReason("Exceeds critical threshold");
        } else if (sizeMB > partitionConfig.getWarningThresholdMB()) {
            decision.setAction(PartitionAction.MONITOR_CLOSELY);
            decision.setPriority(Priority.MEDIUM);
            decision.setReason("Approaching critical threshold");
        } else {
            decision.setAction(PartitionAction.NO_ACTION);
            decision.setPriority(Priority.LOW);
            decision.setReason("Within normal limits");
        }
        
        // Additional factors
        evaluateGrowthTrend(decision);
        evaluateTimeBasedFactors(decision);
        evaluateSystemLoad(decision);
        
        return decision;
    }
    
    /**
     * Evaluate growth trend factors
     */
    private void evaluateGrowthTrend(PartitionDecision decision) {
        try {
            // This is a placeholder for growth trend analysis
            // Future implementation could analyze:
            // - Historical size growth rates
            // - Time-based projections
            // - Seasonal patterns
            
            decisionLogger.debug("📈 Growth trend evaluation completed for {}", decision.getPartitionName());
            
        } catch (Exception e) {
            decisionLogger.error("❌ Error evaluating growth trend: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Evaluate time-based factors
     */
    private void evaluateTimeBasedFactors(PartitionDecision decision) {
        try {
            String partitionName = decision.getPartitionName();
            
            // Check if this is current month partition
            String currentPartition = partitionUtils.getCurrentPartitionName();
            boolean isCurrentMonth = partitionName.equals(currentPartition);
            
            if (isCurrentMonth && decision.getAction() == PartitionAction.SPLIT_REQUIRED) {
                // More aggressive for current month
                decision.setPriority(Priority.CRITICAL);
                decision.addNote("Current month partition requires immediate attention");
            }
            
            decisionLogger.debug("📅 Time-based evaluation: current={}, adjusted priority={}", 
                               isCurrentMonth, decision.getPriority());
            
        } catch (Exception e) {
            decisionLogger.error("❌ Error evaluating time-based factors: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Evaluate system load factors
     */
    private void evaluateSystemLoad(PartitionDecision decision) {
        try {
            // This is a placeholder for system load analysis
            // Future implementation could consider:
            // - Database performance metrics
            // - Query response times
            // - CPU and memory usage
            // - Concurrent connection counts
            
            decisionLogger.debug("⚡ System load evaluation completed for {}", decision.getPartitionName());
            
        } catch (Exception e) {
            decisionLogger.error("❌ Error evaluating system load: {}", e.getMessage(), e);
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════════
    // UTILITY METHODS
    // ═══════════════════════════════════════════════════════════════════════════════════
    
    /**
     * Check if partition is already split
     */
    private boolean isPartitionSplit(String partitionName) {
        Matcher matcher = PARTITION_PATTERN.matcher(partitionName);
        boolean isSplit = matcher.matches() && matcher.group(3) != null;
        
        strategyLogger.debug("🔍 Partition {} split status: {}", partitionName, isSplit);
        return isSplit;
    }
    
    /**
     * Check if partition can be split
     */
    private boolean canPartitionBeSplit(String partitionName) {
        try {
            Matcher matcher = PARTITION_PATTERN.matcher(partitionName);
            
            if (!matcher.matches()) {
                return false;
            }
            
            String suffix = matcher.group(3);
            
            if (suffix == null) {
                return true; // Can always split unsplit partition
            }
            
            char suffixChar = suffix.charAt(0);
            boolean canSplit = suffixChar < 'z';
            
            strategyLogger.debug("🔍 Partition {} can be split: {} (current suffix: {})", 
                               partitionName, canSplit, suffix);
            
            return canSplit;
            
        } catch (Exception e) {
            strategyLogger.error("❌ Error checking if partition can be split: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Get partition base name (without suffix)
     */
    public String getPartitionBaseName(String partitionName) {
        Matcher matcher = PARTITION_PATTERN.matcher(partitionName);
        
        if (matcher.matches()) {
            String year = matcher.group(1);
            String month = matcher.group(2);
            return String.format("p_%s%s", year, month);
        }
        
        return partitionName;
    }
    
    /**
     * Get all related partitions for a base name
     */
    public List<String> getRelatedPartitions(String baseName) {
        strategyLogger.debug("🔍 Finding related partitions for base: {}", baseName);
        
        try {
            List<String> relatedPartitions = new ArrayList<>();
            List<Map<String, Object>> allPartitions = partitionUtils.getAllPartitionInfo();
            
            String basePattern = baseName.replace("p_", "");
            
            for (Map<String, Object> partition : allPartitions) {
                String name = (String) partition.get("name");
                if (name != null && name.startsWith("p_" + basePattern)) {
                    relatedPartitions.add(name);
                }
            }
            
            Collections.sort(relatedPartitions);
            strategyLogger.debug("🔍 Found {} related partitions for {}: {}", 
                               relatedPartitions.size(), baseName, relatedPartitions);
            
            return relatedPartitions;
            
        } catch (Exception e) {
            strategyLogger.error("❌ Error finding related partitions for {}: {}", baseName, e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════════
    // LOGGING AND MONITORING
    // ═══════════════════════════════════════════════════════════════════════════════════
    
    /**
     * Log decision details
     */
    private void logDecision(String partitionName, double sizeMB, PartitionDecision decision) {
        String level = partitionConfig.getThresholdLevel(sizeMB);
        
        switch (decision.getPriority()) {
            case CRITICAL:
                alertLogger.error("🚨 CRITICAL DECISION: {} ({:.1f}MB, {}) - {}", 
                                partitionName, sizeMB, level, decision.getReason());
                break;
            case HIGH:
                alertLogger.warn("⚠️ HIGH PRIORITY: {} ({:.1f}MB, {}) - {}", 
                               partitionName, sizeMB, level, decision.getReason());
                break;
            case MEDIUM:
                strategyLogger.warn("📋 MEDIUM PRIORITY: {} ({:.1f}MB, {}) - {}", 
                                  partitionName, sizeMB, level, decision.getReason());
                break;
            default:
                strategyLogger.debug("✅ LOW PRIORITY: {} ({:.1f}MB, {}) - {}", 
                                   partitionName, sizeMB, level, decision.getReason());
                break;
        }
        
        if (!decision.getNotes().isEmpty()) {
            decision.getNotes().forEach(note -> decisionLogger.info("📝 Note for {}: {}", partitionName, note));
        }
    }
    
    /**
     * Get strategy statistics
     */
    public Map<String, Object> getStrategyStatistics() {
        strategyLogger.debug("📊 Calculating strategy statistics...");
        
        try {
            Map<String, Object> stats = new HashMap<>();
            List<Map<String, Object>> allPartitions = partitionUtils.getAllPartitionInfo();
            
            int totalPartitions = allPartitions.size();
            int splitPartitions = 0;
            int oversizedPartitions = 0;
            
            for (Map<String, Object> partition : allPartitions) {
                String name = (String) partition.get("name");
                Double sizeMB = (Double) partition.get("sizeMB");
                
                if (name != null && isPartitionSplit(name)) {
                    splitPartitions++;
                }
                
                if (sizeMB != null && sizeMB > partitionConfig.getCriticalThresholdMB()) {
                    oversizedPartitions++;
                }
            }
            
            stats.put("totalPartitions", totalPartitions);
            stats.put("splitPartitions", splitPartitions);
            stats.put("oversizedPartitions", oversizedPartitions);
            stats.put("splitPercentage", totalPartitions > 0 ? (double) splitPartitions / totalPartitions * 100 : 0);
            stats.put("oversizedPercentage", totalPartitions > 0 ? (double) oversizedPartitions / totalPartitions * 100 : 0);
            
            return stats;
            
        } catch (Exception e) {
            strategyLogger.error("❌ Error calculating strategy statistics: {}", e.getMessage(), e);
            return new HashMap<>();
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════════
    // INNER CLASSES
    // ═══════════════════════════════════════════════════════════════════════════════════
    
    /**
     * Partition decision result
     */
    public static class PartitionDecision {
        private final String partitionName;
        private final double sizeMB;
        private PartitionAction action = PartitionAction.NO_ACTION;
        private Priority priority = Priority.LOW;
        private String reason = "";
        private List<String> notes = new ArrayList<>();
        
        public PartitionDecision(String partitionName, double sizeMB) {
            this.partitionName = partitionName;
            this.sizeMB = sizeMB;
        }
        
        public boolean shouldCreateNew() {
            return action == PartitionAction.SPLIT_REQUIRED || action == PartitionAction.EMERGENCY_SPLIT;
        }
        
        public boolean shouldSplit() {
            return action == PartitionAction.SPLIT_REQUIRED || action == PartitionAction.EMERGENCY_SPLIT;
        }
        
        public void addNote(String note) {
            notes.add(note);
        }
        
        // Getters and setters
        public String getPartitionName() { return partitionName; }
        public double getSizeMB() { return sizeMB; }
        public PartitionAction getAction() { return action; }
        public void setAction(PartitionAction action) { this.action = action; }
        public Priority getPriority() { return priority; }
        public void setPriority(Priority priority) { this.priority = priority; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
        public List<String> getNotes() { return notes; }
    }
    
    /**
     * Partition action enumeration
     */
    public enum PartitionAction {
        NO_ACTION,
        MONITOR_CLOSELY,
        SPLIT_REQUIRED,
        EMERGENCY_SPLIT
    }
    
    /**
     * Priority enumeration
     */
    public enum Priority {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }
}