
package com.GpsTracker.Thinture;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import java.time.LocalDateTime;
import java.util.TimeZone;
import javax.annotation.PostConstruct;
import java.sql.Timestamp;


@SpringBootApplication
@EnableScheduling
@EnableAsync
@EnableAspectJAutoProxy(proxyTargetClass = true)  // ← ADD THIS LINE
public class ThintureGpsTrackerApplication {

    public static void main(String[] args) {
        
        // ================================================================================================
        // 🔧 FIXED: RAW TIMESTAMP STORAGE SETUP - No timezone conversion
        // ================================================================================================
        System.out.println("📅 Setting up RAW timestamp storage...");
        System.out.println("🌍 Setting JVM timezone to UTC for consistency...");
        
        // Set JVM timezone to UTC for consistency (but no conversion in app)
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        System.setProperty("user.timezone", "UTC");
        
        // Verify timezone setting
        System.out.println("🌍 JVM Timezone set to: " + TimeZone.getDefault().getID());
        System.out.println("🌍 System property: " + System.getProperty("user.timezone"));
        
        System.out.println("═══════════════════════════════════════");
        System.out.println("📅 RAW TIMESTAMP STORAGE CONFIGURED");
        System.out.println("═══════════════════════════════════════");
        System.out.println("Storage Mode: RAW (No conversion)");
        System.out.println("Dubai device: Stores Dubai time AS-IS");
        System.out.println("India device: Stores India time AS-IS");
        System.out.println("Kenya device: Stores Kenya time AS-IS");
        System.out.println("JVM Timezone: " + TimeZone.getDefault().getID() + " (for consistency only)");
        System.out.println("═══════════════════════════════════════");
        
        SpringApplication.run(ThintureGpsTrackerApplication.class, args);
    }

    // ✅ FIXED: Verify RAW timestamp storage after Spring context loads
    @PostConstruct
    public void verifyRawTimestampConfiguration() {
        System.out.println("═══════════════════════════════════════");
        System.out.println("🧪 RAW TIMESTAMP STORAGE VERIFICATION");
        System.out.println("═══════════════════════════════════════");
        System.out.println("JVM Default Timezone: " + TimeZone.getDefault().getID());
        System.out.println("System Property: " + System.getProperty("user.timezone"));
        System.out.println("Current Server Time: " + LocalDateTime.now());
        
        // Test RAW timestamp parsing
        testRawTimestampParsing();
        
        System.out.println("═══════════════════════════════════════");
    }

    // ✅ FIXED: Test RAW timestamp parsing (no conversion)
    private void testRawTimestampParsing() {
        try {
            // Test cases for different device times
            String[] testCases = {
                "2025-07-09 08:15:31",  // Dubai time (UTC+4)
                "2025-07-09 13:45:31",  // India time (UTC+5:30)
                "2025-07-09 11:20:31"   // Kenya time (UTC+3)
            };
            
            System.out.println("🧪 RAW TIMESTAMP TESTS:");
            
            for (String testInput : testCases) {
                // Test exactly how DataTransformer does it
                LocalDateTime ldt = LocalDateTime.parse(testInput,
                    java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                Timestamp result = Timestamp.valueOf(ldt);
                
                boolean isRawStorage = testInput.equals(result.toString().substring(0, 19));
                
                System.out.println("   Input: " + testInput);
                System.out.println("   Stored: " + result.toString().substring(0, 19));
                System.out.println("   RAW Storage: " + (isRawStorage ? "✅ YES" : "❌ NO"));
                System.out.println();
            }
            
            System.out.println("🎯 EXPECTED BEHAVIOR:");
            System.out.println("   ✅ All inputs should store EXACTLY as received");
            System.out.println("   ✅ No timezone conversion should occur");
            System.out.println("   ✅ Dubai 08:15 stores as 08:15");
            System.out.println("   ✅ India 13:45 stores as 13:45");
            System.out.println("   ✅ Kenya 11:20 stores as 11:20");
            
        } catch (Exception e) {
            System.out.println("❌ Error in RAW timestamp test: " + e.getMessage());
        }
    }
}