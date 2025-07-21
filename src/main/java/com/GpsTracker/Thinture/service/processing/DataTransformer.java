package com.GpsTracker.Thinture.service.processing;

import com.GpsTracker.Thinture.dto.LocationUpdate;
import com.GpsTracker.Thinture.model.GpsData;
import com.GpsTracker.Thinture.model.Vehicle;
import com.GpsTracker.Thinture.model.VehicleHistory;
import com.GpsTracker.Thinture.model.VehicleLastLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 🌟 ENHANCED Data Transformer - Production Ready for 5000+ Devices
 * ✅ Colorful, structured logging with emojis
 * ✅ RAW timestamp storage (no timezone conversion)
 * ✅ Enhanced ignition status handling
 * ✅ Performance monitoring and metrics
 * ✅ Advanced data validation and cleaning
 * ✅ GPS coordinate processing and validation
 * ✅ Comprehensive error handling
 * 
 * Handles all data conversion for the GPS tracking system
 * Store exactly what device sends - no timezone conversion
 */
@Component
public class DataTransformer {
    
    private static final Logger logger = LoggerFactory.getLogger(DataTransformer.class);
    private static final Logger performanceLogger = LoggerFactory.getLogger("PERFORMANCE");
    private static final Logger transformationLogger = LoggerFactory.getLogger("TRANSFORMATION");
    
    // 🎨 ANSI Color codes for beautiful console output
    private static final String RESET = "\u001B[0m";
    private static final String RED = "\u001B[31m";
    private static final String GREEN = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String BLUE = "\u001B[34m";
    private static final String PURPLE = "\u001B[35m";
    private static final String CYAN = "\u001B[36m";
    private static final String WHITE = "\u001B[37m";
    private static final String BRIGHT_RED = "\u001B[91m";
    private static final String BRIGHT_GREEN = "\u001B[92m";
    private static final String BRIGHT_YELLOW = "\u001B[93m";
    private static final String BRIGHT_BLUE = "\u001B[94m";
    private static final String BRIGHT_MAGENTA = "\u001B[95m";
    private static final String BRIGHT_CYAN = "\u001B[96m";
    private static final String BRIGHT_WHITE = "\u001B[97m";
    
    // 📊 Date/Time formatting
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    
    // ✅ Enhanced ignition status sets
    private static final Set<String> IGNITION_ON_VALUES = Set.of(
        "1", "ON", "TRUE", "IGON", "IG_ON", "IGNITION_ON", "ENGINE_ON", "STARTED"
    );
    
    private static final Set<String> IGNITION_OFF_VALUES = Set.of(
        "0", "OFF", "FALSE", "IGOFF", "IG_OFF", "IGNITION_OFF", "ENGINE_OFF", "STOPPED"
    );
    
    // 📊 Performance metrics
    private final AtomicLong totalTransformations = new AtomicLong(0);
    private final AtomicLong successfulTransformations = new AtomicLong(0);
    private final AtomicLong failedTransformations = new AtomicLong(0);
    private final AtomicLong ignitionNormalizations = new AtomicLong(0);
    private final AtomicLong hexConversions = new AtomicLong(0);
    private final AtomicLong timestampFixedCount = new AtomicLong(0);
    
    // 📊 Performance tracking
    private final Map<String, AtomicLong> transformationCounts = new ConcurrentHashMap<>();
    private final Map<String, Long> transformationTimes = new ConcurrentHashMap<>();
    
    @PostConstruct
    public void initialize() {
        logColorful("🌟 INITIALIZING ENHANCED DATA TRANSFORMER", BRIGHT_CYAN);
        logColorful("⏱️ Startup time: " + LocalDateTime.now().format(DISPLAY_FORMATTER), CYAN);
        
        // 📊 Display configuration
        displayConfiguration();
        
        logColorful("✅ Enhanced Data Transformer initialized successfully", BRIGHT_GREEN);
        displayWelcomeDashboard();
    }
    
    /**
     * 📊 Display transformer configuration
     */
    private void displayConfiguration() {
        logColorful("", "");
        logColorful("╔════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════", BRIGHT_BLUE);
        logColorful("║ ⚙️ DATA TRANSFORMER CONFIGURATION", BRIGHT_BLUE);
        logColorful("╠════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════", BRIGHT_BLUE);
        logColorful("║ 📅 Timestamp Format: yyyy-MM-dd HH:mm:ss (RAW)", BLUE);
        logColorful("║ 🔧 Ignition ON Values: " + IGNITION_ON_VALUES.size() + " variants", BLUE);
        logColorful("║ 🔧 Ignition OFF Values: " + IGNITION_OFF_VALUES.size() + " variants", BLUE);
        logColorful("║ 📊 Storage Mode: RAW (no timezone conversion)", BLUE);
        logColorful("║ 🎯 Target Capacity: 5000+ devices", BLUE);
        logColorful("║ 🏗️ Architecture: Enhanced Multi-Layer", BLUE);
        logColorful("╚════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════", BRIGHT_BLUE);
    }
    
    /**
     * 🎨 Display welcome dashboard
     */
    private void displayWelcomeDashboard() {
        logColorful("", "");
        logColorful("╔════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════", BRIGHT_MAGENTA);
        logColorful("║ 🎯 ENHANCED DATA TRANSFORMER DASHBOARD", BRIGHT_MAGENTA);
        logColorful("║ 🚀 Ready to transform GPS data for 5000+ devices", BRIGHT_MAGENTA);
        logColorful("╠════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════", BRIGHT_MAGENTA);
        logColorful("║ 🔄 GpsData → VehicleHistory: ACTIVE", BRIGHT_GREEN);
        logColorful("║ 🔄 GpsData → VehicleLastLocation: ACTIVE", BRIGHT_GREEN);
        logColorful("║ 🔄 GpsData → LocationUpdate: ACTIVE", BRIGHT_GREEN);
        logColorful("║ 🔧 Ignition Normalization: ACTIVE", BRIGHT_GREEN);
        logColorful("║ 🧹 Data Cleaning: ACTIVE", BRIGHT_GREEN);
        logColorful("║ 📅 RAW Timestamp Storage: ACTIVE", BRIGHT_GREEN);
        logColorful("╚════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════", BRIGHT_MAGENTA);
    }
    
