package com.GpsTracker.Thinture.service.processing;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.GpsTracker.Thinture.dto.LocationUpdate;
import com.GpsTracker.Thinture.model.GpsData;
import com.GpsTracker.Thinture.model.Vehicle;
import com.GpsTracker.Thinture.model.VehicleHistory;
import com.GpsTracker.Thinture.model.VehicleLastLocation;
import com.GpsTracker.Thinture.service.VehicleService;
import com.GpsTracker.Thinture.service.persistence.BatchPersistenceService;
import com.GpsTracker.Thinture.service.persistence.LocationPersistenceService;
import com.GpsTracker.Thinture.service.websocket.LocationBroadcastService;
import com.GpsTracker.Thinture.service.mqtt.MqttMessageReceiver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataAccessException;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 🌟 ENHANCED GPS Data Processor - Production Ready for 5000+ Devices
 * ✅ Colorful, structured logging with emojis
 * ✅ Enhanced error handling and validation
 * ✅ Integration with enhanced MQTT architecture
 * ✅ Performance monitoring and metrics
 * ✅ Batch processing optimization
 * ✅ Real-time device tracking
 * ✅ Advanced alerting and notifications
 */
@Service
public class GpsDataProcessor {
    
    private static final Logger logger = LoggerFactory.getLogger(GpsDataProcessor.class);
    private static final Logger performanceLogger = LoggerFactory.getLogger("PERFORMANCE");
    private static final Logger metricsLogger = LoggerFactory.getLogger("METRICS");
    private static final Logger deviceLogger = LoggerFactory.getLogger("DEVICE_TRACKING");
    
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
    
    // 🔗 Enhanced dependencies
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private GpsDataValidator validator;
    
    @Autowired
    private DataTransformer transformer;
    
    @Autowired
    private VehicleService vehicleService;
    
    @Autowired
    private BatchPersistenceService batchPersistenceService;
    
    @Autowired
    private LocationPersistenceService locationPersistenceService;
    
    @Autowired
    private LocationBroadcastService broadcastService;
    
    // 📊 Configuration
    @Value("${gps.processing.batch-size:100}")
    private int batchSize;
    
    @Value("${gps.processing.max-retry-attempts:3}")
    private int maxRetryAttempts;
    
    @Value("${gps.processing.alert-threshold-speed:120.0}")
    private double alertThresholdSpeed;
    
    @Value("${gps.processing.device-timeout-minutes:30}")
    private int deviceTimeoutMinutes;
    
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    
    // 📊 Enhanced metrics tracking
    private final AtomicLong totalProcessed = new AtomicLong(0);
    private final AtomicLong successfullyProcessed = new AtomicLong(0);
    private final AtomicLong failedProcessed = new AtomicLong(0);
    private final AtomicLong validationFailures = new AtomicLong(0);
    private final AtomicLong ignitionStatusFixed = new AtomicLong(0);
    private final AtomicLong batchProcessed = new AtomicLong(0);
    private final AtomicLong alertsGenerated = new AtomicLong(0);
    
    // 📱 Device tracking
    private final Map<String, DeviceProcessingInfo> deviceTracker = new ConcurrentHashMap<>();
    private final Map<String, Long> deviceLastProcessed = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> deviceMessageCounts = new ConcurrentHashMap<>();
    
    // 🏃 Performance tracking
    private final Map<String, Long> processingTimes = new ConcurrentHashMap<>();
    private final AtomicLong totalProcessingTime = new AtomicLong(0);
    private final AtomicLong startTime = new AtomicLong(System.currentTimeMillis());
    
    @PostConstruct
    public void initialize() {
        logColorful("🌟 INITIALIZING ENHANCED GPS DATA PROCESSOR", BRIGHT_CYAN);
        logColorful("⏱️ Startup time: " + LocalDateTime.now().format(timeFormatter), CYAN);
        
        // 📊 Display configuration
        displayConfiguration();
        
        // 🔄 Initialize metrics
        initializeMetrics();
        
        logColorful("✅ Enhanced GPS Data Processor initialized successfully", BRIGHT_GREEN);
        displayWelcomeDashboard();
    }
    
    /**
     * 📊 Display processor configuration
     */
    private void displayConfiguration() {
        logColorful("", "");
        logColorful("╔════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════", BRIGHT_BLUE);
        logColorful("║ ⚙️ GPS DATA PROCESSOR CONFIGURATION", BRIGHT_BLUE);
        logColorful("╠════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════", BRIGHT_BLUE);
        logColorful("║ 📦 Batch Size: " + batchSize, BLUE);
        logColorful("║ 🔄 Max Retry Attempts: " + maxRetryAttempts, BLUE);
        logColorful("║ 🚨 Speed Alert Threshold: " + alertThresholdSpeed + " km/h", BLUE);
        logColorful("║ 📱 Device Timeout: " + deviceTimeoutMinutes + " minutes", BLUE);
        logColorful("║ 🎯 Target Capacity: 5000+ devices", BLUE);
        logColorful("║ 🏗️ Architecture: Enhanced Multi-Layer", BLUE);
        logColorful("╚════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════", BRIGHT_BLUE);
    }
    
