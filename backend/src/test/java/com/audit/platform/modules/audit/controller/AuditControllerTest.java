package com.audit.platform.modules.audit.controller;

import com.audit.platform.modules.audit.domain.AuditStatus;
import com.audit.platform.modules.audit.dto.*;
import com.audit.platform.modules.audit.service.AuditService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuditController Unit Tests")
class AuditControllerTest {

    @Mock private AuditService auditService;
    @InjectMocks private AuditController controller;

    @Test
    @DisplayName("create returns 201 CREATED")
    void create() {
        AuditResponse resp = AuditResponse.builder().id(UUID.randomUUID()).title("T").build();
        when(auditService.create(any())).thenReturn(resp);
        ResponseEntity<AuditResponse> r = controller.create(new CreateAuditRequest());
        assertEquals(HttpStatus.CREATED, r.getStatusCode());
        assertEquals("T", r.getBody().getTitle());
    }

    @Test
    @DisplayName("listAll returns page")
    void listAll() {
        Page<AuditResponse> page = new PageImpl<>(List.of());
        when(auditService.listAll(any())).thenReturn(page);
        ResponseEntity<Page<AuditResponse>> r = controller.listAll(Pageable.unpaged());
        assertEquals(HttpStatus.OK, r.getStatusCode());
    }

    @Test
    @DisplayName("listMine returns page")
    void listMine() {
        when(auditService.listMine(any())).thenReturn(new PageImpl<>(List.of()));
        assertEquals(HttpStatus.OK, controller.listMine(Pageable.unpaged()).getStatusCode());
    }

    @Test
    @DisplayName("unassigned returns page")
    void unassigned() {
        when(auditService.listUnassigned(any())).thenReturn(new PageImpl<>(List.of()));
        assertEquals(HttpStatus.OK, controller.unassigned(Pageable.unpaged()).getStatusCode());
    }

    @Test
    @DisplayName("get returns audit by ID")
    void get() {
        UUID id = UUID.randomUUID();
        AuditResponse resp = AuditResponse.builder().id(id).build();
        when(auditService.getById(id)).thenReturn(resp);
        assertEquals(id, controller.get(id).getBody().getId());
    }

    @Test
    @DisplayName("assign returns updated audit")
    void assign() {
        UUID id = UUID.randomUUID();
        AuditResponse resp = AuditResponse.builder().id(id).build();
        when(auditService.assign(eq(id), any())).thenReturn(resp);
        assertEquals(HttpStatus.OK, controller.assign(id, new AssignAuditRequest()).getStatusCode());
    }

    @Test
    @DisplayName("changeStatus returns updated audit")
    void changeStatus() {
        UUID id = UUID.randomUUID();
        AuditResponse resp = AuditResponse.builder().id(id).status(AuditStatus.COMPLETED).build();
        when(auditService.changeStatus(eq(id), any(), any())).thenReturn(resp);
        assertEquals(AuditStatus.COMPLETED, controller.changeStatus(id, AuditStatus.COMPLETED, null).getBody().getStatus());
    }

    @Test
    @DisplayName("history returns list")
    void history() {
        UUID id = UUID.randomUUID();
        when(auditService.getHistory(id)).thenReturn(List.of());
        assertEquals(HttpStatus.OK, controller.history(id).getStatusCode());
    }

    @Test
    @DisplayName("delete returns 204")
    void delete() {
        UUID id = UUID.randomUUID();
        assertEquals(HttpStatus.NO_CONTENT, controller.delete(id).getStatusCode());
        verify(auditService).delete(id);
    }
}
