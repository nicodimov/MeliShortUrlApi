package com.melishorturlapi.service;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import org.springframework.stereotype.Service;

@Service
public class MetricsService {

    private final Meter meter = GlobalOpenTelemetry.getMeter("com.melishorturlapi");
    private final LongCounter redirectCounter = meter.counterBuilder("shorturl_redirect")
        .setDescription("Number of redirects per shortUrl")
        .setUnit("1")
        .build();

    private final LongCounter viweCounter = meter.counterBuilder("shorturl_view")
        .setDescription("Number of views per shortUrl")
        .setUnit("1")
        .build();

    private final LongCounter endpointHitCounter = meter.counterBuilder("shorturl_endpoint")
        .setDescription("Endpoint Hit Counter")
        .setUnit("1")
        .build();

    private final LongCounter createShorturlCounter = meter.counterBuilder("shorturl_create")
        .setDescription("Create new short Url Counter")
        .setUnit("1")
        .build();

    public void incrementRedirectCalls(String shortUrl) {
        redirectCounter.add(1, Attributes.of(AttributeKey.stringKey("shortUrl"), shortUrl));
    }

    public void incrementEndpointHit(String service, String endpointName) {
        endpointHitCounter.add(1, Attributes.of(
            AttributeKey.stringKey("service"), service,
            AttributeKey.stringKey("endpoint"), endpointName
        ));
    }

    public void incrementViewUrl(String shortUrl, String originalUrl) {
        viweCounter.add(1, Attributes.of(AttributeKey.stringKey("shortUrl"), shortUrl));
        viweCounter.add(1, Attributes.of(AttributeKey.stringKey("originalUrl"), originalUrl));
    }

    public void incrementShortUrlCreated() {
        createShorturlCounter.add(1, Attributes.of(AttributeKey.stringKey("event"), "shorturl_created"));
    }
}