package com.GpsTracker.Thinture.repository;

import com.GpsTracker.Thinture.model.VehicleDailyKm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface DistanceKmCalcRepository extends JpaRepository<VehicleDailyKm, Long> {

    /**
     * Find all records for a device between start and end date.
     */
    @Query("""
        SELECT v FROM VehicleDailyKm v
        WHERE v.device_id = :deviceId
          AND v.date BETWEEN :startDate AND :endDate
        ORDER BY v.date ASC
        """)
    List<VehicleDailyKm> findByDeviceIdAndDateRange(
        @Param("deviceId") String deviceId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * Find all records for a single day.
     */
    @Query("""
        SELECT v FROM VehicleDailyKm v
        WHERE v.device_id = :deviceId
          AND v.date = :date
        ORDER BY v.timeSlot ASC
        """)
    List<VehicleDailyKm> findByDeviceIdAndDate(
        @Param("deviceId") String deviceId,
        @Param("date") LocalDate date
    );

    /**
     * Get all records for a given date.
     */
    @Query("""
        SELECT v FROM VehicleDailyKm v
        WHERE v.date = :date
        ORDER BY v.device_id, v.timeSlot ASC
        """)
    List<VehicleDailyKm> findAllByDate(
        @Param("date") LocalDate date
    );

    /**
     * Count records for device in date range.
     */
    @Query("""
        SELECT COUNT(v) FROM VehicleDailyKm v
        WHERE v.device_id = :deviceId
          AND v.date BETWEEN :startDate AND :endDate
        """)
    long countRecordsInRange(
        @Param("deviceId") String deviceId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

}
