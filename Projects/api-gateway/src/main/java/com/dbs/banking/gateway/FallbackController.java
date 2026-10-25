package com.dbs.banking.gateway;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping("/accounts")
    public ResponseEntity<Map<String, Object>> accountServiceFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                "success", false,
                "message", "Account Service is currently unavailable. Request circuit opened.",
                "timestamp", Instant.now()
        ));
    }

    @PostMapping("/transfers")
    @GetMapping("/transfers")
    public ResponseEntity<Map<String, Object>> transferServiceFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                "success", false,
                "message", "Transfer Service is currently experiencing high latency or outages. Please retry shortly.",
                "timestamp", Instant.now()
        ));
    }
}
