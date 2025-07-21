package com.GpsTracker.Thinture.exception;

public class GpsDataException extends RuntimeException {
    
    private String deviceId;
    private String errorCode;
    
    public GpsDataException(String message) {
        super(message);
    }
    
    public GpsDataException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public GpsDataException(String message, String deviceId) {
        super(message);
        this.deviceId = deviceId;
    }
    
    public GpsDataException(String message, String deviceId, String errorCode) {
        super(message);
        this.deviceId = deviceId;
        this.errorCode = errorCode;
    }
    
    public GpsDataException(String message, Throwable cause, String deviceId) {
        super(message, cause);
        this.deviceId = deviceId;
    }
    
    public String getDeviceId() {
        return deviceId;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getName()).append(": ").append(getMessage());
        if (deviceId != null) {
            sb.append(" [Device: ").append(deviceId).append("]");
        }
        if (errorCode != null) {
            sb.append(" [Code: ").append(errorCode).append("]");
        }
        return sb.toString();
    }
}