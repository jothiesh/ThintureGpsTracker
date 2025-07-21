package com.GpsTracker.Thinture.controller.websocket;

import com.GpsTracker.Thinture.controller.websocket.WebSocketConfig.GpsPrincipal;
import com.GpsTracker.Thinture.controller.websocket.WebSocketConfig.GpsTopics;
import com.GpsTracker.Thinture.dto.LocationUpdate;
import com.GpsTracker.Thinture.service.VehicleService;
import com.GpsTracker.Thinture.service.UserTypeFilterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Controller;
import org.springframework.security.access.AccessDeniedException;

import java.security.Principal;
import java.util.List;

/**
 * Enhanced GPS WebSocket Controller with full role hierarchy support
 * Supports: SUPERADMIN, ADMIN, DEALER, CLIENT, USER
 */
@Controller
public class GpsWebSocketController {
    
    private static final Logger logger = LoggerFactory.getLogger(GpsWebSocketController.class);
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    @Autowired
    private VehicleService vehicleService;
    
    @Autowired
    private UserTypeFilterService userTypeFilterService;
    
    // ===============================
    // SUBSCRIPTION MAPPINGS
    // ===============================
    
    /**
     * 🔐 DEALER: Handle dealer subscription with role validation
     */
    @SubscribeMapping(GpsTopics.LOCATION_DEALER + "{dealerId}")
    public void subscribeToDealerLocations(@DestinationVariable String dealerId,
                                         StompHeaderAccessor accessor) {
        
        GpsPrincipal gpsUser = (GpsPrincipal) accessor.getUser();
        
        if (gpsUser == null) {
            throw new AccessDeniedException("No user authentication");
        }
        
        // 🔒 Role-based access control
        if (!"DEALER".equals(gpsUser.getUserRole())) {
            logger.warn("🚫 Unauthorized dealer access attempt: user={}, role={}", 
                gpsUser.getUserId(), gpsUser.getUserRole());
            throw new AccessDeniedException("Not authorized for dealer data");
        }
        
        // 🔒 User can only access their own dealer data
        if (!dealerId.equals(gpsUser.getUserId())) {
            logger.warn("🚫 Cross-dealer access attempt: user={} trying to access dealer={}", 
                gpsUser.getUserId(), dealerId);
            throw new AccessDeniedException("Can only access own dealer data");
        }
        
        logger.info("✅ Dealer subscription approved: user={}, dealerId={}", 
            gpsUser.getUserId(), dealerId);
    }
    
    /**
     * 🔐 ADMIN: Handle admin subscription with role validation
     */
    @SubscribeMapping(GpsTopics.LOCATION_ADMIN + "{adminId}")
    public void subscribeToAdminLocations(@DestinationVariable String adminId,
                                        StompHeaderAccessor accessor) {
        
        GpsPrincipal gpsUser = (GpsPrincipal) accessor.getUser();
        
        // ✅ Only ADMIN or SUPERADMIN can subscribe
        if (gpsUser == null || 
            (!("ADMIN".equals(gpsUser.getUserRole()) || "SUPERADMIN".equals(gpsUser.getUserRole())))) {
            throw new AccessDeniedException("Admin access required");
        }
        
        logger.info("✅ Admin subscription approved: user={}, adminId={}", 
            gpsUser.getUserId(), adminId);
    }
    
    /**
     * 🔐 CLIENT: Handle client subscription with role validation
     */
    @SubscribeMapping(GpsTopics.LOCATION_CLIENT + "{clientId}")
    public void subscribeToClientLocations(@DestinationVariable String clientId,
                                         StompHeaderAccessor accessor) {
        
        GpsPrincipal gpsUser = (GpsPrincipal) accessor.getUser();
        
        if (gpsUser == null) {
            throw new AccessDeniedException("No user authentication");
        }
        
        // 🔒 Role-based access control - CLIENT can only access own data
        if ("CLIENT".equals(gpsUser.getUserRole())) {
            if (!clientId.equals(gpsUser.getUserId())) {
                logger.warn("🚫 Cross-client access attempt: user={} trying to access client={}", 
                    gpsUser.getUserId(), clientId);
                throw new AccessDeniedException("Can only access own client data");
            }
        } 
        // 🔒 DEALER can access their clients
        else if ("DEALER".equals(gpsUser.getUserRole())) {
            if (!vehicleService.dealerHasAccessToClient(gpsUser.getUserId(), clientId)) {
                throw new AccessDeniedException("Dealer cannot access this client");
            }
        }
        // 🔒 ADMIN/SUPERADMIN can access all
        else if (!("ADMIN".equals(gpsUser.getUserRole()) || "SUPERADMIN".equals(gpsUser.getUserRole()))) {
            throw new AccessDeniedException("Not authorized for client data");
        }
        
        logger.info("✅ Client subscription approved: user={}, role={}, clientId={}", 
            gpsUser.getUserId(), gpsUser.getUserRole(), clientId);
    }
    
