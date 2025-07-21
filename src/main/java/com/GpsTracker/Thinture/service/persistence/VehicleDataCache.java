package com.GpsTracker.Thinture.service.persistence;

import com.GpsTracker.Thinture.model.Vehicle;
import com.GpsTracker.Thinture.model.VehicleLastLocation;
import com.GpsTracker.Thinture.repository.VehicleRepository;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * In-memory cache for vehicle data to reduce database lookups
 * Critical for handling 5000+ devices with high-frequency updates
 */
@Service
public class VehicleDataCache {
    
    private static final Logger logger = LoggerFactory.getLogger(VehicleDataCache.class);
    
    @Autowired
    private VehicleRepository vehicleRepository;
    
    @Value("${cache.vehicle.max-size:10000}")
    private long maxCacheSize;
    
    @Value("${cache.vehicle.expire-after-write-minutes:60}")
    private long expireAfterWriteMinutes;
    
    @Value("${cache.vehicle.expire-after-access-minutes:30}")
    private long expireAfterAccessMinutes;
    
    // Vehicle cache (IMEI -> Vehicle)
    private LoadingCache<String, Optional<Vehicle>> vehicleByImeiCache;
    
    // Vehicle cache (ID -> Vehicle)
    private Cache<Long, Vehicle> vehicleByIdCache;
    
    // Last location cache (DeviceID -> Location)
    private Cache<String, VehicleLastLocation> lastLocationByDeviceCache;
    
    // Last location cache (IMEI -> Location)
    private Cache<String, VehicleLastLocation> lastLocationByImeiCache;
    
    // Device ID to IMEI mapping cache
    private Cache<String, String> deviceToImeiCache;
    
    // Metrics
    private final AtomicLong cacheHits = new AtomicLong(0);
    private final AtomicLong cacheMisses = new AtomicLong(0);
    private final Map<String, Long> accessFrequency = new ConcurrentHashMap<>();
    
    @PostConstruct
    public void initialize() {
        logger.info("Initializing vehicle data cache with max size: {}", maxCacheSize);
        
        // Initialize vehicle by IMEI cache with loader
        vehicleByImeiCache = Caffeine.newBuilder()
            .maximumSize(maxCacheSize)
            .expireAfterWrite(expireAfterWriteMinutes, TimeUnit.MINUTES)
            .expireAfterAccess(expireAfterAccessMinutes, TimeUnit.MINUTES)
            .recordStats()
            .build(imei -> vehicleRepository.findByImei(imei));
        
        // Initialize vehicle by ID cache
        vehicleByIdCache = Caffeine.newBuilder()
            .maximumSize(maxCacheSize)
            .expireAfterWrite(expireAfterWriteMinutes, TimeUnit.MINUTES)
            .expireAfterAccess(expireAfterAccessMinutes, TimeUnit.MINUTES)
            .recordStats()
            .build();
        
        // Initialize last location caches
        lastLocationByDeviceCache = Caffeine.newBuilder()
            .maximumSize(maxCacheSize * 2) // Larger size for locations
            .expireAfterWrite(10, TimeUnit.MINUTES) // Shorter expiry for real-time data
            .recordStats()
            .build();
        
        lastLocationByImeiCache = Caffeine.newBuilder()
            .maximumSize(maxCacheSize * 2)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .recordStats()
            .build();
        
        // Initialize device to IMEI mapping cache
        deviceToImeiCache = Caffeine.newBuilder()
            .maximumSize(maxCacheSize)
            .expireAfterWrite(expireAfterWriteMinutes, TimeUnit.MINUTES)
            .build();
        
        // Preload frequently accessed vehicles
        preloadFrequentVehicles();
    }
    
    /**
     * Gets vehicle by IMEI from cache
     */
    public Optional<Vehicle> getVehicleByImei(String imei) {
        try {
            // Track access frequency
            accessFrequency.merge(imei, 1L, Long::sum);
            
            Optional<Vehicle> vehicle = vehicleByImeiCache.get(imei);
            
            if (vehicle != null && vehicle.isPresent()) {
                cacheHits.incrementAndGet();
                // Also cache by ID
                vehicleByIdCache.put(vehicle.get().getId(), vehicle.get());
                // Cache device to IMEI mapping
                if (vehicle.get().getDeviceID() != null) {
                    deviceToImeiCache.put(vehicle.get().getDeviceID(), imei);
                }
            } else {
                cacheMisses.incrementAndGet();
            }
            
            return vehicle;
        } catch (Exception e) {
            logger.error("Error getting vehicle from cache for IMEI: {}", imei, e);
            cacheMisses.incrementAndGet();
            return Optional.empty();
        }
    }
    
