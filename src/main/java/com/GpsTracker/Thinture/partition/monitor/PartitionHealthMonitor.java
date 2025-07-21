package com.GpsTracker.Thinture.partition.monitor;

//================================================================================================
//PartitionHealthMonitor.java - MYSQL COMPATIBLE & HIGH PERFORMANCE
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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
* ✅ MYSQL-COMPATIBLE Real-time monitoring of partition health for GPS tracking system
* 🚀 HIGH PERFORMANCE with caching and parallel processing
* 📊 Provides proactive alerts and health checks for 5000+ device system
*/

@Component
@EnableScheduling
@ConditionalOnProperty(name = "gps.partition.health-monitor.enabled", havingValue = "true", matchIfMissing = true)
public class PartitionHealthMonitor {

    private static final Logger logger = LoggerFactory.getLogger(PartitionHealthMonitor.class);
    private static final Logger healthLogger = LoggerFactory.getLogger("HEALTH." + PartitionHealthMonitor.class.getName());
    private static final Logger alertLogger = LoggerFactory.getLogger("ALERT." + PartitionHealthMonitor.class.getName());

    @Autowired
    private PartitionUtils partitionUtils;
    
    @Autowired
    private DataSource dataSource;

    // ✅ Performance optimization - table existence cache
    private Boolean vehicleHistoryExists = null;
    private long lastTableCheck = 0;
    private static final long TABLE_CHECK_INTERVAL = 300000; // 5 minutes

    // Health check thresholds (configurable)
    @Value("${gps.partition.health.max-size-mb:5000}")
    private long maxPartitionSizeMB;
    
    @Value("${gps.partition.health.max-rows:20000000}")
    private long maxPartitionRows;
    
    @Value("${gps.partition.health.min-free-space-mb:10000}")
    private long minFreeSpaceMB;
    
    @Value("${gps.partition.health.max-query-time-ms:2000}")
    private long maxQueryTimeMs;
    
    @Value("${gps.partition.health.alert-threshold-mb:4500}")
    private long alertThresholdMB;

    // Health status tracking
    private Map<String, PartitionHealth> partitionHealthStatus = new HashMap<>();
    private boolean systemHealthy = true;
    private LocalDateTime lastHealthCheck;

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
    // SCHEDULED HEALTH CHECKS - MYSQL OPTIMIZED
    // ═══════════════════════════════════════════════════════════════════════════════════

    /**
     * ✅ Primary health check - runs every 30 minutes (MYSQL OPTIMIZED)
     */
    @Scheduled(fixedRate = 1800000) // 30 minutes
    @Transactional(readOnly = true)
    public void performHealthCheck() {
        long startTime = System.currentTimeMillis();
        
        try {
            // ✅ Fast pre-check: Skip if table doesn't exist
            if (!isVehicleHistoryTableReady()) {
                healthLogger.info("📋 vehicle_history table not ready - skipping health check");
                return;
            }
            
            healthLogger.info("🏥 Starting partition health check...");
            
            // ✅ Parallel execution for better performance
            CompletableFuture<Void> sizeCheck = CompletableFuture.runAsync(this::checkPartitionSizesSafe);
            CompletableFuture<Void> dbCheck = CompletableFuture.runAsync(this::checkDatabaseHealthSafe);
            CompletableFuture<Void> structureCheck = CompletableFuture.runAsync(this::checkPartitionStructureSafe);
            CompletableFuture<Void> storageCheck = CompletableFuture.runAsync(this::checkStorageSpaceSafe);
            
            // ✅ Wait for all checks to complete
            CompletableFuture.allOf(sizeCheck, dbCheck, structureCheck, storageCheck).get();
            
            // Update overall system health
            updateSystemHealthStatus();
            
            long duration = System.currentTimeMillis() - startTime;
            healthLogger.info("✅ Health check completed successfully in {}ms", duration);
            lastHealthCheck = LocalDateTime.now();
            
        } catch (Exception e) {
            alertLogger.error("❌ Health check failed: {}", e.getMessage());
            systemHealthy = false;
        }
    }