    /**
     * 🔐 USER: Handle user subscription with role validation
     */
    @SubscribeMapping(GpsTopics.LOCATION_USER + "{userId}")
    public void subscribeToUserLocations(@DestinationVariable String userId,
                                       StompHeaderAccessor accessor) {
        
        GpsPrincipal gpsUser = (GpsPrincipal) accessor.getUser();
        
        if (gpsUser == null) {
            throw new AccessDeniedException("No user authentication");
        }
        
        // 🔒 Role-based access control
        if ("USER".equals(gpsUser.getUserRole())) {
            // USER can only access own data
            if (!userId.equals(gpsUser.getUserId())) {
                logger.warn("🚫 Cross-user access attempt: user={} trying to access user={}", 
                    gpsUser.getUserId(), userId);
                throw new AccessDeniedException("Can only access own user data");
            }
        }
        // 🔒 CLIENT can access their users
        else if ("CLIENT".equals(gpsUser.getUserRole())) {
            if (!vehicleService.clientHasAccessToUser(gpsUser.getUserId(), userId)) {
                throw new AccessDeniedException("Client cannot access this user");
            }
        }
        // 🔒 Higher roles can access all
        else if (!("DEALER".equals(gpsUser.getUserRole()) || 
                   "ADMIN".equals(gpsUser.getUserRole()) || 
                   "SUPERADMIN".equals(gpsUser.getUserRole()))) {
            throw new AccessDeniedException("Not authorized for user data");
        }
        
        logger.info("✅ User subscription approved: user={}, role={}, userId={}", 
            gpsUser.getUserId(), gpsUser.getUserRole(), userId);
    }
    
    // ===============================
    // MESSAGE MAPPINGS - GET LOCATIONS
    // ===============================
    
    /**
     * 📡 Get dealer vehicle locations
     */
    @MessageMapping("/locations/dealer/{dealerId}")
    public void getDealerVehicles(@DestinationVariable String dealerId,
                                StompHeaderAccessor accessor) {
        
        GpsPrincipal gpsUser = (GpsPrincipal) accessor.getUser();
        
        // Security validation
        validateDealerAccess(gpsUser, dealerId);
        
        // Get dealer vehicles
        List<LocationUpdate> locations = vehicleService.getLocationsByDealerId(dealerId);
        
        // ✅ Send using GpsTopics constants
        String topic = GpsTopics.LOCATION_DEALER + dealerId;
        messagingTemplate.convertAndSend(topic, locations);
        
        logger.info("📡 Sent {} locations to dealer topic: {}", locations.size(), topic);
    }
    
    /**
     * 📡 Get admin vehicle locations
     */
    @MessageMapping("/locations/admin/{adminId}")
    public void getAdminVehicles(@DestinationVariable String adminId,
                               StompHeaderAccessor accessor) {
        
        GpsPrincipal gpsUser = (GpsPrincipal) accessor.getUser();
        
        // Security validation
        validateAdminAccess(gpsUser, adminId);
        
        // Get admin vehicles
        List<LocationUpdate> locations = vehicleService.getLocationsByAdminId(adminId);
        
        // ✅ Send using GpsTopics constants
        String topic = GpsTopics.LOCATION_ADMIN + adminId;
        messagingTemplate.convertAndSend(topic, locations);
        
        logger.info("📡 Sent {} locations to admin topic: {}", locations.size(), topic);
    }
    
    /**
     * 📡 Get client vehicle locations
     */
    @MessageMapping("/locations/client/{clientId}")
    public void getClientVehicles(@DestinationVariable String clientId,
                                StompHeaderAccessor accessor) {
        
        GpsPrincipal gpsUser = (GpsPrincipal) accessor.getUser();
        
        // Security validation
        validateClientAccess(gpsUser, clientId);
        
        // Get client vehicles
        List<LocationUpdate> locations = vehicleService.getLocationsByClientId(clientId);
        
        // ✅ Send using GpsTopics constants
        String topic = GpsTopics.LOCATION_CLIENT + clientId;
        messagingTemplate.convertAndSend(topic, locations);
        
        logger.info("📡 Sent {} locations to client topic: {}", locations.size(), topic);
    }
    
