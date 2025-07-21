// ================================================================================================
// BatchPersistenceService.java - FIXED FOR YOUR EXISTING SETUP
// ================================================================================================

package com.GpsTracker.Thinture.service.persistence;

import com.GpsTracker.Thinture.model.VehicleHistory;
import com.GpsTracker.Thinture.service.VehicleHistoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * ✅ FIXED: Compatible with your existing VehicleHistoryService
 * High-performance batch persistence service for GPS data
 */
@Service
public class BatchPersistenceService {
    
    private static final Logger logger = LoggerFactory.getLogger(BatchPersistenceService.class);
    
    @Autowired
    private VehicleHistoryService vehicleHistoryService;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Value("${batch.size:1000}")
    private int batchSize;
    
    @Value("${batch.flush.interval:500}")
    private int flushInterval;
    
    @Value("${batch.max.wait.time:5000}")
    private int maxWaitTime;
    
    @Value("${batch.parallel.threads:4}")
    private int parallelThreads;
    
    // Thread-safe ID generator
    private static final AtomicLong ID_COUNTER = new AtomicLong(System.currentTimeMillis() * 1000L);
    
    // Multiple queues for parallel processing
    private final List<BlockingQueue<VehicleHistory>> batchQueues = new ArrayList<>();
    private final ExecutorService batchExecutor = Executors.newFixedThreadPool(4);
    
    // Overflow queue for when main queues are full
    private final BlockingQueue<VehicleHistory> overflowQueue = new LinkedBlockingQueue<>(10000);
    
    // Metrics
    private final AtomicLong totalRecordsSaved = new AtomicLong(0);
    private final AtomicLong totalBatchesSaved = new AtomicLong(0);
    private final AtomicLong failedBatches = new AtomicLong(0);
    private final AtomicLong rejectedRecords = new AtomicLong(0);
    private final Map<Integer, Long> batchSaveTimes = new ConcurrentHashMap<>();
    
    // Performance tracking
    private final Map<String, AtomicLong> deviceMessageCounts = new ConcurrentHashMap<>();
    private volatile long lastFlushTime = System.currentTimeMillis();
    
    @PostConstruct
    public void initialize() {
        logger.info("🔄 Initializing BatchPersistenceService with batch size: {}, flush interval: {}ms", 
            batchSize, flushInterval);
        
        // Initialize multiple queues for parallel processing
        for (int i = 0; i < parallelThreads; i++) {
            batchQueues.add(new ArrayBlockingQueue<>(batchSize * 2));
        }
        
        // Start batch processing threads
        for (int i = 0; i < parallelThreads; i++) {
            final int queueIndex = i;
            batchExecutor.submit(() -> processBatchQueue(queueIndex));
        }
        
        logger.info("✅ BatchPersistenceService initialized with {} parallel queues", parallelThreads);
    }
    
    /**
     * ✅ FIXED: Adds a vehicle history record to the batch
     */
    public boolean addToBatch(VehicleHistory history) {
        if (history == null) {
            return false;
        }
        
        try {
            // ✅ FIXED: Use simple initialization compatible with your existing model
            prepareHistoryForBatch(history);
            
            // Track device message frequency
            String deviceId = getDeviceId(history);
            if (deviceId != null) {
                deviceMessageCounts.computeIfAbsent(deviceId, k -> new AtomicLong()).incrementAndGet();
            }
            
            // Distribute across queues based on device ID for better parallelism
            int queueIndex = Math.abs(deviceId != null ? deviceId.hashCode() : 0) % parallelThreads;
            BlockingQueue<VehicleHistory> queue = batchQueues.get(queueIndex);
            
            // Try to add to primary queue
            if (!queue.offer(history, 10, TimeUnit.MILLISECONDS)) {
                // Primary queue full, try overflow queue
                if (!overflowQueue.offer(history)) {
                    rejectedRecords.incrementAndGet();
                    logger.warn("⚠️ Rejected record for device: {} - all queues full", deviceId);
                    return false;
                }
                logger.debug("📦 Record added to overflow queue for device: {}", deviceId);
            }
            
            // Check if immediate flush needed
            if (queue.size() >= batchSize || shouldForceFlush()) {
                synchronized (queue) {
                    queue.notify(); // Wake up processing thread
                }
            }
            
            return true;
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("❌ Interrupted while adding to batch", e);
            return false;
        } catch (Exception e) {
            logger.error("❌ Error adding record to batch for device: {}", 
                        getDeviceId(history), e);
            return false;
        }
    }
    
