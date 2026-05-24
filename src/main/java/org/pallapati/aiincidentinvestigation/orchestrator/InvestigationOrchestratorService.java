package org.pallapati.aiincidentinvestigation.orchestrator;

import org.pallapati.aiincidentinvestigation.dto.InvestigationRequest;
import org.pallapati.aiincidentinvestigation.dto.InvestigationResponse;
import org.pallapati.aiincidentinvestigation.model.IncidentInvestigation;
import org.pallapati.aiincidentinvestigation.repository.IncidentInvestigationRepository;
import org.pallapati.aiincidentinvestigation.service.LogSearchService;
import org.pallapati.aiincidentinvestigation.service.agent.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Coordinates the end-to-end multi-agent incident investigation flow.
 *
 * Flow:
 * 1) semantic search for relevant logs
 * 2) DetectionAgent -> symptoms/services/severity
 * 3) CorrelationAgent -> cross-service patterns
 * 4) RootCauseAgent -> probable root cause
 * 5) RemediationAgent -> recommended actions
 * 6) SummaryAgent -> final narrative
 * 7) EscalationAgent -> optional ticket creation
 */
@Service
public class InvestigationOrchestratorService {

    private final LogSearchService logSearchService;
    private final DetectionAgent detectionAgent;
    private final CorrelationAgent correlationAgent;
    private final RootCauseAgent rootCauseAgent;
    private final RemediationAgent remediationAgent;
    private final SummaryAgent summaryAgent;
    private final EscalationAgent escalationAgent;
    private final IncidentInvestigationRepository investigationRepository;

    public InvestigationOrchestratorService(LogSearchService logSearchService,
                                            DetectionAgent detectionAgent,
                                            CorrelationAgent correlationAgent,
                                            RootCauseAgent rootCauseAgent,
                                            RemediationAgent remediationAgent,
                                            SummaryAgent summaryAgent,
                                            EscalationAgent escalationAgent,
                                            IncidentInvestigationRepository investigationRepository) {
        this.logSearchService = logSearchService;
        this.detectionAgent = detectionAgent;
        this.correlationAgent = correlationAgent;
        this.rootCauseAgent = rootCauseAgent;
        this.remediationAgent = remediationAgent;
        this.summaryAgent = summaryAgent;
        this.escalationAgent = escalationAgent;
        this.investigationRepository = investigationRepository;
    }

    public InvestigationResponse investigate(InvestigationRequest request) {
        // Persist initial investigation record
        IncidentInvestigation inv = new IncidentInvestigation();
        inv.setQuery(request.getQuery());
        inv.setStatus("RUNNING");
        inv = investigationRepository.save(inv);

        // 1) Semantic search
        var searchReq = new org.pallapati.aiincidentinvestigation.dto.LogSearchRequest();
        searchReq.setQuery(request.getQuery());
        searchReq.setTopK(12);
        var searchResp = logSearchService.search(searchReq);
        List<String> snippets = new ArrayList<>();
        searchResp.getMatchingLogChunks().forEach(c -> snippets.add(c.getRawText()));

        // 2) Detection
        var det = detectionAgent.run(inv.getId(), request.getQuery(), snippets);
        inv.setDetectedSymptoms(det.getSymptoms());
        inv.setAffectedServices(det.getServices());

        // 3) Correlation
        var corr = correlationAgent.run(inv.getId(), request.getQuery(), snippets);

        // 4) Root Cause
        var rc = rootCauseAgent.run(inv.getId(), request.getQuery(), det.getSymptoms(), corr.getPatterns(), snippets);
        inv.setRootCause(rc.getRootCause());
        inv.setConfidenceScore(rc.getConfidence());

        // 5) Remediation
        var rem = remediationAgent.run(inv.getId(), rc.getRootCause(), snippets);
        inv.setRecommendedActions(rem.getActions());

        // 6) Summary
        var sum = summaryAgent.run(inv.getId(), request.getQuery(), rc.getRootCause(), rem.getActions(), snippets);
        inv.setSummary(sum.getSummary());

        // 7) Escalation
        var esc = escalationAgent.run(inv.getId(), det.getSeverity(), inv.getConfidenceScore() != null ? inv.getConfidenceScore() : 0.6, request.isCreateTicketIfNeeded(), rc.getRootCause(), rem.getActions(), sum.getSummary());

        inv.setStatus("COMPLETED");
        investigationRepository.save(inv);

        // Build response
        InvestigationResponse resp = new InvestigationResponse();
        resp.setInvestigationId(inv.getId());
        resp.setQuery(inv.getQuery());
        resp.setStatus(inv.getStatus());
        resp.setSymptoms(inv.getDetectedSymptoms());
        resp.setAffectedServices(inv.getAffectedServices());
        resp.setRootCause(inv.getRootCause());
        resp.setConfidenceScore(inv.getConfidenceScore());
        resp.setRecommendedActions(inv.getRecommendedActions());
        resp.setSummary(inv.getSummary());
        resp.setTicketCreated(esc.ticketCreated());
        resp.setTicketId(esc.ticketId());
        return resp;
    }
}

