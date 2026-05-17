package com.audit.platform.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AppProperties Unit Tests")
class AppPropertiesTest {

    @Test
    @DisplayName("Default JWT settings")
    void defaultJwtSettings() {
        AppProperties props = new AppProperties();
        assertEquals(15, props.getJwt().getAccessTokenMinutes());
        assertEquals(7, props.getJwt().getRefreshTokenDays());
        assertNull(props.getJwt().getSecret());
    }

    @Test
    @DisplayName("Default CORS settings")
    void defaultCorsSettings() {
        AppProperties props = new AppProperties();
        assertEquals("http://localhost:3000", props.getCors().getAllowedOrigins());
    }

    @Test
    @DisplayName("Default Minio bucket settings")
    void defaultMinioBuckets() {
        AppProperties props = new AppProperties();
        assertEquals("documents", props.getMinio().getBuckets().getDocuments());
        assertEquals("reports", props.getMinio().getBuckets().getReports());
        assertEquals("chat-files", props.getMinio().getBuckets().getChatFiles());
    }

    @Test
    @DisplayName("Default AI settings")
    void defaultAiSettings() {
        AppProperties props = new AppProperties();
        assertEquals("llama-3.3-70b-versatile", props.getAi().getGroq().getModel());
        assertEquals("https://api.groq.com/openai/v1", props.getAi().getGroq().getBaseUrl());
        assertEquals("mistral-small-latest", props.getAi().getMistral().getModel());
        assertEquals("gpt-4o-mini", props.getAi().getOpenai().getModel());
    }

    @Test
    @DisplayName("Default rate limit settings")
    void defaultRateLimitSettings() {
        AppProperties props = new AppProperties();
        assertEquals(10, props.getRateLimit().getAuthPerMinute());
    }

    @Test
    @DisplayName("Default client doc quota")
    void defaultDocQuota() {
        AppProperties props = new AppProperties();
        assertEquals(500, props.getClientDocQuotaMb());
    }

    @Test
    @DisplayName("Mutable JWT settings")
    void mutableJwtSettings() {
        AppProperties props = new AppProperties();
        props.getJwt().setSecret("my-secret");
        props.getJwt().setAccessTokenMinutes(30);
        props.getJwt().setRefreshTokenDays(14);
        assertEquals("my-secret", props.getJwt().getSecret());
        assertEquals(30, props.getJwt().getAccessTokenMinutes());
        assertEquals(14, props.getJwt().getRefreshTokenDays());
    }

    @Test
    @DisplayName("Mutable Minio settings")
    void mutableMinioSettings() {
        AppProperties props = new AppProperties();
        props.getMinio().setEndpoint("http://minio:9000");
        props.getMinio().setAccessKey("key");
        props.getMinio().setSecretKey("secret");
        assertEquals("http://minio:9000", props.getMinio().getEndpoint());
        assertEquals("key", props.getMinio().getAccessKey());
        assertEquals("secret", props.getMinio().getSecretKey());
    }

    @Test
    @DisplayName("Mutable AI Groq settings")
    void mutableAiGroqSettings() {
        AppProperties props = new AppProperties();
        props.getAi().getGroq().setApiKey("groq-key");
        props.getAi().getGroq().setModel("custom-model");
        assertEquals("groq-key", props.getAi().getGroq().getApiKey());
        assertEquals("custom-model", props.getAi().getGroq().getModel());
    }

    @Test
    @DisplayName("Mutable CORS settings")
    void mutableCorsSettings() {
        AppProperties props = new AppProperties();
        props.getCors().setAllowedOrigins("http://example.com");
        assertEquals("http://example.com", props.getCors().getAllowedOrigins());
    }

    @Test
    @DisplayName("Mutable rate limit settings")
    void mutableRateLimitSettings() {
        AppProperties props = new AppProperties();
        props.getRateLimit().setAuthPerMinute(5);
        assertEquals(5, props.getRateLimit().getAuthPerMinute());
    }
}
