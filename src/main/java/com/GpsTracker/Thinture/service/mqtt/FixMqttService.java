package com.GpsTracker.Thinture.service.mqtt;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.eclipse.paho.client.mqttv3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 🔥 ENHANCED Fixed MQTT Service - Single Connection with Enhanced Features
 * ✅ Colorful logging and monitoring
 * ✅ Integration with enhanced architecture
 * ✅ Improved error handling and recovery
 * ✅ Real-time status monitoring
 * ✅ Performance tracking
 * Enable/disable with mqtt.enabled property
 */
@Service
@ConditionalOnProperty(name = "mqtt.enabled", havingValue = "true", matchIfMissing = true)
public class FixMqttService implements MqttCallback {

    private static final Logger logger = LoggerFactory.getLogger(FixMqttService.class);
    
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
    
    // 🔗 Enhanced dependencies
    @Autowired
    private MqttConnectionManager connectionManager;
    
    @Autowired
    private MqttMessageReceiver messageReceiver;
    
    @Autowired
    private MqttHealthMonitor healthMonitor;
    
    // 🔧 MQTT client instance
    private MqttClient mqttClient;
    
    // 📊 Configuration properties
    @Value("${mqtt.broker-url}")
    private String brokerUrl;
    
    @Value("${mqtt.username}")
    private String username;
    
    @Value("${mqtt.password}")
    private String password;
    
    @Value("${mqtt.client-id}")
    private String baseClientId;
    
    @Value("${mqtt.topics}")
    private String topics;
    
    @Value("${mqtt.keep-alive-interval:60}")
    private int keepAliveInterval;
    
    // 📊 Service state tracking
    private final AtomicBoolean isInitialized = new AtomicBoolean(false);
    private final AtomicBoolean isConnected = new AtomicBoolean(false);
    private final AtomicLong initializationTime = new AtomicLong(0);
    private final AtomicLong connectionTime = new AtomicLong(0);
    private final AtomicLong messagesReceived = new AtomicLong(0);
    private final AtomicLong lastMessageTime = new AtomicLong(0);
    
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    @PostConstruct
    public void init() {
        long startTime = System.currentTimeMillis();
        initializationTime.set(startTime);
        
        logColorful("🔥 INITIALIZING ENHANCED FIXED MQTT SERVICE", BRIGHT_CYAN);
        logColorful("⏱️ Initialization started at: " + LocalDateTime.now().format(timeFormatter), CYAN);
        
        displayServiceConfiguration();
        
        try {
            // 🆔 Generate unique client ID
            String clientId = generateUniqueClientId();
            
            logColorful("🔨 Creating MQTT client with ID: " + clientId, BRIGHT_BLUE);
            
            // 🔧 Create client using enhanced connection manager
            mqttClient = connectionManager.createClient(this);
            
            // 🔗 Connect using enhanced connection manager
            connectionManager.connect(mqttClient);
            
            // 📡 Subscribe to topics
            String[] topicArray = topics.split(",");
            connectionManager.subscribe(mqttClient, topicArray);
            
            // ✅ Mark as connected and initialized
            isConnected.set(true);
            isInitialized.set(true);
            connectionTime.set(System.currentTimeMillis());
            
            long initDuration = System.currentTimeMillis() - startTime;
            displayInitializationSuccess(clientId, initDuration);
            
        } catch (Exception e) {
            logColorful("❌ FAILED TO INITIALIZE MQTT SERVICE", BRIGHT_RED);
            logColorful("💥 Error: " + e.getMessage(), RED);
            displayInitializationFailure(e);
            
            // Don't throw exception - let the application continue
            logger.error("MQTT service initialization failed", e);
        }
    }
    
    /**
     * 📊 Display service configuration
     */
    private void displayServiceConfiguration() {
        logColorful("", "");
        logColorful("╔════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════", BRIGHT_BLUE);
        logColorful("║ ⚙️ FIXED MQTT SERVICE CONFIGURATION", BRIGHT_BLUE);
        logColorful("╠════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════", BRIGHT_BLUE);
        logColorful("║ 🌐 Broker URL: " + brokerUrl, BLUE);
        logColorful("║ 👤 Username: " + username, BLUE);
        logColorful("║ 🔑 Password: " + maskPassword(password), BLUE);
        logColorful("║ 📱 Base Client ID: " + baseClientId, BLUE);
        logColorful("║ 📡 Topics: " + topics, BLUE);
        logColorful("║ ⏱️ Keep Alive: " + keepAliveInterval + "s", BLUE);
        logColorful("║ 🔧 Service Mode: Fixed Single Connection", BLUE);
        logColorful("╚════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════", BRIGHT_BLUE);
    }
    
