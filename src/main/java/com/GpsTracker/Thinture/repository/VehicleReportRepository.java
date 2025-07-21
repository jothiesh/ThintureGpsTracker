
// ================================================================================================
// VehicleReportRepository.java - UPDATED FOR PARTITIONED TABLE
// ================================================================================================

package com.GpsTracker.Thinture.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.GpsTracker.Thinture.model.Vehicle;
import java.sql.Timestamp;
import java.util.List;

/**
 * ═══════════════════════════════════════════════════════════════════════════════════
 * 📊 VEHICLE REPORT REPOSITORY - OPTIMIZED FOR PARTITIONED TABLES
 * ═══════════════════════════════════════════════════════════════════════════════════
 * 
 * Key Features:
 * ✅ All queries use partition pruning (timestamp filters)
 * ✅ Optimized for reporting and analytics
 * ✅ Simple List returns for easy processing
 * ✅ Native queries for maximum performance
 * 
 * Perfect for: Management reports, fleet analytics, compliance reporting
 * ═══════════════════════════════════════════════════════════════════════════════════
 */
@Repository
public interface VehicleReportRepository extends JpaRepository<Vehicle, Long> {

    // ═══════════════════════════════════════════════════════════════════════════════════
    // 📊 CORE REPORTING QUERIES (PARTITION-AWARE)
    // ═══════════════════════════════════════════════════════════════════════════════════

    /**
     * 📋 MAIN REPORT: General GPS data report with filters
     * Always includes timestamp for partition pruning
     */
    @Query(value = """
        SELECT 
            device_id, 
            latitude, 
            longitude, 
            speed, 
            timestamp, 
            ignition, 
            vehicleStatus,
            status,
            course,
            gsmStrength,
            panic
        FROM vehicle_history 
        WHERE timestamp >= :startDate 
          AND timestamp <= :endDate
          AND (:deviceId IS NULL OR device_id = :deviceId) 
          AND (:vehicleStatus IS NULL OR vehicleStatus = :vehicleStatus)
          AND (:adminId IS NULL OR admin_id = :adminId)
        ORDER BY device_id, timestamp DESC
        """, nativeQuery = true)
    List<Object[]> findReportsPartitionAware(
        @Param("startDate") Timestamp startDate,
        @Param("endDate") Timestamp endDate,
        @Param("deviceId") String deviceId,
        @Param("vehicleStatus") String vehicleStatus,
        @Param("adminId") Long adminId
    );

    /**
     * 🅿️ PARKING REPORT: Find all parking events in date range
     * Optimized for partition pruning
     */
    @Query(value = """
        SELECT 
            device_id, 
            latitude, 
            longitude, 
            speed, 
            timestamp, 
            vehicleStatus, 
            ignition 
        FROM vehicle_history 
        WHERE timestamp >= :startDate 
          AND timestamp <= :endDate
          AND vehicleStatus = 'PARKED'
          AND (:deviceId IS NULL OR device_id = :deviceId)
          AND (:adminId IS NULL OR admin_id = :adminId)
        ORDER BY device_id, timestamp DESC
        """, nativeQuery = true)
    List<Object[]> findParkedReportsPartitionAware(
        @Param("startDate") Timestamp startDate, 
        @Param("endDate") Timestamp endDate,
        @Param("deviceId") String deviceId,
        @Param("adminId") Long adminId
    );

    /**
     * ⏱️ PARKING DURATION: Calculate parking durations (OPTIMIZED)
     * Uses partition pruning and window functions
     */
    @Query(value = """
        SELECT 
            device_id AS deviceId,
            timestamp AS startParkedTime,
            latitude AS parkedLatitude,
            longitude AS parkedLongitude,
            LEAD(timestamp) OVER (
                PARTITION BY device_id 
                ORDER BY timestamp
            ) AS endParkedTime,
            TIMESTAMPDIFF(MINUTE, 
                timestamp, 
                LEAD(timestamp) OVER (
                    PARTITION BY device_id 
                    ORDER BY timestamp
                )
            ) AS parkedDurationMinutes
        FROM vehicle_history 
        WHERE timestamp >= :startDate 
          AND timestamp <= :endDate
          AND vehicleStatus = 'PARKED'
          AND (:deviceId IS NULL OR device_id = :deviceId)
          AND (:adminId IS NULL OR admin_id = :adminId)
        ORDER BY device_id, timestamp
        """, nativeQuery = true)
    List<Object[]> findParkingDurationsOptimized(
        @Param("startDate") Timestamp startDate,
        @Param("endDate") Timestamp endDate,
        @Param("deviceId") String deviceId,
        @Param("adminId") Long adminId
    );

