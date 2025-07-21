package com.GpsTracker.Thinture.partition.dto;

//================================================================================================
//PartitionInfoDTO.java - STRUCTURED PARTITION INFORMATION RESPONSE
//================================================================================================


import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
* Data Transfer Object for partition information API responses
* Provides structured format for partition details instead of Map<String, Object>
*/
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Partition information details")
public class PartitionInfoDTO {

 // ═══════════════════════════════════════════════════════════════════════════════════
 // BASIC PARTITION INFORMATION
 // ═══════════════════════════════════════════════════════════════════════════════════

 @JsonProperty("partitionName")
 @Schema(description = "Partition name", example = "p_202506", required = true)
 private String partitionName;

 @JsonProperty("partitionDescription")
 @Schema(description = "Partition range description", example = "202507")
 private String partitionDescription;

 @JsonProperty("exists")
 @Schema(description = "Whether partition exists in database", example = "true")
 private Boolean exists;

 @JsonProperty("tableName")
 @Schema(description = "Table name this partition belongs to", example = "vehicle_history")
 private String tableName = "vehicle_history";

 // ═══════════════════════════════════════════════════════════════════════════════════
 // SIZE AND CAPACITY INFORMATION
 // ═══════════════════════════════════════════════════════════════════════════════════

 @JsonProperty("rows")
 @Schema(description = "Number of rows in partition", example = "18500000")
 private Long rows;

 @JsonProperty("sizeMB")
 @Schema(description = "Partition size in megabytes", example = "4250.75")
 private Double sizeMB;

 @JsonProperty("dataSizeMB")
 @Schema(description = "Data size in megabytes", example = "3800.50")
 private Double dataSizeMB;

 @JsonProperty("indexSizeMB")
 @Schema(description = "Index size in megabytes", example = "450.25")
 private Double indexSizeMB;

 @JsonProperty("compressionRatio")
 @Schema(description = "Compression ratio percentage", example = "65.5")
 private Double compressionRatio;

 @JsonProperty("isCompressed")
 @Schema(description = "Whether partition is compressed", example = "true")
 private Boolean isCompressed;

 // ═══════════════════════════════════════════════════════════════════════════════════
 // TEMPORAL INFORMATION
 // ═══════════════════════════════════════════════════════════════════════════════════

 @JsonProperty("year")
 @Schema(description = "Partition year", example = "2025")
 private Integer year;

 @JsonProperty("month")
 @Schema(description = "Partition month", example = "6")
 private Integer month;

 @JsonProperty("ageInDays")
 @Schema(description = "Age of partition in days", example = "45")
 private Long ageInDays;

 @JsonProperty("dataTier")
 @Schema(description = "Data tier classification", example = "ACTIVE", 
         allowableValues = {"ACTIVE", "WARM", "COLD", "ARCHIVE"})
 private String dataTier;

 // ═══════════════════════════════════════════════════════════════════════════════════
 // METADATA AND TIMESTAMPS
 // ═══════════════════════════════════════════════════════════════════════════════════

 @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
 @JsonProperty("createTime")
 @Schema(description = "Partition creation timestamp", example = "2025-06-01 00:00:00")
 private Timestamp createTime;

 @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
 @JsonProperty("updateTime")
 @Schema(description = "Last update timestamp", example = "2025-06-15 14:30:00")
 private Timestamp updateTime;

 @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
 @JsonProperty("checkTime")
 @Schema(description = "Last check timestamp", example = "2025-06-15 14:30:00")
 private Timestamp checkTime;

 @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
 @JsonProperty("lastQueried")
 @Schema(description = "Last query timestamp", example = "2025-06-15 14:30:00")
 private LocalDateTime lastQueried;

 // ═══════════════════════════════════════════════════════════════════════════════════
 // PERFORMANCE INFORMATION
 // ═══════════════════════════════════════════════════════════════════════════════════

 @JsonProperty("averageQueryTimeMs")
 @Schema(description = "Average query time in milliseconds", example = "125.5")
 private Double averageQueryTimeMs;

