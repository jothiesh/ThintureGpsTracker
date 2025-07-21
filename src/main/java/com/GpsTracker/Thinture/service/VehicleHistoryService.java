// ================================================================================================
// VehicleHistoryService.java - FIXED FOR SIMPLIFIED ENTITY
// ================================================================================================

package com.GpsTracker.Thinture.service;

import com.GpsTracker.Thinture.dto.DeviceLocation;
import com.GpsTracker.Thinture.dto.PanicAlertDTO;
import com.GpsTracker.Thinture.model.VehicleHistory;
import com.GpsTracker.Thinture.repository.VehicleHistoryRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * ✅ FIXED: Service for simplified VehicleHistory entity with auto-generated ID
 * - Fixed all method calls to match simplified entity structure
 * - Removed references to non-existent fields
 * - Updated upsert logic for simple ID approach
 */
@Service
@Transactional
public class VehicleHistoryService {

    private static final Logger logger = LoggerFactory.getLogger(VehicleHistoryService.class);
    private static final double EARTH_RADIUS = 6371; // Earth radius in kilometers
    
    @Autowired
    private VehicleHistoryRepository vehicleHistoryRepository;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ═══════════════════════════════════════════════════════════════════════════════════
    // ✅ FIXED UPSERT METHODS - Updated for simplified entity
    // ═══════════════════════════════════════════════════════════════════════════════════

    /**
     * ✅ FIXED: Process GPS data with proper upsert logic (prevents duplicates)
     */
    public void processGpsDataWithUpsert(String gpsDataJson) {
        logger.info("🔄 Processing GPS data with upsert: {}", gpsDataJson);
        
        try {
            // Parse GPS data - handle both single record and array
            List<VehicleHistory> records = parseGpsData(gpsDataJson);
            
            for (VehicleHistory record : records) {
                upsertVehicleHistory(record);
            }
            
            logger.info("✅ Successfully processed {} GPS records with upsert", records.size());
            
        } catch (Exception e) {
            logger.error("❌ Error processing GPS data with upsert: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process GPS data", e);
        }
    }

    /**
     * ✅ FIXED: Upsert single vehicle history record (prevents duplicates)
     */
    @Transactional
    public VehicleHistory upsertVehicleHistory(VehicleHistory history) {
        if (history == null || history.getDevice_id() == null) {
            logger.warn("⚠️ Cannot upsert null history or null device ID");
            return null;
        }
        
        String deviceId = history.getDevice_id();
        Timestamp timestamp = history.getTimestamp();
        
        logger.debug("🔄 Upserting record for device: {} at {}", deviceId, timestamp);
        
        try {
            // ✅ FIXED: Find existing record by device_id and timestamp
            Optional<VehicleHistory> existingOpt = vehicleHistoryRepository
                .findByDevice_idAndTimestamp(deviceId, timestamp);
            
            if (existingOpt.isPresent()) {
                // Update existing record
                VehicleHistory existing = existingOpt.get();
                updateExistingRecord(existing, history);
                
                VehicleHistory saved = vehicleHistoryRepository.save(existing);
                logger.debug("✅ Updated existing record for device: {}", deviceId);
                return saved;
                
            } else {
                // Create new record
                VehicleHistory saved = vehicleHistoryRepository.save(history);
                logger.debug("✅ Created new record for device: {}", deviceId);
                return saved;
            }
            
        } catch (DataIntegrityViolationException e) {
            // Handle race condition - try to update existing record
            logger.warn("⚠️ Duplicate key detected for device: {}, attempting update", deviceId);
            return handleDuplicateKey(history);
            
        } catch (Exception e) {
            logger.error("❌ Error upserting record for device: {} - {}", deviceId, e.getMessage(), e);
            throw new RuntimeException("Failed to upsert vehicle history", e);
        }
    }

