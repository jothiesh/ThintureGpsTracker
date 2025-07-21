package com.GpsTracker.Thinture.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.sql.Timestamp;

@Configuration
public class TimestampJacksonConfig {

    // ✅ NO @Primary annotation - won't conflict with existing ObjectMapper
    @Bean(name = "timestampObjectMapper")
    public ObjectMapper timestampObjectMapper() {
        System.out.println("🔧 Creating timestampObjectMapper for raw timestamp handling");
        
        ObjectMapper mapper = new ObjectMapper();
        
        // ✅ Disable timezone adjustments
        mapper.disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        // ✅ Register modules
        mapper.registerModule(new JavaTimeModule());
        
        // ✅ Add custom timestamp deserializer
        SimpleModule timestampModule = new SimpleModule("RawTimestampModule");
        timestampModule.addDeserializer(Timestamp.class, new RawTimestampDeserializer());
        mapper.registerModule(timestampModule);
        
        System.out.println("✅ timestampObjectMapper configured");
        return mapper;
    }
}