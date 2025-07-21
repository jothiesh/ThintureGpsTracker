package com.GpsTracker.Thinture.service.processing;

import com.GpsTracker.Thinture.model.GpsData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 🌟 ENHANCED GPS Data Validator - Production Ready for 5000+ Devices
 * ✅ Colorful, structured logging with emojis
 * ✅ RAW timestamp validation (no timezone conversion)
 * ✅ Enhanced ignition status validation
 * ✅ Performance monitoring and metrics
 * ✅ Advanced validation rules and thresholds
 * ✅ Real-time validation dashboard
 * ✅ Device-specific validation tracking
 * ✅ Batch validation optimization
 * 
 * Validates GPS data WITHOUT timezone conversion
 * Ensures data quality for 5000+ devices from any timezone
 */
@Component
public class GpsDataValidator {
    
    private static final Logger logger = LoggerFactory.getLogger(GpsDataValidator.class);
    private static final Logger performanceLogger = LoggerFactory.getLogger("PERFORMANCE");
    private static final Logger validationLogger = LoggerFactory.getLogger("VALIDATION");
    private static final Logger metricsLogger = LoggerFactory.getLogger("METRICS");
    
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
    
    // 🔧 Validation constants
    private static final double MIN_LATITUDE = -90.0;
    private static final double MAX_LATITUDE = 90.0;
    private static final double MIN_LONGITUDE = -180.0;
    private static final double MAX_LONGITUDE = 180.0;
    private static final double MAX_SPEED = 300.0; // km/h
    private static final double MIN_SPEED = 0.0;
    private static final int MIN_GSM_STRENGTH = 0;
    private static final int MAX_GSM_STRENGTH = 31;
    private static final double SUSPICIOUS_COORDINATE_THRESHOLD = 0.000001; // For (0,0) detection
    
    // 📅 Date/Time formatting
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    
    // ✅ Enhanced ignition status validation
    private static final Set<String> VALID_IGNITION_ON = Set.of(
        "ON", "1", "TRUE", "IGON", "IG_ON", "IGNITION_ON", "ENGINE_ON", "STARTED"
    );
    
    private static final Set<String> VALID_IGNITION_OFF = Set.of(
        "OFF", "0", "FALSE", "IGOFF", "IG_OFF", "IGNITION_OFF", "ENGINE_OFF", "STOPPED"
    );
    
    // 📊 Performance metrics
    private final AtomicLong totalValidations = new AtomicLong(0);
    private final AtomicLong successfulValidations = new AtomicLong(0);
    private final AtomicLong failedValidations = new AtomicLong(0);
    private final AtomicLong batchValidations = new AtomicLong(0);
    private final AtomicLong suspiciousCoordinates = new AtomicLong(0);
    private final AtomicLong timestampIssues = new AtomicLong(0);
    private final AtomicLong ignitionIssues = new AtomicLong(0);
    private final AtomicLong imeiIssues = new AtomicLong(0);
    private final AtomicLong coordinateIssues = new AtomicLong(0);
    
    // 📊 Device-specific validation tracking
    private final Map<String, DeviceValidationStats> deviceValidationStats = new ConcurrentHashMap<>();
    private final Map<String, Long> validationTimes = new ConcurrentHashMap<>();
    
    // 🎯 Validation rules configuration
    private final Map<String, ValidationRule> validationRules = new ConcurrentHashMap<>();
    
    @PostConstruct
    public void initialize() {
        logColorful("🌟 INITIALIZING ENHANCED GPS DATA VALIDATOR", BRIGHT_CYAN);
        logColorful("⏱️ Startup time: " + LocalDateTime.now().format(DISPLAY_FORMATTER), CYAN);
        
        // 📊 Display configuration
        displayConfiguration();
        
        // 🔧 Initialize validation rules
        initializeValidationRules();
        
        logColorful("✅ Enhanced GPS Data Validator initialized successfully", BRIGHT_GREEN);
        displayWelcomeDashboard();
    }
    
    /**
     * 📊 Display validator configuration
     */
    private void displayConfiguration() {
        logColorful("", "");
        logColorful("╔════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════", BRIGHT_BLUE);
        logColorful("║ ⚙️ GPS DATA VALIDATOR CONFIGURATION", BRIGHT_BLUE);
        logColorful("╠════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════", BRIGHT_BLUE);
        logColorful("║ 📍 Latitude Range: " + MIN_LATITUDE + " to " + MAX_LATITUDE, BLUE);
        logColorful("║ 📍 Longitude Range: " + MIN_LONGITUDE + " to " + MAX_LONGITUDE, BLUE);
        logColorful("║ 🏃 Speed Range: " + MIN_SPEED + " to " + MAX_SPEED + " km/h", BLUE);
        logColorful("║ 📡 GSM Strength Range: " + MIN_GSM_STRENGTH + " to " + MAX_GSM_STRENGTH, BLUE);
        logColorful("║ 🔧 Ignition ON Values: " + VALID_IGNITION_ON.size() + " variants", BLUE);
        logColorful("║ 🔧 Ignition OFF Values: " + VALID_IGNITION_OFF.size() + " variants", BLUE);
        logColorful("║ 📅 Timestamp Format: yyyy-MM-dd HH:mm:ss (RAW)", BLUE);
        logColorful("║ 🎯 Target Capacity: 5000+ devices", BLUE);
        logColorful("║ 🏗️ Architecture: Enhanced Multi-Layer", BLUE);
        logColorful("╚════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════", BRIGHT_BLUE);
    }
    
