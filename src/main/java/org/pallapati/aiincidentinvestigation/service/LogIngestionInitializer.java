package org.pallapati.aiincidentinvestigation.service;

import org.pallapati.aiincidentinvestigation.model.LogChunk;
import org.pallapati.aiincidentinvestigation.repository.LogChunkRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Application startup initializer that scans local logs, chunks them, and embeds into vector store.
 *
 * Deterministic responsibilities: file I/O, chunking, deduplication, persistence status updates.
 * AI responsibility: embeddings creation via the configured EmbeddingModel when adding to VectorStore.
 */
@Component
public class LogIngestionInitializer {

    private static final Logger log = LoggerFactory.getLogger(LogIngestionInitializer.class);

    private final LogDirectoryScannerService scannerService;
    private final LogChunkingService chunkingService;
    private final LogEmbeddingService embeddingService;
    private final LogChunkRepository logChunkRepository;

    private final boolean ingestionEnabled;

    public LogIngestionInitializer(LogDirectoryScannerService scannerService,
                                   LogChunkingService chunkingService,
                                   LogEmbeddingService embeddingService,
                                   LogChunkRepository logChunkRepository,
                                   @Value("${app.ingestion.enabled:true}") boolean ingestionEnabled) {
        this.scannerService = scannerService;
        this.chunkingService = chunkingService;
        this.embeddingService = embeddingService;
        this.logChunkRepository = logChunkRepository;
        this.ingestionEnabled = ingestionEnabled;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        if (!ingestionEnabled) {
            log.info("Log ingestion disabled via property app.ingestion.enabled=false");
            return;
        }
        try {
            List<Path> files = scannerService.findLogFiles();
            List<LogChunk> all = new ArrayList<>();
            for (Path f : files) {
                List<LogChunk> chunks = chunkingService.chunkFile(f);
                // Ensure contentHash uniqueness via repository lookup to avoid duplicates across restarts
                for (LogChunk c : chunks) {
                    logChunkRepository.findByContentHash(c.getContentHash()).ifPresentOrElse(existing -> {
                        // Skip, already persisted and presumably embedded
                    }, () -> all.add(c));
                }
            }
            if (!all.isEmpty()) {
                log.info("Persisting {} new chunks", all.size());
                logChunkRepository.saveAll(all);
                embeddingService.embedAndStore(all);
                log.info("Embedding completed for {} chunks", all.size());
            } else {
                log.info("No new chunks to ingest; skipping embedding.");
            }
        } catch (IOException e) {
            log.error("Failed to scan logs", e);
        } catch (Exception e) {
            log.error("Unexpected error during log ingestion", e);
        }
    }
}