 @JsonProperty("queryCount")
 @Schema(description = "Total number of queries executed", example = "1250")
 private Long queryCount;

 @JsonProperty("slowQueryCount")
 @Schema(description = "Number of slow queries", example = "5")
 private Long slowQueryCount;

 @JsonProperty("growthRatePerHour")
 @Schema(description = "Growth rate in rows per hour", example = "2500.0")
 private Double growthRatePerHour;

 // ═══════════════════════════════════════════════════════════════════════════════════
 // STATUS AND HEALTH
 // ═══════════════════════════════════════════════════════════════════════════════════

 @JsonProperty("status")
 @Schema(description = "Partition status", example = "HEALTHY", 
         allowableValues = {"HEALTHY", "WARNING", "CRITICAL", "UNKNOWN"})
 private String status;

 @JsonProperty("healthScore")
 @Schema(description = "Health score from 0-100", example = "95")
 private Integer healthScore;

 @JsonProperty("issues")
 @Schema(description = "List of identified issues")
 private List<String> issues = new ArrayList<>();

 @JsonProperty("recommendations")
 @Schema(description = "List of optimization recommendations")
 private List<String> recommendations = new ArrayList<>();

 // ═══════════════════════════════════════════════════════════════════════════════════
 // OPERATIONAL FLAGS
 // ═══════════════════════════════════════════════════════════════════════════════════

 @JsonProperty("isCurrent")
 @Schema(description = "Whether this is the current active partition", example = "true")
 private Boolean isCurrent;

 @JsonProperty("isOptimized")
 @Schema(description = "Whether partition has been optimized", example = "true")
 private Boolean isOptimized;

 @JsonProperty("canBeDropped")
 @Schema(description = "Whether partition can be safely dropped", example = "false")
 private Boolean canBeDropped;

 @JsonProperty("canBeArchived")
 @Schema(description = "Whether partition can be archived", example = "false")
 private Boolean canBeArchived;

 @JsonProperty("canBeCompressed")
 @Schema(description = "Whether partition can be compressed", example = "true")
 private Boolean canBeCompressed;

 // ═══════════════════════════════════════════════════════════════════════════════════
 // CONSTRUCTORS
 // ═══════════════════════════════════════════════════════════════════════════════════

 /**
  * Default constructor
  */
 public PartitionInfoDTO() {
     this.issues = new ArrayList<>();
     this.recommendations = new ArrayList<>();
 }

 /**
  * Constructor with basic information
  */
 public PartitionInfoDTO(String partitionName, Boolean exists) {
     this();
     this.partitionName = partitionName;
     this.exists = exists;
     parsePartitionName();
 }

 /**
  * Constructor with detailed information
  */
 public PartitionInfoDTO(String partitionName, Boolean exists, Long rows, Double sizeMB) {
     this(partitionName, exists);
     this.rows = rows;
     this.sizeMB = sizeMB;
 }

 // ═══════════════════════════════════════════════════════════════════════════════════
 // UTILITY METHODS
 // ═══════════════════════════════════════════════════════════════════════════════════

 /**
  * Parse year and month from partition name
  */
 private void parsePartitionName() {
     if (partitionName != null && partitionName.matches("p_\\d{6}")) {
         String yearMonth = partitionName.substring(2);
         this.year = Integer.parseInt(yearMonth.substring(0, 4));
         this.month = Integer.parseInt(yearMonth.substring(4, 6));
     }
 }

 /**
  * Add an issue to the issues list
  */
 public void addIssue(String issue) {
     if (this.issues == null) {
         this.issues = new ArrayList<>();
     }
     this.issues.add(issue);
 }

 /**
  * Add a recommendation to the recommendations list
  */
 public void addRecommendation(String recommendation) {
     if (this.recommendations == null) {
         this.recommendations = new ArrayList<>();
     }
     this.recommendations.add(recommendation);
 }

 /**
  * Check if partition has any issues
  */
 public boolean hasIssues() {
     return issues != null && !issues.isEmpty();
 }

 /**
  * Check if partition has recommendations
  */
 public boolean hasRecommendations() {
     return recommendations != null && !recommendations.isEmpty();
 }

