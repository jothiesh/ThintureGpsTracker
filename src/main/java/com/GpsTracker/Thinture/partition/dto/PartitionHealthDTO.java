package com.GpsTracker.Thinture.partition.dto;
//================================================================================================
//PartitionHealthDTO.java - PARTITION HEALTH STATUS SUMMARY
//================================================================================================




import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
* Data Transfer Object for partition health status API responses
* Provides quick health overview and alert information
*/
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Partition health status and alerts")
public class PartitionHealthDTO {

 // ═══════════════════════════════════════════════════════════════════════════════════
 // BASIC HEALTH INFORMATION
 // ═══════════════════════════════════════════════════════════════════════════════════

 @JsonProperty("partitionName")
 @Schema(description = "Partition name", example = "p_202506", required = true)
 private String partitionName;

 @JsonProperty("overallStatus")
 @Schema(description = "Overall health status", example = "HEALTHY", allowableValues = {"HEALTHY", "WARNING", "CRITICAL", "ERROR", "UNKNOWN"})
 private String overallStatus;

 @JsonProperty("healthScore")
 @Schema(description = "Health score from 0-100", example = "87", minimum = "0", maximum = "100")
 private Integer healthScore;

 @JsonProperty("isHealthy")
 @Schema(description = "Quick boolean health indicator", example = "true")
 private Boolean isHealthy;

 @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
 @JsonProperty("lastChecked")
 @Schema(description = "When health was last checked", example = "2025-06-15 14:30:00", required = true)
 private LocalDateTime lastChecked;

 @JsonProperty("checkDurationMs")
 @Schema(description = "Time taken for health check in milliseconds", example = "45")
 private Long checkDurationMs;
 
 
 

 // ═══════════════════════════════════════════════════════════════════════════════════
 // HEALTH INDICATORS
 // ═══════════════════════════════════════════════════════════════════════════════════

 @JsonProperty("healthIndicators")
 @Schema(description = "Individual health check results")
 private HealthIndicators healthIndicators;

 public static class HealthIndicators {
     @JsonProperty("sizeHealth")
     @Schema(description = "Size-related health status", example = "HEALTHY")
     private String sizeHealth;

     @JsonProperty("performanceHealth")
     @Schema(description = "Performance-related health status", example = "WARNING")
     private String performanceHealth;

     @JsonProperty("maintenanceHealth")
     @Schema(description = "Maintenance-related health status", example = "HEALTHY")
     private String maintenanceHealth;

     @JsonProperty("accessibilityHealth")
     @Schema(description = "Accessibility/connectivity health status", example = "HEALTHY")
     private String accessibilityHealth;

     @JsonProperty("dataIntegrityHealth")
     @Schema(description = "Data integrity health status", example = "HEALTHY")
     private String dataIntegrityHealth;

     // Constructors
     public HealthIndicators() {
         this.sizeHealth = "UNKNOWN";
         this.performanceHealth = "UNKNOWN";
         this.maintenanceHealth = "UNKNOWN";
         this.accessibilityHealth = "UNKNOWN";
         this.dataIntegrityHealth = "UNKNOWN";
     }

     // Getters and setters
     public String getSizeHealth() { return sizeHealth; }
     public void setSizeHealth(String sizeHealth) { this.sizeHealth = sizeHealth; }
     public String getPerformanceHealth() { return performanceHealth; }
     public void setPerformanceHealth(String performanceHealth) { this.performanceHealth = performanceHealth; }
     public String getMaintenanceHealth() { return maintenanceHealth; }
     public void setMaintenanceHealth(String maintenanceHealth) { this.maintenanceHealth = maintenanceHealth; }
     public String getAccessibilityHealth() { return accessibilityHealth; }
     public void setAccessibilityHealth(String accessibilityHealth) { this.accessibilityHealth = accessibilityHealth; }
     public String getDataIntegrityHealth() { return dataIntegrityHealth; }
     public void setDataIntegrityHealth(String dataIntegrityHealth) { this.dataIntegrityHealth = dataIntegrityHealth; }
 }