    /**
     * 🔄 Initialize metrics
     */
    private void initializeMetrics() {
        startTime.set(System.currentTimeMillis());
        logColorful("📊 Metrics initialized at: " + LocalDateTime.now().format(timeFormatter), CYAN);
    }
    
    /**
     * 🎨 Display welcome dashboard
     */
    private void displayWelcomeDashboard() {
        logColorful("", "");
        logColorful("╔════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════", BRIGHT_MAGENTA);
        logColorful("║ 🎯 ENHANCED GPS DATA PROCESSOR DASHBOARD", BRIGHT_MAGENTA);
        logColorful("║ 🚀 Ready to process GPS data from 5000+ devices", BRIGHT_MAGENTA);
        logColorful("╠════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════", BRIGHT_MAGENTA);
        logColorful("║ 📊 Validation Engine: ACTIVE", BRIGHT_GREEN);
        logColorful("║ 🔄 Data Transformation: ACTIVE", BRIGHT_GREEN);
        logColorful("║ 📦 Batch Processing: ACTIVE", BRIGHT_GREEN);
        logColorful("║ 📡 Real-time Broadcasting: ACTIVE", BRIGHT_GREEN);
        logColorful("║ 🏥 Health Monitoring: ACTIVE", BRIGHT_GREEN);
        logColorful("║ 🚨 Alert System: ACTIVE", BRIGHT_GREEN);
        logColorful("╚════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════", BRIGHT_MAGENTA);
    }
    
    /**
     * 🚀 Main entry point for async GPS data processing from MQTT
     */
    @Async("gpsProcessingExecutor")
    public CompletableFuture<ProcessingResult> processPayloadAsync(String payload) {
        long startTime = System.currentTimeMillis();
        String requestId = generateRequestId();
        
        // 🔧 Set MDC for request tracking
        MDC.put("requestId", requestId);
        MDC.put("operation", "processPayload");
        
        try {
            logColorful("🚀 STARTING GPS PAYLOAD PROCESSING", BRIGHT_CYAN);
            logColorful("📥 Request ID: " + requestId, CYAN);
            logColorful("📊 Payload size: " + (payload != null ? payload.length() : 0) + " chars", CYAN);
            
            // 🔍 Enhanced payload validation
            if (payload == null || payload.trim().isEmpty()) {
                logColorful("🚫 INVALID PAYLOAD: null or empty", BRIGHT_RED);
                failedProcessed.incrementAndGet();
                return CompletableFuture.completedFuture(
                    new ProcessingResult(false, "Payload is null or empty", requestId)
                );
            }
            
            // 🧹 Clean the payload
            String cleanedPayload = transformer.cleanPayload(payload);
            logColorful("🧹 Payload cleaned: " + cleanedPayload.length() + " chars", BLUE);
            
            // 🔍 Validate cleaned payload
            if (cleanedPayload.isEmpty()) {
                logColorful("🚫 PAYLOAD EMPTY AFTER CLEANING", BRIGHT_RED);
                failedProcessed.incrementAndGet();
                return CompletableFuture.completedFuture(
                    new ProcessingResult(false, "Payload is empty after cleaning", requestId)
                );
            }
            
            // 🔧 Check if hex and convert
            if (transformer.isHexadecimal(cleanedPayload)) {
                logColorful("🔧 CONVERTING HEX TO ASCII", YELLOW);
                cleanedPayload = transformer.hexToAscii(cleanedPayload);
                logColorful("✅ Hex conversion complete: " + cleanedPayload.length() + " chars", GREEN);
            }
            
            // 📋 Parse JSON
            ProcessingResult result;
            if (cleanedPayload.trim().startsWith("[")) {
                // 📦 Batch processing
                logColorful("📦 BATCH PROCESSING DETECTED", BRIGHT_BLUE);
                List<GpsData> gpsDataList = objectMapper.readValue(
                    cleanedPayload, 
                    new TypeReference<List<GpsData>>() {}
                );
                
                if (gpsDataList.isEmpty()) {
                    logColorful("📦 EMPTY BATCH RECEIVED", BRIGHT_YELLOW);
                    return CompletableFuture.completedFuture(
                        new ProcessingResult(false, "Empty batch received", requestId)
                    );
                }
                
                MDC.put("batchSize", String.valueOf(gpsDataList.size()));
                logColorful("📦 Batch size: " + gpsDataList.size() + " records", BRIGHT_BLUE);
                result = processBatch(gpsDataList, requestId);
                
            } else {
                // 📄 Single record processing
                logColorful("📄 SINGLE RECORD PROCESSING", BRIGHT_GREEN);
                GpsData gpsData = objectMapper.readValue(cleanedPayload, GpsData.class);
                
                if (gpsData == null) {
                    logColorful("📄 PARSED GPS DATA IS NULL", BRIGHT_RED);
                    return CompletableFuture.completedFuture(
                        new ProcessingResult(false, "Parsed GPS data is null", requestId)
                    );
                }
                
                MDC.put("deviceId", gpsData.getDeviceID());
                logColorful("📱 Device ID: " + gpsData.getDeviceID(), GREEN);
                result = processSingle(gpsData, requestId);
            }
            
            // 📊 Calculate processing time
            long processingTime = System.currentTimeMillis() - startTime;
            result.setProcessingTimeMs(processingTime);
            result.setRequestId(requestId);
            
            // 📈 Update performance metrics
            totalProcessingTime.addAndGet(processingTime);
            processingTimes.put(requestId, processingTime);
            
            // 🐌 Performance monitoring
            if (processingTime > 1000) {
                logColorful("🐌 SLOW PROCESSING WARNING: " + processingTime + "ms", BRIGHT_YELLOW);
                performanceLogger.warn("Slow processing detected: {}ms for {} records [Request: {}]", 
                    processingTime, result.getTotalRecords(), requestId);
            } else {
                logColorful("⚡ PROCESSING COMPLETED: " + processingTime + "ms", BRIGHT_GREEN);
                performanceLogger.debug("Processing completed in {}ms [Request: {}]", processingTime, requestId);
            }
            
            // 📊 Log final result
            if (result.isSuccess()) {
                logColorful("✅ GPS PAYLOAD PROCESSING SUCCESSFUL", BRIGHT_GREEN);
                logColorful("📊 Records processed: " + result.getTotalRecords(), GREEN);
                logColorful("⏱️ Processing time: " + processingTime + "ms", GREEN);
            } else {
                logColorful("❌ GPS PAYLOAD PROCESSING FAILED", BRIGHT_RED);
                logColorful("💥 Error: " + result.getMessage(), RED);
            }
            
            return CompletableFuture.completedFuture(result);
            
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            logColorful("🔴 JSON PARSING ERROR", BRIGHT_RED);
            logColorful("💥 Details: " + e.getMessage(), RED);
            logger.error("JSON parsing error for GPS payload [Request: {}]: {}", requestId, e.getMessage());
            failedProcessed.incrementAndGet();
            return CompletableFuture.completedFuture(
                new ProcessingResult(false, "JSON parsing failed: " + e.getMessage(), requestId)
            );
        } catch (Exception e) {
            logColorful("🔴 PROCESSING ERROR", BRIGHT_RED);
            logColorful("💥 Exception: " + e.getClass().getSimpleName(), RED);
            logColorful("💥 Message: " + e.getMessage(), RED);
            logger.error("Failed to process GPS payload [Request: {}]", requestId, e);
            failedProcessed.incrementAndGet();
            
            return CompletableFuture.completedFuture(
                new ProcessingResult(false, "Processing failed: " + e.getMessage(), requestId)
            );
        } finally {
            // 🧹 Clear MDC
            MDC.clear();
        }
    }
    
