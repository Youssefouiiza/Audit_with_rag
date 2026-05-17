package com.audit.platform.config;

import com.audit.platform.modules.auth.repository.TokenBlacklistRepository;
import com.audit.platform.modules.user.domain.User;
import com.audit.platform.modules.user.domain.UserRole;
import com.audit.platform.modules.user.domain.UserStatus;
import com.audit.platform.modules.user.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationFilter Unit Tests")
class JwtAuthenticationFilterTest {

    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private TokenBlacklistRepository tokenBlacklistRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("No Authorization header passes through without auth")
    void noAuthHeader() throws Exception {
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(req.getHeader("Authorization")).thenReturn(null);

        filter.doFilterInternal(req, resp, chain);
        verify(chain).doFilter(req, resp);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    @DisplayName("Non-Bearer token passes through")
    void nonBearerToken() throws Exception {
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(req.getHeader("Authorization")).thenReturn("Basic abc");

        filter.doFilterInternal(req, resp, chain);
        verify(chain).doFilter(req, resp);
    }

    @Test
    @DisplayName("Blacklisted token returns 401")
    void blacklistedToken() throws Exception {
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(req.getHeader("Authorization")).thenReturn("Bearer valid-token");
        when(jwtTokenProvider.getJti("valid-token")).thenReturn("jti-123");
        when(tokenBlacklistRepository.existsByJti("jti-123")).thenReturn(true);

        filter.doFilterInternal(req, resp, chain);
        verify(resp).setStatus(401);
        verify(chain, never()).doFilter(req, resp);
    }

    @Test
    @DisplayName("Valid token with ACTIVE user sets authentication")
    void validToken_ActiveUser() throws Exception {
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).email("e@e.com").passwordHash("h")
                .role(UserRole.AUDITOR).status(UserStatus.ACTIVE).build();

        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(req.getHeader("Authorization")).thenReturn("Bearer valid-token");
        when(jwtTokenProvider.getJti("valid-token")).thenReturn("jti");
        when(tokenBlacklistRepository.existsByJti("jti")).thenReturn(false);
        when(jwtTokenProvider.getUserId("valid-token")).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        filter.doFilterInternal(req, resp, chain);
        verify(chain).doFilter(req, resp);
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    @DisplayName("User not found returns 401")
    void userNotFound() throws Exception {
        UUID userId = UUID.randomUUID();
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(req.getHeader("Authorization")).thenReturn("Bearer token");
        when(jwtTokenProvider.getJti("token")).thenReturn("jti");
        when(tokenBlacklistRepository.existsByJti("jti")).thenReturn(false);
        when(jwtTokenProvider.getUserId("token")).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        filter.doFilterInternal(req, resp, chain);
        verify(resp).setStatus(401);
    }

    @Test
    @DisplayName("Inactive user returns 403")
    void inactiveUser() throws Exception {
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).email("e").passwordHash("h")
                .role(UserRole.CLIENT).status(UserStatus.INACTIVE).build();

        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(req.getHeader("Authorization")).thenReturn("Bearer token");
        when(jwtTokenProvider.getJti("token")).thenReturn("jti");
        when(tokenBlacklistRepository.existsByJti("jti")).thenReturn(false);
        when(jwtTokenProvider.getUserId("token")).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        filter.doFilterInternal(req, resp, chain);
        verify(resp).setStatus(403);
    }

    @Test
    @DisplayName("Invalid token returns 401")
    void invalidToken() throws Exception {
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(req.getHeader("Authorization")).thenReturn("Bearer bad");
        when(jwtTokenProvider.getJti("bad")).thenThrow(new RuntimeException("parse error"));

        filter.doFilterInternal(req, resp, chain);
        verify(resp).setStatus(401);
    }
}
