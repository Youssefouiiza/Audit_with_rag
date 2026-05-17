package com.audit.platform.modules.document.service;

import com.audit.platform.config.AppProperties;
import com.audit.platform.config.SecurityUserDetails;
import com.audit.platform.modules.audit.domain.Audit;
import com.audit.platform.modules.audit.domain.AuditStatus;
import com.audit.platform.modules.audit.repository.AuditRepository;
import com.audit.platform.modules.document.domain.*;
import com.audit.platform.modules.document.dto.CreateDocRequestRequest;
import com.audit.platform.modules.document.dto.DocumentRequestResponse;
import com.audit.platform.modules.document.dto.DocumentResponse;
import com.audit.platform.modules.document.repository.DocumentRepository;
import com.audit.platform.modules.document.repository.DocumentRequestRepository;
import com.audit.platform.modules.notification.service.NotificationService;
import com.audit.platform.modules.user.domain.User;
import com.audit.platform.modules.user.domain.UserRole;
import com.audit.platform.modules.user.repository.UserRepository;
import com.audit.platform.shared.exception.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DocumentService Unit Tests")
class DocumentServiceTest {

    @Mock private DocumentRepository documentRepository;
    @Mock private DocumentRequestRepository documentRequestRepository;
    @Mock private AuditRepository auditRepository;
    @Mock private UserRepository userRepository;
    @Mock private MinioStorageService storageService;
    @Mock private NotificationService notificationService;
    @Mock private AppProperties appProperties;

    @InjectMocks private DocumentService documentService;

    private User clientUser, auditorUser;
    private Audit audit;
    private UUID auditId;

    @BeforeEach
    void setUp() {
        clientUser = User.builder().id(UUID.randomUUID()).email("c@t.com").fullName("Client").role(UserRole.CLIENT).build();
        auditorUser = User.builder().id(UUID.randomUUID()).email("a@t.com").fullName("Auditor").role(UserRole.AUDITOR).build();
        auditId = UUID.randomUUID();
        audit = Audit.builder().id(auditId).title("Test Audit").client(clientUser).auditor(auditorUser).status(AuditStatus.IN_PROGRESS).build();
        mockSecurityContext(clientUser);
    }

    private void mockSecurityContext(User user) {
        SecurityUserDetails details = new SecurityUserDetails(user);
        var auth = new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities());
        SecurityContext ctx = mock(SecurityContext.class);
        lenient().when(ctx.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(ctx);
    }

    private AppProperties.Minio mockMinio() {
        AppProperties.Minio minio = new AppProperties.Minio();
        lenient().when(appProperties.getMinio()).thenReturn(minio);
        return minio;
    }

    @Test
    @DisplayName("upload saves document and returns response")
    void upload_Success() {
        mockMinio();
        MultipartFile file = mock(MultipartFile.class);
        when(file.getSize()).thenReturn(1024L);
        when(file.getOriginalFilename()).thenReturn("test.pdf");
        when(file.getContentType()).thenReturn("application/pdf");
        when(auditRepository.findById(auditId)).thenReturn(Optional.of(audit));
        when(userRepository.findById(clientUser.getId())).thenReturn(Optional.of(clientUser));
        when(storageService.uploadDocument(file, "audit/" + auditId)).thenReturn("key123");
        when(storageService.presignedGetUrl(any(), eq("key123"))).thenReturn("http://url");

        Document saved = Document.builder().id(UUID.randomUUID()).audit(audit).uploadedBy(clientUser)
                .fileName("test.pdf").fileKey("key123").fileSize(1024L).mimeType("application/pdf")
                .category(DocumentCategory.OTHER).status(DocumentEntityStatus.PENDING).build();
        when(documentRepository.save(any())).thenReturn(saved);

        DocumentResponse r = documentService.upload(auditId, file, null);
        assertNotNull(r);
        assertEquals("test.pdf", r.getFileName());
        verify(documentRepository).save(any());
    }

    @Test
    @DisplayName("upload throws when file exceeds 50MB")
    void upload_ExceedsQuota() {
        when(auditRepository.findById(auditId)).thenReturn(Optional.of(audit));
        when(userRepository.findById(clientUser.getId())).thenReturn(Optional.of(clientUser));
        MultipartFile file = mock(MultipartFile.class);
        when(file.getSize()).thenReturn(51L * 1024 * 1024);
        assertThrows(ApiException.class, () -> documentService.upload(auditId, file, null));
    }

    @Test
    @DisplayName("upload moves AWAITING_DOCS audit to IN_PROGRESS")
    void upload_MovesAwaitingDocsToInProgress() {
        mockMinio();
        audit.setStatus(AuditStatus.AWAITING_DOCS);
        MultipartFile file = mock(MultipartFile.class);
        when(file.getSize()).thenReturn(1024L);
        when(file.getOriginalFilename()).thenReturn("f.pdf");
        when(file.getContentType()).thenReturn("application/pdf");
        when(auditRepository.findById(auditId)).thenReturn(Optional.of(audit));
        when(userRepository.findById(clientUser.getId())).thenReturn(Optional.of(clientUser));
        when(storageService.uploadDocument(any(), any())).thenReturn("k");
        when(storageService.presignedGetUrl(any(), any())).thenReturn("u");

        Document saved = Document.builder().id(UUID.randomUUID()).audit(audit).uploadedBy(clientUser)
                .fileName("f.pdf").fileKey("k").fileSize(1024L).mimeType("application/pdf")
                .category(DocumentCategory.OTHER).status(DocumentEntityStatus.PENDING).build();
        when(documentRepository.save(any())).thenReturn(saved);

        documentService.upload(auditId, file, DocumentCategory.FINANCIAL);
        assertEquals(AuditStatus.IN_PROGRESS, audit.getStatus());
        verify(auditRepository).save(audit);
    }