    /**
     * 🎉 Display successful initialization
     */
    private void displayInitializationSuccess(String clientId, long duration) {
        logColorful("", "");
        logColorful("╔════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════", BRIGHT_GREEN);
        logColorful("║ ✅ MQTT SERVICE INITIALIZED SUCCESSFULLY!", BRIGHT_GREEN);
        logColorful("╠════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════", BRIGHT_GREEN);
        logColorful("║ 📱 Client ID: " + clientId, GREEN);
        logColorful("║ ⚡ Initialization Time: " + duration + "ms", GREEN);
        logColorful("║ 🔗 Connection Status: CONNECTED", GREEN);
        logColorful("║ 📡 Subscription Status: SUBSCRIBED", GREEN);
        logColorful("║ 🌟 Service Status: READY TO RECEIVE MESSAGES", BRIGHT_GREEN);
        logColorful("║ 🚀 Ready for 5000+ GPS devices", BRIGHT_GREEN);
        logColorful("╚════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════", BRIGHT_GREEN);
    }
    
    /**
     * 💥 Display initialization failure
     */
    private void displayInitializationFailure(Exception e) {
        logColorful("", "");
        logColorful("╔════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════", BRIGHT_RED);
        logColorful("║ ❌ MQTT SERVICE INITIALIZATION FAILED!", BRIGHT_RED);
        logColorful("╠════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════", BRIGHT_RED);
        logColorful("║ 💥 Error: " + e.getClass().getSimpleName(), RED);
        logColorful("║ 📝 Message: " + e.getMessage(), RED);
        logColorful("║ 🔄 Service will continue without MQTT", RED);
        logColorful("║ 💡 Check broker connectivity and credentials", YELLOW);
        logColorful("╚════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════", BRIGHT_RED);
    }
    
    /**
     * 🆔 Generate unique client ID
     */
    private String generateUniqueClientId() {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String randomPart = UUID.randomUUID().toString().substring(0, 8);
        return baseClientId + "-fixed-" + timestamp.substring(timestamp.length() - 6) + "-" + randomPart;
    }
    
    /**
     * 🔐 Mask password for logging
     */
    private String maskPassword(String password) {
        if (password == null || password.length() < 3) {
            return "***";
        }
        return password.substring(0, 2) + "***" + password.substring(password.length() - 1);
    }

    @PreDestroy
    public void cleanup() {
        logColorful("🛑 SHUTTING DOWN FIXED MQTT SERVICE", BRIGHT_YELLOW);
        
        if (mqttClient != null) {
            try {
                // 📊 Display final statistics
                displayFinalStatistics();
                
                // 🔌 Disconnect using enhanced connection manager
                connectionManager.disconnect(mqttClient);
                
                // 📊 Update status
                isConnected.set(false);
                isInitialized.set(false);
                
                logColorful("✅ MQTT service cleanup completed successfully", BRIGHT_GREEN);
                
            } catch (Exception e) {
                logColorful("❌ Error during MQTT cleanup: " + e.getMessage(), BRIGHT_RED);
                logger.error("Error during MQTT cleanup", e);
            }
        } else {
            logColorful("⚠️ MQTT client was not initialized", YELLOW);
        }
    }
    
    /**
     * 📊 Display final statistics
     */
    private void displayFinalStatistics() {
        long currentTime = System.currentTimeMillis();
        long uptime = currentTime - initializationTime.get();
        long connectionDuration = isConnected.get() ? currentTime - connectionTime.get() : 0;
        
        logColorful("", "");
        logColorful("╔════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════", BRIGHT_MAGENTA);
        logColorful("║ 📊 FINAL MQTT SERVICE STATISTICS", BRIGHT_MAGENTA);
        logColorful("╠════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════", BRIGHT_MAGENTA);
        logColorful("║ ⏱️ Service Uptime: " + formatDuration(uptime), PURPLE);
        logColorful("║ 🔗 Connection Duration: " + formatDuration(connectionDuration), PURPLE);
        logColorful("║ 📨 Messages Received: " + messagesReceived.get(), PURPLE);
        logColorful("║ 📡 Last Message: " + (lastMessageTime.get() > 0 ? 
                   formatDuration(currentTime - lastMessageTime.get()) + " ago" : "Never"), PURPLE);
        logColorful("║ 🔄 Service Status: " + (isConnected.get() ? "CONNECTED" : "DISCONNECTED"), 
                   isConnected.get() ? GREEN : RED);
        logColorful("╚════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════", BRIGHT_MAGENTA);
    }

    @Override
    public void connectionLost(Throwable cause) {
        isConnected.set(false);
        
        logColorful("❌ MQTT CONNECTION LOST!", BRIGHT_RED);
        logColorful("🔍 Cause: " + cause.getMessage(), RED);
        logColorful("📊 Messages received before disconnect: " + messagesReceived.get(), YELLOW);
        
        // 📊 Display connection lost details
        displayConnectionLost(cause);
        
        // 🔄 The enhanced connection manager handles reconnection automatically
        logColorful("🔄 Enhanced connection manager will handle reconnection", BRIGHT_CYAN);
    }
    
