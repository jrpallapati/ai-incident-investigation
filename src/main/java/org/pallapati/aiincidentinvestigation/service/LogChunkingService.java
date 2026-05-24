package org.pallapati.aiincidentinvestigation.service;

import org.pallapati.aiincidentinvestigation.model.LogChunk;
import org.pallapati.aiincidentinvestigation.util.HashingUtil;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Chunks raw log files into semantically meaningful units.
 *
 * Why chunking matters:
 * - Embeddings work best on moderately sized, coherent text blocks (e.g., 300-1200 tokens).
 * - We group lines by temporal/session boundaries and preserve stack traces as a single chunk
 *   to keep crucial cross-line context.
 *
 * Implementation notes:
 * - We use regexes to heuristically identify timestamps, severity, and trace/session IDs.
 * - We build chunks up to a soft size limit or when a new event boundary is detected.
 */
@Service
public class LogChunkingService {

    private static final int SOFT_CHAR_LIMIT = 4000; // stay within reasonable token limits per chunk

    private static final Pattern TIMESTAMP = Pattern.compile("^(?<ts>\\d{4}-\\d{2}-\\d{2}[ T]\\d{2}:\\d{2}:\\d{2}(?:\\.\\d+)?(?:Z|[+\\-]\\d{2}:?\\d{2})?)");
    private static final Pattern SEVERITY = Pattern.compile("\\b(INFO|WARN|ERROR|DEBUG|TRACE|CRITICAL|FATAL)\\b");
    private static final Pattern TRACE_ID = Pattern.compile("trace(id)?=([a-fA-F0-9\\-]{8,})|\\btrace[_-]?id[:=]([a-fA-F0-9\\-]{8,})", Pattern.CASE_INSENSITIVE);
    private static final Pattern SESSION_ID = Pattern.compile("session(id)?=([a-zA-Z0-9\\-]{6,})|\\bsession[_-]?id[:=]([a-zA-Z0-9\\-]{6,})", Pattern.CASE_INSENSITIVE);

    public List<LogChunk> chunkFile(Path file) throws IOException {
        List<LogChunk> chunks = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            StringBuilder buf = new StringBuilder();
            String currentSeverity = null;
            String currentTimestamp = null;
            String currentTrace = null;
            String currentSession = null;
            int lineCount = 0;

            while ((line = reader.readLine()) != null) {
                // detect new event boundary when timestamp at line start is found and buffer has content
                Matcher tsMatcher = TIMESTAMP.matcher(line);
                boolean newEvent = tsMatcher.find() && buf.length() > 0 && lineCount > 0;

                if (newEvent || buf.length() > SOFT_CHAR_LIMIT) {
                    chunks.add(buildChunk(file, buf.toString(), currentSeverity, currentTimestamp, currentTrace, currentSession));
                    buf.setLength(0);
                    currentSeverity = null;
                    currentTimestamp = null;
                    currentTrace = null;
                    currentSession = null;
                }

                // update metadata from this line if present
                if (tsMatcher.find()) {
                    currentTimestamp = tsMatcher.group("ts");
                }
                Matcher sev = SEVERITY.matcher(line);
                if (sev.find()) {
                    currentSeverity = sev.group(1);
                }
                Matcher tr = TRACE_ID.matcher(line);
                if (tr.find()) {
                    currentTrace = firstNonNull(tr.group(2), tr.group(3));
                }
                Matcher sn = SESSION_ID.matcher(line);
                if (sn.find()) {
                    currentSession = firstNonNull(sn.group(2), sn.group(3));
                }

                buf.append(line).append("\n");
                lineCount++;
            }
            if (buf.length() > 0) {
                chunks.add(buildChunk(file, buf.toString(), currentSeverity, currentTimestamp, currentTrace, currentSession));
            }
        }
        return chunks;
    }

    private LogChunk buildChunk(Path file, String text, String severity, String timestamp, String traceId, String sessionId) {
        LogChunk c = new LogChunk();
        c.setSourceFile(file.toString());
        c.setServiceName(inferServiceName(file));
        c.setSeverity(severity);
        c.setTimestamp(timestamp);
        c.setTraceId(traceId);
        c.setSessionId(sessionId);
        // message = first non-empty line
        String message = Arrays.stream(text.split("\n"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .findFirst()
                .orElse("");
        c.setMessage(message);
        c.setRawText(text);
        Map<String, Object> md = new LinkedHashMap<>();
        md.put("sourceFile", c.getSourceFile());
        md.put("serviceName", c.getServiceName());
        if (severity != null) md.put("severity", severity);
        if (timestamp != null) md.put("timestamp", timestamp);
        if (traceId != null) md.put("traceId", traceId);
        if (sessionId != null) md.put("sessionId", sessionId);
        c.setMetadata(md);
        c.setEmbeddingStatus("PENDING");
        c.setContentHash(HashingUtil.sha256(c.getSourceFile() + "|" + c.getTimestamp() + "|" + message.hashCode() + "|" + text.length()));
        return c;
    }

    private String inferServiceName(Path file) {
        // Heuristic: take the parent folder name as service name if present.
        Path parent = file.getParent();
        if (parent != null) {
            return parent.getFileName().toString();
        }
        return "unknown-service";
    }

    private static String firstNonNull(String a, String b) {
        return a != null ? a : b;
    }
}

