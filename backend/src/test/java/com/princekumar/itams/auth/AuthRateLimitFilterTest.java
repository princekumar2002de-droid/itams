package com.princekumar.itams.auth;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link AuthRateLimitFilter}. No Spring context — we drive the
 * filter directly with mock servlet objects. Tests use tiny window / low
 * threshold values so they stay fast.
 */
class AuthRateLimitFilterTest {

    private AuthRateLimitFilter filter;
    private FilterChain chain;

    @BeforeEach
    void setup() {
        // 3 attempts / 60s so we can trip the limit in one test tick.
        filter = new AuthRateLimitFilter(3, 60);
        chain = mock(FilterChain.class);
    }

    @Test
    void allows_requests_below_the_limit() throws Exception {
        for (int i = 0; i < 3; i++) filter.doFilter(loginReq("1.2.3.4"), res(), chain);
        verify(chain, times(3)).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void blocks_with_429_over_the_limit() throws Exception {
        for (int i = 0; i < 3; i++) filter.doFilter(loginReq("1.2.3.4"), res(), chain);
        MockHttpServletResponse res = res();
        filter.doFilter(loginReq("1.2.3.4"), res, chain);
        assertThat(res.getStatus()).isEqualTo(429);
        assertThat(res.getContentType()).contains("application/json");
        assertThat(res.getContentAsString()).contains("auth.rate_limited");
        assertThat(res.getHeader("Retry-After")).isEqualTo("60");
    }

    @Test
    void does_not_block_ip_B_when_ip_A_is_over_limit() throws Exception {
        for (int i = 0; i < 3; i++) filter.doFilter(loginReq("1.2.3.4"), res(), chain);
        MockHttpServletResponse res = res();
        filter.doFilter(loginReq("9.9.9.9"), res, chain);
        assertThat(res.getStatus()).isEqualTo(200);
        verify(chain, times(4)).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void does_not_count_requests_to_other_endpoints() throws Exception {
        for (int i = 0; i < 10; i++) {
            MockHttpServletRequest r = new MockHttpServletRequest("POST", "/api/v1/tickets");
            r.setRemoteAddr("1.2.3.4");
            filter.doFilter(r, res(), chain);
        }
        assertThat(filter.windowSize("1.2.3.4")).isZero();
    }

    @Test
    void does_not_count_get_requests_to_login_path() throws Exception {
        for (int i = 0; i < 10; i++) {
            MockHttpServletRequest r = new MockHttpServletRequest("GET", "/api/v1/auth/login");
            r.setRemoteAddr("1.2.3.4");
            filter.doFilter(r, res(), chain);
        }
        assertThat(filter.windowSize("1.2.3.4")).isZero();
    }

    @Test
    void x_forwarded_for_first_hop_is_used_as_key() throws Exception {
        MockHttpServletRequest r = loginReq("10.0.0.1");
        r.addHeader("X-Forwarded-For", "203.0.113.5, 10.0.0.1");
        for (int i = 0; i < 3; i++) filter.doFilter(r, res(), chain);
        assertThat(filter.windowSize("203.0.113.5")).isEqualTo(3);
        assertThat(filter.windowSize("10.0.0.1")).isZero();
    }

    @Test
    void chain_is_not_invoked_when_blocked() throws Exception {
        for (int i = 0; i < 3; i++) filter.doFilter(loginReq("1.2.3.4"), res(), chain);
        FilterChain freshChain = mock(FilterChain.class);
        filter.doFilter(loginReq("1.2.3.4"), res(), freshChain);
        verify(freshChain, never()).doFilter(
            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private static MockHttpServletRequest loginReq(String ip) {
        MockHttpServletRequest r = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        r.setRemoteAddr(ip);
        return r;
    }

    private static MockHttpServletResponse res() { return new MockHttpServletResponse(); }
}
