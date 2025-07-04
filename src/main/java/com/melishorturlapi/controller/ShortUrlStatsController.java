package com.melishorturlapi.controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.melishorturlapi.service.MetricsService;

@RestController
public class ShortUrlStatsController {

    @Autowired
    private MetricsService metricsService;

    @GetMapping("/api/v1/stats/{shortUrl}")
    public ResponseEntity<String> getStats(@PathVariable String shortUrl) {
        metricsService.incrementEndpointHit("getStats", "statsService");
        return ResponseEntity.ok().body(shortUrl + " has 38 hits");
    }
}