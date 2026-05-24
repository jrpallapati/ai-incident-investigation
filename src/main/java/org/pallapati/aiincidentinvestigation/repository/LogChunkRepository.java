package org.pallapati.aiincidentinvestigation.repository;

import org.pallapati.aiincidentinvestigation.model.LogChunk;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface LogChunkRepository extends MongoRepository<LogChunk, String> {
    Optional<LogChunk> findByContentHash(String contentHash);
}

