package com.GpsTracker.Thinture.partition.dto;
//================================================================================================
//PartitionMetricsDTO.java - STRUCTURED PERFORMANCE METRICS RESPONSE
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
* Data Transfer Object for partition performance metrics API responses
* Provides structured format for performance analytics and monitoring data
*/
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Partition performance metrics and analytics")
public class PartitionMetricsDTO {

 // ═══════════════════════════════════════════════════════════════════════════════════
 // BASIC METRICS INFORMATION
 // ═══════════════════════════════════════════════════════════════════════════════════

 @JsonProperty("partitionName")
 @Schema(description = "Partition name", example = "p_202506")
 private String partitionName;

 @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
 @JsonProperty("collectionTime")
 @Schema(description = "When metrics were collected", example = "2025-06-15 14:30:00", required = true)
 private LocalDateTime collectionTime;

 @JsonProperty("metricsVersion")
 @Schema(description = "Metrics format version", example = "1.0")
 private String metricsVersion = "1.0";

 @JsonProperty("collectionDurationMs")
 @Schema(description = "Time taken to collect metrics in milliseconds", example = "125")
 private Long collectionDurationMs;

 // ═══════════════════════════════════════════════════════════════════════════════════
 // SIZE AND STORAGE METRICS
 // ═══════════════════════════════════════════════════════════════════════════════════

 @JsonProperty("sizeMetrics")
 @Schema(description = "Size and storage related metrics")
 private SizeMetrics sizeMetrics;

 public static class SizeMetrics {
     @JsonProperty("currentSizeMB")
     @Schema(description = "Current partition size in MB", example = "4250.75")
     private Double currentSizeMB;

     @JsonProperty("dataSizeMB")
     @Schema(description = "Data size in MB", example = "3800.50")
     private Double dataSizeMB;

     @JsonProperty("indexSizeMB")
     @Schema(description = "Index size in MB", example = "450.25")
     private Double indexSizeMB;

     @JsonProperty("growthSinceLast")
     @Schema(description = "Size growth since last collection in MB", example = "125.5")
     private Double growthSinceLast;

     @JsonProperty("growthRatePerHour")
     @Schema(description = "Average growth rate in MB per hour", example = "85.3")
     private Double growthRatePerHour;

     @JsonProperty("projectedSizeIn30Days")
     @Schema(description = "Projected size in 30 days in MB", example = "6500.0")
     private Double projectedSizeIn30Days;

     @JsonProperty("compressionRatio")
     @Schema(description = "Compression ratio percentage", example = "65.5")
     private Double compressionRatio;

     @JsonProperty("storageTier")
     @Schema(description = "Current storage tier", example = "WARM")
     private String storageTier;

     // Getters and setters
     public Double getCurrentSizeMB() { return currentSizeMB; }
     public void setCurrentSizeMB(Double currentSizeMB) { this.currentSizeMB = currentSizeMB; }
     public Double getDataSizeMB() { return dataSizeMB; }
     public void setDataSizeMB(Double dataSizeMB) { this.dataSizeMB = dataSizeMB; }
     public Double getIndexSizeMB() { return indexSizeMB; }
     public void setIndexSizeMB(Double indexSizeMB) { this.indexSizeMB = indexSizeMB; }
     public Double getGrowthSinceLast() { return growthSinceLast; }
     public void setGrowthSinceLast(Double growthSinceLast) { this.growthSinceLast = growthSinceLast; }
     public Double getGrowthRatePerHour() { return growthRatePerHour; }
     public void setGrowthRatePerHour(Double growthRatePerHour) { this.growthRatePerHour = growthRatePerHour; }
     public Double getProjectedSizeIn30Days() { return projectedSizeIn30Days; }
     public void setProjectedSizeIn30Days(Double projectedSizeIn30Days) { this.projectedSizeIn30Days = projectedSizeIn30Days; }
     public Double getCompressionRatio() { return compressionRatio; }
     public void setCompressionRatio(Double compressionRatio) { this.compressionRatio = compressionRatio; }
     public String getStorageTier() { return storageTier; }
     public void setStorageTier(String storageTier) { this.storageTier = storageTier; }
 }

