package com.melishorturlapi.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class UrlHashServiceTest {
    @Test
    void testDefaultLength() {
        UrlHashService service = new UrlHashService();
        String hash = service.hashUrl("https://www.mercadolibre.com.ar/");
        assertEquals(6, hash.length(), "Default hash length should be 6");
    }

    @Test
    void testCustomLength() {
        UrlHashService service = new UrlHashService(10);
        String hash = service.hashUrl("https://www.mercadolibre.com.ar/");
        assertEquals(10, hash.length(), "Custom hash length should be 10");
    }

    @Test
    void testDeterministicOutput() {
        UrlHashService service = new UrlHashService();
        String hash1 = service.hashUrl("https://www.mercadolibre.com.ar/");
        String hash2 = service.hashUrl("https://www.mercadolibre.com.ar/");
        assertEquals(hash1, hash2, "Hashing the same URL should produce the same result");
    }

    @Test
    void testDifferentInputDifferentOutput() {
        UrlHashService service = new UrlHashService();
        String hash1 = service.hashUrl("https://www.mercadolibre.com.ar/");
        String hash2 = service.hashUrl("https://www.example.org");
        assertNotEquals(hash1, hash2, "Different URLs should produce different hashes");
    }

    @Test
    void testBase62Output() {
        UrlHashService service = new UrlHashService();
        String hash = service.hashUrl("https://www.mercadolibre.com.ar/");
        assertTrue(hash.matches("[0-9A-Za-z]+"), "Hash should be base62");
    }

    @Test
    void testLargeUrl() {
        UrlHashService service = new UrlHashService();
        String longUrl = "https://eletronicos.mercadolivre.com.br/seguranca-casa/#menu=categories";
        String hash = service.hashUrl(longUrl);
        assertEquals(6, hash.length(), "Hash length for large URL should be 6");
        assertTrue(hash.matches("[0-9A-Za-z]+"), "Hash should be base62");
    }

    @Test
    void testLargeUrlCustomLength() {
        UrlHashService service = new UrlHashService(12);
        String longUrl = "https://www.mercadolibre.com.ar/something-something-lallalalala/other/page.com?value=dsfsd&redirectTo=otherUrl";
        String hash = service.hashUrl(longUrl);
        assertEquals(12, hash.length(), "Custom hash length for large URL should be 12");
        assertTrue(hash.matches("[0-9A-Za-z]+"), "Hash should be base62");
    }
} 