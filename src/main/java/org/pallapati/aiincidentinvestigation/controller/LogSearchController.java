package org.pallapati.aiincidentinvestigation.controller;

import org.pallapati.aiincidentinvestigation.dto.LogSearchRequest;
import org.pallapati.aiincidentinvestigation.dto.LogSearchResponse;
import org.pallapati.aiincidentinvestigation.service.LogSearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class LogSearchController {

    private final LogSearchService logSearchService;

    public LogSearchController(LogSearchService logSearchService) {
        this.logSearchService = logSearchService;
    }

    @PostMapping("/logs/search")
    public ResponseEntity<LogSearchResponse> search(@RequestBody LogSearchRequest request) {
        return ResponseEntity.ok(logSearchService.search(request));
    }
}