 // ═══════════════════════════════════════════════════════════════════════════════════
 // PERFORMANCE METRICS
 // ═══════════════════════════════════════════════════════════════════════════════════

 @JsonProperty("performanceMetrics")
 @Schema(description = "Query and operation performance metrics")
 private PerformanceMetrics performanceMetrics;

 public static class PerformanceMetrics {
     @JsonProperty("totalQueries")
     @Schema(description = "Total number of queries executed", example = "1250")
     private Long totalQueries;

     @JsonProperty("averageQueryTimeMs")
     @Schema(description = "Average query execution time in milliseconds", example = "125.5")
     private Double averageQueryTimeMs;

     @JsonProperty("minQueryTimeMs")
     @Schema(description = "Minimum query time in milliseconds", example = "15")
     private Long minQueryTimeMs;

     @JsonProperty("maxQueryTimeMs")
     @Schema(description = "Maximum query time in milliseconds", example = "2500")
     private Long maxQueryTimeMs;

     @JsonProperty("slowQueries")
     @Schema(description = "Number of queries exceeding threshold", example = "5")
     private Long slowQueries;

     @JsonProperty("slowQueryPercentage")
     @Schema(description = "Percentage of slow queries", example = "0.4")
     private Double slowQueryPercentage;

     @JsonProperty("queriesPerSecond")
     @Schema(description = "Average queries per second", example = "2.5")
     private Double queriesPerSecond;

     @JsonProperty("insertPerformance")
     @Schema(description = "Insert operation performance metrics")
     private InsertMetrics insertPerformance;

     @JsonProperty("lastOptimization")
     @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
     @Schema(description = "Last optimization timestamp", example = "2025-06-10 03:00:00")
     private LocalDateTime lastOptimization;

     // Getters and setters
     public Long getTotalQueries() { return totalQueries; }
     public void setTotalQueries(Long totalQueries) { this.totalQueries = totalQueries; }
     public Double getAverageQueryTimeMs() { return averageQueryTimeMs; }
     public void setAverageQueryTimeMs(Double averageQueryTimeMs) { this.averageQueryTimeMs = averageQueryTimeMs; }
     public Long getMinQueryTimeMs() { return minQueryTimeMs; }
     public void setMinQueryTimeMs(Long minQueryTimeMs) { this.minQueryTimeMs = minQueryTimeMs; }
     public Long getMaxQueryTimeMs() { return maxQueryTimeMs; }
     public void setMaxQueryTimeMs(Long maxQueryTimeMs) { this.maxQueryTimeMs = maxQueryTimeMs; }
     public Long getSlowQueries() { return slowQueries; }
     public void setSlowQueries(Long slowQueries) { this.slowQueries = slowQueries; }
     public Double getSlowQueryPercentage() { return slowQueryPercentage; }
     public void setSlowQueryPercentage(Double slowQueryPercentage) { this.slowQueryPercentage = slowQueryPercentage; }
     public Double getQueriesPerSecond() { return queriesPerSecond; }
     public void setQueriesPerSecond(Double queriesPerSecond) { this.queriesPerSecond = queriesPerSecond; }
     public InsertMetrics getInsertPerformance() { return insertPerformance; }
     public void setInsertPerformance(InsertMetrics insertPerformance) { this.insertPerformance = insertPerformance; }
     public LocalDateTime getLastOptimization() { return lastOptimization; }
     public void setLastOptimization(LocalDateTime lastOptimization) { this.lastOptimization = lastOptimization; }
 }

 public static class InsertMetrics {
     @JsonProperty("totalInserts")
     @Schema(description = "Total number of inserts", example = "50000")
     private Long totalInserts;

