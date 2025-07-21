package com.GpsTracker.Thinture.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.sql.Timestamp;

@Configuration
public class ObjectMapperCustomizer {
	@Bean
	@Primary  // Keep this as primary
	public ObjectMapper gpsOptimizedObjectMapper() {
	    ObjectMapper mapper = new ObjectMapper()
	        .registerModule(new SimpleModule()
	            .addDeserializer(Timestamp.class, new RawTimestampDeserializer())) // ADD THIS LINE
	        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
	        .disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE) // ADD THIS
	        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS); // ADD THIS
	    
	    System.out.println("📦 GPS-optimized ObjectMapper configured with raw timestamp support");
	    return mapper;
	}
}