package org.pallapati.aiincidentinvestigation.service.agent;

import org.pallapati.aiincidentinvestigation.model.AgentFinding;
import org.pallapati.aiincidentinvestigation.model.EscalationTicket;
import org.pallapati.aiincidentinvestigation.repository.AgentFindingRepository;
import org.pallapati.aiincidentinvestigation.repository.EscalationTicketRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * EscalationAgent
 *
 * Purpose: Determine whether escalation is necessary based on severity/confidence and create a ticket.
 * The decision threshold remains deterministic (configurable by request), while AI provides narrative.
 */
@Service
public class EscalationAgent {

    private final ChatClient chatClient;
    private final AgentFindingRepository findingRepository;
    private final EscalationTicketRepository ticketRepository;

    private static final String SYSTEM_PROMPT = "You are an SRE escalation coordinator. Given severity, confidence, and summary, produce a crisp ticket title and description. Output JSON: {\"title\":\"...\",\"description\":\"...\"}.";

    public EscalationAgent(ChatClient chatClient,
                           AgentFindingRepository findingRepository,
                           EscalationTicketRepository ticketRepository) {
        this.chatClient = chatClient;
        this.findingRepository = findingRepository;
        this.ticketRepository = ticketRepository;
    }

    public EscalationResult run(String investigationId, String severity, double confidence, boolean createIfNeeded, String rootCause, List<String> actions, String summary) {
        boolean shouldEscalate = createIfNeeded && ("HIGH".equalsIgnoreCase(severity) || "CRITICAL".equalsIgnoreCase(severity));
        if (!shouldEscalate) {
            persistDecision(investigationId, false, null);
            return new EscalationResult(false, null);
        }
        String input = buildInput(severity, confidence, rootCause, actions, summary);
        String content = chatClient.prompt().system(SYSTEM_PROMPT).user(input).call().content();
        String title = DetectionAgent.DetectionResult.extractScalar(content, "title");
        String description = DetectionAgent.DetectionResult.extractScalar(content, "description");

        EscalationTicket t = new EscalationTicket();
        t.setInvestigationId(investigationId);
        t.setTitle(title != null ? title : ("Incident: " + rootCause));
        t.setDescription(description != null ? description : summary);
        t.setSeverity(severity.toUpperCase());
        t.setStatus("OPEN");
        t.setRootCause(rootCause);
        t.setRecommendedActions(actions);
        EscalationTicket saved = ticketRepository.save(t);

        persistDecision(investigationId, true, saved.getId());
        return new EscalationResult(true, saved.getId());
    }

    private String buildInput(String severity, double confidence, String rootCause, List<String> actions, String summary) {
        return "Severity: " + severity + "\nConfidence: " + confidence + "\nRoot Cause: " + rootCause + "\nActions: " + String.join(", ", actions) + "\nSummary: " + summary;
    }

    private void persistDecision(String investigationId, boolean created, String ticketId) {
        AgentFinding f = new AgentFinding();
        f.setInvestigationId(investigationId);
        f.setAgentName("EscalationAgent");
        f.setFindingType("ESCALATION_DECISION");
        f.setFindingText(created ? "Escalation ticket created: " + ticketId : "No escalation required");
        f.setConfidenceScore(created ? 0.9 : 0.9);
        findingRepository.save(f);
    }

    public record EscalationResult(boolean ticketCreated, String ticketId) {}
}
