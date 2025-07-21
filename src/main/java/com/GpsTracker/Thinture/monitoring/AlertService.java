package com.GpsTracker.Thinture.monitoring;

import com.GpsTracker.Thinture.service.websocket.LocationBroadcastService;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class AlertService {
    
    private static final Logger logger = LoggerFactory.getLogger(AlertService.class);
    
    @Autowired(required = false)
    private JavaMailSender mailSender;
    
    @Autowired(required = false)
    private LocationBroadcastService broadcastService;
    
    @Value("${twilio.account.sid:}")
    private String twilioAccountSid;
    
    @Value("${twilio.auth.token:}")
    private String twilioAuthToken;
    
    @Value("${twilio.phone.number:}")
    private String twilioPhoneNumber;
    
    @Value("${alert.email.to:}")
    private String alertEmailTo;
    
    @Value("${alert.email.from:noreply@gpstracker.com}")
    private String alertEmailFrom;
    
    @Value("${alert.sms.to:}")
    private String alertSmsTo;
    
    @Value("${alert.rate.limit.per.hour:10}")
    private int alertRateLimitPerHour;
    
    private final Map<String, AlertHistory> alertHistories = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> alertCounts = new ConcurrentHashMap<>();
    private final Set<String> mutedAlerts = ConcurrentHashMap.newKeySet();
    
    private boolean twilioEnabled = false;
    private boolean emailEnabled = false;
    
    @PostConstruct
    public void init() {
        // Initialize Twilio if credentials are provided
        if (!twilioAccountSid.isEmpty() && !twilioAuthToken.isEmpty()) {
            try {
                Twilio.init(twilioAccountSid, twilioAuthToken);
                twilioEnabled = true;
                logger.info("Twilio SMS alerts enabled");
            } catch (Exception e) {
                logger.error("Failed to initialize Twilio", e);
            }
        }
        
        // Check if email is configured
        emailEnabled = mailSender != null && !alertEmailTo.isEmpty();
        if (emailEnabled) {
            logger.info("Email alerts enabled");
        }
    }
    
    @Async
    public void sendCriticalAlert(String alertType, String message) {
        sendAlert(AlertLevel.CRITICAL, alertType, message, null);
    }
    
    @Async
    public void sendCriticalAlert(String alertType, String message, Map<String, Object> details) {
        sendAlert(AlertLevel.CRITICAL, alertType, message, details);
    }
    
    @Async
    public void sendWarningAlert(String alertType, String message) {
        sendAlert(AlertLevel.WARNING, alertType, message, null);
    }
    
    @Async
    public void sendInfoAlert(String alertType, String message) {
        sendAlert(AlertLevel.INFO, alertType, message, null);
    }
    
    private void sendAlert(AlertLevel level, String alertType, String message, Map<String, Object> details) {
        try {
            // Check if alert is muted
            if (mutedAlerts.contains(alertType)) {
                logger.debug("Alert {} is muted", alertType);
                return;
            }
            
            // Check rate limiting
            if (!checkRateLimit(alertType)) {
                logger.warn("Alert {} exceeded rate limit", alertType);
                return;
            }
            
            // Create alert object
            Alert alert = new Alert(level, alertType, message, details);
            
            // Store in history
            storeAlertHistory(alert);
            
            // Log the alert
            logAlert(alert);
            
            // Send notifications based on level
            if (level == AlertLevel.CRITICAL) {
                sendSmsNotification(alert);
                sendEmailNotification(alert);
                broadcastWebSocketAlert(alert);
            } else if (level == AlertLevel.WARNING) {
                sendEmailNotification(alert);
                broadcastWebSocketAlert(alert);
            } else {
                broadcastWebSocketAlert(alert);
            }
            
        } catch (Exception e) {
            logger.error("Failed to send alert", e);
        }
    }
    
    private boolean checkRateLimit(String alertType) {
        AtomicLong count = alertCounts.computeIfAbsent(alertType, k -> new AtomicLong(0));
        long currentCount = count.incrementAndGet();
        
        // Reset count every hour
        if (currentCount == 1) {
            scheduleCountReset(alertType);
        }
        
        return currentCount <= alertRateLimitPerHour;
    }
    
    private void scheduleCountReset(String alertType) {
        Timer timer = new Timer(true);
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                alertCounts.get(alertType).set(0);
            }
        }, 3600000); // 1 hour
    }
    
    private void storeAlertHistory(Alert alert) {
        AlertHistory history = alertHistories.computeIfAbsent(
            alert.getAlertType(), 
            k -> new AlertHistory(k)
        );
        history.addAlert(alert);
    }
    
    private void logAlert(Alert alert) {
        switch (alert.getLevel()) {
            case CRITICAL:
                logger.error("CRITICAL ALERT [{}]: {}", alert.getAlertType(), alert.getMessage());
                break;
            case WARNING:
                logger.warn("WARNING ALERT [{}]: {}", alert.getAlertType(), alert.getMessage());
                break;
            case INFO:
                logger.info("INFO ALERT [{}]: {}", alert.getAlertType(), alert.getMessage());
                break;
        }
    }
    
    private void sendSmsNotification(Alert alert) {
        if (!twilioEnabled || alertSmsTo.isEmpty()) {
            return;
        }
        
        try {
            String smsBody = String.format("[%s] %s: %s", 
                alert.getLevel(), 
                alert.getAlertType(), 
                alert.getMessage()
            );
            
            Message message = Message.creator(
                new PhoneNumber(alertSmsTo),
                new PhoneNumber(twilioPhoneNumber),
                smsBody
            ).create();
            
            logger.info("SMS alert sent to {} - SID: {}", alertSmsTo, message.getSid());
        } catch (Exception e) {
            logger.error("Failed to send SMS alert", e);
        }
    }
    
    private void sendEmailNotification(Alert alert) {
        if (!emailEnabled) {
            return;
        }
        
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(alertEmailTo.split(","));
            message.setFrom(alertEmailFrom);
            message.setSubject(String.format("GPS Tracker %s Alert: %s", 
                alert.getLevel(), alert.getAlertType()));
            
            StringBuilder body = new StringBuilder();
            body.append("Alert Level: ").append(alert.getLevel()).append("\n");
            body.append("Alert Type: ").append(alert.getAlertType()).append("\n");
            body.append("Message: ").append(alert.getMessage()).append("\n");
            body.append("Timestamp: ").append(alert.getTimestamp()).append("\n");
            
            if (alert.getDetails() != null && !alert.getDetails().isEmpty()) {
                body.append("\nDetails:\n");
                alert.getDetails().forEach((key, value) -> 
                    body.append("  ").append(key).append(": ").append(value).append("\n")
                );
            }
            
            message.setText(body.toString());
            
            mailSender.send(message);
            logger.info("Email alert sent to {}", alertEmailTo);
        } catch (Exception e) {
            logger.error("Failed to send email alert", e);
        }
    }
    
    private void broadcastWebSocketAlert(Alert alert) {
        if (broadcastService == null) {
            return;
        }
        
        try {
            Map<String, Object> alertData = new HashMap<>();
            alertData.put("level", alert.getLevel().toString());
            alertData.put("type", alert.getAlertType());
            alertData.put("message", alert.getMessage());
            alertData.put("timestamp", alert.getTimestamp());
            alertData.put("details", alert.getDetails());
            
            // Broadcast through WebSocket
            // This would need to be implemented in LocationBroadcastService
            logger.debug("WebSocket alert broadcast: {}", alertData);
        } catch (Exception e) {
            logger.error("Failed to broadcast WebSocket alert", e);
        }
    }
    
    public void muteAlert(String alertType) {
        mutedAlerts.add(alertType);
        logger.info("Alert {} muted", alertType);
    }
    
    public void unmuteAlert(String alertType) {
        mutedAlerts.remove(alertType);
        logger.info("Alert {} unmuted", alertType);
    }
    
    public Set<String> getMutedAlerts() {
        return new HashSet<>(mutedAlerts);
    }
    
    public Map<String, AlertHistory> getAlertHistories() {
        return new HashMap<>(alertHistories);
    }
    
    public AlertHistory getAlertHistory(String alertType) {
        return alertHistories.get(alertType);
    }
    
    public void clearAlertHistory(String alertType) {
        alertHistories.remove(alertType);
    }
    
    public enum AlertLevel {
        INFO, WARNING, CRITICAL
    }
    
    public static class Alert {
        private final AlertLevel level;
        private final String alertType;
        private final String message;
        private final Map<String, Object> details;
        private final LocalDateTime timestamp;
        
        public Alert(AlertLevel level, String alertType, String message, Map<String, Object> details) {
            this.level = level;
            this.alertType = alertType;
            this.message = message;
            this.details = details != null ? new HashMap<>(details) : new HashMap<>();
            this.timestamp = LocalDateTime.now();
        }
        
        public AlertLevel getLevel() { return level; }
        public String getAlertType() { return alertType; }
        public String getMessage() { return message; }
        public Map<String, Object> getDetails() { return details; }
        public LocalDateTime getTimestamp() { return timestamp; }
        
        public String getFormattedTimestamp() {
            return timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }
    }
    
    public static class AlertHistory {
        private final String alertType;
        private final List<Alert> alerts = new ArrayList<>();
        private final int maxHistorySize = 100;
        
        public AlertHistory(String alertType) {
            this.alertType = alertType;
        }
        
        public synchronized void addAlert(Alert alert) {
            alerts.add(alert);
            if (alerts.size() > maxHistorySize) {
                alerts.remove(0);
            }
        }
        
        public String getAlertType() { return alertType; }
        public List<Alert> getAlerts() { return new ArrayList<>(alerts); }
        
        public List<Alert> getRecentAlerts(int count) {
            int size = alerts.size();
            int fromIndex = Math.max(0, size - count);
            return new ArrayList<>(alerts.subList(fromIndex, size));
        }
        
        public long getAlertCount(AlertLevel level) {
            return alerts.stream()
                .filter(alert -> alert.getLevel() == level)
                .count();
        }
    }
}