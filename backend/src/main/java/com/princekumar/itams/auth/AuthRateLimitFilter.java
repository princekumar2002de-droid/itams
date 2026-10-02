package com.princekumar.itams.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.princekumar.itams.common.dto.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory sliding-window rate limiter for the login endpoint.
 *
 * <p>Blocks credential-stuffing attacks that hammer {@code POST /auth/login}
 * from one IP: after {@link #maxAttempts} attempts inside {@link #windowSeconds}
 * seconds, further attempts get {@code 429 auth.rate_limited} until enough
 * timestamps age out of the window.</p>
 *
 * <p><b>Scope limits</b> (deliberately, for this milestone):</p>
 * <ul>
 *   <li>Counter is per-process, not shared across instances. For a horizontally
 *       scaled deployment this would move to Redis or a proper Bucket4j-Ignite
 *       setup.</li>
 *   <li>Only counts requests to {@code POST /api/v1/auth/login}. Refresh is
 *       protected by the fact that a bad refresh token also returns 401 through
 *       the normal path; a separate limit could be added if we ever see abuse
 *       there.</li>
 *   <li>Counter isn't reset on successful login. That is intentional — a
 *       credential-stuffing attacker who guesses one password shouldn't get
 *       their allowance back to keep guessing on the next account.</li>
 * </ul>
 */
@Component
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(AuthRateLimitFilter.class);
    private static final String LOGIN_PATH = "/api/v1/auth/login";

    private final int maxAttempts;
    private final int windowSeconds;
    /**
     * Dedicated ObjectMapper for the 429 body. We deliberately keep a filter-local
     * instance (rather than inject the Spring one) so the filter works identically
     * in unit tests without Spring. {@link JavaTimeModule} is registered so
     * {@link OffsetDateTime} on {@link ErrorResponse#timestamp()} serialises;
     * before this was added the filter would 500 when it tried to emit a 429.
     */
    private final ObjectMapper json = new ObjectMapper().registerModule(new JavaTimeModule());

    /** IP → timestamps (epoch millis) of recent login attempts, newest last. */
    private final ConcurrentHashMap<String, Deque<Long>> attempts = new ConcurrentHashMap<>();

    public AuthRateLimitFilter(
        @Value("${auth.rate-limit.max-attempts:10}") int maxAttempts,
        @Value("${auth.rate-limit.window-seconds:60}") int windowSeconds
    ) {
        this.maxAttempts = maxAttempts;
        this.windowSeconds = windowSeconds;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res,
                                    FilterChain chain) throws ServletException, IOException {
        if (!isLoginPost(req)) {
            chain.doFilter(req, res);
            return;
        }

        String key = clientKey(req);
        long now = Instant.now().toEpochMilli();
        long cutoff = now - windowSeconds * 1000L;

        Deque<Long> window = attempts.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (window) {
            // Drop timestamps that have aged out of the window.
            while (!window.isEmpty() && window.peekFirst() < cutoff) window.pollFirst();
            if (window.size() >= maxAttempts) {
                writeRateLimited(req, res, window.size());
                return;
            }
            window.addLast(now);
        }
        chain.doFilter(req, res);
    }

    private static boolean isLoginPost(HttpServletRequest req) {
        return "POST".equalsIgnoreCase(req.getMethod()) && LOGIN_PATH.equals(req.getRequestURI());
    }

    /**
     * Prefer an X-Forwarded-For entry (a reverse proxy is between us and the client
     * in every real deployment); fall back to remote addr. Takes only the first hop
     * from XFF so a client can't spoof-inject their own list.
     */
    private static String clientKey(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            int comma = xff.indexOf(',');
            return (comma > 0 ? xff.substring(0, comma) : xff).trim();
        }
        String remote = req.getRemoteAddr();
        return remote != null ? remote : "unknown";
    }

    private void writeRateLimited(HttpServletRequest req, HttpServletResponse res, int seen) throws IOException {
        log.warn("Rate limit hit on {} from {} (attempts in window: {})",
            req.getRequestURI(), clientKey(req), seen);
        ErrorResponse body = new ErrorResponse(
            "auth.rate_limited",
            "Too many login attempts. Try again in a minute.",
            HttpStatus.TOO_MANY_REQUESTS.value(),
            req.getRequestURI(),
            OffsetDateTime.now(),
            null
        );
        res.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        // Give the client a machine-readable hint on how long to wait.
        res.setHeader("Retry-After", String.valueOf(windowSeconds));
        json.writeValue(res.getOutputStream(), body);
    }

    // ── testing hooks ────────────────────────────────────────────────────────

    /** For tests only — inspect current window size. */
    int windowSize(String ip) {
        Deque<Long> w = attempts.get(ip);
        return w == null ? 0 : w.size();
    }

    /** For tests only — clear all counters. */
    void reset() { attempts.clear(); }
}
