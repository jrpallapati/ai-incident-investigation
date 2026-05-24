package org.pallapati.aiincidentinvestigation.model;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

/**
 * Domain entity representing a semantically searchable chunk of logs.
 *
 * Purpose:
 * - Persist deterministic metadata about a block of related log lines (a "chunk").
 * - Act as the source-of-truth for metadata that is also mirrored to the VectorStore document.
 * - Decouple raw log storage from vector storage to allow re-embedding, re-indexing, etc.
 *
 * Deterministic responsibilities (code):
 * - File source, timestamps, severity, trace/session IDs, content hash, and createdAt are fully deterministic.
 * - We never store LLM-generated content here. Only raw/log-derived data.
 */
@Document(collection = "log_chunks")
public class LogChunk {

    @Id
    private String id;

    private String sourceFile;
    private String serviceName;
    private String severity;
    private String timestamp; // store as string to preserve original format; can be parsed as needed
    private String traceId;
    private String sessionId;

    /** Primary message line capturing the essence of the chunk. */
    private String message;

    /** Entire raw text of the chunk, including stack traces and multi-line context. */
    private String rawText;

    /** Flexible metadata bag mirrored to vector store document's metadata. */
    private Map<String, Object> metadata;

    /** Tracks whether embedding for this chunk has been created and pushed to vector store. */
    private String embeddingStatus; // e.g. PENDING, EMBEDDED, FAILED

    /** Uniqueness key to prevent duplicate embeddings on re-start. */
    @Indexed(unique = true)
    private String contentHash;

    @CreatedDate
    private Instant createdAt;

    // Getters and setters

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSourceFile() { return sourceFile; }
    public void setSourceFile(String sourceFile) { this.sourceFile = sourceFile; }

    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getRawText() { return rawText; }
    public void setRawText(String rawText) { this.rawText = rawText; }

    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }

    public String getEmbeddingStatus() { return embeddingStatus; }
    public void setEmbeddingStatus(String embeddingStatus) { this.embeddingStatus = embeddingStatus; }

    public String getContentHash() { return contentHash; }
    public void setContentHash(String contentHash) { this.contentHash = contentHash; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}