    /**
     * 🔧 Initialize validation rules
     */
    private void initializeValidationRules() {
        validationRules.put("REQUIRED_FIELDS", new ValidationRule("Required Fields", true, "All required fields must be present"));
        validationRules.put("COORDINATE_RANGE", new ValidationRule("Coordinate Range", true, "Coordinates must be within valid GPS range"));
        validationRules.put("SPEED_RANGE", new ValidationRule("Speed Range", true, "Speed must be within realistic range"));
        validationRules.put("TIMESTAMP_FORMAT", new ValidationRule("Timestamp Format", true, "Timestamp must be in valid format"));
        validationRules.put("IMEI_FORMAT", new ValidationRule("IMEI Format", true, "IMEI must be 15 digits"));
        validationRules.put("IGNITION_STATUS", new ValidationRule("Ignition Status", false, "Ignition status should be recognized"));
        validationRules.put("GSM_STRENGTH", new ValidationRule("GSM Strength", false, "GSM strength should be in valid range"));
        validationRules.put("SUSPICIOUS_COORDINATES", new ValidationRule("Suspicious Coordinates", false, "Check for (0,0) coordinates"));
        
        logColorful("🔧 Validation rules initialized: " + validationRules.size() + " rules", CYAN);
    }
    
    /**
     * 🎨 Display welcome dashboard
     */
    private void displayWelcomeDashboard() {
        logColorful("", "");
        logColorful("╔════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════", BRIGHT_MAGENTA);
        logColorful("║ 🎯 ENHANCED GPS DATA VALIDATOR DASHBOARD", BRIGHT_MAGENTA);
        logColorful("║ 🚀 Ready to validate GPS data for 5000+ devices", BRIGHT_MAGENTA);
        logColorful("╠════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════", BRIGHT_MAGENTA);
        logColorful("║ 🔍 Required Fields Validation: ACTIVE", BRIGHT_GREEN);
        logColorful("║ 📍 Coordinate Validation: ACTIVE", BRIGHT_GREEN);
        logColorful("║ 🏃 Speed Validation: ACTIVE", BRIGHT_GREEN);
        logColorful("║ 📅 RAW Timestamp Validation: ACTIVE", BRIGHT_GREEN);
        logColorful("║ 📱 IMEI Validation: ACTIVE", BRIGHT_GREEN);
        logColorful("║ 🔧 Ignition Status Validation: ACTIVE", BRIGHT_GREEN);
        logColorful("║ 📊 Batch Validation: ACTIVE", BRIGHT_GREEN);
        logColorful("║ 📈 Performance Monitoring: ACTIVE", BRIGHT_GREEN);
        logColorful("╚════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════", BRIGHT_MAGENTA);
    }
    
    /**
     * 🔍 ENHANCED: Validate a single GPS data record with colorful logging
     */
    public ValidationResult validate(GpsData gpsData) {
        long startTime = System.currentTimeMillis();
        String deviceId = gpsData != null ? gpsData.getDeviceID() : "unknown";
        
        try {
            totalValidations.incrementAndGet();
            
            logColorful("🔍 VALIDATING GPS DATA RECORD", BRIGHT_BLUE);
            logColorful("📱 Device ID: " + deviceId, BLUE);
            
            List<String> errors = new ArrayList<>();
            
            // 🔍 Null check
            if (gpsData == null) {
                errors.add("GPS data is null");
                logColorful("🚫 GPS DATA IS NULL", BRIGHT_RED);
                failedValidations.incrementAndGet();
                return new ValidationResult(false, errors);
            }
            
            logColorful("✅ GPS data object validation passed", GREEN);
            
            // 🔍 Required fields validation
            logColorful("🔍 VALIDATING REQUIRED FIELDS", BLUE);
            validateRequiredFields(gpsData, errors);
            
            // 🔍 Data integrity validation
            if (errors.isEmpty()) {
                logColorful("🔍 VALIDATING DATA INTEGRITY", BLUE);
                validateDataIntegrity(gpsData, errors);
            }
            
            // 🔍 RAW business rules validation
            if (errors.isEmpty()) {
                logColorful("🔍 VALIDATING BUSINESS RULES", BLUE);
                validateRawBusinessRules(gpsData, errors);
            }
            
            // 📊 Update device-specific stats
            updateDeviceValidationStats(deviceId, errors.isEmpty());
            
            // 📊 Performance tracking
            long duration = System.currentTimeMillis() - startTime;
            validationTimes.put(deviceId + "-" + System.currentTimeMillis(), duration);
            
            // 📊 Final result
            boolean isValid = errors.isEmpty();
            
            if (isValid) {
                successfulValidations.incrementAndGet();
                logColorful("✅ GPS DATA VALIDATION SUCCESSFUL", BRIGHT_GREEN);
                logColorful("📱 Device: " + deviceId, GREEN);
                logColorful("⏱️ Validation time: " + duration + "ms", GREEN);
                validationLogger.debug("GPS data validation successful for device: {} in {}ms", deviceId, duration);
            } else {
                failedValidations.incrementAndGet();
                logColorful("❌ GPS DATA VALIDATION FAILED", BRIGHT_RED);
                logColorful("📱 Device: " + deviceId, RED);
                logColorful("💥 Errors: " + String.join(", ", errors), RED);
                logColorful("⏱️ Validation time: " + duration + "ms", RED);
                validationLogger.warn("GPS data validation failed for device {}: {}", deviceId, String.join(", ", errors));
            }
            
            return new ValidationResult(isValid, errors);
            
        } catch (Exception e) {
            failedValidations.incrementAndGet();
            logColorful("❌ VALIDATION EXCEPTION", BRIGHT_RED);
            logColorful("💥 Error: " + e.getMessage(), RED);
            logger.error("Exception during GPS data validation for device: {}", deviceId, e);
            return new ValidationResult(false, List.of("Validation exception: " + e.getMessage()));
        }
    }
    
