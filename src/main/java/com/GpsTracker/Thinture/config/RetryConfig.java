package com.GpsTracker.Thinture.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

/**
 * Enables Spring Retry for the application
 * Required for @Retryable annotations in persistence services
 */
@Configuration
@EnableRetry
public class RetryConfig {
    // Configuration is handled via annotations
    // @Retryable annotations in services will now work
}