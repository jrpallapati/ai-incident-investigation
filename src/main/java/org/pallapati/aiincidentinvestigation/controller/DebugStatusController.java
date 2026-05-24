package org.pallapati.aiincidentinvestigation.controller;

import org.pallapati.aiincidentinvestigation.repository.LogChunkRepository;
import org.pallapati.aiincidentinvestigation.service.LogDirectoryScannerService;
import org.pallapati.aiincidentinvestigation.service.admin.IngestionAdminService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.mongodb.client.MongoCollection;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bson.Document;

@RestController
@RequestMapping("/api/debug")
public class DebugStatusController {

    private final LogDirectoryScannerService scannerService;
    private final LogChunkRepository logChunkRepository;
    private final MongoTemplate mongoTemplate;
    private final IngestionAdminService ingestionAdminService;

    @Value("${app.logs.path:./sample-logs}")
    private String logsPath;

    @Value("${spring.ai.vectorstore.mongodb.atlas.collection-name}")
    private String vectorCollectionName;

    public DebugStatusController(LogDirectoryScannerService scannerService,
                                 LogChunkRepository logChunkRepository,
                                 MongoTemplate mongoTemplate,
                                 IngestionAdminService ingestionAdminService) {
        this.scannerService = scannerService;
        this.logChunkRepository = logChunkRepository;
        this.mongoTemplate = mongoTemplate;
        this.ingestionAdminService = ingestionAdminService;
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        Map<String, Object> m = new HashMap<>();
        m.put("logsPath", logsPath);
        try {
            m.put("logFilesFound", scannerService.findLogFiles().size());
        } catch (IOException e) {
            m.put("logFilesFound", 0);
            m.put("logScanError", e.getMessage());
        }
        m.put("logChunksInMongo", logChunkRepository.count());
        try {
            MongoCollection<org.bson.Document> coll = mongoTemplate.getDb().getCollection(vectorCollectionName);
            long vectorDocs = coll.countDocuments();
            m.put("vectorDocsInAtlas", vectorDocs);
            m.put("vectorCollection", vectorCollectionName);
        } catch (Exception e) {
            m.put("vectorDocsInAtlas", 0);
            m.put("vectorCountError", e.getMessage());
        }
        return ResponseEntity.ok(m);
    }

    @PostMapping("/reembed")
    public ResponseEntity<Map<String, Object>> reembedAll() {
        long processed = ingestionAdminService.reembedAll();
        return ResponseEntity.ok(Map.of("reembedded", processed));
    }

    @PostMapping("/rescan")
    public ResponseEntity<Map<String, Object>> rescan() throws IOException {
        long added = ingestionAdminService.rescanAndIngest();
        return ResponseEntity.ok(Map.of("newChunks", added));
    }

    @GetMapping("/vector-sample")
    public ResponseEntity<Map<String, Object>> vectorSample() {
        Map<String, Object> out = new HashMap<>();
        try {
            MongoCollection<org.bson.Document> coll = mongoTemplate.getDb().getCollection(vectorCollectionName);
            long count = coll.countDocuments();
            List<org.bson.Document> first = coll.find().limit(3).into(new java.util.ArrayList<>());
            out.put("collection", vectorCollectionName);
            out.put("count", count);
            out.put("sample", first);
        } catch (Exception e) {
            out.put("error", e.getMessage());
        }
        return ResponseEntity.ok(out);
    }

    @GetMapping("/indexes")
    public ResponseEntity<Map<String, Object>> listSearchIndexes() {
        Map<String, Object> out = new HashMap<>();
        try {
            Document cmd = new Document("listSearchIndexes", vectorCollectionName);
            Document result = mongoTemplate.getDb().runCommand(cmd);
            out.put("result", result);
            out.put("collection", vectorCollectionName);
        } catch (Exception e) {
            out.put("error", e.getMessage());
        }
        return ResponseEntity.ok(out);
    }
}