    /**
     * 📦 Process batch of GPS data with enhanced logging
     */
    @Async("gpsProcessingExecutor")
    public CompletableFuture<ProcessingResult> processBatchAsync(List<MqttMessageReceiver.GpsMessage> batchMessages) {
        String requestId = generateRequestId();
        
        try {
            logColorful("📦 PROCESSING BATCH FROM MQTT RECEIVER", BRIGHT_BLUE);
            logColorful("📥 Request ID: " + requestId, CYAN);
            logColorful("📊 Batch size: " + batchMessages.size(), BRIGHT_BLUE);
            
            // 🔄 Convert to GpsData objects
            List<GpsData> gpsDataList = new ArrayList<>();
            for (MqttMessageReceiver.GpsMessage message : batchMessages) {
                try {
                    GpsData gpsData = objectMapper.readValue(message.getPayload(), GpsData.class);
                    gpsDataList.add(gpsData);
                } catch (Exception e) {
                    logColorful("⚠️ Failed to parse message from device: " + message.getDeviceId(), YELLOW);
                    logger.warn("Failed to parse message from device: {}", message.getDeviceId(), e);
                }
            }
            
            if (gpsDataList.isEmpty()) {
                logColorful("📦 NO VALID MESSAGES IN BATCH", BRIGHT_YELLOW);
                return CompletableFuture.completedFuture(
                    new ProcessingResult(false, "No valid messages in batch", requestId)
                );
            }
            
            // 📊 Process the batch
            ProcessingResult result = processBatch(gpsDataList, requestId);
            batchProcessed.incrementAndGet();
            
            logColorful("✅ BATCH PROCESSING COMPLETED", BRIGHT_GREEN);
            logColorful("📊 Success: " + result.getSuccessfulRecords() + "/" + result.getTotalRecords(), GREEN);
            
            return CompletableFuture.completedFuture(result);
            
        } catch (Exception e) {
            logColorful("❌ BATCH PROCESSING ERROR", BRIGHT_RED);
            logColorful("💥 Error: " + e.getMessage(), RED);
            logger.error("Batch processing error [Request: {}]", requestId, e);
            
            return CompletableFuture.completedFuture(
                new ProcessingResult(false, "Batch processing failed: " + e.getMessage(), requestId)
            );
        }
    }
    
