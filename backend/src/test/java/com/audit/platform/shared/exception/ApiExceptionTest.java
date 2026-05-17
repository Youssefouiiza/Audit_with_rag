package com.audit.platform.shared.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ApiException Unit Tests")
class ApiExceptionTest {

    @Test
    @DisplayName("Constructor without details")
    void constructorWithoutDetails() {
        ApiException ex = new ApiException(ErrorCode.USER_001, HttpStatus.NOT_FOUND);
        assertEquals(ErrorCode.USER_001, ex.getErrorCode());
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        assertNull(ex.getDetails());
        assertEquals("User not found", ex.getMessage());
    }

    @Test
    @DisplayName("Constructor with details")
    void constructorWithDetails() {
        ApiException ex = new ApiException(ErrorCode.AUTH_001, HttpStatus.UNAUTHORIZED, "Bad password");
        assertEquals(ErrorCode.AUTH_001, ex.getErrorCode());
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
        assertEquals("Bad password", ex.getDetails());
        assertEquals("Invalid credentials", ex.getMessage());
    }

    @Test
    @DisplayName("All error codes have correct codes")
    void allErrorCodes() {
        assertEquals("USER_001", ErrorCode.USER_001.getCode());
        assertEquals("USER_002", ErrorCode.USER_002.getCode());
        assertEquals("AUTH_001", ErrorCode.AUTH_001.getCode());
        assertEquals("AUTH_002", ErrorCode.AUTH_002.getCode());
        assertEquals("AUTH_003", ErrorCode.AUTH_003.getCode());
        assertEquals("AUDIT_001", ErrorCode.AUDIT_001.getCode());
        assertEquals("AUDIT_002", ErrorCode.AUDIT_002.getCode());
        assertEquals("DOC_001", ErrorCode.DOC_001.getCode());
        assertEquals("DOC_002", ErrorCode.DOC_002.getCode());
        assertEquals("DOC_003", ErrorCode.DOC_003.getCode());
        assertEquals("AI_001", ErrorCode.AI_001.getCode());
        assertEquals("CHAT_001", ErrorCode.CHAT_001.getCode());
        assertEquals("VAL_001", ErrorCode.VAL_001.getCode());
        assertEquals("GEN_001", ErrorCode.GEN_001.getCode());
    }

    @Test
    @DisplayName("All error codes have default messages")
    void allErrorCodesHaveMessages() {
        for (ErrorCode code : ErrorCode.values()) {
            assertNotNull(code.getDefaultMessage());
            assertFalse(code.getDefaultMessage().isEmpty());
        }
    }
}
