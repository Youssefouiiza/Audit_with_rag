package com.audit.platform.shared.exception;

import com.audit.platform.shared.dto.ErrorResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("GlobalExceptionHandler Unit Tests")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("handleApi returns correct error response")
    void handleApi() {
        ApiException ex = new ApiException(ErrorCode.AUDIT_001, HttpStatus.NOT_FOUND, "Audit 123 not found");
        ResponseEntity<ErrorResponse> resp = handler.handleApi(ex);
        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
        assertEquals("AUDIT_001", resp.getBody().getCode());
        assertEquals("Audit not found", resp.getBody().getMessage());
        assertEquals("Audit 123 not found", resp.getBody().getDetails());
        assertNotNull(resp.getBody().getTimestamp());
    }

    @Test
    @DisplayName("handleApi without details")
    void handleApi_NoDetails() {
        ApiException ex = new ApiException(ErrorCode.USER_001, HttpStatus.NOT_FOUND);
        ResponseEntity<ErrorResponse> resp = handler.handleApi(ex);
        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
        assertEquals("USER_001", resp.getBody().getCode());
        assertNull(resp.getBody().getDetails());
    }

    @Test
    @DisplayName("handleValidation returns BAD_REQUEST with field errors")
    void handleValidation() {
        BindingResult br = mock(BindingResult.class);
        FieldError fe1 = new FieldError("obj", "email", "must not be blank");
        FieldError fe2 = new FieldError("obj", "name", "too short");
        when(br.getFieldErrors()).thenReturn(List.of(fe1, fe2));
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, br);

        ResponseEntity<ErrorResponse> resp = handler.handleValidation(ex);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        assertEquals("VAL_001", resp.getBody().getCode());
        assertTrue(resp.getBody().getDetails().contains("email"));
        assertTrue(resp.getBody().getDetails().contains("name"));
    }

    @Test
    @DisplayName("handleConstraint returns BAD_REQUEST")
    void handleConstraint() {
        ConstraintViolationException ex = new ConstraintViolationException("field: invalid value", Set.of());
        ResponseEntity<ErrorResponse> resp = handler.handleConstraint(ex);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        assertEquals("VAL_001", resp.getBody().getCode());
    }

    @Test
    @DisplayName("handleAuth returns UNAUTHORIZED")
    void handleAuth() {
        BadCredentialsException ex = new BadCredentialsException("Bad credentials");
        ResponseEntity<ErrorResponse> resp = handler.handleAuth(ex);
        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
        assertEquals("AUTH_002", resp.getBody().getCode());
        assertEquals("Bad credentials", resp.getBody().getMessage());
    }

    @Test
    @DisplayName("handleAuth with null message uses default")
    void handleAuth_NullMessage() {
        BadCredentialsException ex = new BadCredentialsException(null);
        ResponseEntity<ErrorResponse> resp = handler.handleAuth(ex);
        assertEquals("Invalid or expired token", resp.getBody().getMessage());
    }

    @Test
    @DisplayName("handleAccessDenied returns FORBIDDEN")
    void handleAccessDenied() {
        AccessDeniedException ex = new AccessDeniedException("Forbidden");
        ResponseEntity<ErrorResponse> resp = handler.handleAccessDenied(ex);
        assertEquals(HttpStatus.FORBIDDEN, resp.getStatusCode());
        assertEquals("AUTH_003", resp.getBody().getCode());
        assertEquals("Access denied", resp.getBody().getMessage());
    }

    @Test
    @DisplayName("handleDataIntegrity returns CONFLICT")
    void handleDataIntegrity() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException("FK constraint");
        ResponseEntity<ErrorResponse> resp = handler.handleDataIntegrity(ex);
        assertEquals(HttpStatus.CONFLICT, resp.getStatusCode());
        assertEquals("DB_001", resp.getBody().getCode());
    }

    @Test
    @DisplayName("handleGeneric returns INTERNAL_SERVER_ERROR")
    void handleGeneric() {
        Exception ex = new RuntimeException("Something broke");
        ResponseEntity<ErrorResponse> resp = handler.handleGeneric(ex);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resp.getStatusCode());
        assertEquals("GEN_001", resp.getBody().getCode());
        assertEquals("Something broke", resp.getBody().getDetails());
    }
}
