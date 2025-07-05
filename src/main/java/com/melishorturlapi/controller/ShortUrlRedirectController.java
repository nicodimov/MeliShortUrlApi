package com.melishorturlapi.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.melishorturlapi.service.MetricsService;
import com.melishorturlapi.service.ShortUrlService;

import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/")
public class ShortUrlRedirectController {

    @Autowired
    private MetricsService metricsService;

    @Autowired
    private ShortUrlService shortUrlService;
        // Redirect to the original URL
    @GetMapping("/{shortUrl}")
    public Mono<ResponseEntity<Void>> redirectToOriginal(@PathVariable String shortUrl) {
        metricsService.incrementEndpointHit("redirectToOriginal", "shortUrlService");
        return shortUrlService.getShortUrl(shortUrl)
        .map(url -> {
            metricsService.incrementUrlCall(url.getShortUrl());
            return ResponseEntity.status(HttpStatus.FOUND).header("Location", url.getOriginalUrl()).build();
        });
    }
}