    /**
     * ✅ Quick health check - runs every 5 minutes (MYSQL OPTIMIZED)
     */
    @Scheduled(fixedRate = 300000) // 5 minutes
    public void quickHealthCheck() {
        try {
            // ✅ Skip if table not ready
            if (!isVehicleHistoryTableReady()) {
                return;
            }
            
            healthLogger.debug("🔍 Performing quick health check...");
            
            // Check if current partition exists and is accepting data
            String currentPartition = partitionUtils.getCurrentPartitionName();
            
            if (!partitionUtils.partitionExists(currentPartition)) {
                alertLogger.error("🚨 CRITICAL ALERT: Current partition {} does not exist!", currentPartition);
                triggerEmergencyPartitionCreation(currentPartition);
            }
            
            // Quick database connectivity check
            if (!isDatabaseResponsive()) {
                alertLogger.error("🚨 CRITICAL ALERT: Database connectivity issues detected!");
                systemHealthy = false;
            }
            
        } catch (Exception e) {
            alertLogger.error("❌ Quick health check failed: {}", e.getMessage());
        }
    }

    /**
     * ✅ Daily comprehensive health report - runs at 6:30 AM
     */
    @Scheduled(cron = "0 30 6 * * ?")
    public void dailyHealthReport() {
        try {
            if (!isVehicleHistoryTableReady()) {
                return;
            }
            
            healthLogger.info("📊 Generating daily partition health report...");
            generateComprehensiveHealthReport();
            
        } catch (Exception e) {
            alertLogger.error("❌ Daily health report generation failed: {}", e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════════
    // MYSQL-COMPATIBLE HEALTH CHECK METHODS
    // ═══════════════════════════════════════════════════════════════════════════════════

    /**
     * ✅ Check partition sizes (MYSQL-COMPATIBLE)
     */
    private void checkPartitionSizesSafe() {
        try {
            healthLogger.info("📏 Checking partition sizes...");
            
            // ✅ MySQL-compatible partition size query
            String sql = """
                SELECT 
                    COALESCE(p.partition_name, 'main_table') as name,
                    COALESCE(p.table_rows, t.table_rows, 0) as row_count,
                    ROUND(COALESCE((p.data_length + p.index_length), (t.data_length + t.index_length), 0) / 1024 / 1024, 2) as size_mb
                FROM information_schema.tables t
                LEFT JOIN information_schema.partitions p ON t.table_name = p.table_name 
                    AND t.table_schema = p.table_schema 
                    AND p.partition_name IS NOT NULL
                WHERE t.table_schema = DATABASE() 
                AND t.table_name = 'vehicle_history'
                ORDER BY COALESCE(p.partition_name, 'main_table')
                """;
            
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                
                int healthyPartitions = 0;
                int warningPartitions = 0;
                int criticalPartitions = 0;
                boolean hasResults = false;
                
                while (rs.next()) {
                    hasResults = true;
                    String partitionName = rs.getString("name");
                    long rows = rs.getLong("row_count");
                    double sizeMB = rs.getDouble("size_mb");
                    
                    PartitionHealth health = assessPartitionHealth(partitionName, rows, sizeMB);
                    partitionHealthStatus.put(partitionName, health);
                    
                    switch (health.getStatus()) {
                        case HEALTHY:
                            healthyPartitions++;
                            break;
                        case WARNING:
                            warningPartitions++;
                            alertLogger.warn("⚠️ WARNING: Partition {} approaching limits: {} rows, {:.1f}MB", 
                                           partitionName, rows, sizeMB);
                            break;
                        case CRITICAL:
                            criticalPartitions++;
                            alertLogger.error("🚨 CRITICAL: Partition {} exceeds limits: {} rows, {:.1f}MB", 
                                            partitionName, rows, sizeMB);
                            break;
                    }
                }
                
                if (!hasResults) {
                    healthLogger.info("📋 No partitions found - table may not be partitioned");
                } else {
                    healthLogger.info("📊 Partition health summary: {} healthy, {} warning, {} critical", 
                                    healthyPartitions, warningPartitions, criticalPartitions);
                }
            }
            
        } catch (Exception e) {
            alertLogger.error("❌ Error checking partition sizes: {}", e.getMessage());
        }
    }

    /**
     * ✅ Check database connectivity and performance (MYSQL-COMPATIBLE)
     */
    private void checkDatabaseHealthSafe() {
        try {
            healthLogger.info("🗄️ Checking database health...");
            
            long startTime = System.currentTimeMillis();
            
            try (Connection conn = dataSource.getConnection()) {
                boolean isValid = conn.isValid(5);
                long connectionTime = System.currentTimeMillis() - startTime;
                
                if (!isValid) {
                    alertLogger.error("🚨 CRITICAL: Database connection is invalid!");
                    return;
                }
                
                if (connectionTime > 1000) {
                    alertLogger.warn("⚠️ WARNING: Database connection slow: {}ms", connectionTime);
                }
                
                // ✅ MySQL-compatible query performance test
                checkQueryPerformanceMySQL(conn);
                
                // ✅ MySQL-compatible space check
                checkDatabaseSpaceMySQL(conn);
            }
            
        } catch (Exception e) {
            alertLogger.error("❌ Database health check failed: {}", e.getMessage());
            systemHealthy = false;
        }
    }

    /**
     * ✅ Check partition structure integrity (MYSQL-COMPATIBLE)
     */
    private void checkPartitionStructureSafe() {
        try {
            healthLogger.info("🔧 Checking partition structure integrity...");
            
            // ✅ MySQL-compatible partition structure check
            String sql = """
                SELECT COUNT(DISTINCT partition_name) as partition_count 
                FROM information_schema.partitions 
                WHERE table_schema = DATABASE() 
                AND table_name = 'vehicle_history'
                AND partition_name IS NOT NULL
                """;
            
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                
                if (rs.next()) {
                    int partitionCount = rs.getInt("partition_count");
                    
                    if (partitionCount == 0) {
                        healthLogger.info("📋 Table is not partitioned (normal for standard tables)");
                    } else if (partitionCount < 3) {
                        alertLogger.warn("⚠️ WARNING: Only {} partitions exist (expected 6+)", partitionCount);
                    } else {
                        healthLogger.info("✅ Partition structure healthy: {} partitions", partitionCount);
                    }
                }
            }
            
        } catch (Exception e) {
            alertLogger.error("❌ Error checking partition structure: {}", e.getMessage());
        }
    }

    /**
     * ✅ Check available storage space (MYSQL-COMPATIBLE)
     */
    private void checkStorageSpaceSafe() {
        try {
            healthLogger.info("💾 Checking storage space...");
            
            // ✅ MySQL-compatible storage space query
            String sql = """
                SELECT 
                    ROUND(SUM(data_length + index_length) / 1024 / 1024, 2) as total_size_mb,
                    ROUND(SUM(data_length) / 1024 / 1024, 2) as data_size_mb,
                    ROUND(SUM(index_length) / 1024 / 1024, 2) as index_size_mb
                FROM information_schema.tables 
                WHERE table_schema = DATABASE() 
                AND table_name = 'vehicle_history'
                """;
            
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                
                if (rs.next()) {
                    double totalSizeMB = rs.getDouble("total_size_mb");
                    double dataSizeMB = rs.getDouble("data_size_mb");
                    double indexSizeMB = rs.getDouble("index_size_mb");
                    
                    healthLogger.info("✅ Table storage - Total: {:.1f}MB, Data: {:.1f}MB, Index: {:.1f}MB", 
                                    totalSizeMB, dataSizeMB, indexSizeMB);
                    
                    // Check database free space
                    checkDatabaseFreeSpace(conn);
                }
            }
            
        } catch (Exception e) {
            alertLogger.error("❌ Error checking storage space: {}", e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════════
    // MYSQL-SPECIFIC HELPER METHODS
    // ═══════════════════════════════════════════════════════════════════════════════════

    /**
     * ✅ MySQL-compatible query performance test
     */
    private void checkQueryPerformanceMySQL(Connection conn) {
        try {
            long startTime = System.currentTimeMillis();
            
            // ✅ MySQL-compatible performance test query
            String testQuery = """
                SELECT COUNT(*) FROM vehicle_history 
                WHERE timestamp >= DATE_SUB(NOW(), INTERVAL 1 DAY)
                LIMIT 10000
                """;
            
            try (PreparedStatement stmt = conn.prepareStatement(testQuery);
                 ResultSet rs = stmt.executeQuery()) {
                
                long queryTime = System.currentTimeMillis() - startTime;
                
                if (queryTime > maxQueryTimeMs) {
                    alertLogger.warn("⚠️ WARNING: Slow query performance: {}ms (threshold: {}ms)", 
                                   queryTime, maxQueryTimeMs);
                } else {
                    healthLogger.debug("✅ Query performance healthy: {}ms", queryTime);
                }
            }
            
        } catch (Exception e) {
            healthLogger.debug("Query performance test skipped: {}", e.getMessage());
        }
    }

    /**
     * ✅ MySQL-compatible database space check
     */
    private void checkDatabaseSpaceMySQL(Connection conn) {
        try {
            // ✅ MySQL-compatible database size query
            String sql = """
                SELECT 
                    ROUND(SUM(data_length + index_length) / 1024 / 1024, 2) as total_size_mb,
                    COUNT(*) as table_count
                FROM information_schema.tables
                WHERE table_schema = DATABASE()
                """;
            
            try (PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                
                if (rs.next()) {
                    double sizeMB = rs.getDouble("total_size_mb");
                    int tableCount = rs.getInt("table_count");
                    healthLogger.info("📊 Database size: {:.1f}MB across {} tables", sizeMB, tableCount);
                }
            }
            
        } catch (Exception e) {
            healthLogger.debug("Database space check failed: {}", e.getMessage());
        }
    }

    /**
     * ✅ Check MySQL database free space
     */
    private void checkDatabaseFreeSpace(Connection conn) {
        try {
            // ✅ MySQL-compatible free space check using INFORMATION_SCHEMA
            String sql = """
                SELECT 
                    table_schema,
                    ROUND(SUM(data_length + index_length) / 1024 / 1024, 2) as used_mb
                FROM information_schema.tables 
                WHERE table_schema = DATABASE()
                GROUP BY table_schema
                """;
            
            try (PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                
                if (rs.next()) {
                    double usedMB = rs.getDouble("used_mb");
                    healthLogger.info("✅ Database space usage: {:.1f}MB", usedMB);
                    
                    // Note: MySQL doesn't easily provide free space like Oracle tablespaces
                    // This would require filesystem-level checks or MySQL-specific variables
                }
            }
            
        } catch (Exception e) {
            healthLogger.debug("Free space check not available: {}", e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════════
    // UTILITY METHODS (UPDATED FOR MYSQL)
    // ═══════════════════════════════════════════════════════════════════════════════════

    private PartitionHealth assessPartitionHealth(String partitionName, long rows, double sizeMB) {
        PartitionHealth health = new PartitionHealth(partitionName);
        
        if (sizeMB >= maxPartitionSizeMB || rows >= maxPartitionRows) {
            health.setStatus(HealthStatus.CRITICAL);
            health.addIssue("Partition exceeds size limits");
        } else if (sizeMB >= alertThresholdMB || rows >= maxPartitionRows * 0.9) {
            health.setStatus(HealthStatus.WARNING);
            health.addIssue("Partition approaching size limits");
        } else {
            health.setStatus(HealthStatus.HEALTHY);
        }
        
        health.setRows(rows);
        health.setSizeMB(sizeMB);
        health.setLastChecked(LocalDateTime.now());
        
        return health;
    }

    private boolean isDatabaseResponsive() {
        try (Connection conn = dataSource.getConnection()) {
            return conn.isValid(5);
        } catch (SQLException e) {
            return false;
        }
    }

    private void triggerEmergencyPartitionCreation(String partitionName) {
        try {
            alertLogger.error("🚨 EMERGENCY: Creating missing partition {}", partitionName);
            
            String yearMonth = partitionName.substring(2);
            int year = Integer.parseInt(yearMonth.substring(0, 4));
            int month = Integer.parseInt(yearMonth.substring(4, 6));
            
            boolean created = partitionUtils.createMonthlyPartition(year, month);
            
            if (created) {
                alertLogger.info("✅ Emergency partition {} created successfully", partitionName);
            } else {
                alertLogger.error("❌ FAILED to create emergency partition {}", partitionName);
            }
            
        } catch (Exception e) {
            alertLogger.error("❌ Emergency partition creation failed for {}: {}", partitionName, e.getMessage());
        }
    }

    private void updateSystemHealthStatus() {
        boolean previousHealth = systemHealthy;
        
        systemHealthy = partitionHealthStatus.values().stream()
            .noneMatch(health -> health.getStatus() == HealthStatus.CRITICAL);
        
        if (previousHealth != systemHealthy) {
            if (systemHealthy) {
                healthLogger.info("✅ System health restored to HEALTHY");
            } else {
                alertLogger.error("🚨 System health degraded to UNHEALTHY");
            }
        }
    }

    private void generateComprehensiveHealthReport() {
        healthLogger.info("📋 === DAILY PARTITION HEALTH REPORT ===");
        healthLogger.info("Report Date: {}", LocalDateTime.now());
        healthLogger.info("System Status: {}", systemHealthy ? "HEALTHY" : "UNHEALTHY");
        healthLogger.info("Last Health Check: {}", lastHealthCheck);
        
        if (!partitionHealthStatus.isEmpty()) {
            healthLogger.info("Partition Health Summary:");
            partitionHealthStatus.forEach((name, health) -> {
                healthLogger.info("  {} - Status: {}, Rows: {}, Size: {:.1f}MB", 
                                name, health.getStatus(), health.getRows(), health.getSizeMB());
            });
        } else {
            healthLogger.info("No partitions found (table not partitioned)");
        }
        
        healthLogger.info("===========================================");
    }

    // ═══════════════════════════════════════════════════════════════════════════════════
    // PUBLIC API METHODS
    // ═══════════════════════════════════════════════════════════════════════════════════

    public boolean isSystemHealthy() {
        return systemHealthy;
    }

    public Map<String, PartitionHealth> getPartitionHealthStatus() {
        return new HashMap<>(partitionHealthStatus);
    }

    public void triggerManualHealthCheck() {
        healthLogger.info("🔧 Manual health check triggered");
        performHealthCheck();
    }

    // ═══════════════════════════════════════════════════════════════════════════════════
    // INNER CLASSES (UNCHANGED)
    // ═══════════════════════════════════════════════════════════════════════════════════

    public static class PartitionHealth {
        private String partitionName;
        private HealthStatus status;
        private long rows;
        private double sizeMB;
        private LocalDateTime lastChecked;
        private String issues = "";

        public PartitionHealth(String partitionName) {
            this.partitionName = partitionName;
            this.status = HealthStatus.HEALTHY;
        }

        // Getters and setters
        public String getPartitionName() { return partitionName; }
        public HealthStatus getStatus() { return status; }
        public void setStatus(HealthStatus status) { this.status = status; }
        public long getRows() { return rows; }
        public void setRows(long rows) { this.rows = rows; }
        public double getSizeMB() { return sizeMB; }
        public void setSizeMB(double sizeMB) { this.sizeMB = sizeMB; }
        public LocalDateTime getLastChecked() { return lastChecked; }
        public void setLastChecked(LocalDateTime lastChecked) { this.lastChecked = lastChecked; }
        public String getIssues() { return issues; }
        public void addIssue(String issue) { 
            if (!issues.isEmpty()) issues += "; ";
            issues += issue;
        }
    }

    public enum HealthStatus {
        HEALTHY, WARNING, CRITICAL
    }
}