    /**
     * ✅ FIXED: Prepare history entity for batch processing (compatible with existing model)
     */
    private void prepareHistoryForBatch(VehicleHistory history) {
        // Set timestamp if not present
        if (history.getTimestamp() == null) {
            history.setTimestamp(new Timestamp(System.currentTimeMillis()));
        }
        
        // Set device ID if not present but vehicle is available
        if (getDeviceId(history) == null && history.getVehicle() != null) {
            // Try to get device ID from vehicle
            String deviceId = history.getVehicle().getDeviceID();
            if (deviceId != null) {
                // Use reflection or setter if available
                try {
                    // This assumes your VehicleHistory has a setDeviceId method
                    history.getClass().getMethod("setDeviceId", String.class).invoke(history, deviceId);
                } catch (Exception e) {
                    logger.debug("Could not set device ID on history record", e);
                }
            }
        }
    }
    
    /**
     * ✅ FIXED: Get device ID from history (compatible with your existing model)
     */
    private String getDeviceId(VehicleHistory history) {
        if (history == null) return null;
        
        // Try different ways to get device ID based on your model
        try {
            // Method 1: Try getDeviceId() if it exists
            return (String) history.getClass().getMethod("getDeviceId").invoke(history);
        } catch (Exception e) {
            // Method 2: Try getting from vehicle relationship
            if (history.getVehicle() != null) {
                return history.getVehicle().getDeviceID();
            }
            // Method 3: Try getting from IMEI if that's how you identify devices
            return history.getImei();
        }
    }
    
    /**
     * Processes a specific batch queue
     */
    private void processBatchQueue(int queueIndex) {
        BlockingQueue<VehicleHistory> queue = batchQueues.get(queueIndex);
        List<VehicleHistory> batch = new ArrayList<>(batchSize);
        
        while (!Thread.currentThread().isInterrupted()) {
            try {
                // Wait for records with timeout
                VehicleHistory record = queue.poll(flushInterval, TimeUnit.MILLISECONDS);
                
                if (record != null) {
                    batch.add(record);
                    
                    // Drain additional records up to batch size
                    queue.drainTo(batch, batchSize - 1);
                }
                
                // Process batch if ready
                if (!batch.isEmpty() && (batch.size() >= batchSize || shouldFlush(batch))) {
                    saveBatch(batch);
                    batch.clear();
                }
                
                // Process overflow queue periodically
                if (queueIndex == 0 && !overflowQueue.isEmpty()) {
                    processOverflowQueue();
                }
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.info("🔄 Batch processing thread {} interrupted", queueIndex);
                break;
            } catch (Exception e) {
                logger.error("❌ Error in batch processing thread {}", queueIndex, e);
            }
        }
        
        // Save any remaining records before shutdown
        if (!batch.isEmpty()) {
            saveBatch(batch);
        }
    }
    
    /**
     * Scheduled flush for all queues
     */
    @Scheduled(fixedRateString = "${batch.flush.interval:500}")
    public void scheduledFlush() {
        for (int i = 0; i < batchQueues.size(); i++) {
            BlockingQueue<VehicleHistory> queue = batchQueues.get(i);
            if (!queue.isEmpty()) {
                synchronized (queue) {
                    queue.notify(); // Wake up processing thread
                }
            }
        }
        lastFlushTime = System.currentTimeMillis();
    }
    
