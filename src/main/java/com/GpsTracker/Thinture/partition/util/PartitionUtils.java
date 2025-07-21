package com.GpsTracker.Thinture.partition.util;

//================================================================================================
//PartitionUtils.java - MYSQL ONLY PARTITION MANAGEMENT
//================================================================================================

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ✅ MYSQL PARTITION MANAGEMENT
 * 🚀 Auto-detects table structure and creates partitions automatically
 * 📊 No manual intervention required!
 */
@Component
public class PartitionUtils {

    private static final Logger logger = LoggerFactory.getLogger(PartitionUtils.class);

    @Autowired
    private DataSource dataSource;

    // ═══════════════════════════════════════════════════════════════════════════════════
    // AUTO-SETUP ON APPLICATION START
    // ═══════════════════════════════════════════════════════════════════════════════════

    /**
     * 🚀 Automatically setup partitioning when application starts
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initializePartitioning() {
        logger.info("🚀 Initializing MySQL partitioning...");
        
        try {
            // Check if table exists
            if (!tableExists()) {
                logger.warn("⚠️ Table 'vehicle_history' does not exist. Please create it first.");
                return;
            }
            
            // Auto-convert to partitioned if needed
            if (!isTablePartitioned()) {
                logger.info("🔧 Table not partitioned. Converting automatically...");
                convertToPartitioned();
            } else {
                logger.info("✅ Table already partitioned");
            }
            
            // Create future partitions
            createFuturePartitions(3);
            
            logger.info("✅ Partition initialization completed");
            
        } catch (Exception e) {
            logger.error("❌ Error initializing partitioning: {}", e.getMessage(), e);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════════
    // PARTITION NAME UTILITIES
    // ═══════════════════════════════════════════════════════════════════════════════════

    /**
     * Get partition name for a date (p_202501, p_202502, etc.)
     */
    public String getPartitionName(LocalDate date) {
        return String.format("p_%04d%02d", date.getYear(), date.getMonthValue());
    }

    /**
     * Get partition name for year/month
     */
    public String getPartitionName(int year, int month) {
        return String.format("p_%04d%02d", year, month);
    }

    /**
     * Get current month partition name
     */
    public String getCurrentPartitionName() {
        LocalDate now = LocalDate.now();
        return getPartitionName(now);
    }

    /**
     * Get next month partition name
     */
    public String getNextPartitionName() {
        LocalDate nextMonth = LocalDate.now().plusMonths(1);
        return getPartitionName(nextMonth);
    }

    // ═══════════════════════════════════════════════════════════════════════════════════
    // TABLE DETECTION AND CONVERSION
    // ═══════════════════════════════════════════════════════════════════════════════════

