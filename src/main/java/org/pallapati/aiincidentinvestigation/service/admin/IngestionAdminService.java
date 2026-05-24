package org.pallapati.aiincidentinvestigation.service.admin;

import org.pallapati.aiincidentinvestigation.model.LogChunk;
import org.pallapati.aiincidentinvestigation.repository.LogChunkRepository;
import org.pallapati.aiincidentinvestigation.service.LogChunkingService;
import org.pallapati.aiincidentinvestigation.service.LogDirectoryScannerService;
import org.pallapati.aiincidentinvestigation.service.LogEmbeddingService;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service
public class IngestionAdminService {

    private final LogChunkRepository logChunkRepository;
    private final LogEmbeddingService logEmbeddingService;
    private final LogDirectoryScannerService scannerService;
    private final LogChunkingService chunkingService;

    public IngestionAdminService(LogChunkRepository logChunkRepository,
                                 LogEmbeddingService logEmbeddingService,
                                 LogDirectoryScannerService scannerService,
                                 LogChunkingService chunkingService) {
        this.logChunkRepository = logChunkRepository;
        this.logEmbeddingService = logEmbeddingService;
        this.scannerService = scannerService;
        this.chunkingService = chunkingService;
    }

    public long reembedAll() {
        List<LogChunk> all = logChunkRepository.findAll();
        all.forEach(c -> c.setEmbeddingStatus("PENDING"));
        logChunkRepository.saveAll(all);
        logEmbeddingService.reembedAll(all);
        return all.size();
    }

    public long rescanAndIngest() throws IOException {
        List<Path> files = scannerService.findLogFiles();
        List<LogChunk> toPersist = new ArrayList<>();
        for (Path f : files) {
            List<LogChunk> chunks = chunkingService.chunkFile(f);
            for (LogChunk c : chunks) {
                if (logChunkRepository.findByContentHash(c.getContentHash()).isEmpty()) {
                    toPersist.add(c);
                }
            }
        }
        if (!toPersist.isEmpty()) {
            logChunkRepository.saveAll(toPersist);
            logEmbeddingService.embedAndStore(toPersist);
        }
        return toPersist.size();
    }
}
