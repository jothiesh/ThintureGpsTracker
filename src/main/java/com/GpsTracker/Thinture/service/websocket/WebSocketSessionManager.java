package com.GpsTracker.Thinture.service.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.*;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 🌟 ENHANCED WebSocket Session Manager - Production Ready for 5000+ Devices
 * ✅ Colorful, structured logging with emojis
 * ✅ Advanced session tracking and management
 * ✅ Performance monitoring and metrics
 * ✅ Real-time session dashboard
 * ✅ Role-based subscription monitoring
 * ✅ Device-specific tracking
 * ✅ Automatic cleanup and memory management
 * ✅ Connection health monitoring
 * 
 * Manages WebSocket sessions and subscriptions
 * Tracks connected clients and their subscriptions for 5000+ devices
 * Enhanced with comprehensive monitoring and beautiful logging
 */
@Component
public class WebSocketSessionManager {
    
    private static final Logger logger = LoggerFactory.getLogger(WebSocketSessionManager.class);
    private static final Logger performanceLogger = LoggerFactory.getLogger("PERFORMANCE");
    private static final Logger sessionLogger = LoggerFactory.getLogger("SESSION");
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
    // 📊 Session management
    private final Map<String, EnhancedSessionInfo> sessions = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> topicSubscriptions = new ConcurrentHashMap<>();
    private final AtomicInteger totalConnections = new AtomicInteger(0);
    private final AtomicInteger totalDisconnections = new AtomicInteger(0);
    private final AtomicInteger totalSubscriptions = new AtomicInteger(0);
    private final AtomicInteger totalUnsubscriptions = new AtomicInteger(0);
    
    // 📊 Enhanced metrics
    private final Map<String, Long> sessionDurations = new ConcurrentHashMap<>();
    private final Map<String, Integer> sessionMessageCounts = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> roleSubscriptionCounts = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> deviceSubscriptionCounts = new ConcurrentHashMap<>();
    private final AtomicLong peakConcurrentSessions = new AtomicLong(0);
    private final AtomicLong totalSessionTime = new AtomicLong(0);
    
    // ⚙️ Configuration constants
    private static final long INACTIVE_SESSION_TIMEOUT = 3600000; // 1 hour
    private static final long STALE_SESSION_TIMEOUT = 1800000;    // 30 minutes
    private static final int MAX_SESSION_DURATIONS = 1000;
    private static final long CLEANUP_INTERVAL = 300000; // 5 minutes
    
    // 🕒 Time formatting
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    
    @PostConstruct
    public void initialize() {
        logColorful("🌟 INITIALIZING ENHANCED WEBSOCKET SESSION MANAGER", BRIGHT_CYAN);
        logColorful("⏱️ Startup time: " + LocalDateTime.now().format(timeFormatter), CYAN);
        
        // 📊 Display configuration
        displayConfiguration();
        
        // 🔧 Initialize role subscription counters
        initializeRoleCounters();
        
        logColorful("✅ Enhanced WebSocket Session Manager initialized successfully", BRIGHT_GREEN);
        displayWelcomeDashboard();
    }
    
    /**
     * 📊 Display manager configuration
     */
    private void displayConfiguration() {
        logColorful("", "");
        logColorful("╔════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════", BRIGHT_BLUE);
        logColorful("║ ⚙️ WEBSOCKET SESSION MANAGER CONFIGURATION", BRIGHT_BLUE);
        logColorful("╠════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════", BRIGHT_BLUE);
        logColorful("║ ⏰ Inactive Session Timeout: " + (INACTIVE_SESSION_TIMEOUT / 60000) + " minutes", BLUE);
        logColorful("║ 🗑️ Stale Session Timeout: " + (STALE_SESSION_TIMEOUT / 60000) + " minutes", BLUE);
        logColorful("║ 📊 Max Session Durations: " + MAX_SESSION_DURATIONS, BLUE);
        logColorful("║ 🧹 Cleanup Interval: " + (CLEANUP_INTERVAL / 60000) + " minutes", BLUE);
        logColorful("║ 🎯 Target Capacity: 5000+ concurrent sessions", BLUE);
        logColorful("║ 🏗️ Architecture: Enhanced Session & Subscription Management", BLUE);
        logColorful("╚════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════", BRIGHT_BLUE);
    }
    
    /**
     * 🔧 Initialize role subscription counters
     */
    private void initializeRoleCounters() {
        roleSubscriptionCounts.put("DEALER", new AtomicLong(0));
        roleSubscriptionCounts.put("ADMIN", new AtomicLong(0));
        roleSubscriptionCounts.put("CLIENT", new AtomicLong(0));
        roleSubscriptionCounts.put("USER", new AtomicLong(0));
        roleSubscriptionCounts.put("SUPERADMIN", new AtomicLong(0));
        roleSubscriptionCounts.put("GENERAL", new AtomicLong(0));
        roleSubscriptionCounts.put("DEVICE", new AtomicLong(0));
        roleSubscriptionCounts.put("ALERTS", new AtomicLong(0));
        roleSubscriptionCounts.put("STATS", new AtomicLong(0));
        
        logColorful("🔧 Role subscription counters initialized: " + roleSubscriptionCounts.size() + " roles", CYAN);
    }
    