    /**
     * ✅ FIXED: Batch upsert method (prevents duplicates in batch)
     */
    @Transactional
    public void upsertGpsBatch(List<VehicleHistory> histories) {
        if (histories == null || histories.isEmpty()) {
            logger.warn("⚠️ Cannot upsert empty batch");
            return;
        }
        
        logger.info("🔄 Upserting batch of {} records", histories.size());
        
        try {
            // Use MySQL-specific ON DUPLICATE KEY UPDATE for better performance
            upsertBatchWithMySQL(histories);
            
        } catch (Exception e) {
            logger.error("❌ Batch upsert failed, falling back to individual upserts", e);
            
            // Fallback to individual upserts
            for (VehicleHistory history : histories) {
                try {
                    upsertVehicleHistory(history);
                } catch (Exception ex) {
                    logger.error("❌ Failed to upsert individual record for device: {}", 
                               history.getDevice_id(), ex);
                }
            }
        }
    }

    /**
     * ✅ FIXED: Update live location (N1 status) - only one live record per device
     */
    @Transactional
    public VehicleHistory updateLiveLocation(String deviceId, Double latitude, Double longitude, Double speed) {
        logger.info("📍 Updating live location for device: {}", deviceId);
        
        try {
            // Find existing live record
            Optional<VehicleHistory> existingLive = vehicleHistoryRepository
                .findByDevice_idAndStatusOrderByTimestampDesc(deviceId, "N1");
            
            VehicleHistory liveRecord;
            
            if (existingLive.isPresent()) {
                // Update existing live record
                liveRecord = existingLive.get();
                liveRecord.setLatitude(latitude);
                liveRecord.setLongitude(longitude);
                liveRecord.setSpeed(speed);
                liveRecord.setTimestamp(new Timestamp(System.currentTimeMillis()));
                
            } else {
                // Create new live record
                liveRecord = createLiveRecord(deviceId, latitude, longitude, speed);
            }
            
            return vehicleHistoryRepository.save(liveRecord);
            
        } catch (Exception e) {
            logger.error("❌ Failed to update live location for device: {}", deviceId, e);
            throw new RuntimeException("Failed to update live location", e);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════════
    // ✅ FIXED HELPER METHODS
    // ═══════════════════════════════════════════════════════════════════════════════════

    /**
     * ✅ FIXED: Handle duplicate key scenario
     */
    private VehicleHistory handleDuplicateKey(VehicleHistory history) {
        try {
            // Try to find and update existing record
            Optional<VehicleHistory> existingOpt = vehicleHistoryRepository
                .findByDevice_idAndTimestamp(history.getDevice_id(), history.getTimestamp());
            
            if (existingOpt.isPresent()) {
                VehicleHistory existing = existingOpt.get();
                updateExistingRecord(existing, history);
                return vehicleHistoryRepository.save(existing);
            } else {
                // Race condition resolved, try insert again
                return vehicleHistoryRepository.save(history);
            }
            
        } catch (Exception e) {
            logger.error("❌ Failed to handle duplicate key for device: {}", 
                       history.getDevice_id(), e);
            return null;
        }
    }

    /**
     * ✅ FIXED: Update existing record with new data
     */
    private void updateExistingRecord(VehicleHistory existing, VehicleHistory newData) {
        // Update GPS coordinates
        if (newData.getLatitude() != null) {
            existing.setLatitude(newData.getLatitude());
        }
        if (newData.getLongitude() != null) {
            existing.setLongitude(newData.getLongitude());
        }
        if (newData.getSpeed() != null) {
            existing.setSpeed(newData.getSpeed());
        }
        
        // Update other fields
        if (newData.getCourse() != null) {
            existing.setCourse(newData.getCourse());
        }
        if (newData.getSequenceNumber() != null) {
            existing.setSequenceNumber(newData.getSequenceNumber());
        }
        if (newData.getIgnition() != null) {
            existing.setIgnition(newData.getIgnition());
        }
        if (newData.getVehicleStatus() != null) {
            existing.setVehicleStatus(newData.getVehicleStatus());
        }
        if (newData.getStatus() != null) {
            existing.setStatus(newData.getStatus());
        }
        if (newData.getTimeIntervals() != null) {
            existing.setTimeIntervals(newData.getTimeIntervals());
        }
        if (newData.getDistanceIntervals() != null) {
            existing.setDistanceIntervals(newData.getDistanceIntervals());
        }
        if (newData.getGsmStrength() != null) {
            existing.setGsmStrength(newData.getGsmStrength());
        }
        if (newData.getImei() != null) {
            existing.setImei(newData.getImei());
        }
        if (newData.getAdditionalData() != null) {
            existing.setAdditionalData(newData.getAdditionalData());
        }
      
        if (newData.getDealerName() != null) {
            existing.setDealerName(newData.getDealerName());
        }
        if (newData.getSerialNo() != null) {
            existing.setSerialNo(newData.getSerialNo());
        }
        
        logger.debug("✅ Updated existing record for device: {}", existing.getDevice_id());
    }

    /**
     * ✅ FIXED: MySQL-specific batch upsert using ON DUPLICATE KEY UPDATE
     */
    private void upsertBatchWithMySQL(List<VehicleHistory> histories) {
        String sql = """
            INSERT INTO vehicle_history (
                device_id, timestamp, latitude, longitude, speed, course, 
                sequenceNumber, ignition, vehicleStatus, status, 
                timeIntervals, distanceIntervals, gsmStrength, imei, 
                additionalData, admin_id, client_id, user_id, driver_id, 
                dealer_id, superadmin_id, panic, serialNo, dealerName, dealerId
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                latitude = VALUES(latitude),
                longitude = VALUES(longitude),
                speed = VALUES(speed),
                course = VALUES(course),
                sequenceNumber = VALUES(sequenceNumber),
                ignition = VALUES(ignition),
                vehicleStatus = VALUES(vehicleStatus),
                status = VALUES(status),
                timeIntervals = VALUES(timeIntervals),
                distanceIntervals = VALUES(distanceIntervals),
                gsmStrength = VALUES(gsmStrength),
                imei = VALUES(imei),
                additionalData = VALUES(additionalData),
                admin_id = VALUES(admin_id),
                client_id = VALUES(client_id),
                user_id = VALUES(user_id),
                driver_id = VALUES(driver_id),
                dealer_id = VALUES(dealer_id),
                superadmin_id = VALUES(superadmin_id),
                panic = VALUES(panic),
                serialNo = VALUES(serialNo),
                dealerName = VALUES(dealerName),
                dealerId = VALUES(dealerId)
            """;
        
        jdbcTemplate.batchUpdate(sql, histories, histories.size(),
            (ps, history) -> {
                ps.setString(1, history.getDevice_id());
                ps.setTimestamp(2, history.getTimestamp());
                ps.setObject(3, history.getLatitude());
                ps.setObject(4, history.getLongitude());
                ps.setObject(5, history.getSpeed());
                ps.setString(6, history.getCourse());
                ps.setString(7, history.getSequenceNumber());
                ps.setString(8, history.getIgnition());
                ps.setString(9, history.getVehicleStatus());
                ps.setString(10, history.getStatus());
                ps.setString(11, history.getTimeIntervals());
                ps.setString(12, history.getDistanceIntervals());
                ps.setString(13, history.getGsmStrength());
                ps.setString(14, history.getImei());
                ps.setString(15, history.getAdditionalData());
                ps.setObject(16, history.getAdmin_id());
                ps.setObject(17, history.getClient_id());
                ps.setObject(18, history.getUser_id());
                ps.setObject(19, history.getDriver_id());
                ps.setObject(20, history.getDealer_id());
                ps.setObject(21, history.getSuperadmin_id());
                ps.setObject(22, history.getPanic());
                ps.setString(23, history.getSerialNo());
                ps.setString(24, history.getDealerName());
               
            });
        
        logger.info("✅ MySQL batch upsert completed for {} records", histories.size());
    }

    /**
     * ✅ FIXED: Create live record helper method
     */
    private VehicleHistory createLiveRecord(String deviceId, Double latitude, Double longitude, Double speed) {
        VehicleHistory history = new VehicleHistory();
        history.setDevice_id(deviceId);
        history.setTimestamp(new Timestamp(System.currentTimeMillis()));
        history.setLatitude(latitude);
        history.setLongitude(longitude);
        history.setSpeed(speed);
        history.setStatus("N1");
        history.setVehicleStatus("LIVE");
        return history;
    }

    /**
     * ✅ FIXED: Create history record helper method
     */
    private VehicleHistory createHistoryRecord(String deviceId, Double latitude, Double longitude, Double speed) {
        VehicleHistory history = new VehicleHistory();
        history.setDevice_id(deviceId);
        history.setTimestamp(new Timestamp(System.currentTimeMillis()));
        history.setLatitude(latitude);
        history.setLongitude(longitude);
        history.setSpeed(speed);
        history.setStatus("N2");
        history.setVehicleStatus("HISTORY");
        return history;
    }

    /**
     * ✅ FIXED: Parse GPS data JSON into VehicleHistory objects
     */
    private List<VehicleHistory> parseGpsData(String gpsDataJson) throws JsonProcessingException {
        List<VehicleHistory> histories = new ArrayList<>();
        
        // Handle concatenated JSON objects (your current format)
        if (!gpsDataJson.startsWith("[")) {
            // Split by }{ pattern and parse each JSON object
            String[] jsonObjects = gpsDataJson.split("\\}\\{");
            
            for (int i = 0; i < jsonObjects.length; i++) {
                String jsonObj = jsonObjects[i];
                
                // Fix the JSON object format
                if (i > 0) {
                    jsonObj = "{" + jsonObj;
                }
                if (i < jsonObjects.length - 1) {
                    jsonObj = jsonObj + "}";
                }
                
                try {
                    VehicleHistory history = parseJsonToVehicleHistory(jsonObj);
                    if (history != null) {
                        histories.add(history);
                    }
                } catch (Exception e) {
                    logger.error("❌ Failed to parse JSON object: {}", jsonObj, e);
                }
            }
        } else {
            // Handle JSON array format
            JsonNode jsonArray = objectMapper.readTree(gpsDataJson);
            
            for (JsonNode jsonNode : jsonArray) {
                try {
                    VehicleHistory history = parseJsonToVehicleHistory(jsonNode.toString());
                    if (history != null) {
                        histories.add(history);
                    }
                } catch (Exception e) {
                    logger.error("❌ Failed to parse JSON array element: {}", jsonNode.toString(), e);
                }
            }
        }
        
        return histories;
    }

    /**
     * ✅ FIXED: Parse single JSON object to VehicleHistory
     */
    private VehicleHistory parseJsonToVehicleHistory(String json) throws JsonProcessingException {
        JsonNode jsonNode = objectMapper.readTree(json);
        
        // Extract required fields
        String deviceId = jsonNode.get("deviceID").asText();
        String timestampStr = jsonNode.get("timestamp").asText();
        String status = jsonNode.has("status") ? jsonNode.get("status").asText() : "N2";
        
        // Parse timestamp
        Timestamp timestamp = parseTimestamp(timestampStr);
        
        // Parse coordinates
        Double latitude = parseCoordinate(jsonNode.get("latitude").asText());
        Double longitude = parseCoordinate(jsonNode.get("longitude").asText());
        Double speed = parseDouble(jsonNode.get("speed").asText());
        
        // ✅ FIXED: Create VehicleHistory using simple constructor
        VehicleHistory history = new VehicleHistory();
        history.setDevice_id(deviceId);
        history.setTimestamp(timestamp);
        history.setLatitude(latitude);
        history.setLongitude(longitude);
        history.setSpeed(speed);
        history.setStatus(status);
        
        // Set other fields
        history.setImei(getJsonValue(jsonNode, "IMEI"));
        history.setCourse(getJsonValue(jsonNode, "course"));
        history.setIgnition(getJsonValue(jsonNode, "ignition"));
        history.setVehicleStatus(getJsonValue(jsonNode, "vehicleStatus"));
        history.setAdditionalData(getJsonValue(jsonNode, "additionalData"));
        history.setTimeIntervals(getJsonValue(jsonNode, "timeIntervals"));
        history.setDistanceIntervals(getJsonValue(jsonNode, "distanceInterval"));
        history.setGsmStrength(getJsonValue(jsonNode, "gsmStrength"));
        history.setSequenceNumber(getJsonValue(jsonNode, "sequenceNumber"));
        history.setSerialNo(getJsonValue(jsonNode, "serialNo"));
        history.setDealerName(getJsonValue(jsonNode, "dealerName"));
       
        
        return history;
    }

    /**
     * ✅ Parse timestamp string to Timestamp object
     */
    private Timestamp parseTimestamp(String timestampStr) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime dateTime = LocalDateTime.parse(timestampStr, formatter);
            return Timestamp.valueOf(dateTime);
        } catch (Exception e) {
            logger.error("❌ Failed to parse timestamp: {}", timestampStr, e);
            return new Timestamp(System.currentTimeMillis());
        }
    }