    /**
     * 📄 Process single GPS data record with enhanced logging
     */
    @Transactional(rollbackFor = Exception.class)
    private ProcessingResult processSingle(GpsData gpsData, String requestId) {
        totalProcessed.incrementAndGet();
        
        try {
            logColorful("📄 PROCESSING SINGLE GPS RECORD", BRIGHT_GREEN);
            logColorful("📱 Device ID: " + gpsData.getDeviceID(), GREEN);
            logColorful("📍 Location: " + gpsData.getLatitude() + ", " + gpsData.getLongitude(), GREEN);
            logColorful("⏱️ Timestamp: " + gpsData.getTimestamp(), GREEN);
            
            // 🔍 Enhanced validation
            logColorful("🔍 VALIDATING GPS DATA", BLUE);
            GpsDataValidator.ValidationResult validationResult = validator.validate(gpsData);
            
            if (!validationResult.isValid()) {
                validationFailures.incrementAndGet();
                String errorMsg = String.join(", ", validationResult.getErrors());
                logColorful("❌ VALIDATION FAILED", BRIGHT_RED);
                logColorful("💥 Errors: " + errorMsg, RED);
                return new ProcessingResult(false, "Validation failed: " + errorMsg, requestId);
            }
            
            logColorful("✅ VALIDATION SUCCESSFUL", BRIGHT_GREEN);
            
            // 🔍 Enhanced vehicle lookup
            logColorful("🔍 LOOKING UP VEHICLE", BLUE);
            String imei = gpsData.getImei();
            if (imei == null || imei.trim().isEmpty()) {
                logColorful("🚫 IMEI IS NULL OR EMPTY", BRIGHT_RED);
                return new ProcessingResult(false, "IMEI is null or empty", requestId);
            }
            
            logColorful("📱 IMEI: " + imei, CYAN);
            Optional<Vehicle> vehicleOpt = vehicleService.getVehicleByImei(imei);
            
            if (vehicleOpt.isEmpty()) {
                logColorful("🚫 VEHICLE NOT FOUND", BRIGHT_RED);
                logColorful("📱 IMEI: " + imei, RED);
                return new ProcessingResult(false, "No vehicle found with IMEI: " + imei, requestId);
            }
            
            Vehicle vehicle = vehicleOpt.get();
            logColorful("✅ VEHICLE FOUND", BRIGHT_GREEN);
            logColorful("🚗 Vehicle: " + vehicle.getVehicleNumber(), GREEN);
            logColorful("🏷️ Vehicle ID: " + vehicle.getId(), GREEN);
            
            // 🔍 Device validation
            if (!validateAndUpdateDeviceId(vehicle, gpsData, requestId)) {
                logColorful("🚫 DEVICE ID VALIDATION FAILED", BRIGHT_RED);
                return new ProcessingResult(false, "Device ID mismatch for IMEI: " + imei, requestId);
            }
            
            // 🔄 Transform data
            logColorful("🔄 TRANSFORMING DATA", BLUE);
            String originalIgnition = gpsData.getIgnition();
            
            VehicleHistory history = transformer.toVehicleHistory(gpsData, vehicle);
            VehicleLastLocation lastLocation = transformer.toVehicleLastLocation(gpsData);
            LocationUpdate locationUpdate = transformer.toLocationUpdate(gpsData);
            
            // 🔧 Track ignition fixes
            if (originalIgnition != null && originalIgnition.equalsIgnoreCase("IGon")) {
                ignitionStatusFixed.incrementAndGet();
                logColorful("🔧 IGNITION STATUS FIXED: " + originalIgnition + " -> " + history.getIgnition(), BRIGHT_YELLOW);
            }
            
            logColorful("✅ DATA TRANSFORMATION COMPLETE", BRIGHT_GREEN);
            
            // 💾 Persist data
            logColorful("💾 PERSISTING DATA", BLUE);
            batchPersistenceService.addToBatch(history);
            locationPersistenceService.updateLastLocation(lastLocation);
            logColorful("✅ DATA PERSISTED", BRIGHT_GREEN);
            
            // 📡 Broadcast update
            logColorful("📡 BROADCASTING UPDATE", BLUE);
            broadcastService.broadcastLocationUpdate(locationUpdate);
            logColorful("✅ UPDATE BROADCASTED", BRIGHT_GREEN);
            
            // 📊 Update device tracking
            updateDeviceTracking(gpsData, requestId);
            
            // 🚨 Check for alerts
            checkForAlerts(gpsData, requestId);
            
            successfullyProcessed.incrementAndGet();
            
            logColorful("✅ SINGLE RECORD PROCESSING COMPLETE", BRIGHT_GREEN);
            logColorful("📊 Total processed: " + totalProcessed.get(), GREEN);
            
            return new ProcessingResult(true, "Success", 1, 1, 0, requestId);
            
        } catch (DataAccessException e) {
            logColorful("🔴 DATABASE ERROR", BRIGHT_RED);
            logColorful("💥 Details: " + e.getMessage(), RED);
            logger.error("Database error processing GPS data for device: {} [Request: {}]", 
                gpsData.getDeviceID(), requestId, e);
            failedProcessed.incrementAndGet();
            return new ProcessingResult(false, "Database error: " + e.getMessage(), requestId);
        } catch (Exception e) {
            logColorful("🔴 PROCESSING ERROR", BRIGHT_RED);
            logColorful("💥 Exception: " + e.getClass().getSimpleName(), RED);
            logColorful("💥 Message: " + e.getMessage(), RED);
            logger.error("Error processing GPS data for device: {} [Request: {}]", 
                gpsData.getDeviceID(), requestId, e);
            failedProcessed.incrementAndGet();
            return new ProcessingResult(false, "Processing error: " + e.getMessage(), requestId);
        }
    }
    