    /**
     * 🔄 ENHANCED: Transform GpsData to VehicleHistory with colorful logging
     */
    public VehicleHistory toVehicleHistory(GpsData gpsData, Vehicle vehicle) {
        long startTime = System.currentTimeMillis();
        String deviceId = gpsData != null ? gpsData.getDeviceID() : "unknown";
        
        try {
            totalTransformations.incrementAndGet();
            transformationCounts.computeIfAbsent("VehicleHistory", k -> new AtomicLong(0)).incrementAndGet();
            
            logColorful("🔄 TRANSFORMING GPS DATA TO VEHICLE HISTORY", BRIGHT_BLUE);
            logColorful("📱 Device ID: " + deviceId, BLUE);
            
            // 🔍 Validation
            if (gpsData == null || vehicle == null) {
                logColorful("🚫 INVALID INPUT: gpsData=" + (gpsData != null) + ", vehicle=" + (vehicle != null), BRIGHT_RED);
                failedTransformations.incrementAndGet();
                throw new IllegalArgumentException("GpsData and Vehicle cannot be null");
            }
            
            logColorful("✅ Input validation passed", GREEN);
            
            // 🏗️ Create history entity
            VehicleHistory history = new VehicleHistory();
            
            // 🔗 Set vehicle reference
            history.setVehicle(vehicle);
            logColorful("🔗 Vehicle reference set: " + vehicle.getVehicleNumber(), CYAN);
            
            // 📊 Set basic fields with enhanced logging
            logColorful("📊 SETTING BASIC FIELDS", BLUE);
            
            // 📅 Timestamp handling
            Timestamp timestamp = parseRawTimestamp(gpsData.getTimestamp());
            history.setTimestamp(timestamp);
            logColorful("📅 Timestamp set: " + timestamp, CYAN);
            
            // 📱 Device info
            history.setDevice_id(gpsData.getDeviceID());
            history.setImei(gpsData.getImei());
            logColorful("📱 Device info set - ID: " + gpsData.getDeviceID() + ", IMEI: " + gpsData.getImei(), CYAN);
            
            // 📍 Location data
            double latitude = parseDouble(gpsData.getLatitude());
            double longitude = parseDouble(gpsData.getLongitude());
            history.setLatitude(latitude);
            history.setLongitude(longitude);
            logColorful("📍 Location set: " + String.format("%.6f", latitude) + ", " + String.format("%.6f", longitude), CYAN);
            
            // 🏃 Movement data
            Double speed = parseDoubleOrNull(gpsData.getSpeed());
            history.setSpeed(speed);
            history.setCourse(gpsData.getCourse());
            history.setSequenceNumber(gpsData.getSequenceNumber());
            logColorful("🏃 Movement data set - Speed: " + speed + ", Course: " + gpsData.getCourse(), CYAN);
            
            // 🔧 Enhanced ignition handling
            String originalIgnition = gpsData.getIgnition();
            String normalizedIgnition = normalizeIgnition(originalIgnition);
            history.setIgnition(normalizedIgnition);
            
            if (!Objects.equals(originalIgnition, normalizedIgnition)) {
                ignitionNormalizations.incrementAndGet();
                logColorful("🔧 IGNITION NORMALIZED: " + originalIgnition + " → " + normalizedIgnition, BRIGHT_YELLOW);
            } else {
                logColorful("🔧 Ignition status: " + normalizedIgnition, CYAN);
            }
            
            // 📊 Status fields
            history.setVehicleStatus(gpsData.getVehicleStatus());
            history.setStatus(gpsData.getStatus());
            logColorful("📊 Status fields set", CYAN);
            
            // 📡 Communication data
            history.setGsmStrength(gpsData.getGsmStrength());
            history.setTimeIntervals(gpsData.getTimeIntervals());
            logColorful("📡 Communication data set", CYAN);
            
            // 🔧 Additional data processing
            if (gpsData.getAdditionalData() != null && !gpsData.getAdditionalData().isEmpty()) {
                String processedData = processAdditionalData(gpsData.getAdditionalData());
                history.setAdditionalData(processedData);
                logColorful("🔧 Additional data processed: " + gpsData.getAdditionalData() + " → " + processedData, BRIGHT_CYAN);
            } else {
                logColorful("📋 No additional data to process", CYAN);
            }
            
            // 📊 Performance tracking
            long duration = System.currentTimeMillis() - startTime;
            transformationTimes.put("VehicleHistory-" + deviceId, duration);
            successfulTransformations.incrementAndGet();
            
            logColorful("✅ VEHICLE HISTORY TRANSFORMATION COMPLETE", BRIGHT_GREEN);
            logColorful("⏱️ Processing time: " + duration + "ms", GREEN);
            transformationLogger.debug("VehicleHistory transformation completed for device: {} in {}ms", deviceId, duration);
            
            return history;
            
        } catch (Exception e) {
            failedTransformations.incrementAndGet();
            logColorful("❌ VEHICLE HISTORY TRANSFORMATION FAILED", BRIGHT_RED);
            logColorful("💥 Error: " + e.getMessage(), RED);
            logger.error("Failed to transform GpsData to VehicleHistory for device: {}", deviceId, e);
            throw e;
        }
    }
    