 // ═══════════════════════════════════════════════════════════════════════════════════
 // ALERTS AND WARNINGS
 // ═══════════════════════════════════════════════════════════════════════════════════

 @JsonProperty("alerts")
 @Schema(description = "Current health alerts")
 private List<HealthAlert> alerts;

 public static class HealthAlert {
     @JsonProperty("level")
     @Schema(description = "Alert severity level", example = "WARNING", allowableValues = {"INFO", "WARNING", "CRITICAL"})
     private String level;

     @JsonProperty("category")
     @Schema(description = "Alert category", example = "PERFORMANCE")
     private String category;

     @JsonProperty("message")
     @Schema(description = "Alert message", example = "Query performance has degraded by 15% over last 24 hours")
     private String message;

     @JsonProperty("metric")
     @Schema(description = "Related metric name", example = "averageQueryTimeMs")
     private String metric;

     @JsonProperty("value")
     @Schema(description = "Current metric value", example = "125.5")
     private Double value;

     @JsonProperty("threshold")
     @Schema(description = "Alert threshold value", example = "100.0")
     private Double threshold;

     @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
     @JsonProperty("firstDetected")
     @Schema(description = "When alert was first detected", example = "2025-06-15 10:15:00")
     private LocalDateTime firstDetected;

     @JsonProperty("recommendation")
     @Schema(description = "Recommended action", example = "Consider running partition optimization")
     private String recommendation;

     // Constructors
     public HealthAlert() {}

     public HealthAlert(String level, String category, String message) {
         this.level = level;
         this.category = category;
         this.message = message;
         this.firstDetected = LocalDateTime.now();
     }

     public HealthAlert(String level, String category, String message, String metric, Double value, Double threshold) {
         this(level, category, message);
         this.metric = metric;
         this.value = value;
         this.threshold = threshold;
     }

     // Getters and setters
     public String getLevel() { return level; }
     public void setLevel(String level) { this.level = level; }
     public String getCategory() { return category; }
     public void setCategory(String category) { this.category = category; }
     public String getMessage() { return message; }
     public void setMessage(String message) { this.message = message; }
     public String getMetric() { return metric; }
     public void setMetric(String metric) { this.metric = metric; }
     public Double getValue() { return value; }
     public void setValue(Double value) { this.value = value; }
     public Double getThreshold() { return threshold; }
     public void setThreshold(Double threshold) { this.threshold = threshold; }
     public LocalDateTime getFirstDetected() { return firstDetected; }
     public void setFirstDetected(LocalDateTime firstDetected) { this.firstDetected = firstDetected; }
     public String getRecommendation() { return recommendation; }
     public void setRecommendation(String recommendation) { this.recommendation = recommendation; }
 }

 // ═══════════════════════════════════════════════════════════════════════════════════
 // HEALTH METRICS SUMMARY
 // ═══════════════════════════════════════════════════════════════════════════════════

 @JsonProperty("quickMetrics")
 @Schema(description = "Quick health metrics summary")
 private QuickMetrics quickMetrics;

 public static class QuickMetrics {
     @JsonProperty("recordCount")
     @Schema(description = "Current record count", example = "1250000")
     private Long recordCount;

     @JsonProperty("sizeMB")
     @Schema(description = "Current size in MB", example = "4250.75")
     private Double sizeMB;

     @JsonProperty("fragmentationPercentage")
     @Schema(description = "Fragmentation level percentage", example = "5.2")
     private Double fragmentationPercentage;

     @JsonProperty("avgQueryTimeMs")
     @Schema(description = "Average query time in milliseconds", example = "125.5")
     private Double avgQueryTimeMs;

     @JsonProperty("slowQueryPercentage")
     @Schema(description = "Percentage of slow queries", example = "2.5")
     private Double slowQueryPercentage;

     @JsonProperty("indexEfficiency")
     @Schema(description = "Index efficiency percentage", example = "87.5")
     private Double indexEfficiency;

