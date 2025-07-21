package com.GpsTracker.Thinture.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;

@Configuration
@Profile({"dev", "prod"})
public class DatabaseTestConfig {

    @Autowired
    private DataSource dataSource;

    @Bean
    CommandLineRunner testDatabaseConnection() {
        return args -> {
            try (Connection connection = dataSource.getConnection()) {
                DatabaseMetaData metaData = connection.getMetaData();
                System.out.println("=====================================");
                System.out.println("DATABASE CONNECTION TEST");
                System.out.println("=====================================");
                System.out.println("Connected to: " + metaData.getURL());
                System.out.println("Database: " + metaData.getDatabaseProductName());
                System.out.println("Version: " + metaData.getDatabaseProductVersion());
                System.out.println("Driver: " + metaData.getDriverName());
                System.out.println("User: " + metaData.getUserName());
                System.out.println("=====================================");
                System.out.println("CONNECTION SUCCESSFUL!");
                System.out.println("=====================================");
            } catch (Exception e) {
                System.err.println("=====================================");
                System.err.println("DATABASE CONNECTION FAILED!");
                System.err.println("=====================================");
                System.err.println("Error: " + e.getMessage());
                System.err.println("=====================================");
                throw e;
            }
        };
    }
}