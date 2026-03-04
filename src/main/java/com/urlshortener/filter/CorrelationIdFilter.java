package com.urlshortener.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

/**
 * Filter to add and propagate a correlation ID for each request.
 * <p>
 * The correlation ID:
 * - Is taken from request header if present
 * - Otherwise a new UUID is generated
 * - Added to response headers
 * - Stored in MDC for logging
 */
@Component
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String CORRELATION_ID = "X-Correlation-Id";

    /**
     * Extracts or generates correlation ID and attaches it to request,
     * response, and logging context.
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String correlationId = Optional.ofNullable(
                        request.getHeader(CORRELATION_ID))
                .orElse(UUID.randomUUID().toString());

        MDC.put(CORRELATION_ID, correlationId);

        response.setHeader(CORRELATION_ID, correlationId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(CORRELATION_ID);
        }
    }
}