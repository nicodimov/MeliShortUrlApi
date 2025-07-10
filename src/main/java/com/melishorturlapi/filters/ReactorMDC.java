package com.melishorturlapi.filters;

import org.slf4j.MDC;
import reactor.core.publisher.Mono;
/***
 * Mapped Diagnostic Context. It's a feature in logging frameworks like SLF4J and Logback that allows developers to associate contextual information with log message
 */
public class ReactorMDC {
    public static <T> Mono<T> withRequestId(Mono<T> mono) {
        return mono.contextWrite(ctx -> {
            String requestId = ctx.getOrDefault(RequestIdWebFilter.REQUEST_ID_KEY, null);
            if (requestId != null) {
                MDC.put(RequestIdWebFilter.REQUEST_ID_HEADER, requestId);
            }
            return ctx;
        });
    }
}