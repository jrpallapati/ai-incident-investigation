package org.pallapati.aiincidentinvestigation.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Recursively scans a configured local directory for log files.
 *
 * Deterministic responsibilities:
 * - Strictly file-system I/O: list, walk, filter by extensions (.log, .txt).
 * - No AI logic here; just returns discovered files.
 */
@Service
public class LogDirectoryScannerService {

    private final Path rootPath;

    public LogDirectoryScannerService(@Value("${app.logs.path:./sample-logs}") String logsPath) {
        this.rootPath = Paths.get(logsPath).toAbsolutePath().normalize();
    }

    public List<Path> findLogFiles() throws IOException {
        if (!Files.exists(rootPath) || !Files.isDirectory(rootPath)) {
            throw new NoSuchFileException("Log directory not found: " + rootPath);
        }
        List<Path> result = new ArrayList<>();
        try (var stream = Files.walk(rootPath)) {
            stream.filter(p -> Files.isRegularFile(p))
                    .filter(p -> {
                        String name = p.getFileName().toString().toLowerCase();
                        return name.endsWith(".log") || name.endsWith(".txt");
                    })
                    .forEach(result::add);
        }
        return result;
    }
}