 /**
  * Calculate index ratio percentage
  */
 public Double getIndexRatio() {
     if (dataSizeMB != null && indexSizeMB != null && dataSizeMB > 0) {
         return (indexSizeMB / dataSizeMB) * 100;
     }
     return null;
 }

 /**
  * Get formatted size string
  */
 public String getFormattedSize() {
     if (sizeMB == null) return "Unknown";
     
     if (sizeMB > 1024) {
         return String.format("%.2f GB", sizeMB / 1024);
     } else {
         return String.format("%.2f MB", sizeMB);
     }
 }

 /**
  * Get formatted row count
  */
 public String getFormattedRowCount() {
     if (rows == null) return "Unknown";
     
     if (rows > 1_000_000) {
         return String.format("%.1fM", rows / 1_000_000.0);
     } else if (rows > 1_000) {
         return String.format("%.1fK", rows / 1_000.0);
     } else {
         return String.valueOf(rows);
     }
 }

 // ═══════════════════════════════════════════════════════════════════════════════════
 // BUILDER PATTERN
 // ═══════════════════════════════════════════════════════════════════════════════════

 /**
  * Create a new builder instance
  */
 public static Builder builder() {
     return new Builder();
 }

 /**
  * Builder class for fluent partition info creation
  */
 public static class Builder {
     private PartitionInfoDTO dto = new PartitionInfoDTO();

     public Builder partitionName(String partitionName) {
         dto.partitionName = partitionName;
         dto.parsePartitionName();
         return this;
     }

     public Builder exists(Boolean exists) {
         dto.exists = exists;
         return this;
     }

     public Builder rows(Long rows) {
         dto.rows = rows;
         return this;
     }

     public Builder sizeMB(Double sizeMB) {
         dto.sizeMB = sizeMB;
         return this;
     }

     public Builder dataSizeMB(Double dataSizeMB) {
         dto.dataSizeMB = dataSizeMB;
         return this;
     }

     public Builder indexSizeMB(Double indexSizeMB) {
         dto.indexSizeMB = indexSizeMB;
         return this;
     }

     public Builder isCompressed(Boolean isCompressed) {
         dto.isCompressed = isCompressed;
         return this;
     }

     public Builder compressionRatio(Double compressionRatio) {
         dto.compressionRatio = compressionRatio;
         return this;
     }

     public Builder status(String status) {
         dto.status = status;
         return this;
     }

     public Builder healthScore(Integer healthScore) {
         dto.healthScore = healthScore;
         return this;
     }

     public Builder dataTier(String dataTier) {
         dto.dataTier = dataTier;
         return this;
     }

     public Builder isCurrent(Boolean isCurrent) {
         dto.isCurrent = isCurrent;
         return this;
     }

     public Builder averageQueryTimeMs(Double averageQueryTimeMs) {
         dto.averageQueryTimeMs = averageQueryTimeMs;
         return this;
     }

     public Builder growthRatePerHour(Double growthRatePerHour) {
         dto.growthRatePerHour = growthRatePerHour;
         return this;
     }

     public Builder createTime(Timestamp createTime) {
         dto.createTime = createTime;
         return this;
     }

     public Builder updateTime(Timestamp updateTime) {
         dto.updateTime = updateTime;
         return this;
     }

     public PartitionInfoDTO build() {
         return dto;
     }
 }

 // ═══════════════════════════════════════════════════════════════════════════════════
 // GETTERS AND SETTERS
 // ═══════════════════════════════════════════════════════════════════════════════════

 public String getPartitionName() { return partitionName; }
 public void setPartitionName(String partitionName) { 
     this.partitionName = partitionName; 
     parsePartitionName();
 }

 public String getPartitionDescription() { return partitionDescription; }
 public void setPartitionDescription(String partitionDescription) { this.partitionDescription = partitionDescription; }

 public Boolean getExists() { return exists; }
 public void setExists(Boolean exists) { this.exists = exists; }

 public String getTableName() { return tableName; }
 public void setTableName(String tableName) { this.tableName = tableName; }

