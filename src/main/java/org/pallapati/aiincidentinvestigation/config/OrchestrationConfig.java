package org.pallapati.aiincidentinvestigation.config;

import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.state.AgentState;
import org.pallapati.aiincidentinvestigation.orchestrator.InvestigationGraphBuilder;
import org.pallapati.aiincidentinvestigation.service.LogSearchService;
import org.pallapati.aiincidentinvestigation.service.agent.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the LangGraph DAG and exposes it as a Spring bean so controllers/services can inject it.
 */
@Configuration
public class OrchestrationConfig {

    @Bean
    public InvestigationGraphBuilder investigationGraphBuilder(LogSearchService logSearchService,
                                                               DetectionAgent detectionAgent,
                                                               CorrelationAgent correlationAgent,
                                                               RootCauseAgent rootCauseAgent,
                                                               RemediationAgent remediationAgent,
                                                               SummaryAgent summaryAgent,
                                                               EscalationAgent escalationAgent) {
        return new InvestigationGraphBuilder(logSearchService, detectionAgent, correlationAgent,
                rootCauseAgent, remediationAgent, summaryAgent, escalationAgent);
    }

    @Bean
    public CompiledGraph<AgentState> investigationGraph(InvestigationGraphBuilder builder) {
        return builder.build();
    }
}

