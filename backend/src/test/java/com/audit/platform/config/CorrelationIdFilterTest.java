package com.audit.platform.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CorrelationIdFilter Unit Tests")
class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @Test
    @DisplayName("Uses provided correlation ID from header")
    void usesProvidedId() throws Exception {
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(req.getHeader("X-Correlation-Id")).thenReturn("my-corr-id");

        doAnswer(inv -> {
            assertEquals("my-corr-id", MDC.get("correlationId"));
            return null;
        }).when(chain).doFilter(req, resp);

        filter.doFilterInternal(req, resp, chain);
        verify(resp).setHeader("X-Correlation-Id", "my-corr-id");
        verify(chain).doFilter(req, resp);
        assertNull(MDC.get("correlationId")); // cleaned up
    }

    @Test
    @DisplayName("Generates new correlation ID when header is missing")
    void generatesNewId() throws Exception {
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(req.getHeader("X-Correlation-Id")).thenReturn(null);

        doAnswer(inv -> {
            String corrId = MDC.get("correlationId");
            assertNotNull(corrId);
            assertFalse(corrId.isEmpty());
            return null;
        }).when(chain).doFilter(req, resp);

        filter.doFilterInternal(req, resp, chain);
        verify(resp).setHeader(eq("X-Correlation-Id"), argThat(s -> s != null && !s.isEmpty()));
    }

    @Test
    @DisplayName("Generates new correlation ID when header is blank")
    void generatesNewIdForBlank() throws Exception {
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(req.getHeader("X-Correlation-Id")).thenReturn("   ");

        filter.doFilterInternal(req, resp, chain);
        verify(resp).setHeader(eq("X-Correlation-Id"), argThat(s -> s != null && !s.isBlank()));
    }

    @Test
    @DisplayName("MDC is cleaned up even on exception")
    void mdcCleanedOnException() throws Exception {
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(req.getHeader("X-Correlation-Id")).thenReturn("test-id");
        doThrow(new RuntimeException("boom")).when(chain).doFilter(req, resp);

        assertThrows(RuntimeException.class, () -> filter.doFilterInternal(req, resp, chain));
        assertNull(MDC.get("correlationId"));
    }
}