     @JsonProperty("averageInsertTimeMs")
     @Schema(description = "Average insert time in milliseconds", example = "2.5")
     private Double averageInsertTimeMs;

     @JsonProperty("insertsPerSecond")
     @Schema(description = "Average inserts per second", example = "3500")
     private Double insertsPerSecond;

     @JsonProperty("batchInsertEfficiency")
     @Schema(description = "Batch insert efficiency percentage", example = "95.5")
     private Double batchInsertEfficiency;

     // Getters and setters
     public Long getTotalInserts() { return totalInserts; }
     public void setTotalInserts(Long totalInserts) { this.totalInserts = totalInserts; }
     public Double getAverageInsertTimeMs() { return averageInsertTimeMs; }
     public void setAverageInsertTimeMs(Double averageInsertTimeMs) { this.averageInsertTimeMs = averageInsertTimeMs; }
     public Double getInsertsPerSecond() { return insertsPerSecond; }
     public void setInsertsPerSecond(Double insertsPerSecond) { this.insertsPerSecond = insertsPerSecond; }
     public Double getBatchInsertEfficiency() { return batchInsertEfficiency; }
     public void setBatchInsertEfficiency(Double batchInsertEfficiency) { this.batchInsertEfficiency = batchInsertEfficiency; }
 }

 // ═══════════════════════════════════════════════════════════════════════════════════
 // RECORD COUNT METRICS
 // ═══════════════════════════════════════════════════════════════════════════════════

 @JsonProperty("recordMetrics")
 @Schema(description = "Record count and growth metrics")
 private RecordMetrics recordMetrics;

 public static class RecordMetrics {
     @JsonProperty("currentRecordCount")
     @Schema(description = "Current number of records", example = "18500000")
     private Long currentRecordCount;

     @JsonProperty("recordsAddedSinceLast")
     @Schema(description = "Records added since last collection", example = "125000")
     private Long recordsAddedSinceLast;

     @JsonProperty("recordsPerHour")
     @Schema(description = "Average records added per hour", example = "75000")
     private Double recordsPerHour;

     @JsonProperty("recordsPerDay")
     @Schema(description = "Average records added per day", example = "1800000")
     private Double recordsPerDay;

     @JsonProperty("projectedRecordsIn30Days")
     @Schema(description = "Projected record count in 30 days", example = "72500000")
     private Long projectedRecordsIn30Days;

     @JsonProperty("recordDensity")
     @Schema(description = "Records per MB", example = "4365.5")
     private Double recordDensity;

     @JsonProperty("duplicateRecordPercentage")
     @Schema(description = "Estimated duplicate record percentage", example = "0.02")
     private Double duplicateRecordPercentage;

     // Getters and setters
     public Long getCurrentRecordCount() { return currentRecordCount; }
     public void setCurrentRecordCount(Long currentRecordCount) { this.currentRecordCount = currentRecordCount; }
     public Long getRecordsAddedSinceLast() { return recordsAddedSinceLast; }
     public void setRecordsAddedSinceLast(Long recordsAddedSinceLast) { this.recordsAddedSinceLast = recordsAddedSinceLast; }
     public Double getRecordsPerHour() { return recordsPerHour; }
     public void setRecordsPerHour(Double recordsPerHour) { this.recordsPerHour = recordsPerHour; }
     public Double getRecordsPerDay() { return recordsPerDay; }
     public void setRecordsPerDay(Double recordsPerDay) { this.recordsPerDay = recordsPerDay; }
     public Long getProjectedRecordsIn30Days() { return projectedRecordsIn30Days; }
     public void setProjectedRecordsIn30Days(Long projectedRecordsIn30Days) { this.projectedRecordsIn30Days = projectedRecordsIn30Days; }
     public Double getRecordDensity() { return recordDensity; }
     public void setRecordDensity(Double recordDensity) { this.recordDensity = recordDensity; }
     public Double getDuplicateRecordPercentage() { return duplicateRecordPercentage; }
     public void setDuplicateRecordPercentage(Double duplicateRecordPercentage) { this.duplicateRecordPercentage = duplicateRecordPercentage; }
 }

