package org.pallapati.aiincidentinvestigation.service;

import org.pallapati.aiincidentinvestigation.dto.LogSearchRequest;
import org.pallapati.aiincidentinvestigation.dto.LogSearchResponse;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Performs semantic log search using the VectorStore similarity capabilities.
 * Accepts natural language queries and returns the most relevant log chunks.
 */
@Service
public class LogSearchService {

    private static final Logger log = LoggerFactory.getLogger(LogSearchService.class);

    private final VectorStore vectorStore;

    public LogSearchService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public LogSearchResponse search(LogSearchRequest request) {
        int topK = request.getTopK() != null ? Math.max(1, Math.min(50, request.getTopK())) : 8;
        long t0 = System.currentTimeMillis();
        log.info("[SemanticSearch] query='{}' topK={}", request.getQuery(), topK);
        SearchRequest.Builder builder = SearchRequest.builder()
                .query(request.getQuery())
                .topK(topK);
        List<Document> docs = vectorStore.similaritySearch(builder.build());
        long dt = System.currentTimeMillis() - t0;
        log.info("[SemanticSearch] matched={} in {} ms", docs.size(), dt);
        LogSearchResponse resp = new LogSearchResponse();
        resp.setMatchingLogChunks(docs.stream().map(d -> {
            LogSearchResponse.MatchingLogChunk mlc = new LogSearchResponse.MatchingLogChunk();
            mlc.setId(d.getId());
            mlc.setSourceFile((String) d.getMetadata().get("sourceFile"));
            mlc.setServiceName((String) d.getMetadata().get("serviceName"));
            mlc.setSeverity((String) d.getMetadata().get("severity"));
            mlc.setTimestamp((String) d.getMetadata().get("timestamp"));
            mlc.setMessage((String) d.getMetadata().getOrDefault("message", ""));
            mlc.setRawText((String) d.getMetadata().getOrDefault("rawText", ""));
            Object score = d.getMetadata().get("score");
            if (score instanceof Number n) {
                mlc.setScore(n.doubleValue());
            }
            mlc.setMetadata(d.getMetadata());
            return mlc;
        }).collect(Collectors.toList()));
        return resp;
    }
}