    /**
     * ✅ FIXED: Saves batch using your existing VehicleHistoryService
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private void saveBatch(List<VehicleHistory> batch) {
        if (batch.isEmpty()) {
            return;
        }
        
        long startTime = System.currentTimeMillis();
        int batchSizeActual = batch.size();
        
        logger.debug("🔄 💾 Starting batch save of {} records", batchSizeActual);
        
        try {
            // ✅ FIXED: Use your existing VehicleHistoryService methods
            vehicleHistoryService.saveGpsBatch(batch);
            
            long duration = System.currentTimeMillis() - startTime;
            
            // Update metrics
            totalRecordsSaved.addAndGet(batchSizeActual);
            totalBatchesSaved.incrementAndGet();
            batchSaveTimes.put(batchSizeActual, duration);
            
            double recordsPerSecond = duration > 0 ? (double) batchSizeActual / (duration / 1000.0) : 0;
            logger.info("✅ GPS batch saved successfully: {} records in {}ms ({:.0f} records/sec)", 
                batchSizeActual, duration, recordsPerSecond);
            
            // Log warning if save is slow
            if (duration > 2000) {
                logger.warn("⚠️ Slow batch save detected: {}ms for {} records", duration, batchSizeActual);
            }
            
        } catch (Exception e) {
            failedBatches.incrementAndGet();
            logger.error("❌ Failed to save batch of {} records: {}", batchSizeActual, e.getMessage());
            logger.debug("Full error details:", e);
            
            // ✅ FIXED: Fallback to individual saves
            saveIndividually(batch);
        }
    }
    
    /**
     * ✅ FIXED: Fallback method using individual saves
     */
    private void saveIndividually(List<VehicleHistory> batch) {
        int saved = 0, failed = 0;
        
        for (VehicleHistory history : batch) {
            try {
                vehicleHistoryService.saveVehicleHistory(history);
                saved++;
                
            } catch (Exception e) {
                failed++;
                logger.error("❌ Failed to save individual record for device: {}", 
                           getDeviceId(history), e);
            }
        }
        
        logger.info("🔧 Individual save complete: {} saved, {} failed from batch of {}", 
                   saved, failed, batch.size());
        totalRecordsSaved.addAndGet(saved);
    }
    
    /**
     * Processes overflow queue
     */
    private void processOverflowQueue() {
        if (overflowQueue.isEmpty()) {
            return;
        }
        
        List<VehicleHistory> overflowBatch = new ArrayList<>(batchSize);
        overflowQueue.drainTo(overflowBatch, batchSize);
        
        if (!overflowBatch.isEmpty()) {
            logger.info("🔄 Processing {} records from overflow queue", overflowBatch.size());
            saveBatch(overflowBatch);
        }
    }
    
    /**
     * Determines if batch should be flushed
     */
    private boolean shouldFlush(List<VehicleHistory> batch) {
        if (batch.isEmpty()) {
            return false;
        }
        
        // Flush if batch is full
        if (batch.size() >= batchSize) {
            return true;
        }
        
        // Flush if oldest record is too old
        VehicleHistory oldest = batch.get(0);
        long age = System.currentTimeMillis() - oldest.getTimestamp().getTime();
        return age > maxWaitTime;
    }
    
    /**
     * Checks if force flush is needed
     */
    private boolean shouldForceFlush() {
        return System.currentTimeMillis() - lastFlushTime > flushInterval * 2;
    }
    
    /**
     * Gets batch processing statistics
     */
    public BatchStatistics getStatistics() {
        long avgBatchSaveTime = 0;
        if (!batchSaveTimes.isEmpty()) {
            avgBatchSaveTime = batchSaveTimes.values().stream()
                .mapToLong(Long::longValue)
                .sum() / batchSaveTimes.size();
        }
        
        int currentQueueSize = batchQueues.stream()
            .mapToInt(Queue::size)
            .sum() + overflowQueue.size();
        
        return new BatchStatistics(
            totalRecordsSaved.get(),
            totalBatchesSaved.get(),
            failedBatches.get(),
            rejectedRecords.get(),
            currentQueueSize,
            avgBatchSaveTime,
            getTopDevices()
        );
    }
    