    /**
     * 🔄 ENHANCED: Transform GpsData to VehicleLastLocation with colorful logging
     */
    public VehicleLastLocation toVehicleLastLocation(GpsData gpsData) {
        long startTime = System.currentTimeMillis();
        String deviceId = gpsData != null ? gpsData.getDeviceID() : "unknown";
        
        try {
            totalTransformations.incrementAndGet();
            transformationCounts.computeIfAbsent("VehicleLastLocation", k -> new AtomicLong(0)).incrementAndGet();
            
            logColorful("🔄 TRANSFORMING GPS DATA TO VEHICLE LAST LOCATION", BRIGHT_BLUE);
            logColorful("📱 Device ID: " + deviceId, BLUE);
            
            // 🔍 Validation
            if (gpsData == null) {
                logColorful("🚫 GPS DATA IS NULL", BRIGHT_RED);
                failedTransformations.incrementAndGet();
                throw new IllegalArgumentException("GpsData cannot be null");
            }
            
            logColorful("✅ Input validation passed", GREEN);
            
            // 🏗️ Create last location entity
            VehicleLastLocation location = new VehicleLastLocation();
            
            // 📱 Set identifiers
            location.setDeviceId(gpsData.getDeviceID());
            location.setImei(gpsData.getImei());
            logColorful("📱 Identifiers set - Device: " + gpsData.getDeviceID() + ", IMEI: " + gpsData.getImei(), CYAN);
            
            // 📍 Set location data
            double latitude = parseDouble(gpsData.getLatitude());
            double longitude = parseDouble(gpsData.getLongitude());
            location.setLatitude(latitude);
            location.setLongitude(longitude);
            logColorful("📍 Location set: " + String.format("%.6f", latitude) + ", " + String.format("%.6f", longitude), CYAN);
            
            // 📅 Set timestamp (RAW)
            Timestamp timestamp = parseRawTimestamp(gpsData.getTimestamp());
            location.setTimestamp(timestamp);
            logColorful("📅 RAW timestamp set: " + timestamp, CYAN);
            
            // 🏃 Movement data
            location.setSpeed(gpsData.getSpeed());
            location.setCourse(gpsData.getCourse());
            logColorful("🏃 Movement data set - Speed: " + gpsData.getSpeed() + ", Course: " + gpsData.getCourse(), CYAN);
            
            // 📊 Status data
            location.setStatus(gpsData.getStatus());
            String normalizedIgnition = normalizeIgnition(gpsData.getIgnition());
            location.setIgnition(normalizedIgnition);
            location.setVehicleStatus(gpsData.getVehicleStatus());
            location.setGsmStrength(gpsData.getGsmStrength());
            logColorful("📊 Status data set - Ignition: " + normalizedIgnition, CYAN);
            
            // 📡 Additional fields
            location.setTimeIntervals(gpsData.getTimeIntervals());
            logColorful("📡 Additional fields set", CYAN);
            
            // 📊 Performance tracking
            long duration = System.currentTimeMillis() - startTime;
            transformationTimes.put("VehicleLastLocation-" + deviceId, duration);
            successfulTransformations.incrementAndGet();
            
            logColorful("✅ VEHICLE LAST LOCATION TRANSFORMATION COMPLETE", BRIGHT_GREEN);
            logColorful("⏱️ Processing time: " + duration + "ms", GREEN);
            transformationLogger.debug("VehicleLastLocation transformation completed for device: {} in {}ms", deviceId, duration);
            
            return location;
            
        } catch (Exception e) {
            failedTransformations.incrementAndGet();
            logColorful("❌ VEHICLE LAST LOCATION TRANSFORMATION FAILED", BRIGHT_RED);
            logColorful("💥 Error: " + e.getMessage(), RED);
            logger.error("Failed to transform GpsData to VehicleLastLocation for device: {}", deviceId, e);
            throw e;
        }
    }
    
    /**
     * 🔄 ENHANCED: Transform GpsData to LocationUpdate with colorful logging
     */
    public LocationUpdate toLocationUpdate(GpsData gpsData) {
        long startTime = System.currentTimeMillis();
        String deviceId = gpsData != null ? gpsData.getDeviceID() : "unknown";
        
        try {
            totalTransformations.incrementAndGet();
            transformationCounts.computeIfAbsent("LocationUpdate", k -> new AtomicLong(0)).incrementAndGet();
            
            logColorful("🔄 TRANSFORMING GPS DATA TO LOCATION UPDATE", BRIGHT_BLUE);
            logColorful("📱 Device ID: " + deviceId, BLUE);
            
            // 🔍 Validation
            if (gpsData == null) {
                logColorful("🚫 GPS DATA IS NULL", BRIGHT_RED);
                failedTransformations.incrementAndGet();
                throw new IllegalArgumentException("GpsData cannot be null");
            }
            
            logColorful("✅ Input validation passed", GREEN);
            
            // 🏗️ Create location update
            LocationUpdate update = new LocationUpdate(
                parseDouble(gpsData.getLatitude()),
                parseDouble(gpsData.getLongitude()),
                gpsData.getDeviceID(),
                gpsData.getTimestamp(), // Use RAW timestamp string
                gpsData.getSpeed(),
                normalizeIgnition(gpsData.getIgnition()),
                gpsData.getCourse(),
                gpsData.getVehicleStatus(),
                gpsData.getGsmStrength(),
                gpsData.getAdditionalData() != null ? gpsData.getAdditionalData() : "",
                gpsData.getTimeIntervals() != null ? gpsData.getTimeIntervals() : ""
            );
            
            // 📊 Performance tracking
            long duration = System.currentTimeMillis() - startTime;
            transformationTimes.put("LocationUpdate-" + deviceId, duration);
            successfulTransformations.incrementAndGet();
            
            logColorful("✅ LOCATION UPDATE TRANSFORMATION COMPLETE", BRIGHT_GREEN);
            logColorful("⏱️ Processing time: " + duration + "ms", GREEN);
            transformationLogger.debug("LocationUpdate transformation completed for device: {} in {}ms", deviceId, duration);
            
            return update;
            
        } catch (Exception e) {
            failedTransformations.incrementAndGet();
            logColorful("❌ LOCATION UPDATE TRANSFORMATION FAILED", BRIGHT_RED);
            logColorful("💥 Error: " + e.getMessage(), RED);
            logger.error("Failed to transform GpsData to LocationUpdate for device: {}", deviceId, e);
            throw e;
        }
    }
    
