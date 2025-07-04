package com.melishorturlapi.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MetricsService {

    @Autowired
    private MeterRegistry meterRegistry;

    public void incrementEndpointHit(String endpointName, String serviceName) {
        Counter counter = meterRegistry.counter("shorturl.endpoint.hits", serviceName, endpointName);
        counter.increment();
    }
}