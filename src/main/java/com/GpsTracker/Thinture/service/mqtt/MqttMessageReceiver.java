package com.GpsTracker.Thinture.service.mqtt;

import com.GpsTracker.Thinture.service.processing.GpsDataProcessor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.paho.client.mqttv3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * 🌟 ENHANCED MQTT Message Receiver - NO INTERFACE VERSION
 * ✅ Colorful JSON message visualization
 * ✅ Batch processing for performance
 * ✅ Real-time device monitoring
 * ✅ Advanced message validation
 * ✅ Performance metrics tracking
 * ✅ NO PROXY ISSUES!
 */
@Service
public class MqttMessageReceiver {
    
    private static final Logger logger = LoggerFactory.getLogger(MqttMessageReceiver.class);
    
    // 🎨 ANSI Color codes for beautiful console output
    private static final String RESET = "\u001B[0m";
    private static final String BLACK = "\u001B[30m";
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
    private static final String BRIGHT_WHITE = "\u001B[96m";
    
    @Autowired
    private GpsDataProcessor gpsDataProcessor;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    
    // 📊 Performance metrics
    private final AtomicLong lastMessageTimestamp = new AtomicLong(0);
    private final AtomicLong messageCount = new AtomicLong(0);
    private final AtomicLong totalBytesReceived = new AtomicLong(0);
    private final AtomicLong validMessagesCount = new AtomicLong(0);
    private final AtomicLong invalidMessagesCount = new AtomicLong(0);
    private final AtomicInteger activeDevices = new AtomicInteger(0);
    
    // 📦 Batch processing for performance
    private final Queue<GpsMessage> messageBatch = new ConcurrentLinkedQueue<>();
    private final ReentrantLock batchLock = new ReentrantLock();
    private static final int BATCH_SIZE = 100;
    private static final int MAX_BATCH_WAIT_MS = 2000;
    
    // 🔍 Device tracking and monitoring
    private final Map<String, DeviceInfo> deviceTracker = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> deviceMessageCounts = new ConcurrentHashMap<>();
    private final Map<String, String> deviceLastMessages = new ConcurrentHashMap<>();
    
    // 📈 Real-time statistics
    private final AtomicLong startTime = new AtomicLong(System.currentTimeMillis());
    private final AtomicInteger messagesPerSecond = new AtomicInteger(0);
    private final AtomicInteger peakMessagesPerSecond = new AtomicInteger(0);
    private static final StringBuilder logBuffer = new StringBuilder(256);
    @PostConstruct
    public void initialize() {
        logColorful("🌟 INITIALIZING ENHANCED MQTT MESSAGE RECEIVER (NO PROXY)", BRIGHT_GREEN);
        logColorful("📦 Batch size: " + BATCH_SIZE + " messages", BLUE);
        logColorful("⏱️ Max batch wait: " + MAX_BATCH_WAIT_MS + "ms", BLUE);
        logColorful("🔄 Ready to process messages from 5000+ devices", BRIGHT_CYAN);
        
        // Initialize metrics
        startTime.set(System.currentTimeMillis());
        logColorful("✅ Message receiver initialized successfully", GREEN);
    }
    
    // ================================
    // MQTT CALLBACK METHODS (PUBLIC)
    // ================================
    
    public void connectionLost(Throwable cause) {
        logColorful("❌ MQTT CONNECTION LOST!", BRIGHT_RED);
        logColorful("🔍 Cause: " + cause.getMessage(), RED);
        logColorful("📊 Messages processed before disconnect: " + messageCount.get(), YELLOW);
        logDeviceStatus();
    }
    
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        // 📊 Update basic metrics
        long currentTime = System.currentTimeMillis();
        lastMessageTimestamp.set(currentTime);
        messageCount.incrementAndGet();
        totalBytesReceived.addAndGet(message.getPayload().length);
        