    /**
     * 📦 ENHANCED: Batch validation for multiple GPS records
     */
    public BatchValidationResult validateBatch(List<GpsData> gpsDataList) {
        long startTime = System.currentTimeMillis();
        
        try {
            batchValidations.incrementAndGet();
            
            logColorful("📦 STARTING BATCH VALIDATION", BRIGHT_BLUE);
            
            if (gpsDataList == null || gpsDataList.isEmpty()) {
                logColorful("🚫 BATCH IS NULL OR EMPTY", BRIGHT_RED);
                return new BatchValidationResult(0, 0, 0, new ArrayList<>());
            }
            
            int totalRecords = gpsDataList.size();
            int validRecords = 0;
            int invalidRecords = 0;
            List<ValidationError> validationErrors = new ArrayList<>();
            
            logColorful("📊 Batch size: " + totalRecords + " records", BRIGHT_BLUE);
            
            // 📊 Progress tracking for large batches
            int progressInterval = Math.max(1, totalRecords / 10); // 10% intervals
            
            for (int i = 0; i < gpsDataList.size(); i++) {
                GpsData gpsData = gpsDataList.get(i);
                
                // 🔍 Null record check
                if (gpsData == null) {
                    invalidRecords++;
                    validationErrors.add(new ValidationError(i, "unknown", List.of("GPS data is null")));
                    continue;
                }
                
                // 🔍 Validate individual record
                ValidationResult result = validate(gpsData);
                
                if (result.isValid()) {
                    validRecords++;
                } else {
                    invalidRecords++;
                    validationErrors.add(new ValidationError(i, gpsData.getDeviceID(), result.getErrors()));
                }
                
                // 📊 Progress logging
                if (totalRecords > 100 && (i + 1) % progressInterval == 0) {
                    int progress = (int) ((i + 1) * 100.0 / totalRecords);
                    logColorful("📊 BATCH PROGRESS: " + progress + "% (" + (i + 1) + "/" + totalRecords + ")", BRIGHT_CYAN);
                }
            }
            
            // 📊 Final batch results
            long duration = System.currentTimeMillis() - startTime;
            double validationRate = (validRecords * 100.0) / totalRecords;
            
            logColorful("📈 BATCH VALIDATION COMPLETE", BRIGHT_GREEN);
            logColorful("✅ Valid records: " + validRecords, GREEN);
            logColorful("❌ Invalid records: " + invalidRecords, invalidRecords > 0 ? RED : GREEN);
            logColorful("📊 Validation rate: " + String.format("%.2f%%", validationRate), 
                       validationRate > 95 ? BRIGHT_GREEN : validationRate > 80 ? BRIGHT_YELLOW : BRIGHT_RED);
            logColorful("⏱️ Total time: " + duration + "ms", CYAN);
            logColorful("⚡ Avg time per record: " + String.format("%.2f", (double) duration / totalRecords) + "ms", CYAN);
            
            validationLogger.info("Batch validation complete: {}/{} valid records ({:.2f}%) in {}ms",
                validRecords, totalRecords, validationRate, duration);
            
            return new BatchValidationResult(totalRecords, validRecords, invalidRecords, validationErrors);
            
        } catch (Exception e) {
            logColorful("❌ BATCH VALIDATION EXCEPTION", BRIGHT_RED);
            logColorful("💥 Error: " + e.getMessage(), RED);
            logger.error("Exception during batch validation", e);
            return new BatchValidationResult(0, 0, 0, List.of(new ValidationError(-1, "batch", List.of("Batch validation exception: " + e.getMessage()))));
        }
    }
    