    /**
     * 📦 Process batch of GPS data records
     */
    private ProcessingResult processBatch(List<GpsData> gpsDataList, String requestId) {
        if (gpsDataList == null || gpsDataList.isEmpty()) {
            logColorful("🚫 NULL OR EMPTY BATCH", BRIGHT_RED);
            return new ProcessingResult(false, "GPS data list is null or empty", 0, 0, 0, requestId);
        }
        
        int totalRecords = gpsDataList.size();
        int successCount = 0;
        int failureCount = 0;
        
        logColorful("📦 PROCESSING BATCH", BRIGHT_BLUE);
        logColorful("📊 Total records: " + totalRecords, BRIGHT_BLUE);
        
        // 🔍 Batch validation
        logColorful("🔍 VALIDATING ENTIRE BATCH", BLUE);
        GpsDataValidator.BatchValidationResult batchValidation = validator.validateBatch(gpsDataList);
        
        if (batchValidation.getInvalidRecords() > 0) {
            logColorful("⚠️ BATCH CONTAINS INVALID RECORDS", BRIGHT_YELLOW);
            logColorful("📊 Invalid: " + batchValidation.getInvalidRecords() + "/" + totalRecords, YELLOW);
            
            // 📋 Log validation errors
            for (GpsDataValidator.ValidationError error : batchValidation.getErrors()) {
                logColorful("❌ Validation error at index " + error.getRecordIndex() + 
                           " for device " + error.getDeviceId() + ": " + 
                           String.join(", ", error.getErrors()), RED);
            }
        }
        
        // 🔄 Process individual records
        logColorful("🔄 PROCESSING INDIVIDUAL RECORDS", BLUE);
        for (int i = 0; i < gpsDataList.size(); i++) {
            final int index = i;
            GpsData gpsData = gpsDataList.get(i);
            
            if (gpsData == null) {
                logColorful("🚫 NULL GPS DATA AT INDEX " + i, BRIGHT_RED);
                failureCount++;
                continue;
            }
            
            // 🔍 Skip if validation failed
            boolean isInvalid = batchValidation.getErrors().stream()
                .anyMatch(error -> error.getRecordIndex() == index);
            
            if (isInvalid) {
                failureCount++;
                validationFailures.incrementAndGet();
                continue;
            }
            
            // 🔄 Process the record
            ProcessingResult result = processSingle(gpsData, requestId + "-" + i);
            
            if (result.isSuccess()) {
                successCount++;
                logColorful("✅ Record " + (i + 1) + " processed successfully", GREEN);
            } else {
                failureCount++;
                logColorful("❌ Record " + (i + 1) + " failed: " + result.getMessage(), RED);
            }
            
            // 📊 Progress logging for large batches
            if (totalRecords > 100 && (i + 1) % 50 == 0) {
                logColorful("📊 BATCH PROGRESS: " + (i + 1) + "/" + totalRecords + " records processed", BRIGHT_BLUE);
            }
        }
        
        // 📈 Final batch summary
        logColorful("📈 BATCH PROCESSING COMPLETE", BRIGHT_GREEN);
        logColorful("✅ Success: " + successCount, GREEN);
        logColorful("❌ Failed: " + failureCount, failureCount > 0 ? RED : GREEN);
        logColorful("📊 Total: " + totalRecords, BLUE);
        
        return new ProcessingResult(
            failureCount == 0, 
            String.format("Processed %d/%d records successfully", successCount, totalRecords),
            totalRecords,
            successCount,
            failureCount,
            requestId
        );
    }
    
    /**
     * 🔍 Validate and update device ID
     */
    private boolean validateAndUpdateDeviceId(Vehicle vehicle, GpsData gpsData, String requestId) {
        if (vehicle == null || gpsData == null || gpsData.getDeviceID() == null) {
            logColorful("🚫 INVALID INPUT FOR DEVICE ID VALIDATION", BRIGHT_RED);
            return false;
        }
        
        String incomingDeviceId = gpsData.getDeviceID().trim();
        String existingDeviceId = vehicle.getDeviceID();
        
        if (incomingDeviceId.isEmpty()) {
            logColorful("🚫 EMPTY DEVICE ID", BRIGHT_RED);
            return false;
        }
        
        if (existingDeviceId == null || existingDeviceId.trim().isEmpty()) {
            // 🆕 First time registration
            try {
                vehicle.setDeviceID(incomingDeviceId);
                vehicleService.save(vehicle);
                logColorful("🆕 REGISTERED NEW DEVICE ID: " + incomingDeviceId, BRIGHT_GREEN);
                logColorful("📱 For IMEI: " + vehicle.getImei(), GREEN);
                return true;
            } catch (Exception e) {
                logColorful("🔴 FAILED TO SAVE DEVICE ID", BRIGHT_RED);
                logColorful("💥 Error: " + e.getMessage(), RED);
                return false;
            }
        }
        
        // 🔍 Validate existing device ID
        if (!incomingDeviceId.equalsIgnoreCase(existingDeviceId.trim())) {
            logColorful("⚠️ DEVICE ID MISMATCH", BRIGHT_YELLOW);
            logColorful("📱 Expected: " + existingDeviceId, YELLOW);
            logColorful("📱 Received: " + incomingDeviceId, YELLOW);
            logColorful("📱 IMEI: " + vehicle.getImei(), YELLOW);
            return false;
        }
        
        logColorful("✅ DEVICE ID VALIDATION SUCCESSFUL", BRIGHT_GREEN);
        return true;
    }
    