     @JsonProperty("lastOptimized")
     @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
     @Schema(description = "Last optimization timestamp", example = "2025-06-10 03:00:00")
     private LocalDateTime lastOptimized;

     // Getters and setters
     public Long getRecordCount() { return recordCount; }
     public void setRecordCount(Long recordCount) { this.recordCount = recordCount; }
     public Double getSizeMB() { return sizeMB; }
     public void setSizeMB(Double sizeMB) { this.sizeMB = sizeMB; }
     public Double getFragmentationPercentage() { return fragmentationPercentage; }
     public void setFragmentationPercentage(Double fragmentationPercentage) { this.fragmentationPercentage = fragmentationPercentage; }
     public Double getAvgQueryTimeMs() { return avgQueryTimeMs; }
     public void setAvgQueryTimeMs(Double avgQueryTimeMs) { this.avgQueryTimeMs = avgQueryTimeMs; }
     public Double getSlowQueryPercentage() { return slowQueryPercentage; }
     public void setSlowQueryPercentage(Double slowQueryPercentage) { this.slowQueryPercentage = slowQueryPercentage; }
     public Double getIndexEfficiency() { return indexEfficiency; }
     public void setIndexEfficiency(Double indexEfficiency) { this.indexEfficiency = indexEfficiency; }
     public LocalDateTime getLastOptimized() { return lastOptimized; }
     public void setLastOptimized(LocalDateTime lastOptimized) { this.lastOptimized = lastOptimized; }
 }

 // ═══════════════════════════════════════════════════════════════════════════════════
 // MAINTENANCE STATUS
 // ═══════════════════════════════════════════════════════════════════════════════════

 @JsonProperty("maintenanceStatus")
 @Schema(description = "Maintenance and optimization status")
 private MaintenanceStatus maintenanceStatus;

 public static class MaintenanceStatus {
     @JsonProperty("needsOptimization")
     @Schema(description = "Whether partition needs optimization", example = "false")
     private Boolean needsOptimization;

     @JsonProperty("needsAnalysis")
     @Schema(description = "Whether partition needs analysis", example = "true")
     private Boolean needsAnalysis;

     @JsonProperty("daysSinceLastOptimization")
     @Schema(description = "Days since last optimization", example = "5")
     private Integer daysSinceLastOptimization;

     @JsonProperty("daysSinceLastAnalysis")
     @Schema(description = "Days since last analysis", example = "2")
     private Integer daysSinceLastAnalysis;

     @JsonProperty("recommendedActions")
     @Schema(description = "List of recommended maintenance actions")
     private List<String> recommendedActions;

     @JsonProperty("urgentAction")
     @Schema(description = "Most urgent action needed", example = "OPTIMIZE_INDEXES")
     private String urgentAction;

     // Constructors
     public MaintenanceStatus() {
         this.needsOptimization = false;
         this.needsAnalysis = false;
         this.recommendedActions = new ArrayList<>();
     }

     // Getters and setters
     public Boolean getNeedsOptimization() { return needsOptimization; }
     public void setNeedsOptimization(Boolean needsOptimization) { this.needsOptimization = needsOptimization; }
     public Boolean getNeedsAnalysis() { return needsAnalysis; }
     public void setNeedsAnalysis(Boolean needsAnalysis) { this.needsAnalysis = needsAnalysis; }
     public Integer getDaysSinceLastOptimization() { return daysSinceLastOptimization; }
     public void setDaysSinceLastOptimization(Integer daysSinceLastOptimization) { this.daysSinceLastOptimization = daysSinceLastOptimization; }
     public Integer getDaysSinceLastAnalysis() { return daysSinceLastAnalysis; }
     public void setDaysSinceLastAnalysis(Integer daysSinceLastAnalysis) { this.daysSinceLastAnalysis = daysSinceLastAnalysis; }
     public List<String> getRecommendedActions() { return recommendedActions; }
     public void setRecommendedActions(List<String> recommendedActions) { this.recommendedActions = recommendedActions; }
     public String getUrgentAction() { return urgentAction; }
     public void setUrgentAction(String urgentAction) { this.urgentAction = urgentAction; }
 }