 public Long getRows() { return rows; }
 public void setRows(Long rows) { this.rows = rows; }

 public Double getSizeMB() { return sizeMB; }
 public void setSizeMB(Double sizeMB) { this.sizeMB = sizeMB; }

 public Double getDataSizeMB() { return dataSizeMB; }
 public void setDataSizeMB(Double dataSizeMB) { this.dataSizeMB = dataSizeMB; }

 public Double getIndexSizeMB() { return indexSizeMB; }
 public void setIndexSizeMB(Double indexSizeMB) { this.indexSizeMB = indexSizeMB; }

 public Double getCompressionRatio() { return compressionRatio; }
 public void setCompressionRatio(Double compressionRatio) { this.compressionRatio = compressionRatio; }

 public Boolean getIsCompressed() { return isCompressed; }
 public void setIsCompressed(Boolean isCompressed) { this.isCompressed = isCompressed; }

 public Integer getYear() { return year; }
 public void setYear(Integer year) { this.year = year; }

 public Integer getMonth() { return month; }
 public void setMonth(Integer month) { this.month = month; }

 public Long getAgeInDays() { return ageInDays; }
 public void setAgeInDays(Long ageInDays) { this.ageInDays = ageInDays; }

 public String getDataTier() { return dataTier; }
 public void setDataTier(String dataTier) { this.dataTier = dataTier; }

 public Timestamp getCreateTime() { return createTime; }
 public void setCreateTime(Timestamp createTime) { this.createTime = createTime; }

 public Timestamp getUpdateTime() { return updateTime; }
 public void setUpdateTime(Timestamp updateTime) { this.updateTime = updateTime; }

 public Timestamp getCheckTime() { return checkTime; }
 public void setCheckTime(Timestamp checkTime) { this.checkTime = checkTime; }

 public LocalDateTime getLastQueried() { return lastQueried; }
 public void setLastQueried(LocalDateTime lastQueried) { this.lastQueried = lastQueried; }

 public Double getAverageQueryTimeMs() { return averageQueryTimeMs; }
 public void setAverageQueryTimeMs(Double averageQueryTimeMs) { this.averageQueryTimeMs = averageQueryTimeMs; }

 public Long getQueryCount() { return queryCount; }
 public void setQueryCount(Long queryCount) { this.queryCount = queryCount; }

 public Long getSlowQueryCount() { return slowQueryCount; }
 public void setSlowQueryCount(Long slowQueryCount) { this.slowQueryCount = slowQueryCount; }

 public Double getGrowthRatePerHour() { return growthRatePerHour; }
 public void setGrowthRatePerHour(Double growthRatePerHour) { this.growthRatePerHour = growthRatePerHour; }

 public String getStatus() { return status; }
 public void setStatus(String status) { this.status = status; }

 public Integer getHealthScore() { return healthScore; }
 public void setHealthScore(Integer healthScore) { this.healthScore = healthScore; }

 public List<String> getIssues() { return issues; }
 public void setIssues(List<String> issues) { this.issues = issues; }

 public List<String> getRecommendations() { return recommendations; }
 public void setRecommendations(List<String> recommendations) { this.recommendations = recommendations; }

 public Boolean getIsCurrent() { return isCurrent; }
 public void setIsCurrent(Boolean isCurrent) { this.isCurrent = isCurrent; }

 public Boolean getIsOptimized() { return isOptimized; }
 public void setIsOptimized(Boolean isOptimized) { this.isOptimized = isOptimized; }

 public Boolean getCanBeDropped() { return canBeDropped; }
 public void setCanBeDropped(Boolean canBeDropped) { this.canBeDropped = canBeDropped; }

 public Boolean getCanBeArchived() { return canBeArchived; }
 public void setCanBeArchived(Boolean canBeArchived) { this.canBeArchived = canBeArchived; }

 public Boolean getCanBeCompressed() { return canBeCompressed; }
 public void setCanBeCompressed(Boolean canBeCompressed) { this.canBeCompressed = canBeCompressed; }
}