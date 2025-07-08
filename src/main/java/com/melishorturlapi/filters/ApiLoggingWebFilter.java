package com.melishorturlapi.filters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import org.reactivestreams.Publisher;

@Component
public class ApiLoggingWebFilter implements WebFilter {
    private static final Logger logger = LoggerFactory.getLogger(ApiLoggingWebFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();

        // Buffer and log the request body
        return DataBufferUtils.join(request.getBody())
            .defaultIfEmpty(exchange.getResponse().bufferFactory().wrap(new byte[0]))
            .flatMap(dataBuffer -> {
                byte[] bodyBytes = new byte[dataBuffer.readableByteCount()];
                dataBuffer.read(bodyBytes);
                DataBufferUtils.release(dataBuffer);
                String requestBody = new String(bodyBytes, StandardCharsets.UTF_8);
                logger.info("--- API CALL ---\nMethod: {}\nURI: {}\nHeaders: {}\nRequest Body: {}",
                        request.getMethod(), request.getURI(), request.getHeaders(), requestBody);

                // Decorate the request with the cached body
                ServerHttpRequest mutatedRequest = request.mutate().build();
                mutatedRequest = new org.springframework.http.server.reactive.ServerHttpRequestDecorator(mutatedRequest) {
                    @Override
                    public Flux<DataBuffer> getBody() {
                        return Flux.just(exchange.getResponse().bufferFactory().wrap(bodyBytes));
                    }
                };

                // Buffer and log the response body
                ServerHttpResponse mutatedResponse = response;
                org.springframework.http.server.reactive.ServerHttpResponseDecorator responseDecorator =
                        new org.springframework.http.server.reactive.ServerHttpResponseDecorator(mutatedResponse) {
                    @Override
                    public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
                        if (body instanceof Flux) {
                            Flux<? extends DataBuffer> fluxBody = (Flux<? extends DataBuffer>) body;
                            return super.writeWith(
                                    fluxBody.doOnNext(dataBuffer -> {
                                        byte[] content = new byte[dataBuffer.readableByteCount()];
                                        dataBuffer.read(content);
                                        String responseBody = new String(content, StandardCharsets.UTF_8);
                                        logger.info("Response Status: {}\nResponse Body: {}", getStatusCode(), responseBody);
                                    })
                            );
                        }
                        return super.writeWith(body);
                    }
                };

                return chain.filter(exchange.mutate().request(mutatedRequest).response(responseDecorator).build());
            });
    }
}