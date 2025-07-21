package com.GpsTracker.Thinture.service.websocket;

import com.GpsTracker.Thinture.dto.LocationUpdate;
import com.GpsTracker.Thinture.model.GpsData;
import com.GpsTracker.Thinture.model.Vehicle;
import com.GpsTracker.Thinture.model.VehicleLastLocation;
import com.GpsTracker.Thinture.repository.VehicleLastLocationRepository;
import com.GpsTracker.Thinture.service.VehicleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 🌟 ENHANCED Location Broadcast Service - Production Ready for 5000+ Devices
 * ✅ Colorful, structured logging with emojis
 * ✅ Role-based broadcasting with detailed tracking
 * ✅ Performance monitoring and metrics
 * ✅ Advanced alert system with configurable thresholds
 * ✅ Real-time broadcasting dashboard
 * ✅ Device-specific broadcasting tracking
 * ✅ WebSocket session management integration
 * ✅ Rate limiting and performance optimization
 * 
 * Shows complete ID fetching and role-based broadcasting process
 * Handles real-time location broadcasting for GPS tracking system
 */
@Service
public class LocationBroadcastService {
    
    private static final Logger logger = LoggerFactory.getLogger(LocationBroadcastService.class);
    private static final Logger performanceLogger = LoggerFactory.getLogger("PERFORMANCE");
    private static final Logger broadcastLogger = LoggerFactory.getLogger("BROADCAST");
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
   // private static final String BRIGHT_MAGENTA = "\u001B[95m";
    // 🔗 Enhanced dependencies
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    @Autowired
    private WebSocketSessionManager sessionManager;
    
    @Autowired
    private VehicleLastLocationRepository vehicleLastLocationRepository;
    
    @Autowired
    private VehicleService vehicleService;
    
    // 📊 Configuration
    @Value("${websocket.broadcast.rate-limit-ms:100}")
    private long rateLimitMs;
    
    @Value("${websocket.broadcast.alert-speed-threshold:120.0}")
    private double alertSpeedThreshold;
    
    @Value("${websocket.broadcast.operating-hours-start:6}")
    private int operatingHoursStart;
    
    @Value("${websocket.broadcast.operating-hours-end:22}")
    private int operatingHoursEnd;
    
    @Value("${websocket.broadcast.batch-delay-ms:10}")
    private long batchDelayMs;
    
    // 📡 WebSocket topics
    private static final String TOPIC_ALL_LOCATIONS = "/topic/location-updates";
    private static final String TOPIC_DEVICE_PREFIX = "/topic/device/";
    private static final String TOPIC_ALERTS = "/topic/alerts";
    private static final String TOPIC_STATS = "/topic/stats";
    
    // 🎯 Role-based topics
    private static final String TOPIC_DEALER_PREFIX = "/topic/location-updates/dealer/";
    private static final String TOPIC_ADMIN_PREFIX = "/topic/location-updates/admin/";
    private static final String TOPIC_CLIENT_PREFIX = "/topic/location-updates/client/";
    private static final String TOPIC_USER_PREFIX = "/topic/location-updates/user/";
    private static final String TOPIC_SUPERADMIN_PREFIX = "/topic/location-updates/superadmin/";
    
    // 📊 Performance metrics
    private final AtomicLong totalBroadcasts = new AtomicLong(0);
    private final AtomicLong successfulBroadcasts = new AtomicLong(0);
    private final AtomicLong failedBroadcasts = new AtomicLong(0);
    private final AtomicLong roleBroadcasts = new AtomicLong(0);
    private final AtomicLong alertsBroadcast = new AtomicLong(0);
    private final AtomicLong rateLimitedBroadcasts = new AtomicLong(0);
    private final AtomicLong vehicleLookupsSuccessful = new AtomicLong(0);
    private final AtomicLong vehicleLookupsFound = new AtomicLong(0);
    
    // 📊 Device and role tracking
    private final Map<String, Long> lastBroadcastTime = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> deviceBroadcastCounts = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> roleBroadcastCounts = new ConcurrentHashMap<>();
    private final Map<String, Long> broadcastTimes = new ConcurrentHashMap<>();
    
    // 🕒 Time formatting
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    
    @PostConstruct
    public void initialize() {
        logColorful("🌟 INITIALIZING ENHANCED LOCATION BROADCAST SERVICE", BRIGHT_CYAN);
        logColorful("⏱️ Startup time: " + LocalDateTime.now().format(timeFormatter), CYAN);
        
        // 📊 Display configuration
        displayConfiguration();
        
        // 🔧 Initialize role broadcast counters
        initializeRoleBroadcastCounters();
        
        logColorful("✅ Enhanced Location Broadcast Service initialized successfully", BRIGHT_GREEN);
        displayWelcomeDashboard();
    }
    
    /**
     * 📊 Display service configuration
     */
    private void displayConfiguration() {
        logColorful("", "");
        logColorful("╔════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════", BRIGHT_BLUE);
        logColorful("║ ⚙️ LOCATION BROADCAST SERVICE CONFIGURATION", BRIGHT_BLUE);
        logColorful("╠════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════", BRIGHT_BLUE);
        logColorful("║ 📡 Rate Limit: " + rateLimitMs + "ms", BLUE);
        logColorful("║ 🚨 Speed Alert Threshold: " + alertSpeedThreshold + " km/h", BLUE);
        logColorful("║ 🕒 Operating Hours: " + operatingHoursStart + ":00 - " + operatingHoursEnd + ":00", BLUE);
        logColorful("║ 📦 Batch Delay: " + batchDelayMs + "ms", BLUE);
        logColorful("║ 🎯 Target Capacity: 5000+ devices", BLUE);
        logColorful("║ 🏗️ Architecture: Enhanced Role-Based Broadcasting", BLUE);
        logColorful("║ 📊 Topics: " + getRoleTopicCount() + " role-based + " + getSystemTopicCount() + " system", BLUE);
        logColorful("╚════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════", BRIGHT_BLUE);
    }
    
    /**
     * 🔧 Initialize role broadcast counters
     */
    private void initializeRoleBroadcastCounters() {
        roleBroadcastCounts.put("DEALER", new AtomicLong(0));
        roleBroadcastCounts.put("ADMIN", new AtomicLong(0));
        roleBroadcastCounts.put("CLIENT", new AtomicLong(0));
        roleBroadcastCounts.put("USER", new AtomicLong(0));
        roleBroadcastCounts.put("SUPERADMIN", new AtomicLong(0));
        roleBroadcastCounts.put("GENERAL", new AtomicLong(0));
        roleBroadcastCounts.put("DEVICE", new AtomicLong(0));
        roleBroadcastCounts.put("ALERTS", new AtomicLong(0));
        
        logColorful("🔧 Role broadcast counters initialized: " + roleBroadcastCounts.size() + " roles", CYAN);
    }
    