    /**
     * 📊 Update device tracking
     */
    private void updateDeviceTracking(GpsData gpsData, String requestId) {
        String deviceId = gpsData.getDeviceID();
        long currentTime = System.currentTimeMillis();
        
        // 📊 Update device info
        deviceTracker.compute(deviceId, (id, existing) -> {
            if (existing == null) {
                logColorful("🆕 NEW DEVICE DETECTED: " + deviceId, BRIGHT_GREEN);
                deviceLogger.info("New device detected: {} [Request: {}]", deviceId, requestId);
                return new DeviceProcessingInfo(deviceId, currentTime);
            } else {
                existing.updateActivity(currentTime);
                return existing;
            }
        });
        
        // 📊 Update message count
        deviceMessageCounts.computeIfAbsent(deviceId, k -> new AtomicLong(0)).incrementAndGet();
        deviceLastProcessed.put(deviceId, currentTime);
        
        // 📊 Log device activity
        long messageCount = deviceMessageCounts.get(deviceId).get();
        if (messageCount % 100 == 0) {
            logColorful("📊 DEVICE MILESTONE: " + deviceId + " reached " + messageCount + " messages", BRIGHT_CYAN);
            deviceLogger.info("Device {} reached {} messages [Request: {}]", deviceId, messageCount, requestId);
        }
    }
    
    /**
     * 🚨 Check for alerts
     */
    private void checkForAlerts(GpsData gpsData, String requestId) {
        try {
            List<String> alerts = new ArrayList<>();
            
            // 🏎️ Speed alert
            if (gpsData.getSpeed() != null && !gpsData.getSpeed().isEmpty()) {
                double speed = Double.parseDouble(gpsData.getSpeed());
                if (speed > alertThresholdSpeed) {
                    alerts.add("SPEED_ALERT: " + speed + " km/h (threshold: " + alertThresholdSpeed + ")");
                }
            }
            
            // 🔥 Ignition alert (outside hours)
            if ("ON".equalsIgnoreCase(gpsData.getIgnition()) && isOutsideOperatingHours()) {
                alerts.add("IGNITION_ALERT: Vehicle started outside operating hours");
            }
            
            // 🚨 Process alerts
            if (!alerts.isEmpty()) {
                alertsGenerated.incrementAndGet();
                logColorful("🚨 ALERTS GENERATED FOR DEVICE: " + gpsData.getDeviceID(), BRIGHT_RED);
                alerts.forEach(alert -> {
                    logColorful("🚨 " + alert, BRIGHT_RED);
                    logger.warn("Alert generated for device {}: {} [Request: {}]", 
                        gpsData.getDeviceID(), alert, requestId);
                });
            }
            
        } catch (Exception e) {
            logColorful("❌ ERROR CHECKING ALERTS", BRIGHT_RED);
            logger.error("Error checking alerts for device: {} [Request: {}]", gpsData.getDeviceID(), requestId, e);
        }
    }
    
    /**
     * 🕒 Check if outside operating hours
     */
    private boolean isOutsideOperatingHours() {
        int currentHour = java.time.LocalTime.now().getHour();
        return currentHour < 6 || currentHour >= 22;
    }
    
    /**
     * 📊 Scheduled statistics reporting
     */
    @Scheduled(fixedRate = 60000) // Every minute
    public void reportStatistics() {
        try {
            long currentTime = System.currentTimeMillis();
            long uptime = currentTime - startTime.get();
            
            // 📊 Calculate rates
            double processingRate = uptime > 0 ? (totalProcessed.get() * 60000.0) / uptime : 0;
            double successRate = totalProcessed.get() > 0 ? 
                (successfullyProcessed.get() * 100.0) / totalProcessed.get() : 0;
            
            // 📊 Active devices
            int activeDevices = (int) deviceLastProcessed.entrySet().stream()
                .filter(entry -> (currentTime - entry.getValue()) < (deviceTimeoutMinutes * 60000L))
                .count();
            
            // 🎨 Display statistics dashboard
            displayStatisticsDashboard(processingRate, successRate, activeDevices, uptime);
            
            // 📊 Log to metrics logger
            metricsLogger.info("Processing Stats - Rate: {:.2f}/min, Success: {:.2f}%, Active Devices: {}, Uptime: {}min",
                processingRate, successRate, activeDevices, uptime / 60000);
            
        } catch (Exception e) {
            logColorful("❌ ERROR REPORTING STATISTICS", BRIGHT_RED);
            logger.error("Error reporting statistics", e);
        }
    }
    