 // ═══════════════════════════════════════════════════════════════════════════════════
 // EFFICIENCY METRICS
 // ═══════════════════════════════════════════════════════════════════════════════════

 @JsonProperty("efficiencyMetrics")
 @Schema(description = "Partition efficiency and optimization metrics")
 private EfficiencyMetrics efficiencyMetrics;

 public static class EfficiencyMetrics {
     @JsonProperty("indexEfficiency")
     @Schema(description = "Index usage efficiency percentage", example = "85.5")
     private Double indexEfficiency;

     @JsonProperty("indexToDataRatio")
     @Schema(description = "Index to data size ratio percentage", example = "12.5")
     private Double indexToDataRatio;

     @JsonProperty("fragmentationLevel")
     @Schema(description = "Table fragmentation level percentage", example = "5.2")
     private Double fragmentationLevel;

     @JsonProperty("compressionEfficiency")
     @Schema(description = "Compression efficiency percentage", example = "68.5")
     private Double compressionEfficiency;

     @JsonProperty("partitionPruningEffectiveness")
     @Schema(description = "Partition pruning effectiveness percentage", example = "95.0")
     private Double partitionPruningEffectiveness;

     @JsonProperty("cacheHitRatio")
     @Schema(description = "Cache hit ratio percentage", example = "92.5")
     private Double cacheHitRatio;

     @JsonProperty("optimizationScore")
     @Schema(description = "Overall optimization score from 0-100", example = "87")
     private Integer optimizationScore;

     // Getters and setters
     public Double getIndexEfficiency() { return indexEfficiency; }
     public void setIndexEfficiency(Double indexEfficiency) { this.indexEfficiency = indexEfficiency; }
     public Double getIndexToDataRatio() { return indexToDataRatio; }
     public void setIndexToDataRatio(Double indexToDataRatio) { this.indexToDataRatio = indexToDataRatio; }
     public Double getFragmentationLevel() { return fragmentationLevel; }
     public void setFragmentationLevel(Double fragmentationLevel) { this.fragmentationLevel = fragmentationLevel; }
     public Double getCompressionEfficiency() { return compressionEfficiency; }
     public void setCompressionEfficiency(Double compressionEfficiency) { this.compressionEfficiency = compressionEfficiency; }
     public Double getPartitionPruningEffectiveness() { return partitionPruningEffectiveness; }
     public void setPartitionPruningEffectiveness(Double partitionPruningEffectiveness) { this.partitionPruningEffectiveness = partitionPruningEffectiveness; }
     public Double getCacheHitRatio() { return cacheHitRatio; }
     public void setCacheHitRatio(Double cacheHitRatio) { this.cacheHitRatio = cacheHitRatio; }
     public Integer getOptimizationScore() { return optimizationScore; }
     public void setOptimizationScore(Integer optimizationScore) { this.optimizationScore = optimizationScore; }
 }

 // ═══════════════════════════════════════════════════════════════════════════════════
 // TREND ANALYSIS
 // ═══════════════════════════════════════════════════════════════════════════════════

 @JsonProperty("trendAnalysis")
 @Schema(description = "Historical trend analysis")
 private TrendAnalysis trendAnalysis;

 public static class TrendAnalysis {
     @JsonProperty("growthTrend")
     @Schema(description = "Growth trend", example = "INCREASING", allowableValues = {"INCREASING", "DECREASING", "STABLE", "VOLATILE"})
     private String growthTrend;

     @JsonProperty("performanceTrend")
     @Schema(description = "Performance trend", example = "IMPROVING", allowableValues = {"IMPROVING", "DEGRADING", "STABLE"})
     private String performanceTrend;

