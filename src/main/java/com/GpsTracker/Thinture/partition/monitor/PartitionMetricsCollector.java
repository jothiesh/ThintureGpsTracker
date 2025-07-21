package com.GpsTracker.Thinture.partition.monitor;

//================================================================================================
//PartitionMetricsCollector.java - MYSQL COMPATIBLE & HIGH PERFORMANCE
//================================================================================================

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.GpsTracker.Thinture.partition.util.PartitionUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;

/**
* ✅ MYSQL-COMPATIBLE High-performance partition metrics collector
* 🚀 Optimized for speed with caching, parallel processing, and efficient queries
* 📊 Collects detailed metrics for partition performance analysis
*/

@Component
@EnableScheduling
@ConditionalOnProperty(name = "gps.partition.metrics.enabled", havingValue = "true", matchIfMissing = true)
public class PartitionMetricsCollector {

    private static final Logger logger = LoggerFactory.getLogger(PartitionMetricsCollector.class);
    private static final Logger metricsLogger = LoggerFactory.getLogger("METRICS." + PartitionMetricsCollector.class.getName());
    private static final Logger performanceLogger = LoggerFactory.getLogger("PERFORMANCE." + PartitionMetricsCollector.class.getName());

    @Autowired
    private PartitionUtils partitionUtils;
    
    @Autowired
    private DataSource dataSource;

    // ✅ Performance optimization - table existence cache
    private Boolean vehicleHistoryExists = null;
    private long lastTableCheck = 0;
    private static final long TABLE_CHECK_INTERVAL = 300000; // 5 minutes

    // Configuration properties
    @Value("${gps.partition.metrics.retention-days:30}")
    private int metricsRetentionDays;
    
    @Value("${gps.partition.metrics.performance-threshold-ms:1000}")
    private long performanceThresholdMs;
    
    @Value("${gps.partition.metrics.growth-alert-percentage:80}")
    private double growthAlertPercentage;

    // In-memory metrics storage (optimized for performance)
    private final Map<String, List<PartitionMetric>> partitionMetrics = new ConcurrentHashMap<>();
    private final Map<String, QueryPerformanceMetric> queryMetrics = new ConcurrentHashMap<>();
    private final List<SystemMetric> systemMetrics = Collections.synchronizedList(new ArrayList<>());

    // Performance tracking
    private volatile long totalQueriesExecuted = 0;
    private volatile double averageQueryTime = 0.0;
    private volatile long slowQueries = 0;

    // ═══════════════════════════════════════════════════════════════════════════════════
    // MYSQL TABLE EXISTENCE CHECK WITH CACHING
    // ═══════════════════════════════════════════════════════════════════════════════════

    private boolean isVehicleHistoryTableReady() {
        long currentTime = System.currentTimeMillis();
        
        // ✅ Use cached result if recent
        if (vehicleHistoryExists != null && 
            (currentTime - lastTableCheck) < TABLE_CHECK_INTERVAL) {
            return vehicleHistoryExists;
        }
        
        // ✅ MySQL-compatible table existence check
        String sql = """
            SELECT COUNT(*) 
            FROM information_schema.tables 
            WHERE table_schema = DATABASE() 
            AND table_name = 'vehicle_history'
            """;
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            if (rs.next()) {
                vehicleHistoryExists = rs.getInt(1) > 0;
                lastTableCheck = currentTime;
                return vehicleHistoryExists;
            }
            
        } catch (SQLException e) {
            logger.debug("Table existence check failed: {}", e.getMessage());
            vehicleHistoryExists = false;
        }
        