    // ═══════════════════════════════════════════════════════════════════════════════════
    // 🚗 VEHICLE STATUS REPORTS
    // ═══════════════════════════════════════════════════════════════════════════════════

    /**
     * 🏃 RUNNING VEHICLES: Get all running vehicles in time range
     */
    @Query(value = """
        SELECT 
            device_id,
            latitude,
            longitude,
            speed,
            timestamp,
            ignition,
            vehicleStatus
        FROM vehicle_history 
        WHERE timestamp >= :startDate 
          AND timestamp <= :endDate
          AND vehicleStatus IN ('RUNNING', 'MOVING')
          AND (:adminId IS NULL OR admin_id = :adminId)
        ORDER BY device_id, timestamp DESC
        """, nativeQuery = true)
    List<Object[]> findRunningVehiclesReport(
        @Param("startDate") Timestamp startDate,
        @Param("endDate") Timestamp endDate,
        @Param("adminId") Long adminId
    );

    /**
     * 🛑 IDLE VEHICLES: Get all idle vehicles in time range
     */
    @Query(value = """
        SELECT 
            device_id,
            latitude,
            longitude,
            speed,
            timestamp,
            ignition,
            vehicleStatus
        FROM vehicle_history 
        WHERE timestamp >= :startDate 
          AND timestamp <= :endDate
          AND vehicleStatus = 'IDLE'
          AND speed < 5
          AND (:adminId IS NULL OR admin_id = :adminId)
        ORDER BY device_id, timestamp DESC
        """, nativeQuery = true)
    List<Object[]> findIdleVehiclesReport(
        @Param("startDate") Timestamp startDate,
        @Param("endDate") Timestamp endDate,
        @Param("adminId") Long adminId
    );

    // ═══════════════════════════════════════════════════════════════════════════════════
    // 🚨 ALERT & VIOLATION REPORTS
    // ═══════════════════════════════════════════════════════════════════════════════════

    /**
     * ⚡ SPEED VIOLATIONS: Comprehensive speeding report
     */
    @Query(value = """
        SELECT 
            device_id,
            latitude,
            longitude,
            speed,
            timestamp,
            vehicleStatus,
            course
        FROM vehicle_history 
        WHERE timestamp >= :startDate 
          AND timestamp <= :endDate
          AND speed > :speedLimit
          AND (:deviceId IS NULL OR device_id = :deviceId)
          AND (:adminId IS NULL OR admin_id = :adminId)
        ORDER BY speed DESC, timestamp DESC
        """, nativeQuery = true)
    List<Object[]> findSpeedViolationReport(
        @Param("startDate") Timestamp startDate,
        @Param("endDate") Timestamp endDate,
        @Param("speedLimit") Double speedLimit,
        @Param("deviceId") String deviceId,
        @Param("adminId") Long adminId
    );

    /**
     * 🚨 PANIC ALERT REPORT: All panic incidents
     */
    @Query(value = """
        SELECT 
            device_id,
            latitude,
            longitude,
            speed,
            timestamp,
            vehicleStatus,
            ignition,
            panic
        FROM vehicle_history 
        WHERE timestamp >= :startDate 
          AND timestamp <= :endDate
          AND panic = 1
          AND (:deviceId IS NULL OR device_id = :deviceId)
          AND (:adminId IS NULL OR admin_id = :adminId)
        ORDER BY timestamp DESC
        """, nativeQuery = true)
    List<Object[]> findPanicAlertReport(
        @Param("startDate") Timestamp startDate,
        @Param("endDate") Timestamp endDate,
        @Param("deviceId") String deviceId,
        @Param("adminId") Long adminId
    );

    // ═══════════════════════════════════════════════════════════════════════════════════
    // 📈 ANALYTICS & SUMMARY REPORTS
    // ═══════════════════════════════════════════════════════════════════════════════════