    /**
     * 🔍 ENHANCED: Validate required fields with detailed logging
     */
    private void validateRequiredFields(GpsData gpsData, List<String> errors) {
        logColorful("🔍 Checking required fields", BLUE);
        
        // 📱 Device ID validation
        if (isNullOrEmpty(gpsData.getDeviceID())) {
            errors.add("DeviceID is required");
            logColorful("❌ Missing DeviceID", RED);
        } else {
            logColorful("✅ DeviceID present: " + gpsData.getDeviceID(), GREEN);
        }
        
        // 📱 IMEI validation
        if (isNullOrEmpty(gpsData.getImei())) {
            errors.add("IMEI is required");
            logColorful("❌ Missing IMEI", RED);
            imeiIssues.incrementAndGet();
        } else {
            logColorful("✅ IMEI present: " + gpsData.getImei(), GREEN);
        }
        
        // 📍 Latitude validation
        if (isNullOrEmpty(gpsData.getLatitude())) {
            errors.add("Latitude is required");
            logColorful("❌ Missing Latitude", RED);
            coordinateIssues.incrementAndGet();
        } else {
            logColorful("✅ Latitude present: " + gpsData.getLatitude(), GREEN);
        }
        
        // 📍 Longitude validation
        if (isNullOrEmpty(gpsData.getLongitude())) {
            errors.add("Longitude is required");
            logColorful("❌ Missing Longitude", RED);
            coordinateIssues.incrementAndGet();
        } else {
            logColorful("✅ Longitude present: " + gpsData.getLongitude(), GREEN);
        }
        
        // 📅 Timestamp validation
        if (isNullOrEmpty(gpsData.getTimestamp())) {
            errors.add("Timestamp is required");
            logColorful("❌ Missing Timestamp", RED);
            timestampIssues.incrementAndGet();
        } else {
            logColorful("✅ Timestamp present: " + gpsData.getTimestamp(), GREEN);
        }
        
        // 📊 Status validation
        if (isNullOrEmpty(gpsData.getStatus())) {
            errors.add("Status is required");
            logColorful("❌ Missing Status", RED);
        } else {
            logColorful("✅ Status present: " + gpsData.getStatus(), GREEN);
        }
        
        if (errors.isEmpty()) {
            logColorful("✅ ALL REQUIRED FIELDS PRESENT", BRIGHT_GREEN);
        } else {
            logColorful("❌ MISSING REQUIRED FIELDS: " + errors.size(), BRIGHT_RED);
        }
    }
    
    /**
     * 🔍 ENHANCED: Validate data integrity with detailed logging
     */
    private void validateDataIntegrity(GpsData gpsData, List<String> errors) {
        logColorful("🔍 Checking data integrity", BLUE);
        
        // 📍 Coordinate validation
        validateCoordinates(gpsData, errors);
        
        // 🏃 Speed validation
        validateSpeed(gpsData, errors);
        
        // 📅 Timestamp validation
        validateTimestamp(gpsData, errors);
        
        // 📱 IMEI validation
        validateImei(gpsData, errors);
        
        // 📡 GSM strength validation
        validateGsmStrength(gpsData, errors);
        
        if (errors.isEmpty()) {
            logColorful("✅ DATA INTEGRITY VALIDATION PASSED", BRIGHT_GREEN);
        } else {
            logColorful("❌ DATA INTEGRITY ISSUES: " + errors.size(), BRIGHT_RED);
        }
    }
    
    /**
     * 📍 Validate coordinates
     */
    private void validateCoordinates(GpsData gpsData, List<String> errors) {
        try {
            // 📍 Latitude validation
            double latitude = Double.parseDouble(gpsData.getLatitude());
            if (latitude < MIN_LATITUDE || latitude > MAX_LATITUDE) {
                errors.add(String.format("Invalid latitude: %f (must be between %f and %f)", 
                    latitude, MIN_LATITUDE, MAX_LATITUDE));
                logColorful("❌ INVALID LATITUDE: " + latitude, RED);
                coordinateIssues.incrementAndGet();
            } else {
                logColorful("✅ Latitude valid: " + latitude, GREEN);
            }
            
            // 📍 Longitude validation
            double longitude = Double.parseDouble(gpsData.getLongitude());
            if (longitude < MIN_LONGITUDE || longitude > MAX_LONGITUDE) {
                errors.add(String.format("Invalid longitude: %f (must be between %f and %f)", 
                    longitude, MIN_LONGITUDE, MAX_LONGITUDE));
                logColorful("❌ INVALID LONGITUDE: " + longitude, RED);
                coordinateIssues.incrementAndGet();
            } else {
                logColorful("✅ Longitude valid: " + longitude, GREEN);
            }
            
            // 🎯 Suspicious coordinates check (e.g., 0,0)
            if (Math.abs(latitude) < SUSPICIOUS_COORDINATE_THRESHOLD && 
                Math.abs(longitude) < SUSPICIOUS_COORDINATE_THRESHOLD) {
                logColorful("⚠️ SUSPICIOUS COORDINATES: " + latitude + ", " + longitude + " (near 0,0)", BRIGHT_YELLOW);
                suspiciousCoordinates.incrementAndGet();
                // Not adding to errors as this is just suspicious, not invalid
            }
            
        } catch (NumberFormatException e) {
            errors.add("Invalid coordinate format");
            logColorful("❌ COORDINATE FORMAT ERROR: " + e.getMessage(), RED);
            coordinateIssues.incrementAndGet();
        }
    }
    
