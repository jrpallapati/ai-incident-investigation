package org.pallapati.aiincidentinvestigation.service;

import org.pallapati.aiincidentinvestigation.model.LogChunk;
import org.pallapati.aiincidentinvestigation.repository.LogChunkRepository;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Generates embeddings for log chunks and stores them in MongoDB Atlas VectorStore.
 *
 * Why embeddings matter:
 * - They convert text into numeric vectors capturing semantic similarity. This enables fuzzy
 *   natural-language search across large, messy logs.
 *
 * Metadata importance:
 * - We mirror deterministic fields from LogChunk to the Document metadata so we can filter and
 *   present rich context in search results without extra DB round-trips.
 */
@Service
public class LogEmbeddingService {

    private final VectorStore vectorStore;
    private final LogChunkRepository logChunkRepository;

    public LogEmbeddingService(VectorStore vectorStore, LogChunkRepository logChunkRepository) {
        this.vectorStore = vectorStore;
        this.logChunkRepository = logChunkRepository;
    }

    @Transactional
    public void embedAndStore(List<LogChunk> chunks) {
        List<LogChunk> toEmbed = chunks.stream()
                .filter(c -> c.getEmbeddingStatus() == null || !"EMBEDDED".equals(c.getEmbeddingStatus()))
                .collect(Collectors.toList());
        if (toEmbed.isEmpty()) return;

        List<Document> docs = toEmbed.stream().map(this::toDocument).collect(Collectors.toList());
        vectorStore.add(docs);

        toEmbed.forEach(c -> c.setEmbeddingStatus("EMBEDDED"));
        logChunkRepository.saveAll(toEmbed);
    }

    @Transactional
    public long reembedAll(List<LogChunk> chunks) {
        // Force re-embedding regardless of prior status
        List<Document> docs = chunks.stream().map(this::toDocument).collect(Collectors.toList());
        vectorStore.add(docs);
        chunks.forEach(c -> c.setEmbeddingStatus("EMBEDDED"));
        logChunkRepository.saveAll(chunks);
        return chunks.size();
    }

    private Document toDocument(LogChunk c) {
        Map<String, Object> md = c.getMetadata();
        md.putIfAbsent("rawText", c.getRawText());
        md.putIfAbsent("message", c.getMessage());
        md.put("contentHash", c.getContentHash());
        return new Document(c.getRawText(), md);
    }
}