    /**
     * 🎨 Display welcome dashboard
     */
    private void displayWelcomeDashboard() {
        logColorful("", "");
        logColorful("╔════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════", BRIGHT_MAGENTA);
        logColorful("║ 🎯 ENHANCED WEBSOCKET SESSION MANAGER DASHBOARD", BRIGHT_MAGENTA);
        logColorful("║ 🚀 Ready to manage WebSocket sessions for 5000+ devices", BRIGHT_MAGENTA);
        logColorful("╠════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════", BRIGHT_MAGENTA);
        logColorful("║ 🔌 Connection Management: ACTIVE", BRIGHT_GREEN);
        logColorful("║ 📡 Subscription Tracking: ACTIVE", BRIGHT_GREEN);
        logColorful("║ 🎯 Role-Based Monitoring: ACTIVE", BRIGHT_GREEN);
        logColorful("║ 📱 Device-Specific Tracking: ACTIVE", BRIGHT_GREEN);
        logColorful("║ 🧹 Automatic Cleanup: ACTIVE", BRIGHT_GREEN);
        logColorful("║ 📊 Performance Monitoring: ACTIVE", BRIGHT_GREEN);
        logColorful("║ 📈 Real-time Statistics: ACTIVE", BRIGHT_GREEN);
        logColorful("╚════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════", BRIGHT_MAGENTA);
    }
    
