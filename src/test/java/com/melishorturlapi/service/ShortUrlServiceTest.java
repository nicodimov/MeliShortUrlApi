package com.melishorturlapi.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import org.springframework.cache.CacheManager;
import org.springframework.boot.test.mock.mockito.MockBean;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import com.melishorturlapi.config.Resilience4jTestConfig;
import com.melishorturlapi.model.ShortUrl;
import com.melishorturlapi.repository.ShortUrlRepository;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import reactor.test.StepVerifier;

@SpringBootTest
@Import(Resilience4jTestConfig.class)
class ShortUrlServiceTest {

    @Autowired
    private ShortUrlService shortUrlService;

    @MockBean
    private ShortUrlRepository shortUrlRepository;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @MockBean(name = "caffeineCacheManager")
    private CacheManager caffeineCacheManager;

    @MockBean(name = "redisCacheManager")
    private CacheManager redisCacheManager;

    @MockBean
    private UrlHashService urlHashService;

    @Test
    void circuitBreakerOpensAfterFailures() throws InterruptedException {
        ShortUrl dummy = new ShortUrl();

        // Simulamos error al guardar
        when(shortUrlRepository.save(any()))
            .thenThrow(new RuntimeException("DB down"));

        // Forzamos errores suficientes para abrir el CB
        for (int i = 0; i < 60; i++) {
            try {
                shortUrlService.createShortUrlCB(dummy).block();
            } catch (Exception e) {
                System.out.println("Llamada " + i + " fallo: " + e.getMessage());
            }
        }
    

        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("shortUrlService");

        System.out.println("CB STATE: " + cb.getState());
        System.out.println("Failures: " + cb.getMetrics().getNumberOfFailedCalls());
        System.out.println("Failure rate: " + cb.getMetrics().getFailureRate());

        assertEquals(CircuitBreaker.State.OPEN, cb.getState(), "Circuit Breaker should be OPEN");
    }

    @Test
    void createShortUrl_success() {
        ShortUrl dummy = new ShortUrl();
        when(shortUrlRepository.save(any())).thenReturn(dummy);

        StepVerifier.create(shortUrlService.createShortUrl(dummy))
            .expectNext(dummy)
            .verifyComplete();
    }

    @Test
    void createShortUrl_fallback() {
        ShortUrl dummy = new ShortUrl();
        when(shortUrlRepository.save(any())).thenThrow(new RuntimeException("DB error"));

        StepVerifier.create(shortUrlService.createShortUrl(dummy))
            .expectErrorMatches(e -> e instanceof RuntimeException && e.getMessage().contains("DB error"))
            .verify();
    }

    @Test
    void getShortUrl_fromDb() {
        ShortUrl dummy = new ShortUrl();
        dummy.setShortUrl("abc");
        when(shortUrlRepository.findByShortUrl("abc")).thenReturn(dummy);

        StepVerifier.create(shortUrlService.getShortUrl("abc"))
            .expectNext(dummy)
            .verifyComplete();
    }

    @Test
    void getShortUrlByOriginalUrl_fromDb() {
        ShortUrl dummy = new ShortUrl();
        dummy.setOriginalUrl("http://test.com");
        when(shortUrlRepository.findByOriginalUrl("http://test.com")).thenReturn(dummy);

        StepVerifier.create(shortUrlService.getShortUrlByOriginalUrl("http://test.com"))
            .expectNext(dummy)
            .verifyComplete();
    }

    @Test
    void deleteShortUrl_removesFromRepoAndCache() {
        ShortUrl dummy = new ShortUrl();
        dummy.setShortUrl("abc");
        dummy.setOriginalUrl("http://test.com");
        when(shortUrlRepository.findByShortUrl("abc")).thenReturn(dummy);
        doNothing().when(shortUrlRepository).deleteById("abc");

        StepVerifier.create(shortUrlService.deleteShortUrl("abc"))
            .verifyComplete();
        verify(shortUrlRepository).deleteById("abc");
    }

    @Test
    void generateShortUrl_unique() {
        when(urlHashService.hashUrl(anyString())).thenReturn("abc", "abc1");
        when(shortUrlRepository.findByShortUrl("abc")).thenReturn(new ShortUrl());
        when(shortUrlRepository.findByShortUrl("abc1")).thenReturn(null);

        String code = shortUrlService.generateShortUrl("http://test.com");
        assertEquals("abc1", code);
    }

    @Test
    void getShortUrlStats_returnsStats() {
        ShortUrl dummy = new ShortUrl();
        when(shortUrlRepository.findById("abc")).thenReturn(java.util.Optional.of(dummy));

        StepVerifier.create(shortUrlService.getShortUrlStats("abc"))
            .expectNext(dummy)
            .verifyComplete();
    }
}
