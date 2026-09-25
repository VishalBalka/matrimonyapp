package com.matrimonyapp.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
@Tag(name = "Health", description = "Operational system and database health check")
public class HealthController {

    private final DataSource dataSource;
    private final Instant startTime = Instant.now();

    public HealthController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping
    @Operation(summary = "Health check", description = "Report operational status, database connectivity, and uptime")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "UP");
        response.put("service", "matrimony-backend");
        response.put("version", "1.0.0");
        response.put("timestamp", Instant.now().toString());
        response.put("uptimeSeconds", Instant.now().getEpochSecond() - startTime.getEpochSecond());

        boolean dbConnected = false;
        try (Connection connection = dataSource.getConnection()) {
            dbConnected = connection.isValid(2);
        } catch (Exception ignored) {
        }

        Map<String, Object> components = new LinkedHashMap<>();
        components.put("database", dbConnected ? "UP" : "DEGRADED");
        response.put("components", components);

        return ResponseEntity.ok(response);
    }
}
