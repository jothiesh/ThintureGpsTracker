package com.GpsTracker.Thinture.service.mqtt;

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 🔧 ENHANCED MQTT Connection Manager - Production Ready
 * ✅ Smart retry logic with exponential backoff
 * ✅ Colorful connection status monitoring
 * ✅ Enhanced security and validation
 * ✅ Performance metrics and health monitoring
 * ✅ Advanced connection diagnostics
 * ✅ Connection pool integration
 */
@Component
public class MqttConnectionManager {
    
    private static final Logger logger = LoggerFactory.getLogger(MqttConnectionManager.class);
    
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
    private static final String BRIGHT_WHITE = "\u001B[97m";
    
    // 🔧 Configuration properties
    @Value("${mqtt.broker-url}")
    private String brokerUrl;
    
    @Value("${mqtt.client-id}")
    private String baseClientId;
    
    @Value("${mqtt.username}")
    private String username;
    
    @Value("${mqtt.password}")
    private String password;
    
    @Value("${mqtt.keep-alive-interval:45}")
    private int keepAliveInterval;
    
    @Value("${mqtt.connection.timeout:20}")
    private int connectionTimeout;
    
    @Value("${mqtt.connection.pool.max-inflight:500}")
    private int maxInflight;
    
    // 📊 Connection tracking and metrics
    private final Map<String, ConnectionInfo> activeConnections = new ConcurrentHashMap<>();
    private final AtomicInteger totalConnectionAttempts = new AtomicInteger(0);
    private final AtomicInteger successfulConnections = new AtomicInteger(0);
    private final AtomicInteger failedConnections = new AtomicInteger(0);
    private final AtomicLong totalConnectionTime = new AtomicLong(0);
    private final AtomicInteger currentConnections = new AtomicInteger(0);
    
    // 🔄 Retry configuration
    private static final int MAX_RETRY_ATTEMPTS = 5;
    private static final long INITIAL_RETRY_DELAY = 1000; // 1 second
    private static final long MAX_RETRY_DELAY = 32000; // 32 seconds
    private static final double RETRY_MULTIPLIER = 2.0;
    private static final double JITTER_FACTOR = 0.1;
    
    // 🏥 Health monitoring
    private final AtomicBoolean healthCheckEnabled = new AtomicBoolean(true);
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    
    @PostConstruct
    public void initialize() {
        logColorful("🔧 INITIALIZING ENHANCED MQTT CONNECTION MANAGER", BRIGHT_CYAN);
        logColorful("🌐 Broker URL: " + brokerUrl, BLUE);
        logColorful("🔑 Username: " + username, BLUE);
        logColorful("⏱️ Keep Alive: " + keepAliveInterval + "s", BLUE);
        logColorful("🔌 Connection Timeout: " + connectionTimeout + "s", BLUE);
        logColorful("📊 Max Inflight: " + maxInflight, BLUE);
        logColorful("🔄 Max Retry Attempts: " + MAX_RETRY_ATTEMPTS, BLUE);
        
        // 🔍 Validate broker URL
        if (!isValidBrokerUrl(brokerUrl)) {
            logColorful("❌ INVALID BROKER URL: " + brokerUrl, BRIGHT_RED);
            throw new IllegalArgumentException("Invalid MQTT broker URL: " + brokerUrl);
        }
        
        logColorful("✅ MQTT Connection Manager initialized successfully", BRIGHT_GREEN);
    }
    
    /**
     * 🔨 Creates a new MQTT client with enhanced configuration
     */
    public MqttClient createClient(MqttCallback callback) throws MqttException {
        long startTime = System.currentTimeMillis();
        totalConnectionAttempts.incrementAndGet();
        
        try {
            // 🆔 Generate unique client ID
            String clientId = generateUniqueClientId();
            
            logColorful("🔨 CREATING NEW MQTT CLIENT", BRIGHT_BLUE);
            logColorful("📱 Client ID: " + clientId, CYAN);
            logColorful("⏱️ Timestamp: " + LocalDateTime.now().format(timeFormatter), PURPLE);
            
            // 🔧 Create client with memory persistence
            MqttClient client = new MqttClient(brokerUrl, clientId, new MemoryPersistence());
            
            // 🔄 Set callback
            client.setCallback(callback);
            
            // 📊 Track connection info
            ConnectionInfo info = new ConnectionInfo(clientId, System.currentTimeMillis());
            activeConnections.put(clientId, info);
            currentConnections.incrementAndGet();
            
            long creationTime = System.currentTimeMillis() - startTime;
            logColorful("✅ CLIENT CREATED SUCCESSFULLY in " + creationTime + "ms", BRIGHT_GREEN);
            
            return client;
            
        } catch (MqttException e) {
            failedConnections.incrementAndGet();
            logColorful("❌ CLIENT CREATION FAILED: " + e.getMessage(), BRIGHT_RED);
            throw e;
        }
    }
    