    /**
     * 🔄 Transform VehicleLastLocation to LocationUpdate
     */
    public LocationUpdate toLocationUpdate(VehicleLastLocation lastLocation) {
        long startTime = System.currentTimeMillis();
        String deviceId = lastLocation != null ? lastLocation.getDeviceId() : "unknown";
        
        try {
            totalTransformations.incrementAndGet();
            transformationCounts.computeIfAbsent("LocationUpdate-FromLastLocation", k -> new AtomicLong(0)).incrementAndGet();
            
            logColorful("🔄 TRANSFORMING VEHICLE LAST LOCATION TO LOCATION UPDATE", BRIGHT_BLUE);
            logColorful("📱 Device ID: " + deviceId, BLUE);
            
            // 🔍 Validation
            if (lastLocation == null) {
                logColorful("🚫 VEHICLE LAST LOCATION IS NULL", BRIGHT_RED);
                failedTransformations.incrementAndGet();
                throw new IllegalArgumentException("VehicleLastLocation cannot be null");
            }
            
            logColorful("✅ Input validation passed", GREEN);
            
            // 🏗️ Create location update
            LocationUpdate update = new LocationUpdate(
                lastLocation.getLatitude(),
                lastLocation.getLongitude(),
                lastLocation.getDeviceId(),
                lastLocation.getTimestamp().toString(),
                lastLocation.getSpeed(),
                lastLocation.getIgnition(),
                lastLocation.getCourse(),
                lastLocation.getVehicleStatus(),
                lastLocation.getGsmStrength(),
                lastLocation.getAdditionalData() != null ? lastLocation.getAdditionalData() : "",
                lastLocation.getTimeIntervals() != null ? lastLocation.getTimeIntervals() : ""
            );
            
            // 📊 Performance tracking
            long duration = System.currentTimeMillis() - startTime;
            transformationTimes.put("LocationUpdate-FromLastLocation-" + deviceId, duration);
            successfulTransformations.incrementAndGet();
            
            logColorful("✅ LOCATION UPDATE FROM LAST LOCATION TRANSFORMATION COMPLETE", BRIGHT_GREEN);
            logColorful("⏱️ Processing time: " + duration + "ms", GREEN);
            
            return update;
            
        } catch (Exception e) {
            failedTransformations.incrementAndGet();
            logColorful("❌ LOCATION UPDATE FROM LAST LOCATION TRANSFORMATION FAILED", BRIGHT_RED);
            logColorful("💥 Error: " + e.getMessage(), RED);
            logger.error("Failed to transform VehicleLastLocation to LocationUpdate for device: {}", deviceId, e);
            throw e;
        }
    }
    
    /**
     * 📅 ENHANCED: Parse RAW timestamp with colorful logging
     */
    private Timestamp parseRawTimestamp(String timestampStr) {
        try {
            if (timestampStr == null || timestampStr.trim().isEmpty()) {
                logColorful("📅 NULL/EMPTY TIMESTAMP - using current time", BRIGHT_YELLOW);
                timestampFixedCount.incrementAndGet();
                return new Timestamp(System.currentTimeMillis());
            }
            
            logColorful("📅 PARSING RAW TIMESTAMP: " + timestampStr, BLUE);
            
            // 🔍 Parse timestamp exactly as received - NO timezone conversion
            LocalDateTime rawDateTime = LocalDateTime.parse(timestampStr, TIMESTAMP_FORMATTER);
            Timestamp result = Timestamp.valueOf(rawDateTime);
            
            logColorful("✅ RAW timestamp parsed successfully: " + result, GREEN);
            transformationLogger.debug("RAW timestamp stored: Input={} → Stored={} (NO CONVERSION)", timestampStr, result);
            
            return result;
            
        } catch (DateTimeParseException e) {
            logColorful("❌ TIMESTAMP PARSING FAILED: " + timestampStr, BRIGHT_RED);
            logColorful("💥 Error: " + e.getMessage(), RED);
            logColorful("🔧 Using current time as fallback", BRIGHT_YELLOW);
            
            timestampFixedCount.incrementAndGet();
            logger.error("Failed to parse RAW timestamp: {} - Error: {}", timestampStr, e.getMessage());
            return new Timestamp(System.currentTimeMillis()); // Fallback to current time
        }
    }
    
    /**
     * 🧹 ENHANCED: Clean and normalize GPS payload
     */
    public String cleanPayload(String payload) {
        long startTime = System.currentTimeMillis();
        
        try {
            logColorful("🧹 CLEANING PAYLOAD", BLUE);
            
            if (payload == null) {
                logColorful("🧹 Payload is null, returning empty string", BRIGHT_YELLOW);
                return "";
            }
            
            int originalLength = payload.length();
            logColorful("📊 Original payload length: " + originalLength + " chars", CYAN);
            
            // 🔧 Remove non-ASCII characters
            String cleaned = payload.replaceAll("[^\\x00-\\x7F]", "");
            
            // 🔧 Trim whitespace
            cleaned = cleaned.trim();
            
            int cleanedLength = cleaned.length();
            logColorful("📊 Cleaned payload length: " + cleanedLength + " chars", CYAN);
            
            if (originalLength != cleanedLength) {
                logColorful("🧹 PAYLOAD CLEANED: " + originalLength + " → " + cleanedLength + " chars", BRIGHT_CYAN);
            } else {
                logColorful("✅ Payload was already clean", GREEN);
            }
            
            long duration = System.currentTimeMillis() - startTime;
            transformationLogger.debug("Payload cleaned: {} chars -> {} chars in {}ms", originalLength, cleanedLength, duration);
            
            return cleaned;
            
        } catch (Exception e) {
            logColorful("❌ PAYLOAD CLEANING FAILED", BRIGHT_RED);
            logColorful("💥 Error: " + e.getMessage(), RED);
            logger.error("Error cleaning payload", e);
            return payload != null ? payload : "";
        }
    }
    
