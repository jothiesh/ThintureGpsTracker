package com.GpsTracker.Thinture.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Custom deserializer for raw timestamp storage without timezone conversion
 * This ensures that "2025-07-08 16:18:11" is stored exactly as "2025-07-08 16:18:11"
 * No timezone conversion occurs anywhere in the process
 */
public class RawTimestampDeserializer extends JsonDeserializer<Timestamp> {
    
    private static final DateTimeFormatter FORMATTER = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public Timestamp deserialize(JsonParser parser, DeserializationContext context) 
            throws IOException {
        
        String timestampStr = parser.getValueAsString();
        if (timestampStr == null || timestampStr.trim().isEmpty()) {
            return null;
        }
        
        try {
            System.out.println("🔍 Raw deserializing timestamp: " + timestampStr);
            
            // Parse as LocalDateTime (no timezone information)
            LocalDateTime localDateTime = LocalDateTime.parse(timestampStr.trim(), FORMATTER);
            
            // Convert directly to Timestamp without any timezone conversion
            Timestamp result = Timestamp.valueOf(localDateTime);
            
            System.out.println("✅ Raw timestamp result: " + result);
            return result;
            
        } catch (Exception e) {
            System.err.println("❌ Failed to parse timestamp: " + timestampStr);
            e.printStackTrace();
            throw new IOException("Failed to parse timestamp: " + timestampStr + 
                ". Expected format: yyyy-MM-dd HH:mm:ss", e);
        }
    }
}