    /**
     * ✅ Parse coordinate string to Double (handles GPS format)
     */
    private Double parseCoordinate(String coordStr) {
        try {
            return Double.parseDouble(coordStr);
        } catch (Exception e) {
            logger.error("❌ Failed to parse coordinate: {}", coordStr, e);
            return 0.0;
        }
    }

    /**
     * ✅ Parse double value safely
     */
    private Double parseDouble(String value) {
        try {
            return Double.parseDouble(value);
        } catch (Exception e) {
            return 0.0;
        }
    }

    /**
     * ✅ Get JSON value safely
     */
    private String getJsonValue(JsonNode node, String fieldName) {
        return node.has(fieldName) ? node.get(fieldName).asText() : null;
    }

    // ═══════════════════════════════════════════════════════════════════════════════════
    // ✅ FIXED MAIN SERVICE METHODS
    // ═══════════════════════════════════════════════════════════════════════════════════

    /**
     * ⚡ PRIMARY METHOD: Get GPS history with date range (PARTITION-AWARE)
     */
    public List<VehicleHistory> getVehicleHistory(String deviceId, Timestamp startDate, Timestamp endDate) {
        logger.info("🔍 Fetching GPS history for deviceId: {} from {} to {}", 
                   deviceId, startDate, endDate);
        
        try {
            List<VehicleHistory> history = vehicleHistoryRepository.findByDevice_idAndTimestampBetween(
                deviceId, startDate, endDate
            );
            
            if (history.isEmpty()) {
                logger.warn("⚠️ No GPS history found for deviceId: {} in date range", deviceId);
                return Collections.emptyList();
            }
            
            logger.info("✅ Fetched {} GPS records for deviceId: {}", history.size(), deviceId);
            return history;
            
        } catch (Exception e) {
            logger.error("❌ Error fetching GPS history for deviceId: {} - {}", deviceId, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch GPS history", e);
        }
    }

    /**
     * 📅 FIND BY DEVICE ID, MONTH, YEAR
     */
    public List<VehicleHistory> findByDeviceIDMonthYear(String deviceId, int month, int year) {
        logger.info("📅 Fetching GPS data for deviceId: {} for {}/{}", deviceId, month, year);
        
        try {
            LocalDateTime monthStart = LocalDateTime.of(year, month, 1, 0, 0, 0);
            LocalDateTime monthEnd = monthStart.plusMonths(1).minusSeconds(1);
            
            Timestamp startTimestamp = Timestamp.valueOf(monthStart);
            Timestamp endTimestamp = Timestamp.valueOf(monthEnd);
            
            return getVehicleHistory(deviceId, startTimestamp, endTimestamp);
            
        } catch (Exception e) {
            logger.error("❌ Error fetching data for deviceId: {} ({}/{}) - {}", deviceId, month, year, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * 📍 LATEST LOCATION: Get most recent GPS position
     */
    public VehicleHistory getLatestLocation(String deviceId) {
        logger.info("📍 Fetching latest location for deviceId: {}", deviceId);
        
        try {
            Optional<VehicleHistory> latest = vehicleHistoryRepository.findTopByDevice_idOrderByTimestampDesc(deviceId);
            
            if (latest.isPresent()) {
                logger.info("✅ Found latest location for deviceId: {}", deviceId);
                return latest.get();
            } else {
                logger.warn("⚠️ No location found for deviceId: {}", deviceId);
                return null;
            }
            
        } catch (Exception e) {
            logger.error("❌ Error fetching latest location for deviceId: {} - {}", deviceId, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 💾 BATCH SAVE: Optimized batch insert for GPS data
     */
    @Transactional
    public void saveGpsBatch(List<VehicleHistory> gpsDataBatch) {
        if (gpsDataBatch == null || gpsDataBatch.isEmpty()) {
            logger.warn("⚠️ GPS batch is empty, nothing to save");
            return;
        }
        
        logger.info("💾 Saving GPS batch of {} records with upsert", gpsDataBatch.size());
        
        try {
            upsertGpsBatch(gpsDataBatch);
            
        } catch (Exception e) {
            logger.error("❌ Error saving GPS batch: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to save GPS batch", e);
        }
    }

    /**
     * 💾 SINGLE SAVE: Save individual GPS record
     */
    @Transactional
    public void saveVehicleHistory(VehicleHistory history) {
        logger.info("💾 Saving single GPS record for deviceId: {}", history.getDevice_id());
        
        try {
            upsertVehicleHistory(history);
            logger.info("✅ GPS record saved successfully");
            
        } catch (Exception e) {
            logger.error("❌ Error saving GPS record: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to save GPS record", e);
        }
    }

    /**
     * 🚨 PANIC ALERTS: Get panic alerts in date range
     */
    public List<PanicAlertDTO> getPanicAlertsInRange(String deviceId, Timestamp startDate, Timestamp endDate) {
        logger.info("🚨 Fetching panic alerts for deviceId: {} from {} to {}", deviceId, startDate, endDate);
        
        try {
            List<VehicleHistory> panicAlerts = vehicleHistoryRepository.findByDevice_idAndTimestampBetweenAndPanic(
                deviceId, startDate, endDate, 1
            );
            
            List<PanicAlertDTO> alertDTOs = panicAlerts.stream()
                .map(this::convertToPanicAlertDTO)
                .collect(Collectors.toList());
            
            logger.info("✅ Found {} panic alerts for deviceId: {}", alertDTOs.size(), deviceId);
            return alertDTOs;
            
        } catch (Exception e) {
            logger.error("❌ Error fetching panic alerts for deviceId: {} - {}", deviceId, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * 🔄 DTO CONVERSION: Convert to PanicAlertDTO
     */
    private PanicAlertDTO convertToPanicAlertDTO(VehicleHistory history) {
        PanicAlertDTO dto = new PanicAlertDTO();
        dto.setId(history.getDevice_id() + "_" + history.getTimestamp().getTime());
        dto.setVehicleId(history.getDevice_id());
        dto.setLatitude(history.getLatitude());
        dto.setLongitude(history.getLongitude());
        dto.setTimestamp(history.getTimestamp());
        dto.setPanic(history.getPanic());
        return dto;
    }

    /**
     * 🏃‍♂️ DISTANCE CALCULATION: Calculate total distance traveled
     */
    public double calculateTotalDistance(String deviceId, Timestamp startDate, Timestamp endDate) {
        logger.info("🏃‍♂️ Calculating distance for deviceId: {} from {} to {}", deviceId, startDate, endDate);
        
        try {
            List<VehicleHistory> records = getVehicleHistory(deviceId, startDate, endDate);
            
            if (records.size() < 2) {
                logger.warn("⚠️ Not enough GPS points to calculate distance for deviceId: {}", deviceId);
                return 0.0;
            }
            
            double totalDistance = 0.0;
            for (int i = 0; i < records.size() - 1; i++) {
                VehicleHistory point1 = records.get(i);
                VehicleHistory point2 = records.get(i + 1);
                
                if (point1.hasValidCoordinates() && point2.hasValidCoordinates()) {
                    totalDistance += calculateDistance(
                        point1.getLatitude(), point1.getLongitude(),
                        point2.getLatitude(), point2.getLongitude()
                    );
                }
            }
            
            logger.info("✅ Total distance calculated: {:.2f} km for deviceId: {}", totalDistance, deviceId);
            return totalDistance;
            
        } catch (Exception e) {
            logger.error("❌ Error calculating distance for deviceId: {} - {}", deviceId, e.getMessage(), e);
            return 0.0;
        }
    }

    /**
     * 🧮 DISTANCE CALCULATION: Haversine formula
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return EARTH_RADIUS * c;
    }
}