     @JsonProperty("dataPointsAnalyzed")
     @Schema(description = "Number of historical data points analyzed", example = "168")
     private Integer dataPointsAnalyzed;

     @JsonProperty("trendConfidence")
     @Schema(description = "Confidence level of trend analysis", example = "HIGH", allowableValues = {"LOW", "MEDIUM", "HIGH"})
     private String trendConfidence;

     @JsonProperty("seasonalPatterns")
     @Schema(description = "Detected seasonal patterns")
     private List<String> seasonalPatterns;

     @JsonProperty("anomaliesDetected")
     @Schema(description = "Number of anomalies detected", example = "2")
     private Integer anomaliesDetected;

     // Getters and setters
     public String getGrowthTrend() { return growthTrend; }
     public void setGrowthTrend(String growthTrend) { this.growthTrend = growthTrend; }
     public String getPerformanceTrend() { return performanceTrend; }
     public void setPerformanceTrend(String performanceTrend) { this.performanceTrend = performanceTrend; }
     public Integer getDataPointsAnalyzed() { return dataPointsAnalyzed; }
     public void setDataPointsAnalyzed(Integer dataPointsAnalyzed) { this.dataPointsAnalyzed = dataPointsAnalyzed; }
     public String getTrendConfidence() { return trendConfidence; }
     public void setTrendConfidence(String trendConfidence) { this.trendConfidence = trendConfidence; }
     public List<String> getSeasonalPatterns() { return seasonalPatterns; }
     public void setSeasonalPatterns(List<String> seasonalPatterns) { this.seasonalPatterns = seasonalPatterns; }
     public Integer getAnomaliesDetected() { return anomaliesDetected; }
     public void setAnomaliesDetected(Integer anomaliesDetected) { this.anomaliesDetected = anomaliesDetected; }
 }

 // ═══════════════════════════════════════════════════════════════════════════════════
 // RECOMMENDATIONS
 // ═══════════════════════════════════════════════════════════════════════════════════

 @JsonProperty("recommendations")
 @Schema(description = "Performance optimization recommendations")
 private List<OptimizationRecommendation> recommendations;

 public static class OptimizationRecommendation {
     @JsonProperty("type")
     @Schema(description = "Recommendation type", example = "INDEX_OPTIMIZATION")
     private String type;

     @JsonProperty("priority")
     @Schema(description = "Priority level", example = "HIGH", allowableValues = {"LOW", "MEDIUM", "HIGH", "CRITICAL"})
     private String priority;

     @JsonProperty("description")
     @Schema(description = "Recommendation description", example = "Consider optimizing indexes to improve query performance")
     private String description;

     @JsonProperty("expectedImpact")
     @Schema(description = "Expected performance impact", example = "15-25% query improvement")
     private String expectedImpact;

     @JsonProperty("estimatedEffort")
     @Schema(description = "Estimated implementation effort", example = "LOW", allowableValues = {"LOW", "MEDIUM", "HIGH"})
     private String estimatedEffort;

     // Constructors
     public OptimizationRecommendation() {}

     public OptimizationRecommendation(String type, String priority, String description) {
         this.type = type;
         this.priority = priority;
         this.description = description;
     }

     // Getters and setters
     public String getType() { return type; }
     public void setType(String type) { this.type = type; }
     public String getPriority() { return priority; }
     public void setPriority(String priority) { this.priority = priority; }
     public String getDescription() { return description; }
     public void setDescription(String description) { this.description = description; }
     public String getExpectedImpact() { return expectedImpact; }
     public void setExpectedImpact(String expectedImpact) { this.expectedImpact = expectedImpact; }
     public String getEstimatedEffort() { return estimatedEffort; }
     public void setEstimatedEffort(String estimatedEffort) { this.estimatedEffort = estimatedEffort; }
 }

 // ═══════════════════════════════════════════════════════════════════════════════════
 // HISTORICAL COMPARISON
 // ═══════════════════════════════════════════════════════════════════════════════════

