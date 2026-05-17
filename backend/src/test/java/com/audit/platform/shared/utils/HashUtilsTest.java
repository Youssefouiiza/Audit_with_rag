package com.audit.platform.shared.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("HashUtils Unit Tests")
class HashUtilsTest {

    @Test
    @DisplayName("sha256Hex produces consistent output for same input")
    void sha256Hex_Consistent() {
        String hash1 = HashUtils.sha256Hex("hello");
        String hash2 = HashUtils.sha256Hex("hello");
        assertEquals(hash1, hash2);
    }

    @Test
    @DisplayName("sha256Hex produces 64-character hex string")
    void sha256Hex_CorrectLength() {
        String hash = HashUtils.sha256Hex("test input");
        assertEquals(64, hash.length());
        assertTrue(hash.matches("[0-9a-f]+"));
    }

    @Test
    @DisplayName("sha256Hex produces different output for different inputs")
    void sha256Hex_DifferentInputs() {
        String h1 = HashUtils.sha256Hex("input1");
        String h2 = HashUtils.sha256Hex("input2");
        assertNotEquals(h1, h2);
    }

    @Test
    @DisplayName("sha256Hex handles empty string")
    void sha256Hex_EmptyString() {
        String hash = HashUtils.sha256Hex("");
        assertNotNull(hash);
        assertEquals(64, hash.length());
        // SHA-256 of empty string is well-known
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", hash);
    }

    @Test
    @DisplayName("sha256Hex handles unicode input")
    void sha256Hex_Unicode() {
        String hash = HashUtils.sha256Hex("ét€st");
        assertNotNull(hash);
        assertEquals(64, hash.length());
    }

    @Test
    @DisplayName("sha256Hex handles long input")
    void sha256Hex_LongInput() {
        String longInput = "a".repeat(10000);
        String hash = HashUtils.sha256Hex(longInput);
        assertNotNull(hash);
        assertEquals(64, hash.length());
    }
}
