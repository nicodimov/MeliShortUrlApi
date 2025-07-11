package com.melishorturlapi.service;

import com.melishorturlapi.model.ShortUrl;
import com.melishorturlapi.repository.ShortUrlRepository;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;

@Service
public class ShortUrlService {

    private static final String SHORT_URL_CACHE = "shortUrlCache";
    private static final String ORIGINAL_URL_CACHE = "shortUrlByOriginalCache";

    @Autowired
    private ShortUrlRepository shortUrlRepository;

    @Autowired
    @Qualifier("caffeineCacheManager")
    private CacheManager caffeineCacheManager;

    @Autowired
    @Qualifier("redisCacheManager")
    private CacheManager redisCacheManager;

    @Autowired
    private UrlHashService urlHashService;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;


    private static final Logger logger = LoggerFactory.getLogger(ShortUrlService.class);

    
    public Mono<ShortUrl> createShortUrlCB(ShortUrl shortUrl) {
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("shortUrlService");
    
        return Mono.fromCallable(() -> {
                ShortUrl result = shortUrlRepository.save(shortUrl);
                evictFromBothCaches(SHORT_URL_CACHE, shortUrl.getShortUrl());
                evictFromBothCaches(ORIGINAL_URL_CACHE, shortUrl.getOriginalUrl());
                return result;
            })
            .transformDeferred(CircuitBreakerOperator.of(cb))
            // Fallback solo si el CircuitBreaker dejó pasar y falló
            .onErrorResume(throwable -> {
                // ⚠️ Solo propagamos error si queremos que cuente como fallo
                // return Mono.error(throwable);
    
                // ✅ Si querés fallback "suave" pero que el CB registre la excepción
                logger.warn("Fallback ejecutado tras fallo: {}", throwable.toString());
                return fallbackGetShortUrl(shortUrl.getShortUrl(), throwable);
            })
            .subscribeOn(Schedulers.boundedElastic());
    }
    

    @io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker(name = "shortUrlService", fallbackMethod = "fallbackGetShortUrl")
    public Mono<ShortUrl> createShortUrl(ShortUrl shortUrl) {
        return Mono.fromCallable(() -> {
            ShortUrl result = shortUrlRepository.save(shortUrl);
            evictFromBothCaches(SHORT_URL_CACHE, shortUrl.getShortUrl());
            evictFromBothCaches(ORIGINAL_URL_CACHE, shortUrl.getOriginalUrl());
            return result;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker(name = "shortUrlService", fallbackMethod = "fallbackGetShortUrl")
    public Mono<ShortUrl> getShortUrl(String shortUrl) {
        logger.info("[getShortUrl] Called with shortUrl: {}", shortUrl);
        return getCachedOrFetch(SHORT_URL_CACHE, shortUrl, 
            () -> Mono.fromCallable(() -> shortUrlRepository.findByShortUrl(shortUrl)).subscribeOn(Schedulers.boundedElastic()));
    }

    public Mono<ShortUrl> getShortUrlByOriginalUrl(String originalUrl) {
        return getCachedOrFetch(ORIGINAL_URL_CACHE, originalUrl, 
            () -> Mono.fromCallable(() -> shortUrlRepository.findByOriginalUrl(originalUrl)).subscribeOn(Schedulers.boundedElastic()));
    }

    public Mono<Void> deleteShortUrl(String shortUrl) {
        return Mono.fromRunnable(() -> {
            ShortUrl url = shortUrlRepository.findByShortUrl(shortUrl);
            shortUrlRepository.deleteById(shortUrl);
            evictFromBothCaches(SHORT_URL_CACHE, shortUrl);
            if (url != null) {
                evictFromBothCaches(ORIGINAL_URL_CACHE, url.getOriginalUrl());
            }
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    public Mono<ShortUrl> getShortUrlStats(String shortUrl) {
        return Mono.fromCallable(() -> shortUrlRepository.findById(shortUrl).orElse(null)).subscribeOn(Schedulers.boundedElastic());
    }

    public String generateShortUrl(String originalUrl) {
        String code = urlHashService.hashUrl(originalUrl);
        int attempt = 1;
        while (shortUrlRepository.findByShortUrl(code) != null) {
            code = urlHashService.hashUrl(originalUrl + ":" + attempt);
            attempt++;
        }
        return code;
    }

    public Mono<ShortUrl> fallbackGetShortUrl(String shortUrl, Throwable t) {
        logger.info("fallbackGetShortUrl: "+ t.getMessage());
        return Mono.error(t);
    }

    private Mono<ShortUrl> getCachedOrFetch(String cacheName, String key, java.util.function.Supplier<Mono<ShortUrl>> fetcher) {
        // Try Caffeine cache first (L1)
        ShortUrl cached = getFromCache(caffeineCacheManager, cacheName, key);
        if (cached != null) {
            logger.info("[getCachedOrFetch] L1 cache hit for key: {} in cache: {}", key, cacheName);
            return Mono.just(cached);
        }

        // Try Redis cache (L2)
        cached = getFromCache(redisCacheManager, cacheName, key);
        if (cached != null) {
            logger.info("[getCachedOrFetch] L2 cache hit for key: {} in cache: {}", key, cacheName);
            // Populate L1 cache for next time
            putInCache(caffeineCacheManager, cacheName, key, cached);
            return Mono.just(cached);
        }

        logger.info("[getCachedOrFetch] Cache miss for key: {} in cache: {}. Fetching from DB...", key, cacheName);
        // Fetch from database reactively
        return fetcher.get()
            .doOnNext(dbResult -> {
                logger.info("[getCachedOrFetch] DB fetch result for key: {} in cache: {}: {}", key, cacheName, dbResult != null ? "FOUND" : "NOT FOUND");
                // Populate both caches
                putInCache(redisCacheManager, cacheName, key, dbResult);
                putInCache(caffeineCacheManager, cacheName, key, dbResult);
            });
    }

    private void evictFromBothCaches(String cacheName, String key) {
        evictFromCache(caffeineCacheManager, cacheName, key);
        evictFromCache(redisCacheManager, cacheName, key);
    }

    private ShortUrl getFromCache(CacheManager cacheManager, String cacheName, String key) {
        Cache cache = cacheManager.getCache(cacheName);
        return cache != null ? cache.get(key, ShortUrl.class) : null;
    }

    private void putInCache(CacheManager cacheManager, String cacheName, String key, ShortUrl value) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.put(key, value);
        }
    }

    private void evictFromCache(CacheManager cacheManager, String cacheName, String key) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.evict(key);
        }
    }
}