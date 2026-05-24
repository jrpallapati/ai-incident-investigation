package org.pallapati.aiincidentinvestigation.service.agent;

import org.pallapati.aiincidentinvestigation.model.AgentFinding;
import org.pallapati.aiincidentinvestigation.repository.AgentFindingRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * CorrelationAgent
 *
 * Purpose: Analyze cross-service relationships and patterns across the provided log snippets.
 * Deterministic code assembles input and persists results; AI reasons about correlations.
 */
@Service
public class CorrelationAgent {

    private final ChatClient chatClient;
    private final AgentFindingRepository findingRepository;

    private static final String SYSTEM_PROMPT = "You are an SRE correlation analyst. From the logs, infer correlations across services, timing, and dependencies. Output compact JSON: {\"patterns\":[...],\"notes\":\"...\",\"confidence\":0-1}.";

    public CorrelationAgent(ChatClient chatClient, AgentFindingRepository findingRepository) {
        this.chatClient = chatClient;
        this.findingRepository = findingRepository;
    }

    public CorrelationResult run(String investigationId, String userQuery, List<String> logSnippets) {
        String input = buildInput(userQuery, logSnippets);
        String content = chatClient.prompt().system(SYSTEM_PROMPT).user(input).call().content();
        CorrelationResult r = CorrelationResult.fromJson(content);
        persistFinding(investigationId, r, logSnippets);
        return r;
    }

    private String buildInput(String query, List<String> snippets) {
        StringBuilder sb = new StringBuilder();
        sb.append("Question: ").append(query).append("\n\n");
        for (String s : snippets) {
            if (sb.length() + s.length() > 12000) break;
            sb.append("---\n").append(s).append("\n");
        }
        return sb.toString();
    }

    private void persistFinding(String investigationId, CorrelationResult r, List<String> snippets) {
        AgentFinding f = new AgentFinding();
        f.setInvestigationId(investigationId);
        f.setAgentName("CorrelationAgent");
        f.setFindingType("PATTERNS");
        f.setFindingText("Patterns: " + String.join(" | ", r.getPatterns()));
        f.setEvidence(snippets.stream().limit(5).toList());
        f.setConfidenceScore(r.getConfidence());
        findingRepository.save(f);
    }

    public static class CorrelationResult {
        private List<String> patterns;
        private Double confidence = 0.7;

        public static CorrelationResult fromJson(String json) {
            CorrelationResult r = new CorrelationResult();
            r.patterns = DetectionAgent.DetectionResult.extractList(json, "patterns");
            String conf = DetectionAgent.DetectionResult.extractScalar(json, "confidence");
            if (conf != null) {
                try { r.confidence = Double.parseDouble(conf); } catch (Exception ignored) {}
            }
            if (r.patterns == null) r.patterns = List.of();
            return r;
        }

        public List<String> getPatterns() { return patterns; }
        public Double getConfidence() { return confidence; }
    }
}
