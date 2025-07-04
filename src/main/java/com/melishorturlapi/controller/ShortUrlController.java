package com.melishorturlapi.controller;

import java.util.Optional;

import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.melishorturlapi.config.AppConfig;
import com.melishorturlapi.model.ShortUrl;
import com.melishorturlapi.model.UrlRequest;
import com.melishorturlapi.service.MetricsService;
import com.melishorturlapi.service.ShortUrlService;

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
    public ResponseEntity<String> createShortUrl(@RequestBody UrlRequest request) {
        metricsService.incrementEndpointHit("createShortUrl", "urlService");
        String originalUrl = request.getOriginalUrl();
        
        if (originalUrl == null || originalUrl.isEmpty()) {
            return ResponseEntity.badRequest().body("No es posible acortar una url vacia");
        }
        Optional<ShortUrl> url = shortUrlService.getShortUrlByOriginalUrl(originalUrl);
        if (url.isPresent()) {
            return ResponseEntity.ok("Short URL creada (existente): " + appConfig.getBaseShortUrl() + url.get().getShortUrl());
        }
        ShortUrl newShortUrl = new ShortUrl();
        newShortUrl.setOriginalUrl(originalUrl);
        newShortUrl.setShortUrl(shortUrlService.generateShortUrl(originalUrl));
        newShortUrl.setCreatedAt(DateTime.now().getMillis());
        newShortUrl.setRedirectCount(0L);
        shortUrlService.createShortUrl(newShortUrl);
        return ResponseEntity.ok("Short URL creada: "+ appConfig.getBaseShortUrl() + newShortUrl.getShortUrl());
    }

    @GetMapping("/view/{shortUrl}")
    public ResponseEntity<String> getOriginal(@PathVariable String shortUrl) {
        metricsService.incrementEndpointHit("getOriginal", "urlService");
        ShortUrl target = shortUrlService.getShortUrl(shortUrl).get();
        return ResponseEntity.ok("Url original: " + target.getOriginalUrl());
    }

    // Delete a short URL
    @DeleteMapping("/{shortUrl}")
    public ResponseEntity<String> deleteShortUrl(@PathVariable String shortUrl) {
        metricsService.incrementEndpointHit("deleteShortUrl", "urlService");
        try {
        shortUrlService.deleteShortUrl(shortUrl);
        return ResponseEntity.ok("Short URL eliminada");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error eliminando url");
        }
    }
}