    /**
     * 📊 Display statistics dashboard
     */
    private void displayStatisticsDashboard(double processingRate, double successRate, int activeDevices, long uptime) {
        logColorful("", "");
        logColorful("╔════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════", BRIGHT_CYAN);
        logColorful("║ 📊 GPS DATA PROCESSOR STATISTICS", BRIGHT_CYAN);
        logColorful("║ 🕒 " + LocalDateTime.now().format(timeFormatter), BRIGHT_CYAN);
        logColorful("╠════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════", BRIGHT_CYAN);
        logColorful("║ 📈 Processing Rate: " + String.format("%.2f", processingRate) + " records/min", BRIGHT_GREEN);
        logColorful("║ ✅ Success Rate: " + String.format("%.2f", successRate) + "%", successRate > 95 ? BRIGHT_GREEN : BRIGHT_YELLOW);
        logColorful("║ 📊 Total Processed: " + totalProcessed.get(), BLUE);
        logColorful("║ ✅ Successful: " + successfullyProcessed.get(), GREEN);
        logColorful("║ ❌ Failed: " + failedProcessed.get(), failedProcessed.get() > 0 ? RED : GREEN);
        logColorful("║ 🔍 Validation Failures: " + validationFailures.get(), validationFailures.get() > 0 ? YELLOW : GREEN);
        logColorful("║ 🔧 Ignition Fixed: " + ignitionStatusFixed.get(), BRIGHT_YELLOW);
        logColorful("║ 📦 Batches Processed: " + batchProcessed.get(), PURPLE);
        logColorful("║ 🚨 Alerts Generated: " + alertsGenerated.get(), alertsGenerated.get() > 0 ? BRIGHT_RED : GREEN);
        logColorful("║ 📱 Active Devices: " + activeDevices, BRIGHT_CYAN);
        logColorful("║ ⏱️ Uptime: " + formatDuration(uptime), CYAN);
        
        // 📊 Performance metrics
        long avgProcessingTime = totalProcessed.get() > 0 ? 
            totalProcessingTime.get() / totalProcessed.get() : 0;
        logColorful("║ ⚡ Avg Processing Time: " + avgProcessingTime + "ms", BRIGHT_BLUE);
        
        logColorful("╚════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════", BRIGHT_CYAN);
    }
    
