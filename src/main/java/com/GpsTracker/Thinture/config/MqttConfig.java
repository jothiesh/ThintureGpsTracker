package com.GpsTracker.Thinture.config;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;

/**
 * Fixed MQTT Configuration to prevent multiple connections and reduce logging
 */
@Configuration
@EnableIntegration
@IntegrationComponentScan
public class MqttConfig {

    @Value("${mqtt.broker-url}")
    private String brokerUrl;

    @Value("${mqtt.username}")
    private String username;

    @Value("${mqtt.password}")
    private String password;

    @Value("${mqtt.client-id}")
    private String clientId;

    @Value("${mqtt.keep-alive-interval:60}")
    private int keepAliveInterval;

    @Value("${mqtt.automatic-reconnect:true}")
    private boolean automaticReconnect;

    @Value("${mqtt.enabled:true}")
    private boolean mqttEnabled;

    @Bean
    public MqttPahoClientFactory mqttClientFactory() {
        DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
        
        if (!mqttEnabled) {
            System.out.println("MQTT is disabled via configuration");
            return factory;
        }
        
        MqttConnectOptions options = new MqttConnectOptions();
        options.setServerURIs(new String[]{brokerUrl});
        options.setUserName(username);
        options.setPassword(password.toCharArray());
        options.setKeepAliveInterval(keepAliveInterval);
        options.setAutomaticReconnect(automaticReconnect);
        options.setCleanSession(true);
        options.setConnectionTimeout(30);
        
        // Important: Set max inflight to reduce message spam
        options.setMaxInflight(100);
        
        factory.setConnectionOptions(options);
        
        System.out.println("========================================");
        System.out.println("MQTT Configuration:");
        System.out.println("Broker: " + brokerUrl);
        System.out.println("Client ID: " + clientId);
        System.out.println("Keep Alive: " + keepAliveInterval + "s");
        System.out.println("========================================");
        
        return factory;
    }
}