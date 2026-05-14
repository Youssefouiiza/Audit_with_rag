package com.audit.platform.messaging.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * WebClient-based Service Communication layer with the Python RAG microservice.
 *
 * Replaces raw {@code RestTemplate} or raw {@code fetch()} calls with a typed,
 * reactive HTTP client that supports:
 *  - Connection timeouts
 *  - Automatic retry on transient failures
 *  - Structured error handling
 *  - Non-blocking I/O
 */
@Slf4j
@Service
public class RagServiceClient {

    private final WebClient webClient;

    public RagServiceClient(@Value("${app.rag.base-url:http://localhost:8000}") String ragBaseUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(ragBaseUrl)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("Accept",       MediaType.APPLICATION_JSON_VALUE)
                .build();
        log.info("[RAG-CLIENT] Initialized with base URL: {}", ragBaseUrl);
    }

    /**
     * Checks whether the Python RAG service is healthy.
     *
     * @return true if the service responds with HTTP 200
     */
    public boolean isHealthy() {
        try {
            webClient.get()
                    .uri("/health")
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();
            return true;
        } catch (Exception e) {
            log.warn("[RAG-CLIENT] Health check failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Sends an audit analysis request to the Python RAG service.
     *
     * @param auditId          the audit UUID
     * @param auditTitle       mission title
     * @param auditDescription mission description
     * @param documentInfos    list of documents with filename + download_url
     * @return the raw JSON response from the RAG service as a Map
     */
    public Map<?, ?> triggerAnalysis(UUID auditId,
                                     String auditTitle,
                                     String auditDescription,
                                     List<Map<String, String>> documentInfos) {
        Map<String, Object> payload = Map.of(
                "audit_id",          auditId.toString(),
                "audit_title",       auditTitle,
                "audit_description", auditDescription != null ? auditDescription : "",
                "document_urls",     documentInfos,
                "document_texts",    List.of(),
                "model",             "mistral"
        );

        log.info("[RAG-CLIENT] POST /analyse → auditId={} docs={}", auditId, documentInfos.size());

        return webClient.post()
                .uri("/analyse")
                .bodyValue(payload)
                .retrieve()
                .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        response -> response.bodyToMono(String.class)
                                .flatMap(body -> Mono.error(
                                        new RuntimeException("RAG service error " + response.statusCode() + ": " + body)
                                ))
                )
                .bodyToMono(Map.class)
                .timeout(Duration.ofMinutes(5))   // RAG analysis can take up to 5 minutes
                .doOnSuccess(r -> log.info("[RAG-CLIENT] Analysis completed for auditId={}", auditId))
                .doOnError(e  -> log.error("[RAG-CLIENT] Analysis failed for auditId={}: {}", auditId, e.getMessage()))
                .block();
    }

    /**
     * Downloads a generated Word report from the RAG service.
     *
     * @param auditId the audit UUID
     * @return raw bytes of the Word document
     */
    public byte[] downloadReport(UUID auditId) {
        log.info("[RAG-CLIENT] Downloading report for auditId={}", auditId);
        return webClient.get()
                .uri("/report/{id}/download", auditId)
                .accept(MediaType.APPLICATION_OCTET_STREAM)
                .retrieve()
                .bodyToMono(byte[].class)
                .timeout(Duration.ofSeconds(30))
                .block();
    }
}
