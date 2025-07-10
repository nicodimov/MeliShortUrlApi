package com.melishorturlapi.filters;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import java.util.UUID;

@Component
public class RequestIdWebFilter implements WebFilter {

    public static final String REQUEST_ID_HEADER = "shorturl-request-id";
    public static final String REQUEST_ID_KEY = "shorturlRequestId";

    @Override
    public Mono<Void> filter(@NonNull ServerWebExchange exchange,@NonNull WebFilterChain chain) {
        String requestId = exchange.getRequest().getHeaders().getFirst(REQUEST_ID_HEADER);
        if (requestId == null || requestId.isEmpty()) {
            requestId = UUID.randomUUID().toString();
        }

        // Agrega el header a la respuesta
        exchange.getResponse().getHeaders().add(REQUEST_ID_HEADER, requestId);

        // Loguea el requestId y la ruta al inicio de cada request
        String path = exchange.getRequest().getPath().toString();
        String method = exchange.getRequest().getMethodValue();
        org.slf4j.LoggerFactory.getLogger(RequestIdWebFilter.class)
            .info("Request started: method={}, path={}, shorturl-request-id={}", method, path, requestId);

        // Propaga el requestId en el contexto reactivo
        return chain.filter(exchange)
                .contextWrite(Context.of(REQUEST_ID_KEY, requestId));
    }
}
