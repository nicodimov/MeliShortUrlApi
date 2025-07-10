package com.melishorturlapi.controller;

import com.melishorturlapi.model.ShortUrl;
import com.melishorturlapi.service.MetricsService;
import com.melishorturlapi.service.ShortUrlService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShortUrlRedirectControllerTest {

    @Mock
    private MetricsService metricsService;

    @Mock
    private ShortUrlService shortUrlService;

    @InjectMocks
    private ShortUrlRedirectController controller;

    private static final String SHORT_URL_CODE = "abc123";
    private static final String ORIGINAL_URL = "https://www.mercadolibre.com.ar/";

    @BeforeEach
    void setUp() {
        // No-op, mocks are injected
    }

    @Test
    void redirectToOriginal_WithValidShortUrl_ShouldRedirect() {
        ShortUrl shortUrl = new ShortUrl();
        shortUrl.setShortUrl(SHORT_URL_CODE);
        shortUrl.setOriginalUrl(ORIGINAL_URL);
        when(shortUrlService.getShortUrl(SHORT_URL_CODE)).thenReturn(Mono.just(shortUrl));

        Mono<ResponseEntity<Void>> response = controller.redirectToOriginal(SHORT_URL_CODE);

        StepVerifier.create(response)
            .assertNext(resp -> {
                assertEquals(HttpStatus.FOUND, resp.getStatusCode());
                assertTrue(resp.getHeaders().containsKey("Location"));
                assertEquals(ORIGINAL_URL, resp.getHeaders().getFirst("Location"));
            })
            .verifyComplete();

        verify(metricsService).incrementEndpointHit("shortUrlService", "redirectToOriginal");
        verify(metricsService).incrementRedirectCalls(SHORT_URL_CODE);
        verify(shortUrlService).getShortUrl(SHORT_URL_CODE);
    }

    @Test
    void redirectToOriginal_WithNonExistentShortUrl_ShouldReturnEmpty() {
        when(shortUrlService.getShortUrl(SHORT_URL_CODE)).thenReturn(Mono.empty());

        Mono<ResponseEntity<Void>> response = controller.redirectToOriginal(SHORT_URL_CODE);

        StepVerifier.create(response)
            .expectNextCount(1)
            .verifyComplete();

        verify(metricsService).incrementEndpointHit("shortUrlService", "redirectToOriginal");
        verify(shortUrlService).getShortUrl(SHORT_URL_CODE);
        verify(metricsService, never()).incrementRedirectCalls(anyString());
    }
} 