 @JsonProperty("comparison")
 @Schema(description = "Comparison with previous metrics")
 private Map<String, ChangeMetric> comparison;

 public static class ChangeMetric {
     @JsonProperty("previousValue")
     @Schema(description = "Previous metric value", example = "120.5")
     private Double previousValue;

     @JsonProperty("currentValue")
     @Schema(description = "Current metric value", example = "125.5")
     private Double currentValue;

     @JsonProperty("changeAmount")
     @Schema(description = "Absolute change amount", example = "5.0")
     private Double changeAmount;

     @JsonProperty("changePercentage")
     @Schema(description = "Percentage change", example = "4.15")
     private Double changePercentage;

     @JsonProperty("trend")
     @Schema(description = "Change trend", example = "INCREASING", allowableValues = {"INCREASING", "DECREASING", "STABLE"})
     private String trend;

     // Constructors
     public ChangeMetric() {}

     public ChangeMetric(Double previousValue, Double currentValue) {
         this.previousValue = previousValue;
         this.currentValue = currentValue;
         calculateChange();
     }

     private void calculateChange() {
         if (previousValue != null && currentValue != null) {
             this.changeAmount = currentValue - previousValue;
             if (previousValue != 0) {
                 this.changePercentage = (changeAmount / previousValue) * 100;
             }
             this.trend = changeAmount > 0 ? "INCREASING" : changeAmount < 0 ? "DECREASING" : "STABLE";
         }
     }

     // Getters and setters
     public Double getPreviousValue() { return previousValue; }
     public void setPreviousValue(Double previousValue) { this.previousValue = previousValue; }
     public Double getCurrentValue() { return currentValue; }
     public void setCurrentValue(Double currentValue) { this.currentValue = currentValue; }
     public Double getChangeAmount() { return changeAmount; }
     public void setChangeAmount(Double changeAmount) { this.changeAmount = changeAmount; }
     public Double getChangePercentage() { return changePercentage; }
     public void setChangePercentage(Double changePercentage) { this.changePercentage = changePercentage; }
     public String getTrend() { return trend; }
     public void setTrend(String trend) { this.trend = trend; }
 }

 // ═══════════════════════════════════════════════════════════════════════════════════
 // CONSTRUCTORS
 // ═══════════════════════════════════════════════════════════════════════════════════

 /**
  * Default constructor
  */
 public PartitionMetricsDTO() {
     this.collectionTime = LocalDateTime.now();
     this.recommendations = new ArrayList<>();
     this.comparison = new HashMap<>();
     this.sizeMetrics = new SizeMetrics();
     this.performanceMetrics = new PerformanceMetrics();
     this.recordMetrics = new RecordMetrics();
     this.efficiencyMetrics = new EfficiencyMetrics();
     this.trendAnalysis = new TrendAnalysis();
 }

 /**
  * Constructor with partition name
  */
 public PartitionMetricsDTO(String partitionName) {
     this();
     this.partitionName = partitionName;
 }

 // ═══════════════════════════════════════════════════════════════════════════════════
 // UTILITY METHODS
 // ═══════════════════════════════════════════════════════════════════════════════════

 /**
  * Add a recommendation
  */
 public void addRecommendation(String type, String priority, String description) {
     if (this.recommendations == null) {
         this.recommendations = new ArrayList<>();
     }
     this.recommendations.add(new OptimizationRecommendation(type, priority, description));
 }

 /**
  * Add comparison metric
  */
 public void addComparison(String metricName, Double previousValue, Double currentValue) {
     if (this.comparison == null) {
         this.comparison = new HashMap<>();
     }
     this.comparison.put(metricName, new ChangeMetric(previousValue, currentValue));
 }