    /**
     * 🏃 Validate speed
     */
    private void validateSpeed(GpsData gpsData, List<String> errors) {
        if (gpsData.getSpeed() != null && !gpsData.getSpeed().isEmpty()) {
            try {
                double speed = Double.parseDouble(gpsData.getSpeed());
                if (speed < MIN_SPEED || speed > MAX_SPEED) {
                    errors.add(String.format("Invalid speed: %f km/h (must be between %f and %f)", 
                        speed, MIN_SPEED, MAX_SPEED));
                    logColorful("❌ INVALID SPEED: " + speed + " km/h", RED);
                } else {
                    logColorful("✅ Speed valid: " + speed + " km/h", GREEN);
                }
            } catch (NumberFormatException e) {
                errors.add("Invalid speed format: " + gpsData.getSpeed());
                logColorful("❌ SPEED FORMAT ERROR: " + gpsData.getSpeed(), RED);
            }
        } else {
            logColorful("📊 Speed not provided (optional)", YELLOW);
        }
    }
    
    /**
     * 📅 Validate timestamp
     */
    private void validateTimestamp(GpsData gpsData, List<String> errors) {
        if (!isValidRawTimestamp(gpsData.getTimestamp())) {
            errors.add("Invalid timestamp format: " + gpsData.getTimestamp() + " (expected: yyyy-MM-dd HH:mm:ss)");
            logColorful("❌ INVALID TIMESTAMP FORMAT: " + gpsData.getTimestamp(), RED);
            timestampIssues.incrementAndGet();
        } else {
            logColorful("✅ RAW timestamp format valid: " + gpsData.getTimestamp(), GREEN);
        }
    }
    
    /**
     * 📱 Validate IMEI
     */
    private void validateImei(GpsData gpsData, List<String> errors) {
        if (!isValidImei(gpsData.getImei())) {
            errors.add("Invalid IMEI: " + gpsData.getImei() + " (must be 15 digits)");
            logColorful("❌ INVALID IMEI: " + gpsData.getImei(), RED);
            imeiIssues.incrementAndGet();
        } else {
            logColorful("✅ IMEI valid: " + gpsData.getImei(), GREEN);
        }
    }
    
    /**
     * 📡 Validate GSM strength
     */
    private void validateGsmStrength(GpsData gpsData, List<String> errors) {
        if (gpsData.getGsmStrength() != null && !gpsData.getGsmStrength().isEmpty()) {
            try {
                int gsmStrength = Integer.parseInt(gpsData.getGsmStrength());
                if (gsmStrength < MIN_GSM_STRENGTH || gsmStrength > MAX_GSM_STRENGTH) {
                    // Warning only, not critical
                    logColorful("⚠️ GSM STRENGTH OUT OF RANGE: " + gsmStrength + " (typical: 0-31)", BRIGHT_YELLOW);
                } else {
                    logColorful("✅ GSM strength valid: " + gsmStrength, GREEN);
                }
            } catch (NumberFormatException e) {
                logColorful("⚠️ GSM STRENGTH FORMAT ERROR: " + gpsData.getGsmStrength(), YELLOW);
            }
        } else {
            logColorful("📊 GSM strength not provided (optional)", YELLOW);
        }
    }
    
    /**
     * 🔍 ENHANCED: RAW business rules validation
     */
    private void validateRawBusinessRules(GpsData gpsData, List<String> errors) {
        logColorful("🔍 Checking business rules", BLUE);
        
        // 📍 Suspicious coordinates check
        try {
            double lat = Double.parseDouble(gpsData.getLatitude());
            double lon = Double.parseDouble(gpsData.getLongitude());
            
            if (lat == 0.0 && lon == 0.0) {
                logColorful("⚠️ EXACT ZERO COORDINATES DETECTED: (0,0)", BRIGHT_YELLOW);
                suspiciousCoordinates.incrementAndGet();
                // Warning only, not an error
            }
        } catch (NumberFormatException e) {
            // Already handled in data integrity
        }
        
        // 🔍 RAW timestamp validation (format only, no timezone checks)
        try {
            LocalDateTime deviceTimestamp = LocalDateTime.parse(gpsData.getTimestamp(), TIMESTAMP_FORMATTER);
            logColorful("📅 RAW timestamp validation: " + deviceTimestamp + " - Format OK", GREEN);
        } catch (DateTimeParseException e) {
            // Already handled in data integrity
        }
        
        // 🔧 Enhanced ignition status validation
        validateIgnitionStatus(gpsData, errors);
        
        if (errors.isEmpty()) {
            logColorful("✅ BUSINESS RULES VALIDATION PASSED", BRIGHT_GREEN);
        } else {
            logColorful("❌ BUSINESS RULES ISSUES: " + errors.size(), BRIGHT_RED);
        }
    }
    
