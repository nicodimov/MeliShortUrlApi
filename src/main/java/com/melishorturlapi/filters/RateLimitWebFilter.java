package com.melishorturlapi.filters;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitWebFilter implements WebFilter {
    private static final Logger logger = LoggerFactory.getLogger(RateLimitWebFilter.class);
    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Value("${rate.limit.enabled:true}")
    private boolean rateLimitEnabled;

    public RateLimitWebFilter() {
        logger.info("[RateLimit] RateLimitWebFilter constructor called - filter is being instantiated");
    }

    @PostConstruct
    public void init() {
        logger.info("[RateLimit] Rate limiting is enabled: {}", rateLimitEnabled);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        // If rate limiting is disabled, just continue with the chain
        if (!rateLimitEnabled) {
            logger.debug("[RateLimit] Rate limiting is disabled, allowing request");
            return chain.filter(exchange);
        }

        String clientIp = exchange.getRequest().getRemoteAddress() != null ? 
            exchange.getRequest().getRemoteAddress().getAddress().getHostAddress() : "unknown";
        
        logger.info("[RateLimit] Request from IP: {}, URI: {}", clientIp, exchange.getRequest().getURI());
        
        Bucket bucket = buckets.computeIfAbsent(clientIp, k -> {
            logger.info("[RateLimit] Creating new bucket for IP: {}", clientIp);
            return Bucket.builder()
                .addLimit(Bandwidth.classic(10, Refill.greedy(10, Duration.ofSeconds(20))))
                .build();
        });
        
        boolean consumed = bucket.tryConsume(1);
        logger.info("[RateLimit] IP: {}, Tokens consumed: {}, Remaining: {}", 
                   clientIp, consumed, bucket.getAvailableTokens());
        
        if (consumed) {
            logger.info("[RateLimit] Allowing request for IP: {}", clientIp);
            return chain.filter(exchange);
        } else {
            logger.warn("[RateLimit] Rate limiting request for IP: {}", clientIp);
            exchange.getResponse().setStatusCode(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS);
            return exchange.getResponse().setComplete();
        }
    }
}