    /**
     * 🔧 ENHANCED: Convert hexadecimal string to ASCII
     */
    public String hexToAscii(String hexStr) {
        long startTime = System.currentTimeMillis();
        
        try {
            logColorful("🔧 CONVERTING HEX TO ASCII", BLUE);
            
            if (hexStr == null || hexStr.isEmpty()) {
                logColorful("🔧 Hex string is null or empty", BRIGHT_YELLOW);
                return "";
            }
            
            logColorful("📊 Input hex length: " + hexStr.length() + " chars", CYAN);
            
            StringBuilder output = new StringBuilder();
            
            for (int i = 0; i < hexStr.length(); i += 2) {
                if (i + 1 < hexStr.length()) {
                    String str = hexStr.substring(i, i + 2);
                    output.append((char) Integer.parseInt(str, 16));
                }
            }
            
            String result = output.toString();
            hexConversions.incrementAndGet();
            
            long duration = System.currentTimeMillis() - startTime;
            logColorful("✅ HEX TO ASCII CONVERSION COMPLETE", BRIGHT_GREEN);
            logColorful("📊 Output length: " + result.length() + " chars", GREEN);
            logColorful("⏱️ Processing time: " + duration + "ms", GREEN);
            
            transformationLogger.debug("Hex to ASCII conversion: {} chars -> {} chars in {}ms", 
                hexStr.length(), result.length(), duration);
            
            return result;
            
        } catch (Exception e) {
            logColorful("❌ HEX TO ASCII CONVERSION FAILED", BRIGHT_RED);
            logColorful("💥 Error: " + e.getMessage(), RED);
            logger.error("Error converting hex to ASCII: {}", e.getMessage());
            return hexStr; // Return original if conversion fails
        }
    }
    
    /**
     * 🔍 ENHANCED: Check if string is hexadecimal
     */
    public boolean isHexadecimal(String str) {
        try {
            if (str == null || str.isEmpty()) {
                return false;
            }
            
            boolean isHex = str.matches("\\p{XDigit}+") && str.length() % 2 == 0;
            
            if (isHex) {
                logColorful("🔍 HEX PATTERN DETECTED: " + str.substring(0, Math.min(20, str.length())) + "...", BRIGHT_CYAN);
            }
            
            transformationLogger.debug("Hex check: {} chars -> {}", str.length(), isHex);
            return isHex;
            
        } catch (Exception e) {
            logColorful("❌ HEX CHECK FAILED", BRIGHT_RED);
            logger.error("Error checking hex pattern", e);
            return false;
        }
    }
    