    /**
     * 🔧 Validate ignition status
     */
    private void validateIgnitionStatus(GpsData gpsData, List<String> errors) {
        if (gpsData.getIgnition() != null) {
            String ignition = gpsData.getIgnition().trim().toUpperCase();
            
            if (!VALID_IGNITION_ON.contains(ignition) && !VALID_IGNITION_OFF.contains(ignition)) {
                logColorful("⚠️ UNKNOWN IGNITION STATUS: " + gpsData.getIgnition() + " (will be normalized to OFF)", BRIGHT_YELLOW);
                ignitionIssues.incrementAndGet();
                // Warning only, not an error as it will be normalized
            } else {
                logColorful("✅ Ignition status valid: " + gpsData.getIgnition(), GREEN);
            }
        } else {
            logColorful("📊 Ignition status not provided (optional)", YELLOW);
        }
    }
    
    /**
     * 📅 ENHANCED: Validate RAW timestamp format
     */
    private boolean isValidRawTimestamp(String timestamp) {
        if (timestamp == null || timestamp.trim().isEmpty()) {
            return false;
        }
        
        try {
            LocalDateTime.parse(timestamp, TIMESTAMP_FORMATTER);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }
    
    /**
     * 📱 ENHANCED: Validate IMEI format
     */
    private boolean isValidImei(String imei) {
        if (imei == null || imei.trim().isEmpty()) {
            return false;
        }
        
        String cleanImei = imei.trim();
        
        // IMEI should be 15 digits
        if (cleanImei.length() != 15) {
            return false;
        }
        
        // Should contain only digits
        return cleanImei.matches("\\d{15}");
    }
    
    /**
     * 🔍 Check if string is null or empty
     */
    private boolean isNullOrEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }
    
    /**
     * 📊 Update device-specific validation statistics
     */
    private void updateDeviceValidationStats(String deviceId, boolean isValid) {
        deviceValidationStats.compute(deviceId, (id, existing) -> {
            if (existing == null) {
                return new DeviceValidationStats(deviceId, isValid);
            } else {
                existing.updateStats(isValid);
                return existing;
            }
        });
    }
    
    /**
     * 📊 Get validation statistics with dashboard
     */
    public ValidationStatistics getValidationStatistics() {
        ValidationStatistics stats = new ValidationStatistics(
            totalValidations.get(),
            successfulValidations.get(),
            failedValidations.get(),
            batchValidations.get(),
            suspiciousCoordinates.get(),
            timestampIssues.get(),
            ignitionIssues.get(),
            imeiIssues.get(),
            coordinateIssues.get(),
            deviceValidationStats.size()
        );
        
        // 📊 Display statistics dashboard
        displayStatisticsDashboard(stats);
        
        return stats;
    }
    
    /**
     * 📊 Display statistics dashboard
     */
    private void displayStatisticsDashboard(ValidationStatistics stats) {
        logColorful("", "");
        logColorful("╔════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════", BRIGHT_CYAN);
        logColorful("║ 📊 GPS DATA VALIDATION STATISTICS DASHBOARD", BRIGHT_CYAN);
        logColorful("║ 🕒 " + LocalDateTime.now().format(DISPLAY_FORMATTER), BRIGHT_CYAN);
        logColorful("╠════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════", BRIGHT_CYAN);
        logColorful("║ 📈 Total Validations: " + stats.getTotalValidations(), BLUE);
        logColorful("║ ✅ Successful: " + stats.getSuccessfulValidations(), BRIGHT_GREEN);
        logColorful("║ ❌ Failed: " + stats.getFailedValidations(), stats.getFailedValidations() > 0 ? BRIGHT_RED : GREEN);
        logColorful("║ 📦 Batch Validations: " + stats.getBatchValidations(), PURPLE);
        logColorful("║ 📊 Success Rate: " + String.format("%.2f%%", stats.getSuccessRate()), 
                   stats.getSuccessRate() > 95 ? BRIGHT_GREEN : stats.getSuccessRate() > 80 ? BRIGHT_YELLOW : BRIGHT_RED);
        logColorful("║ 📱 Active Devices: " + stats.getActiveDevices(), CYAN);
        
        // 📊 Issue breakdown
        logColorful("║ 🔍 ISSUE BREAKDOWN:", BRIGHT_BLUE);
        logColorful("║   ⚠️ Suspicious Coordinates: " + stats.getSuspiciousCoordinates(), 
                   stats.getSuspiciousCoordinates() > 0 ? BRIGHT_YELLOW : GREEN);
        logColorful("║   📅 Timestamp Issues: " + stats.getTimestampIssues(), 
                   stats.getTimestampIssues() > 0 ? BRIGHT_RED : GREEN);
        logColorful("║   🔧 Ignition Issues: " + stats.getIgnitionIssues(), 
                   stats.getIgnitionIssues() > 0 ? BRIGHT_YELLOW : GREEN);
        logColorful("║   📱 IMEI Issues: " + stats.getImeiIssues(), 
                   stats.getImeiIssues() > 0 ? BRIGHT_RED : GREEN);
        logColorful("║   📍 Coordinate Issues: " + stats.getCoordinateIssues(), 
                   stats.getCoordinateIssues() > 0 ? BRIGHT_RED : GREEN);
        
        logColorful("╚════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════", BRIGHT_CYAN);
    }
    