 // ═══════════════════════════════════════════════════════════════════════════════════
 // CONSTRUCTORS
 // ═══════════════════════════════════════════════════════════════════════════════════

 /**
  * Default constructor
  */
 public PartitionHealthDTO() {
     this.lastChecked = LocalDateTime.now();
     this.overallStatus = "UNKNOWN";
     this.isHealthy = false;
     this.alerts = new ArrayList<>();
     this.healthIndicators = new HealthIndicators();
     this.quickMetrics = new QuickMetrics();
     this.maintenanceStatus = new MaintenanceStatus();
 }

 /**
  * Constructor with partition name
  */
 public PartitionHealthDTO(String partitionName) {
     this();
     this.partitionName = partitionName;
 }

 /**
  * Constructor with basic health info
  */
 public PartitionHealthDTO(String partitionName, Integer healthScore, String overallStatus) {
     this(partitionName);
     this.healthScore = healthScore;
     this.overallStatus = overallStatus;
     this.isHealthy = determineHealthyStatus(healthScore, overallStatus);
 }

 // ═══════════════════════════════════════════════════════════════════════════════════
 // UTILITY METHODS
 // ═══════════════════════════════════════════════════════════════════════════════════

 /**
  * Determine if partition is healthy based on score and status
  */
 private boolean determineHealthyStatus(Integer score, String status) {
     if (score != null && score < 70) return false;
     if (status != null && (status.equals("CRITICAL") || status.equals("ERROR"))) return false;
     return true;
 }

 /**
  * Add a health alert
  */
 public void addAlert(String level, String category, String message) {
     if (this.alerts == null) {
         this.alerts = new ArrayList<>();
     }
     this.alerts.add(new HealthAlert(level, category, message));
     updateOverallStatusFromAlerts();
 }

 /**
  * Add a health alert with metric details
  */
 public void addAlert(String level, String category, String message, String metric, Double value, Double threshold) {
     if (this.alerts == null) {
         this.alerts = new ArrayList<>();
     }
     this.alerts.add(new HealthAlert(level, category, message, metric, value, threshold));
     updateOverallStatusFromAlerts();
 }

 /**
  * Update overall status based on alerts
  */
 private void updateOverallStatusFromAlerts() {
     if (alerts == null || alerts.isEmpty()) {
         this.overallStatus = "HEALTHY";
         this.isHealthy = true;
         return;
     }

     boolean hasCritical = alerts.stream().anyMatch(alert -> "CRITICAL".equals(alert.getLevel()));
     boolean hasWarning = alerts.stream().anyMatch(alert -> "WARNING".equals(alert.getLevel()));

     if (hasCritical) {
         this.overallStatus = "CRITICAL";
         this.isHealthy = false;
     } else if (hasWarning) {
         this.overallStatus = "WARNING";
         this.isHealthy = (healthScore != null && healthScore >= 70);
     } else {
         this.overallStatus = "HEALTHY";
         this.isHealthy = true;
     }
 }

 /**
  * Calculate health score from various metrics
  */
 public void calculateHealthScore() {
     if (quickMetrics == null) {
         this.healthScore = 0;
         return;
     }

     int score = 100;

     // Deduct points for performance issues
     if (quickMetrics.getSlowQueryPercentage() != null) {
         score -= (int) (quickMetrics.getSlowQueryPercentage() * 2); // 2 points per 1% slow queries
     }

     // Deduct points for fragmentation
     if (quickMetrics.getFragmentationPercentage() != null) {
         if (quickMetrics.getFragmentationPercentage() > 10) {
             score -= (int) (quickMetrics.getFragmentationPercentage() - 10); // 1 point per 1% above 10%
         }
     }

     // Deduct points for poor index efficiency
     if (quickMetrics.getIndexEfficiency() != null) {
         if (quickMetrics.getIndexEfficiency() < 80) {
             score -= (int) (80 - quickMetrics.getIndexEfficiency()); // 1 point per 1% below 80%
         }
     }

     // Deduct points for query performance
     if (quickMetrics.getAvgQueryTimeMs() != null) {
         if (quickMetrics.getAvgQueryTimeMs() > 100) {
             score -= Math.min(20, (int) ((quickMetrics.getAvgQueryTimeMs() - 100) / 10)); // Up to 20 points
         }
     }

     this.healthScore = Math.max(0, Math.min(100, score));
     this.isHealthy = this.healthScore >= 70;
 }

