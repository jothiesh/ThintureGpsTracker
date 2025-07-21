package com.GpsTracker.Thinture.controller.websocket;



import java.util.Map;
import java.security.Principal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.*;
import org.springframework.web.socket.server.HandshakeHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

/**
 * Enhanced WebSocket Configuration for GPS Tracking System
 * Optimized for 5000+ concurrent devices with role-based broadcasting
 * Support for Dealer, Admin, Client, and User role filtering
 * 
 * ✅ FIXED: Added /ws endpoint for frontend compatibility
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    
    private static final Logger logger = LoggerFactory.getLogger(WebSocketConfig.class);
    
    // Configurable properties for different environments
    @Value("${websocket.heartbeat.client:25000}")
    private long clientHeartbeat;
    
    @Value("${websocket.heartbeat.server:25000}")
    private long serverHeartbeat;
    
    @Value("${websocket.scheduler.pool.size:4}")
    private int schedulerPoolSize;
    
    @Value("${websocket.message.size.limit:128}")
    private int messageSizeLimitKB;
    
    @Value("${websocket.send.buffer.size:512}")
    private int sendBufferSizeLimitKB;
    
    /**
     * Configure message broker with GPS-optimized settings
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enhanced broker configuration for GPS tracking
        config.enableSimpleBroker("/topic", "/queue", "/user")
              .setTaskScheduler(gpsHeartbeatScheduler())
              .setHeartbeatValue(new long[] {serverHeartbeat, clientHeartbeat});

        // Application prefixes for GPS commands
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
        
        logger.info("🚀 GPS WebSocket Broker Configured:");
        logger.info("   📡 Topics: /topic (locations), /queue (private), /user (user-specific)");
        logger.info("   💓 Heartbeat: server={}ms, client={}ms", serverHeartbeat, clientHeartbeat);
        logger.info("   🔧 Scheduler pool: {} threads", schedulerPoolSize);
    }

    /**
     * ✅ FIXED: Register optimized STOMP endpoints with /ws added
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // ✅ PRIMARY FRONTEND ENDPOINT - Your map connects to this
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*") // Configure specific domains in production
                .setHandshakeHandler(gpsHandshakeHandler())
                .addInterceptors(new HttpSessionHandshakeInterceptor())
                .withSockJS()
                .setStreamBytesLimit(messageSizeLimitKB * 1024)
                .setHttpMessageCacheSize(1000)
                .setDisconnectDelay(30000);
        
        // Primary GPS tracking endpoint with SockJS
        registry.addEndpoint("/ws-gps")
                .setAllowedOriginPatterns("*") // Configure specific domains in production
                .setHandshakeHandler(gpsHandshakeHandler())
                .addInterceptors(new HttpSessionHandshakeInterceptor())
                .withSockJS()
                .setStreamBytesLimit(messageSizeLimitKB * 1024)
                .setHttpMessageCacheSize(1000)
                .setDisconnectDelay(30000)
                .setClientLibraryUrl("https://cdn.jsdelivr.net/npm/sockjs-client@1.6.1/dist/sockjs.min.js");

        // Native WebSocket endpoint for mobile apps and direct connections
        registry.addEndpoint("/ws-gps-native")
                .setAllowedOriginPatterns("*") // Configure specific domains in production
                .setHandshakeHandler(gpsHandshakeHandler())
                .addInterceptors(new HttpSessionHandshakeInterceptor());
        
        // Admin monitoring endpoint
        registry.addEndpoint("/ws-admin")
                .setAllowedOriginPatterns("*") // Restrict to admin domains only
                .setHandshakeHandler(gpsHandshakeHandler())
                .addInterceptors(new HttpSessionHandshakeInterceptor())
                .withSockJS();
        
        logger.info("🔌 GPS WebSocket Endpoints Registered:");
        logger.info("   🌐 /ws (SockJS) - Frontend GPS map connection");          // ✅ NEW
        logger.info("   🌐 /ws-gps (SockJS) - Main GPS tracking");
        logger.info("   📱 /ws-gps-native (Native) - Mobile/Direct");
        logger.info("   ⚙️ /ws-admin (SockJS) - Admin monitoring");
    }

    /**
     * Configure WebSocket transport with GPS-optimized limits
     */
    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        registration.setMessageSizeLimit(messageSizeLimitKB * 1024)      // 128KB default
                   .setSendBufferSizeLimit(sendBufferSizeLimitKB * 1024) // 512KB default
                   .setSendTimeLimit(20000)                              // 20 seconds
                   .setTimeToFirstMessage(60000);                        // 60 seconds connection timeout
        
        logger.info("🔧 GPS WebSocket Transport Configured:");
        logger.info("   📦 Message size: {}KB", messageSizeLimitKB);
        logger.info("   🗂️ Send buffer: {}KB", sendBufferSizeLimitKB);
        logger.info("   ⏱️ Timeouts: send=20s, connection=60s");
    }

    /**
     * Enhanced heartbeat scheduler for high-volume GPS data
     */
    @Bean
    public TaskScheduler gpsHeartbeatScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(schedulerPoolSize);
        scheduler.setThreadNamePrefix("gps-heartbeat-");
        scheduler.setAwaitTerminationSeconds(60);
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.initialize();
        
        logger.info("⚙️ GPS Heartbeat Scheduler: {} threads", schedulerPoolSize);
        return scheduler;
    }
    
    /**
     * WebSocket executor for async GPS processing
     */
    @Bean(name = "gpsWebSocketExecutor")
    public ThreadPoolTaskExecutor gpsWebSocketExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);               // Base threads
        executor.setMaxPoolSize(50);                // Peak load threads
        executor.setQueueCapacity(2000);            // Large queue for GPS updates
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("gps-ws-exec-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        
        logger.info("🏃 GPS WebSocket Executor: core=10, max=50, queue=2000");
        return executor;
    }

    /**
     * Enhanced handshake handler with user role determination
     */
    @Bean
    public HandshakeHandler gpsHandshakeHandler() {
        return new DefaultHandshakeHandler() {
            @Override
            protected Principal determineUser(ServerHttpRequest request, 
                                            WebSocketHandler wsHandler, 
                                            Map<String, Object> attributes) {
                
                // Try to get user info from headers
                String sessionId = request.getHeaders().getFirst("X-Session-Id");
                String userId = request.getHeaders().getFirst("X-User-Id");
                String userRole = request.getHeaders().getFirst("X-User-Role");
                String deviceId = request.getHeaders().getFirst("X-Device-Id");
                
                // Create GPS-specific principal
                if (sessionId != null || userId != null) {
                    return new GpsPrincipal(
                        sessionId != null ? sessionId : userId,
                        userId,
                        userRole,
                        deviceId
                    );
                }
                
                // Fallback to default behavior
                return super.determineUser(request, wsHandler, attributes);
            }
        };
    }

    /**
     * Enhanced channel interceptor with GPS-specific authentication and logging
     */
    @Bean
    public ChannelInterceptor gpsChannelInterceptor() {
        return new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = 
                    MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                
                if (accessor != null) {
                    StompCommand command = accessor.getCommand();
                    
                    switch (command) {
                        case CONNECT:
                            handleConnect(accessor);
                            break;
                        case SUBSCRIBE:
                            handleSubscribe(accessor);
                            break;
                        case DISCONNECT:
                            handleDisconnect(accessor);
                            break;
                        default:
                            break;
                    }
                }
                
                return message;
            }
            
            private void handleConnect(StompHeaderAccessor accessor) {
                String authToken = accessor.getFirstNativeHeader("X-Auth-Token");
                String userRole = accessor.getFirstNativeHeader("X-User-Role");
                String userId = accessor.getFirstNativeHeader("X-User-Id");
                
                // Add your authentication logic here
                if (authToken != null) {
                    // Validate token and set user attributes
                    logger.info("🔐 GPS WebSocket connection: user={}, role={}", userId, userRole);
                } else {
                    logger.info("🔐 GPS WebSocket connection: session-based authentication");
                }
            }
            
            private void handleSubscribe(StompHeaderAccessor accessor) {
                String destination = accessor.getDestination();
                String sessionId = accessor.getSessionId();
                Principal user = accessor.getUser();
                
                if (destination != null && destination.startsWith("/topic/location-updates/")) {
                    logger.info("📡 GPS Subscription: session={}, user={}, topic={}", 
                        sessionId, user != null ? user.getName() : "anonymous", destination);
                }
            }
            
            private void handleDisconnect(StompHeaderAccessor accessor) {
                String sessionId = accessor.getSessionId();
                Principal user = accessor.getUser();
                
                logger.info("🔌 GPS WebSocket disconnected: session={}, user={}", 
                    sessionId, user != null ? user.getName() : "anonymous");
            }
        };
    }
    
    /**
     * GPS-specific Principal implementation
     */
    public static class GpsPrincipal implements Principal {
        private final String name;
        private final String userId;
        private final String userRole;
        private final String deviceId;
        
        public GpsPrincipal(String name, String userId, String userRole, String deviceId) {
            this.name = name;
            this.userId = userId;
            this.userRole = userRole;
            this.deviceId = deviceId;
        }
        
        @Override
        public String getName() {
            return name;
        }
        
        public String getUserId() { return userId; }
        public String getUserRole() { return userRole; }
        public String getDeviceId() { return deviceId; }
        
        @Override
        public String toString() {
            return String.format("GpsPrincipal{name='%s', userId='%s', role='%s', deviceId='%s'}", 
                name, userId, userRole, deviceId);
        }
    }
    
    /**
     * Topic constants for GPS tracking
     */
    public static final class GpsTopics {
        // Role-based location update topics
        public static final String LOCATION_DEALER = "/topic/location-updates/dealer/";
        public static final String LOCATION_ADMIN = "/topic/location-updates/admin/";
        public static final String LOCATION_CLIENT = "/topic/location-updates/client/";
        public static final String LOCATION_USER = "/topic/location-updates/user/";
        
        // General topics
        public static final String LOCATION_ALL = "/topic/location-updates";
        public static final String DEVICE_PREFIX = "/topic/device/";
        public static final String ALERTS = "/topic/alerts";
        public static final String STATS = "/topic/stats";
        
        // Application destinations
        public static final String APP_SUBSCRIBE = "/app/subscribe";
        public static final String APP_LOCATIONS = "/app/locations";
        public static final String APP_DEVICE = "/app/device";
        
        private GpsTopics() {}
    }
}