    /**
     * 📊 Scheduled statistics reporting
     */
    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void reportStatistics() {
        try {
            ValidationStatistics stats = getValidationStatistics();
            
            // 📊 Log to metrics logger
            metricsLogger.info("Validation Stats - Total: {}, Success: {}, Failed: {}, Rate: {:.2f}%, Issues: [Coords: {}, Timestamp: {}, IMEI: {}, Ignition: {}]",
                stats.getTotalValidations(), stats.getSuccessfulValidations(), stats.getFailedValidations(),
                stats.getSuccessRate(), stats.getCoordinateIssues(), stats.getTimestampIssues(),
                stats.getImeiIssues(), stats.getIgnitionIssues());
            
            // 📊 Clean up old validation times
            cleanupOldValidationTimes();
            
        } catch (Exception e) {
            logColorful("❌ ERROR REPORTING VALIDATION STATISTICS", BRIGHT_RED);
            logger.error("Error reporting validation statistics", e);
        }
    }
    
    /**
     * 🧹 Clean up old validation times
     */
    private void cleanupOldValidationTimes() {
        if (validationTimes.size() > 10000) {
            // Keep only last 5000 entries
            List<String> oldestKeys = validationTimes.keySet().stream()
                .sorted()
                .limit(validationTimes.size() - 5000)
                .collect(Collectors.toList());
            
            oldestKeys.forEach(validationTimes::remove);
            logColorful("🧹 Cleaned up " + oldestKeys.size() + " old validation time entries", CYAN);
        }
    }
    
    /**
     * 📋 Get all valid ignition statuses
     */
    public Set<String> getValidIgnitionStatuses() {
        Set<String> allValid = new HashSet<>();
        allValid.addAll(VALID_IGNITION_ON);
        allValid.addAll(VALID_IGNITION_OFF);
        
        logColorful("📋 VALID IGNITION STATUSES:", BRIGHT_BLUE);
        logColorful("🟢 ON Values: " + VALID_IGNITION_ON, BRIGHT_GREEN);
        logColorful("🔴 OFF Values: " + VALID_IGNITION_OFF, BRIGHT_RED);
        
        return allValid;
    }
    
    /**
     * 🔄 Reset validation metrics
     */
    public void resetMetrics() {
        totalValidations.set(0);
        successfulValidations.set(0);
        failedValidations.set(0);
        batchValidations.set(0);
        suspiciousCoordinates.set(0);
        timestampIssues.set(0);
        ignitionIssues.set(0);
        imeiIssues.set(0);
        coordinateIssues.set(0);
        deviceValidationStats.clear();
        validationTimes.clear();
        
        logColorful("🔄 VALIDATION METRICS RESET", BRIGHT_GREEN);
        logger.info("GPS data validation metrics reset");
    }
    