 /**
  * Get alert counts by level
  */
 public Map<String, Integer> getAlertCounts() {
     Map<String, Integer> counts = new HashMap<>();
     counts.put("CRITICAL", 0);
     counts.put("WARNING", 0);
     counts.put("INFO", 0);

     if (alerts != null) {
         for (HealthAlert alert : alerts) {
             String level = alert.getLevel();
             counts.put(level, counts.getOrDefault(level, 0) + 1);
         }
     }

     return counts;
 }

 /**
  * Get health summary
  */
 public Map<String, Object> getHealthSummary() {
     Map<String, Object> summary = new HashMap<>();
     summary.put("partitionName", partitionName);
     summary.put("overallStatus", overallStatus);
     summary.put("healthScore", healthScore);
     summary.put("isHealthy", isHealthy);
     summary.put("lastChecked", lastChecked);
     summary.put("alertCounts", getAlertCounts());

     if (quickMetrics != null) {
         summary.put("recordCount", quickMetrics.getRecordCount());
         summary.put("sizeMB", quickMetrics.getSizeMB());
     }

     return summary;
 }

 /**
  * Check if urgent action is needed
  */
 public boolean needsUrgentAction() {
     if (overallStatus != null && overallStatus.equals("CRITICAL")) return true;
     if (healthScore != null && healthScore < 50) return true;
     if (maintenanceStatus != null && maintenanceStatus.getUrgentAction() != null) return true;
     return false;
 }

 /**
  * Get health status icon/emoji
  */
 public String getHealthIcon() {
     if (overallStatus == null) return "❓";
     
     switch (overallStatus) {
         case "HEALTHY": return "✅";
         case "WARNING": return "⚠️";
         case "CRITICAL": return "🚨";
         case "ERROR": return "❌";
         default: return "❓";
     }
 }

 /**
  * Create from PartitionMetricsDTO
  */
 public static PartitionHealthDTO fromMetrics(PartitionMetricsDTO metrics) {
     PartitionHealthDTO health = new PartitionHealthDTO(metrics.getPartitionName());
     
     // Copy basic health score
     Integer overallScore = metrics.calculateOverallHealthScore();
     health.setHealthScore(overallScore);
     
     // Set overall status based on score
     if (overallScore >= 90) {
         health.setOverallStatus("HEALTHY");
     } else if (overallScore >= 70) {
         health.setOverallStatus("WARNING");
     } else {
         health.setOverallStatus("CRITICAL");
     }
     
     // Copy quick metrics
     if (health.quickMetrics == null) health.quickMetrics = new QuickMetrics();
     
     if (metrics.getRecordMetrics() != null) {
         health.quickMetrics.setRecordCount(metrics.getRecordMetrics().getCurrentRecordCount());
     }
     
     if (metrics.getSizeMetrics() != null) {
         health.quickMetrics.setSizeMB(metrics.getSizeMetrics().getCurrentSizeMB());
     }
     
     if (metrics.getPerformanceMetrics() != null) {
         health.quickMetrics.setAvgQueryTimeMs(metrics.getPerformanceMetrics().getAverageQueryTimeMs());
         health.quickMetrics.setSlowQueryPercentage(metrics.getPerformanceMetrics().getSlowQueryPercentage());
         health.quickMetrics.setLastOptimized(metrics.getPerformanceMetrics().getLastOptimization());
     }
     
     if (metrics.getEfficiencyMetrics() != null) {
         health.quickMetrics.setFragmentationPercentage(metrics.getEfficiencyMetrics().getFragmentationLevel());
         health.quickMetrics.setIndexEfficiency(metrics.getEfficiencyMetrics().getIndexEfficiency());
     }
     
     // Generate alerts based on metrics
     health.generateAlertsFromMetrics(metrics);
     
     health.calculateHealthScore();
     health.updateOverallStatusFromAlerts();
     
     return health;
 }

