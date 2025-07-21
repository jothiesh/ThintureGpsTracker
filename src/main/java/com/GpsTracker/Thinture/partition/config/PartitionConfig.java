package com.GpsTracker.Thinture.partition.config;

//================================================================================================
//PartitionConfig.java - ENHANCED MYSQL PARTITION CONFIGURATION
//================================================================================================

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import javax.annotation.PostConstruct;


import java.util.Arrays;
import java.util.List;

/**
 * ⚙️ Enhanced MySQL Partition Configuration
 * 🚀 Centralized configuration with validation and MySQL-specific optimizations
 */
@Configuration
@ConfigurationProperties(prefix = "gps.partition")
@Validated
public class PartitionConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(PartitionConfig.class);
    private static final Logger configLogger = LoggerFactory.getLogger("CONFIG." + PartitionConfig.class.getName());
    
    // ═══════════════════════════════════════════════════════════════════════════════════
    // SIZE THRESHOLD CONFIGURATION (with validation)
    // ═══════════════════════════════════════════════════════════════════════════════════
    
    @Min(value = 512, message = "Warning threshold must be at least 512MB")
    @Max(value = 50000, message = "Warning threshold cannot exceed 50GB")
    @Value("${gps.partition.size.warning-mb:2048}")
    private int warningThresholdMB;
    
    @Min(value = 1024, message = "Critical threshold must be at least 1GB")
    @Max(value = 100000, message = "Critical threshold cannot exceed 100GB")
    @Value("${gps.partition.size.critical-mb:2560}")
    private int criticalThresholdMB;
    
    @Min(value = 1536, message = "Emergency threshold must be at least 1.5GB")
    @Max(value = 150000, message = "Emergency threshold cannot exceed 150GB")
    @Value("${gps.partition.size.emergency-mb:3072}")
    private int emergencyThresholdMB;
    
    // ═══════════════════════════════════════════════════════════════════════════════════
    // AUTO-CREATION CONFIGURATION
    // ═══════════════════════════════════════════════════════════════════════════════════
    
    @Value("${gps.partition.auto-create:true}")
    private boolean autoCreateEnabled;
    
    @Value("${gps.partition.size-check-enabled:true}")
    private boolean sizeCheckEnabled;
    
    @Value("${gps.partition.auto-split:false}")  // Disabled for MySQL by default
    private boolean autoSplitEnabled;
    
    @Value("${gps.partition.auto-convert:true}")  // New: Auto-convert non-partitioned tables
    private boolean autoConvertEnabled;
    
    // ═══════════════════════════════════════════════════════════════════════════════════
    // TIMING CONFIGURATION (enhanced validation)
    // ═══════════════════════════════════════════════════════════════════════════════════
    
    @Min(value = 300000, message = "Size check interval must be at least 5 minutes")
    @Max(value = 86400000, message = "Size check interval cannot exceed 24 hours")
    @Value("${gps.partition.size-check-interval:3600000}")
    private long sizeCheckIntervalMs;
    
    @Min(value = 1, message = "Future months must be at least 1")
    @Max(value = 12, message = "Future months cannot exceed 12")
    @Value("${gps.partition.future-months:3}")
    private int futureMonthsToCreate;
    
    @Min(value = 1, message = "Retention months must be at least 1")
    @Max(value = 120, message = "Retention months cannot exceed 10 years")
    @Value("${gps.partition.retention-months:12}")
    private int retentionMonths;
    
    // ═══════════════════════════════════════════════════════════════════════════════════
    // MYSQL-SPECIFIC PERFORMANCE CONFIGURATION
    // ═══════════════════════════════════════════════════════════════════════════════════
    
    @Min(value = 1000, message = "Query timeout must be at least 1 second")
    @Max(value = 300000, message = "Query timeout cannot exceed 5 minutes")
    @Value("${gps.partition.query-timeout-ms:5000}")
    private long queryTimeoutMs;
    
    @Min(value = 100, message = "Batch size must be at least 100")
    @Max(value = 50000, message = "Batch size cannot exceed 50,000")
    @Value("${gps.partition.batch-size:1000}")
    private int batchSize;
    
    @Min(value = 1, message = "Max concurrent operations must be at least 1")
    @Max(value = 10, message = "Max concurrent operations cannot exceed 10")
    @Value("${gps.partition.max-concurrent-operations:3}")
    private int maxConcurrentOperations;
    
    // New MySQL-specific settings
    @Value("${gps.partition.mysql.innodb-buffer-pool-aware:true}")
    private boolean innodbBufferPoolAware;
    
    @Value("${gps.partition.mysql.optimize-after-partition:true}")
    private boolean optimizeAfterPartition;
    
    @Value("${gps.partition.mysql.analyze-after-partition:true}")
    private boolean analyzeAfterPartition;
    
    // ═══════════════════════════════════════════════════════════════════════════════════
    // ENHANCED ALERTING CONFIGURATION
    // ═══════════════════════════════════════════════════════════════════════════════════
    
    @Value("${gps.partition.alerts.enabled:true}")
    private boolean alertsEnabled;
    
    @Value("${gps.partition.alerts.email-enabled:false}")
    private boolean emailAlertsEnabled;
    
    @Value("${gps.partition.alerts.slack-enabled:false}")
    private boolean slackAlertsEnabled;
    
    @Value("${gps.partition.alerts.webhook-enabled:false}")
    private boolean webhookAlertsEnabled;
    
    @NotBlank(message = "Email recipients cannot be blank if email alerts are enabled")
    @Value("${gps.partition.alerts.email-recipients:admin@example.com}")
    private String emailRecipients;
    
    @Value("${gps.partition.alerts.slack-webhook-url:}")
    private String slackWebhookUrl;
    
    @Value("${gps.partition.alerts.webhook-url:}")
    private String webhookUrl;
    
    // Alert frequency controls
    @Min(value = 300000, message = "Alert cooldown must be at least 5 minutes")
    @Value("${gps.partition.alerts.cooldown-ms:1800000}")
    private long alertCooldownMs;
    
    // ═══════════════════════════════════════════════════════════════════════════════════
    // INITIALIZATION AND VALIDATION
    // ═══════════════════════════════════════════════════════════════════════════════════
    
    @PostConstruct
    public void initializeConfiguration() {
        configLogger.info("🚀 Initializing Enhanced MySQL Partition Configuration...");
        
        try {
            // Validate configuration values
            validateConfiguration();
            
            // Validate MySQL-specific settings
            validateMySQLConfiguration();
            
            // Log configuration summary
            logConfigurationSummary();
            
            configLogger.info("✅ Enhanced Partition Configuration initialized successfully");
            
        } catch (Exception e) {
            configLogger.error("❌ Failed to initialize Partition Configuration: {}", e.getMessage(), e);
            throw new RuntimeException("Partition Configuration initialization failed", e);
        }
    }
    
    /**
     * Enhanced validation including MySQL-specific checks
     */
    private void validateConfiguration() {
        configLogger.debug("🔍 Validating partition configuration...");
        
        // Validate size thresholds with logical ordering
        if (warningThresholdMB <= 0) {
            throw new IllegalArgumentException("Warning threshold must be positive: " + warningThresholdMB);
        }
        
        if (criticalThresholdMB <= warningThresholdMB) {
            throw new IllegalArgumentException("Critical threshold must be greater than warning threshold: " + 
                                             criticalThresholdMB + " <= " + warningThresholdMB);
        }
        
        if (emergencyThresholdMB <= criticalThresholdMB) {
            throw new IllegalArgumentException("Emergency threshold must be greater than critical threshold: " + 
                                             emergencyThresholdMB + " <= " + criticalThresholdMB);
        }
        
        // Enhanced timing validation
        if (sizeCheckIntervalMs < 300000) { // Minimum 5 minutes
            configLogger.warn("⚠️ Size check interval is very low: {}ms. Minimum recommended: 300000ms (5 minutes)", sizeCheckIntervalMs);
        }
        
        if (futureMonthsToCreate < 1 || futureMonthsToCreate > 12) {
            throw new IllegalArgumentException("Future months to create must be between 1-12: " + futureMonthsToCreate);
        }
        
        if (retentionMonths < 1) {
            throw new IllegalArgumentException("Retention months must be positive: " + retentionMonths);
        }
        
        // Performance settings validation
        if (queryTimeoutMs < 1000) {
            configLogger.warn("⚠️ Query timeout is very low: {}ms. Minimum recommended: 1000ms", queryTimeoutMs);
        }
        
        if (batchSize <= 0 || batchSize > 50000) {
            throw new IllegalArgumentException("Batch size must be between 1-50000: " + batchSize);
        }
        
        if (maxConcurrentOperations <= 0 || maxConcurrentOperations > 10) {
            throw new IllegalArgumentException("Max concurrent operations must be between 1-10: " + maxConcurrentOperations);
        }
        
        configLogger.debug("✅ Configuration validation completed successfully");
    }
    
    /**
     * Validate MySQL-specific configuration
     */
    private void validateMySQLConfiguration() {
        configLogger.debug("🔍 Validating MySQL-specific configuration...");
        
        // Validate alert settings
        if (emailAlertsEnabled && (emailRecipients == null || emailRecipients.trim().isEmpty())) {
            throw new IllegalArgumentException("Email recipients must be specified when email alerts are enabled");
        }
        
        if (slackAlertsEnabled && (slackWebhookUrl == null || slackWebhookUrl.trim().isEmpty())) {
            throw new IllegalArgumentException("Slack webhook URL must be specified when Slack alerts are enabled");
        }
        
        if (webhookAlertsEnabled && (webhookUrl == null || webhookUrl.trim().isEmpty())) {
            throw new IllegalArgumentException("Webhook URL must be specified when webhook alerts are enabled");
        }
        
        // Validate alert cooldown
        if (alertCooldownMs < 300000) {
            configLogger.warn("⚠️ Alert cooldown is very low: {}ms. Minimum recommended: 300000ms (5 minutes)", alertCooldownMs);
        }
        
        configLogger.debug("✅ MySQL-specific validation completed successfully");
    }
    
    /**
     * Enhanced configuration summary with MySQL-specific details
     */
    private void logConfigurationSummary() {
        configLogger.info("════════════════════════════════════════════════════════");
        configLogger.info("📊 ENHANCED MYSQL PARTITION CONFIGURATION SUMMARY");
        configLogger.info("════════════════════════════════════════════════════════");
        
        // Size thresholds
        configLogger.info("📏 Size Thresholds:");
        configLogger.info("  Warning: {} MB ({:.1f} GB)", warningThresholdMB, warningThresholdMB / 1024.0);
        configLogger.info("  Critical: {} MB ({:.1f} GB)", criticalThresholdMB, criticalThresholdMB / 1024.0);
        configLogger.info("  Emergency: {} MB ({:.1f} GB)", emergencyThresholdMB, emergencyThresholdMB / 1024.0);
        
        // Auto-creation settings
        configLogger.info("🤖 Automation Settings:");
        configLogger.info("  Auto Create: {}", autoCreateEnabled ? "ENABLED" : "DISABLED");
        configLogger.info("  Size Check: {}", sizeCheckEnabled ? "ENABLED" : "DISABLED");
        configLogger.info("  Auto Split: {}", autoSplitEnabled ? "ENABLED" : "DISABLED");
        configLogger.info("  Auto Convert: {}", autoConvertEnabled ? "ENABLED" : "DISABLED");
        
        // Timing settings
        configLogger.info("⏱️ Timing Settings:");
        configLogger.info("  Size Check Interval: {} minutes", sizeCheckIntervalMs / 60000);
        configLogger.info("  Future Months: {}", futureMonthsToCreate);
        configLogger.info("  Retention Months: {}", retentionMonths);
        
        // Performance settings
        configLogger.info("⚡ Performance Settings:");
        configLogger.info("  Query Timeout: {} seconds", queryTimeoutMs / 1000);
        configLogger.info("  Batch Size: {}", batchSize);
        configLogger.info("  Max Concurrent Operations: {}", maxConcurrentOperations);
        
        // MySQL-specific settings
        configLogger.info("🐬 MySQL-Specific Settings:");
        configLogger.info("  InnoDB Buffer Pool Aware: {}", innodbBufferPoolAware ? "ENABLED" : "DISABLED");
        configLogger.info("  Optimize After Partition: {}", optimizeAfterPartition ? "ENABLED" : "DISABLED");
        configLogger.info("  Analyze After Partition: {}", analyzeAfterPartition ? "ENABLED" : "DISABLED");
        
        // Alert settings
        configLogger.info("🔔 Alert Settings:");
        configLogger.info("  Alerts: {}", alertsEnabled ? "ENABLED" : "DISABLED");
        configLogger.info("  Email Alerts: {}", emailAlertsEnabled ? "ENABLED" : "DISABLED");
        configLogger.info("  Slack Alerts: {}", slackAlertsEnabled ? "ENABLED" : "DISABLED");
        configLogger.info("  Webhook Alerts: {}", webhookAlertsEnabled ? "ENABLED" : "DISABLED");
        configLogger.info("  Alert Cooldown: {} minutes", alertCooldownMs / 60000);
        
        if (emailAlertsEnabled) {
            configLogger.info("  Email Recipients: {}", maskEmailAddresses(emailRecipients));
        }
        
        configLogger.info("════════════════════════════════════════════════════════");
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════════
    // ENHANCED UTILITY METHODS
    // ═══════════════════════════════════════════════════════════════════════════════════
    
    /**
     * Get threshold level with detailed information
     */
    public ThresholdInfo getThresholdInfo(double sizeMB) {
        ThresholdInfo info = new ThresholdInfo();
        
        if (sizeMB > emergencyThresholdMB) {
            info.setLevel("EMERGENCY");
            info.setSeverity(4);
            info.setAction("IMMEDIATE_ACTION_REQUIRED");
            info.setDescription("Partition size exceeds emergency threshold - immediate action required");
        } else if (sizeMB > criticalThresholdMB) {
            info.setLevel("CRITICAL");
            info.setSeverity(3);
            info.setAction("URGENT_ATTENTION");
            info.setDescription("Partition size exceeds critical threshold - urgent attention needed");
        } else if (sizeMB > warningThresholdMB) {
            info.setLevel("WARNING");
            info.setSeverity(2);
            info.setAction("MONITOR_CLOSELY");
            info.setDescription("Partition size exceeds warning threshold - monitor closely");
        } else {
            info.setLevel("NORMAL");
            info.setSeverity(1);
            info.setAction("CONTINUE_MONITORING");
            info.setDescription("Partition size is within normal limits");
        }
        
        info.setCurrentSize(sizeMB);
        info.setPercentageOfWarning((sizeMB / warningThresholdMB) * 100);
        info.setPercentageOfCritical((sizeMB / criticalThresholdMB) * 100);
        info.setPercentageOfEmergency((sizeMB / emergencyThresholdMB) * 100);
        
        return info;
    }
    
    /**
     * Get recommended actions based on current state
     */
    public List<String> getRecommendedActions(double sizeMB, long rowCount) {
        List<String> actions = new java.util.ArrayList<>();
        
        ThresholdInfo info = getThresholdInfo(sizeMB);
        
        switch (info.getLevel()) {
            case "EMERGENCY":
                actions.add("Create new partition immediately");
                actions.add("Consider archiving old data");
                actions.add("Review partition strategy");
                actions.add("Scale database resources if needed");
                break;
            case "CRITICAL":
                actions.add("Plan partition creation within 24 hours");
                actions.add("Monitor growth rate closely");
                actions.add("Prepare for data archiving");
                break;
            case "WARNING":
                actions.add("Schedule partition creation");
                actions.add("Review data retention policies");
                actions.add("Monitor daily growth");
                break;
            case "NORMAL":
                actions.add("Continue normal monitoring");
                if (autoCreateEnabled) {
                    actions.add("Future partitions will be created automatically");
                }
                break;
        }
        
        // Add row-count specific recommendations
        if (rowCount > 50000000) {
            actions.add("Consider indexing optimization for large row count");
        }
        
        return actions;
    }
    
    /**
     * Mask email addresses for logging security
     */
    private String maskEmailAddresses(String emails) {
        if (emails == null || emails.trim().isEmpty()) {
            return "";
        }
        
        return Arrays.stream(emails.split(","))
                .map(email -> {
                    email = email.trim();
                    int atIndex = email.indexOf('@');
                    if (atIndex > 0) {
                        return email.substring(0, Math.min(2, atIndex)) + "***@" + email.substring(atIndex + 1);
                    }
                    return "***";
                })
                .reduce((a, b) -> a + ", " + b)
                .orElse("");
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════════
    // GETTERS WITH ENHANCED LOGGING
    // ═══════════════════════════════════════════════════════════════════════════════════
    
    // ... (keep all your existing getters and add the new ones)
    
    public boolean isAutoConvertEnabled() {
        return autoConvertEnabled;
    }
    
    public boolean isInnodbBufferPoolAware() {
        return innodbBufferPoolAware;
    }
    
    public boolean isOptimizeAfterPartition() {
        return optimizeAfterPartition;
    }
    
    public boolean isAnalyzeAfterPartition() {
        return analyzeAfterPartition;
    }
    
    public boolean isWebhookAlertsEnabled() {
        return webhookAlertsEnabled;
    }
    
    public String getEmailRecipients() {
        return emailRecipients;
    }
    
    public String getSlackWebhookUrl() {
        return slackWebhookUrl;
    }
    
    public String getWebhookUrl() {
        return webhookUrl;
    }
    
    public long getAlertCooldownMs() {
        return alertCooldownMs;
    }
    
    // Keep all your existing getters...
    public int getWarningThresholdMB() { return warningThresholdMB; }
    public int getCriticalThresholdMB() { return criticalThresholdMB; }
    public int getEmergencyThresholdMB() { return emergencyThresholdMB; }
    public boolean isAutoCreateEnabled() { return autoCreateEnabled; }
    public boolean isSizeCheckEnabled() { return sizeCheckEnabled; }
    public boolean isAutoSplitEnabled() { return autoSplitEnabled; }
    public long getSizeCheckIntervalMs() { return sizeCheckIntervalMs; }
    public int getFutureMonthsToCreate() { return futureMonthsToCreate; }
    public int getRetentionMonths() { return retentionMonths; }
    public long getQueryTimeoutMs() { return queryTimeoutMs; }
    public int getBatchSize() { return batchSize; }
    public int getMaxConcurrentOperations() { return maxConcurrentOperations; }
    public boolean isAlertsEnabled() { return alertsEnabled; }
    public boolean isEmailAlertsEnabled() { return emailAlertsEnabled; }
    public boolean isSlackAlertsEnabled() { return slackAlertsEnabled; }
    
    // ═══════════════════════════════════════════════════════════════════════════════════
    // THRESHOLD INFO CLASS
    // ═══════════════════════════════════════════════════════════════════════════════════
    
    public static class ThresholdInfo {
        private String level;
        private int severity;
        private String action;
        private String description;
        private double currentSize;
        private double percentageOfWarning;
        private double percentageOfCritical;
        private double percentageOfEmergency;
        
        // Getters and setters
        public String getLevel() { return level; }
        public void setLevel(String level) { this.level = level; }
        public int getSeverity() { return severity; }
        public void setSeverity(int severity) { this.severity = severity; }
        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public double getCurrentSize() { return currentSize; }
        public void setCurrentSize(double currentSize) { this.currentSize = currentSize; }
        public double getPercentageOfWarning() { return percentageOfWarning; }
        public void setPercentageOfWarning(double percentageOfWarning) { this.percentageOfWarning = percentageOfWarning; }
        public double getPercentageOfCritical() { return percentageOfCritical; }
        public void setPercentageOfCritical(double percentageOfCritical) { this.percentageOfCritical = percentageOfCritical; }
        public double getPercentageOfEmergency() { return percentageOfEmergency; }
        public void setPercentageOfEmergency(double percentageOfEmergency) { this.percentageOfEmergency = percentageOfEmergency; }
    }
    
    /**
     * Logs the current effective partition configuration in a readable format.
     */
    public void logCurrentStatus() {
        configLogger.info("════════════════════════════════════════════════════════");
        configLogger.info("📊 CURRENT PARTITION CONFIGURATION STATUS");
        configLogger.info("════════════════════════════════════════════════════════");

        configLogger.info("📏 Size Thresholds:");
        configLogger.info("  Warning Threshold: {} MB", warningThresholdMB);
        configLogger.info("  Critical Threshold: {} MB", criticalThresholdMB);
        configLogger.info("  Emergency Threshold: {} MB", emergencyThresholdMB);

        configLogger.info("🤖 Automation Settings:");
        configLogger.info("  Auto Create Enabled: {}", autoCreateEnabled);
        configLogger.info("  Size Check Enabled: {}", sizeCheckEnabled);
        configLogger.info("  Auto Split Enabled: {}", autoSplitEnabled);
        configLogger.info("  Auto Convert Enabled: {}", autoConvertEnabled);

        configLogger.info("⏱️ Timing Settings:");
        configLogger.info("  Size Check Interval: {} ms ({} minutes)", sizeCheckIntervalMs, sizeCheckIntervalMs / 60000);
        configLogger.info("  Future Months To Create: {}", futureMonthsToCreate);
        configLogger.info("  Retention Months: {}", retentionMonths);

        configLogger.info("⚡ Performance Settings:");
        configLogger.info("  Query Timeout: {} ms", queryTimeoutMs);
        configLogger.info("  Batch Size: {}", batchSize);
        configLogger.info("  Max Concurrent Operations: {}", maxConcurrentOperations);

        configLogger.info("🐬 MySQL-Specific Settings:");
        configLogger.info("  InnoDB Buffer Pool Aware: {}", innodbBufferPoolAware);
        configLogger.info("  Optimize After Partition: {}", optimizeAfterPartition);
        configLogger.info("  Analyze After Partition: {}", analyzeAfterPartition);

        configLogger.info("🔔 Alert Settings:");
        configLogger.info("  Alerts Enabled: {}", alertsEnabled);
        configLogger.info("  Email Alerts Enabled: {}", emailAlertsEnabled);
        configLogger.info("  Slack Alerts Enabled: {}", slackAlertsEnabled);
        configLogger.info("  Webhook Alerts Enabled: {}", webhookAlertsEnabled);
        configLogger.info("  Alert Cooldown: {} ms ({} minutes)", alertCooldownMs, alertCooldownMs / 60000);

        if (emailAlertsEnabled) {
            configLogger.info("  Email Recipients: {}", maskEmailAddresses(emailRecipients));
        }

        if (slackAlertsEnabled) {
            configLogger.info("  Slack Webhook URL: {}", slackWebhookUrl);
        }

        if (webhookAlertsEnabled) {
            configLogger.info("  Webhook URL: {}", webhookUrl);
        }

        configLogger.info("════════════════════════════════════════════════════════");
    }
    /**
     * Get the threshold level as a String for a given size in MB.
     * Returns one of: NORMAL, WARNING, CRITICAL, EMERGENCY
     */
    public String getThresholdLevel(double sizeMB) {
        return getThresholdInfo(sizeMB).getLevel();
    }
}