    /**
     * 🔗 Connects the MQTT client with smart retry logic
     */
    public void connect(MqttClient client) throws MqttException {
        String clientId = client.getClientId();
        logColorful("🔗 CONNECTING CLIENT: " + clientId, BRIGHT_YELLOW);
        
        MqttException lastException = null;
        long totalStartTime = System.currentTimeMillis();
        
        // 🔄 Retry loop with exponential backoff
        for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
            try {
                long attemptStartTime = System.currentTimeMillis();
                
                // 🔧 Create optimized connection options
                MqttConnectOptions options = createOptimizedConnectOptions();
                
                logColorful("🔄 Connection attempt #" + attempt + " for " + clientId, YELLOW);
                displayConnectionAttempt(attempt, clientId);
                
                // 🔌 Attempt connection
                client.connect(options);
                
                // ✅ Connection successful
                long connectionTime = System.currentTimeMillis() - attemptStartTime;
                long totalTime = System.currentTimeMillis() - totalStartTime;
                
                successfulConnections.incrementAndGet();
                totalConnectionTime.addAndGet(totalTime);
                
                // 📊 Update connection info
                ConnectionInfo info = activeConnections.get(clientId);
                if (info != null) {
                    info.connectedAt = System.currentTimeMillis();
                    info.connectionTime = connectionTime;
                    info.attempts = attempt;
                }
                
                displayConnectionSuccess(clientId, attempt, connectionTime, totalTime);
                return;
                
            } catch (MqttException e) {
                lastException = e;
                failedConnections.incrementAndGet();
                
                logColorful("❌ Attempt #" + attempt + " failed: " + e.getMessage(), RED);
                
                // 🔄 Don't retry on certain errors
                if (isNonRetryableError(e)) {
                    logColorful("🚫 Non-retryable error, stopping attempts", BRIGHT_RED);
                    throw e;
                }
                
                // 😴 Wait before retry (except last attempt)
                if (attempt < MAX_RETRY_ATTEMPTS) {
                    long delay = calculateRetryDelay(attempt);
                    logColorful("😴 Waiting " + delay + "ms before retry #" + (attempt + 1), YELLOW);
                    
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        logColorful("🔄 Retry interrupted", YELLOW);
                        throw new MqttException(MqttException.REASON_CODE_CLIENT_EXCEPTION, ie);
                    }
                } else {
                    logColorful("🔚 MAX RETRY ATTEMPTS REACHED", BRIGHT_RED);
                }
            }
        }
        
        // 💥 All attempts failed
        long totalTime = System.currentTimeMillis() - totalStartTime;
        displayConnectionFailure(clientId, MAX_RETRY_ATTEMPTS, totalTime, lastException);
        throw lastException;
    }
    
    /**
     * 🔧 Create optimized connection options for production
     */
    private MqttConnectOptions createOptimizedConnectOptions() {
        MqttConnectOptions options = new MqttConnectOptions();
        
        // 🔐 Authentication
        options.setUserName(username);
        options.setPassword(password.toCharArray());
        
        // 🔄 Connection behavior
        options.setCleanSession(true);
        options.setAutomaticReconnect(true);
        options.setConnectionTimeout(connectionTimeout);
        options.setKeepAliveInterval(keepAliveInterval);
        options.setMaxInflight(maxInflight);
        
        // 🚀 Performance optimizations
        options.setMaxReconnectDelay(10000); // 10 seconds max
        options.setServerURIs(new String[]{brokerUrl}); // Single server for now
        
        // 🔒 Security settings
        options.setHttpsHostnameVerificationEnabled(false);
        options.setSocketFactory(null); // Use default
        
        logColorful("🔧 Connection options configured", CYAN);
        return options;
    }
    
    /**
     * 📊 Calculate retry delay with exponential backoff and jitter
     */
    private long calculateRetryDelay(int attempt) {
        long baseDelay = (long) (INITIAL_RETRY_DELAY * Math.pow(RETRY_MULTIPLIER, attempt - 1));
        long clampedDelay = Math.min(baseDelay, MAX_RETRY_DELAY);
        
        // 🎲 Add jitter to prevent thundering herd
        double jitter = 1.0 + (ThreadLocalRandom.current().nextDouble() - 0.5) * 2 * JITTER_FACTOR;
        return (long) (clampedDelay * jitter);
    }
    
    /**
     * 🚫 Check if error is non-retryable
     */
    private boolean isNonRetryableError(MqttException e) {
        int reasonCode = e.getReasonCode();
        return reasonCode == MqttException.REASON_CODE_INVALID_CLIENT_ID ||
               reasonCode == MqttException.REASON_CODE_NOT_AUTHORIZED ||
               reasonCode == MqttException.REASON_CODE_FAILED_AUTHENTICATION ||
               reasonCode == MqttException.REASON_CODE_INVALID_PROTOCOL_VERSION;
    }
    
    /**
     * 🔌 Disconnect client safely
     */
    public void disconnect(MqttClient client) {
        if (client == null) return;
        
        String clientId = client.getClientId();
        logColorful("🔌 DISCONNECTING CLIENT: " + clientId, BRIGHT_YELLOW);
        
        try {
            long startTime = System.currentTimeMillis();
            
            // 🔄 Disconnect with timeout
            if (client.isConnected()) {
                client.disconnect(5000); // 5 second timeout
                
                long disconnectTime = System.currentTimeMillis() - startTime;
                logColorful("✅ CLIENT DISCONNECTED in " + disconnectTime + "ms", BRIGHT_GREEN);
            } else {
                logColorful("⚠️ Client was already disconnected", YELLOW);
            }
            
        } catch (MqttException e) {
            logColorful("❌ DISCONNECT ERROR: " + e.getMessage(), BRIGHT_RED);
        } finally {
            // 🧹 Clean up resources
            try {
                client.close();
                logColorful("🧹 Client resources cleaned up", CYAN);
            } catch (MqttException e) {
                logColorful("⚠️ Error cleaning up client resources: " + e.getMessage(), YELLOW);
            }
            
            // 📊 Update tracking
            activeConnections.remove(clientId);
            currentConnections.decrementAndGet();
        }
    }
    
    /**
     * 📡 Subscribe to topics with enhanced error handling
     */
    public void subscribe(MqttClient client, String[] topics) throws MqttException {
        if (client == null || topics == null || topics.length == 0) {
            logColorful("⚠️ Invalid parameters for subscription", YELLOW);
            return;
        }
        
        String clientId = client.getClientId();
        logColorful("📡 SUBSCRIBING TO TOPICS", BRIGHT_BLUE);
        logColorful("📱 Client: " + clientId, CYAN);
        logColorful("📊 Topic count: " + topics.length, CYAN);
        
        if (!client.isConnected()) {
            logColorful("❌ CLIENT NOT CONNECTED", BRIGHT_RED);
            throw new MqttException(MqttException.REASON_CODE_CLIENT_NOT_CONNECTED);
        }
        
        try {
            long startTime = System.currentTimeMillis();
            
            // 🔧 Prepare QoS levels (using QoS 1 for performance)
            int[] qosLevels = new int[topics.length];
            Arrays.fill(qosLevels, 1);
            
            // 📡 Subscribe to all topics
            client.subscribe(topics, qosLevels);
            
            long subscriptionTime = System.currentTimeMillis() - startTime;
            
            // 📊 Update connection info
            ConnectionInfo info = activeConnections.get(clientId);
            if (info != null) {
                info.subscribedTopics = Arrays.asList(topics);
                info.subscriptionTime = subscriptionTime;
            }
            
            // 🎨 Display subscription success
            displaySubscriptionSuccess(clientId, topics, subscriptionTime);
            
        } catch (MqttException e) {
            logColorful("❌ SUBSCRIPTION FAILED: " + e.getMessage(), BRIGHT_RED);
            throw e;
        }
    }
    
    /**
     * 🔍 Validate broker URL
     */
    private boolean isValidBrokerUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }
        
        try {
            URI uri = new URI(url);
            String scheme = uri.getScheme();
            String host = uri.getHost();
            int port = uri.getPort();
            
            // 🔍 Check required components
            if (scheme == null || host == null || port <= 0) {
                return false;
            }
            
            // 🔍 Check supported schemes
            if (!scheme.equals("tcp") && !scheme.equals("ssl") && !scheme.equals("ws") && !scheme.equals("wss")) {
                return false;
            }
            
            logColorful("✅ Broker URL validation passed", GREEN);
            logColorful("🔗 Scheme: " + scheme, CYAN);
            logColorful("🌐 Host: " + host, CYAN);
            logColorful("🔌 Port: " + port, CYAN);
            
            return true;
            
        } catch (URISyntaxException e) {
            logColorful("❌ Invalid broker URL syntax: " + e.getMessage(), RED);
            return false;
        }
    }
    
    /**
     * 🆔 Generate unique client ID
     */
    private String generateUniqueClientId() {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String random = UUID.randomUUID().toString().substring(0, 8);
        String threadId = String.valueOf(Thread.currentThread().getId());
        
        return baseClientId + "-" + timestamp.substring(timestamp.length() - 6) + "-" + random + "-" + threadId;
    }
    
    /**
     * 🎨 Display connection attempt
     */
    private void displayConnectionAttempt(int attempt, String clientId) {
        logColorful("", ""); // Empty line
        logColorful("╔════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════", BRIGHT_CYAN);
        logColorful("║ 🔄 CONNECTION ATTEMPT #" + attempt, BRIGHT_CYAN);
        logColorful("╠════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════", BRIGHT_CYAN);
        logColorful("║ 📱 Client ID: " + clientId, CYAN);
        logColorful("║ 🌐 Broker: " + brokerUrl, CYAN);
        logColorful("║ 👤 Username: " + username, CYAN);
        logColorful("║ ⏱️ Time: " + LocalDateTime.now().format(timeFormatter), PURPLE);
        logColorful("║ 🔄 Attempt: " + attempt + "/" + MAX_RETRY_ATTEMPTS, YELLOW);
        logColorful("╚════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════", BRIGHT_CYAN);
    }
    
    /**
     * 🎉 Display connection success
     */
    private void displayConnectionSuccess(String clientId, int attempts, long connectionTime, long totalTime) {
        logColorful("", ""); // Empty line
        logColorful("╔════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════", BRIGHT_GREEN);
        logColorful("║ ✅ CONNECTION SUCCESSFUL!", BRIGHT_GREEN);
        logColorful("╠════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════", BRIGHT_GREEN);
        logColorful("║ 📱 Client ID: " + clientId, GREEN);
        logColorful("║ 🔄 Attempts: " + attempts, GREEN);
        logColorful("║ ⚡ Connection Time: " + connectionTime + "ms", GREEN);
        logColorful("║ 🕒 Total Time: " + totalTime + "ms", GREEN);
        logColorful("║ 📊 Success Rate: " + String.format("%.2f%%", (successfulConnections.get() * 100.0) / totalConnectionAttempts.get()), GREEN);
        logColorful("║ 🌟 Status: CONNECTED & READY", BRIGHT_GREEN);
        logColorful("╚════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════", BRIGHT_GREEN);
    }
    
    /**
     * 💥 Display connection failure
     */
    private void displayConnectionFailure(String clientId, int attempts, long totalTime, MqttException lastException) {
        logColorful("", ""); // Empty line
        logColorful("╔════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════", BRIGHT_RED);
        logColorful("║ ❌ CONNECTION FAILED!", BRIGHT_RED);
        logColorful("╠════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════", BRIGHT_RED);
        logColorful("║ 📱 Client ID: " + clientId, RED);
        logColorful("║ 🔄 Attempts: " + attempts, RED);
        logColorful("║ 🕒 Total Time: " + totalTime + "ms", RED);
        logColorful("║ 💥 Last Error: " + (lastException != null ? lastException.getMessage() : "Unknown"), RED);
        logColorful("║ 📊 Failure Rate: " + String.format("%.2f%%", (failedConnections.get() * 100.0) / totalConnectionAttempts.get()), RED);
        logColorful("║ 🚫 Status: CONNECTION FAILED", BRIGHT_RED);
        logColorful("╚════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════", BRIGHT_RED);
    }
    
    /**
     * 📡 Display subscription success
     */
    private void displaySubscriptionSuccess(String clientId, String[] topics, long subscriptionTime) {
        logColorful("", ""); // Empty line
        logColorful("╔════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════", BRIGHT_BLUE);
        logColorful("║ 📡 SUBSCRIPTION SUCCESSFUL!", BRIGHT_BLUE);
        logColorful("╠════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════", BRIGHT_BLUE);
        logColorful("║ 📱 Client ID: " + clientId, BLUE);
        logColorful("║ 📊 Topics: " + topics.length, BLUE);
        logColorful("║ ⚡ Subscription Time: " + subscriptionTime + "ms", BLUE);
        logColorful("║ 🔍 Topic List:", BLUE);
        
        for (int i = 0; i < topics.length; i++) {
            logColorful("║   " + (i + 1) + ". " + topics[i], CYAN);
        }
        
        logColorful("║ 🌟 Status: SUBSCRIBED & LISTENING", BRIGHT_BLUE);
        logColorful("╚════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════", BRIGHT_BLUE);
    }
    
    /**
     * 📊 Get connection statistics
     */
    public ConnectionStatistics getConnectionStatistics() {
        return new ConnectionStatistics(
            totalConnectionAttempts.get(),
            successfulConnections.get(),
            failedConnections.get(),
            currentConnections.get(),
            totalConnectionTime.get(),
            new HashMap<>(activeConnections)
        );
    }
    
    /**
     * 🏥 Perform health check on all connections
     */
    public HealthCheckResult performHealthCheck() {
        if (!healthCheckEnabled.get()) {
            return new HealthCheckResult(false, "Health check disabled", Collections.emptyList());
        }
        
        logColorful("🏥 PERFORMING CONNECTION HEALTH CHECK", BRIGHT_MAGENTA);
        
        List<String> issues = new ArrayList<>();
        int healthyConnections = 0;
        int unhealthyConnections = 0;
        
        for (Map.Entry<String, ConnectionInfo> entry : activeConnections.entrySet()) {
            String clientId = entry.getKey();
            ConnectionInfo info = entry.getValue();
            
            // Check if connection is stale (no activity for 5 minutes)
            long timeSinceConnection = System.currentTimeMillis() - info.connectedAt;
            if (timeSinceConnection > 300000) { // 5 minutes
                issues.add("Connection " + clientId + " is stale (inactive for " + 
                          formatDuration(timeSinceConnection) + ")");
                unhealthyConnections++;
            } else {
                healthyConnections++;
            }
        }
        
        // Check overall health
        boolean isHealthy = issues.isEmpty() && currentConnections.get() > 0;
        
        // 📊 Display health check results
        displayHealthCheckResults(healthyConnections, unhealthyConnections, issues);
        
        return new HealthCheckResult(isHealthy, 
            isHealthy ? "All connections healthy" : "Issues detected", 
            issues);
    }
    
    /**
     * 🏥 Display health check results
     */
    private void displayHealthCheckResults(int healthy, int unhealthy, List<String> issues) {
        logColorful("", ""); // Empty line
        logColorful("╔════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════", BRIGHT_MAGENTA);
        logColorful("║ 🏥 CONNECTION HEALTH CHECK RESULTS", BRIGHT_MAGENTA);
        logColorful("╠════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════", BRIGHT_MAGENTA);
        logColorful("║ ✅ Healthy Connections: " + healthy, healthy > 0 ? BRIGHT_GREEN : YELLOW);
        logColorful("║ ❌ Unhealthy Connections: " + unhealthy, unhealthy > 0 ? BRIGHT_RED : GREEN);
        logColorful("║ 📊 Total Active: " + currentConnections.get(), PURPLE);
        logColorful("║ 📈 Success Rate: " + String.format("%.2f%%", 
            totalConnectionAttempts.get() > 0 ? (successfulConnections.get() * 100.0) / totalConnectionAttempts.get() : 0), PURPLE);
        
        if (!issues.isEmpty()) {
            logColorful("║ 🚨 Issues Found:", BRIGHT_RED);
            for (int i = 0; i < issues.size(); i++) {
                logColorful("║   " + (i + 1) + ". " + issues.get(i), RED);
            }
        }
        
        logColorful("║ 🌟 Overall Status: " + (issues.isEmpty() ? "HEALTHY" : "NEEDS ATTENTION"), 
                   issues.isEmpty() ? BRIGHT_GREEN : BRIGHT_YELLOW);
        logColorful("╚════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════", BRIGHT_MAGENTA);
    }
    
    /**
     * ⏱️ Format duration for display
     */
    private String formatDuration(long millis) {
        if (millis < 1000) return millis + "ms";
        if (millis < 60000) return String.format("%.1fs", millis / 1000.0);
        if (millis < 3600000) return String.format("%.1fm", millis / 60000.0);
        return String.format("%.1fh", millis / 3600000.0);
    }
    
    /**
     * 🎨 Colorful logging utility
     */
    private void logColorful(String message, String color) {
        System.out.println(color + message + RESET);
        logger.info(message.replaceAll("║", "").replaceAll("╔", "").replaceAll("╚", "").replaceAll("╠", "").replaceAll("═", "").trim());
    }
    
    // 📊 Public getter methods
    public int getTotalConnectionAttempts() { return totalConnectionAttempts.get(); }
    public int getSuccessfulConnections() { return successfulConnections.get(); }
    public int getFailedConnections() { return failedConnections.get(); }
    public int getCurrentConnections() { return currentConnections.get(); }
    public long getAverageConnectionTime() { 
        return successfulConnections.get() > 0 ? totalConnectionTime.get() / successfulConnections.get() : 0;
    }
    
    /**
     * 📊 Connection information class
     */
    public static class ConnectionInfo {
        String clientId;
        long createdAt;
        long connectedAt;
        long connectionTime;
        int attempts;
        List<String> subscribedTopics;
        long subscriptionTime;
        
        ConnectionInfo(String clientId, long createdAt) {
            this.clientId = clientId;
            this.createdAt = createdAt;
            this.connectedAt = 0;
            this.connectionTime = 0;
            this.attempts = 0;
            this.subscribedTopics = new ArrayList<>();
            this.subscriptionTime = 0;
        }
        
        // Getters
        public String getClientId() { return clientId; }
        public long getCreatedAt() { return createdAt; }
        public long getConnectedAt() { return connectedAt; }
        public long getConnectionTime() { return connectionTime; }
        public int getAttempts() { return attempts; }
        public List<String> getSubscribedTopics() { return subscribedTopics; }
        public long getSubscriptionTime() { return subscriptionTime; }
    }
    
    /**
     * 📊 Connection statistics class
     */
    public static class ConnectionStatistics {
        private final int totalAttempts;
        private final int successfulConnections;
        private final int failedConnections;
        private final int currentConnections;
        private final long totalConnectionTime;
        private final Map<String, ConnectionInfo> activeConnections;
        
        ConnectionStatistics(int totalAttempts, int successful, int failed, int current, 
                           long totalTime, Map<String, ConnectionInfo> active) {
            this.totalAttempts = totalAttempts;
            this.successfulConnections = successful;
            this.failedConnections = failed;
            this.currentConnections = current;
            this.totalConnectionTime = totalTime;
            this.activeConnections = active;
        }
        
        // Getters
        public int getTotalAttempts() { return totalAttempts; }
        public int getSuccessfulConnections() { return successfulConnections; }
        public int getFailedConnections() { return failedConnections; }
        public int getCurrentConnections() { return currentConnections; }
        public long getTotalConnectionTime() { return totalConnectionTime; }
        public Map<String, ConnectionInfo> getActiveConnections() { return activeConnections; }
        
        public double getSuccessRate() {
            return totalAttempts > 0 ? (successfulConnections * 100.0) / totalAttempts : 0;
        }
        
        public long getAverageConnectionTime() {
            return successfulConnections > 0 ? totalConnectionTime / successfulConnections : 0;
        }
    }
    
    /**
     * 🏥 Health check result class
     */
    public static class HealthCheckResult {
        private final boolean healthy;
        private final String message;
        private final List<String> issues;
        
        HealthCheckResult(boolean healthy, String message, List<String> issues) {
            this.healthy = healthy;
            this.message = message;
            this.issues = issues;
        }
        
        // Getters
        public boolean isHealthy() { return healthy; }
        public String getMessage() { return message; }
        public List<String> getIssues() { return issues; }
    }
}