    /**
     * Gets top devices by message count
     */
    private Map<String, Long> getTopDevices() {
        return deviceMessageCounts.entrySet().stream()
            .sorted(Map.Entry.<String, AtomicLong>comparingByValue(
                (a, b) -> Long.compare(b.get(), a.get()))
            )
            .limit(10)
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> e.getValue().get(),
                (e1, e2) -> e1,
                LinkedHashMap::new
            ));
    }
    
    /**
     * Async version of addToBatch
     */
    @Async
    public CompletableFuture<Boolean> addToBatchAsync(VehicleHistory history) {
        return CompletableFuture.completedFuture(addToBatch(history));
    }
    
    /**
     * Force flush all queues
     */
    public void forceFlushAll() {
        logger.info("🔄 Force flushing all batch queues");
        
        for (int i = 0; i < batchQueues.size(); i++) {
            BlockingQueue<VehicleHistory> queue = batchQueues.get(i);
            List<VehicleHistory> batch = new ArrayList<>();
            queue.drainTo(batch);
            
            if (!batch.isEmpty()) {
                saveBatch(batch);
            }
        }
        
        // Process overflow queue
        processOverflowQueue();
    }
    
    @PreDestroy
    public void shutdown() {
        logger.info("🔄 Shutting down BatchPersistenceService...");
        
        // Stop accepting new records
        batchExecutor.shutdown();
        
        try {
            // Wait for threads to finish
            if (!batchExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                batchExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            batchExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        // Final flush
        forceFlushAll();
        
        // Log final statistics
        logger.info("✅ BatchPersistenceService shutdown complete. Total records saved: {}, " +
            "Total batches: {}, Failed batches: {}, Rejected records: {}", 
            totalRecordsSaved.get(), totalBatchesSaved.get(), 
            failedBatches.get(), rejectedRecords.get());
    }
    
    /**
     * Batch statistics class
     */
    public static class BatchStatistics {
        private final long totalRecordsSaved;
        private final long totalBatchesSaved;
        private final long failedBatches;
        private final long rejectedRecords;
        private final int currentQueueSize;
        private final long avgBatchSaveTime;
        private final Map<String, Long> topDevices;
        
        public BatchStatistics(long totalRecordsSaved, long totalBatchesSaved, 
                             long failedBatches, long rejectedRecords, 
                             int currentQueueSize, long avgBatchSaveTime,
                             Map<String, Long> topDevices) {
            this.totalRecordsSaved = totalRecordsSaved;
            this.totalBatchesSaved = totalBatchesSaved;
            this.failedBatches = failedBatches;
            this.rejectedRecords = rejectedRecords;
            this.currentQueueSize = currentQueueSize;
            this.avgBatchSaveTime = avgBatchSaveTime;
            this.topDevices = topDevices;
        }
        
        // Getters
        public long getTotalRecordsSaved() { return totalRecordsSaved; }
        public long getTotalBatchesSaved() { return totalBatchesSaved; }
        public long getFailedBatches() { return failedBatches; }
        public long getRejectedRecords() { return rejectedRecords; }
        public int getCurrentQueueSize() { return currentQueueSize; }
        public long getAvgBatchSaveTime() { return avgBatchSaveTime; }
        public Map<String, Long> getTopDevices() { return topDevices; }
        
        public double getSuccessRate() {
            if (totalBatchesSaved == 0) return 0;
            return ((double)(totalBatchesSaved - failedBatches) / totalBatchesSaved) * 100;
        }
        
        @Override
        public String toString() {
            return String.format(
                "BatchStatistics{saved=%d, batches=%d, failed=%d, rejected=%d, " +
                "queueSize=%d, avgTime=%dms, successRate=%.1f%%}",
                totalRecordsSaved, totalBatchesSaved, failedBatches, rejectedRecords,
                currentQueueSize, avgBatchSaveTime, getSuccessRate()
            );
        }
    }
    
    /**
     * ✅ COMPATIBILITY: Method to add GPS data from JSON string
     */
    public void addGpsDataFromJson(String gpsJsonData) {
        try {
            // Parse your GPS JSON data and convert to VehicleHistory
            // This is a placeholder - you'll need to implement based on your JSON format
            logger.info("📥 Adding GPS data from JSON: {}", gpsJsonData);
            
            // Example implementation:
            // VehicleHistory history = parseGpsJson(gpsJsonData);
            // addToBatch(history);
            
        } catch (Exception e) {
            logger.error("❌ Error processing GPS JSON data: {}", e.getMessage(), e);
        }
    }
    
    /**
     * ✅ COMPATIBILITY: Batch add multiple GPS records
     */
    public void addMultipleGpsRecords(List<VehicleHistory> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        
        logger.info("📥 Adding {} GPS records to batch", records.size());
        
        for (VehicleHistory record : records) {
            addToBatch(record);
        }
    }
    
    /**
     * ✅ COMPATIBILITY: Get queue status
     */
    public String getQueueStatus() {
        int totalQueued = batchQueues.stream()
            .mapToInt(Queue::size)
            .sum() + overflowQueue.size();
        
        return String.format("Queued: %d, Saved: %d, Failed: %d", 
                           totalQueued, totalRecordsSaved.get(), failedBatches.get());
    }
}