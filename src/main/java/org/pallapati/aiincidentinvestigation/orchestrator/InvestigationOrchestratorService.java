package org.pallapati.aiincidentinvestigation.orchestrator;

import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.state.AgentState;
import org.pallapati.aiincidentinvestigation.dto.InvestigationRequest;
import org.pallapati.aiincidentinvestigation.dto.InvestigationResponse;
import org.pallapati.aiincidentinvestigation.model.IncidentInvestigation;
import org.pallapati.aiincidentinvestigation.repository.IncidentInvestigationRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class InvestigationOrchestratorService {

    private static final Logger log = LoggerFactory.getLogger(InvestigationOrchestratorService.class);

    private final IncidentInvestigationRepository investigationRepository;
    private final CompiledGraph<AgentState> graph;

    public InvestigationOrchestratorService(IncidentInvestigationRepository investigationRepository,
                                            InvestigationGraphBuilder graphBuilder) {
        this.investigationRepository = investigationRepository;
        this.graph = graphBuilder.build();
    }

    public InvestigationResponse investigate(InvestigationRequest request) {
        log.info("[Orchestrator] create investigation for queryLen={} override={} ", request.getQuery() != null ? request.getQuery().length() : 0, request.getCreateTicketIfNeeded());
        IncidentInvestigation inv = new IncidentInvestigation();
        inv.setQuery(request.getQuery());
        inv.setStatus("RUNNING");
        inv = investigationRepository.save(inv);

        Map<String,Object> input = new HashMap<>();
        input.put("investigationId", inv.getId());
        input.put("query", request.getQuery());
        // Pass nullable createTicketIfNeeded to allow LLM decision when null
        input.put("createTicketIfNeeded", request.getCreateTicketIfNeeded());

        var stateOpt = graph.invoke(input);
        log.info("[Orchestrator] graph invocation completed");
        var state = stateOpt.orElseGet(() -> new org.bsc.langgraph4j.state.AgentState(Map.of()));

        inv.setDetectedSymptoms(state.value("symptoms", java.util.List.of()));
        inv.setAffectedServices(state.value("services", java.util.List.of()));
        inv.setRootCause(state.value("rootCause", ""));
        Object conf = state.value("rootCauseConfidence", 0.7);
        if (conf instanceof Number n) inv.setConfidenceScore(n.doubleValue());
        inv.setRecommendedActions(state.value("actions", java.util.List.of()));
        inv.setSummary(state.value("summary", ""));
        inv.setStatus("COMPLETED");
        investigationRepository.save(inv);

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
        resp.setTicketCreated(Boolean.TRUE.equals(state.value("ticketCreated", false)));
        resp.setTicketId(state.value("ticketId", ""));
        log.info("[Orchestrator] completed investigationId={} ticketCreated={} ticketId={}", inv.getId(), Boolean.TRUE.equals(state.value("ticketCreated", false)), state.value("ticketId", ""));
        return resp;
    }
}