    /**
     * 🎨 Display welcome dashboard
     */
    private void displayWelcomeDashboard() {
        logColorful("", "");
        logColorful("╔════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════", BRIGHT_MAGENTA);
        logColorful("║ 🎯 ENHANCED LOCATION BROADCAST DASHBOARD", BRIGHT_MAGENTA);
        logColorful("║ 🚀 Ready to broadcast GPS data for 5000+ devices", BRIGHT_MAGENTA);
        logColorful("╠════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════", BRIGHT_MAGENTA);
        logColorful("║ 📡 Role-Based Broadcasting: ACTIVE", BRIGHT_GREEN);
        logColorful("║ 🎯 Device-Specific Broadcasting: ACTIVE", BRIGHT_GREEN);
        logColorful("║ 🚨 Alert Broadcasting: ACTIVE", BRIGHT_GREEN);
        logColorful("║ 📊 Statistics Broadcasting: ACTIVE", BRIGHT_GREEN);
        logColorful("║ 🔒 Security Context Management: ACTIVE", BRIGHT_GREEN);
        logColorful("║ ⚡ Rate Limiting: ACTIVE", BRIGHT_GREEN);
        logColorful("║ 📈 Performance Monitoring: ACTIVE", BRIGHT_GREEN);
        logColorful("╚════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════", BRIGHT_MAGENTA);
    }
    