    /**
     * Gets vehicle by ID from cache
     */
    public Optional<Vehicle> getVehicleById(Long id) {
        Vehicle vehicle = vehicleByIdCache.getIfPresent(id);
        
        if (vehicle != null) {
            cacheHits.incrementAndGet();
            return Optional.of(vehicle);
        }
        
        // Try to load from database
        Optional<Vehicle> loaded = vehicleRepository.findById(id);
        if (loaded.isPresent()) {
            cacheVehicle(loaded.get());
        } else {
            cacheMisses.incrementAndGet();
        }
        
        return loaded;
    }
    
    /**
     * Caches a vehicle
     */
    public void cacheVehicle(Vehicle vehicle) {
        if (vehicle == null || vehicle.getImei() == null) {
            return;
        }
        
        vehicleByImeiCache.put(vehicle.getImei(), Optional.of(vehicle));
        vehicleByIdCache.put(vehicle.getId(), vehicle);
        
        if (vehicle.getDeviceID() != null) {
            deviceToImeiCache.put(vehicle.getDeviceID(), vehicle.getImei());
        }
        
        logger.debug("Cached vehicle with IMEI: {}", vehicle.getImei());
    }
    
    /**
     * Caches last location
     */
    public void cacheLastLocation(VehicleLastLocation location) {
        if (location == null) {
            return;
        }
        
        if (location.getDeviceId() != null) {
            lastLocationByDeviceCache.put(location.getDeviceId(), location);
        }
        
        if (location.getImei() != null) {
            lastLocationByImeiCache.put(location.getImei(), location);
        }
        
        logger.debug("Cached last location for device: {}", location.getDeviceId());
    }
    
    /**
     * Gets last location by device ID from cache
     */
    public Optional<VehicleLastLocation> getLastLocationByDeviceId(String deviceId) {
        VehicleLastLocation location = lastLocationByDeviceCache.getIfPresent(deviceId);
        
        if (location != null) {
            cacheHits.incrementAndGet();
            return Optional.of(location);
        }
        
        cacheMisses.incrementAndGet();
        return Optional.empty();
    }
    
    /**
     * Gets last location by IMEI from cache
     */
    public Optional<VehicleLastLocation> getLastLocationByImei(String imei) {
        VehicleLastLocation location = lastLocationByImeiCache.getIfPresent(imei);
        
        if (location != null) {
            cacheHits.incrementAndGet();
            return Optional.of(location);
        }
        
        cacheMisses.incrementAndGet();
        return Optional.empty();
    }
    
    /**
     * Gets IMEI by device ID from cache
     */
    public Optional<String> getImeiByDeviceId(String deviceId) {
        String imei = deviceToImeiCache.getIfPresent(deviceId);
        return Optional.ofNullable(imei);
    }
    
    /**
     * Invalidates cache entries for a vehicle
     */
    public void invalidateVehicle(String imei) {
        vehicleByImeiCache.invalidate(imei);
        
        // Also invalidate related entries
        Optional<Vehicle> vehicle = vehicleRepository.findByImei(imei);
        vehicle.ifPresent(v -> {
            vehicleByIdCache.invalidate(v.getId());
            if (v.getDeviceID() != null) {
                deviceToImeiCache.invalidate(v.getDeviceID());
                lastLocationByDeviceCache.invalidate(v.getDeviceID());
            }
        });
        
        lastLocationByImeiCache.invalidate(imei);
        
        logger.info("Invalidated cache entries for IMEI: {}", imei);
    }
    
    /**
     * Clears all caches
     */
    public void clearAll() {
        vehicleByImeiCache.invalidateAll();
        vehicleByIdCache.invalidateAll();
        lastLocationByDeviceCache.invalidateAll();
        lastLocationByImeiCache.invalidateAll();
        deviceToImeiCache.invalidateAll();
        
        logger.info("Cleared all cache entries");
    }
    
    /**
     * Preloads frequently accessed vehicles
     */
    private void preloadFrequentVehicles() {
        try {
            // In a real implementation, you might track and persist access patterns
            // For now, preload all active vehicles
            List<Vehicle> activeVehicles = vehicleRepository.findAll()
                .stream()
                .limit(1000) // Limit initial preload
                .collect(Collectors.toList());
            
            for (Vehicle vehicle : activeVehicles) {
                cacheVehicle(vehicle);
            }
            
            logger.info("Preloaded {} vehicles into cache", activeVehicles.size());
        } catch (Exception e) {
            logger.error("Failed to preload vehicles", e);
        }
    }
    
