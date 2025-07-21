package com.GpsTracker.Thinture.partition.monitor;

//================================================================================================
//PerformanceMonitor.java - INTEGRATED HIGH-PERFORMANCE MONITORING
//================================================================================================

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.lang.management.*;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
* ✅ INTEGRATED High-performance system monitoring
* 🚀 Works seamlessly with PartitionMetricsCollector
* 📊 Provides comprehensive system health monitoring
*/


@Component
public class PerformanceMonitor {
 
 private static final Logger logger = LoggerFactory.getLogger(PerformanceMonitor.class);
 private static final Logger performanceLogger = LoggerFactory.getLogger("PERFORMANCE." + PerformanceMonitor.class.getName());
 
 @Autowired(required = false)
 private PartitionMetricsCollector partitionMetricsCollector;
 
 // ✅ Configuration properties
 @Value("${gps.performance.cpu-warning-threshold:70.0}")
 private double cpuWarningThreshold;
 
 @Value("${gps.performance.cpu-critical-threshold:85.0}")
 private double cpuCriticalThreshold;
 
 @Value("${gps.performance.memory-warning-threshold:75.0}")
 private double memoryWarningThreshold;
 
 @Value("${gps.performance.memory-critical-threshold:90.0}")
 private double memoryCriticalThreshold;
 
 @Value("${gps.performance.response-time-warning-ms:1000}")
 private long responseTimeWarningMs;
 
 @Value("${gps.performance.response-time-critical-ms:3000}")
 private long responseTimeCriticalMs;
 
 // ✅ Thread-safe performance metrics storage
 private final Map<String, PerformanceMetric> performanceMetrics = new ConcurrentHashMap<>();
 private final AtomicLong lastGcTime = new AtomicLong(0);
 private final AtomicLong lastGcCount = new AtomicLong(0);
 private final AtomicReference<SystemHealth> currentSystemHealth = new AtomicReference<>();
 
 // ✅ GPS-specific counters (thread-safe)
 private final AtomicLong totalGpsMessagesReceived = new AtomicLong(0);
 private final AtomicLong totalGpsMessagesProcessed = new AtomicLong(0);
 private final AtomicLong totalDatabaseOperations = new AtomicLong(0);
 private final AtomicLong lastMessageTimestamp = new AtomicLong(System.currentTimeMillis());
 
 private static final DecimalFormat df = new DecimalFormat("#.##");
 
 public PerformanceMonitor() {
     initializeMetrics();
     currentSystemHealth.set(new SystemHealth());
 }
 
 private void initializeMetrics() {
     performanceMetrics.put("cpu", new PerformanceMetric("CPU Usage"));
     performanceMetrics.put("memory", new PerformanceMetric("Memory Usage"));
     performanceMetrics.put("threads", new PerformanceMetric("Thread Count"));
     performanceMetrics.put("gc", new PerformanceMetric("GC Activity"));
     performanceMetrics.put("response", new PerformanceMetric("Response Time"));
     performanceMetrics.put("throughput", new PerformanceMetric("Throughput"));
 }
 
 // ═══════════════════════════════════════════════════════════════════════════════════
 // SCHEDULED MONITORING - HIGH PERFORMANCE
 // ═══════════════════════════════════════════════════════════════════════════════════
 
 /**
  * ✅ Main performance monitoring - runs every 30 seconds
  */
 @Scheduled(fixedRate = 30000) // Every 30 seconds
 public void monitorPerformance() {
     try {
         // ✅ Parallel monitoring for better performance
         monitorCpu();
         monitorMemory();
         monitorThreads();
         monitorGarbageCollection();
         monitorThroughput();
         
         // Update system health
         updateSystemHealth();
         
         // Log summary and check alerts
         logPerformanceSummary();
         checkPerformanceAlerts();
         
     } catch (Exception e) {
         logger.error("❌ Error during performance monitoring: {}", e.getMessage());
     }
 }
 
