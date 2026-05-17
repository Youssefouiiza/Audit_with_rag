package com.audit.platform.modules.notification.service;

import com.audit.platform.modules.notification.domain.Notification;
import com.audit.platform.modules.notification.domain.NotificationType;
import com.audit.platform.modules.notification.dto.NotificationResponse;
import com.audit.platform.modules.notification.repository.NotificationRepository;
import com.audit.platform.modules.user.domain.User;
import com.audit.platform.modules.user.domain.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationService Unit Tests")
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private ObjectProvider<SimpMessagingTemplate> messagingTemplate;

    @InjectMocks
    private NotificationService notificationService;

    private User testUser;
    private UUID userId;
    private Notification notification;
    private UUID notifId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        testUser = User.builder()
                .id(userId)
                .email("user@test.com")
                .fullName("Test User")
                .role(UserRole.AUDITOR)
                .build();

        notifId = UUID.randomUUID();
        notification = Notification.builder()
                .id(notifId)
                .user(testUser)
                .type(NotificationType.AUDIT_ASSIGNED)
                .title("Test Notification")
                .content("Test content")
                .referenceId("ref-123")
                .referenceType("AUDIT")
                .createdAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("Push notification should save and attempt WebSocket delivery")
    void push_SavesNotification() {
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

        notificationService.push(testUser, NotificationType.AUDIT_ASSIGNED,
                "Test Title", "Test Content", "ref-123", "AUDIT");

        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    @DisplayName("getMyNotifications should return all notifications for user")
    void getMyNotifications_ReturnsAll() {
        Notification n2 = Notification.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .type(NotificationType.AI_READY)
                .title("AI Ready")
                .content("AI results ready")
                .createdAt(Instant.now())
                .build();

        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(userId))
                .thenReturn(List.of(notification, n2));

        List<NotificationResponse> result = notificationService.getMyNotifications(userId);

        assertEquals(2, result.size());
        assertEquals("Test Notification", result.get(0).getTitle());
        assertEquals("AI Ready", result.get(1).getTitle());
    }

    @Test
    @DisplayName("getMyNotifications with empty list should return empty")
    void getMyNotifications_EmptyList() {
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(userId))
                .thenReturn(List.of());

        List<NotificationResponse> result = notificationService.getMyNotifications(userId);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("getUnread should return only unread notifications")
    void getUnread_ReturnsUnread() {
        when(notificationRepository.findByUserIdAndReadAtIsNullOrderByCreatedAtDesc(userId))
                .thenReturn(List.of(notification));

        List<NotificationResponse> result = notificationService.getUnread(userId);

        assertEquals(1, result.size());
        assertNull(result.get(0).getReadAt());
    }

    @Test
    @DisplayName("markRead should update readAt for existing notification")
    void markRead_UpdatesReadAt() {
        when(notificationRepository.findById(notifId)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

        notificationService.markRead(notifId);

        verify(notificationRepository).save(any(Notification.class));
        assertNotNull(notification.getReadAt());
    }

    @Test
    @DisplayName("markRead should do nothing for non-existent notification")
    void markRead_NonExistent() {
        when(notificationRepository.findById(notifId)).thenReturn(Optional.empty());

        notificationService.markRead(notifId);

        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    @DisplayName("markAllRead should mark all unread notifications as read")
    void markAllRead_MarksAll() {
        Notification n2 = Notification.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .type(NotificationType.DOC_REQUESTED)
                .title("Doc Request")
                .content("Doc needed")
                .createdAt(Instant.now())
                .build();

        when(notificationRepository.findByUserIdAndReadAtIsNullOrderByCreatedAtDesc(userId))
                .thenReturn(List.of(notification, n2));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> i.getArgument(0));

        notificationService.markAllRead(userId);

        verify(notificationRepository, times(2)).save(any(Notification.class));
        assertNotNull(notification.getReadAt());
        assertNotNull(n2.getReadAt());
    }

    @Test
    @DisplayName("markAllRead with no unread notifications should not save anything")
    void markAllRead_NoUnread() {
        when(notificationRepository.findByUserIdAndReadAtIsNullOrderByCreatedAtDesc(userId))
                .thenReturn(List.of());

        notificationService.markAllRead(userId);

        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    @DisplayName("Push notification response should contain correct fields")
    void push_ResponseContainsCorrectFields() {
        Notification savedNotif = Notification.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .type(NotificationType.REPORT_READY)
                .title("Report Title")
                .content("Report Content")
                .referenceId("audit-123")
                .referenceType("AUDIT")
                .createdAt(Instant.now())
                .build();

        when(notificationRepository.save(any(Notification.class))).thenReturn(savedNotif);

        notificationService.push(testUser, NotificationType.REPORT_READY,
                "Report Title", "Report Content", "audit-123", "AUDIT");

        verify(notificationRepository).save(argThat(n ->
                n.getTitle().equals("Report Title") &&
                n.getContent().equals("Report Content") &&
                n.getType() == NotificationType.REPORT_READY
        ));
    }
}
