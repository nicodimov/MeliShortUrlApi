package com.melishorturlapi.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;

@Configuration
public class AppConfig {
    
    @Value("${url.shortening.base-url}")
    private String baseShortUrl;

    public String getBaseShortUrl() {
        return baseShortUrl;
    }
}
