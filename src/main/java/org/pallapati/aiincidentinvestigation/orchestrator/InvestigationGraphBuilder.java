package org.pallapati.aiincidentinvestigation.orchestrator;

import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.action.NodeAction;
import org.bsc.langgraph4j.state.AgentState;
import org.bsc.langgraph4j.GraphStateException;
import org.pallapati.aiincidentinvestigation.dto.LogSearchRequest;
import org.pallapati.aiincidentinvestigation.service.LogSearchService;
import org.pallapati.aiincidentinvestigation.service.agent.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Builds a LangGraph DAG that orchestrates the multi-agent investigation.
 *
 * Topology:
 * START -> semanticSearch -> [detection, correlation] -> rootCause -> remediation -> summary -> escalation -> END
 *
 * Each node returns a partial state delta Map<String,Object> which LangGraph merges into the AgentState.
 * We keep all deterministic persistence inside each agent (AgentFinding, EscalationTicket).
 */
public class InvestigationGraphBuilder {

    private static final Logger log = LoggerFactory.getLogger(InvestigationGraphBuilder.class);

    // Node IDs (stable names for graph introspection/diagrams)
    public static final String NODE_SEARCH = "semanticSearch";
    public static final String NODE_DETECTION = "detection";
    public static final String NODE_CORRELATION = "correlation";
    public static final String NODE_ROOT_CAUSE = "rootCause";
    public static final String NODE_REMEDIATION = "remediation";
    public static final String NODE_SUMMARY = "summary";
    public static final String NODE_ESCALATION = "escalation";

    private final LogSearchService logSearchService;
    private final DetectionAgent detectionAgent;
    private final CorrelationAgent correlationAgent;
    private final RootCauseAgent rootCauseAgent;
    private final RemediationAgent remediationAgent;
    private final SummaryAgent summaryAgent;
    private final EscalationAgent escalationAgent;

    public InvestigationGraphBuilder(LogSearchService logSearchService,
                                     DetectionAgent detectionAgent,
                                     CorrelationAgent correlationAgent,
                                     RootCauseAgent rootCauseAgent,
                                     RemediationAgent remediationAgent,
                                     SummaryAgent summaryAgent,
                                     EscalationAgent escalationAgent) {
        this.logSearchService = logSearchService;
        this.detectionAgent = detectionAgent;
        this.correlationAgent = correlationAgent;
        this.rootCauseAgent = rootCauseAgent;
        this.remediationAgent = remediationAgent;
        this.summaryAgent = summaryAgent;
        this.escalationAgent = escalationAgent;
    }