    /**
     * Check if vehicle_history table exists
     */
    private boolean tableExists() {
        String sql = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'vehicle_history'";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException e) {
            logger.error("Error checking table existence: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Check if table is partitioned
     */
    public boolean isTablePartitioned() {
        String sql = """
            SELECT COUNT(*) 
            FROM INFORMATION_SCHEMA.PARTITIONS 
            WHERE TABLE_SCHEMA = DATABASE() 
            AND TABLE_NAME = 'vehicle_history' 
            AND PARTITION_NAME IS NOT NULL
            """;
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException e) {
            logger.debug("Could not check table partitioning: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 🔧 Auto-convert table to partitioned format
     */
    public boolean convertToPartitioned() {
        logger.info("🔧 Converting vehicle_history table to partitioned format...");
        
        try (Connection conn = dataSource.getConnection()) {
            
            // Check if already partitioned
            if (isTablePartitioned()) {
                logger.info("✅ Table is already partitioned");
                return true;
            }
            
            String sql = """
                ALTER TABLE vehicle_history 
                PARTITION BY RANGE (YEAR(timestamp) * 100 + MONTH(timestamp)) (
                    PARTITION p_202412 VALUES LESS THAN (202501),
                    PARTITION p_202501 VALUES LESS THAN (202502),
                    PARTITION p_202502 VALUES LESS THAN (202503),
                    PARTITION p_202503 VALUES LESS THAN (202504),
                    PARTITION p_202504 VALUES LESS THAN (202505),
                    PARTITION p_202505 VALUES LESS THAN (202506),
                    PARTITION p_202506 VALUES LESS THAN (202507),
                    PARTITION p_202507 VALUES LESS THAN (202508),
                    PARTITION p_202508 VALUES LESS THAN (202509),
                    PARTITION p_202509 VALUES LESS THAN (202510),
                    PARTITION p_202510 VALUES LESS THAN (202511),
                    PARTITION p_202511 VALUES LESS THAN (202512),
                    PARTITION p_202512 VALUES LESS THAN (202601)
                )
                """;
            
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(sql);
                logger.info("✅ Successfully converted table to partitioned format");
                return true;
            }
            
        } catch (SQLException e) {
            logger.error("❌ Error converting table to partitioned: {}", e.getMessage());
            
            // Check if it's a primary key issue
            if (e.getMessage().toLowerCase().contains("primary key")) {
                logger.error("💡 SOLUTION: Your table needs a composite primary key including 'timestamp'");
                logger.error("💡 Example: ALTER TABLE vehicle_history DROP PRIMARY KEY, ADD PRIMARY KEY (id, timestamp);");
            }
            
            return false;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════════
    // PARTITION EXISTENCE CHECKS
    // ═══════════════════════════════════════════════════════════════════════════════════

    /**
     * Check if partition exists
     */
    public boolean partitionExists(String partitionName) {
        String sql = """
            SELECT COUNT(*) 
            FROM INFORMATION_SCHEMA.PARTITIONS 
            WHERE TABLE_SCHEMA = DATABASE() 
            AND TABLE_NAME = 'vehicle_history' 
            AND PARTITION_NAME = ?
            """;
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, partitionName);
            
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
            
        } catch (SQLException e) {
            logger.error("Error checking partition existence: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Check if current month partition exists
     */
    public boolean currentPartitionExists() {
        return partitionExists(getCurrentPartitionName());
    }

    // ═══════════════════════════════════════════════════════════════════════════════════
    // PARTITION CREATION
    // ═══════════════════════════════════════════════════════════════════════════════════

    /**
     * Create monthly partition
     */
    public boolean createMonthlyPartition(int year, int month) {
        String partitionName = getPartitionName(year, month);
        
        if (partitionExists(partitionName)) {
            logger.info("✅ Partition {} already exists", partitionName);
            return true;
        }
        
        YearMonth yearMonth = YearMonth.of(year, month);
        YearMonth nextMonth = yearMonth.plusMonths(1);
        
        // ✅ FIXED: Replace UNIX_TIMESTAMP with deterministic function
        String sql = String.format("""
            ALTER TABLE vehicle_history 
            ADD PARTITION (
                PARTITION %s VALUES LESS THAN (%d)
            )
            """, partitionName, nextMonth.getYear() * 100 + nextMonth.getMonthValue());
        
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            
            stmt.executeUpdate(sql);
            logger.info("✅ Created partition: {}", partitionName);
            return true;
            
        } catch (SQLException e) {
            // Check for duplicate partition
            String errorMsg = e.getMessage().toLowerCase();
            if (errorMsg.contains("duplicate partition name") || errorMsg.contains("already exists")) {
                logger.info("✅ Partition {} already exists", partitionName);
                return true;
            }
            logger.error("❌ Error creating partition {}: {}", partitionName, e.getMessage());
            return false;
        }
    }

    /**
     * Create current month partition
     */
    public boolean createCurrentPartition() {
        LocalDate now = LocalDate.now();
        return createMonthlyPartition(now.getYear(), now.getMonthValue());
    }

    /**
     * Create next month partition
     */
    public boolean createNextPartition() {
        LocalDate nextMonth = LocalDate.now().plusMonths(1);
        return createMonthlyPartition(nextMonth.getYear(), nextMonth.getMonthValue());
    }

    /**
     * Create future partitions (next N months)
     */
    public List<String> createFuturePartitions(int monthsAhead) {
        List<String> createdPartitions = new ArrayList<>();
        LocalDate current = LocalDate.now();
        
        for (int i = 1; i <= monthsAhead; i++) {
            LocalDate futureDate = current.plusMonths(i);
            String partitionName = getPartitionName(futureDate);
            
            if (!partitionExists(partitionName)) {
                boolean created = createMonthlyPartition(futureDate.getYear(), futureDate.getMonthValue());
                if (created) {
                    createdPartitions.add(partitionName);
                }
            }
        }
        
        if (!createdPartitions.isEmpty()) {
            logger.info("Created {} future partitions: {}", createdPartitions.size(), createdPartitions);
        }
        
        return createdPartitions;
    }

    // ═══════════════════════════════════════════════════════════════════════════════════
    // PARTITION INFORMATION
    // ═══════════════════════════════════════════════════════════════════════════════════

    /**
     * Get all partition information
     */
    public List<Map<String, Object>> getAllPartitionInfo() {
        String sql = """
            SELECT 
                PARTITION_NAME as name,
                PARTITION_DESCRIPTION as description,
                TABLE_ROWS as rows,
                ROUND((DATA_LENGTH + INDEX_LENGTH) / 1024 / 1024, 2) AS size_mb
            FROM INFORMATION_SCHEMA.PARTITIONS 
            WHERE TABLE_SCHEMA = DATABASE() 
            AND TABLE_NAME = 'vehicle_history' 
            AND PARTITION_NAME IS NOT NULL
            ORDER BY PARTITION_NAME
            """;
        
        List<Map<String, Object>> partitions = new ArrayList<>();
        
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                Map<String, Object> partition = new HashMap<>();
                partition.put("name", rs.getString("name"));
                partition.put("description", rs.getString("description"));
                partition.put("rows", rs.getLong("rows"));
                partition.put("sizeMB", rs.getDouble("size_mb"));
                partitions.add(partition);
            }
            
            logger.debug("Retrieved {} partition info records", partitions.size());
            
        } catch (SQLException e) {
            logger.error("Error getting partition info: {}", e.getMessage());
        }
        
        return partitions;
    }

    /**
     * Get partition details for specific partition
     */
    public Map<String, Object> getPartitionDetails(String partitionName) {
        String sql = """
            SELECT 
                PARTITION_NAME as name,
                PARTITION_DESCRIPTION as description,
                TABLE_ROWS as rows,
                ROUND((DATA_LENGTH + INDEX_LENGTH) / 1024 / 1024, 2) AS size_mb,
                CREATE_TIME as create_time,
                UPDATE_TIME as update_time,
                CHECK_TIME as check_time
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
                    Map<String, Object> details = new HashMap<>();
                    details.put("name", rs.getString("name"));
                    details.put("description", rs.getString("description"));
                    details.put("rows", rs.getLong("rows"));
                    details.put("sizeMB", rs.getDouble("size_mb"));
                    details.put("createTime", rs.getTimestamp("create_time"));
                    details.put("updateTime", rs.getTimestamp("update_time"));
                    details.put("checkTime", rs.getTimestamp("check_time"));
                    details.put("databaseType", "MYSQL");
                    return details;
                }
            }
            
        } catch (SQLException e) {
            logger.error("Error getting partition details: {}", e.getMessage());
        }
        
        return new HashMap<>();
    }

    // ═══════════════════════════════════════════════════════════════════════════════════
    // PARTITION MAINTENANCE
    // ═══════════════════════════════════════════════════════════════════════════════════

    /**
     * Drop old partition
     */
    public boolean dropPartition(String partitionName) {
        if (!partitionExists(partitionName)) {
            logger.warn("Partition {} does not exist", partitionName);
            return false;
        }
        
        String sql = "ALTER TABLE vehicle_history DROP PARTITION " + partitionName;
        
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            
            stmt.executeUpdate(sql);
            logger.info("Dropped partition {}", partitionName);
            return true;
            
        } catch (SQLException e) {
            logger.error("Error dropping partition {}: {}", partitionName, e.getMessage());
            return false;
        }
    }

    /**
     * Optimize partition
     */
    public Map<String, Object> optimizePartition(String partitionName) {
        String sql = "OPTIMIZE TABLE vehicle_history PARTITION " + partitionName;
        Map<String, Object> result = new HashMap<>();
        
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            if (rs.next()) {
                result.put("table", rs.getString("Table"));
                result.put("op", rs.getString("Op"));
                result.put("msgType", rs.getString("Msg_type"));
                result.put("msgText", rs.getString("Msg_text"));
            }
            
            result.put("databaseType", "MYSQL");
            logger.info("Optimized partition {}", partitionName);
            
        } catch (SQLException e) {
            logger.error("Error optimizing partition {}: {}", partitionName, e.getMessage());
            result.put("error", e.getMessage());
            result.put("databaseType", "MYSQL");
        }
        
        return result;
    }

    /**
     * Analyze partition
     */
    public Map<String, Object> analyzePartition(String partitionName) {
        String sql = "ANALYZE TABLE vehicle_history PARTITION " + partitionName;
        Map<String, Object> result = new HashMap<>();
        
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            if (rs.next()) {
                result.put("table", rs.getString("Table"));
                result.put("op", rs.getString("Op"));
                result.put("msgType", rs.getString("Msg_type"));
                result.put("msgText", rs.getString("Msg_text"));
            }
            
            result.put("databaseType", "MYSQL");
            logger.info("Analyzed partition {}", partitionName);
            
        } catch (SQLException e) {
            logger.error("Error analyzing partition {}: {}", partitionName, e.getMessage());
            result.put("error", e.getMessage());
            result.put("databaseType", "MYSQL");
        }
        
        return result;
    }

    // ═══════════════════════════════════════════════════════════════════════════════════
    // UTILITY METHODS
    // ═══════════════════════════════════════════════════════════════════════════════════

    /**
     * Check if partition name is valid format
     */
    public boolean isValidPartitionName(String partitionName) {
        return partitionName != null && partitionName.matches("p_\\d{6}");
    }

    /**
     * Check if partition is recent (within last 3 months)
     */
    public boolean isRecentPartition(String partitionName) {
        if (!isValidPartitionName(partitionName)) {
            return false;
        }
        
        try {
            String yearMonth = partitionName.substring(2);
            int year = Integer.parseInt(yearMonth.substring(0, 4));
            int month = Integer.parseInt(yearMonth.substring(4, 6));
            
            LocalDate partitionDate = LocalDate.of(year, month, 1);
            LocalDate threeMonthsAgo = LocalDate.now().minusMonths(3);
            
            return partitionDate.isAfter(threeMonthsAgo);
            
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get partition metrics summary
     */
    public Map<String, Object> getPartitionMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        List<Map<String, Object>> partitions = getAllPartitionInfo();
        
        long totalRows = 0;
        double totalSizeMB = 0;
        
        for (Map<String, Object> partition : partitions) {
            totalRows += (Long) partition.get("rows");
            totalSizeMB += (Double) partition.get("sizeMB");
        }
        
        metrics.put("totalPartitions", partitions.size());
        metrics.put("totalRows", totalRows);
        metrics.put("totalSizeMB", Math.round(totalSizeMB * 100.0) / 100.0);
        metrics.put("avgRowsPerPartition", partitions.size() > 0 ? totalRows / partitions.size() : 0);
        metrics.put("avgSizeMBPerPartition", partitions.size() > 0 ? Math.round((totalSizeMB / partitions.size()) * 100.0) / 100.0 : 0);
        metrics.put("databaseType", "MYSQL");
        
        return metrics;
    }

    /**
     * Run automated maintenance (create future partitions, etc.)
     */
    public Map<String, Object> runAutomatedMaintenance() {
        Map<String, Object> result = new HashMap<>();
        
        // Create next 3 months of partitions
        List<String> createdPartitions = createFuturePartitions(3);
        result.put("createdPartitions", createdPartitions);
        
        // Get current metrics
        Map<String, Object> metrics = getPartitionMetrics();
        result.put("metrics", metrics);
        result.put("databaseType", "MYSQL");
        
        logger.info("Automated maintenance completed: created {} partitions", createdPartitions.size());
        
        return result;
    }

    /**
     * Cleanup old partitions (older than retention months)
     */
    public Map<String, Object> cleanupOldPartitions(int retentionMonths) {
        List<String> droppedPartitions = new ArrayList<>();
        LocalDate cutoffDate = LocalDate.now().minusMonths(retentionMonths);
        
        List<Map<String, Object>> allPartitions = getAllPartitionInfo();
        
        for (Map<String, Object> partition : allPartitions) {
            String partitionName = (String) partition.get("name");
            
            if (isValidPartitionName(partitionName)) {
                try {
                    String yearMonth = partitionName.substring(2);
                    int year = Integer.parseInt(yearMonth.substring(0, 4));
                    int month = Integer.parseInt(yearMonth.substring(4, 6));
                    
                    LocalDate partitionDate = LocalDate.of(year, month, 1);
                    
                    if (partitionDate.isBefore(cutoffDate)) {
                        if (dropPartition(partitionName)) {
                            droppedPartitions.add(partitionName);
                        }
                    }
                    
                } catch (Exception e) {
                    logger.error("Error processing partition {} for cleanup: {}", partitionName, e.getMessage());
                }
            }
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("droppedPartitions", droppedPartitions);
        result.put("retentionMonths", retentionMonths);
        result.put("cutoffDate", cutoffDate.toString());
        result.put("databaseType", "MYSQL");
        
        logger.info("Cleanup completed: dropped {} old partitions", droppedPartitions.size());
        
        return result;
    }

    /**
     * Test database connectivity
     */
    public Map<String, Object> testConnection() {
        Map<String, Object> result = new HashMap<>();
        long startTime = System.currentTimeMillis();
        
        try (Connection conn = dataSource.getConnection()) {
            boolean isValid = conn.isValid(5);
            long responseTime = System.currentTimeMillis() - startTime;
            
            result.put("connected", isValid);
            result.put("responseTimeMs", responseTime);
            result.put("databaseType", "MYSQL");
            result.put("autoCommit", conn.getAutoCommit());
            result.put("transactionIsolation", conn.getTransactionIsolation());
            result.put("readOnly", conn.isReadOnly());
            
            if (isValid) {
                // Test a simple query
                try (PreparedStatement stmt = conn.prepareStatement("SELECT 1");
                     ResultSet rs = stmt.executeQuery()) {
                    result.put("queryTest", rs.next());
                }
            }
            
        } catch (SQLException e) {
            result.put("connected", false);
            result.put("error", e.getMessage());
            logger.error("Connection test failed: {}", e.getMessage());
        }
        
        return result;
    }

    /**
     * Get database information
     */
    public Map<String, Object> getDatabaseInfo() {
        Map<String, Object> info = new HashMap<>();
        
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            
            info.put("databaseType", "MYSQL");
            info.put("productName", metaData.getDatabaseProductName());
            info.put("productVersion", metaData.getDatabaseProductVersion());
            info.put("driverName", metaData.getDriverName());
            info.put("driverVersion", metaData.getDriverVersion());
            info.put("supportsPartitioning", true);
            info.put("partitioningType", "MySQL Native");
            
            // Check table status
            info.put("vehicleHistoryExists", tableExists());
            info.put("isPartitioned", isTablePartitioned());
            info.put("partitionCount", getAllPartitionInfo().size());
            
        } catch (SQLException e) {
            logger.error("Error getting database info: {}", e.getMessage());
            info.put("error", e.getMessage());
        }
        
        return info;
    }
}