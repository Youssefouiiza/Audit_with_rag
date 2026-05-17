package com.audit.platform.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthRateLimitFilter Unit Tests")
class AuthRateLimitFilterTest {

    private AuthRateLimitFilter filter;
    private AppProperties appProperties;

    @BeforeEach
    void setUp() {
        appProperties = new AppProperties();
        appProperties.getRateLimit().setAuthPerMinute(3);
        filter = new AuthRateLimitFilter(appProperties);
    }

    @Test
    @DisplayName("shouldNotFilter returns true for non-auth paths")
    void shouldNotFilter_NonAuthPath() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getRequestURI()).thenReturn("/api/users/me");
        assertTrue(filter.shouldNotFilter(req));
    }

    @Test
    @DisplayName("shouldNotFilter returns false for auth paths")
    void shouldNotFilter_AuthPath() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getRequestURI()).thenReturn("/api/auth/login");
        assertFalse(filter.shouldNotFilter(req));
    }

    @Test
    @DisplayName("Allows requests within rate limit")
    void allowsWithinLimit() throws Exception {
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(req.getHeader("X-Forwarded-For")).thenReturn(null);
        when(req.getRemoteAddr()).thenReturn("1.2.3.4");

        filter.doFilterInternal(req, resp, chain);
        verify(chain).doFilter(req, resp);
    }

    @Test
    @DisplayName("Blocks requests exceeding rate limit")
    void blocksExceedingLimit() throws Exception {
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(req.getHeader("X-Forwarded-For")).thenReturn(null);
        when(req.getRemoteAddr()).thenReturn("10.0.0.1");

        // First 3 should pass
        for (int i = 0; i < 3; i++) {
            filter.doFilterInternal(req, resp, chain);
        }
        verify(chain, times(3)).doFilter(req, resp);

        // 4th should be blocked
        filter.doFilterInternal(req, resp, chain);
        verify(resp).setStatus(429);
        verify(chain, times(3)).doFilter(req, resp); // still 3
    }

    @Test
    @DisplayName("Uses X-Forwarded-For for client IP")
    void usesXForwardedFor() throws Exception {
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(req.getHeader("X-Forwarded-For")).thenReturn("99.99.99.99, 10.0.0.1");

        filter.doFilterInternal(req, resp, chain);
        verify(chain).doFilter(req, resp);
    }
}
