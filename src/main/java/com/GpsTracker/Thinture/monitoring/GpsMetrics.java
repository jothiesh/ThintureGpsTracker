package com.GpsTracker.Thinture.monitoring;

import io.micrometer.core.instrument.*;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

@Component
public class GpsMetrics {
    
    private final Counter gpsMessagesReceived;
    private final Counter gpsMessagesSaved;
    private final Counter gpsMessagesFaileed;
    private final Timer gpsProcessingTime;
    private final Gauge activeMqttConnections;
    private final Gauge activeWebSocketConnections;
    private final Gauge cacheHitRatio;
    private final Gauge batchQueueSize;
    private final AtomicInteger mqttConnectionCount = new AtomicInteger(0);
    private final AtomicInteger webSocketConnectionCount = new AtomicInteger(0);
    private final AtomicInteger currentBatchQueueSize = new AtomicInteger(0);
    
    private final DistributionSummary messageSizeDistribution;
    private final Timer databaseSaveTime;
    private final Counter cacheHits;
    private final Counter cacheMisses;
    
    public GpsMetrics(MeterRegistry meterRegistry) {
        this.gpsMessagesReceived = Counter.builder("gps.messages.received")
                .description("Total GPS messages received from devices")
                .tag("type", "mqtt")
                .register(meterRegistry);
                
        this.gpsMessagesSaved = Counter.builder("gps.messages.saved")
                .description("Total GPS messages saved to database")
                .tag("type", "database")
                .register(meterRegistry);
                
        this.gpsMessagesFaileed = Counter.builder("gps.messages.failed")
                .description("Total GPS messages failed to process")
                .tag("type", "error")
                .register(meterRegistry);
                
        this.gpsProcessingTime = Timer.builder("gps.processing.time")
                .description("Time taken to process GPS messages")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry);
                
        this.activeMqttConnections = Gauge.builder("mqtt.connections.active", mqttConnectionCount, AtomicInteger::get)
                .description("Number of active MQTT connections")
                .register(meterRegistry);
                
        this.activeWebSocketConnections = Gauge.builder("websocket.connections.active", webSocketConnectionCount, AtomicInteger::get)
                .description("Number of active WebSocket connections")
                .register(meterRegistry);
                
        this.cacheHitRatio = Gauge.builder("cache.hit.ratio", this, GpsMetrics::calculateCacheHitRatio)
                .description("Cache hit ratio percentage")
                .register(meterRegistry);
                
        this.batchQueueSize = Gauge.builder("batch.queue.size", currentBatchQueueSize, AtomicInteger::get)
                .description("Current batch queue size")
                .register(meterRegistry);
                
        this.messageSizeDistribution = DistributionSummary.builder("gps.message.size")
                .description("Distribution of GPS message sizes")
                .baseUnit("bytes")
                .register(meterRegistry);
                
        this.databaseSaveTime = Timer.builder("database.save.time")
                .description("Time taken to save to database")
                .tag("operation", "batch_save")
                .register(meterRegistry);
                
        this.cacheHits = Counter.builder("cache.hits")
                .description("Number of cache hits")
                .register(meterRegistry);
                
        this.cacheMisses = Counter.builder("cache.misses")
                .description("Number of cache misses")
                .register(meterRegistry);
    }
    
    public void incrementMessagesReceived() {
        gpsMessagesReceived.increment();
    }
    
    public void incrementMessagesSaved() {
        gpsMessagesSaved.increment();
    }
    
    public void incrementMessagesFailed() {
        gpsMessagesFaileed.increment();
    }
    
    public void recordProcessingTime(long milliseconds) {
        gpsProcessingTime.record(milliseconds, java.util.concurrent.TimeUnit.MILLISECONDS);
    }
    
    public Timer.Sample startProcessingTimer() {
        return Timer.start();
    }
    
    public void stopProcessingTimer(Timer.Sample sample) {
        sample.stop(gpsProcessingTime);
    }
    
    public void setMqttConnectionCount(int count) {
        mqttConnectionCount.set(count);
    }
    
    public void incrementMqttConnections() {
        mqttConnectionCount.incrementAndGet();
    }
    
    public void decrementMqttConnections() {
        mqttConnectionCount.decrementAndGet();
    }
    
    public void setWebSocketConnectionCount(int count) {
        webSocketConnectionCount.set(count);
    }
    
    public void incrementWebSocketConnections() {
        webSocketConnectionCount.incrementAndGet();
    }
    
    public void decrementWebSocketConnections() {
        webSocketConnectionCount.decrementAndGet();
    }
    
    public void recordMessageSize(int bytes) {
        messageSizeDistribution.record(bytes);
    }
    
    public void recordDatabaseSaveTime(long milliseconds) {
        databaseSaveTime.record(milliseconds, java.util.concurrent.TimeUnit.MILLISECONDS);
    }
    
    public void incrementCacheHits() {
        cacheHits.increment();
    }
    
    public void incrementCacheMisses() {
        cacheMisses.increment();
    }
    
    public void setBatchQueueSize(int size) {
        currentBatchQueueSize.set(size);
    }
    
    private double calculateCacheHitRatio() {
        double hits = cacheHits.count();
        double misses = cacheMisses.count();
        double total = hits + misses;
        return total > 0 ? (hits / total) * 100 : 0;
    }
    
    public double getMessagesPerSecond() {
        return gpsMessagesReceived.count() / (System.currentTimeMillis() / 1000.0);
    }
    
    public long getTotalMessagesReceived() {
        return (long) gpsMessagesReceived.count();
    }
    
    public long getTotalMessagesSaved() {
        return (long) gpsMessagesSaved.count();
    }
    
    public long getTotalMessagesFailed() {
        return (long) gpsMessagesFaileed.count();
    }
}