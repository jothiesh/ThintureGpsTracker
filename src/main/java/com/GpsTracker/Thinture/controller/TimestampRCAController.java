// ================================================================================================
// ROOT CAUSE ANALYSIS - STEP BY STEP DEBUGGING
// ================================================================================================

// ================================================================================================
// STEP 1: Debug Controller - Trace Every Step
// ================================================================================================

package com.GpsTracker.Thinture.controller;

import com.GpsTracker.Thinture.model.VehicleHistory;
import com.GpsTracker.Thinture.repository.VehicleHistoryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

@RestController
@RequestMapping("/api/debug-timestamp")
public class TimestampRCAController {

    @Autowired
    private VehicleHistoryRepository repository;
    
    @Autowired
    private ObjectMapper objectMapper;

    @PostMapping("/trace")
    public Map<String, Object> traceTimestampConversion(@RequestBody String rawJson) {
        Map<String, Object> result = new HashMap<>();
        
        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println("🔍 TIMESTAMP RCA - STEP BY STEP ANALYSIS");
        System.out.println("═══════════════════════════════════════════════════════");
        
        try {
            // STEP 1: Check system timezone settings
            System.out.println("STEP 1: SYSTEM TIMEZONE ANALYSIS");
            System.out.println("Default TimeZone: " + TimeZone.getDefault().getID());
            System.out.println("Default ZoneId: " + ZoneId.systemDefault());
            System.out.println("JVM timezone: " + System.getProperty("user.timezone"));
            System.out.println("Current time: " + LocalDateTime.now());
            
            // STEP 2: Check raw JSON input
            System.out.println("\nSTEP 2: RAW JSON INPUT");
            System.out.println("Raw JSON received: " + rawJson);
            
            // STEP 3: Manual JSON parsing to isolate the timestamp
            System.out.println("\nSTEP 3: MANUAL TIMESTAMP EXTRACTION");
            String timestampFromJson = extractTimestampFromJson(rawJson);
            System.out.println("Extracted timestamp string: " + timestampFromJson);
            
            // STEP 4: Test direct LocalDateTime parsing (no Jackson)
            System.out.println("\nSTEP 4: DIRECT PARSING TEST (NO JACKSON)");
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime directParsed = LocalDateTime.parse(timestampFromJson, formatter);
            Timestamp directTimestamp = Timestamp.valueOf(directParsed);
            System.out.println("Direct LocalDateTime: " + directParsed);
            System.out.println("Direct Timestamp: " + directTimestamp);
            
            // STEP 5: Test Jackson ObjectMapper parsing
            System.out.println("\nSTEP 5: JACKSON OBJECTMAPPER PARSING");
            VehicleHistory vh = objectMapper.readValue(rawJson, VehicleHistory.class);
            System.out.println("Jackson parsed timestamp: " + vh.getTimestamp());
            System.out.println("Timestamp class: " + vh.getTimestamp().getClass().getName());
            
            // STEP 6: Compare before and after Jackson
            System.out.println("\nSTEP 6: BEFORE/AFTER COMPARISON");
            System.out.println("Original:     " + timestampFromJson);
            System.out.println("Direct parse: " + directTimestamp);
            System.out.println("Jackson parse:" + vh.getTimestamp());
            boolean jacksonMatches = directTimestamp.equals(vh.getTimestamp());
            System.out.println("Jackson matches direct: " + jacksonMatches);
            
            if (!jacksonMatches) {
                long diff = vh.getTimestamp().getTime() - directTimestamp.getTime();
                System.out.println("Time difference (ms): " + diff);
                System.out.println("Time difference (hours): " + (diff / 3600000.0));
                System.out.println("🚨 JACKSON IS CONVERTING THE TIMESTAMP!");
            }
            
            // STEP 7: Test database storage
            System.out.println("\nSTEP 7: DATABASE STORAGE TEST");
            
            // Save direct timestamp
            VehicleHistory directVh = new VehicleHistory();
            directVh.setDevice_id("DIRECT_TEST");
            directVh.setTimestamp(directTimestamp);
            directVh.setLatitude(8.5);
            directVh.setLongitude(76.9);
            directVh.setStatus("TEST");
            VehicleHistory savedDirect = repository.save(directVh);
            
            // Save Jackson timestamp
            vh.setDevice_id("JACKSON_TEST");
            vh.setLatitude(8.5);
            vh.setLongitude(76.9);
            vh.setStatus("TEST");
            VehicleHistory savedJackson = repository.save(vh);
            
            // Retrieve both and compare
            VehicleHistory retrievedDirect = repository.findById(savedDirect.getId()).orElse(null);
            VehicleHistory retrievedJackson = repository.findById(savedJackson.getId()).orElse(null);
            
            System.out.println("Direct stored:     " + retrievedDirect.getTimestamp());
            System.out.println("Jackson stored:    " + retrievedJackson.getTimestamp());
            System.out.println("DB storage matches: " + retrievedDirect.getTimestamp().equals(retrievedJackson.getTimestamp()));
            
            // STEP 8: Check ObjectMapper configuration
            System.out.println("\nSTEP 8: OBJECTMAPPER CONFIGURATION ANALYSIS");
            System.out.println("ObjectMapper class: " + objectMapper.getClass().getName());
            System.out.println("ADJUST_DATES_TO_CONTEXT_TIME_ZONE: " + 
                objectMapper.getDeserializationConfig().isEnabled(
                    com.fasterxml.jackson.databind.DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE));
            System.out.println("WRITE_DATES_AS_TIMESTAMPS: " + 
                objectMapper.getSerializationConfig().isEnabled(
                    com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS));
            
            // STEP 9: Manual timestamp creation test
            System.out.println("\nSTEP 9: MANUAL TIMESTAMP CREATION");
            Timestamp manual = new Timestamp(2025-1900, 7-1, 8, 16, 18, 11, 0);
            System.out.println("Manual timestamp: " + manual);
            System.out.println("Manual toString: " + manual.toString());
            
            // Build result
            result.put("success", true);
            result.put("originalTimestamp", timestampFromJson);
            result.put("directParsed", directTimestamp.toString());
            result.put("jacksonParsed", vh.getTimestamp().toString());
            result.put("jacksonMatches", jacksonMatches);
            result.put("systemTimezone", TimeZone.getDefault().getID());
            result.put("dbStoredDirect", retrievedDirect.getTimestamp().toString());
            result.put("dbStoredJackson", retrievedJackson.getTimestamp().toString());
            
            if (!jacksonMatches) {
                long diff = vh.getTimestamp().getTime() - directTimestamp.getTime();
                result.put("conversionDetected", true);
                result.put("timeDifferenceMs", diff);
                result.put("timeDifferenceHours", diff / 3600000.0);
                result.put("rootCause", "JACKSON_OBJECTMAPPER_CONVERSION");
            }
            
            // Cleanup
            repository.deleteById(savedDirect.getId());
            repository.deleteById(savedJackson.getId());
            
        } catch (Exception e) {
            System.err.println("❌ RCA Error: " + e.getMessage());
            e.printStackTrace();
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        
        System.out.println("═══════════════════════════════════════════════════════");
        return result;
    }
    
    private String extractTimestampFromJson(String json) {
        // Simple regex to extract timestamp value
        String pattern = "\"timestamp\"\\s*:\\s*\"([^\"]+)\"";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(json);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }
    
    @GetMapping("/system-info")
    public Map<String, Object> getSystemInfo() {
        Map<String, Object> info = new HashMap<>();
        
        // System timezone info
        info.put("defaultTimezone", TimeZone.getDefault().getID());
        info.put("defaultZoneId", ZoneId.systemDefault().toString());
        info.put("jvmTimezone", System.getProperty("user.timezone"));
        info.put("currentTime", LocalDateTime.now().toString());
        
        // Spring/Jackson info
        info.put("objectMapperClass", objectMapper.getClass().getName());
        
        // JVM properties related to timezone
        info.put("userTimezone", System.getProperty("user.timezone"));
        info.put("javaTimezone", System.getProperty("java.time.zone"));
        
        return info;
    }
    
 // ================================================================================================
 // QUICK RCA CHECK - Add this to any existing controller or create a simple test
 // ================================================================================================

 @GetMapping("/quick-timezone-check")
 public Map<String, Object> quickTimezoneCheck() {
     Map<String, Object> result = new HashMap<>();
     
     System.out.println("🔍 QUICK TIMEZONE RCA CHECK");
     System.out.println("══════════════════════════════════════════");
     
     // 1. JVM Timezone Settings
     System.out.println("1. JVM TIMEZONE SETTINGS:");
     String jvmTimezone = System.getProperty("user.timezone");
     String javaTimezone = System.getProperty("java.time.zone");
     TimeZone defaultTz = TimeZone.getDefault();
     ZoneId defaultZone = ZoneId.systemDefault();
     
     System.out.println("   user.timezone: " + jvmTimezone);
     System.out.println("   java.time.zone: " + javaTimezone);
     System.out.println("   TimeZone.getDefault(): " + defaultTz.getID());
     System.out.println("   ZoneId.systemDefault(): " + defaultZone);
     
     result.put("jvmTimezone", jvmTimezone);
     result.put("javaTimezone", javaTimezone);
     result.put("defaultTimezone", defaultTz.getID());
     result.put("defaultZoneId", defaultZone.toString());
     
     // 2. Check if timezone is IST (which would cause 5.5 hour difference)
     boolean isIST = "Asia/Kolkata".equals(defaultZone.toString()) || 
                    "IST".equals(defaultTz.getID()) ||
                    "Asia/Calcutta".equals(defaultZone.toString());
     
     System.out.println("2. TIMEZONE ANALYSIS:");
     System.out.println("   Is Indian Standard Time: " + isIST);
     System.out.println("   Current offset from UTC: " + defaultTz.getRawOffset() / 3600000.0 + " hours");
     
     result.put("isIST", isIST);
     result.put("utcOffsetHours", defaultTz.getRawOffset() / 3600000.0);
     
     // 3. Test timestamp parsing
     System.out.println("3. TIMESTAMP PARSING TEST:");
     String testTimestamp = "2025-07-08 16:18:11";
     
     try {
         // Direct parsing (should work correctly)
         LocalDateTime direct = LocalDateTime.parse(testTimestamp, 
             DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
         Timestamp directTs = Timestamp.valueOf(direct);
         
         System.out.println("   Input: " + testTimestamp);
         System.out.println("   Direct LocalDateTime: " + direct);
         System.out.println("   Direct Timestamp: " + directTs);
         
         result.put("inputTimestamp", testTimestamp);
         result.put("directParsed", directTs.toString());
         
         // Check if Timestamp.toString() is applying timezone
         String timestampString = directTs.toString();
         boolean matches = testTimestamp.equals(timestampString.substring(0, 19));
         
         System.out.println("   Timestamp.toString(): " + timestampString);
         System.out.println("   Direct parsing correct: " + matches);
         
         result.put("timestampToString", timestampString);
         result.put("directParsingCorrect", matches);
         
         if (!matches) {
             System.out.println("   🚨 ISSUE: Timestamp.toString() is applying timezone conversion!");
             result.put("rootCauseFound", "TIMESTAMP_TOSTRING_CONVERSION");
         }
         
     } catch (Exception e) {
         System.out.println("   ❌ Error in timestamp parsing: " + e.getMessage());
         result.put("parsingError", e.getMessage());
     }
     
     // 4. Check ObjectMapper settings (if available)
     try {
         // You can inject ObjectMapper here if needed
         System.out.println("4. OBJECTMAPPER CHECK:");
         System.out.println("   (Run full RCA test for complete ObjectMapper analysis)");
         
     } catch (Exception e) {
         System.out.println("   ObjectMapper check failed: " + e.getMessage());
     }
     
     // 5. Potential root causes summary
     System.out.println("5. POTENTIAL ROOT CAUSES:");
     if (isIST) {
         System.out.println("   🚨 LIKELY CAUSE: JVM timezone is set to IST");
         System.out.println("   🚨 IST is UTC+5:30, which matches your 5.5 hour difference!");
         result.put("likelyRootCause", "JVM_TIMEZONE_IST");
         result.put("solution", "Set JVM timezone to UTC or disable timezone conversion");
     }
     
     System.out.println("══════════════════════════════════════════");
     
     return result;
 }

 // ================================================================================================
 // IMMEDIATE SOLUTIONS TO TRY:
 // ================================================================================================

 /*
 BASED ON RCA, TRY THESE SOLUTIONS IN ORDER:

 1. SET JVM TIMEZONE TO UTC:
    Add this to your application startup:
    -Duser.timezone=UTC

 2. OR SET TIMEZONE IN APPLICATION:
    Add this to your main() method:
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

 3. OR USE ZONE-NEUTRAL PARSING:
    Instead of Timestamp, use String for storage

 4. CHECK YOUR DOCKER/CONTAINER:
    If using Docker, set timezone:
    ENV TZ=UTC

 5. CHECK APPLICATION.PROPERTIES:
    Make sure these are set:
    spring.jackson.deserialization.adjust-dates-to-context-time-zone=false
    spring.jackson.time-zone=  (leave empty or remove)
 */

 // ================================================================================================
 // TEST THIS IMMEDIATELY:
 // ================================================================================================

 // Add this method to any controller and call it:
 @PostMapping("/test-timezone-fix")
 public String testTimezoneFix() {
     // Force JVM to UTC temporarily
     TimeZone originalTz = TimeZone.getDefault();
     
     try {
         System.out.println("Original timezone: " + originalTz.getID());
         
         // Set to UTC
         TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
         System.out.println("Changed to UTC");
         
         // Test timestamp parsing
         String input = "2025-07-08 16:18:11";
         LocalDateTime ldt = LocalDateTime.parse(input, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
         Timestamp ts = Timestamp.valueOf(ldt);
         
         System.out.println("Input: " + input);
         System.out.println("Timestamp: " + ts);
         System.out.println("Matches: " + input.equals(ts.toString().substring(0, 19)));
         
         return "Input: " + input + " -> Output: " + ts + " -> Matches: " + input.equals(ts.toString().substring(0, 19));
         
     } finally {
         // Restore original timezone
         TimeZone.setDefault(originalTz);
         System.out.println("Restored to: " + TimeZone.getDefault().getID());
     }
 }
}

// ================================================================================================
// STEP 2: MySQL Timezone Check Script
// ================================================================================================

/*
-- Run these SQL queries to check MySQL timezone settings:

-- Check MySQL timezone settings
SELECT @@global.time_zone as global_tz, @@session.time_zone as session_tz;

-- Check current MySQL time
SELECT NOW() as mysql_now, UTC_TIMESTAMP() as mysql_utc;

-- Test timestamp insertion and retrieval
CREATE TEMPORARY TABLE timestamp_test (
    id INT AUTO_INCREMENT PRIMARY KEY,
    test_timestamp DATETIME,
    test_timestamp_ts TIMESTAMP
);

-- Insert the exact timestamp
INSERT INTO timestamp_test (test_timestamp, test_timestamp_ts) 
VALUES ('2025-07-08 16:18:11', '2025-07-08 16:18:11');

-- Check what was stored
SELECT 
    test_timestamp,
    test_timestamp_ts,
    DATE_FORMAT(test_timestamp, '%Y-%m-%d %H:%i:%s') as datetime_formatted,
    DATE_FORMAT(test_timestamp_ts, '%Y-%m-%d %H:%i:%s') as timestamp_formatted
FROM timestamp_test;

-- Clean up
DROP TEMPORARY TABLE timestamp_test;
*/

// ================================================================================================
// STEP 3: JVM Arguments Check
// ================================================================================================

/*
Check your JVM startup arguments for timezone-related settings:

1. Look for: -Duser.timezone=
2. Look for: -Djava.time.zone=
3. Check application server timezone settings
4. Check Docker container timezone (if using Docker)

Common problematic JVM args:
-Duser.timezone=UTC
-Duser.timezone=GMT
-Djava.time.zone=UTC

If you find any of these, they might be causing the conversion.
*/

// ================================================================================================
// STEP 4: Application Server Check
// ================================================================================================

/*
If you're using an application server (Tomcat, etc.), check:

1. Server timezone configuration
2. CATALINA_OPTS environment variable
3. Container timezone settings
4. Operating system timezone

Commands to check:
- echo $TZ
- timedatectl (Linux)
- date
- cat /etc/timezone
*/