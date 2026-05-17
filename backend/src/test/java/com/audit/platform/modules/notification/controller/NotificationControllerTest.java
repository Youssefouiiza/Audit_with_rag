package com.audit.platform.modules.notification.controller;

import com.audit.platform.config.SecurityUserDetails;
import com.audit.platform.modules.notification.dto.NotificationResponse;
import com.audit.platform.modules.notification.service.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationController Unit Tests")
class NotificationControllerTest {

    @Mock private NotificationService notificationService;
    @InjectMocks private NotificationController controller;

    private Authentication mockAuth() {
        UUID userId = UUID.randomUUID();
        SecurityUserDetails details = mock(SecurityUserDetails.class);
        when(details.getId()).thenReturn(userId);
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(details);
        return auth;
    }

    @Test
    @DisplayName("getAll returns notifications")
    void getAll() {
        Authentication auth = mockAuth();
        UUID userId = ((SecurityUserDetails) auth.getPrincipal()).getId();
        when(notificationService.getMyNotifications(userId)).thenReturn(List.of());
        assertEquals(HttpStatus.OK, controller.getAll(auth).getStatusCode());
    }

    @Test
    @DisplayName("getUnread returns unread notifications")
    void getUnread() {
        Authentication auth = mockAuth();
        UUID userId = ((SecurityUserDetails) auth.getPrincipal()).getId();
        when(notificationService.getUnread(userId)).thenReturn(List.of());
        assertEquals(HttpStatus.OK, controller.getUnread(auth).getStatusCode());
    }

    @Test
    @DisplayName("markRead calls service and returns 204")
    void markRead() {
        UUID id = UUID.randomUUID();
        assertEquals(HttpStatus.NO_CONTENT, controller.markRead(id).getStatusCode());
        verify(notificationService).markRead(id);
    }

    @Test
    @DisplayName("markAllRead calls service and returns 204")
    void markAllRead() {
        Authentication auth = mockAuth();
        UUID userId = ((SecurityUserDetails) auth.getPrincipal()).getId();
        assertEquals(HttpStatus.NO_CONTENT, controller.markAllRead(auth).getStatusCode());
        verify(notificationService).markAllRead(userId);
    }
}