    /**
     * 🚀 ENHANCED: Main broadcast method for GpsData with colorful logging
     */
    @Async("websocketExecutor")
    public void broadcastLocationUpdate(GpsData gpsData) {
        long startTime = System.currentTimeMillis();
        String deviceId = gpsData != null ? gpsData.getDeviceID() : "unknown";
        
        try {
            setWebSocketAuthenticationContext();
            totalBroadcasts.incrementAndGet();
            
            logColorful("🚀 STARTING LOCATION BROADCAST", BRIGHT_CYAN);
            logColorful("📱 Device ID: " + deviceId, CYAN);
            logColorful("⏱️ Timestamp: " + LocalDateTime.now().format(timeFormatter), CYAN);
            
            // 🔍 Input validation
            if (gpsData == null) {
                logColorful("🚫 GPS DATA IS NULL", BRIGHT_RED);
                failedBroadcasts.incrementAndGet();
                return;
            }
            
            // ⏰ Rate limiting check
            if (!shouldBroadcast(deviceId)) {
                rateLimitedBroadcasts.incrementAndGet();
                logColorful("⏰ RATE LIMIT EXCEEDED for device: " + deviceId, BRIGHT_YELLOW);
                logColorful("⏱️ Last broadcast: " + getTimeSinceLastBroadcast(deviceId) + "ms ago", YELLOW);
                return;
            }
            
            logColorful("✅ RATE LIMIT CHECK PASSED", GREEN);
            
            // 🔄 Create location update
            logColorful("🔄 CREATING LOCATION UPDATE", BLUE);
            LocationUpdate update = createLocationUpdate(gpsData);
            logColorful("✅ Location update created", GREEN);
            
            // 🔍 Vehicle lookup with detailed logging
            logColorful("🔍 VEHICLE LOOKUP STARTING", BLUE);
            vehicleLookupsSuccessful.incrementAndGet();
            Optional<Vehicle> vehicleOpt = vehicleService.getVehicleByDeviceID(deviceId);
            
            if (vehicleOpt.isPresent()) {
                vehicleLookupsFound.incrementAndGet();
                Vehicle vehicle = vehicleOpt.get();
                
                logColorful("✅ VEHICLE FOUND", BRIGHT_GREEN);
                logColorful("🚗 Vehicle Number: " + vehicle.getVehicleNumber(), GREEN);
                logColorful("🏷️ Vehicle ID: " + vehicle.getId(), GREEN);
                logColorful("📋 Vehicle Type: " + vehicle.getVehicleType(), GREEN);
                
                // 🔍 Role ID extraction and logging
                logVehicleRoleIds(vehicle, deviceId);
                
                // 📡 Role-based broadcasting
                broadcastToRoleBasedTopics(vehicle, update);
                
            } else {
                logColorful("❌ VEHICLE NOT FOUND for device: " + deviceId, BRIGHT_RED);
                logColorful("💡 Role-based broadcasting will be skipped", YELLOW);
            }
            
            // 📡 Broadcast to generic topics
            logColorful("📡 BROADCASTING TO GENERIC TOPICS", BLUE);
            broadcastToAllLocations(update);
            broadcastToDeviceTopic(update);
            
            // 🚨 Check and broadcast alerts
            checkAndBroadcastAlerts(gpsData);
            
            // 📊 Update metrics and tracking
            updateBroadcastMetrics(deviceId, startTime, true);
            
            long duration = System.currentTimeMillis() - startTime;
            logColorful("✅ LOCATION BROADCAST COMPLETED", BRIGHT_GREEN);
            logColorful("📱 Device: " + deviceId, GREEN);
            logColorful("⏱️ Total time: " + duration + "ms", GREEN);
            
            broadcastLogger.info("Location broadcast completed for device: {} in {}ms", deviceId, duration);
            
        } catch (Exception e) {
            failedBroadcasts.incrementAndGet();
            updateBroadcastMetrics(deviceId, startTime, false);
            
            logColorful("❌ LOCATION BROADCAST FAILED", BRIGHT_RED);
            logColorful("📱 Device: " + deviceId, RED);
            logColorful("💥 Error: " + e.getMessage(), RED);
            
            logger.error("Error broadcasting location update for device: {}", deviceId, e);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
    
    /**
     * 🚀 ENHANCED: Broadcast method for LocationUpdate with colorful logging
     */
    @Async("websocketExecutor")
    public void broadcastLocationUpdate(LocationUpdate update) {
        long startTime = System.currentTimeMillis();
        String deviceId = update != null ? update.getDeviceID() : "unknown";
        
        try {
            setWebSocketAuthenticationContext();
            totalBroadcasts.incrementAndGet();
            
            logColorful("🚀 STARTING LOCATION UPDATE BROADCAST", BRIGHT_CYAN);
            logColorful("📱 Device ID: " + deviceId, CYAN);
            
            // 🔍 Input validation
            if (update == null) {
                logColorful("🚫 LOCATION UPDATE IS NULL", BRIGHT_RED);
                failedBroadcasts.incrementAndGet();
                return;
            }
            
            // 🔍 Vehicle lookup for role-based broadcasting
            logColorful("🔍 VEHICLE LOOKUP FOR ROLE BROADCASTING", BLUE);
            Optional<Vehicle> vehicleOpt = vehicleService.getVehicleByDeviceID(deviceId);
            
            if (vehicleOpt.isPresent()) {
                Vehicle vehicle = vehicleOpt.get();
                logColorful("✅ VEHICLE FOUND for role broadcasting", BRIGHT_GREEN);
                logVehicleRoleIds(vehicle, deviceId);
                broadcastToRoleBasedTopics(vehicle, update);
            } else {
                logColorful("❌ VEHICLE NOT FOUND for role broadcasting", BRIGHT_RED);
            }
            
            // 📡 Broadcast to generic topics
            broadcastToAllLocations(update);
            broadcastToDeviceTopic(update);
            
            // 📊 Update metrics
            updateBroadcastMetrics(deviceId, startTime, true);
            
            long duration = System.currentTimeMillis() - startTime;
            logColorful("✅ LOCATION UPDATE BROADCAST COMPLETED", BRIGHT_GREEN);
            logColorful("⏱️ Duration: " + duration + "ms", GREEN);
            
        } catch (Exception e) {
            failedBroadcasts.incrementAndGet();
            updateBroadcastMetrics(deviceId, startTime, false);
            
            logColorful("❌ LOCATION UPDATE BROADCAST FAILED", BRIGHT_RED);
            logColorful("💥 Error: " + e.getMessage(), RED);
            logger.error("Error broadcasting location update", e);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
    
    /**
     * 🔍 ENHANCED: Detailed logging of vehicle role IDs
     */
    private void logVehicleRoleIds(Vehicle vehicle, String deviceId) {
        logColorful("🔍 ROLE ID EXTRACTION STARTING", BRIGHT_BLUE);
        logColorful("📱 Device: " + deviceId, BLUE);
        
        // 📊 Extract all role IDs
        Long dealerId = vehicle.getDealer_id();
        Long adminId = vehicle.getAdmin_id();
        Long clientId = vehicle.getClient_id();
        Long userId = vehicle.getUser_id();
        Long superAdminId = vehicle.getSuperadmin_id();
        
        // 📊 Log each ID with validation status
        logColorful("📊 ROLE ID ANALYSIS:", BRIGHT_BLUE);
        logRoleIdStatus("DEALER", dealerId);
        logRoleIdStatus("ADMIN", adminId);
        logRoleIdStatus("CLIENT", clientId);
        logRoleIdStatus("USER", userId);
        logRoleIdStatus("SUPERADMIN", superAdminId);
        
        // 📡 Log topic preview
        logColorful("📡 TOPICS TO BE CREATED:", BRIGHT_BLUE);
        if (isValidId(dealerId)) {
            logColorful("   🎯 Dealer Topic: " + TOPIC_DEALER_PREFIX + dealerId, CYAN);
        }
        if (isValidId(adminId)) {
            logColorful("   🎯 Admin Topic: " + TOPIC_ADMIN_PREFIX + adminId, CYAN);
        }
        if (isValidId(clientId)) {
            logColorful("   🎯 Client Topic: " + TOPIC_CLIENT_PREFIX + clientId, CYAN);
        }
        if (isValidId(userId)) {
            logColorful("   🎯 User Topic: " + TOPIC_USER_PREFIX + userId, CYAN);
        }
        if (isValidId(superAdminId)) {
            logColorful("   🎯 SuperAdmin Topic: " + TOPIC_SUPERADMIN_PREFIX + superAdminId, CYAN);
        }
        
        // 📊 Summary
        int validRoleCount = countValidRoles(vehicle);
        logColorful("📊 SUMMARY: Device " + deviceId + " has " + validRoleCount + " valid role IDs", BRIGHT_GREEN);
        logColorful("📡 Will attempt " + validRoleCount + " role-based broadcasts", BRIGHT_GREEN);
    }
    
    /**
     * 📊 Log individual role ID status
     */
    private void logRoleIdStatus(String roleType, Long roleId) {
        boolean isValid = isValidId(roleId);
        String status = isValid ? "VALID" : "INVALID";
        String broadcast = isValid ? "YES" : "NO";
        String color = isValid ? BRIGHT_GREEN : BRIGHT_RED;
        
        logColorful("   📊 " + roleType + " ID: " + roleId + " | Status: " + status + " | Broadcast: " + broadcast, color);
    }
    
    /**
     * 📊 Count valid roles
     */
    private int countValidRoles(Vehicle vehicle) {
        int count = 0;
        if (isValidId(vehicle.getDealer_id())) count++;
        if (isValidId(vehicle.getAdmin_id())) count++;
        if (isValidId(vehicle.getClient_id())) count++;
        if (isValidId(vehicle.getUser_id())) count++;
        if (isValidId(vehicle.getSuperadmin_id())) count++;
        return count;
    }
    
    /**
     * 🔒 Set WebSocket authentication context
     */
    private void setWebSocketAuthenticationContext() {
        try {
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "websocket-service",
                null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_SYSTEM"))
            );
            
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(auth);
            SecurityContextHolder.setContext(context);
            
            logColorful("🔐 WebSocket authentication context set", GREEN);
            
        } catch (Exception e) {
            logColorful("❌ Failed to set WebSocket authentication context", BRIGHT_RED);
            logger.error("Failed to set WebSocket authentication context", e);
        }
    }
    
    /**
     * 📡 ENHANCED: Role-based broadcasting with detailed logging
     */
    private void broadcastToRoleBasedTopics(Vehicle vehicle, LocationUpdate update) {
        if (vehicle == null || update == null) {
            logColorful("❌ INVALID INPUT for role-based broadcasting", BRIGHT_RED);
            return;
        }
        
        String deviceId = update.getDeviceID();
        logColorful("🚀 ROLE-BASED BROADCASTING STARTING", BRIGHT_BLUE);
        logColorful("📱 Device: " + deviceId, BLUE);
        
        try {
            // 📡 Dealer broadcasting
            if (isValidId(vehicle.getDealer_id())) {
                String dealerId = String.valueOf(vehicle.getDealer_id()).trim();
                String topic = TOPIC_DEALER_PREFIX + dealerId;
                logColorful("📡 DEALER BROADCAST", BRIGHT_GREEN);
                broadcastToSpecificRole("DEALER", dealerId, topic, update, deviceId);
            } else {
                logColorful("⏭️ DEALER BROADCAST SKIPPED - Invalid ID", YELLOW);
            }

            // 📡 Admin broadcasting
            if (isValidId(vehicle.getAdmin_id())) {
                String adminId = String.valueOf(vehicle.getAdmin_id()).trim();
                String topic = TOPIC_ADMIN_PREFIX + adminId;
                logColorful("📡 ADMIN BROADCAST", BRIGHT_GREEN);
                broadcastToSpecificRole("ADMIN", adminId, topic, update, deviceId);
            } else {
                logColorful("⏭️ ADMIN BROADCAST SKIPPED - Invalid ID", YELLOW);
            }

            // 📡 Client broadcasting
            if (isValidId(vehicle.getClient_id())) {
                String clientId = String.valueOf(vehicle.getClient_id()).trim();
                String topic = TOPIC_CLIENT_PREFIX + clientId;
                logColorful("📡 CLIENT BROADCAST", BRIGHT_GREEN);
                broadcastToSpecificRole("CLIENT", clientId, topic, update, deviceId);
            } else {
                logColorful("⏭️ CLIENT BROADCAST SKIPPED - Invalid ID", YELLOW);
            }

            // 📡 User broadcasting
            if (isValidId(vehicle.getUser_id())) {
                String userId = String.valueOf(vehicle.getUser_id()).trim();
                String topic = TOPIC_USER_PREFIX + userId;
                logColorful("📡 USER BROADCAST", BRIGHT_GREEN);
                broadcastToSpecificRole("USER", userId, topic, update, deviceId);
            } else {
                logColorful("⏭️ USER BROADCAST SKIPPED - Invalid ID", YELLOW);
            }

            // 📡 SuperAdmin broadcasting
            if (isValidId(vehicle.getSuperadmin_id())) {
                String superAdminId = String.valueOf(vehicle.getSuperadmin_id()).trim();
                String topic = TOPIC_SUPERADMIN_PREFIX + superAdminId;
                logColorful("📡 SUPERADMIN BROADCAST", BRIGHT_GREEN);
                broadcastToSpecificRole("SUPERADMIN", superAdminId, topic, update, deviceId);
            } else {
                logColorful("⏭️ SUPERADMIN BROADCAST SKIPPED - Invalid ID", YELLOW);
            }

            logColorful("✅ ROLE-BASED BROADCASTING COMPLETED", BRIGHT_GREEN);

        } catch (Exception e) {
            logColorful("❌ ROLE-BASED BROADCASTING ERROR", BRIGHT_RED);
            logColorful("💥 Error: " + e.getMessage(), RED);
            logger.error("Error in role-based broadcasting for device: {}", deviceId, e);
        }
    }
    
    /**
     * 📡 ENHANCED: Broadcast to specific role with detailed logging
     */
    private void broadcastToSpecificRole(String roleType, String roleId, String topic, 
                                       LocationUpdate update, String deviceId) {
        try {
            logColorful("🔍 ROLE BROADCAST CHECK: " + roleType, BLUE);
            logColorful("🎯 Role ID: " + roleId, CYAN);
            logColorful("📡 Topic: " + topic, CYAN);
            
            int subscriberCount = sessionManager.getSubscriberCount(topic);
            logColorful("👥 Subscribers: " + subscriberCount, CYAN);
            
            if (subscriberCount > 0) {
                logColorful("📤 BROADCASTING to " + subscriberCount + " subscribers", BRIGHT_GREEN);
                messagingTemplate.convertAndSend(topic, update);
                
                // 📊 Update metrics
                roleBroadcasts.incrementAndGet();
                roleBroadcastCounts.get(roleType).incrementAndGet();
                
                logColorful("✅ ROLE BROADCAST SUCCESS", BRIGHT_GREEN);
                logColorful("📊 " + roleType + " (ID=" + roleId + ") | Device=" + deviceId + " | Subscribers=" + subscriberCount, GREEN);
                
                broadcastLogger.info("Role broadcast successful - {} (ID={}) for device {} to {} subscribers", 
                    roleType, roleId, deviceId, subscriberCount);
                
            } else {
                logColorful("⚠️ NO SUBSCRIBERS for " + roleType + " topic", BRIGHT_YELLOW);
                logColorful("💡 To receive broadcasts, subscribe to: " + topic, YELLOW);
            }
            
        } catch (Exception e) {
            logColorful("❌ ROLE BROADCAST ERROR", BRIGHT_RED);
            logColorful("💥 Role: " + roleType + ", Error: " + e.getMessage(), RED);
            logger.error("Error broadcasting to {} role (ID: {}) for device: {}", roleType, roleId, deviceId, e);
        }
    }
    
    /**
     * 📡 ENHANCED: Broadcast to all locations topic
     */
    private void broadcastToAllLocations(LocationUpdate update) {
        try {
            int subscriberCount = sessionManager.getSubscriberCount(TOPIC_ALL_LOCATIONS);
            logColorful("📡 GENERAL BROADCAST CHECK", BLUE);
            logColorful("👥 General subscribers: " + subscriberCount, CYAN);
            
            if (subscriberCount > 0) {
                messagingTemplate.convertAndSend(TOPIC_ALL_LOCATIONS, update);
                roleBroadcastCounts.get("GENERAL").incrementAndGet();
                
                logColorful("✅ GENERAL BROADCAST SUCCESS", BRIGHT_GREEN);
                logColorful("📊 Sent to " + subscriberCount + " general subscribers", GREEN);
            } else {
                logColorful("⚠️ NO GENERAL SUBSCRIBERS", YELLOW);
            }
        } catch (Exception e) {
            logColorful("❌ GENERAL BROADCAST ERROR", BRIGHT_RED);
            logColorful("💥 Error: " + e.getMessage(), RED);
            logger.error("Error broadcasting to general locations topic", e);
        }
    }
    
    /**
     * 📡 ENHANCED: Broadcast to device-specific topic
     */
    private void broadcastToDeviceTopic(LocationUpdate update) {
        try {
            String deviceTopic = TOPIC_DEVICE_PREFIX + update.getDeviceID();
            int subscriberCount = sessionManager.getSubscriberCount(deviceTopic);
            
            logColorful("📡 DEVICE BROADCAST CHECK", BLUE);
            logColorful("📱 Device topic: " + deviceTopic, CYAN);
            logColorful("👥 Device subscribers: " + subscriberCount, CYAN);
            
            if (subscriberCount > 0) {
                messagingTemplate.convertAndSend(deviceTopic, update);
                roleBroadcastCounts.get("DEVICE").incrementAndGet();
                
                logColorful("✅ DEVICE BROADCAST SUCCESS", BRIGHT_GREEN);
                logColorful("📊 Sent to " + subscriberCount + " device subscribers", GREEN);
            } else {
                logColorful("⚠️ NO DEVICE SUBSCRIBERS", YELLOW);
            }
        } catch (Exception e) {
            logColorful("❌ DEVICE BROADCAST ERROR", BRIGHT_RED);
            logColorful("💥 Error: " + e.getMessage(), RED);
            logger.error("Error broadcasting to device topic", e);
        }
    }
    
    /**
     * 🚨 ENHANCED: Check and broadcast alerts
     */
    private void checkAndBroadcastAlerts(GpsData gpsData) {
        try {
            logColorful("🚨 ALERT CHECK STARTING", BLUE);
            List<Map<String, Object>> alerts = new ArrayList<>();
            
            // 🏎️ Speed alert
            if (gpsData.getSpeed() != null && !gpsData.getSpeed().isEmpty()) {
                try {
                    double speed = Double.parseDouble(gpsData.getSpeed());
                    if (speed > alertSpeedThreshold) {
                        alerts.add(createAlert("SPEED_ALERT", gpsData.getDeviceID(), 
                            "Speed limit exceeded: " + speed + " km/h (threshold: " + alertSpeedThreshold + ")"));
                        logColorful("🚨 SPEED ALERT: " + speed + " km/h > " + alertSpeedThreshold + " km/h", BRIGHT_RED);
                    }
                } catch (NumberFormatException e) {
                    logColorful("⚠️ Invalid speed format: " + gpsData.getSpeed(), YELLOW);
                }
            }
            
            // 🔥 Ignition alert (outside operating hours)
            if ("ON".equalsIgnoreCase(gpsData.getIgnition()) && isOutsideOperatingHours()) {
                alerts.add(createAlert("IGNITION_ALERT", gpsData.getDeviceID(), 
                    "Vehicle started outside operating hours (" + operatingHoursStart + ":00-" + operatingHoursEnd + ":00)"));
                logColorful("🚨 IGNITION ALERT: Vehicle started outside operating hours", BRIGHT_RED);
            }
            
            // 📍 Location alert (suspicious coordinates)
            try {
                double lat = Double.parseDouble(gpsData.getLatitude());
                double lon = Double.parseDouble(gpsData.getLongitude());
                if (lat == 0.0 && lon == 0.0) {
                    alerts.add(createAlert("LOCATION_ALERT", gpsData.getDeviceID(), 
                        "Suspicious coordinates detected: (0,0)"));
                    logColorful("🚨 LOCATION ALERT: Suspicious coordinates (0,0)", BRIGHT_YELLOW);
                }
            } catch (NumberFormatException e) {
                logColorful("⚠️ Invalid coordinate format", YELLOW);
            }
            
            // 📡 Broadcast alerts
            if (!alerts.isEmpty()) {
                int alertSubscribers = sessionManager.getSubscriberCount(TOPIC_ALERTS);
                
                if (alertSubscribers > 0) {
                    for (Map<String, Object> alert : alerts) {
                        messagingTemplate.convertAndSend(TOPIC_ALERTS, alert);
                        alertsBroadcast.incrementAndGet();
                        roleBroadcastCounts.get("ALERTS").incrementAndGet();
                        
                        logColorful("🚨 ALERT BROADCASTED: " + alert.get("type"), BRIGHT_RED);
                        logColorful("📱 Device: " + alert.get("deviceId"), RED);
                        logColorful("📝 Message: " + alert.get("message"), RED);
                        logColorful("👥 Sent to: " + alertSubscribers + " subscribers", RED);
                    }
                } else {
                    logColorful("⚠️ ALERTS GENERATED but no subscribers", BRIGHT_YELLOW);
                    alerts.forEach(alert -> 
                        logColorful("🚨 Undelivered alert: " + alert.get("type") + " - " + alert.get("message"), YELLOW));
                }
            } else {
                logColorful("✅ NO ALERTS GENERATED", GREEN);
            }
            
        } catch (Exception e) {
            logColorful("❌ ALERT CHECK ERROR", BRIGHT_RED);
            logColorful("💥 Error: " + e.getMessage(), RED);
            logger.error("Error checking alerts for device: {}", gpsData.getDeviceID(), e);
        }
    }
    
    /**
     * 📊 ENHANCED: Broadcast all locations with progress tracking
     */
    public void broadcastAllLocations() {
        setWebSocketAuthenticationContext();
        
        try {
            logColorful("🚀 BULK BROADCAST STARTING", BRIGHT_CYAN);
            
            List<VehicleLastLocation> locations = vehicleLastLocationRepository.findAll();
            int totalLocations = locations.size();
            int processed = 0;
            
            logColorful("📊 Found " + totalLocations + " vehicle locations to broadcast", BRIGHT_BLUE);
            
            if (totalLocations == 0) {
                logColorful("⚠️ NO LOCATIONS TO BROADCAST", BRIGHT_YELLOW);
                return;
            }
            
            for (VehicleLastLocation location : locations) {
                try {
                    LocationUpdate update = createLocationUpdate(location);
                    
                    // 📡 Role-based broadcasting
                    Optional<Vehicle> vehicleOpt = vehicleService.getVehicleByDeviceID(location.getDeviceId());
                    if (vehicleOpt.isPresent()) {
                        broadcastToRoleBasedTopics(vehicleOpt.get(), update);
                    }
                    
                    // 📡 General broadcasting
                    messagingTemplate.convertAndSend(TOPIC_ALL_LOCATIONS, update);
                    processed++;
                    
                    // 📊 Progress logging
                    if (totalLocations > 100 && processed % 50 == 0) {
                        int progress = (processed * 100) / totalLocations;
                        logColorful("📊 BULK PROGRESS: " + progress + "% (" + processed + "/" + totalLocations + ")", BRIGHT_CYAN);
                    }
                    
                    // 😴 Prevent overwhelming clients
                    if (processed % 100 == 0) {
                        Thread.sleep(batchDelayMs);
                    }
                    
                } catch (Exception e) {
                    logColorful("❌ Failed to broadcast location for device: " + location.getDeviceId(), RED);
                    logger.error("Failed to broadcast location for device: {}", location.getDeviceId(), e);
                }
            }
            
            logColorful("✅ BULK BROADCAST COMPLETED", BRIGHT_GREEN);
            logColorful("📊 Successfully broadcast " + processed + "/" + totalLocations + " locations", GREEN);
            
        } catch (Exception e) {
            logColorful("❌ BULK BROADCAST ERROR", BRIGHT_RED);
            logColorful("💥 Error: " + e.getMessage(), RED);
            logger.error("Error broadcasting all locations", e);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
    
    /**
     * 📡 ENHANCED: Broadcast device location with detailed logging
     */
    public void broadcastDeviceLocation(String deviceId) {
        setWebSocketAuthenticationContext();
        
        try {
            logColorful("🚀 DEVICE LOCATION BROADCAST STARTING", BRIGHT_CYAN);
            logColorful("📱 Device ID: " + deviceId, CYAN);
            
            Optional<VehicleLastLocation> locationOpt = vehicleLastLocationRepository.findByDeviceId(deviceId);
            
            if (locationOpt.isPresent()) {
                VehicleLastLocation location = locationOpt.get();
                LocationUpdate update = createLocationUpdate(location);
                
                logColorful("✅ DEVICE LOCATION FOUND", BRIGHT_GREEN);
                logColorful("📍 Location: " + location.getLatitude() + ", " + location.getLongitude(), GREEN);
                logColorful("⏱️ Timestamp: " + location.getTimestamp(), GREEN);
                
                // 📡 Role-based broadcasting
                Optional<Vehicle> vehicleOpt = vehicleService.getVehicleByDeviceID(deviceId);
                if (vehicleOpt.isPresent()) {
                    broadcastToRoleBasedTopics(vehicleOpt.get(), update);
                }
                
                // 📡 Generic broadcasting
                messagingTemplate.convertAndSend(TOPIC_ALL_LOCATIONS, update);
                messagingTemplate.convertAndSend(TOPIC_DEVICE_PREFIX + deviceId, update);
                
                logColorful("✅ DEVICE LOCATION BROADCAST COMPLETED", BRIGHT_GREEN);
                
            } else {
                logColorful("❌ DEVICE LOCATION NOT FOUND", BRIGHT_RED);
                logColorful("📱 Device ID: " + deviceId, RED);
            }
            
        } catch (Exception e) {
            logColorful("❌ DEVICE LOCATION BROADCAST ERROR", BRIGHT_RED);
            logColorful("💥 Error: " + e.getMessage(), RED);
            logger.error("Error broadcasting location for device: {}", deviceId, e);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
    
    /**
     * 📊 ENHANCED: Broadcast statistics with dashboard
     */
    @Scheduled(fixedRate = 30000) // Every 30 seconds
    public void broadcastStatistics() {
        try {
            int statsSubscribers = sessionManager.getSubscriberCount(TOPIC_STATS);
            
            if (statsSubscribers == 0) {
                return; // No subscribers
            }
            
            // 📊 Comprehensive statistics
            Map<String, Object> stats = new ConcurrentHashMap<>();
            stats.put("totalBroadcasts", totalBroadcasts.get());
            stats.put("successfulBroadcasts", successfulBroadcasts.get());
            stats.put("failedBroadcasts", failedBroadcasts.get());
            stats.put("roleBroadcasts", roleBroadcasts.get());
            stats.put("alertsBroadcast", alertsBroadcast.get());
            stats.put("rateLimitedBroadcasts", rateLimitedBroadcasts.get());
            stats.put("vehicleLookupsSuccessful", vehicleLookupsSuccessful.get());
            stats.put("vehicleLookupsFound", vehicleLookupsFound.get());
            stats.put("activeConnections", sessionManager.getActiveSessionCount());
            stats.put("totalSubscribers", sessionManager.getTotalSubscribers());
            stats.put("roleBroadcastCounts", roleBroadcastCounts.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get())));
            stats.put("timestamp", System.currentTimeMillis());
            stats.put("successRate", calculateSuccessRate());
            
            messagingTemplate.convertAndSend(TOPIC_STATS, stats);
            
            logColorful("📊 STATISTICS BROADCASTED to " + statsSubscribers + " subscribers", BRIGHT_CYAN);
            
        } catch (Exception e) {
            logColorful("❌ STATISTICS BROADCAST ERROR", BRIGHT_RED);
            logger.error("Error broadcasting statistics", e);
        }
    }
    
    /**
     * 📊 Display comprehensive metrics dashboard
     */
    @Scheduled(fixedRate = 60000) // Every minute
    public void displayMetricsDashboard() {
        try {
            logColorful("", "");
            logColorful("╔════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════", BRIGHT_CYAN);
            logColorful("║ 📊 LOCATION BROADCAST METRICS DASHBOARD", BRIGHT_CYAN);
            logColorful("║ 🕒 " + LocalDateTime.now().format(timeFormatter), BRIGHT_CYAN);
            logColorful("╠════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════", BRIGHT_CYAN);
            logColorful("║ 📈 Total Broadcasts: " + totalBroadcasts.get(), BLUE);
            logColorful("║ ✅ Successful: " + successfulBroadcasts.get(), BRIGHT_GREEN);
            logColorful("║ ❌ Failed: " + failedBroadcasts.get(), failedBroadcasts.get() > 0 ? BRIGHT_RED : GREEN);
            logColorful("║ 🎯 Role Broadcasts: " + roleBroadcasts.get(), PURPLE);
            logColorful("║ 🚨 Alerts Broadcast: " + alertsBroadcast.get(), alertsBroadcast.get() > 0 ? BRIGHT_RED : GREEN);
            logColorful("║ ⏰ Rate Limited: " + rateLimitedBroadcasts.get(), rateLimitedBroadcasts.get() > 0 ? BRIGHT_YELLOW : GREEN);
            logColorful("║ 📊 Success Rate: " + String.format("%.2f%%", calculateSuccessRate()), calculateSuccessRate() > 95 ? BRIGHT_GREEN : BRIGHT_YELLOW);
            logColorful("║ 🔍 Vehicle Lookups: " + vehicleLookupsSuccessful.get() + " (Found: " + vehicleLookupsFound.get() + ")", CYAN);
            logColorful("║ 👥 Active Connections: " + sessionManager.getActiveSessionCount(), BRIGHT_BLUE);
            logColorful("║ 📡 Total Subscribers: " + sessionManager.getTotalSubscribers(), BRIGHT_BLUE);
            
            // 📊 Role breakdown
            logColorful("║ 🎯 ROLE BROADCAST BREAKDOWN:", BRIGHT_BLUE);
            roleBroadcastCounts.forEach((role, count) -> 
                logColorful("║   📊 " + role + ": " + count.get(), CYAN));
            
            logColorful("╚════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════", BRIGHT_CYAN);
            
        } catch (Exception e) {
            logColorful("❌ ERROR DISPLAYING METRICS DASHBOARD", BRIGHT_RED);
            logger.error("Error displaying metrics dashboard", e);
        }
    }
    
    // Utility methods
    
    /**
     * 🔍 Validate if ID is not null and not empty
     */
    private boolean isValidId(Object id) {
        if (id == null) return false;
        String idStr = String.valueOf(id).trim();
        return !idStr.isEmpty() && !"null".equalsIgnoreCase(idStr);
    }
    
    /**
     * ⏰ Check if broadcast should be allowed (rate limiting)
     */
    private boolean shouldBroadcast(String deviceId) {
        Long lastBroadcast = lastBroadcastTime.get(deviceId);
        if (lastBroadcast == null) return true;
        
        long timeSinceLastBroadcast = System.currentTimeMillis() - lastBroadcast;
        return timeSinceLastBroadcast >= rateLimitMs;
    }
    
    /**
     * ⏱️ Get time since last broadcast
     */
    private long getTimeSinceLastBroadcast(String deviceId) {
        Long lastBroadcast = lastBroadcastTime.get(deviceId);
        return lastBroadcast != null ? System.currentTimeMillis() - lastBroadcast : 0;
    }
    
    /**
     * 🕒 Check if outside operating hours
     */
    private boolean isOutsideOperatingHours() {
        int currentHour = java.time.LocalTime.now().getHour();
        return currentHour < operatingHoursStart || currentHour >= operatingHoursEnd;
    }
    
    /**
     * 📊 Calculate success rate
     */
    private double calculateSuccessRate() {
        long total = totalBroadcasts.get();
        return total > 0 ? (successfulBroadcasts.get() * 100.0) / total : 0.0;
    }
    
    /**
     * 📊 Update broadcast metrics
     */
    private void updateBroadcastMetrics(String deviceId, long startTime, boolean success) {
        if (success) {
            successfulBroadcasts.incrementAndGet();
        }
        
        // Update device broadcast count
        deviceBroadcastCounts.computeIfAbsent(deviceId, k -> new AtomicLong(0)).incrementAndGet();
        
        // Update last broadcast time
        lastBroadcastTime.put(deviceId, System.currentTimeMillis());
        
        // Track broadcast timing
        long duration = System.currentTimeMillis() - startTime;
        broadcastTimes.put(deviceId + "-" + System.currentTimeMillis(), duration);
        
        // Performance logging
        if (duration > 1000) {
            performanceLogger.warn("Slow broadcast detected: {}ms for device {}", duration, deviceId);
        }
    }
    
    /**
     * 🔧 Create location update from GPS data
     */
    private LocationUpdate createLocationUpdate(GpsData gpsData) {
        return new LocationUpdate(
            Double.parseDouble(gpsData.getLatitude()),
            Double.parseDouble(gpsData.getLongitude()),
            gpsData.getDeviceID(),
            gpsData.getTimestamp(),
            gpsData.getSpeed(),
            gpsData.getIgnition(),
            gpsData.getCourse(),
            gpsData.getVehicleStatus(),
            gpsData.getGsmStrength(),
            gpsData.getAdditionalData() != null ? gpsData.getAdditionalData() : "",
            gpsData.getTimeIntervals() != null ? gpsData.getTimeIntervals() : ""
        );
    }
    
    /**
     * 🔧 Create location update from VehicleLastLocation
     */
    private LocationUpdate createLocationUpdate(VehicleLastLocation location) {
        return new LocationUpdate(
            location.getLatitude(),
            location.getLongitude(),
            location.getDeviceId(),
            location.getTimestamp().toString(),
            location.getSpeed(),
            location.getIgnition(),
            location.getCourse(),
            location.getVehicleStatus(),
            location.getGsmStrength(),
            "", // Additional data not stored in last location
            ""  // Time intervals not stored in last location
        );
    }
    
    /**
     * 🚨 Create alert object
     */
    private Map<String, Object> createAlert(String type, String deviceId, String message) {
        Map<String, Object> alert = new ConcurrentHashMap<>();
        alert.put("type", type);
        alert.put("deviceId", deviceId);
        alert.put("message", message);
        alert.put("timestamp", System.currentTimeMillis());
        alert.put("severity", determineSeverity(type));
        return alert;
    }
    
    /**
     * 🚨 Determine alert severity
     */
    private String determineSeverity(String alertType) {
        return switch (alertType) {
            case "THEFT_ALERT" -> "CRITICAL";
            case "SPEED_ALERT", "IGNITION_ALERT" -> "HIGH";
            case "LOCATION_ALERT", "DRIVING_ALERT" -> "MEDIUM";
            default -> "LOW";
        };
    }
    
    /**
     * 📊 Get topic counts
     */
    private int getRoleTopicCount() {
        return 5; // DEALER, ADMIN, CLIENT, USER, SUPERADMIN
    }
    
    private int getSystemTopicCount() {
        return 3; // ALL_LOCATIONS, DEVICE_PREFIX, ALERTS, STATS
    }
    
    /**
     * 📊 Get broadcast metrics
     */
    public BroadcastMetrics getMetrics() {
        return new BroadcastMetrics(
            totalBroadcasts.get(),
            successfulBroadcasts.get(),
            failedBroadcasts.get(),
            roleBroadcasts.get(),
            alertsBroadcast.get(),
            rateLimitedBroadcasts.get(),
            vehicleLookupsSuccessful.get(),
            vehicleLookupsFound.get(),
            sessionManager.getActiveSessionCount(),
            sessionManager.getTotalSubscribers(),
            calculateSuccessRate(),
            new HashMap<>(roleBroadcastCounts.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get())))
        );
    }
    
    /**
     * 🔄 Reset metrics
     */
    public void resetMetrics() {
        totalBroadcasts.set(0);
        successfulBroadcasts.set(0);
        failedBroadcasts.set(0);
        roleBroadcasts.set(0);
        alertsBroadcast.set(0);
        rateLimitedBroadcasts.set(0);
        vehicleLookupsSuccessful.set(0);
        vehicleLookupsFound.set(0);
        
        lastBroadcastTime.clear();
        deviceBroadcastCounts.clear();
        broadcastTimes.clear();
        
        roleBroadcastCounts.values().forEach(counter -> counter.set(0));
        
        logColorful("🔄 BROADCAST METRICS RESET", BRIGHT_GREEN);
        logger.info("Location broadcast metrics reset");
    }
    
    /**
     * 🎨 Colorful logging utility
     */
    private void logColorful(String message, String color) {
        System.out.println(color + message + RESET);
        logger.info(message.replaceAll("║", "").replaceAll("╔", "").replaceAll("╚", "").replaceAll("╠", "").replaceAll("═", "").trim());
    }
    
    /**
     * 📊 Enhanced broadcast metrics class
     */
    public static class BroadcastMetrics {
        private final long totalBroadcasts;
        private final long successfulBroadcasts;
        private final long failedBroadcasts;
        private final long roleBroadcasts;
        private final long alertsBroadcast;
        private final long rateLimitedBroadcasts;
        private final long vehicleLookupsSuccessful;
        private final long vehicleLookupsFound;
        private final int activeConnections;
        private final int totalSubscribers;
        private final double successRate;
        private final Map<String, Long> roleBroadcastCounts;
        
        public BroadcastMetrics(long totalBroadcasts, long successfulBroadcasts, long failedBroadcasts, 
                              long roleBroadcasts, long alertsBroadcast, long rateLimitedBroadcasts,
                              long vehicleLookupsSuccessful, long vehicleLookupsFound,
                              int activeConnections, int totalSubscribers, double successRate,
                              Map<String, Long> roleBroadcastCounts) {
            this.totalBroadcasts = totalBroadcasts;
            this.successfulBroadcasts = successfulBroadcasts;
            this.failedBroadcasts = failedBroadcasts;
            this.roleBroadcasts = roleBroadcasts;
            this.alertsBroadcast = alertsBroadcast;
            this.rateLimitedBroadcasts = rateLimitedBroadcasts;
            this.vehicleLookupsSuccessful = vehicleLookupsSuccessful;
            this.vehicleLookupsFound = vehicleLookupsFound;
            this.activeConnections = activeConnections;
            this.totalSubscribers = totalSubscribers;
            this.successRate = successRate;
            this.roleBroadcastCounts = roleBroadcastCounts;
        }
        
        // Getters
        public long getTotalBroadcasts() { return totalBroadcasts; }
        public long getSuccessfulBroadcasts() { return successfulBroadcasts; }
        public long getFailedBroadcasts() { return failedBroadcasts; }
        public long getRoleBroadcasts() { return roleBroadcasts; }
        public long getAlertsBroadcast() { return alertsBroadcast; }
        public long getRateLimitedBroadcasts() { return rateLimitedBroadcasts; }
        public long getVehicleLookupsSuccessful() { return vehicleLookupsSuccessful; }
        public long getVehicleLookupsFound() { return vehicleLookupsFound; }
        public int getActiveConnections() { return activeConnections; }
        public int getTotalSubscribers() { return totalSubscribers; }
        public double getSuccessRate() { return successRate; }
        public Map<String, Long> getRoleBroadcastCounts() { return roleBroadcastCounts; }
        
        @Override
        public String toString() {
            return String.format("BroadcastMetrics{total=%d, successful=%d, failed=%d, roles=%d, alerts=%d, " +
                               "rateLimited=%d, lookups=%d, connections=%d, successRate=%.2f%%}", 
                totalBroadcasts, successfulBroadcasts, failedBroadcasts, roleBroadcasts, alertsBroadcast,
                rateLimitedBroadcasts, vehicleLookupsSuccessful, activeConnections, successRate);
        }
    }
    
    
 // ===============================================================================
 // OPTIONAL ADDITIONS - Add these BEFORE the final closing brace if needed
 // ===============================================================================

 /**
  * 🎯 Get top devices by broadcast count
  */
 public Map<String, Long> getTopDevicesByBroadcastCount() {
     return deviceBroadcastCounts.entrySet().stream()
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
  * 📊 Get detailed performance statistics
  */
 public PerformanceStatistics getPerformanceStatistics() {
     long totalTiming = broadcastTimes.values().stream()
         .mapToLong(Long::longValue)
         .sum();
     
     long avgBroadcastTime = broadcastTimes.isEmpty() ? 0 : totalTiming / broadcastTimes.size();
     
     long maxBroadcastTime = broadcastTimes.values().stream()
         .mapToLong(Long::longValue)
         .max()
         .orElse(0);
     
     return new PerformanceStatistics(
         avgBroadcastTime,
         maxBroadcastTime,
         broadcastTimes.size(),
         getTopDevicesByBroadcastCount()
     );
 }

 /**
  * 🔧 Force broadcast to specific role
  */
 public void forceBroadcastToRole(String roleType, String roleId, LocationUpdate update) {
     try {
         String topic = switch (roleType.toUpperCase()) {
             case "DEALER" -> TOPIC_DEALER_PREFIX + roleId;
             case "ADMIN" -> TOPIC_ADMIN_PREFIX + roleId;
             case "CLIENT" -> TOPIC_CLIENT_PREFIX + roleId;
             case "USER" -> TOPIC_USER_PREFIX + roleId;
             case "SUPERADMIN" -> TOPIC_SUPERADMIN_PREFIX + roleId;
             default -> throw new IllegalArgumentException("Invalid role type: " + roleType);
         };
         
         logColorful("🎯 FORCE BROADCAST TO " + roleType, BRIGHT_MAGENTA);
         logColorful("📡 Topic: " + topic, BRIGHT_RED );
         
         messagingTemplate.convertAndSend(topic, update);
         roleBroadcasts.incrementAndGet();
         
         logColorful("✅ FORCE BROADCAST COMPLETED", BRIGHT_GREEN);
         
     } catch (Exception e) {
         logColorful("❌ FORCE BROADCAST FAILED", BRIGHT_RED);
         logger.error("Error in force broadcast to {} role (ID: {})", roleType, roleId, e);
     }
 }

 /**
  * 🧹 Cleanup old broadcast data
  */
 @Scheduled(fixedRate = 300000) // Every 5 minutes
 public void cleanupOldBroadcastData() {
     try {
         // Clean up old broadcast times (keep only last 1000)
         if (broadcastTimes.size() > 1000) {
             List<String> oldestKeys = broadcastTimes.keySet().stream()
                 .sorted()
                 .limit(broadcastTimes.size() - 1000)
                 .collect(Collectors.toList());
             
             oldestKeys.forEach(broadcastTimes::remove);
             logColorful("🧹 Cleaned up " + oldestKeys.size() + " old broadcast time entries", CYAN);
         }
         
         // Clean up old last broadcast times for inactive devices
         long cutoffTime = System.currentTimeMillis() - (24 * 60 * 60 * 1000); // 24 hours
         List<String> inactiveDevices = lastBroadcastTime.entrySet().stream()
             .filter(entry -> entry.getValue() < cutoffTime)
             .map(Map.Entry::getKey)
             .collect(Collectors.toList());
         
         inactiveDevices.forEach(lastBroadcastTime::remove);
         
         if (!inactiveDevices.isEmpty()) {
             logColorful("🧹 Cleaned up " + inactiveDevices.size() + " inactive device entries", CYAN);
         }
         
     } catch (Exception e) {
         logColorful("❌ ERROR DURING CLEANUP", BRIGHT_RED);
         logger.error("Error during broadcast data cleanup", e);
     }
 }

 /**
  * 📊 Performance statistics class
  */
 public static class PerformanceStatistics {
     private final long averageBroadcastTime;
     private final long maxBroadcastTime;
     private final long totalTimedBroadcasts;
     private final Map<String, Long> topDevices;
     
     public PerformanceStatistics(long averageBroadcastTime, long maxBroadcastTime, 
                                long totalTimedBroadcasts, Map<String, Long> topDevices) {
         this.averageBroadcastTime = averageBroadcastTime;
         this.maxBroadcastTime = maxBroadcastTime;
         this.totalTimedBroadcasts = totalTimedBroadcasts;
         this.topDevices = topDevices;
     }
     
     // Getters
     public long getAverageBroadcastTime() { return averageBroadcastTime; }
     public long getMaxBroadcastTime() { return maxBroadcastTime; }
     public long getTotalTimedBroadcasts() { return totalTimedBroadcasts; }
     public Map<String, Long> getTopDevices() { return topDevices; }
     
     @Override
     public String toString() {
         return String.format("PerformanceStatistics{avgTime=%dms, maxTime=%dms, totalTimed=%d}", 
             averageBroadcastTime, maxBroadcastTime, totalTimedBroadcasts);
     }
 }

 // ===============================================================================
 // END OF OPTIONAL ADDITIONS
 // ===============================================================================
    
    
}