    /**
     * 📡 Get user vehicle locations
     */
    @MessageMapping("/locations/user/{userId}")
    public void getUserVehicles(@DestinationVariable String userId,
                              StompHeaderAccessor accessor) {
        
        GpsPrincipal gpsUser = (GpsPrincipal) accessor.getUser();
        
        // Security validation
        validateUserAccess(gpsUser, userId);
        
        // Get user vehicles
        List<LocationUpdate> locations = vehicleService.getLocationsByUserId(userId);
        
        // ✅ Send using GpsTopics constants
        String topic = GpsTopics.LOCATION_USER + userId;
        messagingTemplate.convertAndSend(topic, locations);
        
        logger.info("📡 Sent {} locations to user topic: {}", locations.size(), topic);
    }
    
    /**
     * 📱 DEVICE: Handle device-specific location requests
     */
    @MessageMapping("/device/{deviceId}/location")
    public void getDeviceLocation(@DestinationVariable String deviceId,
                                StompHeaderAccessor accessor) {
        
        GpsPrincipal gpsUser = (GpsPrincipal) accessor.getUser();
        
        // Check if user has access to this device
        if (!vehicleService.userHasAccessToDevice(gpsUser.getUserId(), deviceId)) {
            throw new AccessDeniedException("No access to device: " + deviceId);
        }
        
        LocationUpdate location = vehicleService.getDeviceLocation(deviceId);
        
        // ✅ Send to device-specific topic using constants
        String topic = GpsTopics.DEVICE_PREFIX + deviceId;
        messagingTemplate.convertAndSend(topic, location);
        
        logger.info("📱 Sent device location: deviceId={}, topic={}", deviceId, topic);
    }
    
    // ===============================
    // ALERT BROADCASTING
    // ===============================
    
    /**
     * 🚨 ALERTS: Broadcast alert to appropriate role-based topics
     */
    public void broadcastAlert(String alertType, String deviceId, String message) {
        
        // Get vehicle to determine which roles to notify
        var vehicle = vehicleService.getVehicleByDeviceID(deviceId);
        
        if (vehicle.isPresent()) {
            var v = vehicle.get();
            
            // ✅ Use GpsTopics constants for clean alert broadcasting
            Alert alert = new Alert(alertType, deviceId, message, System.currentTimeMillis());
            
            // Send to all role-based topics
            if (v.getDealer_id() != null) {
                messagingTemplate.convertAndSend(
                    GpsTopics.LOCATION_DEALER + v.getDealer_id(), alert);
            }
            
            if (v.getAdmin_id() != null) {
                messagingTemplate.convertAndSend(
                    GpsTopics.LOCATION_ADMIN + v.getAdmin_id(), alert);
            }
            
            // ✅ NEW: Send to CLIENT and USER
            if (v.getClient_id() != null) {
                messagingTemplate.convertAndSend(
                    GpsTopics.LOCATION_CLIENT + v.getClient_id(), alert);
            }
            
            if (v.getUser_id() != null) {
                messagingTemplate.convertAndSend(
                    GpsTopics.LOCATION_USER + v.getUser_id(), alert);
            }
            
            // Also send to general alerts topic
            messagingTemplate.convertAndSend(GpsTopics.ALERTS, alert);
            
            logger.info("🚨 Alert broadcasted: type={}, device={}", alertType, deviceId);
        }
    }
    
    // ===============================
    // STATISTICS
    // ===============================
    
    /**
     * 📊 STATS: Send statistics to monitoring topic
     */
    @MessageMapping("/stats/request")
    @SendTo(GpsTopics.STATS)
    public SystemStats getSystemStats(StompHeaderAccessor accessor) {
        
        GpsPrincipal gpsUser = (GpsPrincipal) accessor.getUser();
        
        // ✅ Enhanced role-based stats access
        SystemStats stats;
        
        switch (gpsUser.getUserRole()) {
            case "SUPERADMIN":
            case "ADMIN":
                // Full system stats for admins
                stats = new SystemStats(
                    vehicleService.getTotalVehicles(),
                    vehicleService.getActiveDevices(),
                    vehicleService.getTotalAlerts()
                );
                break;
                
            case "DEALER":
                // Limited stats for dealers
                stats = new SystemStats(
                    vehicleService.getDealerVehicleCount(gpsUser.getUserId()),
                    vehicleService.getDealerActiveDevices(gpsUser.getUserId()),
                    vehicleService.getDealerAlerts(gpsUser.getUserId())
                );
                break;
                
            case "CLIENT":
                // Client-specific stats
                stats = new SystemStats(
                    vehicleService.getClientVehicleCount(gpsUser.getUserId()),
                    vehicleService.getClientActiveDevices(gpsUser.getUserId()),
                    vehicleService.getClientAlerts(gpsUser.getUserId())
                );
                break;
                
            case "USER":
                // User-specific stats
                stats = new SystemStats(
                    vehicleService.getUserVehicleCount(gpsUser.getUserId()),
                    vehicleService.getUserActiveDevices(gpsUser.getUserId()),
                    vehicleService.getUserAlerts(gpsUser.getUserId())
                );
                break;
                
            default:
                throw new AccessDeniedException("No access to system stats");
        }
        
        logger.info("📊 Stats requested by: user={}, role={}", 
            gpsUser.getUserId(), gpsUser.getUserRole());
        
        return stats;
    }
    
