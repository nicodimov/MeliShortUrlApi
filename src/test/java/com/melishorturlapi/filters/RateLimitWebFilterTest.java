package com.melishorturlapi.filters;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.InetSocketAddress;

public class RateLimitWebFilterTest {

    @Test
    @Disabled
    void testRateLimiting() {
        RateLimitWebFilter filter = new RateLimitWebFilter();
        
        // Create a mock exchange
        MockServerHttpRequest request = MockServerHttpRequest
            .get("/api/v1/shorturl/view/test")
            .remoteAddress(new InetSocketAddress("127.0.0.1", 8080))
            .build();
        
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        
        // Mock filter chain
        WebFilterChain chain = exchange1 -> {
            exchange1.getResponse().setStatusCode(org.springframework.http.HttpStatus.OK);
            return Mono.empty();
        };
        
        // Send 10 requests (should succeed)
        for (int i = 0; i < 10; i++) {
            StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();
        }
        
        // 11th request should be rate limited (429)
        StepVerifier.create(filter.filter(exchange, chain))
            .verifyComplete();
        
        // Verify the response status is 429
        assert exchange.getResponse().getStatusCode() == org.springframework.http.HttpStatus.TOO_MANY_REQUESTS;
    }
} 