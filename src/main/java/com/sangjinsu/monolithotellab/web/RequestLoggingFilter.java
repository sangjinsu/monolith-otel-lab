package com.sangjinsu.monolithotellab.web;

import static net.logstash.logback.argument.StructuredArguments.kv;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Logs one structured line per request. Runs inside the HTTP observation scope
 * (Spring's ServerHttpObservationFilter registers at HIGHEST_PRECEDENCE + 1, while a
 * plain @Component filter defaults to lowest precedence), so by the time this filter
 * runs the MDC already carries traceId/spanId and the log line is trace-correlated.
 *
 * The emitted line (method/path/status/duration_ms plus trace_id/span_id added by the
 * JSON encoder) is the entry point for log→trace correlation: paste its trace_id into
 * Grafana Tempo and the exact request opens (docs/study-guide.md, checkpoint 7).
 */
@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger("http.access");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        long start = System.currentTimeMillis();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = System.currentTimeMillis() - start;
            log.info("request completed",
                    kv("method", request.getMethod()),
                    kv("path", request.getRequestURI()),
                    kv("status", response.getStatus()),
                    kv("duration_ms", durationMs));
        }
    }
}
