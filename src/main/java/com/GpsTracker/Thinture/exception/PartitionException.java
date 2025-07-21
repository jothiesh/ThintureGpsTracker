// ================================================================================================
// PartitionException.java - CUSTOM PARTITION ERROR HANDLING
// ================================================================================================

package com.GpsTracker.Thinture.exception;

/**
 * Custom exception for partition-related errors in the GPS tracking system
 */
public class PartitionException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final PartitionErrorType errorType;
    private final String partitionName;

    // ═══════════════════════════════════════════════════════════════════════════════════
    // ERROR TYPES
    // ═══════════════════════════════════════════════════════════════════════════════════

    /**
     * Types of partition errors
     */
    public enum PartitionErrorType {
        PARTITION_NOT_FOUND("Partition does not exist"),
        PARTITION_ALREADY_EXISTS("Partition already exists"),
        PARTITION_CREATION_FAILED("Failed to create partition"),
        PARTITION_DROP_FAILED("Failed to drop partition"),
        PARTITION_OPTIMIZATION_FAILED("Failed to optimize partition"),
        PARTITION_ANALYSIS_FAILED("Failed to analyze partition"),
        INVALID_PARTITION_NAME("Invalid partition name format"),
        PARTITION_TOO_OLD("Partition is too old for operation"),
        PARTITION_TOO_RECENT("Partition is too recent for operation"),
        DATABASE_CONNECTION_ERROR("Database connection error"),
        INSUFFICIENT_PERMISSIONS("Insufficient database permissions"),
        PARTITION_CLEANUP_FAILED("Failed to cleanup old partitions"),
        PARTITION_INFO_ERROR("Failed to retrieve partition information"),
        UNKNOWN_ERROR("Unknown partition error");

        private final String defaultMessage;

        PartitionErrorType(String defaultMessage) {
            this.defaultMessage = defaultMessage;
        }

        public String getDefaultMessage() {
            return defaultMessage;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════════
    // CONSTRUCTORS
    // ═══════════════════════════════════════════════════════════════════════════════════

    /**
     * Create exception with error type only
     */
    public PartitionException(PartitionErrorType errorType) {
        super(errorType.getDefaultMessage());
        this.errorType = errorType;
        this.partitionName = null;
    }

    /**
     * Create exception with error type and custom message
     */
    public PartitionException(PartitionErrorType errorType, String message) {
        super(message);
        this.errorType = errorType;
        this.partitionName = null;
    }

    /**
     * Create exception with error type and partition name
     */
    public PartitionException(PartitionErrorType errorType, String message, String partitionName) {
        super(message);
        this.errorType = errorType;
        this.partitionName = partitionName;
    }

    /**
     * Create exception with error type, message, and cause
     */
    public PartitionException(PartitionErrorType errorType, String message, Throwable cause) {
        super(message, cause);
        this.errorType = errorType;
        this.partitionName = null;
    }

    /**
     * Create exception with error type, message, partition name, and cause
     */
    public PartitionException(PartitionErrorType errorType, String message, String partitionName, Throwable cause) {
        super(message, cause);
        this.errorType = errorType;
        this.partitionName = partitionName;
    }

    /**
     * Create exception with just message (uses UNKNOWN_ERROR type)
     */
    public PartitionException(String message) {
        super(message);
        this.errorType = PartitionErrorType.UNKNOWN_ERROR;
        this.partitionName = null;
    }

    /**
     * Create exception with message and cause (uses UNKNOWN_ERROR type)
     */
    public PartitionException(String message, Throwable cause) {
        super(message, cause);
        this.errorType = PartitionErrorType.UNKNOWN_ERROR;
        this.partitionName = null;
    }

    // ═══════════════════════════════════════════════════════════════════════════════════
    // GETTERS
    // ═══════════════════════════════════════════════════════════════════════════════════

    /**
     * Get the error type
     */
    public PartitionErrorType getErrorType() {
        return errorType;
    }

    /**
     * Get the partition name (if applicable)
     */
    public String getPartitionName() {
        return partitionName;
    }

    /**
     * Check if this exception has a partition name
     */
    public boolean hasPartitionName() {
        return partitionName != null && !partitionName.trim().isEmpty();
    }

    // ═══════════════════════════════════════════════════════════════════════════════════
    // UTILITY METHODS
    // ═══════════════════════════════════════════════════════════════════════════════════

    /**
     * Get formatted error message with partition name
     */
    public String getFormattedMessage() {
        if (hasPartitionName()) {
            return String.format("[%s] %s (Partition: %s)", 
                               errorType.name(), getMessage(), partitionName);
        } else {
            return String.format("[%s] %s", errorType.name(), getMessage());
        }
    }

    /**
     * Check if this is a specific error type
     */
    public boolean isErrorType(PartitionErrorType type) {
        return this.errorType == type;
    }

    /**
     * Check if this is a database-related error
     */
    public boolean isDatabaseError() {
        return errorType == PartitionErrorType.DATABASE_CONNECTION_ERROR ||
               errorType == PartitionErrorType.INSUFFICIENT_PERMISSIONS;
    }

    /**
     * Check if this is a permission-related error
     */
    public boolean isPermissionError() {
        return errorType == PartitionErrorType.INSUFFICIENT_PERMISSIONS;
    }

    /**
     * Check if this is a validation error
     */
    public boolean isValidationError() {
        return errorType == PartitionErrorType.INVALID_PARTITION_NAME ||
               errorType == PartitionErrorType.PARTITION_TOO_OLD ||
               errorType == PartitionErrorType.PARTITION_TOO_RECENT;
    }

    // ═══════════════════════════════════════════════════════════════════════════════════
    // STATIC FACTORY METHODS
    // ═══════════════════════════════════════════════════════════════════════════════════

    /**
     * Create partition not found exception
     */
    public static PartitionException partitionNotFound(String partitionName) {
        return new PartitionException(
            PartitionErrorType.PARTITION_NOT_FOUND,
            "Partition '" + partitionName + "' does not exist",
            partitionName
        );
    }

    /**
     * Create partition already exists exception
     */
    public static PartitionException partitionAlreadyExists(String partitionName) {
        return new PartitionException(
            PartitionErrorType.PARTITION_ALREADY_EXISTS,
            "Partition '" + partitionName + "' already exists",
            partitionName
        );
    }

    /**
     * Create partition creation failed exception
     */
    public static PartitionException creationFailed(String partitionName, Throwable cause) {
        return new PartitionException(
            PartitionErrorType.PARTITION_CREATION_FAILED,
            "Failed to create partition '" + partitionName + "'",
            partitionName,
            cause
        );
    }

    /**
     * Create partition drop failed exception
     */
    public static PartitionException dropFailed(String partitionName, Throwable cause) {
        return new PartitionException(
            PartitionErrorType.PARTITION_DROP_FAILED,
            "Failed to drop partition '" + partitionName + "'",
            partitionName,
            cause
        );
    }

    /**
     * Create invalid partition name exception
     */
    public static PartitionException invalidPartitionName(String partitionName) {
        return new PartitionException(
            PartitionErrorType.INVALID_PARTITION_NAME,
            "Invalid partition name format: '" + partitionName + "'. Expected format: p_YYYYMM",
            partitionName
        );
    }

    /**
     * Create database connection error exception
     */
    public static PartitionException databaseConnectionError(Throwable cause) {
        return new PartitionException(
            PartitionErrorType.DATABASE_CONNECTION_ERROR,
            "Database connection error during partition operation",
            cause
        );
    }

    /**
     * Create insufficient permissions exception
     */
    public static PartitionException insufficientPermissions(String operation) {
        return new PartitionException(
            PartitionErrorType.INSUFFICIENT_PERMISSIONS,
            "Insufficient database permissions for partition operation: " + operation
        );
    }

    /**
     * Create partition optimization failed exception
     */
    public static PartitionException optimizationFailed(String partitionName, Throwable cause) {
        return new PartitionException(
            PartitionErrorType.PARTITION_OPTIMIZATION_FAILED,
            "Failed to optimize partition '" + partitionName + "'",
            partitionName,
            cause
        );
    }

    /**
     * Create partition analysis failed exception
     */
    public static PartitionException analysisFailed(String partitionName, Throwable cause) {
        return new PartitionException(
            PartitionErrorType.PARTITION_ANALYSIS_FAILED,
            "Failed to analyze partition '" + partitionName + "'",
            partitionName,
            cause
        );
    }

    /**
     * Create partition cleanup failed exception
     */
    public static PartitionException cleanupFailed(Throwable cause) {
        return new PartitionException(
            PartitionErrorType.PARTITION_CLEANUP_FAILED,
            "Failed to cleanup old partitions",
            cause
        );
    }

    /**
     * Create partition too recent exception
     */
    public static PartitionException partitionTooRecent(String partitionName) {
        return new PartitionException(
            PartitionErrorType.PARTITION_TOO_RECENT,
            "Partition '" + partitionName + "' is too recent for this operation",
            partitionName
        );
    }

    /**
     * Create partition info error exception
     */
    public static PartitionException infoError(Throwable cause) {
        return new PartitionException(
            PartitionErrorType.PARTITION_INFO_ERROR,
            "Failed to retrieve partition information",
            cause
        );
    }

    // ═══════════════════════════════════════════════════════════════════════════════════
    // OVERRIDE METHODS
    // ═══════════════════════════════════════════════════════════════════════════════════

    @Override
    public String toString() {
        return getFormattedMessage();
    }
}