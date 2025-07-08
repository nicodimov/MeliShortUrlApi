package com.melishorturlapi.controller;

import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.melishorturlapi.config.AppConfig;
import com.melishorturlapi.model.ShortUrl;
import com.melishorturlapi.model.UrlRequest;
import com.melishorturlapi.service.MetricsService;
import com.melishorturlapi.service.ShortUrlService;

import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/shorturl")
public class ShortUrlController {

    @Autowired
    private ShortUrlService shortUrlService;

    @Autowired
    private AppConfig appConfig;

    @Autowired
    private MetricsService metricsService;

    @PostMapping
    public Mono<ResponseEntity<String>> createShortUrl(@RequestBody UrlRequest request) {
        metricsService.incrementEndpointHit("createShortUrl", "urlService");
        String originalUrl = request.getOriginalUrl();
        
        if (originalUrl == null || originalUrl.isEmpty()) {
            return Mono.just(ResponseEntity.badRequest().body("No es posible acortar una url vacia"));
        }
        return shortUrlService.getShortUrlByOriginalUrl(originalUrl)
            .flatMap(url -> Mono.just(ResponseEntity.ok("Short URL creada (existente): " + appConfig.getBaseShortUrl() + url.getShortUrl())))
            .switchIfEmpty(
                Mono.defer(() -> {
                    ShortUrl newShortUrl = new ShortUrl();
                    newShortUrl.setOriginalUrl(originalUrl);
                    newShortUrl.setShortUrl(shortUrlService.generateShortUrl(originalUrl));
                    newShortUrl.setCreatedAt(DateTime.now().getMillis());
                    newShortUrl.setRedirectCount(0L);
                    return shortUrlService.createShortUrl(newShortUrl)
                        .map(saved -> {
                            metricsService.incrementShortUrlCreated();
                            return ResponseEntity.ok("Short URL creada: " + appConfig.getBaseShortUrl() + saved.getShortUrl());
                        });
                })
            );
    }

    @GetMapping("/view/{shortUrl}")
    public Mono<ResponseEntity<String>> getOriginal(@PathVariable String shortUrl) {
        metricsService.incrementEndpointHit("urlService", "getOriginal");
        return shortUrlService.getShortUrl(shortUrl)
            .map(t -> ResponseEntity.ok("Url original: " + t.getOriginalUrl()))
            .switchIfEmpty(Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).body("El codigo no corresponde a una url acortada")));
    }

    // Delete a short URL
    @DeleteMapping("/{shortUrl}")
    public Mono<ResponseEntity<String>> deleteShortUrl(@PathVariable String shortUrl) {
        metricsService.incrementEndpointHit("urlService", "deleteShortUrl");
        try {
        shortUrlService.deleteShortUrl(shortUrl);
        return Mono.just(ResponseEntity.ok("Short URL eliminada"));
        } catch (Exception e) {
            return Mono.just(ResponseEntity.badRequest().body("Error eliminando url"));
        }
    }
}