        return false;
    }

    // ═══════════════════════════════════════════════════════════════════════════════════
    // SCHEDULED METRICS COLLECTION - MYSQL OPTIMIZED
    // ═══════════════════════════════════════════════════════════════════════════════════

    /**
     * ✅ Collect detailed partition metrics - runs every hour (MYSQL OPTIMIZED)
     */
    @Scheduled(fixedRate = 3600000) // 1 hour
    @Transactional(readOnly = true)
    public void collectPartitionMetrics() {
        long startTime = System.currentTimeMillis();
        
        try {
            // ✅ Fast pre-check: Skip if table doesn't exist
            if (!isVehicleHistoryTableReady()) {
                metricsLogger.info("📋 vehicle_history table not ready - skipping metrics collection");
                return;
            }
            
            metricsLogger.info("📊 Starting partition metrics collection...");
            
            // ✅ Parallel collection for better performance
            CompletableFuture<Void> sizeMetrics = CompletableFuture.runAsync(this::collectPartitionSizeMetricsSafe);
            CompletableFuture<Void> perfMetrics = CompletableFuture.runAsync(this::collectPartitionPerformanceMetricsSafe);
            CompletableFuture<Void> storageMetrics = CompletableFuture.runAsync(this::collectStorageMetricsSafe);
            
            // ✅ Wait for all to complete
            CompletableFuture.allOf(sizeMetrics, perfMetrics, storageMetrics).get();
            
            // Analyze growth trends
            analyzeGrowthTrends();
            
            // Clean old metrics
            cleanupOldMetrics();
            
            long duration = System.currentTimeMillis() - startTime;
            metricsLogger.info("✅ Metrics collection completed in {}ms", duration);
            
        } catch (Exception e) {
            logger.error("❌ Error collecting partition metrics: {}", e.getMessage());
        }
    }

    /**
     * ✅ Collect real-time performance metrics - runs every 15 minutes (MYSQL OPTIMIZED)
     */
    @Scheduled(fixedRate = 900000) // 15 minutes
    public void collectRealTimeMetrics() {
        try {
            if (!isVehicleHistoryTableReady()) {
                return;
            }
            
            metricsLogger.debug("⚡ Collecting real-time performance metrics...");
            
            // ✅ Parallel real-time collection
            CompletableFuture.runAsync(this::collectSystemLoadMetricsSafe);
            CompletableFuture.runAsync(this::collectCurrentQueryMetricsSafe);
            
        } catch (Exception e) {
            logger.error("❌ Error collecting real-time metrics: {}", e.getMessage());
        }
    }

    /**
     * ✅ Generate daily metrics report - runs at 7:00 AM
     */
    @Scheduled(cron = "0 0 7 * * ?")
    public void generateDailyMetricsReport() {
        try {
            if (!isVehicleHistoryTableReady()) {
                return;
            }
            
            metricsLogger.info("📋 Generating daily metrics report...");
            generatePerformanceReport();
            generateGrowthReport();
            generateEfficiencyReport();
            
        } catch (Exception e) {
            logger.error("❌ Error generating daily metrics report: {}", e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════════
    // MYSQL-COMPATIBLE METRICS COLLECTION METHODS
    // ═══════════════════════════════════════════════════════════════════════════════════

    /**
     * ✅ Collect partition size and growth metrics (MYSQL-COMPATIBLE)
     */
    private void collectPartitionSizeMetricsSafe() {
        try {
            metricsLogger.info("📏 Collecting partition size metrics...");
            
            // ✅ MySQL-compatible partition metrics query
            String sql = """
                SELECT 
                    COALESCE(p.partition_name, 'main_table') as partition_name,
                    COALESCE(p.table_rows, t.table_rows, 0) as row_count,
                    ROUND(COALESCE((p.data_length + p.index_length), (t.data_length + t.index_length), 0) / 1024 / 1024, 2) as size_mb,
                    t.create_time,
                    t.update_time
                FROM information_schema.tables t
                LEFT JOIN information_schema.partitions p ON t.table_name = p.table_name 
                    AND t.table_schema = p.table_schema 
                    AND p.partition_name IS NOT NULL
                WHERE t.table_schema = DATABASE() 
                AND t.table_name = 'vehicle_history'
                """;
            
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                
                LocalDateTime now = LocalDateTime.now();
                boolean hasResults = false;
                
                while (rs.next()) {
                    hasResults = true;
                    String partitionName = rs.getString("partition_name");
                    long rows = rs.getLong("row_count");
                    double sizeMB = rs.getDouble("size_mb");
                    
                    PartitionMetric metric = createPartitionMetric(partitionName, now, rows, sizeMB);
                    addPartitionMetric(partitionName, metric);
                }
                
                if (!hasResults) {
                    // Handle non-partitioned table
                    getTableStats(conn, "main_table", now);
                }
            }
            
        } catch (Exception e) {
            logger.error("❌ Error collecting partition size metrics: {}", e.getMessage());
        }
    }

    /**
     * ✅ Get table stats for non-partitioned tables (MYSQL)
     */
    private void getTableStats(Connection conn, String tableName, LocalDateTime now) {
        try {
            // ✅ MySQL-compatible table statistics
            String sql = """
                SELECT 
                    COALESCE(table_rows, 0) as row_count,
                    ROUND(COALESCE((data_length + index_length), 0) / 1024 / 1024, 2) as size_mb,
                    create_time,
                    update_time
                FROM information_schema.tables 
                WHERE table_schema = DATABASE() 
                AND table_name = 'vehicle_history'
                """;
            
            try (PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                
                if (rs.next()) {
                    long rows = rs.getLong("row_count");
                    double sizeMB = rs.getDouble("size_mb");
                    
                    PartitionMetric metric = createPartitionMetric(tableName, now, rows, sizeMB);
                    addPartitionMetric(tableName, metric);
                    
                    metricsLogger.info("📊 Table metrics: {} rows, {:.1f}MB", rows, sizeMB);
                }
            }
            
        } catch (Exception e) {
            logger.debug("Could not get table stats: {}", e.getMessage());
        }
    }

    /**
     * ✅ Collect partition performance metrics (MYSQL-COMPATIBLE)
     */
    private void collectPartitionPerformanceMetricsSafe() {
        try {
            metricsLogger.info("⚡ Collecting partition performance metrics...");

            // ✅ Fix for effectively final
            final String resolvedPartition = 
                (partitionUtils.getCurrentPartitionName() == null) 
                    ? "main_table" 
                    : partitionUtils.getCurrentPartitionName();

            // ✅ MySQL-compatible performance test
            long queryTime = measureQueryTimeMySQL();

            QueryPerformanceMetric metric = queryMetrics.computeIfAbsent(
                resolvedPartition,
                k -> new QueryPerformanceMetric(resolvedPartition)
            );

            metric.addQueryTime(queryTime);

            if (queryTime > performanceThresholdMs) {
                performanceLogger.warn(
                    "⚠️ SLOW QUERY: {} query took {}ms (threshold: {}ms)", 
                    resolvedPartition, queryTime, performanceThresholdMs
                );
                slowQueries++;
            }

            // Update global metrics
            totalQueriesExecuted++;
            averageQueryTime = ((averageQueryTime * (totalQueriesExecuted - 1)) + queryTime) / totalQueriesExecuted;

        } catch (Exception e) {
            logger.error("❌ Error collecting partition performance metrics: {}", e.getMessage());
        }
    }

    /**
     * ✅ Collect storage utilization metrics (MYSQL-COMPATIBLE)
     */
    private void collectStorageMetricsSafe() {
        try {
            metricsLogger.info("💾 Collecting storage metrics...");
            
            // ✅ MySQL-compatible storage query
            String sql = """
                SELECT 
                    ROUND(SUM(COALESCE(data_length + index_length, 0)) / 1024 / 1024, 2) as total_size_mb,
                    COUNT(*) as table_count,
                    ROUND(SUM(COALESCE(data_length, 0)) / 1024 / 1024, 2) as data_size_mb,
                    ROUND(SUM(COALESCE(index_length, 0)) / 1024 / 1024, 2) as index_size_mb
                FROM information_schema.tables 
                WHERE table_schema = DATABASE()
                """;
            
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                
                if (rs.next()) {
                    SystemMetric metric = new SystemMetric();
                    metric.setTimestamp(LocalDateTime.now());
                    metric.setTotalSizeMB(rs.getDouble("total_size_mb"));
                    metric.setTableCount(rs.getInt("table_count"));
                    metric.setDataSizeMB(rs.getDouble("data_size_mb"));
                    metric.setIndexSizeMB(rs.getDouble("index_size_mb"));
                    
                    systemMetrics.add(metric);
                    
                    metricsLogger.info("💾 Storage metrics: Total {:.1f}MB, Tables: {}, Data: {:.1f}MB, Index: {:.1f}MB", 
                                     metric.getTotalSizeMB(), metric.getTableCount(), 
                                     metric.getDataSizeMB(), metric.getIndexSizeMB());
                }
            }
            
        } catch (Exception e) {
            logger.error("❌ Error collecting storage metrics: {}", e.getMessage());
        }
    }

    /**
     * ✅ Collect system load metrics (MYSQL-COMPATIBLE)
     */
    private void collectSystemLoadMetricsSafe() {
        try {
            // ✅ MySQL-compatible system metrics
            String sql = """
                SELECT 
                    (SELECT COUNT(*) FROM information_schema.processlist WHERE command != 'Sleep') as active_connections,
                    (SELECT COUNT(*) FROM information_schema.processlist) as total_connections
                """;
            
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                
                if (rs.next()) {
                    int activeConnections = rs.getInt("active_connections");
                    int totalConnections = rs.getInt("total_connections");
                    
                    metricsLogger.debug("🔄 System load - Active connections: {}, Total: {}", 
                                      activeConnections, totalConnections);
                }
            }
            
        } catch (Exception e) {
            // Normal for users without PROCESS privilege
            logger.debug("Could not collect system load metrics: {}", e.getMessage());
        }
    }

    /**
     * ✅ Collect current query performance (MYSQL-COMPATIBLE)
     */
    private void collectCurrentQueryMetricsSafe() {
        try {
            long queryTime = measureQueryTimeMySQL();
            
            metricsLogger.debug("⚡ Current query performance: {}ms", queryTime);
            
            if (queryTime > performanceThresholdMs) {
                performanceLogger.warn("⚠️ Current query performance degraded: {}ms", queryTime);
            }
            
        } catch (Exception e) {
            logger.debug("Could not collect current query metrics: {}", e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════════
    // MYSQL-COMPATIBLE HELPER METHODS
    // ═══════════════════════════════════════════════════════════════════════════════════

    /**
     * ✅ Measure query time using MySQL-compatible query
     */
    private long measureQueryTimeMySQL() {
        try {
            long startTime = System.currentTimeMillis();
            
            // ✅ MySQL-compatible performance test query
            String sql = """
                SELECT COUNT(*) FROM vehicle_history 
                WHERE timestamp >= DATE_SUB(NOW(), INTERVAL 1 DAY)
                LIMIT 1000
                """;
            
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                
                rs.next(); // Execute the query
                return System.currentTimeMillis() - startTime;
            }
            
        } catch (Exception e) {
            // Fallback to simple query
            try {
                long startTime = System.currentTimeMillis();
                
                try (Connection conn = dataSource.getConnection();
                     PreparedStatement stmt = conn.prepareStatement("SELECT 1");
                     ResultSet rs = stmt.executeQuery()) {
                    
                    return System.currentTimeMillis() - startTime;
                }
            } catch (Exception e2) {
                logger.debug("Could not measure query time: {}", e2.getMessage());
                return 0;
            }
        }
    }

    /**
     * ✅ Create partition metric with growth calculation
     */
    private PartitionMetric createPartitionMetric(String partitionName, LocalDateTime now, long rows, double sizeMB) {
        PartitionMetric metric = new PartitionMetric();
        metric.setPartitionName(partitionName);
        metric.setTimestamp(now);
        metric.setRows(rows);
        metric.setSizeMB(sizeMB);
        
        // Calculate growth rate if we have previous data
        List<PartitionMetric> history = partitionMetrics.get(partitionName);
        if (history != null && !history.isEmpty()) {
            PartitionMetric lastMetric = history.get(history.size() - 1);
            long hoursDiff = java.time.Duration.between(lastMetric.getTimestamp(), now).toHours();
            
            if (hoursDiff > 0) {
                double growthRate = ((double) (rows - lastMetric.getRows())) / hoursDiff;
                metric.setGrowthRatePerHour(growthRate);
                
                // Alert if growth rate is unusually high
                if (growthRate > 100000) { // More than 100k records/hour
                    performanceLogger.warn("⚠️ HIGH GROWTH RATE: {} growing at {:.0f} rows/hour", 
                                         partitionName, growthRate);
                }
            }
        }
        
        return metric;
    }

    /**
     * ✅ Add partition metric to history
     */
    private void addPartitionMetric(String partitionName, PartitionMetric metric) {
        List<PartitionMetric> history = partitionMetrics.computeIfAbsent(partitionName, k -> new ArrayList<>());
        history.add(metric);
        
        metricsLogger.debug("📊 Collected metrics for {}: {} rows, {:.1f}MB", 
                          partitionName, metric.getRows(), metric.getSizeMB());
    }

    // ═══════════════════════════════════════════════════════════════════════════════════
    // ANALYSIS AND REPORTING METHODS
    // ═══════════════════════════════════════════════════════════════════════════════════

    private void analyzeGrowthTrends() {
        metricsLogger.info("📈 Analyzing partition growth trends...");
        
        try {
            for (Map.Entry<String, List<PartitionMetric>> entry : partitionMetrics.entrySet()) {
                String partitionName = entry.getKey();
                List<PartitionMetric> metrics = entry.getValue();
                
                if (metrics.size() >= 24) { // Need at least 24 hours of data
                    analyzePartitionTrend(partitionName, metrics);
                }
            }
            
        } catch (Exception e) {
            logger.error("❌ Error analyzing growth trends: {}", e.getMessage());
        }
    }

    private void analyzePartitionTrend(String partitionName, List<PartitionMetric> metrics) {
        List<PartitionMetric> recentMetrics = metrics.subList(Math.max(0, metrics.size() - 24), metrics.size());
        
        if (recentMetrics.size() < 2) return;
        
        PartitionMetric first = recentMetrics.get(0);
        PartitionMetric last = recentMetrics.get(recentMetrics.size() - 1);
        
        long hoursDiff = java.time.Duration.between(first.getTimestamp(), last.getTimestamp()).toHours();
        if (hoursDiff == 0) return;
        
        double growthRate = (double) (last.getRows() - first.getRows()) / hoursDiff;
        double sizeGrowthRate = (last.getSizeMB() - first.getSizeMB()) / hoursDiff;
        
        metricsLogger.info("📊 Trend analysis for {}: {:.0f} rows/hour, {:.2f} MB/hour", 
                         partitionName, growthRate, sizeGrowthRate);
        
        // Predict when partition might reach limits
        if (growthRate > 0) {
            long currentRows = last.getRows();
            long maxRows = 20000000; // 20M rows limit
            long hoursToLimit = (long) ((maxRows - currentRows) / growthRate);
            
            if (hoursToLimit < 168) { // Less than 7 days
                performanceLogger.warn("⚠️ CAPACITY WARNING: {} may reach row limit in {} hours", 
                                     partitionName, hoursToLimit);
            }
        }
    }

    private void generatePerformanceReport() {
        metricsLogger.info("📊 === DAILY PERFORMANCE REPORT ===");
        metricsLogger.info("Report Date: {}", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE));
        metricsLogger.info("Total Queries Executed: {}", totalQueriesExecuted);
        metricsLogger.info("Average Query Time: {:.1f}ms", averageQueryTime);
        metricsLogger.info("Slow Queries (>{} ms): {}", performanceThresholdMs, slowQueries);
        metricsLogger.info("Slow Query Percentage: {:.1f}%", 
                         totalQueriesExecuted > 0 ? (slowQueries * 100.0 / totalQueriesExecuted) : 0);
        
        // Performance by partition
        metricsLogger.info("Query Performance by Partition:");
        queryMetrics.entrySet().stream()
            .sorted((e1, e2) -> Double.compare(e1.getValue().getAverageTime(), e2.getValue().getAverageTime()))
            .forEach(entry -> {
                QueryPerformanceMetric metric = entry.getValue();
                metricsLogger.info("  {} - Avg: {:.1f}ms, Min: {}ms, Max: {}ms", 
                                 entry.getKey(), metric.getAverageTime(), 
                                 metric.getMinTime(), metric.getMaxTime());
            });
        
        metricsLogger.info("=====================================");
    }

    private void generateGrowthReport() {
        metricsLogger.info("📈 === DAILY GROWTH REPORT ===");
        
        partitionMetrics.entrySet().stream()
            .filter(entry -> !entry.getValue().isEmpty())
            .forEach(entry -> {
                String partitionName = entry.getKey();
                List<PartitionMetric> metrics = entry.getValue();
                PartitionMetric latest = metrics.get(metrics.size() - 1);
                
                metricsLogger.info("{} - Rows: {}, Size: {:.1f}MB, Growth: {:.0f} rows/hour", 
                                 partitionName, latest.getRows(), latest.getSizeMB(), 
                                 latest.getGrowthRatePerHour());
            });
        
        metricsLogger.info("===============================");
    }

    private void generateEfficiencyReport() {
        metricsLogger.info("⚡ === EFFICIENCY REPORT ===");
        
        if (!systemMetrics.isEmpty()) {
            SystemMetric latest = systemMetrics.get(systemMetrics.size() - 1);
            double indexRatio = latest.getDataSizeMB() > 0 ? 
                (latest.getIndexSizeMB() / latest.getDataSizeMB() * 100) : 0;
            
            metricsLogger.info("Storage Efficiency:");
            metricsLogger.info("  Total Size: {:.1f}MB", latest.getTotalSizeMB());
            metricsLogger.info("  Data Size: {:.1f}MB", latest.getDataSizeMB());
            metricsLogger.info("  Index Size: {:.1f}MB ({:.1f}% of data)", latest.getIndexSizeMB(), indexRatio);
            
            if (indexRatio > 50) {
                metricsLogger.warn("⚠️ High index ratio: {:.1f}% - consider index optimization", indexRatio);
            }
        }
        
        metricsLogger.info("=============================");
    }

    // ═══════════════════════════════════════════════════════════════════════════════════
    // UTILITY METHODS
    // ═══════════════════════════════════════════════════════════════════════════════════

    private void cleanupOldMetrics() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(metricsRetentionDays);
        
        partitionMetrics.forEach((partition, metrics) -> {
            metrics.removeIf(metric -> metric.getTimestamp().isBefore(cutoff));
        });
        
        systemMetrics.removeIf(metric -> metric.getTimestamp().isBefore(cutoff));
        
        metricsLogger.debug("🧹 Cleaned up metrics older than {} days", metricsRetentionDays);
    }

    /**
     * ✅ Record query execution time (called by other components)
     */
    public void recordQueryTime(String partitionName, long queryTimeMs) {
        QueryPerformanceMetric metric = queryMetrics.computeIfAbsent(partitionName, 
            k -> new QueryPerformanceMetric(partitionName));
        
        metric.addQueryTime(queryTimeMs);
        
        if (queryTimeMs > performanceThresholdMs) {
            performanceLogger.warn("⚠️ Slow query on {}: {}ms", partitionName, queryTimeMs);
        }
    }

    /**
     * ✅ Get metrics summary for external use
     */
    public Map<String, Object> getMetricsSummary() {
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalQueries", totalQueriesExecuted);
        summary.put("averageQueryTime", averageQueryTime);
        summary.put("slowQueries", slowQueries);
        summary.put("partitionCount", partitionMetrics.size());
        summary.put("lastCollection", LocalDateTime.now());
        summary.put("tableReady", vehicleHistoryExists);
        
        return summary;
    }

    // ═══════════════════════════════════════════════════════════════════════════════════
    // INNER CLASSES (UNCHANGED)
    // ═══════════════════════════════════════════════════════════════════════════════════

    public static class PartitionMetric {
        private String partitionName;
        private LocalDateTime timestamp;
        private long rows;
        private double sizeMB;
        private double growthRatePerHour;

        // Getters and setters
        public String getPartitionName() { return partitionName; }
        public void setPartitionName(String partitionName) { this.partitionName = partitionName; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
        public long getRows() { return rows; }
        public void setRows(long rows) { this.rows = rows; }
        public double getSizeMB() { return sizeMB; }
        public void setSizeMB(double sizeMB) { this.sizeMB = sizeMB; }
        public double getGrowthRatePerHour() { return growthRatePerHour; }
        public void setGrowthRatePerHour(double growthRatePerHour) { this.growthRatePerHour = growthRatePerHour; }
    }

    public static class QueryPerformanceMetric {
        private String partitionName;
        private List<Long> queryTimes = Collections.synchronizedList(new ArrayList<>());
        private volatile long totalTime = 0;
        private volatile long minTime = Long.MAX_VALUE;
        private volatile long maxTime = 0;

        public QueryPerformanceMetric(String partitionName) {
            this.partitionName = partitionName;
        }

        public synchronized void addQueryTime(long timeMs) {
            queryTimes.add(timeMs);
            totalTime += timeMs;
            minTime = Math.min(minTime, timeMs);
            maxTime = Math.max(maxTime, timeMs);
        }

        public double getAverageTime() {
            return queryTimes.isEmpty() ? 0 : (double) totalTime / queryTimes.size();
        }

        // Getters
        public String getPartitionName() { return partitionName; }
        public long getMinTime() { return minTime == Long.MAX_VALUE ? 0 : minTime; }
        public long getMaxTime() { return maxTime; }
        public int getQueryCount() { return queryTimes.size(); }
    }

    public static class SystemMetric {
        private LocalDateTime timestamp;
        private double totalSizeMB;
        private double dataSizeMB;
        private double indexSizeMB;
        private int tableCount;

        // Getters and setters
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
        public double getTotalSizeMB() { return totalSizeMB; }
        public void setTotalSizeMB(double totalSizeMB) { this.totalSizeMB = totalSizeMB; }
        public double getDataSizeMB() { return dataSizeMB; }
        public void setDataSizeMB(double dataSizeMB) { this.dataSizeMB = dataSizeMB; }
        public double getIndexSizeMB() { return indexSizeMB; }
        public void setIndexSizeMB(double indexSizeMB) { this.indexSizeMB = indexSizeMB; }
        public int getTableCount() { return tableCount; }
        public void setTableCount(int tableCount) { this.tableCount = tableCount; }
    }
}