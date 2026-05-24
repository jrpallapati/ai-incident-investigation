package org.pallapati.aiincidentinvestigation.dto;

import java.util.List;
import java.util.Map;

/**
 * Response DTO carrying matched log chunks from vector similarity search.
 */
public class LogSearchResponse {
    public static class MatchingLogChunk {
        private String id;
        private String sourceFile;
        private String serviceName;
        private String severity;
        private String timestamp;
        private String message;
        private String rawText;
        private Map<String, Object> metadata;
        private Double score;

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
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public String getRawText() { return rawText; }
        public void setRawText(String rawText) { this.rawText = rawText; }
        public Map<String, Object> getMetadata() { return metadata; }
        public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
        public Double getScore() { return score; }
        public void setScore(Double score) { this.score = score; }
    }

    private List<MatchingLogChunk> matchingLogChunks;

    public List<MatchingLogChunk> getMatchingLogChunks() { return matchingLogChunks; }
    public void setMatchingLogChunks(List<MatchingLogChunk> matchingLogChunks) { this.matchingLogChunks = matchingLogChunks; }
}

