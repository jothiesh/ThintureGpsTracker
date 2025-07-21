// ================================================================================================
// Since your MySQL column is already DATETIME(6), the issue is in Java configuration
// Here's the focused fix:
// ================================================================================================

// ================================================================================================
// 1. UPDATE YOUR VehicleHistory.java - CHANGE ONLY THE TIMESTAMP ANNOTATION
// ================================================================================================

// ❌ CURRENT (causing conversion):

// ================================================================================================
// 2. COMPLETE FIXED VehicleHistory.java 
// ================================================================================================

package com.GpsTracker.Thinture.model;

import java.sql.Timestamp;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.GpsTracker.Thinture.config.RawTimestampDeserializer;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import java.util.Objects;

@Entity
@Table(
    name = "vehicle_history",
    indexes = {
        @Index(name = "idx_device_time", columnList = "device_id, timestamp"),
        @Index(name = "idx_device_status", columnList = "device_id, status"),
        @Index(name = "idx_admin_time", columnList = "admin_id, timestamp"),
        @Index(name = "idx_coordinates", columnList = "latitude, longitude"),
        @Index(name = "idx_imei", columnList = "imei"),
        @Index(name = "idx_panic_time", columnList = "panic, timestamp")
    }
)
public class VehicleHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
 
    @Column(name = "device_id")
    private String device_id;
    
    @Column(name = "superadmin_id")
    private Long superadmin_id;
    
    @Column(name = "admin_id")
    private Long admin_id;

    @Column(name = "client_id")
    private Long client_id;

    @Column(name = "user_id")
    private Long user_id;

    @Column(name = "driver_id")
    private Long driver_id;

    @Column(name = "dealer_id")
    private Long dealer_id;

    // ✅ FIXED: Raw timestamp storage - use DEFAULT_TIMEZONE instead of GMT+0
 // REPLACE WITH:
    
@Column(name = "timestamp")
@JsonDeserialize(using = RawTimestampDeserializer.class)
@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "")  // Empty timezone
private Timestamp timestamp;
    
    
    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "speed")
    private Double speed;
    
    @Column(name = "course")
    private String course;

    @Column(name = "additionalData")
    private String additionalData;

    @Column(name = "sequenceNumber")
    private String sequenceNumber;

    @Column(name = "ignition")
    private String ignition;

    @Column(name = "vehicleStatus")
    private String vehicleStatus;

    @Column(name = "status")
    private String status;

    @Column(name = "timeIntervals")
    private String timeIntervals;

    @Column(name = "distanceIntervals")
    private String distanceIntervals;

    @Column(name = "gsmStrength")
    private String gsmStrength;

    @Column(name = "panic")
    private Integer panic;

    @Column(name = "serialNo")
    private String serialNo;

    @Column(name = "imei")
    private String imei;

    @Column(name = "dealerName")
    private String dealerName;

    // ✅ Constructors
    public VehicleHistory() {}

    // ✅ BaseEntity method implementations
    @Override 
    public Long getId() { 
        return id; 
    }
    
    @Override 
    public void setId(Long id) { 
        this.id = id; 
    }

    @Override public Long getAdmin_id() { return admin_id; }
    @Override public void setAdmin_id(Long admin_id) { this.admin_id = admin_id; }

    @Override public Long getClient_id() { return client_id; }
    @Override public void setClient_id(Long client_id) { this.client_id = client_id; }

    @Override public Long getUser_id() { return user_id; }
    @Override public void setUser_id(Long user_id) { this.user_id = user_id; }

    @Override public Long getDriver_id() { return driver_id; }
    @Override public void setDriver_id(Long driver_id) { this.driver_id = driver_id; }

    @Override public Long getDealer_id() { return dealer_id; }
    @Override public void setDealer_id(Long dealer_id) { this.dealer_id = dealer_id; }

    @Override public Long getSuperadmin_id() { return superadmin_id; }
    @Override public void setSuperadmin_id(Long superadmin_id) { this.superadmin_id = superadmin_id; }

    // ✅ Device ID getter/setter
    public String getDevice_id() { return device_id; }
    public void setDevice_id(String device_id) { this.device_id = device_id; }

    // ✅ TRANSITION METHODS: To fix compilation errors in existing code
    @Deprecated
    public Vehicle getVehicle() { 
        return null; // Vehicle relationship removed - use getDevice_id() instead
    }
    
    @Deprecated
    public void setVehicle(Vehicle vehicle) { 
        if (vehicle != null && vehicle.getDeviceID() != null) {
            this.device_id = vehicle.getDeviceID();
        }
    }
    
    // ✅ Helper method to get device ID (alternative naming)
    public String getDeviceId() { return device_id; }
    public void setDeviceId(String deviceId) { this.device_id = deviceId; }

    // ✅ Timestamp getter/setter
    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }

    // ✅ Core data getters/setters
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public Double getSpeed() { return speed; }
    public void setSpeed(Double speed) { this.speed = speed; }

    public String getCourse() { return course; }
    public void setCourse(String course) { this.course = course; }

    public String getAdditionalData() { return additionalData; }
    public void setAdditionalData(String additionalData) { this.additionalData = additionalData; }

    public String getSequenceNumber() { return sequenceNumber; }
    public void setSequenceNumber(String sequenceNumber) { this.sequenceNumber = sequenceNumber; }

    public String getIgnition() { return ignition; }
    public void setIgnition(String ignition) { this.ignition = ignition; }

    public String getVehicleStatus() { return vehicleStatus; }
    public void setVehicleStatus(String vehicleStatus) { this.vehicleStatus = vehicleStatus; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getTimeIntervals() { return timeIntervals; }
    public void setTimeIntervals(String timeIntervals) { this.timeIntervals = timeIntervals; }

    public String getDistanceIntervals() { return distanceIntervals; }
    public void setDistanceIntervals(String distanceIntervals) { this.distanceIntervals = distanceIntervals; }

    public String getGsmStrength() { return gsmStrength; }
    public void setGsmStrength(String gsmStrength) { this.gsmStrength = gsmStrength; }

    public Integer getPanic() { return panic; }
    public void setPanic(Integer panic) { this.panic = panic; }

    public String getSerialNo() { return serialNo; }
    public void setSerialNo(String serialNo) { this.serialNo = serialNo; }

    public String getImei() { return imei; }
    public void setImei(String imei) { this.imei = imei; }

    public String getDealerName() { return dealerName; }
    public void setDealerName(String dealerName) { this.dealerName = dealerName; }

    // ✅ Utility methods
    public boolean isLiveRecord() {
        return "N1".equals(this.status);
    }

    public boolean isHistoryRecord() {
        return "N2".equals(this.status);
    }

    public boolean hasValidCoordinates() {
        return latitude != null && longitude != null && 
               latitude != 0.0 && longitude != 0.0;
    }

    public boolean isPanicActive() {
        return panic != null && panic == 1;
    }

    public boolean hasDevice() {
        return device_id != null && !device_id.trim().isEmpty();
    }
    
    public String getDeviceIdentifier() {
        return device_id;
    }
    
    public Double getLatitudeSafe() {
        return latitude != null ? latitude : 0.0;
    }
    
    public Double getLongitudeSafe() {
        return longitude != null ? longitude : 0.0;
    }
    
    public Double getSpeedSafe() {
        return speed != null ? speed : 0.0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        VehicleHistory that = (VehicleHistory) o;
        
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "VehicleHistory{" +
                "id=" + id +
                ", device_id='" + device_id + '\'' +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", speed=" + speed +
                ", status='" + status + '\'' +
                ", vehicleStatus='" + vehicleStatus + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}