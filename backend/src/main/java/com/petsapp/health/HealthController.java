package com.petsapp.health;

import com.petsapp.common.ApiResponse;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpointy do sprawdzania stanu aplikacji.
 *
 * GET /health       — ogolny status (czy aplikacja dziala)
 * GET /health/ready — readiness check (czy aplikacja jest gotowa na ruch)
 *
 * Oba endpointy sa publiczne (bez uwierzytelnienia) — uzywane przez load balancer.
 */
@RestController
@RequestMapping("/health")
public class HealthController {

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, String>>> health() {
        return ResponseEntity.ok(ApiResponse.ok(Map.of("status", "UP")));
    }

    @GetMapping("/ready")
    public ResponseEntity<ApiResponse<Map<String, String>>> ready() {
        return ResponseEntity.ok(ApiResponse.ok(Map.of("status", "READY")));
    }
}