    @Test
    @DisplayName("listByAudit returns documents for audit")
    void listByAudit_Success() {
        mockMinio();
        when(auditRepository.findById(auditId)).thenReturn(Optional.of(audit));
        Document doc = Document.builder().id(UUID.randomUUID()).audit(audit).uploadedBy(clientUser)
                .fileName("f.pdf").fileKey("k").fileSize(100L).mimeType("application/pdf")
                .category(DocumentCategory.FINANCIAL).status(DocumentEntityStatus.PENDING).build();
        when(documentRepository.findByAuditId(auditId)).thenReturn(List.of(doc));
        when(storageService.presignedGetUrl(any(), any())).thenReturn("url");

        List<DocumentResponse> r = documentService.listByAudit(auditId);
        assertEquals(1, r.size());
    }

    @Test
    @DisplayName("getById throws when document not found")
    void getById_NotFound() {
        UUID docId = UUID.randomUUID();
        when(documentRepository.findById(docId)).thenReturn(Optional.empty());
        assertThrows(ApiException.class, () -> documentService.getById(docId));
    }

    @Test
    @DisplayName("delete removes document and storage file")
    void delete_Success() {
        UUID docId = UUID.randomUUID();
        Document doc = Document.builder().id(docId).fileKey("key").build();
        when(documentRepository.findById(docId)).thenReturn(Optional.of(doc));
        documentService.delete(docId);
        verify(storageService).deleteFile("key");
        verify(documentRepository).delete(doc);
    }

    @Test
    @DisplayName("delete handles storage failure gracefully")
    void delete_StorageFailure() {
        UUID docId = UUID.randomUUID();
        Document doc = Document.builder().id(docId).fileKey("key").build();
        when(documentRepository.findById(docId)).thenReturn(Optional.of(doc));
        doThrow(new RuntimeException("IO error")).when(storageService).deleteFile("key");
        documentService.delete(docId);
        verify(documentRepository).delete(doc); // still deletes from DB
    }

    @Test
    @DisplayName("createRequest saves request and notifies client")
    void createRequest_Success() {
        mockSecurityContext(auditorUser);
        when(auditRepository.findById(auditId)).thenReturn(Optional.of(audit));
        when(userRepository.findById(auditorUser.getId())).thenReturn(Optional.of(auditorUser));

        DocumentRequest saved = DocumentRequest.builder().id(UUID.randomUUID()).audit(audit)
                .requestedBy(auditorUser).description("Need bilan").deadline(LocalDate.now().plusDays(7))
                .status(DocumentRequestStatus.PENDING).createdAt(Instant.now()).build();
        when(documentRequestRepository.save(any())).thenReturn(saved);

        CreateDocRequestRequest req = new CreateDocRequestRequest();
        req.setAuditId(auditId);
        req.setDescription("Need bilan");
        req.setDeadline(LocalDate.now().plusDays(7));

        DocumentRequestResponse r = documentService.createRequest(req);
        assertNotNull(r);
        verify(notificationService).push(eq(clientUser), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("createRequest sets audit to AWAITING_DOCS when IN_PROGRESS")
    void createRequest_SetsAwaitingDocs() {
        mockSecurityContext(auditorUser);
        audit.setStatus(AuditStatus.IN_PROGRESS);
        when(auditRepository.findById(auditId)).thenReturn(Optional.of(audit));
        when(userRepository.findById(auditorUser.getId())).thenReturn(Optional.of(auditorUser));

        DocumentRequest saved = DocumentRequest.builder().id(UUID.randomUUID()).audit(audit)
                .requestedBy(auditorUser).description("d").status(DocumentRequestStatus.PENDING).createdAt(Instant.now()).build();
        when(documentRequestRepository.save(any())).thenReturn(saved);

        CreateDocRequestRequest req = new CreateDocRequestRequest();
        req.setAuditId(auditId);
        req.setDescription("d");

        documentService.createRequest(req);
        assertEquals(AuditStatus.AWAITING_DOCS, audit.getStatus());
        verify(auditRepository).save(audit);
    }

    @Test
    @DisplayName("updateRequestStatus updates status")
    void updateRequestStatus_Success() {
        UUID reqId = UUID.randomUUID();
        DocumentRequest dr = DocumentRequest.builder().id(reqId).audit(audit).requestedBy(auditorUser)
                .description("d").status(DocumentRequestStatus.PENDING).createdAt(Instant.now()).build();
        when(documentRequestRepository.findById(reqId)).thenReturn(Optional.of(dr));
        when(documentRequestRepository.save(any())).thenReturn(dr);

        DocumentRequestResponse r = documentService.updateRequestStatus(reqId, DocumentRequestStatus.FULFILLED);
        assertEquals(DocumentRequestStatus.FULFILLED, dr.getStatus());
    }

    @Test
    @DisplayName("updateRequestStatus throws when request not found")
    void updateRequestStatus_NotFound() {
        UUID reqId = UUID.randomUUID();
        when(documentRequestRepository.findById(reqId)).thenReturn(Optional.empty());
        assertThrows(ApiException.class, () -> documentService.updateRequestStatus(reqId, DocumentRequestStatus.FULFILLED));
    }
}