    /**
     * 📊 Get enhanced processing metrics
     */
    public EnhancedProcessingMetrics getEnhancedMetrics() {
        long currentTime = System.currentTimeMillis();
        long uptime = currentTime - startTime.get();
        
        // 📊 Calculate rates
        double processingRate = uptime > 0 ? (totalProcessed.get() * 60000.0) / uptime : 0;
        double successRate = totalProcessed.get() > 0 ? 
            (successfullyProcessed.get() * 100.0) / totalProcessed.get() : 0;
        
        // 📊 Active devices
        int activeDevices = (int) deviceLastProcessed.entrySet().stream()
            .filter(entry -> (currentTime - entry.getValue()) < (deviceTimeoutMinutes * 60000L))
            .count();
        
        // 📊 Top devices
        Map<String, Long> topDevices = deviceMessageCounts.entrySet().stream()
            .sorted(Map.Entry.<String, AtomicLong>comparingByValue((a, b) -> Long.compare(b.get(), a.get())))
            .limit(10)
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> e.getValue().get(),
                (e1, e2) -> e1,
                LinkedHashMap::new
            ));
        
        EnhancedProcessingMetrics metrics = new EnhancedProcessingMetrics(
            totalProcessed.get(),
            successfullyProcessed.get(),
            failedProcessed.get(),
            validationFailures.get(),
            ignitionStatusFixed.get(),
            batchProcessed.get(),
            alertsGenerated.get(),
            activeDevices,
            processingRate,
            successRate,
            uptime,
            topDevices
        );
        
        metricsLogger.info("Enhanced Processing Metrics: {}", metrics.toString());
        return metrics;
    }
    
    /**
     * 🔄 Reset metrics
     */
    public void resetMetrics() {
        totalProcessed.set(0);
        successfullyProcessed.set(0);
        failedProcessed.set(0);
        validationFailures.set(0);
        ignitionStatusFixed.set(0);
        batchProcessed.set(0);
        alertsGenerated.set(0);
        totalProcessingTime.set(0);
        startTime.set(System.currentTimeMillis());
        
        deviceTracker.clear();
        deviceLastProcessed.clear();
        deviceMessageCounts.clear();
        processingTimes.clear();
        
        logColorful("🔄 METRICS RESET COMPLETE", BRIGHT_GREEN);
        logger.info("GPS processing metrics reset");
        metricsLogger.info("Metrics reset - all counters zeroed");
    }
    
    /**
     * 🆔 Generate unique request ID
     */
    private String generateRequestId() {
        return "GPS-" + System.currentTimeMillis() + "-" + Thread.currentThread().getId();
    }
    
    /**
     * ⏱️ Format duration
     */
    private String formatDuration(long millis) {
        if (millis < 1000) return millis + "ms";
        if (millis < 60000) return String.format("%.1fs", millis / 1000.0);
        if (millis < 3600000) return String.format("%.1fm", millis / 60000.0);
        if (millis < 86400000) return String.format("%.1fh", millis / 3600000.0);
        return String.format("%.1fd", millis / 86400000.0);
    }
    
    /**
     * 🎨 Colorful logging utility
     */
    private void logColorful(String message, String color) {
        System.out.println(color + message + RESET);
        logger.info(message.replaceAll("║", "").replaceAll("╔", "").replaceAll("╚", "").replaceAll("╠", "").replaceAll("═", "").trim());
    }
    
    // 📊 Data classes
    public static class EnhancedProcessingMetrics {
        private final long totalProcessed;
        private final long successfullyProcessed;
        private final long failedProcessed;
        private final long validationFailures;
        private final long ignitionStatusFixed;
        private final long batchProcessed;
        private final long alertsGenerated;
        private final int activeDevices;
        private final double processingRate;
        private final double successRate;
        private final long uptime;
        private final Map<String, Long> topDevices;
        
        public EnhancedProcessingMetrics(long totalProcessed, long successfullyProcessed, 
                                       long failedProcessed, long validationFailures, 
                                       long ignitionStatusFixed, long batchProcessed,
                                       long alertsGenerated, int activeDevices,
                                       double processingRate, double successRate,
                                       long uptime, Map<String, Long> topDevices) {
            this.totalProcessed = totalProcessed;
            this.successfullyProcessed = successfullyProcessed;
            this.failedProcessed = failedProcessed;
            this.validationFailures = validationFailures;
            this.ignitionStatusFixed = ignitionStatusFixed;
            this.batchProcessed = batchProcessed;
            this.alertsGenerated = alertsGenerated;
            this.activeDevices = activeDevices;
            this.processingRate = processingRate;
            this.successRate = successRate;
            this.uptime = uptime;
            this.topDevices = topDevices;
        }
        
        // Getters
        public long getTotalProcessed() { return totalProcessed; }
        public long getSuccessfullyProcessed() { return successfullyProcessed; }
        public long getFailedProcessed() { return failedProcessed; }
        public long getValidationFailures() { return validationFailures; }
        public long getIgnitionStatusFixed() { return ignitionStatusFixed; }
        public long getBatchProcessed() { return batchProcessed; }
        public long getAlertsGenerated() { return alertsGenerated; }
        public int getActiveDevices() { return activeDevices; }
        public double getProcessingRate() { return processingRate; }
        public double getSuccessRate() { return successRate; }
        public long getUptime() { return uptime; }
        public Map<String, Long> getTopDevices() { return topDevices; }
        
        @Override
        public String toString() {
            return String.format("EnhancedMetrics{total=%d, success=%d, failed=%d, validationFailed=%d, " +
                               "ignitionFixed=%d, batches=%d, alerts=%d, activeDevices=%d, " +
                               "rate=%.2f/min, successRate=%.2f%%, uptime=%dms}", 
                totalProcessed, successfullyProcessed, failedProcessed, validationFailures,
                ignitionStatusFixed, batchProcessed, alertsGenerated, activeDevices,
                processingRate, successRate, uptime);
        }
    }
    
    public static class DeviceProcessingInfo {
        private final String deviceId;
        private final long firstSeen;
        private long lastSeen;
        private long messageCount;
        private long totalProcessingTime;
        
        public DeviceProcessingInfo(String deviceId, long firstSeen) {
            this.deviceId = deviceId;
            this.firstSeen = firstSeen;
            this.lastSeen = firstSeen;
            this.messageCount = 1;
            this.totalProcessingTime = 0;
        }
        
        public void updateActivity(long currentTime) {
            this.lastSeen = currentTime;
            this.messageCount++;
        }
        
        // Getters
        public String getDeviceId() { return deviceId; }
        public long getFirstSeen() { return firstSeen; }
        public long getLastSeen() { return lastSeen; }
        public long getMessageCount() { return messageCount; }
        public long getTotalProcessingTime() { return totalProcessingTime; }
    }
    
    // Keep existing ProcessingResult class from your original code
    public static class ProcessingResult {
        private final boolean success;
        private final String message;
        private final int totalRecords;
        private final int successfulRecords;
        private final int failedRecords;
        private long processingTimeMs;
        private String requestId;
        
        public ProcessingResult(boolean success, String message, String requestId) {
            this(success, message, 1, success ? 1 : 0, success ? 0 : 1, requestId);
        }
        
        public ProcessingResult(boolean success, String message, 
                              int totalRecords, int successfulRecords, int failedRecords, String requestId) {
            this.success = success;
            this.message = message;
            this.totalRecords = totalRecords;
            this.successfulRecords = successfulRecords;
            this.failedRecords = failedRecords;
            this.requestId = requestId;
        }
        
        // Getters and setters
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public int getTotalRecords() { return totalRecords; }
        public int getSuccessfulRecords() { return successfulRecords; }
        public int getFailedRecords() { return failedRecords; }
        public long getProcessingTimeMs() { return processingTimeMs; }
        public void setProcessingTimeMs(long processingTimeMs) { 
            this.processingTimeMs = processingTimeMs; 
        }
        public String getRequestId() { return requestId; }
        public void setRequestId(String requestId) { this.requestId = requestId; }
        
        @Override
        public String toString() {
            return String.format("ProcessingResult{success=%s, totalRecords=%d, successfulRecords=%d, " +
                               "failedRecords=%d, processingTimeMs=%d, requestId='%s'}", 
                success, totalRecords, successfulRecords, failedRecords, processingTimeMs, requestId);
        }
    }
}