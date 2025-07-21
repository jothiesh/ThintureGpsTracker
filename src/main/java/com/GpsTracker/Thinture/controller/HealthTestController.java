package com.GpsTracker.Thinture.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/test")
public class HealthTestController {

    @Autowired(required = false)
    private JdbcTemplate jdbcTemplate;

    @Value("${spring.profiles.active:default}")
    private String activeProfile;

    @Value("${spring.datasource.url:not-configured}")
    private String datasourceUrl;

    @GetMapping("/status")
    public Map<String, Object> getStatus() {
        Map<String, Object> status = new HashMap<>();
        
        status.put("application", "ThintureGpsTracker");
        status.put("status", "running");
        status.put("activeProfile", activeProfile);
        status.put("timestamp", System.currentTimeMillis());
        
        // Test database connection
        try {
            if (jdbcTemplate != null) {
                Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
                status.put("database", "connected");
                status.put("databaseTest", result);
                status.put("databaseUrl", datasourceUrl.replaceAll("password=.*?(&|$)", "password=***$1"));
            } else {
                status.put("database", "not configured");
            }
        } catch (Exception e) {
            status.put("database", "error");
            status.put("databaseError", e.getMessage());
        }
        
        return status;
    }

    @GetMapping("/ping")
    public String ping() {
        return "pong - " + System.currentTimeMillis();
    }
}