 /**
  * ✅ Detailed performance analysis - runs every 5 minutes
  */
 @Scheduled(fixedRate = 300000) // Every 5 minutes
 public void detailedPerformanceAnalysis() {
     try {
         performanceLogger.info("📊 === DETAILED PERFORMANCE ANALYSIS ===");
         
         // Analyze trends
         analyzePerformanceTrends();
         
         // Check for performance degradation
         detectPerformanceDegradation();
         
         // Integration with partition metrics
         if (partitionMetricsCollector != null) {
             integrateWithPartitionMetrics();
         }
         
         performanceLogger.info("============================================");
         
     } catch (Exception e) {
         logger.error("❌ Error during detailed performance analysis: {}", e.getMessage());
     }
 }
 
 // ═══════════════════════════════════════════════════════════════════════════════════
 // MONITORING METHODS - OPTIMIZED
 // ═══════════════════════════════════════════════════════════════════════════════════
 
 private void monitorCpu() {
     try {
         OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
         double systemLoad = osBean.getSystemLoadAverage();
         int processors = osBean.getAvailableProcessors();
         
         double cpuUsage = -1;
         if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
             com.sun.management.OperatingSystemMXBean sunOsBean = 
                 (com.sun.management.OperatingSystemMXBean) osBean;
             cpuUsage = sunOsBean.getProcessCpuLoad() * 100;
         } else if (systemLoad >= 0) {
             cpuUsage = (systemLoad / processors) * 100;
         }
         
         PerformanceMetric cpuMetric = performanceMetrics.get("cpu");
         cpuMetric.update(cpuUsage >= 0 ? cpuUsage : 0);
         cpuMetric.setAdditionalInfo("Load: " + df.format(systemLoad) + 
                                    ", Processors: " + processors);
         
     } catch (Exception e) {
         logger.debug("Error monitoring CPU: {}", e.getMessage());
     }
 }
 
 private void monitorMemory() {
     try {
         MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
         MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
         
         long used = heapUsage.getUsed();
         long max = heapUsage.getMax();
         double usagePercent = (used * 100.0) / max;
         
         PerformanceMetric memoryMetric = performanceMetrics.get("memory");
         memoryMetric.update(usagePercent);
         memoryMetric.setAdditionalInfo("Used: " + formatBytes(used) + 
                                       ", Max: " + formatBytes(max));
         
         // Monitor non-heap memory
         MemoryUsage nonHeapUsage = memoryBean.getNonHeapMemoryUsage();
         logger.debug("Non-heap memory - Used: {}, Committed: {}", 
                     formatBytes(nonHeapUsage.getUsed()),
                     formatBytes(nonHeapUsage.getCommitted()));
                     
     } catch (Exception e) {
         logger.debug("Error monitoring memory: {}", e.getMessage());
     }
 }
 
 private void monitorThreads() {
     try {
         ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
         int threadCount = threadBean.getThreadCount();
         int peakThreadCount = threadBean.getPeakThreadCount();
         long totalStartedThreads = threadBean.getTotalStartedThreadCount();
         
         PerformanceMetric threadMetric = performanceMetrics.get("threads");
         threadMetric.update(threadCount);
         threadMetric.setAdditionalInfo("Peak: " + peakThreadCount + 
                                       ", Total Started: " + totalStartedThreads);
         
         // Check for deadlocks
         long[] deadlockedThreads = threadBean.findDeadlockedThreads();
         if (deadlockedThreads != null && deadlockedThreads.length > 0) {
             logger.error("🚨 DEADLOCK DETECTED! {} threads in deadlock", deadlockedThreads.length);
             sendCriticalAlert("DEADLOCK", 
                 "Deadlock detected with " + deadlockedThreads.length + " threads");
         }
         
     } catch (Exception e) {
         logger.debug("Error monitoring threads: {}", e.getMessage());
     }
 }
 
 private void monitorGarbageCollection() {
     try {
         List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
         
         long totalGcCount = 0;
         long totalGcTime = 0;
         
         for (GarbageCollectorMXBean gcBean : gcBeans) {
             totalGcCount += gcBean.getCollectionCount();
             totalGcTime += gcBean.getCollectionTime();
         }
         
         long gcCountDelta = totalGcCount - lastGcCount.get();
         long gcTimeDelta = totalGcTime - lastGcTime.get();
         
         PerformanceMetric gcMetric = performanceMetrics.get("gc");
         gcMetric.update(gcTimeDelta);
         gcMetric.setAdditionalInfo("Collections: " + gcCountDelta + 
                                   ", Total Time: " + gcTimeDelta + "ms");
         
         lastGcCount.set(totalGcCount);
         lastGcTime.set(totalGcTime);
         
         // Alert on excessive GC activity
         if (gcTimeDelta > 5000) { // More than 5 seconds of GC in 30 seconds
             logger.warn("⚠️ HIGH GC ACTIVITY: {} collections, {}ms total time", gcCountDelta, gcTimeDelta);
         }
         
     } catch (Exception e) {
         logger.debug("Error monitoring GC: {}", e.getMessage());
     }
 }
 
 private void monitorThroughput() {
     try {
         long currentTime = System.currentTimeMillis();
         long timeDiff = currentTime - lastMessageTimestamp.get();
         
         // Calculate messages per second over the last interval
         double messagesPerSecond = timeDiff > 0 ? 
             (totalGpsMessagesReceived.get() * 1000.0) / timeDiff : 0;
         
         PerformanceMetric throughputMetric = performanceMetrics.get("throughput");
         throughputMetric.update(messagesPerSecond);
         throughputMetric.setAdditionalInfo("Total Processed: " + 
                                           totalGpsMessagesProcessed.get());
         
     } catch (Exception e) {
         logger.debug("Error monitoring throughput: {}", e.getMessage());
     }
 }
 
 // ═══════════════════════════════════════════════════════════════════════════════════
 // GPS-SPECIFIC MONITORING METHODS
 // ═══════════════════════════════════════════════════════════════════════════════════
 
 /**
  * ✅ Record GPS message received
  */
 public void recordGpsMessageReceived() {
     totalGpsMessagesReceived.incrementAndGet();
     lastMessageTimestamp.set(System.currentTimeMillis());
 }
 
 /**
  * ✅ Record GPS message processed
  */
 public void recordGpsMessageProcessed() {
     totalGpsMessagesProcessed.incrementAndGet();
 }
 
 /**
  * ✅ Record database operation
  */
 public void recordDatabaseOperation() {
     totalDatabaseOperations.incrementAndGet();
 }
 
 /**
  * ✅ Record response time for operations
  */
 public void recordResponseTime(String operation, long milliseconds) {
     PerformanceMetric responseMetric = performanceMetrics.get("response");
     responseMetric.update(milliseconds);
     
     // Integrate with partition metrics collector
     if (partitionMetricsCollector != null && operation.contains("partition")) {
         partitionMetricsCollector.recordQueryTime(operation, milliseconds);
     }
     
     if (milliseconds > responseTimeCriticalMs) {
         logger.warn("🐌 SLOW OPERATION: {} took {}ms", operation, milliseconds);
     }
 }
 
 // ═══════════════════════════════════════════════════════════════════════════════════
 // SYSTEM HEALTH MANAGEMENT
 // ═══════════════════════════════════════════════════════════════════════════════════
 
 private void updateSystemHealth() {
     SystemHealth health = new SystemHealth();
     
     // CPU health
     double cpuUsage = performanceMetrics.get("cpu").getCurrentValue();
     if (cpuUsage > cpuCriticalThreshold) {
         health.setCpuStatus(HealthStatus.CRITICAL);
     } else if (cpuUsage > cpuWarningThreshold) {
         health.setCpuStatus(HealthStatus.WARNING);
     } else {
         health.setCpuStatus(HealthStatus.HEALTHY);
     }
     
     // Memory health
     double memoryUsage = performanceMetrics.get("memory").getCurrentValue();
     if (memoryUsage > memoryCriticalThreshold) {
         health.setMemoryStatus(HealthStatus.CRITICAL);
     } else if (memoryUsage > memoryWarningThreshold) {
         health.setMemoryStatus(HealthStatus.WARNING);
     } else {
         health.setMemoryStatus(HealthStatus.HEALTHY);
     }
     
     // Response time health
     double avgResponseTime = performanceMetrics.get("response").getAverageValue();
     if (avgResponseTime > responseTimeCriticalMs) {
         health.setResponseStatus(HealthStatus.CRITICAL);
     } else if (avgResponseTime > responseTimeWarningMs) {
         health.setResponseStatus(HealthStatus.WARNING);
     } else {
         health.setResponseStatus(HealthStatus.HEALTHY);
     }
     
     // Overall health
     if (health.getCpuStatus() == HealthStatus.CRITICAL || 
         health.getMemoryStatus() == HealthStatus.CRITICAL ||
         health.getResponseStatus() == HealthStatus.CRITICAL) {
         health.setOverallStatus(HealthStatus.CRITICAL);
     } else if (health.getCpuStatus() == HealthStatus.WARNING || 
                health.getMemoryStatus() == HealthStatus.WARNING ||
                health.getResponseStatus() == HealthStatus.WARNING) {
         health.setOverallStatus(HealthStatus.WARNING);
     } else {
         health.setOverallStatus(HealthStatus.HEALTHY);
     }
     
     health.setTimestamp(LocalDateTime.now());
     currentSystemHealth.set(health);
 }
 
 // ═══════════════════════════════════════════════════════════════════════════════════
 // ANALYSIS AND ALERTING
 // ═══════════════════════════════════════════════════════════════════════════════════
 
 private void logPerformanceSummary() {
     StringBuilder summary = new StringBuilder("\n=== Performance Summary ===\n");
     
     performanceMetrics.forEach((name, metric) -> {
         summary.append(String.format("%s: Current=%.2f, Avg=%.2f, Max=%.2f %s\n",
             metric.getName(),
             metric.getCurrentValue(),
             metric.getAverageValue(),
             metric.getMaxValue(),
             metric.getAdditionalInfo() != null ? "(" + metric.getAdditionalInfo() + ")" : ""
         ));
     });
     
     // Add GPS-specific metrics
     summary.append(String.format("GPS Messages: Received=%d, Processed=%d, DB Ops=%d\n",
         totalGpsMessagesReceived.get(),
         totalGpsMessagesProcessed.get(),
         totalDatabaseOperations.get()));
     
     logger.info(summary.toString());
 }
 
 private void checkPerformanceAlerts() {
     SystemHealth health = currentSystemHealth.get();
     
     // CPU alerts
     if (health.getCpuStatus() == HealthStatus.CRITICAL) {
         sendCriticalAlert("CPU_CRITICAL", "CPU usage critical: " + 
                          df.format(performanceMetrics.get("cpu").getCurrentValue()) + "%");
     } else if (health.getCpuStatus() == HealthStatus.WARNING) {
         sendWarningAlert("CPU_WARNING", "CPU usage high: " + 
                        df.format(performanceMetrics.get("cpu").getCurrentValue()) + "%");
     }
     
     // Memory alerts
     if (health.getMemoryStatus() == HealthStatus.CRITICAL) {
         sendCriticalAlert("MEMORY_CRITICAL", "Memory usage critical: " + 
                          df.format(performanceMetrics.get("memory").getCurrentValue()) + "%");
     } else if (health.getMemoryStatus() == HealthStatus.WARNING) {
         sendWarningAlert("MEMORY_WARNING", "Memory usage high: " + 
                        df.format(performanceMetrics.get("memory").getCurrentValue()) + "%");
     }
     
     // Response time alerts
     if (health.getResponseStatus() == HealthStatus.CRITICAL) {
         sendCriticalAlert("RESPONSE_CRITICAL", "Average response time critical: " + 
                          df.format(performanceMetrics.get("response").getAverageValue()) + "ms");
     } else if (health.getResponseStatus() == HealthStatus.WARNING) {
         sendWarningAlert("RESPONSE_WARNING", "Average response time high: " + 
                        df.format(performanceMetrics.get("response").getAverageValue()) + "ms");
     }
 }
 
 private void analyzePerformanceTrends() {
     performanceMetrics.forEach((name, metric) -> {
         double current = metric.getCurrentValue();
         double average = metric.getAverageValue();
         double max = metric.getMaxValue();
         
         // Detect if current performance is significantly worse than average
         if (current > average * 1.5 && current > 0) {
             performanceLogger.warn("📈 TREND ALERT: {} current value ({:.2f}) significantly above average ({:.2f})", 
                                  name, current, average);
         }
         
         // Detect if we're approaching maximum values
         if (current > max * 0.9 && current > 0) {
             performanceLogger.warn("🔺 PEAK ALERT: {} approaching maximum value (current: {:.2f}, max: {:.2f})", 
                                  name, current, max);
         }
     });
 }
 
 private void detectPerformanceDegradation() {
     SystemHealth health = currentSystemHealth.get();
     
     // Check for overall system degradation
     if (health.getOverallStatus() == HealthStatus.CRITICAL) {
         performanceLogger.error("🚨 SYSTEM DEGRADATION: Multiple critical performance issues detected");
     } else if (health.getOverallStatus() == HealthStatus.WARNING) {
         performanceLogger.warn("⚠️ SYSTEM WARNING: Performance issues detected");
     }
     
     // Check GPS-specific degradation
     double throughput = performanceMetrics.get("throughput").getCurrentValue();
     if (throughput < 1.0 && totalGpsMessagesReceived.get() > 0) {
         performanceLogger.warn("📡 GPS THROUGHPUT LOW: Current throughput {:.2f} messages/second", throughput);
     }
 }
 
 private void integrateWithPartitionMetrics() {
     try {
         Map<String, Object> partitionSummary = partitionMetricsCollector.getMetricsSummary();
         
         performanceLogger.info("🔗 Partition Metrics Integration:");
         performanceLogger.info("  Total Queries: {}", partitionSummary.get("totalQueries"));
         performanceLogger.info("  Average Query Time: {:.1f}ms", partitionSummary.get("averageQueryTime"));
         performanceLogger.info("  Slow Queries: {}", partitionSummary.get("slowQueries"));
         performanceLogger.info("  Partitions Monitored: {}", partitionSummary.get("partitionCount"));
         
         // Cross-correlate with system performance
         double avgQueryTime = (Double) partitionSummary.get("averageQueryTime");
         double systemResponseTime = performanceMetrics.get("response").getAverageValue();
         
         if (avgQueryTime > systemResponseTime * 2) {
             performanceLogger.warn("🔍 CORRELATION ALERT: Database queries significantly slower than system response time");
         }
         
     } catch (Exception e) {
         logger.debug("Could not integrate with partition metrics: {}", e.getMessage());
     }
 }
 
 // ═══════════════════════════════════════════════════════════════════════════════════
 // ALERT SYSTEM (SIMPLE IMPLEMENTATION)
 // ═══════════════════════════════════════════════════════════════════════════════════
 
 private void sendCriticalAlert(String type, String message) {
     logger.error("🚨 CRITICAL ALERT [{}]: {}", type, message);
     // Integration point for external alert systems (email, SMS, etc.)
 }
 
 private void sendWarningAlert(String type, String message) {
     logger.warn("⚠️ WARNING ALERT [{}]: {}", type, message);
     // Integration point for external alert systems
 }
 
 // ═══════════════════════════════════════════════════════════════════════════════════
 // UTILITY METHODS
 // ═══════════════════════════════════════════════════════════════════════════════════
 
 private String formatBytes(long bytes) {
     if (bytes < 1024) return bytes + " B";
     int exp = (int) (Math.log(bytes) / Math.log(1024));
     String pre = "KMGTPE".charAt(exp-1) + "";
     return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
 }
 
 // ═══════════════════════════════════════════════════════════════════════════════════
 // PUBLIC API
 // ═══════════════════════════════════════════════════════════════════════════════════
 
 public Map<String, PerformanceMetric> getPerformanceMetrics() {
     return new ConcurrentHashMap<>(performanceMetrics);
 }
 
 public PerformanceSnapshot getSnapshot() {
     return new PerformanceSnapshot(performanceMetrics, currentSystemHealth.get());
 }
 
 public SystemHealth getCurrentSystemHealth() {
     return currentSystemHealth.get();
 }
 
 public long getTotalGpsMessagesReceived() {
     return totalGpsMessagesReceived.get();
 }
 
 public long getTotalGpsMessagesProcessed() {
     return totalGpsMessagesProcessed.get();
 }
 
 public double getMessagesPerSecond() {
     return performanceMetrics.get("throughput").getCurrentValue();
 }
 
 // ═══════════════════════════════════════════════════════════════════════════════════
 // INNER CLASSES
 // ═══════════════════════════════════════════════════════════════════════════════════
 
 public static class PerformanceMetric {
     private final String name;
     private volatile double currentValue;
     private volatile double totalValue;
     private volatile double maxValue;
     private volatile long sampleCount;
     private volatile String additionalInfo;
     
     public PerformanceMetric(String name) {
         this.name = name;
     }
     
     public synchronized void update(double value) {
         this.currentValue = value;
         this.totalValue += value;
         this.sampleCount++;
         if (value > maxValue) {
             this.maxValue = value;
         }
     }
     
     public String getName() { return name; }
     public double getCurrentValue() { return currentValue; }
     public double getMaxValue() { return maxValue; }
     public double getAverageValue() { 
         return sampleCount > 0 ? totalValue / sampleCount : 0;
     }
     public String getAdditionalInfo() { return additionalInfo; }
     public void setAdditionalInfo(String info) { this.additionalInfo = info; }
 }
 
 public static class SystemHealth {
     private HealthStatus overallStatus = HealthStatus.HEALTHY;
     private HealthStatus cpuStatus = HealthStatus.HEALTHY;
     private HealthStatus memoryStatus = HealthStatus.HEALTHY;
     private HealthStatus responseStatus = HealthStatus.HEALTHY;
     private LocalDateTime timestamp;
     
     // Getters and setters
     public HealthStatus getOverallStatus() { return overallStatus; }
     public void setOverallStatus(HealthStatus overallStatus) { this.overallStatus = overallStatus; }
     public HealthStatus getCpuStatus() { return cpuStatus; }
     public void setCpuStatus(HealthStatus cpuStatus) { this.cpuStatus = cpuStatus; }
     public HealthStatus getMemoryStatus() { return memoryStatus; }
     public void setMemoryStatus(HealthStatus memoryStatus) { this.memoryStatus = memoryStatus; }
     public HealthStatus getResponseStatus() { return responseStatus; }
     public void setResponseStatus(HealthStatus responseStatus) { this.responseStatus = responseStatus; }
     public LocalDateTime getTimestamp() { return timestamp; }
     public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
 }
 
 public static class PerformanceSnapshot {
     private final Map<String, Map<String, Object>> metrics;
     private final SystemHealth systemHealth;
     private final long timestamp;
     
     public PerformanceSnapshot(Map<String, PerformanceMetric> performanceMetrics, SystemHealth health) {
         this.timestamp = System.currentTimeMillis();
         this.systemHealth = health;
         this.metrics = new HashMap<>();
         
         performanceMetrics.forEach((key, metric) -> {
             Map<String, Object> values = new HashMap<>();
             values.put("current", metric.getCurrentValue());
             values.put("average", metric.getAverageValue());
             values.put("max", metric.getMaxValue());
             values.put("info", metric.getAdditionalInfo());
             metrics.put(key, values);
         });
     }
     
     public Map<String, Map<String, Object>> getMetrics() { return metrics; }
     public SystemHealth getSystemHealth() { return systemHealth; }
     public long getTimestamp() { return timestamp; }
 }
 
 public enum HealthStatus {
     HEALTHY, WARNING, CRITICAL
 }
}