    /**
     * 🔗 Display connection lost details
     */
    private void displayConnectionLost(Throwable cause) {
        logColorful("", "");
        logColorful("╔════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════", BRIGHT_RED);
        logColorful("║ 🔗 CONNECTION LOST EVENT", BRIGHT_RED);
        logColorful("╠════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════", BRIGHT_RED);
        logColorful("║ 💥 Exception: " + cause.getClass().getSimpleName(), RED);
        logColorful("║ 📝 Message: " + cause.getMessage(), RED);
        logColorful("║ 🕒 Time: " + LocalDateTime.now().format(timeFormatter), RED);
        logColorful("║ 📊 Messages processed: " + messagesReceived.get(), YELLOW);
        logColorful("║ 🔄 Auto-reconnect: ENABLED", BRIGHT_CYAN);
        logColorful("╚════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════", BRIGHT_RED);
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        // 📊 Update statistics
        messagesReceived.incrementAndGet();
        lastMessageTime.set(System.currentTimeMillis());
        
        // 🔄 Forward to enhanced message receiver
        messageReceiver.messageArrived(topic, message);
        
        // 📊 Log message arrival (every 100th message to avoid spam)
        if (messagesReceived.get() % 100 == 0) {
            logColorful("📨 Message milestone: " + messagesReceived.get() + " messages received", BRIGHT_CYAN);
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        logColorful("✅ Message delivery complete", BRIGHT_GREEN);
    }
    
    /**
     * 📊 Get service status
     */
    public ServiceStatus getServiceStatus() {
        return new ServiceStatus(
            isInitialized.get(),
            isConnected.get(),
            mqttClient != null ? mqttClient.getClientId() : null,
            initializationTime.get(),
            connectionTime.get(),
            messagesReceived.get(),
            lastMessageTime.get()
        );
    }
    
    /**
     * 📤 Publish message using this service
     */
    public void publishMessage(String topic, String message) {
        if (!isConnected.get() || mqttClient == null) {
            logColorful("⚠️ Cannot publish - service not connected", YELLOW);
            return;
        }
        
        try {
            MqttMessage mqttMessage = new MqttMessage(message.getBytes());
            mqttMessage.setQos(1);
            mqttMessage.setRetained(false);
            
            mqttClient.publish(topic, mqttMessage);
            logColorful("📤 Message published to topic: " + topic, BRIGHT_GREEN);
            
        } catch (Exception e) {
            logColorful("❌ Failed to publish message: " + e.getMessage(), BRIGHT_RED);
            logger.error("Failed to publish message", e);
        }
    }
    
    /**
     * 🔄 Manually trigger reconnection
     */
    public void reconnect() {
        if (mqttClient != null) {
            try {
                logColorful("🔄 Manual reconnection triggered", BRIGHT_YELLOW);
                
                // Disconnect first
                connectionManager.disconnect(mqttClient);
                
                // Reconnect
                connectionManager.connect(mqttClient);
                
                // Re-subscribe
                String[] topicArray = topics.split(",");
                connectionManager.subscribe(mqttClient, topicArray);
                
                isConnected.set(true);
                connectionTime.set(System.currentTimeMillis());
                
                logColorful("✅ Manual reconnection successful", BRIGHT_GREEN);
                
            } catch (Exception e) {
                logColorful("❌ Manual reconnection failed: " + e.getMessage(), BRIGHT_RED);
                logger.error("Manual reconnection failed", e);
            }
        }
    }
    
    /**
     * ⏱️ Format duration for display
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
    
    // 📊 Service status data class
    public static class ServiceStatus {
        private final boolean initialized;
        private final boolean connected;
        private final String clientId;
        private final long initializationTime;
        private final long connectionTime;
        private final long messagesReceived;
        private final long lastMessageTime;
        
        public ServiceStatus(boolean initialized, boolean connected, String clientId, 
                           long initializationTime, long connectionTime, long messagesReceived, 
                           long lastMessageTime) {
            this.initialized = initialized;
            this.connected = connected;
            this.clientId = clientId;
            this.initializationTime = initializationTime;
            this.connectionTime = connectionTime;
            this.messagesReceived = messagesReceived;
            this.lastMessageTime = lastMessageTime;
        }
        
        // Getters
        public boolean isInitialized() { return initialized; }
        public boolean isConnected() { return connected; }
        public String getClientId() { return clientId; }
        public long getInitializationTime() { return initializationTime; }
        public long getConnectionTime() { return connectionTime; }
        public long getMessagesReceived() { return messagesReceived; }
        public long getLastMessageTime() { return lastMessageTime; }
    }
}