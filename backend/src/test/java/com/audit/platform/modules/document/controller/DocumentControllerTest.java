package com.audit.platform.modules.document.controller;

import com.audit.platform.modules.document.domain.DocumentCategory;
import com.audit.platform.modules.document.domain.DocumentRequestStatus;
import com.audit.platform.modules.document.dto.CreateDocRequestRequest;
import com.audit.platform.modules.document.dto.DocumentRequestResponse;
import com.audit.platform.modules.document.dto.DocumentResponse;
import com.audit.platform.modules.document.service.DocumentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DocumentController Unit Tests")
class DocumentControllerTest {

    @Mock private DocumentService documentService;
    @InjectMocks private DocumentController controller;

    @Test
    @DisplayName("upload returns 201 created")
    void upload() {
        UUID auditId = UUID.randomUUID();
        MultipartFile file = mock(MultipartFile.class);
        DocumentResponse resp = DocumentResponse.builder().id(UUID.randomUUID()).build();
        when(documentService.upload(auditId, file, DocumentCategory.FINANCIAL)).thenReturn(resp);

        ResponseEntity<DocumentResponse> r = controller.upload(auditId, DocumentCategory.FINANCIAL, file);
        assertEquals(HttpStatus.CREATED, r.getStatusCode());
        assertEquals(resp.getId(), r.getBody().getId());
    }

    @Test
    @DisplayName("listByAudit returns documents")
    void listByAudit() {
        UUID id = UUID.randomUUID();
        when(documentService.listByAudit(id)).thenReturn(List.of());
        assertEquals(HttpStatus.OK, controller.listByAudit(id).getStatusCode());
    }

    @Test
    @DisplayName("get returns document")
    void get() {
        UUID id = UUID.randomUUID();
        DocumentResponse resp = DocumentResponse.builder().id(id).build();
        when(documentService.getById(id)).thenReturn(resp);
        assertEquals(id, controller.get(id).getBody().getId());
    }

    @Test
    @DisplayName("delete returns 204")
    void delete() {
        UUID id = UUID.randomUUID();
        assertEquals(HttpStatus.NO_CONTENT, controller.delete(id).getStatusCode());
        verify(documentService).delete(id);
    }

    @Test
    @DisplayName("createRequest returns 201")
    void createRequest() {
        CreateDocRequestRequest req = new CreateDocRequestRequest();
        DocumentRequestResponse resp = DocumentRequestResponse.builder().id(UUID.randomUUID()).build();
        when(documentService.createRequest(req)).thenReturn(resp);
        ResponseEntity<DocumentRequestResponse> r = controller.createRequest(req);
        assertEquals(HttpStatus.CREATED, r.getStatusCode());
    }

    @Test
    @DisplayName("listRequests returns requests")
    void listRequests() {
        UUID id = UUID.randomUUID();
        when(documentService.listRequestsByAudit(id)).thenReturn(List.of());
        assertEquals(HttpStatus.OK, controller.listRequests(id).getStatusCode());
    }

    @Test
    @DisplayName("updateRequestStatus returns updated request")
    void updateRequestStatus() {
        UUID id = UUID.randomUUID();
        DocumentRequestResponse resp = DocumentRequestResponse.builder().id(id).status(DocumentRequestStatus.FULFILLED).build();
        when(documentService.updateRequestStatus(id, DocumentRequestStatus.FULFILLED)).thenReturn(resp);
        ResponseEntity<DocumentRequestResponse> r = controller.updateRequestStatus(id, DocumentRequestStatus.FULFILLED);
        assertEquals(DocumentRequestStatus.FULFILLED, r.getBody().getStatus());
    }
}
