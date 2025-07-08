package com.melishorturlapi.filters;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import io.micrometer.core.lang.NonNull;

@Component
public class ApiLoggingFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(ApiLoggingFilter.class);

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        long startTime = System.currentTimeMillis();
        filterChain.doFilter(wrappedRequest, wrappedResponse);
        long duration = System.currentTimeMillis() - startTime;

        String requestBody = new String(wrappedRequest.getContentAsByteArray(), request.getCharacterEncoding());
        String responseBody = new String(wrappedResponse.getContentAsByteArray(), response.getCharacterEncoding());

        String headers = Collections.list(request.getHeaderNames()).stream()
                .map(headerName -> headerName + "=" + request.getHeader(headerName))
                .collect(Collectors.joining(", "));

        logger.info(
                "\n--- API CALL ---\n" +
                "Method: {}\n" +
                "URI: {}\n" +
                "Headers: {}\n" +
                "Request Body: {}\n" +
                "Response Status: {}\n" +
                "Response Body: {}\n" +
                "Duration: {} ms\n" +
                "----------------",
                request.getMethod(),
                request.getRequestURI(),
                headers,
                requestBody,
                response.getStatus(),
                responseBody,
                duration
        );

        wrappedResponse.copyBodyToResponse();
    }
}