package com.GpsTracker.Thinture.service.persistence;

import com.GpsTracker.Thinture.model.VehicleLastLocation;
import com.GpsTracker.Thinture.repository.VehicleLastLocationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

/**
 * Service for persisting and managing vehicle last known locations
 * Optimized for high-frequency updates from 5000+ devices
 */
@Service
public class LocationPersistenceService {
    
    private static final Logger logger = LoggerFactory.getLogger(LocationPersistenceService.class);
    
    @Autowired
    private VehicleLastLocationRepository lastLocationRepository;
    
    @Autowired
    private VehicleDataCache vehicleDataCache;
    
    // Metrics
    private final AtomicLong totalLocationUpdates = new AtomicLong(0);
    private final AtomicLong failedUpdates = new AtomicLong(0);
    private final Map<String, Long> lastUpdateTime = new ConcurrentHashMap<>();
    
    // Update frequency limiting (per device)
    private static final long MIN_UPDATE_INTERVAL_MS = 1000; // Min 1 second between updates
    
    /**
     * Updates or creates last known location for a vehicle
     */
    @Transactional
    @Retryable(value = {DataAccessException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public VehicleLastLocation updateLastLocation(VehicleLastLocation newLocation) {
        try {
            // Check update frequency
            if (!shouldUpdateLocation(newLocation.getDeviceId())) {
                logger.debug("Skipping location update for device {} - too frequent", newLocation.getDeviceId());
                return newLocation;
            }
            
            // Try to find existing location
            VehicleLastLocation existingLocation = findExistingLocation(newLocation);
            
            if (existingLocation != null) {
                // Update existing location
                updateExistingLocation(existingLocation, newLocation);
                VehicleLastLocation saved = lastLocationRepository.save(existingLocation);
                
                // Update cache
                vehicleDataCache.cacheLastLocation(saved);
                
                // Update metrics
                totalLocationUpdates.incrementAndGet();
                lastUpdateTime.put(newLocation.getDeviceId(), System.currentTimeMillis());
                
                logger.debug("Updated last location for device: {}", newLocation.getDeviceId());
                return saved;
                
            } else {
                // Create new location
                VehicleLastLocation saved = lastLocationRepository.save(newLocation);
                
                // Update cache
                vehicleDataCache.cacheLastLocation(saved);
                
                // Update metrics
                totalLocationUpdates.incrementAndGet();
                lastUpdateTime.put(newLocation.getDeviceId(), System.currentTimeMillis());
                
                logger.info("Created new last location for device: {}", newLocation.getDeviceId());
                return saved;
            }
            
        } catch (Exception e) {
            failedUpdates.incrementAndGet();
            logger.error("Failed to update last location for device: {}", newLocation.getDeviceId(), e);
            throw e;
        }
    }
    
    /**
     * Async version of updateLastLocation
     */
    @Async("gpsProcessingExecutor")
    public void updateLastLocationAsync(VehicleLastLocation newLocation) {
        updateLastLocation(newLocation);
    }
    
    /**
     * Batch update last locations
     */
    @Transactional
    public List<VehicleLastLocation> updateLastLocations(List<VehicleLastLocation> locations) {
        List<VehicleLastLocation> savedLocations = new ArrayList<>();
        
        // Group by device ID to handle duplicates
        Map<String, VehicleLastLocation> latestByDevice = locations.stream()
            .collect(Collectors.toMap(
                VehicleLastLocation::getDeviceId,
                loc -> loc,
                (existing, replacement) -> 
                    existing.getTimestamp().after(replacement.getTimestamp()) ? existing : replacement
            ));
        
        for (VehicleLastLocation location : latestByDevice.values()) {
            try {
                VehicleLastLocation saved = updateLastLocation(location);
                savedLocations.add(saved);
            } catch (Exception e) {
                logger.error("Failed to update location in batch for device: {}", location.getDeviceId());
            }
        }
        
        logger.info("Batch updated {} last locations", savedLocations.size());
        return savedLocations;
    }
    
    /**
     * Gets last known location by device ID
     */
    public Optional<VehicleLastLocation> getLastLocationByDeviceId(String deviceId) {
        // Try cache first
        Optional<VehicleLastLocation> cached = vehicleDataCache.getLastLocationByDeviceId(deviceId);
        if (cached.isPresent()) {
            logger.debug("Retrieved last location from cache for device: {}", deviceId);
            return cached;
        }
        
        // Fall back to database
        Optional<VehicleLastLocation> location = lastLocationRepository.findByDeviceId(deviceId);
        
        // Cache if found
        location.ifPresent(vehicleDataCache::cacheLastLocation);
        
        return location;
    }
    
    /**
     * Gets last known location by IMEI
     */
    public Optional<VehicleLastLocation> getLastLocationByImei(String imei) {
        // Try cache first
        Optional<VehicleLastLocation> cached = vehicleDataCache.getLastLocationByImei(imei);
        if (cached.isPresent()) {
            logger.debug("Retrieved last location from cache for IMEI: {}", imei);
            return cached;
        }
        
        // Fall back to database
        Optional<VehicleLastLocation> location = lastLocationRepository.findByImei(imei);
        
        // Cache if found
        location.ifPresent(vehicleDataCache::cacheLastLocation);
        
        return location;
    }
    
    /**
     * Gets all last locations with pagination
     */
    public Page<VehicleLastLocation> getAllLastLocations(Pageable pageable) {
        return lastLocationRepository.findAll(pageable);
    }
    
    /**
     * Gets last locations updated after specific time
     */
    public List<VehicleLastLocation> getLocationsUpdatedAfter(Timestamp timestamp) {
        return lastLocationRepository.findByTimestampAfter(timestamp);
    }
    
    /**
     * Gets locations within a geographical boundary
     */
    public List<VehicleLastLocation> getLocationsWithinBounds(
            double minLat, double maxLat, double minLon, double maxLon) {
        return lastLocationRepository.findByLatitudeBetweenAndLongitudeBetween(
            minLat, maxLat, minLon, maxLon);
    }
    
    /**
     * Gets active vehicles (moved in last N minutes)
     */
    public List<VehicleLastLocation> getActiveVehicles(int lastMinutes) {
        Timestamp cutoffTime = Timestamp.valueOf(LocalDateTime.now().minusMinutes(lastMinutes));
        return lastLocationRepository.findByTimestampAfter(cutoffTime);
    }
    
    /**
     * Gets inactive vehicles (not moved in last N minutes)
     */
    public List<VehicleLastLocation> getInactiveVehicles(int lastMinutes) {
        Timestamp cutoffTime = Timestamp.valueOf(LocalDateTime.now().minusMinutes(lastMinutes));
        return lastLocationRepository.findByTimestampBefore(cutoffTime);
    }
    
    /**
     * Deletes old location records
     */
    @Transactional
    public int deleteOldLocations(int daysToKeep) {
        Timestamp cutoffTime = Timestamp.valueOf(LocalDateTime.now().minusDays(daysToKeep));
        int deleted = lastLocationRepository.deleteByTimestampBefore(cutoffTime);
        logger.info("Deleted {} old location records", deleted);
        return deleted;
    }
    
    /**
     * Gets statistics about last locations
     */
    public LocationStatistics getStatistics() {
        long totalRecords = lastLocationRepository.count();
        long activeVehicles = getActiveVehicles(30).size(); // Active in last 30 minutes
        
        return new LocationStatistics(
            totalRecords,
            activeVehicles,
            totalLocationUpdates.get(),
            failedUpdates.get(),
            vehicleDataCache.getCacheSize()
        );
    }
    
    /**
     * Finds existing location for update
     */
    private VehicleLastLocation findExistingLocation(VehicleLastLocation newLocation) {
        // Try by IMEI first (more reliable)
        if (newLocation.getImei() != null && !newLocation.getImei().isEmpty()) {
            Optional<VehicleLastLocation> byImei = lastLocationRepository.findByImei(newLocation.getImei());
            if (byImei.isPresent()) {
                return byImei.get();
            }
        }
        
        // Try by device ID
        if (newLocation.getDeviceId() != null && !newLocation.getDeviceId().isEmpty()) {
            Optional<VehicleLastLocation> byDeviceId = lastLocationRepository.findByDeviceId(newLocation.getDeviceId());
            if (byDeviceId.isPresent()) {
                return byDeviceId.get();
            }
        }
        
        return null;
    }
    
    /**
     * Updates existing location with new data
     */
    private void updateExistingLocation(VehicleLastLocation existing, VehicleLastLocation newData) {
        existing.setLatitude(newData.getLatitude());
        existing.setLongitude(newData.getLongitude());
        existing.setTimestamp(newData.getTimestamp());
        existing.setSpeed(newData.getSpeed());
        existing.setStatus(newData.getStatus());
        existing.setIgnition(newData.getIgnition());
        existing.setCourse(newData.getCourse());
        existing.setVehicleStatus(newData.getVehicleStatus());
        
        // Update device ID if it was empty
        if (existing.getDeviceId() == null || existing.getDeviceId().isEmpty()) {
            existing.setDeviceId(newData.getDeviceId());
        }
        
        // Update IMEI if it was empty
        if (existing.getImei() == null || existing.getImei().isEmpty()) {
            existing.setImei(newData.getImei());
        }
    }
    
    /**
     * Checks if location should be updated based on frequency
     */
    private boolean shouldUpdateLocation(String deviceId) {
        Long lastUpdate = lastUpdateTime.get(deviceId);
        if (lastUpdate == null) {
            return true;
        }
        
        long timeSinceLastUpdate = System.currentTimeMillis() - lastUpdate;
        return timeSinceLastUpdate >= MIN_UPDATE_INTERVAL_MS;
    }
    
    /**
     * Preloads cache with all locations
     */
    @PostConstruct
    public void preloadCache() {
        try {
            logger.info("Preloading location cache...");
            List<VehicleLastLocation> allLocations = lastLocationRepository.findAll();
            
            for (VehicleLastLocation location : allLocations) {
                vehicleDataCache.cacheLastLocation(location);
            }
            
            logger.info("Preloaded {} locations into cache", allLocations.size());
        } catch (Exception e) {
            logger.error("Failed to preload location cache", e);
        }
    }
    
    /**
     * Location statistics class
     */
    public static class LocationStatistics {
        private final long totalRecords;
        private final long activeVehicles;
        private final long totalUpdates;
        private final long failedUpdates;
        private final long cacheSize;
        
        public LocationStatistics(long totalRecords, long activeVehicles, 
                                long totalUpdates, long failedUpdates, long cacheSize) {
            this.totalRecords = totalRecords;
            this.activeVehicles = activeVehicles;
            this.totalUpdates = totalUpdates;
            this.failedUpdates = failedUpdates;
            this.cacheSize = cacheSize;
        }
        
        // Getters
        public long getTotalRecords() { return totalRecords; }
        public long getActiveVehicles() { return activeVehicles; }
        public long getTotalUpdates() { return totalUpdates; }
        public long getFailedUpdates() { return failedUpdates; }
        public long getCacheSize() { return cacheSize; }
    }
}