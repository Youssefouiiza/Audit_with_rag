package com.audit.platform.shared.mapper;

import com.audit.platform.modules.audit.domain.Audit;
import com.audit.platform.modules.audit.domain.AuditStatus;
import com.audit.platform.modules.audit.dto.AuditResponse;
import com.audit.platform.modules.user.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AuditMapper Unit Tests")
class AuditMapperTest {

    private final AuditMapper mapper = new AuditMapperImpl();

    @Test
    @DisplayName("toResponse maps fields correctly")
    void toResponse() {
        UUID auditId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        UUID auditorId = UUID.randomUUID();

        User client = User.builder().id(clientId).fullName("Client Name").build();
        User auditor = User.builder().id(auditorId).fullName("Auditor Name").build();

        Audit audit = Audit.builder()
                .id(auditId)
                .title("Audit Title")
                .status(AuditStatus.DRAFT)
                .client(client)
                .auditor(auditor)
                .build();

        AuditResponse resp = mapper.toResponse(audit);

        assertNotNull(resp);
        assertEquals(auditId, resp.getId());
        assertEquals("Audit Title", resp.getTitle());
        assertEquals(AuditStatus.DRAFT, resp.getStatus());
        assertEquals(clientId, resp.getClientId());
        assertEquals("Client Name", resp.getClientName());
        assertEquals(auditorId, resp.getAuditorId());
        assertEquals("Auditor Name", resp.getAuditorName());
    }

    @Test
    @DisplayName("toResponse handles null audit")
    void toResponse_Null() {
        assertNull(mapper.toResponse(null));
    }
}
