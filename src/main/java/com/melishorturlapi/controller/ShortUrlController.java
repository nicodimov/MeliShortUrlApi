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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

@RestController
@RequestMapping("/api/v1/shorturl")
public class ShortUrlController {
    private Logger logger = LoggerFactory.getLogger(ShortUrlController.class);

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
        
        if (originalUrl == null || originalUrl.trim() == null || originalUrl.trim().isEmpty()) {
            return Mono.just(ResponseEntity.badRequest().body("No es posible acortar una url vacia"));
        }
        // Validate URL format
        try {
            URI uri = new URI(originalUrl);
            uri.toURL();
        } catch (URISyntaxException | MalformedURLException | IllegalArgumentException e) {
            return Mono.just(ResponseEntity.badRequest().body("Formato de URL invalido"));
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
        logger.info("[getOriginal] Received request for shortUrl: {}", shortUrl);
        metricsService.incrementEndpointHit("urlService", "getOriginal");
        return shortUrlService.getShortUrl(shortUrl)
            .map(t -> {
                logger.info("[getOriginal] Found shortUrl: {} -> original: {}", shortUrl, t.getOriginalUrl());
                return ResponseEntity.ok("Url original: " + t.getOriginalUrl());
            })
            .switchIfEmpty(Mono.fromCallable(() -> {
                logger.warn("[getOriginal] shortUrl not found: {}", shortUrl);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("El codigo no corresponde a una url acortada");
            }));
    }

    // Delete a short URL
    @DeleteMapping("/{shortUrl}")
    public Mono<ResponseEntity<String>> deleteShortUrl(@PathVariable String shortUrl) {
        metricsService.incrementEndpointHit("urlService", "deleteShortUrl");
        return shortUrlService.deleteShortUrl(shortUrl)
            .thenReturn(ResponseEntity.ok("Short URL eliminada"))
            .onErrorResume(e -> Mono.just(ResponseEntity.badRequest().body("Error eliminando url")));
    }
}

