package org.pallapati.aiincidentinvestigation.service.agent;

import org.pallapati.aiincidentinvestigation.model.AgentFinding;
import org.pallapati.aiincidentinvestigation.model.EscalationTicket;
import org.pallapati.aiincidentinvestigation.repository.AgentFindingRepository;
import org.pallapati.aiincidentinvestigation.repository.EscalationTicketRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * EscalationAgent
 *
 * Purpose: Determine whether escalation is necessary based on severity/confidence and create a ticket.
 * The decision threshold remains deterministic (configurable by request), while AI provides narrative.
 */
@Service
public class EscalationAgent {

    private static final Logger log = LoggerFactory.getLogger(EscalationAgent.class);

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

    public EscalationResult run(String investigationId, String severity, double confidence, Boolean createIfNeeded, String rootCause, List<String> actions, String summary) {
        log.info("[Agent:Escalation] start investigationId={} severity={} confidence={} override={} actions={} ", investigationId, severity, String.format("%.2f", confidence), createIfNeeded, actions != null ? actions.size() : 0);
        boolean shouldAutoCreate = createIfNeeded == null;
        boolean shouldEscalate = (createIfNeeded != null && createIfNeeded)
                || (shouldAutoCreate && ("HIGH".equalsIgnoreCase(severity) || "CRITICAL".equalsIgnoreCase(severity)));
        if (!shouldEscalate) {
            log.info("[Agent:Escalation] decision=no-escalation");
            persistDecision(investigationId, false, null);
            return new EscalationResult(false, null);
        }
        String input = buildInput(severity, confidence, rootCause, actions, summary);
        long t0 = System.currentTimeMillis();
        String content = chatClient.prompt().system(SYSTEM_PROMPT).user(input).call().content();
        long dt = System.currentTimeMillis() - t0;
        log.info("[Agent:Escalation] LLM call finished in {} ms", dt);
        String title = DetectionAgent.DetectionResult.extractScalar(content, "title");
        String description = DetectionAgent.DetectionResult.extractScalar(content, "description");

        var existing = ticketRepository.findAll().stream()
                .filter(t -> investigationId.equals(t.getInvestigationId()))
                .findFirst();
        if (existing.isPresent()) {
            log.info("[Agent:Escalation] existing-ticket id={}", existing.get().getId());
            persistDecision(investigationId, true, existing.get().getId());
            return new EscalationResult(true, existing.get().getId());
        }

        EscalationTicket t = new EscalationTicket();
        t.setInvestigationId(investigationId);
        t.setTitle(title != null ? title : ("Incident: " + rootCause));
        t.setDescription(description != null ? description : summary);
        t.setSeverity(severity.toUpperCase());
        t.setStatus("OPEN");
        t.setRootCause(rootCause);
        t.setRecommendedActions(actions);
        EscalationTicket saved = ticketRepository.save(t);
        log.info("[Agent:Escalation] created-ticket id={}", saved.getId());

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
