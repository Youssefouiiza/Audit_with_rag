package com.audit.platform.config;

import com.audit.platform.modules.user.domain.UserRole;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("JwtTokenProvider Unit Tests")
class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        AppProperties props = new AppProperties();
        AppProperties.Jwt jwt = props.getJwt();
        jwt.setSecret("this-is-a-very-secure-secret-key-for-jwt-256-bits!!");
        jwt.setAccessTokenMinutes(15);
        jwtTokenProvider = new JwtTokenProvider(props);
    }

    @Test
    @DisplayName("createAccessToken produces valid token")
    void createAccessToken_Valid() {
        UUID userId = UUID.randomUUID();
        String token = jwtTokenProvider.createAccessToken(userId, "test@e.com", UserRole.AUDITOR, false);
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    @DisplayName("parse returns correct claims")
    void parse_CorrectClaims() {
        UUID userId = UUID.randomUUID();
        String token = jwtTokenProvider.createAccessToken(userId, "test@e.com", UserRole.ADMIN, true);
        Claims claims = jwtTokenProvider.parse(token);
        assertEquals(userId.toString(), claims.getSubject());
        assertEquals("test@e.com", claims.get("email"));
        assertEquals("ADMIN", claims.get("role"));
        assertEquals(true, claims.get("firstLogin"));
        assertNotNull(claims.getId());
        assertNotNull(claims.getIssuedAt());
        assertNotNull(claims.getExpiration());
    }

    @Test
    @DisplayName("getUserId returns correct UUID")
    void getUserId_Correct() {
        UUID userId = UUID.randomUUID();
        String token = jwtTokenProvider.createAccessToken(userId, "e@e.com", UserRole.CLIENT, false);
        UUID parsed = jwtTokenProvider.getUserId(token);
        assertEquals(userId, parsed);
    }

    @Test
    @DisplayName("getJti returns non-null JTI")
    void getJti_NonNull() {
        String token = jwtTokenProvider.createAccessToken(UUID.randomUUID(), "e@e.com", UserRole.MANAGER, false);
        String jti = jwtTokenProvider.getJti(token);
        assertNotNull(jti);
        assertFalse(jti.isEmpty());
    }

    @Test
    @DisplayName("Each token has unique JTI")
    void jti_Unique() {
        UUID userId = UUID.randomUUID();
        String t1 = jwtTokenProvider.createAccessToken(userId, "e@e.com", UserRole.AUDITOR, false);
        String t2 = jwtTokenProvider.createAccessToken(userId, "e@e.com", UserRole.AUDITOR, false);
        assertNotEquals(jwtTokenProvider.getJti(t1), jwtTokenProvider.getJti(t2));
    }

    @Test
    @DisplayName("parse throws on invalid token")
    void parse_InvalidToken() {
        assertThrows(Exception.class, () -> jwtTokenProvider.parse("invalid.token.here"));
    }

    @Test
    @DisplayName("Short secret throws IllegalStateException")
    void shortSecret_Throws() {
        AppProperties props = new AppProperties();
        props.getJwt().setSecret("short");
        JwtTokenProvider provider = new JwtTokenProvider(props);
        assertThrows(IllegalStateException.class, () ->
                provider.createAccessToken(UUID.randomUUID(), "e@e.com", UserRole.ADMIN, false));
    }
}
