package org.pallapati.aiincidentinvestigation.service.agent;

import org.pallapati.aiincidentinvestigation.model.AgentFinding;
import org.pallapati.aiincidentinvestigation.repository.AgentFindingRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * DetectionAgent
 *
 * Architecture:
 * - Dedicated Spring service with its own prompt and ChatClient call.
 * - Deterministic responsibilities: assembling context (vector search results), validating fields,
 *   persisting AgentFinding.
 * - AI responsibilities: extracting symptoms, impacted services, and severity estimate from logs.
 */
@Service
public class DetectionAgent {

    private final ChatClient chatClient;
    private final AgentFindingRepository findingRepository;

    private static final String SYSTEM_PROMPT = "You are a seasoned SRE incident detector. Given log excerpts and a user question, identify: (1) key symptoms, (2) impacted services, (3) rough severity (LOW/MEDIUM/HIGH/CRITICAL). Respond as compact JSON with fields symptoms[], services[], severity, confidence, notes.";

    public DetectionAgent(ChatClient chatClient, AgentFindingRepository findingRepository) {
        this.chatClient = chatClient;
        this.findingRepository = findingRepository;
    }

    public DetectionResult run(String investigationId, String userQuery, List<String> logSnippets) {
        String input = buildInput(userQuery, logSnippets);
        String content = chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(input)
                .call()
                .content();

        DetectionResult result = DetectionResult.fromJson(content);
        persistFinding(investigationId, result, logSnippets);
        return result;
    }

    private String buildInput(String query, List<String> snippets) {
        StringBuilder sb = new StringBuilder();
        sb.append("Question: ").append(query).append("\n\n");
        sb.append("Log snippets (truncate to fit):\n");
        for (String s : snippets) {
            if (sb.length() + s.length() > 12000) break; // keep prompt under safe token budget
            sb.append("---\n").append(s).append("\n");
        }
        return sb.toString();
    }

    private void persistFinding(String investigationId, DetectionResult r, List<String> snippets) {
        AgentFinding f = new AgentFinding();
        f.setInvestigationId(investigationId);
        f.setAgentName("DetectionAgent");
        f.setFindingType("SYMPTOMS");
        f.setFindingText("Symptoms: " + String.join(", ", r.getSymptoms()) + "; Services: " + String.join(", ", r.getServices()) + "; Severity: " + r.getSeverity());
        f.setEvidence(snippets.stream().limit(5).toList());
        f.setConfidenceScore(r.getConfidence());
        findingRepository.save(f);
    }

    // Structured result DTO for agent output
    public static class DetectionResult {
        private List<String> symptoms;
        private List<String> services;
        private String severity;
        private Double confidence = 0.7; // default if model omits

        public static DetectionResult fromJson(String json) {
            DetectionResult r = new DetectionResult();
            r.symptoms = extractList(json, "symptoms");
            r.services = extractList(json, "services");
            r.severity = extractScalar(json, "severity");
            String conf = extractScalar(json, "confidence");
            if (conf != null) {
                try { r.confidence = Double.parseDouble(conf); } catch (Exception ignored) {}
            }
            if (r.symptoms == null) r.symptoms = List.of();
            if (r.services == null) r.services = List.of();
            if (r.severity == null) r.severity = "MEDIUM";
            return r;
        }

        public static List<String> extractList(String json, String key) {
            int i = json.indexOf('"' + key + '"');
            if (i < 0) return List.of();
            int lb = json.indexOf('[', i);
            int rb = json.indexOf(']', lb);
            if (lb < 0 || rb < 0) return List.of();
            String arr = json.substring(lb + 1, rb);
            return Arrays.stream(arr.split(","))
                    .map(s -> s.replaceAll("[\\\\\"\\s]", "").trim())
                    .filter(s -> !s.isEmpty())
                    .toList();
        }

        public static String extractScalar(String json, String key) {
            int i = json.indexOf('"' + key + '"');
            if (i < 0) return null;
            int c = json.indexOf(':', i);
            if (c < 0) return null;
            String rest = json.substring(c + 1).trim();
            if (rest.startsWith("\"")) {
                int end = rest.indexOf('"', 1);
                if (end > 1) return rest.substring(1, end);
            } else {
                int end = rest.indexOf(',');
                if (end < 0) end = rest.indexOf('}');
                if (end > 0) return rest.substring(0, end).trim();
            }
            return null;
        }

        public List<String> getSymptoms() { return symptoms; }
        public List<String> getServices() { return services; }
        public String getSeverity() { return severity; }
        public Double getConfidence() { return confidence; }
    }
}
