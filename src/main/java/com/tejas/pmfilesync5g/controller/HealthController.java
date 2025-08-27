package com.tejas.pmfilesync5g.controller;

import com.tejas.pmfilesync5g.service.MessageProcessorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/health")
@RequiredArgsConstructor
public class HealthController {

    private final MessageProcessorService messageProcessorService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "top-5g-pm-file-sync");
        health.put("timestamp", System.currentTimeMillis());
        
        Map<String, Object> kafka = new HashMap<>();
        kafka.put("status", "UP");
        kafka.put("totalMessagesProcessed", messageProcessorService.getTotalMessagesProcessed());
        kafka.put("processingErrors", messageProcessorService.getProcessingErrors());
        
        health.put("kafka", kafka);
        return ResponseEntity.ok(health);
    }

    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> metrics() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("totalMessagesProcessed", messageProcessorService.getTotalMessagesProcessed());
        metrics.put("processingErrors", messageProcessorService.getProcessingErrors());
        
        long totalMessages = messageProcessorService.getTotalMessagesProcessed();
        long errors = messageProcessorService.getProcessingErrors();
        double errorRate = totalMessages > 0 ? (double) errors / totalMessages : 0.0;
        metrics.put("errorRate", errorRate);
        
        return ResponseEntity.ok(metrics);
    }
}