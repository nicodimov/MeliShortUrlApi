package com.melishorturlapi.controller;

import com.melishorturlapi.config.AppConfig;
import com.melishorturlapi.model.ShortUrl;
import com.melishorturlapi.model.UrlRequest;
import com.melishorturlapi.service.ShortUrlService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShortUrlControllerTest {

    @Mock
    private ShortUrlService shortUrlService;

    @Mock
    private AppConfig appConfig;

    @InjectMocks
    private ShortUrlController shortUrlController;

    private static final String BASE_URL = "https://short.url/";
    private static final String ORIGINAL_URL = "https://www.mercadolibre.com.ar/";
    private static final String SHORT_URL_CODE = "abc123";

    @BeforeEach
    void setUp() {
        when(appConfig.getBaseShortUrl()).thenReturn(BASE_URL);
    }

    @Test
    void createShortUrl_WithValidUrl_ShouldCreateNewShortUrl() {
        // Arrange
        UrlRequest request = new UrlRequest();
        request.setOriginalUrl(ORIGINAL_URL);

        when(shortUrlService.getShortUrlByOriginalUrl(ORIGINAL_URL)).thenReturn(Optional.empty());
        when(shortUrlService.generateShortUrl(ORIGINAL_URL)).thenReturn(SHORT_URL_CODE);
        when(shortUrlService.createShortUrl(any(ShortUrl.class))).thenAnswer(invocation -> {
            ShortUrl shortUrl = invocation.getArgument(0);
            return shortUrl;
        });

        // Act
        ResponseEntity<String> response = shortUrlController.createShortUrl(request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Short URL creada: " + BASE_URL + SHORT_URL_CODE, response.getBody());
        
        verify(shortUrlService).getShortUrlByOriginalUrl(ORIGINAL_URL);
        verify(shortUrlService).generateShortUrl(ORIGINAL_URL);
        verify(shortUrlService).createShortUrl(any(ShortUrl.class));
    }

    @Test
    void createShortUrl_WithExistingUrl_ShouldReturnExistingShortUrl() {
        // Arrange
        UrlRequest request = new UrlRequest();
        request.setOriginalUrl(ORIGINAL_URL);

        ShortUrl existingShortUrl = new ShortUrl();
        existingShortUrl.setOriginalUrl(ORIGINAL_URL);
        existingShortUrl.setShortUrl(SHORT_URL_CODE);

        when(shortUrlService.getShortUrlByOriginalUrl(ORIGINAL_URL)).thenReturn(Optional.of(existingShortUrl));

        // Act
        ResponseEntity<String> response = shortUrlController.createShortUrl(request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Short URL creada (existente): " + BASE_URL + SHORT_URL_CODE, response.getBody());
        
        verify(shortUrlService).getShortUrlByOriginalUrl(ORIGINAL_URL);
        verify(shortUrlService, never()).generateShortUrl(anyString());
        verify(shortUrlService, never()).createShortUrl(any(ShortUrl.class));
    }

    @Test
    void createShortUrl_WithNullUrl_ShouldReturnBadRequest() {
        // Arrange
        UrlRequest request = new UrlRequest();
        request.setOriginalUrl(null);

        // Act
        ResponseEntity<String> response = shortUrlController.createShortUrl(request);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("No es posible acortar una url vacia", response.getBody());
        
        verify(shortUrlService, never()).getShortUrlByOriginalUrl(anyString());
        verify(shortUrlService, never()).generateShortUrl(anyString());
        verify(shortUrlService, never()).createShortUrl(any(ShortUrl.class));
    }

    @Test
    void createShortUrl_WithEmptyUrl_ShouldReturnBadRequest() {
        // Arrange
        UrlRequest request = new UrlRequest();
        request.setOriginalUrl("");

        // Act
        ResponseEntity<String> response = shortUrlController.createShortUrl(request);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("No es posible acortar una url vacia", response.getBody());
        
        verify(shortUrlService, never()).getShortUrlByOriginalUrl(anyString());
        verify(shortUrlService, never()).generateShortUrl(anyString());
        verify(shortUrlService, never()).createShortUrl(any(ShortUrl.class));
    }

    @Test
    void createShortUrl_WithWhitespaceUrl_ShouldReturnBadRequest() {
        // Arrange
        UrlRequest request = new UrlRequest();
        request.setOriginalUrl("   ");

        // Act
        ResponseEntity<String> response = shortUrlController.createShortUrl(request);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("No es posible acortar una url vacia", response.getBody());
        
        verify(shortUrlService, never()).getShortUrlByOriginalUrl(anyString());
        verify(shortUrlService, never()).generateShortUrl(anyString());
        verify(shortUrlService, never()).createShortUrl(any(ShortUrl.class));
    }

    @Test
    void getOriginal_WithValidShortUrl_ShouldReturnOriginalUrl() {
        // Arrange
        ShortUrl shortUrl = new ShortUrl();
        shortUrl.setOriginalUrl(ORIGINAL_URL);
        shortUrl.setShortUrl(SHORT_URL_CODE);

        when(shortUrlService.getShortUrl(SHORT_URL_CODE)).thenReturn(Optional.of(shortUrl));

        // Act
        ResponseEntity<String> response = shortUrlController.getOriginal(SHORT_URL_CODE);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Url original: " + ORIGINAL_URL, response.getBody());
        
        verify(shortUrlService).getShortUrl(SHORT_URL_CODE);
    }

    @Test
    void getOriginal_WithNonExistentShortUrl_ShouldThrowException() {
        // Arrange
        when(shortUrlService.getShortUrl(SHORT_URL_CODE)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            shortUrlController.getOriginal(SHORT_URL_CODE);
        });
        
        verify(shortUrlService).getShortUrl(SHORT_URL_CODE);
    }

    @Test
    void deleteShortUrl_WithValidShortUrl_ShouldDeleteSuccessfully() {
        // Arrange
        doNothing().when(shortUrlService).deleteShortUrl(SHORT_URL_CODE);

        // Act
        ResponseEntity<String> response = shortUrlController.deleteShortUrl(SHORT_URL_CODE);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Short URL eliminada", response.getBody());
        
        verify(shortUrlService).deleteShortUrl(SHORT_URL_CODE);
    }

    @Test
    void deleteShortUrl_WhenServiceThrowsException_ShouldReturnBadRequest() {
        // Arrange
        doThrow(new RuntimeException("Database error")).when(shortUrlService).deleteShortUrl(SHORT_URL_CODE);

        // Act
        ResponseEntity<String> response = shortUrlController.deleteShortUrl(SHORT_URL_CODE);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Error eliminando url", response.getBody());
        
        verify(shortUrlService).deleteShortUrl(SHORT_URL_CODE);
    }

    @Test
    void createShortUrl_ShouldSetCorrectProperties() {
        // Arrange
        UrlRequest request = new UrlRequest();
        request.setOriginalUrl(ORIGINAL_URL);

        when(shortUrlService.getShortUrlByOriginalUrl(ORIGINAL_URL)).thenReturn(Optional.empty());
        when(shortUrlService.generateShortUrl(ORIGINAL_URL)).thenReturn(SHORT_URL_CODE);
        when(shortUrlService.createShortUrl(any(ShortUrl.class))).thenAnswer(invocation -> {
            ShortUrl shortUrl = invocation.getArgument(0);
            return shortUrl;
        });

        // Act
        shortUrlController.createShortUrl(request);

        // Assert
        verify(shortUrlService).createShortUrl(argThat(shortUrl -> {
            assertEquals(ORIGINAL_URL, shortUrl.getOriginalUrl());
            assertEquals(SHORT_URL_CODE, shortUrl.getShortUrl());
            assertEquals(0L, shortUrl.getRedirectCount());
            assertNotNull(shortUrl.getCreatedAt());
            return true;
        }));
    }

    @Test
    void createShortUrl_WithSpecialCharacters_ShouldHandleCorrectly() {
        // Arrange
        String urlWithSpecialChars = "https://example.com/path?param=value&other=123";
        UrlRequest request = new UrlRequest();
        request.setOriginalUrl(urlWithSpecialChars);

        when(shortUrlService.getShortUrlByOriginalUrl(urlWithSpecialChars)).thenReturn(Optional.empty());
        when(shortUrlService.generateShortUrl(urlWithSpecialChars)).thenReturn(SHORT_URL_CODE);
        when(shortUrlService.createShortUrl(any(ShortUrl.class))).thenAnswer(invocation -> {
            ShortUrl shortUrl = invocation.getArgument(0);
            return shortUrl;
        });

        // Act
        ResponseEntity<String> response = shortUrlController.createShortUrl(request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Short URL creada: " + BASE_URL + SHORT_URL_CODE, response.getBody());
        
        verify(shortUrlService).getShortUrlByOriginalUrl(urlWithSpecialChars);
        verify(shortUrlService).generateShortUrl(urlWithSpecialChars);
    }

    @Test
    void createShortUrl_WithVeryLongUrl_ShouldHandleCorrectly() {
        // Arrange
        String longUrl = "https://www.mercadolibre.com.ar/very/long/path/with/many/segments/and/parameters?param1=value1&param2=value2&param3=value3&param4=value4&param5=value5";
        UrlRequest request = new UrlRequest();
        request.setOriginalUrl(longUrl);

        when(shortUrlService.getShortUrlByOriginalUrl(longUrl)).thenReturn(Optional.empty());
        when(shortUrlService.generateShortUrl(longUrl)).thenReturn(SHORT_URL_CODE);
        when(shortUrlService.createShortUrl(any(ShortUrl.class))).thenAnswer(invocation -> {
            ShortUrl shortUrl = invocation.getArgument(0);
            return shortUrl;
        });

        // Act
        ResponseEntity<String> response = shortUrlController.createShortUrl(request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Short URL creada: " + BASE_URL + SHORT_URL_CODE, response.getBody());
        
        verify(shortUrlService).getShortUrlByOriginalUrl(longUrl);
        verify(shortUrlService).generateShortUrl(longUrl);
    }
} 