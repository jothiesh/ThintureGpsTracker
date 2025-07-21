package com.GpsTracker.Thinture.exception;

import org.eclipse.paho.client.mqttv3.MqttException;

public class MqttConnectionException extends RuntimeException {
    
    private String brokerUrl;
    private String clientId;
    private int reasonCode;
    private ConnectionFailureType failureType;
    
    public enum ConnectionFailureType {
        CONNECTION_LOST,
        BROKER_UNAVAILABLE,
        CLIENT_TIMEOUT,
        AUTHENTICATION_FAILED,
        SUBSCRIPTION_FAILED,
        PUBLISH_FAILED,
        POOL_EXHAUSTED,
        UNKNOWN
    }
    
    public MqttConnectionException(String message) {
        super(message);
        this.failureType = ConnectionFailureType.UNKNOWN;
    }
    
    public MqttConnectionException(String message, Throwable cause) {
        super(message, cause);
        this.failureType = ConnectionFailureType.UNKNOWN;
        if (cause instanceof MqttException) {
            this.reasonCode = ((MqttException) cause).getReasonCode();
        }
    }
    
    public MqttConnectionException(String message, ConnectionFailureType failureType) {
        super(message);
        this.failureType = failureType;
    }
    
    public MqttConnectionException(String message, Throwable cause, ConnectionFailureType failureType) {
        super(message, cause);
        this.failureType = failureType;
        if (cause instanceof MqttException) {
            this.reasonCode = ((MqttException) cause).getReasonCode();
        }
    }
    
    public static MqttConnectionException connectionLost(String brokerUrl, Throwable cause) {
        MqttConnectionException ex = new MqttConnectionException(
            "MQTT connection lost to broker: " + brokerUrl, 
            cause, 
            ConnectionFailureType.CONNECTION_LOST
        );
        ex.brokerUrl = brokerUrl;
        return ex;
    }
    
    public static MqttConnectionException brokerUnavailable(String brokerUrl) {
        MqttConnectionException ex = new MqttConnectionException(
            "MQTT broker unavailable: " + brokerUrl,
            ConnectionFailureType.BROKER_UNAVAILABLE
        );
        ex.brokerUrl = brokerUrl;
        return ex;
    }
    
    public static MqttConnectionException authenticationFailed(String clientId) {
        MqttConnectionException ex = new MqttConnectionException(
            "MQTT authentication failed for client: " + clientId,
            ConnectionFailureType.AUTHENTICATION_FAILED
        );
        ex.clientId = clientId;
        return ex;
    }
    
    public static MqttConnectionException subscriptionFailed(String topic, Throwable cause) {
        return new MqttConnectionException(
            "Failed to subscribe to MQTT topic: " + topic,
            cause,
            ConnectionFailureType.SUBSCRIPTION_FAILED
        );
    }
    
    public static MqttConnectionException publishFailed(String topic, Throwable cause) {
        return new MqttConnectionException(
            "Failed to publish to MQTT topic: " + topic,
            cause,
            ConnectionFailureType.PUBLISH_FAILED
        );
    }
    
    public static MqttConnectionException poolExhausted() {
        return new MqttConnectionException(
            "MQTT connection pool exhausted",
            ConnectionFailureType.POOL_EXHAUSTED
        );
    }
    
    public String getBrokerUrl() {
        return brokerUrl;
    }
    
    public String getClientId() {
        return clientId;
    }
    
    public int getReasonCode() {
        return reasonCode;
    }
    
    public ConnectionFailureType getFailureType() {
        return failureType;
    }
    
    public boolean isRecoverable() {
        switch (failureType) {
            case CONNECTION_LOST:
            case BROKER_UNAVAILABLE:
            case CLIENT_TIMEOUT:
            case PUBLISH_FAILED:
                return true;
            case AUTHENTICATION_FAILED:
            case POOL_EXHAUSTED:
            default:
                return false;
        }
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getName()).append(": ").append(getMessage());
        sb.append(" [Type: ").append(failureType).append("]");
        if (brokerUrl != null) {
            sb.append(" [Broker: ").append(brokerUrl).append("]");
        }
        if (clientId != null) {
            sb.append(" [Client: ").append(clientId).append("]");
        }
        if (reasonCode != 0) {
            sb.append(" [Code: ").append(reasonCode).append("]");
        }
        sb.append(" [Recoverable: ").append(isRecoverable()).append("]");
        return sb.toString();
    }
}