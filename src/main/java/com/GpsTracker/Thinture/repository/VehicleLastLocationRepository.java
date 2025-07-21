package com.GpsTracker.Thinture.repository;

import com.GpsTracker.Thinture.model.VehicleLastLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleLastLocationRepository extends BaseRestrictedRepository<VehicleLastLocation, Long> {

    // 🔍 Basic Finders
    Optional<VehicleLastLocation> findByImei(String imei);
    Optional<VehicleLastLocation> findBySerialNo(String serialNo);
    Optional<VehicleLastLocation> findByImeiAndSerialNo(String imei, String serialNo);
    Optional<VehicleLastLocation> findByDeviceId(String deviceId);
    List<VehicleLastLocation> findAll();

    // 🔢 Count by status
    long countByStatus(String status);

    // ⏱️ Time-based Queries
    List<VehicleLastLocation> findByTimestampAfter(Timestamp timestamp);
    List<VehicleLastLocation> findByTimestampBefore(Timestamp timestamp);
    int deleteByTimestampBefore(Timestamp timestamp);

    // 🌍 Geospatial Query
    List<VehicleLastLocation> findByLatitudeBetweenAndLongitudeBetween(
        double minLat, double maxLat,
        double minLon, double maxLon
    );

    // 🔄 Latest by Device ID
    @Query("SELECT v FROM VehicleLastLocation v WHERE v.deviceId = :deviceId ORDER BY v.timestamp DESC")
    VehicleLastLocation findTopByDeviceIDOrderByTimestampDesc(@Param("deviceId") String deviceId);
}