    /**
     * 🎯 Get top devices by validation count
     */
    public Map<String, Long> getTopDevicesByValidationCount() {
        return deviceValidationStats.entrySet().stream()
            .sorted(Map.Entry.<String, DeviceValidationStats>comparingByValue(
                (a, b) -> Long.compare(b.getTotalValidations(), a.getTotalValidations())
            ))
            .limit(10)
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> e.getValue().getTotalValidations(),
                (e1, e2) -> e1,
                LinkedHashMap::new
            ));
    }
    
    /**
     * 🎨 Colorful logging utility
     */
    private void logColorful(String message, String color) {
        System.out.println(color + message + RESET);
        logger.info(message.replaceAll("║", "").replaceAll("╔", "").replaceAll("╚", "").replaceAll("╠", "").replaceAll("═", "").trim());
    }
    
    // 📊 Data classes
    
    /**
     * 📊 Validation result class
     */
    public static class ValidationResult {
        private final boolean valid;
        private final List<String> errors;
        
        public ValidationResult(boolean valid, List<String> errors) {
            this.valid = valid;
            this.errors = errors;
        }
        
        public boolean isValid() { return valid; }
        public List<String> getErrors() { return errors; }
        
        @Override
        public String toString() {
            return String.format("ValidationResult{valid=%s, errors=%d}", valid, errors.size());
        }
    }
    
    /**
     * 📦 Batch validation result
     */
    public static class BatchValidationResult {
        private final int totalRecords;
        private final int validRecords;
        private final int invalidRecords;
        private final List<ValidationError> errors;
        
        public BatchValidationResult(int totalRecords, int validRecords, 
                                   int invalidRecords, List<ValidationError> errors) {
            this.totalRecords = totalRecords;
            this.validRecords = validRecords;
            this.invalidRecords = invalidRecords;
            this.errors = errors;
        }
        
        // Getters
        public int getTotalRecords() { return totalRecords; }
        public int getValidRecords() { return validRecords; }
        public int getInvalidRecords() { return invalidRecords; }
        public List<ValidationError> getErrors() { return errors; }
        
        public double getValidationRate() {
            return totalRecords > 0 ? (validRecords * 100.0) / totalRecords : 0.0;
        }
        
        @Override
        public String toString() {
            return String.format("BatchValidationResult{total=%d, valid=%d, invalid=%d, rate=%.2f%%}", 
                totalRecords, validRecords, invalidRecords, getValidationRate());
        }
    }
    
    /**
     * 📊 Validation error for batch processing
     */
    public static class ValidationError {
        private final int recordIndex;
        private final String deviceId;
        private final List<String> errors;
        
        public ValidationError(int recordIndex, String deviceId, List<String> errors) {
            this.recordIndex = recordIndex;
            this.deviceId = deviceId;
            this.errors = errors;
        }
        
        // Getters
        public int getRecordIndex() { return recordIndex; }
        public String getDeviceId() { return deviceId; }
        public List<String> getErrors() { return errors; }
        
        @Override
        public String toString() {
            return String.format("ValidationError{index=%d, device=%s, errors=%d}", 
                recordIndex, deviceId, errors.size());
        }
    }
    
    /**
     * 📊 Device validation statistics
     */
    public static class DeviceValidationStats {
        private final String deviceId;
        private final long firstValidation;
        private long lastValidation;
        private long totalValidations;
        private long successfulValidations;
        private long failedValidations;
        
        public DeviceValidationStats(String deviceId, boolean firstResult) {
            this.deviceId = deviceId;
            this.firstValidation = System.currentTimeMillis();
            this.lastValidation = firstValidation;
            this.totalValidations = 1;
            this.successfulValidations = firstResult ? 1 : 0;
            this.failedValidations = firstResult ? 0 : 1;
        }
        
        public void updateStats(boolean isValid) {
            this.lastValidation = System.currentTimeMillis();
            this.totalValidations++;
            if (isValid) {
                this.successfulValidations++;
            } else {
                this.failedValidations++;
            }
        }
        
        // Getters
        public String getDeviceId() { return deviceId; }
        public long getFirstValidation() { return firstValidation; }
        public long getLastValidation() { return lastValidation; }
        public long getTotalValidations() { return totalValidations; }
        public long getSuccessfulValidations() { return successfulValidations; }
        public long getFailedValidations() { return failedValidations; }
        
        public double getSuccessRate() {
            return totalValidations > 0 ? (successfulValidations * 100.0) / totalValidations : 0.0;
        }
        
        @Override
        public String toString() {
            return String.format("DeviceValidationStats{device=%s, total=%d, success=%d, failed=%d, rate=%.2f%%}", 
                deviceId, totalValidations, successfulValidations, failedValidations, getSuccessRate());
        }
    }
    
    /**
     * 📊 Validation statistics class
     */
    public static class ValidationStatistics {
        private final long totalValidations;
        private final long successfulValidations;
        private final long failedValidations;
        private final long batchValidations;
        private final long suspiciousCoordinates;
        private final long timestampIssues;
        private final long ignitionIssues;
        private final long imeiIssues;
        private final long coordinateIssues;
        private final int activeDevices;
        
        public ValidationStatistics(long totalValidations, long successfulValidations, 
                                  long failedValidations, long batchValidations,
                                  long suspiciousCoordinates, long timestampIssues,
                                  long ignitionIssues, long imeiIssues, long coordinateIssues,
                                  int activeDevices) {
            this.totalValidations = totalValidations;
            this.successfulValidations = successfulValidations;
            this.failedValidations = failedValidations;
            this.batchValidations = batchValidations;
            this.suspiciousCoordinates = suspiciousCoordinates;
            this.timestampIssues = timestampIssues;
            this.ignitionIssues = ignitionIssues;
            this.imeiIssues = imeiIssues;
            this.coordinateIssues = coordinateIssues;
            this.activeDevices = activeDevices;
        }
        
        // Getters
        public long getTotalValidations() { return totalValidations; }
        public long getSuccessfulValidations() { return successfulValidations; }
        public long getFailedValidations() { return failedValidations; }
        public long getBatchValidations() { return batchValidations; }
        public long getSuspiciousCoordinates() { return suspiciousCoordinates; }
        public long getTimestampIssues() { return timestampIssues; }
        public long getIgnitionIssues() { return ignitionIssues; }
        public long getImeiIssues() { return imeiIssues; }
        public long getCoordinateIssues() { return coordinateIssues; }
        public int getActiveDevices() { return activeDevices; }
        
        public double getSuccessRate() {
            return totalValidations > 0 ? (successfulValidations * 100.0) / totalValidations : 0.0;
        }
        
        @Override
        public String toString() {
            return String.format("ValidationStatistics{total=%d, success=%d, failed=%d, rate=%.2f%%, devices=%d}", 
                totalValidations, successfulValidations, failedValidations, getSuccessRate(), activeDevices);
        }
    }
    
    /**
     * 📊 Validation rule class
     */
    public static class ValidationRule {
        private final String name;
        private final boolean critical;
        private final String description;
        private boolean enabled;
        
        public ValidationRule(String name, boolean critical, String description) {
            this.name = name;
            this.critical = critical;
            this.description = description;
            this.enabled = true;
        }
        
        // Getters and setters
        public String getName() { return name; }
        public boolean isCritical() { return critical; }
        public String getDescription() { return description; }
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        
        @Override
        public String toString() {
            return String.format("ValidationRule{name=%s, critical=%s, enabled=%s}", 
                name, critical, enabled);
        }
    }
}