        try {
            String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
            
            // 🔍 Skip empty messages
            if (payload.trim().isEmpty()) {
                logColorful("⚠️ Empty message received on topic: " + topic, YELLOW);
                return;
            }
            
            // 📋 Parse and validate message
            GpsMessage gpsMessage = parseAndValidateMessage(topic, payload, currentTime);
            
            if (gpsMessage != null) {
                validMessagesCount.incrementAndGet();
                updateDeviceTracking(gpsMessage);
                displayMessageInfo(gpsMessage);
                addToBatch(gpsMessage);
            } else {
                invalidMessagesCount.incrementAndGet();
                logColorful("❌ Invalid message format on topic: " + topic, RED);
            }
            
        } catch (Exception e) {
            invalidMessagesCount.incrementAndGet();
            logColorful("💥 ERROR processing message from topic " + topic, BRIGHT_RED);
            logColorful("🔍 Error details: " + e.getMessage(), RED);
            logger.error("Message processing error", e);
        }
    }
    
    public void deliveryComplete(IMqttDeliveryToken token) {
        logColorful("✅ Message delivery complete", BRIGHT_GREEN);
    }
    
    public void connectComplete(boolean reconnect, String serverURI) {
        String action = reconnect ? "🔄 RECONNECTED" : "🔗 CONNECTED";
        logColorful(action + " to MQTT broker: " + serverURI, BRIGHT_GREEN);
        
        if (reconnect) {
            logColorful("📊 Messages processed before reconnect: " + messageCount.get(), CYAN);
        }
    }
    
    // ================================
    // SCHEDULED METHODS
    // ================================
    
    @Scheduled(fixedDelay = 2000)
    public void processBatchScheduled() {
        batchLock.lock();
        try {
            if (!messageBatch.isEmpty()) {
                long waitTime = System.currentTimeMillis() - lastMessageTimestamp.get();
                if (waitTime > MAX_BATCH_WAIT_MS) {
                    logColorful("⏱️ Batch timeout reached, processing " + messageBatch.size() + " messages", BRIGHT_BLUE);
                    processBatch();
                }
            }
        } finally {
            batchLock.unlock();
        }
    }
    
    @Scheduled(fixedRate = 30000)
    public void printStatistics() {
        long currentTime = System.currentTimeMillis();
        long runtime = currentTime - startTime.get();
        
        // 📈 Calculate rates
        double messagesPerMin = (messageCount.get() * 60000.0) / runtime;
        double bytesPerSec = (totalBytesReceived.get() * 1000.0) / runtime;
        
        logColorful("", "");
        logColorful("╔═══════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════", BRIGHT_CYAN);
        logColorful("║ 📊 MQTT MESSAGE RECEIVER STATISTICS", BRIGHT_CYAN);
        logColorful("╠═══════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════", BRIGHT_CYAN);
        logColorful("║ 📨 Total Messages: " + messageCount.get(), BRIGHT_GREEN);
        logColorful("║ ✅ Valid Messages: " + validMessagesCount.get(), GREEN);
        logColorful("║ ❌ Invalid Messages: " + invalidMessagesCount.get(), RED);
        logColorful("║ 📱 Active Devices: " + activeDevices.get(), BRIGHT_YELLOW);
        logColorful("║ 📊 Total Bytes: " + formatBytes(totalBytesReceived.get()), BRIGHT_BLUE);
        logColorful("║ 📈 Messages/Min: " + String.format("%.2f", messagesPerMin), PURPLE);
        logColorful("║ 📈 Bytes/Sec: " + String.format("%.2f", bytesPerSec), PURPLE);
        logColorful("║ ⏱️ Runtime: " + formatDuration(runtime), CYAN);
        logColorful("║ 📦 Batch Queue: " + messageBatch.size(), BLUE);
        logColorful("╚═══════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════", BRIGHT_CYAN);
        
        showTopActiveDevices();
        cleanupInactiveDevices(currentTime);
    }
    
    // ================================
    // REST OF YOUR EXISTING METHODS
    // ================================
    
    // ... (copy all your existing private methods here - parseAndValidateMessage, parseJsonMessage, etc.)
    
    private GpsMessage parseAndValidateMessage(String topic, String payload, long timestamp) {
        try {
            if (payload.trim().startsWith("{") && payload.trim().endsWith("}")) {
                JsonNode jsonNode = objectMapper.readTree(payload);
                return parseJsonMessage(topic, jsonNode, payload, timestamp);
            } else {
                return parseCustomMessage(topic, payload, timestamp);
            }
        } catch (Exception e) {
            logColorful("⚠️ Failed to parse message: " + e.getMessage(), YELLOW);
            return null;
        }
    }
    
    private GpsMessage parseJsonMessage(String topic, JsonNode json, String payload, long timestamp) {
        try {
            String deviceId = extractDeviceId(topic, json);
            double latitude = json.has("latitude") ? json.get("latitude").asDouble() : 0.0;
            double longitude = json.has("longitude") ? json.get("longitude").asDouble() : 0.0;
            double speed = json.has("speed") ? json.get("speed").asDouble() : 0.0;
            double heading = json.has("heading") ? json.get("heading").asDouble() : 0.0;
            String messageTimestamp = json.has("timestamp") ? json.get("timestamp").asText() : "";
            
            if (isValidGpsCoordinates(latitude, longitude)) {
                return new GpsMessage(deviceId, topic, payload, latitude, longitude, 
                                    speed, heading, messageTimestamp, timestamp, true);
            } else {
                logColorful("❌ Invalid GPS coordinates: " + latitude + ", " + longitude, RED);
                return null;
            }
        } catch (Exception e) {
            logColorful("💥 JSON parsing error: " + e.getMessage(), BRIGHT_RED);
            return null;
        }
    }
    
    private GpsMessage parseCustomMessage(String topic, String payload, long timestamp) {
        try {
            String[] parts = payload.split(",");
            if (parts.length >= 3) {
                String deviceId = parts[0];
                double latitude = Double.parseDouble(parts[1]);
                double longitude = Double.parseDouble(parts[2]);
                double speed = parts.length > 3 ? Double.parseDouble(parts[3]) : 0.0;
                double heading = parts.length > 4 ? Double.parseDouble(parts[4]) : 0.0;
                String messageTimestamp = parts.length > 5 ? parts[5] : "";
                
                if (isValidGpsCoordinates(latitude, longitude)) {
                    return new GpsMessage(deviceId, topic, payload, latitude, longitude, 
                                        speed, heading, messageTimestamp, timestamp, false);
                }
            }
            return null;
        } catch (Exception e) {
            logColorful("💥 Custom format parsing error: " + e.getMessage(), BRIGHT_RED);
            return null;
        }
    }
    
    private String extractDeviceId(String topic, JsonNode json) {
        if (json.has("deviceId")) {
            return json.get("deviceId").asText();
        }
        String[] topicParts = topic.split("/");
        for (int i = 0; i < topicParts.length; i++) {
            if ("device".equals(topicParts[i]) && i + 1 < topicParts.length) {
                return topicParts[i + 1];
            }
        }
        return topic.replaceAll("[^a-zA-Z0-9]", "_");
    }
    
    private boolean isValidGpsCoordinates(double latitude, double longitude) {
        return latitude >= -90 && latitude <= 90 && 
               longitude >= -180 && longitude <= 180 &&
               !(latitude == 0 && longitude == 0);
    }
    
    private void updateDeviceTracking(GpsMessage message) {
        String deviceId = message.getDeviceId();
        long currentTime = System.currentTimeMillis();
        
        deviceTracker.compute(deviceId, (id, existing) -> {
            if (existing == null) {
                activeDevices.incrementAndGet();
                logColorful("🆕 NEW DEVICE DETECTED: " + deviceId, BRIGHT_GREEN);
                return new DeviceInfo(deviceId, currentTime, currentTime, 1);
            } else {
                existing.lastSeen = currentTime;
                existing.messageCount++;
                return existing;
            }
        });
        
        deviceMessageCounts.computeIfAbsent(deviceId, k -> new AtomicLong(0)).incrementAndGet();
        deviceLastMessages.put(deviceId, message.getPayload());
    }
    
    private void displayMessageInfo(GpsMessage message) {
        // Your existing display logic...
        logColorful("📨 Message from device: " + message.getDeviceId(), BRIGHT_GREEN);
    }
    
    private void addToBatch(GpsMessage message) {
        batchLock.lock();
        try {
            messageBatch.offer(message);
            if (messageBatch.size() >= BATCH_SIZE) {
                logColorful("📦 Batch size reached (" + BATCH_SIZE + "), processing...", BRIGHT_BLUE);
                processBatch();
            }
        } finally {
            batchLock.unlock();
        }
    }
    
    private void processBatch() {
        if (messageBatch.isEmpty()) return;
        
        List<GpsMessage> batch = new ArrayList<>();
        GpsMessage message;
        while ((message = messageBatch.poll()) != null) {
            batch.add(message);
        }
        
        if (!batch.isEmpty()) {
            logColorful("🚀 Processing batch of " + batch.size() + " messages", BRIGHT_BLUE);
            processBatchAsync(batch);
        }
    }
    
    @Async
    public void processBatchAsync(List<GpsMessage> batch) {
        try {
            long startTime = System.currentTimeMillis();
            for (GpsMessage message : batch) {
                gpsDataProcessor.processPayloadAsync(message.getPayload());
            }
            long duration = System.currentTimeMillis() - startTime;
            logColorful("✅ Batch processed in " + duration + "ms", BRIGHT_GREEN);
        } catch (Exception e) {
            logColorful("❌ Batch processing error: " + e.getMessage(), BRIGHT_RED);
            logger.error("Batch processing failed", e);
        }
    }
    
    private void showTopActiveDevices() {
        // Your existing logic...
    }
    
    private void cleanupInactiveDevices(long currentTime) {
        // Your existing logic...
    }
    
    private void logDeviceStatus() {
        // Your existing logic...
    }
    
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }
    
    private String formatDuration(long millis) {
        if (millis < 1000) return millis + "ms";
        if (millis < 60000) return String.format("%.1fs", millis / 1000.0);
        if (millis < 3600000) return String.format("%.1fm", millis / 60000.0);
        return String.format("%.1fh", millis / 3600000.0);
    }
    
    private void logColorful(String message, String color) {
        System.out.println(color + message + RESET);
        logger.info(message.replaceAll("║", "").replaceAll("╔", "").replaceAll("╚", "").replaceAll("╠", "").replaceAll("═", "").trim());
    }
    
    // PUBLIC METRICS METHODS
    public long getLastMessageTimestamp() { return lastMessageTimestamp.get(); }
    public long getMessageCount() { return messageCount.get(); }
    public long getTotalBytesReceived() { return totalBytesReceived.get(); }
    public long getValidMessagesCount() { return validMessagesCount.get(); }
    public long getInvalidMessagesCount() { return invalidMessagesCount.get(); }
    public int getActiveDevices() { return activeDevices.get(); }
    public int getBatchQueueSize() { return messageBatch.size(); }
    
    public double getMessagesPerSecond() {
        long runtime = System.currentTimeMillis() - startTime.get();
        return runtime > 0 ? (messageCount.get() * 1000.0) / runtime : 0;
    }
    
    public Map<String, Long> getDeviceMessageCounts() {
        Map<String, Long> result = new HashMap<>();
        deviceMessageCounts.forEach((deviceId, count) -> result.put(deviceId, count.get()));
        return result;
    }
    
    // INNER CLASSES
    private static class DeviceInfo {
        String deviceId;
        long firstSeen;
        long lastSeen;
        long messageCount;
        
        DeviceInfo(String deviceId, long firstSeen, long lastSeen, long messageCount) {
            this.deviceId = deviceId;
            this.firstSeen = firstSeen;
            this.lastSeen = lastSeen;
            this.messageCount = messageCount;
        }
    }
    
    public static class GpsMessage {
        private final String deviceId;
        private final String topic;
        private final String payload;
        private final double latitude;
        private final double longitude;
        private final double speed;
        private final double heading;
        private final String timestamp;
        private final long receivedTime;
        private final boolean jsonFormat;
        
        public GpsMessage(String deviceId, String topic, String payload, double latitude, 
                         double longitude, double speed, double heading, String timestamp, 
                         long receivedTime, boolean jsonFormat) {
            this.deviceId = deviceId;
            this.topic = topic;
            this.payload = payload;
            this.latitude = latitude;
            this.longitude = longitude;
            this.speed = speed;
            this.heading = heading;
            this.timestamp = timestamp;
            this.receivedTime = receivedTime;
            this.jsonFormat = jsonFormat;
        }
        
        // Getters
        public String getDeviceId() { return deviceId; }
        public String getTopic() { return topic; }
        public String getPayload() { return payload; }
        public double getLatitude() { return latitude; }
        public double getLongitude() { return longitude; }
        public double getSpeed() { return speed; }
        public double getHeading() { return heading; }
        public String getTimestamp() { return timestamp; }
        public long getReceivedTime() { return receivedTime; }
        public boolean isJsonFormat() { return jsonFormat; }
    }
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
  //  / In MqttMessageReceiver.java - ADD this method for better cleanup

    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void cleanupDeviceTracking() {
        long currentTime = System.currentTimeMillis();
        long cleanupThreshold = 24 * 60 * 60 * 1000; // 24 hours
        
        // Clean up inactive devices from all tracking maps
        Set<String> inactiveDevices = deviceTracker.entrySet().stream()
            .filter(entry -> currentTime - entry.getValue().lastSeen > cleanupThreshold)
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());
        
        for (String deviceId : inactiveDevices) {
            deviceTracker.remove(deviceId);
            deviceMessageCounts.remove(deviceId);
            deviceLastMessages.remove(deviceId);
            activeDevices.decrementAndGet();
            logColorful("🧹 Cleaned up inactive device: " + deviceId, YELLOW);
        }
        
        logColorful("🧹 Device cleanup completed: " + inactiveDevices.size() + " devices removed", CYAN);
    }
    
    
    /**
     * 🚀 Optimized colorful logging with string pooling
     */
    private void logColorfulOptimized(String message, String color) {
        // Reuse StringBuilder to reduce GC pressure
        logBuffer.setLength(0);
        logBuffer.append(color).append(message).append(RESET);
        System.out.println(logBuffer.toString());
        
        // Clean message for file logging (remove box drawing characters efficiently)
        String cleanMessage = message.length() > 0 && message.charAt(0) == '║' ? 
            message.substring(2).trim() : message;
        
        logger.info(cleanMessage);
    }

    /**
     * 📊 Optimized device display with pagination
     */
    private void showTopActiveDevicesOptimized() {
        if (deviceMessageCounts.isEmpty()) return;
        
        // Use stream with limit for better performance
        List<Map.Entry<String, AtomicLong>> topDevices = deviceMessageCounts.entrySet()
            .stream()
            .sorted(Map.Entry.<String, AtomicLong>comparingByValue(
                (a, b) -> Long.compare(b.get(), a.get())))
            .limit(10) // Only show top 10
            .collect(Collectors.toList());
        
        logColorfulOptimized("🏆 TOP " + Math.min(10, topDevices.size()) + " ACTIVE DEVICES", BRIGHT_YELLOW);
        
        for (int i = 0; i < topDevices.size(); i++) {
            Map.Entry<String, AtomicLong> entry = topDevices.get(i);
            String deviceId = entry.getKey();
            long count = entry.getValue().get();
            
            logColorfulOptimized(String.format("║ %d. %s: %,d messages", 
                i + 1, deviceId, count), YELLOW);
        }
    }
}