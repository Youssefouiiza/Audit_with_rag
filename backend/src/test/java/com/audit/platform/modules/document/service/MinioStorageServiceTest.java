package com.audit.platform.modules.document.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("MinioStorageService Unit Tests")
class MinioStorageServiceTest {

    private MinioStorageService service;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        service = new MinioStorageService();
        ReflectionTestUtils.setField(service, "localStoragePath", tempDir.toString());
        ReflectionTestUtils.setField(service, "allowedOrigins", "http://localhost:3000");
    }

    @Test
    @DisplayName("init creates storage directory")
    void init_CreatesDir() {
        service.init();
        assertTrue(Files.exists(tempDir));
    }

    @Test
    @DisplayName("uploadDocument stores file and returns key")
    void uploadDocument_Success() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getContentType()).thenReturn("application/pdf");
        when(file.getOriginalFilename()).thenReturn("test report.pdf");
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream("PDF content".getBytes()));

        String key = service.uploadDocument(file, "audit/123");
        assertNotNull(key);
        assertTrue(key.startsWith("audit/123/"));
        assertTrue(key.contains("test_report.pdf"));
    }

    @Test
    @DisplayName("uploadDocument sanitizes filename")
    void uploadDocument_SanitizesFilename() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getContentType()).thenReturn("text/plain");
        when(file.getOriginalFilename()).thenReturn("bad file (1).txt");
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream("data".getBytes()));

        String key = service.uploadDocument(file, "prefix");
        assertTrue(key.contains("bad_file__1_.txt"));
    }

    @Test
    @DisplayName("uploadDocument with null filename uses default")
    void uploadDocument_NullFilename() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getContentType()).thenReturn(null);
        when(file.getOriginalFilename()).thenReturn(null);
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream("data".getBytes()));

        String key = service.uploadDocument(file, "pfx");
        assertTrue(key.contains("file"));
    }

    @Test
    @DisplayName("uploadDocumentBytes stores bytes correctly")
    void uploadDocumentBytes_Success() {
        byte[] bytes = "PDF bytes".getBytes();
        String key = service.uploadDocumentBytes(bytes, "report.pdf", "application/pdf", "reports/456", "reports");
        assertNotNull(key);

        byte[] read = service.downloadFile(key);
        assertArrayEquals(bytes, read);
    }

    @Test
    @DisplayName("downloadFile throws for missing file")
    void downloadFile_NotFound() {
        assertThrows(Exception.class, () -> service.downloadFile("nonexistent/file.pdf"));
    }

    @Test
    @DisplayName("presignedGetUrl returns correct URL format")
    void presignedGetUrl_Format() {
        String url = service.presignedGetUrl("documents", "audit/123/file.pdf");
        assertTrue(url.startsWith("http://localhost:8080/api/files/download?key="));
        assertTrue(url.contains("audit"));
    }

    @Test
    @DisplayName("deleteFile removes existing file")
    void deleteFile_Success() throws IOException {
        Path file = tempDir.resolve("test-file.txt");
        Files.writeString(file, "content");
        assertTrue(Files.exists(file));

        service.deleteFile("test-file.txt");
        assertFalse(Files.exists(file));
    }

    @Test
    @DisplayName("deleteFile handles non-existent file gracefully")
    void deleteFile_NonExistent() {
        assertDoesNotThrow(() -> service.deleteFile("does-not-exist.txt"));
    }
}
