package com.GpsTracker.Thinture.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.ZoneId;

/**
 * Timezone configuration for GPS device and server
 * Handles Dubai device time (UTC+4) to UTC conversion
 * 
 * Configuration in application.properties:
 * gps.timezone.device-timezone=Asia/Dubai
 * gps.timezone.server-timezone=UTC
 * gps.timezone.validation-tolerance-minutes=15
 * gps.timezone.max-past-hours=48
 */
@Component
@ConfigurationProperties(prefix = "gps.timezone")
public class TimezoneConfig {
    
    /**
     * Device timezone (where GPS devices are located)
     * Default: Asia/Dubai (UTC+4)
     */
    private String deviceTimezone = "Asia/Dubai";
    
    /**
     * Server timezone (where application runs)
     * Default: UTC
     */
    private String serverTimezone = "UTC";
    
    /**
     * Tolerance for timestamp validation in minutes
     * Allows timestamps this many minutes in the future
     * Default: 15 minutes
     */
    private int validationToleranceMinutes = 15;
    
    /**
     * Maximum age for timestamps in hours
     * Rejects timestamps older than this
     * Default: 48 hours
     */
    private int maxPastHours = 48;
    
    // ================================================================================================
    // GETTERS AND SETTERS
    // ================================================================================================
    
    public String getDeviceTimezone() {
        return deviceTimezone;
    }
    
    public void setDeviceTimezone(String deviceTimezone) {
        this.deviceTimezone = deviceTimezone;
    }
    
    public String getServerTimezone() {
        return serverTimezone;
    }
    
    public void setServerTimezone(String serverTimezone) {
        this.serverTimezone = serverTimezone;
    }
    
    public int getValidationToleranceMinutes() {
        return validationToleranceMinutes;
    }
    
    public void setValidationToleranceMinutes(int validationToleranceMinutes) {
        this.validationToleranceMinutes = validationToleranceMinutes;
    }
    
    public int getMaxPastHours() {
        return maxPastHours;
    }
    
    public void setMaxPastHours(int maxPastHours) {
        this.maxPastHours = maxPastHours;
    }
    
    // ================================================================================================
    // UTILITY METHODS
    // ================================================================================================
    
    /**
     * Gets device timezone as ZoneId
     * @return ZoneId for device timezone
     */
    public ZoneId getDeviceZoneId() {
        return ZoneId.of(deviceTimezone);
    }
    
    /**
     * Gets server timezone as ZoneId
     * @return ZoneId for server timezone
     */
    public ZoneId getServerZoneId() {
        return ZoneId.of(serverTimezone);
    }
    
    /**
     * Gets timezone offset between device and server in hours
     * @return offset in hours (positive if device is ahead of server)
     */
    public long getTimezoneOffsetHours() {
        ZoneId deviceZone = getDeviceZoneId();
        ZoneId serverZone = getServerZoneId();
        
        java.time.Instant now = java.time.Instant.now();
        
        long deviceOffset = deviceZone.getRules().getOffset(now).getTotalSeconds();
        long serverOffset = serverZone.getRules().getOffset(now).getTotalSeconds();
        
        return (deviceOffset - serverOffset) / 3600; // Convert seconds to hours
    }
    
    /**
     * Validates configuration
     * @return true if configuration is valid
     */
    public boolean isValid() {
        try {
            ZoneId.of(deviceTimezone);
            ZoneId.of(serverTimezone);
            return validationToleranceMinutes > 0 && maxPastHours > 0;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Gets configuration summary for logging
     * @return configuration summary string
     */
    public String getConfigSummary() {
        return String.format(
            "TimezoneConfig{device=%s, server=%s, tolerance=%dm, maxPast=%dh, offset=%dh}",
            deviceTimezone, serverTimezone, validationToleranceMinutes, 
            maxPastHours, getTimezoneOffsetHours()
        );
    }
    
    @Override
    public String toString() {
        return "TimezoneConfig{" +
                "deviceTimezone='" + deviceTimezone + '\'' +
                ", serverTimezone='" + serverTimezone + '\'' +
                ", validationToleranceMinutes=" + validationToleranceMinutes +
                ", maxPastHours=" + maxPastHours +
                '}';
    }
}