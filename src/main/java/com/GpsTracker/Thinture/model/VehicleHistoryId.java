// ================================================================================================
// VehicleHistoryId.java - DEVICE-BASED COMPOSITE KEY CLASS
// ================================================================================================

package com.GpsTracker.Thinture.model;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * ✅ FIXED: Device-based composite key for preventing duplicate records
 * Uses device_id + timestamp as natural primary key
 */


@Embeddable
public class VehicleHistoryId implements Serializable {
    
    private static final long serialVersionUID = 1L;
    

    
    // ═══════════════════════════════════════════════════════════════════════════════
    // CONSTRUCTORS
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Default constructor (required by JPA)
     */
    public VehicleHistoryId() {
    }
    
    /**
     * Constructor with parameters
     * @param deviceId The device identifier
     * @param timestamp The record timestamp
     */
    public VehicleHistoryId(String deviceId, Timestamp timestamp) {
        this.deviceId = deviceId;
        this.timestamp = timestamp;
    }
    
    
    @Column(name = "device_id", nullable = false)
    private String deviceId;

    @Column(name = "timestamp", nullable = false)
    private Timestamp timestamp;
    /**
     * Copy constructor for safe cloning
     */
    public VehicleHistoryId(VehicleHistoryId other) {
        if (other != null) {
            this.deviceId = other.deviceId;
            this.timestamp = other.timestamp != null ? new Timestamp(other.timestamp.getTime()) : null;
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // GETTERS AND SETTERS
    // ═══════════════════════════════════════════════════════════════════════════════
    
    public String getDeviceId() {
        return deviceId;
    }
    
    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }
    
    public Timestamp getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // EQUALS AND HASHCODE FOR PROPER HIBERNATE FUNCTIONING
    // ═══════════════════════════════════════════════════════════════════════════════
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        VehicleHistoryId that = (VehicleHistoryId) o;
        
        // Compare device IDs
        if (!Objects.equals(deviceId, that.deviceId)) return false;
        
        // Compare timestamps by time value
        if (timestamp == null && that.timestamp == null) return true;
        if (timestamp == null || that.timestamp == null) return false;
        
        return timestamp.getTime() == that.timestamp.getTime();
    }
    
    @Override
    public int hashCode() {
        int result = deviceId != null ? deviceId.hashCode() : 0;
        result = 31 * result + (timestamp != null ? Long.hashCode(timestamp.getTime()) : 0);
        return result;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // VALIDATION AND UTILITY METHODS
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Check if the composite key is valid
     */
    public boolean isValid() {
        return deviceId != null && !deviceId.trim().isEmpty() && timestamp != null;
    }
    
    /**
     * Validate the composite key and throw exception if invalid
     */
    public void validate() {
        if (deviceId == null || deviceId.trim().isEmpty()) {
            throw new IllegalStateException("VehicleHistoryId.deviceId cannot be null or empty");
        }
        if (timestamp == null) {
            throw new IllegalStateException("VehicleHistoryId.timestamp cannot be null");
        }
    }
    
    /**
     * Create a safe copy of this ID
     */
    public VehicleHistoryId copy() {
        return new VehicleHistoryId(this);
    }
    
    /**
     * Compare with another ID (useful for sorting)
     */
    public int compareTo(VehicleHistoryId other) {
        if (other == null) return 1;
        
        // First compare by device ID
        if (deviceId != null && other.deviceId != null) {
            int deviceComparison = deviceId.compareTo(other.deviceId);
            if (deviceComparison != 0) return deviceComparison;
        }
        
        // Then compare by timestamp
        if (timestamp != null && other.timestamp != null) {
            return timestamp.compareTo(other.timestamp);
        }
        
        return 0;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // DEBUGGING SUPPORT
    // ═══════════════════════════════════════════════════════════════════════════════
    
    @Override
    public String toString() {
        return String.format("VehicleHistoryId{deviceId='%s', timestamp=%s, valid=%s}", 
                           deviceId, timestamp, isValid());
    }
    
    /**
     * Detailed debug information
     */
    public String toDebugString() {
        return String.format(
            "VehicleHistoryId{\n" +
            "  deviceId='%s' (hash: %s),\n" +
            "  timestamp=%s (time: %s),\n" +
            "  valid=%s,\n" +
            "  hashCode=%s\n" +
            "}",
            deviceId, 
            deviceId != null ? deviceId.hashCode() : "null",
            timestamp,
            timestamp != null ? timestamp.getTime() : "null",
            isValid(),
            hashCode()
        );
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // FACTORY METHODS
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Create a VehicleHistoryId with current timestamp
     */
    public static VehicleHistoryId createWithCurrentTime(String deviceId) {
        return new VehicleHistoryId(deviceId, new Timestamp(System.currentTimeMillis()));
    }
    
    /**
     * Create a VehicleHistoryId from epoch milliseconds
     */
    public static VehicleHistoryId create(String deviceId, long timestampMillis) {
        return new VehicleHistoryId(deviceId, new Timestamp(timestampMillis));
    }
    
    /**
     * Create a VehicleHistoryId from timestamp string
     */
    public static VehicleHistoryId create(String deviceId, String timestampStr) {
        try {
            Timestamp timestamp = Timestamp.valueOf(timestampStr);
            return new VehicleHistoryId(deviceId, timestamp);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid timestamp format: " + timestampStr, e);
        }
    }
}