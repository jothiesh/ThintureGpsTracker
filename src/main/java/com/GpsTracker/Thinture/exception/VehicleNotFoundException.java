package com.GpsTracker.Thinture.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class VehicleNotFoundException extends RuntimeException {
    
    private String imei;
    private Long vehicleId;
    private String deviceId;
    
    public VehicleNotFoundException(String message) {
        super(message);
    }
    
    public VehicleNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public static VehicleNotFoundException byImei(String imei) {
        VehicleNotFoundException ex = new VehicleNotFoundException(
            "Vehicle not found with IMEI: " + imei
        );
        ex.imei = imei;
        return ex;
    }
    
    public static VehicleNotFoundException byId(Long vehicleId) {
        VehicleNotFoundException ex = new VehicleNotFoundException(
            "Vehicle not found with ID: " + vehicleId
        );
        ex.vehicleId = vehicleId;
        return ex;
    }
    
    public static VehicleNotFoundException byDeviceId(String deviceId) {
        VehicleNotFoundException ex = new VehicleNotFoundException(
            "Vehicle not found with Device ID: " + deviceId
        );
        ex.deviceId = deviceId;
        return ex;
    }
    
    public String getImei() {
        return imei;
    }
    
    public Long getVehicleId() {
        return vehicleId;
    }
    
    public String getDeviceId() {
        return deviceId;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getName()).append(": ").append(getMessage());
        if (imei != null) {
            sb.append(" [IMEI: ").append(imei).append("]");
        }
        if (vehicleId != null) {
            sb.append(" [ID: ").append(vehicleId).append("]");
        }
        if (deviceId != null) {
            sb.append(" [Device: ").append(deviceId).append("]");
        }
        return sb.toString();
    }
}