    /**
     * 🔧 ENHANCED: Process additional data flags
     */
    private String processAdditionalData(String additionalData) {
        try {
            if (additionalData == null || additionalData.trim().isEmpty()) {
                return "";
            }
            
            logColorful("🔧 PROCESSING ADDITIONAL DATA: " + additionalData, BLUE);
            
            // 🔍 Try to parse as binary flags
            try {
                int value = Integer.parseInt(additionalData, 2);
                Map<String, Boolean> flags = decodeAdditionalDataFlags(value);
                
                String decoded = flags.entrySet().stream()
                    .filter(Map.Entry::getValue)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.joining(", "));
                
                logColorful("🔧 BINARY FLAGS DECODED: " + additionalData + " → " + decoded, BRIGHT_CYAN);
                transformationLogger.debug("Additional data decoded: {} -> {}", additionalData, decoded);
                
                return decoded;
                
            } catch (NumberFormatException e) {
                logColorful("⚠️ NOT BINARY FORMAT - treating as string: " + additionalData, YELLOW);
                transformationLogger.debug("Additional data not binary format: {}, treating as string", additionalData);
                return additionalData;
            }
            
        } catch (Exception e) {
            logColorful("❌ ADDITIONAL DATA PROCESSING FAILED", BRIGHT_RED);
            logColorful("💥 Error: " + e.getMessage(), RED);
            logger.error("Error processing additional data", e);
            return additionalData;
        }
    }
    
    /**
     * 🔧 Decode additional data flags using bitwise operations
     */
    private Map<String, Boolean> decodeAdditionalDataFlags(int additionalData) {
        Map<String, Boolean> flags = new HashMap<>();
        
        flags.put("Speed Crossed", (additionalData & 0b00000001) != 0);
        flags.put("Angle Change > 30°", (additionalData & 0b00000010) != 0);
        flags.put("Theft/Towing", (additionalData & 0b00000100) != 0);
        flags.put("Sharp Turning", (additionalData & 0b00001000) != 0);
        flags.put("Distance Change", (additionalData & 0b00010000) != 0);
        flags.put("Roaming", (additionalData & 0b00100000) != 0);
        flags.put("Harsh Acceleration", (additionalData & 0b01000000) != 0);
        flags.put("Harsh Breaking", (additionalData & 0b10000000) != 0);
        
        return flags;
    }
    
    /**
     * 🔧 ENHANCED: Normalize ignition status with comprehensive logging
     */
    private String normalizeIgnition(String ignition) {
        try {
            if (ignition == null || ignition.trim().isEmpty()) {
                logColorful("🔧 NULL/EMPTY IGNITION - defaulting to OFF", BRIGHT_YELLOW);
                return "OFF";
            }
            
            String normalized = ignition.trim().toUpperCase();
            String result;
            
            if (IGNITION_ON_VALUES.contains(normalized)) {
                result = "ON";
                logColorful("🔧 IGNITION NORMALIZED: " + ignition + " → ON", BRIGHT_GREEN);
            } else if (IGNITION_OFF_VALUES.contains(normalized)) {
                result = "OFF";
                logColorful("🔧 IGNITION NORMALIZED: " + ignition + " → OFF", BRIGHT_BLUE);
            } else {
                result = "OFF";
                logColorful("⚠️ UNKNOWN IGNITION STATUS: " + ignition + " → OFF (default)", BRIGHT_YELLOW);
                logger.warn("Unknown ignition status: {}, defaulting to OFF. Valid values: ON: {}, OFF: {}", 
                    ignition, IGNITION_ON_VALUES, IGNITION_OFF_VALUES);
            }
            
            transformationLogger.debug("Ignition normalized: {} -> {}", ignition, result);
            return result;
            
        } catch (Exception e) {
            logColorful("❌ IGNITION NORMALIZATION FAILED", BRIGHT_RED);
            logColorful("💥 Error: " + e.getMessage(), RED);
            logger.error("Error normalizing ignition status", e);
            return "OFF";
        }
    }
    
    /**
     * 📊 Parse string to double with logging
     */
    private double parseDouble(String value) {
        try {
            if (value == null || value.trim().isEmpty()) {
                logColorful("📊 NULL/EMPTY DOUBLE VALUE - returning 0.0", BRIGHT_YELLOW);
                return 0.0;
            }
            
            double result = Double.parseDouble(value.trim());
            transformationLogger.debug("Double parsed: {} -> {}", value, result);
            return result;
            
        } catch (NumberFormatException e) {
            logColorful("❌ DOUBLE PARSING FAILED: " + value, BRIGHT_RED);
            logColorful("💥 Error: " + e.getMessage(), RED);
            logger.error("Error parsing double: {} - {}", value, e.getMessage());
            return 0.0;
        }
    }
    
    /**
     * 📊 Parse string to Double or return null
     */
    private Double parseDoubleOrNull(String value) {
        try {
            if (value == null || value.trim().isEmpty()) {
                return null;
            }
            
            Double result = Double.parseDouble(value.trim());
            transformationLogger.debug("Optional double parsed: {} -> {}", value, result);
            return result;
            
        } catch (NumberFormatException e) {
            logColorful("❌ OPTIONAL DOUBLE PARSING FAILED: " + value, BRIGHT_RED);
            logger.error("Error parsing optional double: {} - {}", value, e.getMessage());
            return null;
        }
    }
    
    /**
     * 📊 Format coordinates to standard decimal places
     */
    public String formatCoordinate(double coordinate, int decimalPlaces) {
        try {
            String formatted = String.format("%." + decimalPlaces + "f", coordinate);
            transformationLogger.debug("Coordinate formatted: {} -> {} ({} decimals)", coordinate, formatted, decimalPlaces);
            return formatted;
        } catch (Exception e) {
            logColorful("❌ COORDINATE FORMATTING FAILED", BRIGHT_RED);
            logger.error("Error formatting coordinate", e);
            return String.valueOf(coordinate);
        }
    }
    
    /**
     * 📏 Calculate distance between two GPS points (Haversine formula)
     */
    public double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        try {
            final int R = 6371; // Earth's radius in kilometers
            
            double latDistance = Math.toRadians(lat2 - lat1);
            double lonDistance = Math.toRadians(lon2 - lon1);
            
            double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                    + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                    * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
            
            double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
            double distance = R * c; // Distance in kilometers
            
            logColorful("📏 DISTANCE CALCULATED: " + String.format("%.3f", distance) + " km", BRIGHT_CYAN);
            transformationLogger.debug("Distance calculated: ({},{}) to ({},{}) = {} km", 
                lat1, lon1, lat2, lon2, String.format("%.3f", distance));
            
            return distance;
            
        } catch (Exception e) {
            logColorful("❌ DISTANCE CALCULATION FAILED", BRIGHT_RED);
            logger.error("Error calculating distance", e);
            return 0.0;
        }
    }
    
    /**
     * 🏃 Convert speed between units
     */
    public double convertSpeed(double speed, SpeedUnit from, SpeedUnit to) {
        try {
            // First convert to m/s
            double speedInMps = switch (from) {
                case KMH -> speed / 3.6;
                case MPH -> speed / 2.237;
                case MPS -> speed;
            };
            
            // Then convert to target unit
            double result = switch (to) {
                case KMH -> speedInMps * 3.6;
                case MPH -> speedInMps * 2.237;
                case MPS -> speedInMps;
            };
            
            logColorful("🏃 SPEED CONVERTED: " + speed + " " + from + " → " + String.format("%.2f", result) + " " + to, BRIGHT_CYAN);
            transformationLogger.debug("Speed converted: {} {} -> {} {}", speed, from, String.format("%.2f", result), to);
            
            return result;
            
        } catch (Exception e) {
            logColorful("❌ SPEED CONVERSION FAILED", BRIGHT_RED);
            logger.error("Error converting speed", e);
            return speed;
        }
    }
    
    /**
     * 📊 Get transformation metrics with dashboard display
     */
    public TransformationMetrics getMetrics() {
        TransformationMetrics metrics = new TransformationMetrics(
            totalTransformations.get(),
            successfulTransformations.get(),
            failedTransformations.get(),
            ignitionNormalizations.get(),
            hexConversions.get(),
            timestampFixedCount.get(),
            new HashMap<>(transformationCounts.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get())))
        );
        
        // 📊 Display metrics dashboard
        displayMetricsDashboard(metrics);
        
        return metrics;
    }
    
    /**
     * 📊 Display metrics dashboard
     */
    private void displayMetricsDashboard(TransformationMetrics metrics) {
        logColorful("", "");
        logColorful("╔════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════", BRIGHT_CYAN);
        logColorful("║ 📊 DATA TRANSFORMATION METRICS DASHBOARD", BRIGHT_CYAN);
        logColorful("║ 🕒 " + LocalDateTime.now().format(DISPLAY_FORMATTER), BRIGHT_CYAN);
        logColorful("╠════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════", BRIGHT_CYAN);
        logColorful("║ 📈 Total Transformations: " + metrics.getTotalTransformations(), BLUE);
        logColorful("║ ✅ Successful: " + metrics.getSuccessfulTransformations(), BRIGHT_GREEN);
        logColorful("║ ❌ Failed: " + metrics.getFailedTransformations(), metrics.getFailedTransformations() > 0 ? BRIGHT_RED : GREEN);
        logColorful("║ 🔧 Ignition Normalizations: " + metrics.getIgnitionNormalizations(), BRIGHT_YELLOW);
        logColorful("║ 🔄 Hex Conversions: " + metrics.getHexConversions(), BRIGHT_CYAN);
        logColorful("║ 📅 Timestamp Fixes: " + metrics.getTimestampFixed(), BRIGHT_MAGENTA);
        logColorful("║ 📊 Success Rate: " + String.format("%.2f%%", metrics.getSuccessRate()), 
                   metrics.getSuccessRate() > 95 ? BRIGHT_GREEN : BRIGHT_YELLOW);
        
        // 📊 Transformation type breakdown
        if (!metrics.getTransformationCounts().isEmpty()) {
            logColorful("║ 📋 TRANSFORMATION BREAKDOWN:", BRIGHT_BLUE);
            metrics.getTransformationCounts().forEach((type, count) -> 
                logColorful("║   🔄 " + type + ": " + count, CYAN));
        }
        
        logColorful("╚════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════", BRIGHT_CYAN);
    }
    
    /**
     * 📋 Get all valid ignition values for documentation
     */
    public Map<String, Set<String>> getValidIgnitionValues() {
        Map<String, Set<String>> validValues = new HashMap<>();
        validValues.put("ON", new HashSet<>(IGNITION_ON_VALUES));
        validValues.put("OFF", new HashSet<>(IGNITION_OFF_VALUES));
        
        logColorful("📋 IGNITION VALUES REFERENCE:", BRIGHT_BLUE);
        logColorful("🟢 ON Values: " + IGNITION_ON_VALUES, BRIGHT_GREEN);
        logColorful("🔴 OFF Values: " + IGNITION_OFF_VALUES, BRIGHT_RED);
        
        return validValues;
    }
    
    /**
     * 🎯 Validate GPS coordinates
     */
    public boolean isValidGpsCoordinate(double latitude, double longitude) {
        boolean validLat = latitude >= -90.0 && latitude <= 90.0;
        boolean validLon = longitude >= -180.0 && longitude <= 180.0;
        boolean valid = validLat && validLon;
        
        if (!valid) {
            logColorful("❌ INVALID GPS COORDINATES: " + 
                       String.format("lat=%.6f, lon=%.6f", latitude, longitude), BRIGHT_RED);
        } else {
            logColorful("✅ Valid GPS coordinates: " + 
                       String.format("lat=%.6f, lon=%.6f", latitude, longitude), GREEN);
        }
        
        return valid;
    }
    
    /**
     * 🔍 Validate IMEI format
     */
    public boolean isValidImei(String imei) {
        if (imei == null || imei.trim().isEmpty()) {
            logColorful("❌ IMEI is null or empty", BRIGHT_RED);
            return false;
        }
        
        String cleanImei = imei.trim();
        
        // IMEI should be 15 digits
        if (cleanImei.length() != 15) {
            logColorful("❌ IMEI length invalid: " + cleanImei.length() + " (expected 15)", BRIGHT_RED);
            return false;
        }
        
        // Should contain only digits
        boolean isValid = cleanImei.matches("\\d{15}");
        
        if (isValid) {
            logColorful("✅ Valid IMEI: " + cleanImei, GREEN);
        } else {
            logColorful("❌ IMEI contains non-digit characters: " + cleanImei, BRIGHT_RED);
        }
        
        return isValid;
    }
    
    /**
     * 🚀 Comprehensive data validation and transformation
     */
    public ValidationResult validateAndTransform(GpsData gpsData) {
        List<String> issues = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        logColorful("🔍 COMPREHENSIVE DATA VALIDATION", BRIGHT_BLUE);

        if (gpsData == null) {
            issues.add("GPS data is null");
            return new ValidationResult(false, issues, warnings);
        }

        // 📱 Validate IMEI
        if (!isValidImei(gpsData.getImei())) {
            issues.add("Invalid IMEI: " + gpsData.getImei());
        }

        // 📍 Validate coordinates
        try {
            double lat = parseDouble(gpsData.getLatitude());
            double lon = parseDouble(gpsData.getLongitude());

            if (!isValidGpsCoordinate(lat, lon)) {
                issues.add("Invalid GPS coordinates: " + lat + ", " + lon);
            }
        } catch (Exception e) {
            issues.add("Invalid coordinate format");
        }

        // 📅 Validate timestamp
        try {
            parseRawTimestamp(gpsData.getTimestamp());
        } catch (Exception e) {
            warnings.add("Invalid timestamp format, will use current time");
        }

        // 🔧 Check ignition status
        String ignition = gpsData.getIgnition();
        if (ignition != null && !ignition.trim().isEmpty()) {
            String normalized = ignition.trim().toUpperCase();
            if (!IGNITION_ON_VALUES.contains(normalized) && !IGNITION_OFF_VALUES.contains(normalized)) {
                warnings.add("Unknown ignition status: " + ignition + " (will default to OFF)");
            }
        }

        boolean isValid = issues.isEmpty();

        if (isValid) {
            logColorful("✅ COMPREHENSIVE VALIDATION PASSED", BRIGHT_GREEN);
        } else {
            logColorful("❌ VALIDATION FAILED: " + issues.size() + " issues, " + warnings.size() + " warnings", BRIGHT_RED);
        }

        // ✅ This was missing
        return new ValidationResult(isValid, issues, warnings);
    }

    /**
     * 🔄 Reset metrics
     */
    public void resetMetrics() {
        totalTransformations.set(0);
        successfulTransformations.set(0);
        failedTransformations.set(0);
        ignitionNormalizations.set(0);
        hexConversions.set(0);
        timestampFixedCount.set(0);
        transformationCounts.clear();
        transformationTimes.clear();
        
        logColorful("🔄 TRANSFORMATION METRICS RESET", BRIGHT_GREEN);
        logger.info("Data transformation metrics reset");
    }
    
    /**
     * 🚀 Batch transformation with metrics
     */
    public List<VehicleHistory> batchTransformToVehicleHistory(List<GpsData> gpsDataList, Vehicle vehicle) {
        long startTime = System.currentTimeMillis();
        List<VehicleHistory> results = new ArrayList<>();
        int successCount = 0;
        int failureCount = 0;
        
        logColorful("📦 BATCH TRANSFORMATION TO VEHICLE HISTORY", BRIGHT_BLUE);
        logColorful("📊 Batch size: " + gpsDataList.size(), BLUE);
        
        for (int i = 0; i < gpsDataList.size(); i++) {
            try {
                VehicleHistory history = toVehicleHistory(gpsDataList.get(i), vehicle);
                results.add(history);
                successCount++;
            } catch (Exception e) {
                failureCount++;
                logColorful("❌ Failed to transform record " + (i + 1) + ": " + e.getMessage(), RED);
            }
        }
        
        long duration = System.currentTimeMillis() - startTime;
        logColorful("📦 BATCH TRANSFORMATION COMPLETE", BRIGHT_GREEN);
        logColorful("✅ Success: " + successCount, GREEN);
        logColorful("❌ Failed: " + failureCount, failureCount > 0 ? RED : GREEN);
        logColorful("⏱️ Total time: " + duration + "ms", CYAN);
        
        return results;
    }
    
    /**
     * 🎯 Transform with validation
     */
    public VehicleHistory toVehicleHistoryWithValidation(GpsData gpsData, Vehicle vehicle) {
        ValidationResult validation = validateAndTransform(gpsData);
        
        if (!validation.isValid()) {
            throw new IllegalArgumentException("Validation failed: " + String.join(", ", validation.getIssues()));
        }
        
        if (!validation.getWarnings().isEmpty()) {
            logColorful("⚠️ WARNINGS DURING TRANSFORMATION:", BRIGHT_YELLOW);
            validation.getWarnings().forEach(warning -> logColorful("⚠️ " + warning, YELLOW));
        }
        
        return toVehicleHistory(gpsData, vehicle);
    }
    
    /**
     * 📊 Get performance statistics
     */
    public PerformanceStats getPerformanceStats() {
        long totalTime = transformationTimes.values().stream()
            .mapToLong(Long::longValue)
            .sum();
        
        long avgTime = transformationTimes.isEmpty() ? 0 : totalTime / transformationTimes.size();
        
        return new PerformanceStats(
            totalTransformations.get(),
            successfulTransformations.get(),
            failedTransformations.get(),
            avgTime,
            transformationTimes.size()
        );
    }
    
    /**
     * 🎨 Colorful logging utility
     */
    private void logColorful(String message, String color) {
        System.out.println(color + message + RESET);
        logger.info(message.replaceAll("║", "").replaceAll("╔", "").replaceAll("╚", "").replaceAll("╠", "").replaceAll("═", "").trim());
    }
    
    /**
     * 🏃 Speed unit enumeration
     */
    public enum SpeedUnit {
        KMH, // Kilometers per hour
        MPH, // Miles per hour
        MPS  // Meters per second
    }
    
    /**
     * 📊 Transformation metrics data class
     */
    public static class TransformationMetrics {
        private final long totalTransformations;
        private final long successfulTransformations;
        private final long failedTransformations;
        private final long ignitionNormalizations;
        private final long hexConversions;
        private final long timestampFixed;
        private final Map<String, Long> transformationCounts;
        
        public TransformationMetrics(long totalTransformations, long successfulTransformations, 
                                   long failedTransformations, long ignitionNormalizations, 
                                   long hexConversions, long timestampFixed,
                                   Map<String, Long> transformationCounts) {
            this.totalTransformations = totalTransformations;
            this.successfulTransformations = successfulTransformations;
            this.failedTransformations = failedTransformations;
            this.ignitionNormalizations = ignitionNormalizations;
            this.hexConversions = hexConversions;
            this.timestampFixed = timestampFixed;
            this.transformationCounts = transformationCounts;
        }
        
        // Getters
        public long getTotalTransformations() { return totalTransformations; }
        public long getSuccessfulTransformations() { return successfulTransformations; }
        public long getFailedTransformations() { return failedTransformations; }
        public long getIgnitionNormalizations() { return ignitionNormalizations; }
        public long getHexConversions() { return hexConversions; }
        public long getTimestampFixed() { return timestampFixed; }
        public Map<String, Long> getTransformationCounts() { return transformationCounts; }
        
        public double getSuccessRate() {
            return totalTransformations > 0 ? (successfulTransformations * 100.0) / totalTransformations : 0.0;
        }
        
        @Override
        public String toString() {
            return String.format("TransformationMetrics{total=%d, success=%d, failed=%d, ignitionNormalized=%d, " +
                               "hexConverted=%d, timestampFixed=%d, successRate=%.2f%%}", 
                totalTransformations, successfulTransformations, failedTransformations,
                ignitionNormalizations, hexConversions, timestampFixed, getSuccessRate());
        }
    }
    
    /**
     * 📊 Validation result data class
     */
    public static class ValidationResult {
        private final boolean valid;
        private final List<String> issues;
        private final List<String> warnings;
        
        public ValidationResult(boolean valid, List<String> issues, List<String> warnings) {
            this.valid = valid;
            this.issues = issues;
            this.warnings = warnings;
        }
        
        // Getters
        public boolean isValid() { return valid; }
        public List<String> getIssues() { return issues; }
        public List<String> getWarnings() { return warnings; }
        
        @Override
        public String toString() {
            return String.format("ValidationResult{valid=%s, issues=%d, warnings=%d}", 
                valid, issues.size(), warnings.size());
        }
    }
    
    /**
     * 📊 Performance statistics data class
     */
    public static class PerformanceStats {
        private final long totalTransformations;
        private final long successfulTransformations;
        private final long failedTransformations;
        private final long averageTimeMs;
        private final long totalTimedOperations;
        
        public PerformanceStats(long totalTransformations, long successfulTransformations, 
                              long failedTransformations, long averageTimeMs, long totalTimedOperations) {
            this.totalTransformations = totalTransformations;
            this.successfulTransformations = successfulTransformations;
            this.failedTransformations = failedTransformations;
            this.averageTimeMs = averageTimeMs;
            this.totalTimedOperations = totalTimedOperations;
        }
        
        // Getters
        public long getTotalTransformations() { return totalTransformations; }
        public long getSuccessfulTransformations() { return successfulTransformations; }
        public long getFailedTransformations() { return failedTransformations; }
        public long getAverageTimeMs() { return averageTimeMs; }
        public long getTotalTimedOperations() { return totalTimedOperations; }
        
        public double getSuccessRate() {
            return totalTransformations > 0 ? (successfulTransformations * 100.0) / totalTransformations : 0.0;
        }
        
        @Override
        public String toString() {
            return String.format("PerformanceStats{total=%d, success=%d, failed=%d, avgTime=%dms, successRate=%.2f%%}", 
                totalTransformations, successfulTransformations, failedTransformations, 
                averageTimeMs, getSuccessRate());
        }
    }
}