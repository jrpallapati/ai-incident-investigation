package org.pallapati.aiincidentinvestigation.service.agent;

import org.pallapati.aiincidentinvestigation.model.AgentFinding;
import org.pallapati.aiincidentinvestigation.repository.AgentFindingRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RootCauseAgent
 *
 * Purpose: Given the detected symptoms and correlated patterns, determine the most probable root cause
 * and provide a ranked rationale with a confidence score.
 */
@Service
public class RootCauseAgent {

    private static final Logger log = LoggerFactory.getLogger(RootCauseAgent.class);

    private final ChatClient chatClient;
    private final AgentFindingRepository findingRepository;

    private static final String SYSTEM_PROMPT = "You are a Root Cause Analysis assistant for distributed systems. Given symptoms and patterns with logs, propose the most probable root cause and rank alternatives. Output JSON: {\"rootCause\":\"...\",\"alternatives\":[...],\"confidence\":0-1,\"rationale\":\"...\"}.";

    public RootCauseAgent(ChatClient chatClient, AgentFindingRepository findingRepository) {
        this.chatClient = chatClient;
        this.findingRepository = findingRepository;
    }

    public RootCauseResult run(String investigationId, String userQuery, List<String> symptoms, List<String> patterns, List<String> logSnippets) {
        log.info("[Agent:RootCause] start investigationId={} symptoms={} patterns={} snippets={}", investigationId, symptoms != null ? symptoms.size() : 0, patterns != null ? patterns.size() : 0, logSnippets != null ? logSnippets.size() : 0);
        long t0 = System.currentTimeMillis();
        String content = chatClient.prompt().system(SYSTEM_PROMPT).user(buildInput(userQuery, symptoms, patterns, logSnippets)).call().content();
        long dt = System.currentTimeMillis() - t0;
        log.info("[Agent:RootCause] LLM call finished in {} ms", dt);
        RootCauseResult r = RootCauseResult.fromJson(content);
        persistFinding(investigationId, r, logSnippets);
        return r;
    }

    private String buildInput(String query, List<String> symptoms, List<String> patterns, List<String> snippets) {
        StringBuilder sb = new StringBuilder();
        sb.append("Question: ").append(query).append("\n");
        sb.append("Symptoms: ").append(String.join(", ", symptoms)).append("\n");
        sb.append("Patterns: ").append(String.join(", ", patterns)).append("\n\n");
        for (String s : snippets) {
            if (sb.length() + s.length() > 12000) break;
            sb.append("---\n").append(s).append("\n");
        }
        return sb.toString();
    }

    private void persistFinding(String investigationId, RootCauseResult r, List<String> snippets) {
        AgentFinding f = new AgentFinding();
        f.setInvestigationId(investigationId);
        f.setAgentName("RootCauseAgent");
        f.setFindingType("ROOT_CAUSE");
        f.setFindingText("Root cause: " + r.getRootCause());
        f.setEvidence(snippets.stream().limit(5).toList());
        f.setConfidenceScore(r.getConfidence());
        findingRepository.save(f);
    }

    public static class RootCauseResult {
        private String rootCause;
        private List<String> alternatives;
        private Double confidence = 0.7;

        public static RootCauseResult fromJson(String json) {
            RootCauseResult r = new RootCauseResult();
            r.rootCause = DetectionAgent.DetectionResult.extractScalar(json, "rootCause");
            r.alternatives = DetectionAgent.DetectionResult.extractList(json, "alternatives");
            String conf = DetectionAgent.DetectionResult.extractScalar(json, "confidence");
            if (conf != null) {
                try { r.confidence = Double.parseDouble(conf); } catch (Exception ignored) {}
            }
            if (r.alternatives == null) r.alternatives = List.of();
            if (r.rootCause == null) r.rootCause = "Unknown"; // fallback when model omits field
            return r;
        }

        public String getRootCause() { return rootCause; }
        public List<String> getAlternatives() { return alternatives; }
        public Double getConfidence() { return confidence; }
    }
}
