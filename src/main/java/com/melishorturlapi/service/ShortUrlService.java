package com.melishorturlapi.service;

import com.melishorturlapi.model.ShortUrl;
import com.melishorturlapi.repository.ShortUrlRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.Optional;
import java.util.function.Supplier;

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

    public ShortUrl createShortUrl(ShortUrl shortUrl) {
        ShortUrl result = shortUrlRepository.save(shortUrl);
        evictFromBothCaches(SHORT_URL_CACHE, shortUrl.getShortUrl());
        evictFromBothCaches(ORIGINAL_URL_CACHE, shortUrl.getOriginalUrl());
        return result;
    }

    public Optional<ShortUrl> getShortUrl(String shortUrl) {
        return getCachedOrFetch(SHORT_URL_CACHE, shortUrl, 
            () -> shortUrlRepository.findByShortUrl(shortUrl));
    }

    public Optional<ShortUrl> getShortUrlByOriginalUrl(String originalUrl) {
        return getCachedOrFetch(ORIGINAL_URL_CACHE, originalUrl, 
            () -> shortUrlRepository.findByOriginalUrl(originalUrl));
    }

    public void deleteShortUrl(String shortUrl) {
        ShortUrl url = shortUrlRepository.findByShortUrl(shortUrl);
        shortUrlRepository.deleteById(shortUrl);
        
        evictFromBothCaches(SHORT_URL_CACHE, shortUrl);
        if (url != null) {
            evictFromBothCaches(ORIGINAL_URL_CACHE, url.getOriginalUrl());
        }
    }

    public Optional<ShortUrl> getShortUrlStats(String shortUrl) {
        return shortUrlRepository.findById(shortUrl);
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

    private Optional<ShortUrl> getCachedOrFetch(String cacheName, String key, 
                                                Supplier<ShortUrl> fetcher) {
        // Try Caffeine cache first (L1)
        ShortUrl cached = getFromCache(caffeineCacheManager, cacheName, key);
        if (cached != null) {
            return Optional.of(cached);
        }

        // Try Redis cache (L2)
        cached = getFromCache(redisCacheManager, cacheName, key);
        if (cached != null) {
            // Populate L1 cache for next time
            putInCache(caffeineCacheManager, cacheName, key, cached);
            return Optional.of(cached);
        }

        // Fetch from database
        ShortUrl dbResult = fetcher.get();
        if (dbResult != null) {
            // Populate both caches
            putInCache(redisCacheManager, cacheName, key, dbResult);
            putInCache(caffeineCacheManager, cacheName, key, dbResult);
            return Optional.of(dbResult);
        }

        return Optional.empty();
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