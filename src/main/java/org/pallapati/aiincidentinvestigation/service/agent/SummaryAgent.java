package org.pallapati.aiincidentinvestigation.service.agent;

import org.pallapati.aiincidentinvestigation.model.AgentFinding;
import org.pallapati.aiincidentinvestigation.repository.AgentFindingRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * SummaryAgent
 *
 * Purpose: Produce an executive and technical summary of the investigation results.
 */
@Service
public class SummaryAgent {

    private final ChatClient chatClient;
    private final AgentFindingRepository findingRepository;

    private static final String SYSTEM_PROMPT = "You are an SRE summarizer. Create a concise executive and technical summary. Output JSON: {\"summary\":\"...\"}. Avoid speculation beyond provided evidence.";

    public SummaryAgent(ChatClient chatClient, AgentFindingRepository findingRepository) {
        this.chatClient = chatClient;
        this.findingRepository = findingRepository;
    }

    public SummaryResult run(String investigationId, String userQuery, String rootCause, List<String> actions, List<String> logSnippets) {
        String input = buildInput(userQuery, rootCause, actions, logSnippets);
        String content = chatClient.prompt().system(SYSTEM_PROMPT).user(input).call().content();
        SummaryResult r = SummaryResult.fromJson(content);
        persistFinding(investigationId, r, logSnippets);
        return r;
    }

    private String buildInput(String query, String rootCause, List<String> actions, List<String> snippets) {
        StringBuilder sb = new StringBuilder();
        sb.append("Question: ").append(query).append("\n");
        sb.append("Root cause: ").append(rootCause).append("\n");
        sb.append("Actions: ").append(String.join(", ", actions)).append("\n\n");
        for (String s : snippets) {
            if (sb.length() + s.length() > 12000) break;
            sb.append("---\n").append(s).append("\n");
        }
        return sb.toString();
    }

    private void persistFinding(String investigationId, SummaryResult r, List<String> snippets) {
        AgentFinding f = new AgentFinding();
        f.setInvestigationId(investigationId);
        f.setAgentName("SummaryAgent");
        f.setFindingType("SUMMARY");
        f.setFindingText(r.getSummary());
        f.setEvidence(snippets.stream().limit(5).toList());
        f.setConfidenceScore(0.8);
        findingRepository.save(f);
    }

    public static class SummaryResult {
        private String summary;

        public static SummaryResult fromJson(String json) {
            SummaryResult r = new SummaryResult();
            r.summary = DetectionAgent.DetectionResult.extractScalar(json, "summary");
            if (r.summary == null) r.summary = "";
            return r;
        }

        public String getSummary() { return summary; }
    }
}
