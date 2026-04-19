package com.tritit.cashorganizer.api.infrastructure.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

@Slf4j
@Component
@Order(1)
public class RateLimitFilter extends OncePerRequestFilter {

    private record Limit(int max, long windowMs, String retryAfter) {}

    private static final Map<String, Limit> ENDPOINT_LIMITS = Map.of(
        "/api/auth/login",          new Limit(5,  60_000L,       "60"),
        "/api/auth/register",       new Limit(10, 3_600_000L,    "3600"),
        "/api/auth/forgot-password",new Limit(3,  3_600_000L,    "3600")
    );

    private final Map<String, Deque<Long>> requestLog = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        if ("POST".equalsIgnoreCase(req.getMethod())) {
            String uri = req.getRequestURI();
            Limit limit = ENDPOINT_LIMITS.get(uri);
            if (limit != null) {
                String key = getClientIp(req) + ":" + uri;
                if (!isAllowed(key, limit.max(), limit.windowMs())) {
                    log.warn("Rate limit exceeded for IP {} on {}", getClientIp(req), uri);
                    rejectWithTooManyRequests(res, limit.retryAfter());
                    return;
                }
            }
        }

        chain.doFilter(req, res);
    }

    private boolean isAllowed(String key, int max, long windowMs) {
        long now = System.currentTimeMillis();
        Deque<Long> timestamps = requestLog.computeIfAbsent(key, k -> new ConcurrentLinkedDeque<>());
        synchronized (timestamps) {
            timestamps.removeIf(t -> now - t > windowMs);
            if (timestamps.size() >= max) return false;
            timestamps.addLast(now);
            return true;
        }
    }

    private void rejectWithTooManyRequests(HttpServletResponse res, String retryAfter) throws IOException {
        res.setStatus(429);
        res.setContentType("application/json;charset=UTF-8");
        res.setHeader("Retry-After", retryAfter);
        res.getWriter().write(
            "{\"status\":429,\"error\":\"Too Many Requests\"," +
            "\"message\":\"Demasiados intentos. Espera antes de volver a intentarlo.\"}"
        );
    }

    private String getClientIp(HttpServletRequest req) {
        String forwarded = req.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return req.getRemoteAddr();
    }
}
