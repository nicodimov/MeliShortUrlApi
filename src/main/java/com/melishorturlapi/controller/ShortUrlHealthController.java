package com.melishorturlapi.controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.melishorturlapi.service.MetricsService;

@RestController
public class ShortUrlHealthController {

    @Autowired
    private MetricsService metricsService;
    @GetMapping("/api/v1/health/ping")
    public String ping() {
        metricsService.incrementEndpointHit("ping", "healthService");
        return "MeliShortUrlApi is running!";
    }
}