    /**
     * Scheduled cache maintenance
     */
    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void performCacheMaintenance() {
        // Log cache statistics
        logCacheStatistics();
        
        // Cleanup old access frequency entries
        long cutoffTime = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(24);
        accessFrequency.entrySet().removeIf(entry -> entry.getValue() < cutoffTime);
        
        // Preload frequently accessed vehicles that aren't cached
        preloadFrequentlyAccessedVehicles();
    }
    
    /**
     * Preloads frequently accessed vehicles
     */
    private void preloadFrequentlyAccessedVehicles() {
        // Get top accessed IMEIs
        List<String> topImeis = accessFrequency.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(100)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
        
        for (String imei : topImeis) {
            if (vehicleByImeiCache.getIfPresent(imei) == null) {
                // Trigger cache load
                getVehicleByImei(imei);
            }
        }
    }
    
    /**
     * Logs cache statistics
     */
    private void logCacheStatistics() {
        CacheStats vehicleStats = vehicleByImeiCache.stats();
        CacheStats locationDeviceStats = lastLocationByDeviceCache.stats();
        
        logger.info("Cache Statistics - Vehicle Cache: " +
            "size={}, hits={}, misses={}, hitRate={:.2f}%, evictions={}", 
            vehicleByImeiCache.estimatedSize(),
            vehicleStats.hitCount(),
            vehicleStats.missCount(),
            vehicleStats.hitRate() * 100,
            vehicleStats.evictionCount()
        );
        
        logger.info("Cache Statistics - Location Cache: " +
            "size={}, hits={}, misses={}, hitRate={:.2f}%, evictions={}", 
            lastLocationByDeviceCache.estimatedSize(),
            locationDeviceStats.hitCount(),
            locationDeviceStats.missCount(),
            locationDeviceStats.hitRate() * 100,
            locationDeviceStats.evictionCount()
        );
        
        logger.info("Overall Cache Performance: hits={}, misses={}, hitRate={:.2f}%",
            cacheHits.get(),
            cacheMisses.get(),
            (cacheHits.get() * 100.0) / (cacheHits.get() + cacheMisses.get())
        );
    }
    
    /**
     * Gets cache statistics
     */
    public CacheStatistics getCacheStatistics() {
        CacheStats vehicleStats = vehicleByImeiCache.stats();
        CacheStats locationStats = lastLocationByDeviceCache.stats();
        
        return new CacheStatistics(
            vehicleByImeiCache.estimatedSize() + vehicleByIdCache.estimatedSize(),
            lastLocationByDeviceCache.estimatedSize() + lastLocationByImeiCache.estimatedSize(),
            deviceToImeiCache.estimatedSize(),
            cacheHits.get(),
            cacheMisses.get(),
            vehicleStats.hitRate(),
            vehicleStats.evictionCount() + locationStats.evictionCount()
        );
    }
    
    /**
     * Gets total cache size
     */
    public long getCacheSize() {
        return vehicleByImeiCache.estimatedSize() + 
               vehicleByIdCache.estimatedSize() + 
               lastLocationByDeviceCache.estimatedSize() + 
               lastLocationByImeiCache.estimatedSize() +
               deviceToImeiCache.estimatedSize();
    }
    
    /**
     * Cache statistics class
     */
    public static class CacheStatistics {
        private final long vehicleCacheSize;
        private final long locationCacheSize;
        private final long mappingCacheSize;
        private final long totalHits;
        private final long totalMisses;
        private final double hitRate;
        private final long evictions;
        
        public CacheStatistics(long vehicleCacheSize, long locationCacheSize, 
                             long mappingCacheSize, long totalHits, long totalMisses, 
                             double hitRate, long evictions) {
            this.vehicleCacheSize = vehicleCacheSize;
            this.locationCacheSize = locationCacheSize;
            this.mappingCacheSize = mappingCacheSize;
            this.totalHits = totalHits;
            this.totalMisses = totalMisses;
            this.hitRate = hitRate;
            this.evictions = evictions;
        }
        
        // Getters
        public long getVehicleCacheSize() { return vehicleCacheSize; }
        public long getLocationCacheSize() { return locationCacheSize; }
        public long getMappingCacheSize() { return mappingCacheSize; }
        public long getTotalHits() { return totalHits; }
        public long getTotalMisses() { return totalMisses; }
        public double getHitRate() { return hitRate; }
        public long getEvictions() { return evictions; }
        
        public long getTotalCacheSize() {
            return vehicleCacheSize + locationCacheSize + mappingCacheSize;
        }
    }
}