 /**
  * Calculate overall health score based on metrics
  */
 public Integer calculateOverallHealthScore() {
     int score = 100;
     
     // Deduct points based on performance issues
     if (performanceMetrics != null && performanceMetrics.getSlowQueryPercentage() != null) {
         score -= (int) Math.round(performanceMetrics.getSlowQueryPercentage() * 10);
     }
     
     // Deduct points for efficiency issues
     if (efficiencyMetrics != null) {
         if (efficiencyMetrics.getFragmentationLevel() != null && efficiencyMetrics.getFragmentationLevel() > 10) {
             score -= (int) Math.round(efficiencyMetrics.getFragmentationLevel());
         }
         if (efficiencyMetrics.getIndexToDataRatio() != null && efficiencyMetrics.getIndexToDataRatio() > 30) {
             score -= 10;
         }
     }
     
     return Math.max(0, Math.min(100, score));
 }

 /**
  * Get summary of key metrics
  */
 public Map<String, Object> getSummary() {
     Map<String, Object> summary = new HashMap<>();
     summary.put("partitionName", partitionName);
     summary.put("collectionTime", collectionTime);
     summary.put("overallHealthScore", calculateOverallHealthScore());
     
     if (sizeMetrics != null) {
         summary.put("currentSizeMB", sizeMetrics.getCurrentSizeMB());
         summary.put("growthRatePerHour", sizeMetrics.getGrowthRatePerHour());
     }
     
     if (performanceMetrics != null) {
         summary.put("averageQueryTimeMs", performanceMetrics.getAverageQueryTimeMs());
         summary.put("slowQueryPercentage", performanceMetrics.getSlowQueryPercentage());
     }
     
     if (recordMetrics != null) {
         summary.put("currentRecordCount", recordMetrics.getCurrentRecordCount());
         summary.put("recordsPerHour", recordMetrics.getRecordsPerHour());
     }
     
     return summary;
 }

 // ═══════════════════════════════════════════════════════════════════════════════════
 // GETTERS AND SETTERS
 // ═══════════════════════════════════════════════════════════════════════════════════

 public String getPartitionName() { return partitionName; }
 public void setPartitionName(String partitionName) { this.partitionName = partitionName; }

 public LocalDateTime getCollectionTime() { return collectionTime; }
 public void setCollectionTime(LocalDateTime collectionTime) { this.collectionTime = collectionTime; }

 public String getMetricsVersion() { return metricsVersion; }
 public void setMetricsVersion(String metricsVersion) { this.metricsVersion = metricsVersion; }

 public Long getCollectionDurationMs() { return collectionDurationMs; }
 public void setCollectionDurationMs(Long collectionDurationMs) { this.collectionDurationMs = collectionDurationMs; }

 public SizeMetrics getSizeMetrics() { return sizeMetrics; }
 public void setSizeMetrics(SizeMetrics sizeMetrics) { this.sizeMetrics = sizeMetrics; }

 public PerformanceMetrics getPerformanceMetrics() { return performanceMetrics; }
 public void setPerformanceMetrics(PerformanceMetrics performanceMetrics) { this.performanceMetrics = performanceMetrics; }

 public RecordMetrics getRecordMetrics() { return recordMetrics; }
 public void setRecordMetrics(RecordMetrics recordMetrics) { this.recordMetrics = recordMetrics; }

 public EfficiencyMetrics getEfficiencyMetrics() { return efficiencyMetrics; }
 public void setEfficiencyMetrics(EfficiencyMetrics efficiencyMetrics) { this.efficiencyMetrics = efficiencyMetrics; }

 public TrendAnalysis getTrendAnalysis() { return trendAnalysis; }
 public void setTrendAnalysis(TrendAnalysis trendAnalysis) { this.trendAnalysis = trendAnalysis; }

 public List<OptimizationRecommendation> getRecommendations() { return recommendations; }
 public void setRecommendations(List<OptimizationRecommendation> recommendations) { this.recommendations = recommendations; }

 public Map<String, ChangeMetric> getComparison() { return comparison; }
 public void setComparison(Map<String, ChangeMetric> comparison) { this.comparison = comparison; }
}