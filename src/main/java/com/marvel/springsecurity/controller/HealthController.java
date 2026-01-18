package com.marvel.springsecurity.controller;

import com.github.benmanes.caffeine.cache.Cache;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Health check endpoints for monitoring application status.
 */
@Slf4j
@RestController
@RequestMapping("/api/health")
@Tag(name = "Health Check", description = "Application health monitoring endpoints")
public class HealthController {

    private final DataSource dataSource;

    @Autowired(required = false)
    private Cache<String, ?> rateLimitCache;

    public HealthController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Basic health check - returns 200 if application is running.
     */
    @GetMapping
    @Operation(summary = "Basic health check", description = "Returns OK if the application is running")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", Instant.now().toString());
        health.put("application", "Book Shelf API");

        log.debug("Health check performed - status: UP");
        return ResponseEntity.ok(health);
    }

    /**
     * Detailed health check - returns database and cache status (admin only).
     */
    @GetMapping("/detailed")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Detailed health check", description = "Returns detailed status including database and cache (Admin only)")
    public ResponseEntity<Map<String, Object>> detailedHealthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", Instant.now().toString());

        // Database health
        Map<String, Object> dbHealth = new HashMap<>();
        try (Connection conn = dataSource.getConnection()) {
            dbHealth.put("status", "UP");
            dbHealth.put("database", conn.getMetaData().getDatabaseProductName());
            dbHealth.put("version", conn.getMetaData().getDatabaseProductVersion());
        } catch (Exception e) {
            dbHealth.put("status", "DOWN");
            dbHealth.put("error", e.getMessage());
            health.put("status", "DEGRADED");
            log.error("Database health check failed", e);
        }
        health.put("database", dbHealth);

        // Cache health
        Map<String, Object> cacheHealth = new HashMap<>();
        if (rateLimitCache != null) {
            cacheHealth.put("status", "UP");
            cacheHealth.put("estimatedSize", rateLimitCache.estimatedSize());
        } else {
            cacheHealth.put("status", "NOT_CONFIGURED");
        }
        health.put("cache", cacheHealth);

        // Memory info
        Runtime runtime = Runtime.getRuntime();
        Map<String, Object> memory = new HashMap<>();
        memory.put("totalMB", runtime.totalMemory() / (1024 * 1024));
        memory.put("freeMB", runtime.freeMemory() / (1024 * 1024));
        memory.put("usedMB", (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024));
        memory.put("maxMB", runtime.maxMemory() / (1024 * 1024));
        health.put("memory", memory);

        log.info("Detailed health check performed - status: {}", health.get("status"));
        return ResponseEntity.ok(health);
    }
}