    /**
     * 🔌 ENHANCED: Handle new WebSocket connection
     */
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        
        try {
            if (sessionId != null) {
                // 🏗️ Create enhanced session info
                EnhancedSessionInfo sessionInfo = new EnhancedSessionInfo(sessionId);
                sessions.put(sessionId, sessionInfo);
                totalConnections.incrementAndGet();
                
                // 📊 Update peak concurrent sessions
                long currentSessions = sessions.size();
                peakConcurrentSessions.updateAndGet(peak -> Math.max(peak, currentSessions));
                
                logColorful("🔌 NEW WEBSOCKET CONNECTION", BRIGHT_GREEN);
                logColorful("📱 Session ID: " + sessionId, GREEN);
                logColorful("👥 Total active sessions: " + currentSessions, GREEN);
                logColorful("📊 Peak concurrent sessions: " + peakConcurrentSessions.get(), GREEN);
                
                // 📋 Log connection details
                Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();
                if (sessionAttributes != null && !sessionAttributes.isEmpty()) {
                    sessionInfo.setAttributes(sessionAttributes);
                    logColorful("📋 Session attributes: " + sessionAttributes.size() + " attributes", CYAN);
                    sessionLogger.debug("Session attributes for {}: {}", sessionId, sessionAttributes);
                } else {
                    logColorful("📋 No session attributes provided", YELLOW);
                }
                
                // 🌐 Extract connection info
                extractConnectionInfo(headerAccessor, sessionInfo);
                
                sessionLogger.info("WebSocket connection established - Session: {}, Total active: {}, Peak: {}", 
                    sessionId, currentSessions, peakConcurrentSessions.get());
                
            } else {
                logColorful("🚫 CONNECTION EVENT WITH NULL SESSION ID", BRIGHT_RED);
                logger.warn("WebSocket connection event with null session ID");
            }
        } catch (Exception e) {
            logColorful("❌ ERROR HANDLING CONNECTION EVENT", BRIGHT_RED);
            logColorful("💥 Error: " + e.getMessage(), RED);
            logger.error("Error handling WebSocket connection event", e);
        }
    }
    
    /**
     * 🔌 ENHANCED: Handle WebSocket disconnection
     */
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        
        try {
            if (sessionId != null) {
                logColorful("🔌 WEBSOCKET DISCONNECTION", BRIGHT_RED);
                logColorful("📱 Session ID: " + sessionId, RED);
                
                cleanupSession(sessionId, "normal_disconnect");
                
            } else {
                logColorful("🚫 DISCONNECT EVENT WITH NULL SESSION ID", BRIGHT_RED);
                logger.warn("WebSocket disconnect event with null session ID");
            }
        } catch (Exception e) {
            logColorful("❌ ERROR HANDLING DISCONNECTION EVENT", BRIGHT_RED);
            logColorful("💥 Error: " + e.getMessage(), RED);
            logger.error("Error handling WebSocket disconnection event", e);
        }
    }
    
    /**
     * 🧹 ENHANCED: Session cleanup with detailed logging
     */
    private void cleanupSession(String sessionId, String reason) {
        try {
            EnhancedSessionInfo sessionInfo = sessions.remove(sessionId);
            totalDisconnections.incrementAndGet();
            
            if (sessionInfo != null) {
                // 📊 Calculate session duration
                long duration = System.currentTimeMillis() - sessionInfo.getConnectedTime();
                sessionDurations.put(sessionId, duration);
                totalSessionTime.addAndGet(duration);
                
                logColorful("🧹 SESSION CLEANUP STARTING", BRIGHT_BLUE);
                logColorful("📱 Session ID: " + sessionId, BLUE);
                logColorful("⏱️ Duration: " + formatDuration(duration), BLUE);
                logColorful("🔄 Reason: " + reason, BLUE);
                
                // 🧹 Remove all subscriptions for this session
                Set<String> subscriptionsToCleanup = new HashSet<>(sessionInfo.getSubscriptions());
                int cleanedSubscriptions = 0;
                
                for (String topic : subscriptionsToCleanup) {
                    Set<String> subscribers = topicSubscriptions.get(topic);
                    if (subscribers != null) {
                        subscribers.remove(sessionId);
                        cleanedSubscriptions++;
                        
                        if (subscribers.isEmpty()) {
                            topicSubscriptions.remove(topic);
                            logColorful("🗑️ Removed empty topic: " + topic, YELLOW);
                        }
                    }
                }
                
                // 🧹 Clean up device subscriptions
                Set<String> deviceSubscriptions = sessionInfo.getDeviceSubscriptions();
                if (!deviceSubscriptions.isEmpty()) {
                    logColorful("📱 Device subscriptions cleaned: " + deviceSubscriptions.size(), CYAN);
                    deviceSubscriptions.forEach(deviceId -> 
                        deviceSubscriptionCounts.computeIfAbsent(deviceId, k -> new AtomicLong(0)).decrementAndGet());
                }
                
                // 🧹 Clean up message counts
                sessionMessageCounts.remove(sessionId);
                
                logColorful("✅ SESSION CLEANUP COMPLETED", BRIGHT_GREEN);
                logColorful("📊 Cleaned subscriptions: " + cleanedSubscriptions, GREEN);
                logColorful("👥 Remaining active sessions: " + sessions.size(), GREEN);
                
                sessionLogger.info("Session cleanup completed [{}] - Session: {}, Duration: {}ms, " +
                    "Subscriptions cleaned: {}, Remaining active: {}", 
                    reason, sessionId, duration, cleanedSubscriptions, sessions.size());
                
            } else {
                logColorful("⚠️ SESSION NOT FOUND FOR CLEANUP: " + sessionId, BRIGHT_YELLOW);
                logger.warn("Session not found for cleanup: {}", sessionId);
            }
            
            // 🧹 Periodic cleanup trigger
            if (sessions.size() % 100 == 0) {
                cleanupOldMetrics();
            }
            
        } catch (Exception e) {
            logColorful("❌ ERROR DURING SESSION CLEANUP", BRIGHT_RED);
            logColorful("💥 Error: " + e.getMessage(), RED);
            logger.error("Error during session cleanup for session: {}", sessionId, e);
        }
    }
    
    /**
     * 📡 ENHANCED: Handle subscription to a topic
     */
    @EventListener
    public void handleSubscribeEvent(SessionSubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        String destination = headerAccessor.getDestination();
        
        try {
            if (sessionId != null && destination != null) {
                logColorful("📡 NEW SUBSCRIPTION", BRIGHT_GREEN);
                logColorful("📱 Session ID: " + sessionId, GREEN);
                logColorful("🎯 Topic: " + destination, GREEN);
                
                // 📊 Update session info
                EnhancedSessionInfo sessionInfo = sessions.get(sessionId);
                if (sessionInfo != null) {
                    sessionInfo.addSubscription(destination);
                    sessionInfo.updateLastActivity();
                    totalSubscriptions.incrementAndGet();
                    
                    // 📡 Update topic subscriptions
                    Set<String> subscribers = topicSubscriptions.computeIfAbsent(destination, k -> ConcurrentHashMap.newKeySet());
                    subscribers.add(sessionId);
                    
                    int subscriberCount = subscribers.size();
                    logColorful("👥 Total subscribers for topic: " + subscriberCount, GREEN);
                    
                    // 📱 Track device-specific subscriptions
                    if (destination.startsWith("/topic/device/")) {
                        String deviceId = destination.substring("/topic/device/".length());
                        sessionInfo.addDeviceSubscription(deviceId);
                        deviceSubscriptionCounts.computeIfAbsent(deviceId, k -> new AtomicLong(0)).incrementAndGet();
                        
                        logColorful("📱 DEVICE SUBSCRIPTION: " + deviceId, BRIGHT_CYAN);
                        logColorful("📊 Total device subscriptions: " + deviceSubscriptionCounts.get(deviceId).get(), CYAN);
                    }
                    
                    // 🎯 Track role-based subscriptions
                    trackRoleBasedSubscription(destination, sessionId);
                    
                    logColorful("✅ SUBSCRIPTION COMPLETED", BRIGHT_GREEN);
                    sessionLogger.info("Subscription completed - Session: {}, Topic: {}, Subscribers: {}", 
                        sessionId, destination, subscriberCount);
                    
                } else {
                    logColorful("❌ SUBSCRIPTION FOR UNKNOWN SESSION: " + sessionId, BRIGHT_RED);
                    logger.warn("Subscription event for unknown session: {}", sessionId);
                }
                
            } else {
                logColorful("🚫 INVALID SUBSCRIPTION EVENT", BRIGHT_RED);
                logColorful("📱 Session ID: " + sessionId, RED);
                logColorful("🎯 Destination: " + destination, RED);
                logger.warn("Invalid subscription event: sessionId={}, destination={}", sessionId, destination);
            }
        } catch (Exception e) {
            logColorful("❌ ERROR HANDLING SUBSCRIPTION EVENT", BRIGHT_RED);
            logColorful("💥 Error: " + e.getMessage(), RED);
            logger.error("Error handling subscription event", e);
        }
    }
    
    /**
     * 🎯 ENHANCED: Track role-based subscriptions with detailed logging
     */
    private void trackRoleBasedSubscription(String destination, String sessionId) {
        try {
            String roleType = null;
            String roleId = null;
            
            if (destination.startsWith("/topic/location-updates/dealer/")) {
                roleType = "DEALER";
                roleId = destination.substring("/topic/location-updates/dealer/".length());
            } else if (destination.startsWith("/topic/location-updates/admin/")) {
                roleType = "ADMIN";
                roleId = destination.substring("/topic/location-updates/admin/".length());
            } else if (destination.startsWith("/topic/location-updates/client/")) {
                roleType = "CLIENT";
                roleId = destination.substring("/topic/location-updates/client/".length());
            } else if (destination.startsWith("/topic/location-updates/user/")) {
                roleType = "USER";
                roleId = destination.substring("/topic/location-updates/user/".length());
            } else if (destination.startsWith("/topic/location-updates/superadmin/")) {
                roleType = "SUPERADMIN";
                roleId = destination.substring("/topic/location-updates/superadmin/".length());
            } else if (destination.equals("/topic/location-updates")) {
                roleType = "GENERAL";
                roleId = "all";
            } else if (destination.equals("/topic/alerts")) {
                roleType = "ALERTS";
                roleId = "all";
            } else if (destination.equals("/topic/stats")) {
                roleType = "STATS";
                roleId = "all";
            }
            
            if (roleType != null) {
                roleSubscriptionCounts.get(roleType).incrementAndGet();
                
                logColorful("🎯 ROLE-BASED SUBSCRIPTION DETECTED", BRIGHT_MAGENTA);
                logColorful("👤 Role Type: " + roleType, BRIGHT_MAGENTA);
                logColorful("🏷️ Role ID: " + roleId, BRIGHT_MAGENTA);
                logColorful("📊 Total " + roleType + " subscriptions: " + roleSubscriptionCounts.get(roleType).get(), BRIGHT_MAGENTA);
                
                sessionLogger.debug("Role-based subscription - Session: {}, Role: {}, ID: {}, Topic: {}", 
                    sessionId, roleType, roleId, destination);
            }
        } catch (Exception e) {
            logColorful("❌ ERROR TRACKING ROLE-BASED SUBSCRIPTION", BRIGHT_RED);
            logger.error("Error tracking role-based subscription", e);
        }
    }
    
    /**
     * 📡 ENHANCED: Handle unsubscription from a topic
     */
    @EventListener
    public void handleUnsubscribeEvent(SessionUnsubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        String destination = headerAccessor.getDestination();
        
        try {
            if (sessionId != null && destination != null) {
                logColorful("📡 UNSUBSCRIPTION", BRIGHT_YELLOW);
                logColorful("📱 Session ID: " + sessionId, YELLOW);
                logColorful("🎯 Topic: " + destination, YELLOW);
                
                // 📊 Update session info
                EnhancedSessionInfo sessionInfo = sessions.get(sessionId);
                if (sessionInfo != null) {
                    sessionInfo.removeSubscription(destination);
                    sessionInfo.updateLastActivity();
                    totalUnsubscriptions.incrementAndGet();
                    
                    // 📡 Update topic subscriptions
                    Set<String> subscribers = topicSubscriptions.get(destination);
                    if (subscribers != null) {
                        subscribers.remove(sessionId);
                        
                        if (subscribers.isEmpty()) {
                            topicSubscriptions.remove(destination);
                            logColorful("🗑️ Removed empty topic: " + destination, YELLOW);
                        }
                        
                        logColorful("👥 Remaining subscribers: " + subscribers.size(), YELLOW);
                    }
                    
                    // 📱 Track device-specific unsubscriptions
                    if (destination.startsWith("/topic/device/")) {
                        String deviceId = destination.substring("/topic/device/".length());
                        sessionInfo.removeDeviceSubscription(deviceId);
                        
                        AtomicLong deviceCount = deviceSubscriptionCounts.get(deviceId);
                        if (deviceCount != null) {
                            deviceCount.decrementAndGet();
                            logColorful("📱 Device unsubscription: " + deviceId, CYAN);
                        }
                    }
                    
                    // 🎯 Track role-based unsubscriptions
                    trackRoleBasedUnsubscription(destination);
                    
                    logColorful("✅ UNSUBSCRIPTION COMPLETED", BRIGHT_GREEN);
                    sessionLogger.info("Unsubscription completed - Session: {}, Topic: {}", sessionId, destination);
                }
                
            } else {
                logColorful("🚫 INVALID UNSUBSCRIPTION EVENT", BRIGHT_RED);
                logger.warn("Invalid unsubscription event: sessionId={}, destination={}", sessionId, destination);
            }
        } catch (Exception e) {
            logColorful("❌ ERROR HANDLING UNSUBSCRIPTION EVENT", BRIGHT_RED);
            logColorful("💥 Error: " + e.getMessage(), RED);
            logger.error("Error handling unsubscription event", e);
        }
    }
    
    /**
     * 🎯 Track role-based unsubscriptions
     */
    private void trackRoleBasedUnsubscription(String destination) {
        try {
            String roleType = null;
            
            if (destination.startsWith("/topic/location-updates/dealer/")) {
                roleType = "DEALER";
            } else if (destination.startsWith("/topic/location-updates/admin/")) {
                roleType = "ADMIN";
            } else if (destination.startsWith("/topic/location-updates/client/")) {
                roleType = "CLIENT";
            } else if (destination.startsWith("/topic/location-updates/user/")) {
                roleType = "USER";
            } else if (destination.startsWith("/topic/location-updates/superadmin/")) {
                roleType = "SUPERADMIN";
            } else if (destination.equals("/topic/location-updates")) {
                roleType = "GENERAL";
            } else if (destination.equals("/topic/alerts")) {
                roleType = "ALERTS";
            } else if (destination.equals("/topic/stats")) {
                roleType = "STATS";
            }
            
            if (roleType != null) {
                AtomicLong roleCount = roleSubscriptionCounts.get(roleType);
                if (roleCount != null) {
                    roleCount.decrementAndGet();
                    logColorful("🎯 Role unsubscription: " + roleType + " (remaining: " + roleCount.get() + ")", BRIGHT_MAGENTA);
                }
            }
        } catch (Exception e) {
            logger.error("Error tracking role-based unsubscription", e);
        }
    }
    
    /**
     * 🧹 ENHANCED: Scheduled cleanup with detailed logging
     */
    @Scheduled(fixedRate = CLEANUP_INTERVAL)
    public void scheduledCleanup() {
        try {
            logColorful("🧹 SCHEDULED CLEANUP STARTING", BRIGHT_BLUE);
            logColorful("⏱️ Time: " + LocalDateTime.now().format(timeFormatter), BLUE);
            
            long startTime = System.currentTimeMillis();
            
            // 🧹 Various cleanup operations
            cleanupOldMetrics();
            cleanupInactiveSessions();
            cleanupStaleTopics();
            
            long cleanupTime = System.currentTimeMillis() - startTime;
            
            logColorful("✅ SCHEDULED CLEANUP COMPLETED", BRIGHT_GREEN);
            logColorful("⏱️ Cleanup time: " + cleanupTime + "ms", GREEN);
            
            // 📊 Log cleanup statistics
            logCleanupStatistics();
            
        } catch (Exception e) {
            logColorful("❌ ERROR DURING SCHEDULED CLEANUP", BRIGHT_RED);
            logColorful("💥 Error: " + e.getMessage(), RED);
            logger.error("Error during scheduled cleanup", e);
        }
    }
    
    /**
     * 🧹 Clean up inactive sessions
     */
    private void cleanupInactiveSessions() {
        try {
            long now = System.currentTimeMillis();
            List<String> inactiveSessions = sessions.entrySet().stream()
                .filter(entry -> {
                    EnhancedSessionInfo session = entry.getValue();
                    return (now - session.getLastActivityTime()) > INACTIVE_SESSION_TIMEOUT;
                })
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
            
            if (!inactiveSessions.isEmpty()) {
                logColorful("🧹 CLEANING UP INACTIVE SESSIONS", BRIGHT_YELLOW);
                logColorful("📊 Inactive sessions found: " + inactiveSessions.size(), YELLOW);
                
                inactiveSessions.forEach(sessionId -> {
                    logColorful("🗑️ Removing inactive session: " + sessionId, YELLOW);
                    cleanupSession(sessionId, "inactive_timeout");
                });
                
                logColorful("✅ INACTIVE SESSION CLEANUP COMPLETED", BRIGHT_GREEN);
            } else {
                logColorful("✅ NO INACTIVE SESSIONS FOUND", GREEN);
            }
        } catch (Exception e) {
            logColorful("❌ ERROR CLEANING UP INACTIVE SESSIONS", BRIGHT_RED);
            logger.error("Error cleaning up inactive sessions", e);
        }
    }
    
    /**
     * 🧹 Clean up stale topics
     */
    private void cleanupStaleTopics() {
        try {
            List<String> staleTopics = topicSubscriptions.entrySet().stream()
                .filter(entry -> entry.getValue().isEmpty())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
            
            if (!staleTopics.isEmpty()) {
                logColorful("🧹 CLEANING UP STALE TOPICS", BRIGHT_YELLOW);
                logColorful("📊 Stale topics found: " + staleTopics.size(), YELLOW);
                
                staleTopics.forEach(topic -> {
                    topicSubscriptions.remove(topic);
                    logColorful("🗑️ Removed stale topic: " + topic, YELLOW);
                });
                
                logColorful("✅ STALE TOPIC CLEANUP COMPLETED", BRIGHT_GREEN);
            } else {
                logColorful("✅ NO STALE TOPICS FOUND", GREEN);
            }
        } catch (Exception e) {
            logColorful("❌ ERROR CLEANING UP STALE TOPICS", BRIGHT_RED);
            logger.error("Error cleaning up stale topics", e);
        }
    }
    
    /**
     * 🧹 Clean up old metrics
     */
    private void cleanupOldMetrics() {
        try {
            int initialSize = sessionDurations.size();
            
            // 🧹 Keep only last MAX_SESSION_DURATIONS session durations
            if (sessionDurations.size() > MAX_SESSION_DURATIONS) {
                List<String> oldestSessions = sessionDurations.entrySet().stream()
                    .sorted(Map.Entry.comparingByValue())
                    .limit(sessionDurations.size() - MAX_SESSION_DURATIONS)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());
                
                oldestSessions.forEach(sessionDurations::remove);
                
                logColorful("🧹 OLD METRICS CLEANUP", CYAN);
                logColorful("📊 Session durations cleaned: " + oldestSessions.size(), CYAN);
            }
            
            // 🧹 Clean up unused message counts
            Set<String> activeSessions = sessions.keySet();
            List<String> staleCounts = sessionMessageCounts.keySet().stream()
                .filter(sessionId -> !activeSessions.contains(sessionId))
                .collect(Collectors.toList());
            
            if (!staleCounts.isEmpty()) {
                staleCounts.forEach(sessionMessageCounts::remove);
                logColorful("📊 Message counts cleaned: " + staleCounts.size(), CYAN);
            }
            
            // 🧹 Clean up zero device subscription counts
            List<String> zeroDeviceCounts = deviceSubscriptionCounts.entrySet().stream()
                .filter(entry -> entry.getValue().get() <= 0)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
            
            if (!zeroDeviceCounts.isEmpty()) {
                zeroDeviceCounts.forEach(deviceSubscriptionCounts::remove);
                logColorful("📱 Device counts cleaned: " + zeroDeviceCounts.size(), CYAN);
            }
            
        } catch (Exception e) {
            logColorful("❌ ERROR CLEANING UP OLD METRICS", BRIGHT_RED);
            logger.error("Error cleaning up old metrics", e);
        }
    }
    
    /**
     * 📊 Log cleanup statistics
     */
    private void logCleanupStatistics() {
        try {
            int activeSessions = sessions.size();
            int totalTopics = topicSubscriptions.size();
            int totalSubscribers = getTotalSubscribers();
            
            logColorful("📊 CLEANUP STATISTICS", BRIGHT_CYAN);
            logColorful("👥 Active sessions: " + activeSessions, CYAN);
            logColorful("📡 Total topics: " + totalTopics, CYAN);
            logColorful("👥 Total subscribers: " + totalSubscribers, CYAN);
            logColorful("📱 Device subscriptions: " + deviceSubscriptionCounts.size(), CYAN);
            
            logger.debug("Cleanup statistics - Active sessions: {}, Topics: {}, Subscribers: {}, Device subscriptions: {}", 
                activeSessions, totalTopics, totalSubscribers, deviceSubscriptionCounts.size());
        } catch (Exception e) {
            logger.error("Error logging cleanup statistics", e);
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
            logColorful("║ 📊 WEBSOCKET SESSION METRICS DASHBOARD", BRIGHT_CYAN);
            logColorful("║ 🕒 " + LocalDateTime.now().format(timeFormatter), BRIGHT_CYAN);
            logColorful("╠════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════", BRIGHT_CYAN);
            logColorful("║ 👥 Active Sessions: " + sessions.size(), BLUE);
            logColorful("║ 📈 Total Connections: " + totalConnections.get(), BRIGHT_GREEN);
            logColorful("║ 📉 Total Disconnections: " + totalDisconnections.get(), BRIGHT_RED);
            logColorful("║ 📊 Peak Concurrent: " + peakConcurrentSessions.get(), PURPLE);
            logColorful("║ 📡 Active Topics: " + topicSubscriptions.size(), CYAN);
            logColorful("║ 👥 Total Subscribers: " + getTotalSubscribers(), BRIGHT_BLUE);
            logColorful("║ ➕ Total Subscriptions: " + totalSubscriptions.get(), BRIGHT_GREEN);
            logColorful("║ ➖ Total Unsubscriptions: " + totalUnsubscriptions.get(), BRIGHT_YELLOW);
            logColorful("║ 📱 Device Subscriptions: " + deviceSubscriptionCounts.size(), BRIGHT_MAGENTA);
            logColorful("║ ⏱️ Avg Session Duration: " + formatDuration(getAverageSessionDuration()), YELLOW);
            logColorful("║ 📈 Total Session Time: " + formatDuration(totalSessionTime.get()), BLUE);
            
            // 🎯 Role subscription breakdown
            logColorful("║ 🎯 ROLE SUBSCRIPTION BREAKDOWN:", BRIGHT_BLUE);
            roleSubscriptionCounts.forEach((role, count) -> 
                logColorful("║   📊 " + role + ": " + count.get(), CYAN));
            
            logColorful("╚════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════", BRIGHT_CYAN);
            
            // 📊 Log to metrics logger
            metricsLogger.info("Session Metrics - Active: {}, Connections: {}, Disconnections: {}, Peak: {}, " +
                "Topics: {}, Subscribers: {}, DeviceSubscriptions: {}, AvgDuration: {}ms",
                sessions.size(), totalConnections.get(), totalDisconnections.get(), peakConcurrentSessions.get(),
                topicSubscriptions.size(), getTotalSubscribers(), deviceSubscriptionCounts.size(), 
                getAverageSessionDuration());
            
        } catch (Exception e) {
            logColorful("❌ ERROR DISPLAYING METRICS DASHBOARD", BRIGHT_RED);
            logger.error("Error displaying metrics dashboard", e);
        }
    }
    
    /**
     * 🌐 Extract connection info from headers
     */
    private void extractConnectionInfo(StompHeaderAccessor headerAccessor, EnhancedSessionInfo sessionInfo) {
        try {
            // Extract various connection details
            String userAgent = headerAccessor.getFirstNativeHeader("User-Agent");
            String origin = headerAccessor.getFirstNativeHeader("Origin");
            String host = headerAccessor.getFirstNativeHeader("Host");
            
            if (userAgent != null) {
                sessionInfo.setUserAgent(userAgent);
                logColorful("🌐 User Agent: " + userAgent, CYAN);
            }
            
            if (origin != null) {
                sessionInfo.setOrigin(origin);
                logColorful("🌐 Origin: " + origin, CYAN);
            }
            
            if (host != null) {
                sessionInfo.setHost(host);
                logColorful("🌐 Host: " + host, CYAN);
            }
            
        } catch (Exception e) {
            logger.debug("Error extracting connection info", e);
        }
    }
    
    // Utility methods
    
    /**
     * 📊 Get average session duration
     */
    private long getAverageSessionDuration() {
        if (sessionDurations.isEmpty()) return 0;
        return sessionDurations.values().stream()
            .mapToLong(Long::longValue)
            .sum() / sessionDurations.size();
    }
    
    /**
     * ⏱️ Format duration to human readable
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
    
    // Public API methods
    
    /**
     * 📊 Get the number of active sessions
     */
    public int getActiveSessionCount() {
        return sessions.size();
    }
    
    /**
     * 📊 Get the number of subscribers for a topic
     */
    public int getSubscriberCount(String topic) {
        Set<String> subscribers = topicSubscriptions.get(topic);
        return subscribers != null ? subscribers.size() : 0;
    }
    
    /**
     * 📊 Get total number of subscribers across all topics
     */
    public int getTotalSubscribers() {
        return topicSubscriptions.values().stream()
            .mapToInt(Set::size)
            .sum();
    }
    
    /**
     * 📊 Get all topics with subscriber counts
     */
    public Map<String, Integer> getTopicSubscriberCounts() {
        return topicSubscriptions.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> entry.getValue().size()
            ));
    }
    
    /**
     * 📊 Get top devices by subscription count
     */
    public Map<String, Long> getTopDevicesBySubscriptionCount() {
        return deviceSubscriptionCounts.entrySet().stream()
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
     * 📊 Get enhanced connection statistics
     */
    public EnhancedConnectionStatistics getEnhancedStatistics() {
        return new EnhancedConnectionStatistics(
            getActiveSessionCount(),
            totalConnections.get(),
            totalDisconnections.get(),
            getTotalSubscribers(),
            topicSubscriptions.size(),
            deviceSubscriptionCounts.size(),
            getAverageSessionDuration(),
            peakConcurrentSessions.get(),
            totalSubscriptions.get(),
            totalUnsubscriptions.get(),
            totalSessionTime.get(),
            new HashMap<>(roleSubscriptionCounts.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get()))),
            getTopDevicesBySubscriptionCount()
        );
    }
    
    /**
     * 🔄 Reset all metrics
     */
    public void resetMetrics() {
        totalConnections.set(0);
        totalDisconnections.set(0);
        totalSubscriptions.set(0);
        totalUnsubscriptions.set(0);
        peakConcurrentSessions.set(sessions.size());
        totalSessionTime.set(0);
        
        sessionDurations.clear();
        sessionMessageCounts.clear();
        deviceSubscriptionCounts.clear();
        
        roleSubscriptionCounts.values().forEach(counter -> counter.set(0));
        
        logColorful("🔄 WEBSOCKET METRICS RESET", BRIGHT_GREEN);
        logger.info("WebSocket session metrics reset");
    }
    
    // Enhanced data classes
    
    /**
     * 📊 Enhanced session information class
     */
    public static class EnhancedSessionInfo {
        private final String sessionId;
        private final long connectedTime;
        private final Set<String> subscriptions;
        private final Set<String> deviceSubscriptions;
        private Map<String, Object> attributes;
        private volatile long lastActivityTime;
        
        // Enhanced connection info
        private String userAgent;
        private String origin;
        private String host;
        private long messageCount;
        
        public EnhancedSessionInfo(String sessionId) {
            this.sessionId = sessionId;
            this.connectedTime = System.currentTimeMillis();
            this.lastActivityTime = connectedTime;
            this.subscriptions = ConcurrentHashMap.newKeySet();
            this.deviceSubscriptions = ConcurrentHashMap.newKeySet();
            this.attributes = new HashMap<>();
            this.messageCount = 0;
        }
        
        public void addSubscription(String topic) {
            subscriptions.add(topic);
            updateLastActivity();
        }
        
        public void removeSubscription(String topic) {
            subscriptions.remove(topic);
            updateLastActivity();
        }
        
        public void addDeviceSubscription(String deviceId) {
            deviceSubscriptions.add(deviceId);
            updateLastActivity();
        }
        
        public void removeDeviceSubscription(String deviceId) {
            deviceSubscriptions.remove(deviceId);
            updateLastActivity();
        }
        
        public void updateLastActivity() {
            this.lastActivityTime = System.currentTimeMillis();
        }
        
        public void incrementMessageCount() {
            this.messageCount++;
        }
        
        public boolean isActive(long timeoutMs) {
            return (System.currentTimeMillis() - lastActivityTime) < timeoutMs;
        }
        
        // Getters and setters
        public String getSessionId() { return sessionId; }
        public long getConnectedTime() { return connectedTime; }
        public Set<String> getSubscriptions() { return new HashSet<>(subscriptions); }
        public Set<String> getDeviceSubscriptions() { return new HashSet<>(deviceSubscriptions); }
        public Map<String, Object> getAttributes() { return attributes; }
        public void setAttributes(Map<String, Object> attributes) { this.attributes = attributes; }
        public long getLastActivityTime() { return lastActivityTime; }
        public String getUserAgent() { return userAgent; }
        public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
        public String getOrigin() { return origin; }
        public void setOrigin(String origin) { this.origin = origin; }
        public String getHost() { return host; }
        public void setHost(String host) { this.host = host; }
        public long getMessageCount() { return messageCount; }
        
        public long getConnectionDuration() {
            return System.currentTimeMillis() - connectedTime;
        }
        
        @Override
        public String toString() {
            return String.format("EnhancedSessionInfo{id=%s, duration=%dms, subscriptions=%d, devices=%d, active=%s}", 
                sessionId, getConnectionDuration(), subscriptions.size(), deviceSubscriptions.size(), 
                isActive(INACTIVE_SESSION_TIMEOUT));
        }
    }
    
    /**
     * 📊 Enhanced connection statistics class
     */
    public static class EnhancedConnectionStatistics {
        private final int activeSessions;
        private final int totalConnections;
        private final int totalDisconnections;
        private final int totalSubscribers;
        private final int totalTopics;
        private final int deviceSubscriptions;
        private final long averageSessionDuration;
        private final long peakConcurrentSessions;
        private final int totalSubscriptionEvents;
        private final int totalUnsubscriptionEvents;
        private final long totalSessionTime;
        private final Map<String, Long> roleSubscriptionCounts;
        private final Map<String, Long> topDevices;
        
        public EnhancedConnectionStatistics(int activeSessions, int totalConnections, 
                                          int totalDisconnections, int totalSubscribers, 
                                          int totalTopics, int deviceSubscriptions, 
                                          long averageSessionDuration, long peakConcurrentSessions,
                                          int totalSubscriptionEvents, int totalUnsubscriptionEvents,
                                          long totalSessionTime, Map<String, Long> roleSubscriptionCounts,
                                          Map<String, Long> topDevices) {
            this.activeSessions = activeSessions;
            this.totalConnections = totalConnections;
            this.totalDisconnections = totalDisconnections;
            this.totalSubscribers = totalSubscribers;
            this.totalTopics = totalTopics;
            this.deviceSubscriptions = deviceSubscriptions;
            this.averageSessionDuration = averageSessionDuration;
            this.peakConcurrentSessions = peakConcurrentSessions;
            this.totalSubscriptionEvents = totalSubscriptionEvents;
            this.totalUnsubscriptionEvents = totalUnsubscriptionEvents;
            this.totalSessionTime = totalSessionTime;
            this.roleSubscriptionCounts = roleSubscriptionCounts;
            this.topDevices = topDevices;
        }
        
        // Getters
        public int getActiveSessions() { return activeSessions; }
        public int getTotalConnections() { return totalConnections; }
        public int getTotalDisconnections() { return totalDisconnections; }
        public int getTotalSubscribers() { return totalSubscribers; }
        public int getTotalTopics() { return totalTopics; }
        public int getDeviceSubscriptions() { return deviceSubscriptions; }
        public long getAverageSessionDuration() { return averageSessionDuration; }
        public long getPeakConcurrentSessions() { return peakConcurrentSessions; }
        public int getTotalSubscriptionEvents() { return totalSubscriptionEvents; }
        public int getTotalUnsubscriptionEvents() { return totalUnsubscriptionEvents; }
        public long getTotalSessionTime() { return totalSessionTime; }
        public Map<String, Long> getRoleSubscriptionCounts() { return roleSubscriptionCounts; }
        public Map<String, Long> getTopDevices() { return topDevices; }
        
        public double getConnectionRate() {
            return totalDisconnections > 0 ? (double) totalConnections / totalDisconnections : 0;
        }
        
        @Override
        public String toString() {
            return String.format("EnhancedConnectionStatistics{active=%d, connections=%d, disconnections=%d, " +
                               "subscribers=%d, topics=%d, devices=%d, avgDuration=%dms, peak=%d}", 
                activeSessions, totalConnections, totalDisconnections, totalSubscribers, 
                totalTopics, deviceSubscriptions, averageSessionDuration, peakConcurrentSessions);
        }
    }
}