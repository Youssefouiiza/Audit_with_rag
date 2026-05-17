package com.audit.platform.shared.mapper;

import com.audit.platform.modules.notification.domain.Notification;
import com.audit.platform.modules.notification.domain.NotificationType;
import com.audit.platform.modules.notification.dto.NotificationResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("NotificationMapper Unit Tests")
class NotificationMapperTest {

    private final NotificationMapper mapper = new NotificationMapperImpl();

    @Test
    @DisplayName("toResponse maps fields correctly")
    void toResponse() {
        UUID id = UUID.randomUUID();
        Notification notif = Notification.builder()
                .id(id)
                .type(NotificationType.AUDIT_ASSIGNED)
                .title("T")
                .content("C")
                .referenceId("123")
                .referenceType("AUDIT")
                .build();

        NotificationResponse resp = mapper.toResponse(notif);

        assertNotNull(resp);
        assertEquals(id, resp.getId());
        assertEquals(NotificationType.AUDIT_ASSIGNED, resp.getType());
        assertEquals("T", resp.getTitle());
        assertEquals("C", resp.getContent());
        assertEquals("123", resp.getReferenceId());
        assertEquals("AUDIT", resp.getReferenceType());
    }

    @Test
    @DisplayName("toResponse handles null")
    void toResponse_Null() {
        assertNull(mapper.toResponse(null));
    }
}