    public CompiledGraph<AgentState> build() {
        try {
            log.info("[Graph] building investigation DAG with parallel detection/correlation");
            StateGraph<AgentState> g = new StateGraph<>(AgentState::new);

            g.addNode(NODE_SEARCH, AsyncNodeAction.node_async((NodeAction<AgentState>) state -> {
                String query = state.value("query", "");
                LogSearchRequest req = new LogSearchRequest();
                req.setQuery(query);
                req.setTopK(16);
                var resp = logSearchService.search(req);
                List<String> snippets = resp.getMatchingLogChunks().stream().map(c -> c.getRawText()).toList();
                Map<String, Object> delta = new HashMap<>();
                delta.put("snippets", snippets);
                return delta;
            }));

            g.addNode(NODE_DETECTION, AsyncNodeAction.node_async((NodeAction<AgentState>) state -> {
                String invId = state.value("investigationId", "");
                String query = state.value("query", "");
                @SuppressWarnings("unchecked")
                List<String> snippets = (List<String>) state.value("snippets", java.util.Collections.<String>emptyList());
                var r = detectionAgent.run(invId, query, snippets);
                Map<String, Object> delta = new HashMap<>();
                delta.put("symptoms", r.getSymptoms());
                delta.put("services", r.getServices());
                delta.put("severity", r.getSeverity());
                delta.put("detectionConfidence", r.getConfidence());
                return delta;
            }));

            g.addNode(NODE_CORRELATION, AsyncNodeAction.node_async((NodeAction<AgentState>) state -> {
                String invId = state.value("investigationId", "");
                String query = state.value("query", "");
                @SuppressWarnings("unchecked")
                List<String> snippets = (List<String>) state.value("snippets", java.util.Collections.<String>emptyList());
                var r = correlationAgent.run(invId, query, snippets);
                Map<String, Object> delta = new HashMap<>();
                delta.put("patterns", r.getPatterns());
                delta.put("correlationConfidence", r.getConfidence());
                return delta;
            }));

            g.addNode(NODE_ROOT_CAUSE, AsyncNodeAction.node_async((NodeAction<AgentState>) state -> {
                String invId = state.value("investigationId", "");
                String query = state.value("query", "");
                @SuppressWarnings("unchecked")
                List<String> symptoms = (List<String>) state.value("symptoms", java.util.Collections.<String>emptyList());
                @SuppressWarnings("unchecked")
                List<String> patterns = (List<String>) state.value("patterns", java.util.Collections.<String>emptyList());
                @SuppressWarnings("unchecked")
                List<String> snippets = (List<String>) state.value("snippets", java.util.Collections.<String>emptyList());
                var r = rootCauseAgent.run(invId, query, symptoms, patterns, snippets);
                Map<String, Object> delta = new HashMap<>();
                delta.put("rootCause", r.getRootCause());
                delta.put("rootCauseConfidence", r.getConfidence());
                return delta;
            }));

            g.addNode(NODE_REMEDIATION, AsyncNodeAction.node_async((NodeAction<AgentState>) state -> {
                String invId = state.value("investigationId", "");
                String rootCause = state.value("rootCause", "");
                @SuppressWarnings("unchecked")
                List<String> snippets = (List<String>) state.value("snippets", java.util.Collections.<String>emptyList());
                var r = remediationAgent.run(invId, rootCause, snippets);
                Map<String, Object> delta = new HashMap<>();
                delta.put("actions", r.getActions());
                return delta;
            }));

            g.addNode(NODE_SUMMARY, AsyncNodeAction.node_async((NodeAction<AgentState>) state -> {
                String invId = state.value("investigationId", "");
                String query = state.value("query", "");
                String rootCause = state.value("rootCause", "");
                @SuppressWarnings("unchecked")
                List<String> actions = (List<String>) state.value("actions", java.util.Collections.<String>emptyList());
                @SuppressWarnings("unchecked")
                List<String> snippets = (List<String>) state.value("snippets", java.util.Collections.<String>emptyList());
                var r = summaryAgent.run(invId, query, rootCause, actions, snippets);
                Map<String, Object> delta = new HashMap<>();
                delta.put("summary", r.getSummary());
                return delta;
            }));

            g.addNode(NODE_ESCALATION, AsyncNodeAction.node_async((NodeAction<AgentState>) state -> {
                String invId = state.value("investigationId", "");
                String severity = state.value("severity", "MEDIUM");
                double confidence = Double.parseDouble(String.valueOf(state.value("rootCauseConfidence", 0.7)));
                boolean createTicket = Boolean.parseBoolean(String.valueOf(state.value("createTicketIfNeeded", false)));
                String rootCause = state.value("rootCause", "");
                @SuppressWarnings("unchecked")
                List<String> actions = (List<String>) state.value("actions", java.util.Collections.<String>emptyList());
                String summary = state.value("summary", "");
                var e = escalationAgent.run(invId, severity, confidence, createTicket, rootCause, actions, summary);
                Map<String, Object> delta = new HashMap<>();
                delta.put("ticketCreated", e.ticketCreated());
                delta.put("ticketId", e.ticketId());
                return delta;
            }));

            g.addEdge(StateGraph.START, NODE_SEARCH);
            g.addEdge(NODE_SEARCH, NODE_DETECTION);
            g.addEdge(NODE_SEARCH, NODE_CORRELATION);
            g.addEdge(NODE_DETECTION, NODE_ROOT_CAUSE);
            g.addEdge(NODE_CORRELATION, NODE_ROOT_CAUSE);
            g.addEdge(NODE_ROOT_CAUSE, NODE_REMEDIATION);
            g.addEdge(NODE_REMEDIATION, NODE_SUMMARY);
            g.addEdge(NODE_SUMMARY, NODE_ESCALATION);
            g.addEdge(NODE_ESCALATION, StateGraph.END);

            log.info("[Graph] compile");
            return g.compile();
        } catch (GraphStateException e) {
            throw new IllegalStateException("Failed to build investigation graph", e);
        }
    }
}