 /**
  * Generate alerts from metrics
  */
 private void generateAlertsFromMetrics(PartitionMetricsDTO metrics) {
     // Performance alerts
     if (metrics.getPerformanceMetrics() != null) {
         Double slowQueryPct = metrics.getPerformanceMetrics().getSlowQueryPercentage();
         if (slowQueryPct != null && slowQueryPct > 5.0) {
             addAlert("WARNING", "PERFORMANCE", 
                     String.format("High slow query percentage: %.1f%%", slowQueryPct),
                     "slowQueryPercentage", slowQueryPct, 5.0);
         }
     }

     // Efficiency alerts
     if (metrics.getEfficiencyMetrics() != null) {
         Double fragmentation = metrics.getEfficiencyMetrics().getFragmentationLevel();
         if (fragmentation != null && fragmentation > 15.0) {
             addAlert("CRITICAL", "MAINTENANCE", 
                     String.format("High fragmentation level: %.1f%%", fragmentation),
                     "fragmentationLevel", fragmentation, 15.0);
         } else if (fragmentation != null && fragmentation > 10.0) {
             addAlert("WARNING", "MAINTENANCE", 
                     String.format("Moderate fragmentation level: %.1f%%", fragmentation),
                     "fragmentationLevel", fragmentation, 10.0);
         }
     }

     // Add recommendations as info alerts
     if (metrics.getRecommendations() != null) {
         for (PartitionMetricsDTO.OptimizationRecommendation rec : metrics.getRecommendations()) {
             if ("HIGH".equals(rec.getPriority()) || "CRITICAL".equals(rec.getPriority())) {
                 addAlert("WARNING", "OPTIMIZATION", rec.getDescription());
             }
         }
     }
 }

 // ═══════════════════════════════════════════════════════════════════════════════════
 // GETTERS AND SETTERS
 // ═══════════════════════════════════════════════════════════════════════════════════

 public String getPartitionName() { return partitionName; }
 public void setPartitionName(String partitionName) { this.partitionName = partitionName; }

 public String getOverallStatus() { return overallStatus; }
 public void setOverallStatus(String overallStatus) { this.overallStatus = overallStatus; }

 public String getHealthStatus() {
     return this.overallStatus;
 }

 public Integer getHealthScore() { return healthScore; }
 public void setHealthScore(Integer healthScore) { 
     this.healthScore = healthScore;
     this.isHealthy = determineHealthyStatus(healthScore, overallStatus);
 }

 public Boolean getIsHealthy() { return isHealthy; }
 public void setIsHealthy(Boolean isHealthy) { this.isHealthy = isHealthy; }

 public LocalDateTime getLastChecked() { return lastChecked; }
 public void setLastChecked(LocalDateTime lastChecked) { this.lastChecked = lastChecked; }

 public Long getCheckDurationMs() { return checkDurationMs; }
 public void setCheckDurationMs(Long checkDurationMs) { this.checkDurationMs = checkDurationMs; }

 public HealthIndicators getHealthIndicators() { return healthIndicators; }
 public void setHealthIndicators(HealthIndicators healthIndicators) { this.healthIndicators = healthIndicators; }

 public List<HealthAlert> getAlerts() { return alerts; }
 public void setAlerts(List<HealthAlert> alerts) { this.alerts = alerts; }

 public QuickMetrics getQuickMetrics() { return quickMetrics; }
 public void setQuickMetrics(QuickMetrics quickMetrics) { this.quickMetrics = quickMetrics; }

 public MaintenanceStatus getMaintenanceStatus() { return maintenanceStatus; }
 public void setMaintenanceStatus(MaintenanceStatus maintenanceStatus) { this.maintenanceStatus = maintenanceStatus; }
}