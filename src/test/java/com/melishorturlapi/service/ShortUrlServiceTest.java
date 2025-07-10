package com.melishorturlapi.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

import com.melishorturlapi.config.Resilience4jTestConfig;
import com.melishorturlapi.model.ShortUrl;
import com.melishorturlapi.repository.ShortUrlRepository;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;

@SpringBootTest
@Import(Resilience4jTestConfig.class)
class ShortUrlServiceTest {

    @Autowired
    private ShortUrlService shortUrlService;

    @MockBean
    private ShortUrlRepository shortUrlRepository;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

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
}
