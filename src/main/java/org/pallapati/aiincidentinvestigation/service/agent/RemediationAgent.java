package org.pallapati.aiincidentinvestigation.service.agent;

import org.pallapati.aiincidentinvestigation.model.AgentFinding;
import org.pallapati.aiincidentinvestigation.repository.AgentFindingRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * RemediationAgent
 *
 * Purpose: Suggest actionable remediation steps based on identified root cause and evidence.
 */
@Service
public class RemediationAgent {

    private final ChatClient chatClient;
    private final AgentFindingRepository findingRepository;

    private static final String SYSTEM_PROMPT = "You are an SRE remediation assistant. Given root cause and logs, propose safe, concrete remediation steps and risk notes. Output JSON: {\"actions\":[...],\"risks\":\"...\"}.";

    public RemediationAgent(ChatClient chatClient, AgentFindingRepository findingRepository) {
        this.chatClient = chatClient;
        this.findingRepository = findingRepository;
    }

    public RemediationResult run(String investigationId, String rootCause, List<String> logSnippets) {
        String input = buildInput(rootCause, logSnippets);
        String content = chatClient.prompt().system(SYSTEM_PROMPT).user(input).call().content();
        RemediationResult r = RemediationResult.fromJson(content);
        persistFinding(investigationId, r, logSnippets);
        return r;
    }

    private String buildInput(String rootCause, List<String> snippets) {
        StringBuilder sb = new StringBuilder();
        sb.append("Root cause: ").append(rootCause).append("\n\n");
        for (String s : snippets) {
            if (sb.length() + s.length() > 12000) break;
            sb.append("---\n").append(s).append("\n");
        }
        return sb.toString();
    }

    private void persistFinding(String investigationId, RemediationResult r, List<String> snippets) {
        AgentFinding f = new AgentFinding();
        f.setInvestigationId(investigationId);
        f.setAgentName("RemediationAgent");
        f.setFindingType("REMEDIATION");
        f.setFindingText("Actions: " + String.join(" | ", r.getActions()));
        f.setEvidence(snippets.stream().limit(5).toList());
        f.setConfidenceScore(0.7);
        findingRepository.save(f);
    }

    public static class RemediationResult {
        private List<String> actions;
        private String risks;

        public static RemediationResult fromJson(String json) {
            RemediationResult r = new RemediationResult();
            r.actions = DetectionAgent.DetectionResult.extractList(json, "actions");
            r.risks = DetectionAgent.DetectionResult.extractScalar(json, "risks");
            if (r.actions == null) r.actions = List.of();
            return r;
        }

        public List<String> getActions() { return actions; }
        public String getRisks() { return risks; }
    }
}
