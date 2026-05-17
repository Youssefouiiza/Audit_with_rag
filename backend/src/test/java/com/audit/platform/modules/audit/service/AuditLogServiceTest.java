package com.audit.platform.modules.audit.service;

import com.audit.platform.modules.audit.domain.AuditLog;
import com.audit.platform.modules.audit.repository.AuditLogRepository;
import com.audit.platform.modules.user.domain.User;
import com.audit.platform.modules.user.domain.UserRole;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuditLogService Unit Tests")
class AuditLogServiceTest {

    @Mock private AuditLogRepository auditLogRepository;
    @Mock private HttpServletRequest request;

    @InjectMocks
    private AuditLogService auditLogService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder().id(UUID.randomUUID()).email("u@t.com").fullName("User").role(UserRole.AUDITOR).build();
    }

    @Test
    @DisplayName("Log entry with user and direct IP")
    void log_WithUser_DirectIp() {
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        when(request.getHeader("User-Agent")).thenReturn("TestBrowser/1.0");

        auditLogService.log(Optional.of(testUser), "LOGIN", "AUTH", "res-123", request);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog log = captor.getValue();
        assertEquals("LOGIN", log.getAction());
        assertEquals("AUTH", log.getResourceType());
        assertEquals("res-123", log.getResourceId());
        assertEquals("192.168.1.1", log.getIpAddress());
        assertEquals("TestBrowser/1.0", log.getUserAgent());
        assertEquals(testUser, log.getUser());
        assertNotNull(log.getCreatedAt());
    }

    @Test
    @DisplayName("Log entry with X-Forwarded-For header")
    void log_WithXForwardedFor() {
        when(request.getHeader("X-Forwarded-For")).thenReturn("10.0.0.1, 10.0.0.2");
        when(request.getHeader("User-Agent")).thenReturn("Mozilla");

        auditLogService.log(Optional.of(testUser), "ACTION", "TYPE", null, request);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertEquals("10.0.0.1", captor.getValue().getIpAddress());
    }

    @Test
    @DisplayName("Log entry without user")
    void log_WithoutUser() {
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getHeader("User-Agent")).thenReturn(null);

        auditLogService.log(Optional.empty(), "LOGIN_FAILED", "AUTH", null, request);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertNull(captor.getValue().getUser());
        assertEquals("LOGIN_FAILED", captor.getValue().getAction());
    }
}
