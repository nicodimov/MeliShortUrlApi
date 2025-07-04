package com.melishorturlapi.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.melishorturlapi.model.ShortUrl;
import com.melishorturlapi.service.ShortUrlService;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

@RestController
@RequestMapping("/")
public class ShortUrlRedirectController {

    @Autowired
    private MeterRegistry meterRegistry;

    @Autowired
    private ShortUrlService shortUrlService;
        // Redirect to the original URL
    @GetMapping("/{shortUrl}")
    public ResponseEntity<Void> redirectToOriginal(@PathVariable String shortUrl) {
        Counter counter = meterRegistry.counter("shorturl.endpoint.hits", "endpoint", "redirectToOriginal");
        counter.increment();
        ShortUrl url = shortUrlService.getShortUrl(shortUrl).get();
        return ResponseEntity.status(HttpStatus.FOUND).header("Location", url.getOriginalUrl()).build();
    }
}