    /**
     * 📊 DAILY FLEET SUMMARY: Complete daily statistics
     */
    @Query(value = """
        SELECT 
            DATE(timestamp) as report_date,
            device_id,
            COUNT(*) as total_records,
            AVG(speed) as avg_speed,
            MAX(speed) as max_speed,
            SUM(CASE WHEN vehicleStatus = 'RUNNING' THEN 1 ELSE 0 END) as running_count,
            SUM(CASE WHEN vehicleStatus = 'PARKED' THEN 1 ELSE 0 END) as parked_count,
            SUM(CASE WHEN vehicleStatus = 'IDLE' THEN 1 ELSE 0 END) as idle_count,
            SUM(CASE WHEN panic = 1 THEN 1 ELSE 0 END) as panic_count,
            MIN(timestamp) as first_record,
            MAX(timestamp) as last_record
        FROM vehicle_history 
        WHERE timestamp >= :startDate 
          AND timestamp <= :endDate
          AND (:adminId IS NULL OR admin_id = :adminId)
        GROUP BY DATE(timestamp), device_id
        ORDER BY report_date DESC, device_id
        """, nativeQuery = true)
    List<Object[]> findDailyFleetSummary(
        @Param("startDate") Timestamp startDate,
        @Param("endDate") Timestamp endDate,
        @Param("adminId") Long adminId
    );

    /**
     * 🗓️ MONTHLY SUMMARY: High-level monthly statistics
     */
    @Query(value = """
        SELECT 
            YEAR(timestamp) as year,
            MONTH(timestamp) as month,
            device_id,
            COUNT(*) as total_records,
            AVG(speed) as avg_speed,
            MAX(speed) as max_speed,
            SUM(CASE WHEN panic = 1 THEN 1 ELSE 0 END) as total_panic_alerts,
            COUNT(DISTINCT DATE(timestamp)) as active_days
        FROM vehicle_history 
        WHERE timestamp >= :startDate 
          AND timestamp <= :endDate
          AND (:adminId IS NULL OR admin_id = :adminId)
        GROUP BY YEAR(timestamp), MONTH(timestamp), device_id
        ORDER BY year DESC, month DESC, device_id
        """, nativeQuery = true)
    List<Object[]> findMonthlySummary(
        @Param("startDate") Timestamp startDate,
        @Param("endDate") Timestamp endDate,
        @Param("adminId") Long adminId
    );

    // ═══════════════════════════════════════════════════════════════════════════════════
    // 🛣️ ROUTE & DISTANCE REPORTS
    // ═══════════════════════════════════════════════════════════════════════════════════

    /**
     * 🗺️ ROUTE DATA: Get coordinates for route visualization
     */
    @Query(value = """
        SELECT 
            device_id,
            latitude,
            longitude,
            timestamp,
            speed,
            course
        FROM vehicle_history 
        WHERE timestamp >= :startDate 
          AND timestamp <= :endDate
          AND device_id = :deviceId
          AND latitude IS NOT NULL 
          AND longitude IS NOT NULL
        ORDER BY timestamp ASC
        """, nativeQuery = true)
    List<Object[]> findRouteData(
        @Param("deviceId") String deviceId,
        @Param("startDate") Timestamp startDate,
        @Param("endDate") Timestamp endDate
    );

    /**
     * 📍 GPS POINTS SUMMARY: Optimized for map plotting
     */
    @Query(value = """
        SELECT 
            latitude,
            longitude,
            timestamp,
            speed,
            vehicleStatus
        FROM vehicle_history 
        WHERE timestamp >= :startDate 
          AND timestamp <= :endDate
          AND device_id = :deviceId
          AND latitude BETWEEN :minLat AND :maxLat
          AND longitude BETWEEN :minLng AND :maxLng
        ORDER BY timestamp ASC
        """, nativeQuery = true)
    List<Object[]> findGpsPointsInBounds(
        @Param("deviceId") String deviceId,
        @Param("startDate") Timestamp startDate,
        @Param("endDate") Timestamp endDate,
        @Param("minLat") Double minLat,
        @Param("maxLat") Double maxLat,
        @Param("minLng") Double minLng,
        @Param("maxLng") Double maxLng
    );
}