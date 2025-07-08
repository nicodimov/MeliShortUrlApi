package com.melishorturlapi.controller;

import com.melishorturlapi.config.AppConfig;
import com.melishorturlapi.model.ShortUrl;
import com.melishorturlapi.model.UrlRequest;
import com.melishorturlapi.service.MetricsService;
import com.melishorturlapi.service.ShortUrlService;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ShortUrlControllerTest {

    @Mock
    private ShortUrlService shortUrlService;

    @Mock
    private AppConfig appConfig;

    @Mock
    private MetricsService metricsService;

    @InjectMocks
    private ShortUrlController shortUrlController;

    private static final String BASE_URL = "http://localhost:8080/";
    private static final String ORIGINAL_URL = "https://www.mercadolibre.com.ar/";
    private static final String SHORT_URL_CODE = "abc123";

    @BeforeEach
    void setUp() {
        when(appConfig.getBaseShortUrl()).thenReturn(BASE_URL);
    }

    @Test
    void createShortUrl_WithValidUrl_ShouldCreateNewShortUrl() {
        UrlRequest request = new UrlRequest();
        request.setOriginalUrl(ORIGINAL_URL);

        when(shortUrlService.getShortUrlByOriginalUrl(ORIGINAL_URL)).thenReturn(Mono.empty());
        when(shortUrlService.generateShortUrl(ORIGINAL_URL)).thenReturn(SHORT_URL_CODE);
        when(shortUrlService.createShortUrl(any(ShortUrl.class))).thenAnswer(invocation -> {
            ShortUrl shortUrl = invocation.getArgument(0);
            return Mono.just(shortUrl);
        });

        Mono<ResponseEntity<String>> response = shortUrlController.createShortUrl(request);

        StepVerifier.create(response)
            .assertNext(resp -> {
                assertEquals(HttpStatus.OK, resp.getStatusCode());
                assertEquals("Short URL creada: " + BASE_URL + SHORT_URL_CODE, resp.getBody());
            })
            .verifyComplete();
        
        verify(shortUrlService).getShortUrlByOriginalUrl(ORIGINAL_URL);
        verify(shortUrlService).generateShortUrl(ORIGINAL_URL);
        verify(shortUrlService).createShortUrl(any(ShortUrl.class));
    }

    @Test
    void createShortUrl_WithExistingUrl_ShouldReturnExistingShortUrl() {
        UrlRequest request = new UrlRequest();
        request.setOriginalUrl(ORIGINAL_URL);

        ShortUrl existingShortUrl = new ShortUrl();
        existingShortUrl.setOriginalUrl(ORIGINAL_URL);
        existingShortUrl.setShortUrl(SHORT_URL_CODE);

        when(shortUrlService.getShortUrlByOriginalUrl(ORIGINAL_URL)).thenReturn(Mono.just(existingShortUrl));

        Mono<ResponseEntity<String>> response = shortUrlController.createShortUrl(request);

        StepVerifier.create(response)
            .assertNext(resp -> {
                assertEquals(HttpStatus.OK, resp.getStatusCode());
                assertEquals("Short URL creada (existente): " + BASE_URL + SHORT_URL_CODE, resp.getBody());
            })
            .verifyComplete();
        
        verify(shortUrlService).getShortUrlByOriginalUrl(ORIGINAL_URL);
        verify(shortUrlService, never()).generateShortUrl(anyString());
        verify(shortUrlService, never()).createShortUrl(any(ShortUrl.class));
    }

    @Test
    void createShortUrl_WithNullUrl_ShouldReturnBadRequest() {
        UrlRequest request = new UrlRequest();
        request.setOriginalUrl(null);

        Mono<ResponseEntity<String>> response = shortUrlController.createShortUrl(request);

        StepVerifier.create(response)
            .assertNext(resp -> {
                assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
                assertEquals("No es posible acortar una url vacia", resp.getBody());
            })
            .verifyComplete();
        
        verify(shortUrlService, never()).getShortUrlByOriginalUrl(anyString());
        verify(shortUrlService, never()).generateShortUrl(anyString());
        verify(shortUrlService, never()).createShortUrl(any(ShortUrl.class));
    }

    @Test
    void createShortUrl_WithEmptyUrl_ShouldReturnBadRequest() {
        UrlRequest request = new UrlRequest();
        request.setOriginalUrl("");

        Mono<ResponseEntity<String>> response = shortUrlController.createShortUrl(request);

        StepVerifier.create(response)
            .assertNext(resp -> {
                assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
                assertEquals("No es posible acortar una url vacia", resp.getBody());
            })
            .verifyComplete();
        
        verify(shortUrlService, never()).getShortUrlByOriginalUrl(anyString());
        verify(shortUrlService, never()).generateShortUrl(anyString());
        verify(shortUrlService, never()).createShortUrl(any(ShortUrl.class));
    }

    @Test
    void createShortUrl_WithWhitespaceUrl_ShouldReturnBadRequest() {
        UrlRequest request = new UrlRequest();
        request.setOriginalUrl("   ");

        Mono<ResponseEntity<String>> response = shortUrlController.createShortUrl(request);

        StepVerifier.create(response)
            .assertNext(resp -> {
                assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
                assertEquals("No es posible acortar una url vacia", resp.getBody());
            })
            .verifyComplete();
        
        verify(shortUrlService, never()).getShortUrlByOriginalUrl(anyString());
        verify(shortUrlService, never()).generateShortUrl(anyString());
        verify(shortUrlService, never()).createShortUrl(any(ShortUrl.class));
    }

    @Test
    void getOriginal_WithValidShortUrl_ShouldReturnOriginalUrl() {
        ShortUrl shortUrl = new ShortUrl();
        shortUrl.setOriginalUrl(ORIGINAL_URL);
        shortUrl.setShortUrl(SHORT_URL_CODE);

        when(shortUrlService.getShortUrl(SHORT_URL_CODE)).thenReturn(Mono.just(shortUrl));

        Mono<ResponseEntity<String>> response = shortUrlController.getOriginal(SHORT_URL_CODE);

        StepVerifier.create(response)
            .assertNext(resp -> {
                assertEquals(HttpStatus.OK, resp.getStatusCode());
                assertEquals("Url original: " + ORIGINAL_URL, resp.getBody());
            })
            .verifyComplete();
        
        verify(shortUrlService).getShortUrl(SHORT_URL_CODE);
    }

    @Test
    void getOriginal_WithNonExistentShortUrl_ShouldReturnNotFound() {
        when(shortUrlService.getShortUrl(SHORT_URL_CODE)).thenReturn(Mono.empty());

        Mono<ResponseEntity<String>> response = shortUrlController.getOriginal(SHORT_URL_CODE);

        StepVerifier.create(response)
            .assertNext(resp -> {
                assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
                assertEquals("El codigo no corresponde a una url acortada", resp.getBody());
            })
            .verifyComplete();

        verify(shortUrlService).getShortUrl(SHORT_URL_CODE);
    }

    @Test
    void deleteShortUrl_WithValidShortUrl_ShouldDeleteSuccessfully() {
       when(shortUrlService.deleteShortUrl(SHORT_URL_CODE)).thenReturn(Mono.empty());

        Mono<ResponseEntity<String>> response = shortUrlController.deleteShortUrl(SHORT_URL_CODE);

        StepVerifier.create(response)
            .assertNext(resp -> {
                assertEquals(HttpStatus.OK, resp.getStatusCode());
                assertEquals("Short URL eliminada", resp.getBody());
            })
            .verifyComplete();
        
        verify(shortUrlService).deleteShortUrl(SHORT_URL_CODE);
    }

    @Test
    void deleteShortUrl_WhenServiceThrowsException_ShouldReturnBadRequest() {
        when(shortUrlService.deleteShortUrl(SHORT_URL_CODE))
            .thenReturn(Mono.error(new RuntimeException("Database error")));

        Mono<ResponseEntity<String>> response = shortUrlController.deleteShortUrl(SHORT_URL_CODE);

        StepVerifier.create(response)
            .assertNext(resp -> {
                assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
                assertEquals("Error eliminando url", resp.getBody());
            })
            .verifyComplete();
        
        verify(shortUrlService).deleteShortUrl(SHORT_URL_CODE);
    }

    @Test
    void createShortUrl_WithSpecialCharacters_ShouldHandleCorrectly() {
        String urlWithSpecialChars = "https://example.com/path?param=value&other=123";
        UrlRequest request = new UrlRequest();
        request.setOriginalUrl(urlWithSpecialChars);

        when(shortUrlService.getShortUrlByOriginalUrl(urlWithSpecialChars)).thenReturn(Mono.empty());
        when(shortUrlService.generateShortUrl(urlWithSpecialChars)).thenReturn(SHORT_URL_CODE);
        when(shortUrlService.createShortUrl(any(ShortUrl.class))).thenAnswer(invocation -> {
            ShortUrl shortUrl = invocation.getArgument(0);
            return Mono.just(shortUrl);
        });

        Mono<ResponseEntity<String>> response = shortUrlController.createShortUrl(request);

        StepVerifier.create(response)
            .assertNext(resp -> {
                assertEquals(HttpStatus.OK, resp.getStatusCode());
                assertEquals("Short URL creada: " + BASE_URL + SHORT_URL_CODE, resp.getBody());
            })
            .verifyComplete();
        
        verify(shortUrlService).getShortUrlByOriginalUrl(urlWithSpecialChars);
        verify(shortUrlService).generateShortUrl(urlWithSpecialChars);
    }

    @Test
    void createShortUrl_WithVeryLongUrl_ShouldHandleCorrectly() {
        String longUrl = "https://www.mercadolibre.com.ar/very/long/path/with/many/segments/and/parameters?param1=value1&param2=value2&param3=value3&param4=value4&param5=value5";
        UrlRequest request = new UrlRequest();
        request.setOriginalUrl(longUrl);

        when(shortUrlService.getShortUrlByOriginalUrl(longUrl)).thenReturn(Mono.empty());
        when(shortUrlService.generateShortUrl(longUrl)).thenReturn(SHORT_URL_CODE);
        when(shortUrlService.createShortUrl(any(ShortUrl.class))).thenAnswer(invocation -> {
            ShortUrl shortUrl = invocation.getArgument(0);
            return Mono.just(shortUrl);
        });

        Mono<ResponseEntity<String>> response = shortUrlController.createShortUrl(request);

        StepVerifier.create(response)
            .assertNext(resp -> {
                assertEquals(HttpStatus.OK, resp.getStatusCode());
                assertEquals("Short URL creada: " + BASE_URL + SHORT_URL_CODE, resp.getBody());
            })
            .verifyComplete();
        
        verify(shortUrlService).getShortUrlByOriginalUrl(longUrl);
        verify(shortUrlService).generateShortUrl(longUrl);
    }
} 