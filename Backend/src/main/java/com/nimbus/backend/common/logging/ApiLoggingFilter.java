package com.nimbus.backend.common.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

@Slf4j
@Component
public class ApiLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {


        String requestURI = request.getRequestURI();
        if (requestURI.contains("/swagger-ui") || requestURI.contains("/v3/api-docs") || requestURI.contains("/actuator")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 1. Wrap request and response to enable payload caching
        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(request,10);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);

        long startTime = System.currentTimeMillis();

        try {
            // 2. Pass control to the next filter down the pipeline (lets the controller execute)
            filterChain.doFilter(requestWrapper, responseWrapper);
        } finally {
            long duration = System.currentTimeMillis() - startTime;

            // 3. Perform logging actions after the request cycle completes
            logRequestDetails(requestWrapper);
            logResponseDetails(responseWrapper, duration);

            // 🔥 CRITICAL: Copy cached response body back to the real response stream so the client receives it
            responseWrapper.copyBodyToResponse();
        }
    }

    private void logRequestDetails(ContentCachingRequestWrapper request) {
        String payload = getStringValue(request.getContentAsByteArray(), request.getCharacterEncoding());
        log.info("➔ [HTTP REQUEST] | Method: {} | URI: {} | Client IP: {} | Payload: {}",
                request.getMethod(),
                request.getRequestURI(),
                request.getRemoteAddr(),
                payload.isBlank() ? "[EMPTY]" : payload.strip()
        );
    }

    private void logResponseDetails(ContentCachingResponseWrapper response, long duration) {
        String payload = getStringValue(response.getContentAsByteArray(), response.getCharacterEncoding());
        log.info("Status: {} | Duration: {}ms | Payload: {}",
                response.getStatus(),
                duration,
                payload.isBlank() ? "[EMPTY]" : payload.strip()
        );
    }

    private String getStringValue(byte[] contentAsByteArray, String characterEncoding) {
        try {
            if (contentAsByteArray == null || contentAsByteArray.length == 0) {
                return "";
            }
            return new String(contentAsByteArray, 0, contentAsByteArray.length, characterEncoding);
        } catch (UnsupportedEncodingException e) {
            return "[UNSUPPORTED ENCODING PARSE ERROR]";
        }
    }
}