    // ===============================
    // VALIDATION HELPERS
    // ===============================
    
    /**
     * Helper method for dealer access validation
     */
    private void validateDealerAccess(GpsPrincipal gpsUser, String dealerId) {
        if (gpsUser == null) {
            throw new AccessDeniedException("No authentication");
        }
        
        if ("DEALER".equals(gpsUser.getUserRole())) {
            if (!dealerId.equals(gpsUser.getUserId())) {
                throw new AccessDeniedException("Can only access own dealer data");
            }
        } else if (!("ADMIN".equals(gpsUser.getUserRole()) || "SUPERADMIN".equals(gpsUser.getUserRole()))) {
            throw new AccessDeniedException("Dealer, Admin or SuperAdmin access required");
        }
    }
    
    /**
     * Helper method for admin access validation
     */
    private void validateAdminAccess(GpsPrincipal gpsUser, String adminId) {
        if (gpsUser == null) {
            throw new AccessDeniedException("No authentication");
        }
        
        if ("ADMIN".equals(gpsUser.getUserRole())) {
            if (!adminId.equals(gpsUser.getUserId())) {
                throw new AccessDeniedException("Can only access own admin data");
            }
        } else if (!"SUPERADMIN".equals(gpsUser.getUserRole())) {
            throw new AccessDeniedException("Admin or SuperAdmin access required");
        }
    }
    
    /**
     * Helper method for client access validation
     */
    private void validateClientAccess(GpsPrincipal gpsUser, String clientId) {
        if (gpsUser == null) {
            throw new AccessDeniedException("No authentication");
        }
        
        switch (gpsUser.getUserRole()) {
            case "CLIENT":
                if (!clientId.equals(gpsUser.getUserId())) {
                    throw new AccessDeniedException("Can only access own client data");
                }
                break;
            case "DEALER":
                if (!vehicleService.dealerHasAccessToClient(gpsUser.getUserId(), clientId)) {
                    throw new AccessDeniedException("Dealer cannot access this client");
                }
                break;
            case "ADMIN":
            case "SUPERADMIN":
                // Full access for admins
                break;
            default:
                throw new AccessDeniedException("Client, Dealer, Admin or SuperAdmin access required");
        }
    }
    
    /**
     * Helper method for user access validation
     */
    private void validateUserAccess(GpsPrincipal gpsUser, String userId) {
        if (gpsUser == null) {
            throw new AccessDeniedException("No authentication");
        }
        
        switch (gpsUser.getUserRole()) {
            case "USER":
                if (!userId.equals(gpsUser.getUserId())) {
                    throw new AccessDeniedException("Can only access own user data");
                }
                break;
            case "CLIENT":
                if (!vehicleService.clientHasAccessToUser(gpsUser.getUserId(), userId)) {
                    throw new AccessDeniedException("Client cannot access this user");
                }
                break;
            case "DEALER":
            case "ADMIN":
            case "SUPERADMIN":
                // Full access for higher roles
                break;
            default:
                throw new AccessDeniedException("User, Client, Dealer, Admin or SuperAdmin access required");
        }
    }
    
    // ===============================
    // DTOs
    // ===============================
    
    public static class Alert {
        private String type;
        private String deviceId;
        private String message;
        private long timestamp;
        
        public Alert(String type, String deviceId, String message, long timestamp) {
            this.type = type;
            this.deviceId = deviceId;
            this.message = message;
            this.timestamp = timestamp;
        }
        
        // Getters
        public String getType() { return type; }
        public String getDeviceId() { return deviceId; }
        public String getMessage() { return message; }
        public long getTimestamp() { return timestamp; }
    }
    
    public static class SystemStats {
        private int totalVehicles;
        private int activeDevices;
        private int totalAlerts;
        
        public SystemStats(int totalVehicles, int activeDevices, int totalAlerts) {
            this.totalVehicles = totalVehicles;
            this.activeDevices = activeDevices;
            this.totalAlerts = totalAlerts;
        }
        
        // Getters
        public int getTotalVehicles() { return totalVehicles; }
        public int getActiveDevices() { return activeDevices; }
        public int getTotalAlerts() { return totalAlerts; }
    }
}