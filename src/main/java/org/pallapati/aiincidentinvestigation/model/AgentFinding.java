package org.pallapati.aiincidentinvestigation.model;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

/**
 * Captures a single agent's output including evidence and confidence.
 * This is persisted independently for auditability and explainability.
 */
@Document(collection = "agent_findings")
public class AgentFinding {

    @Id
    private String id;

    private String investigationId;
    private String agentName; // e.g., DetectionAgent
    private String findingType; // SYMPTOMS, PATTERNS, ROOT_CAUSE, REMEDIATION, SUMMARY, ESCALATION_DECISION

    private String findingText; // human-readable text produced by the agent

    private List<String> evidence; // log snippets, document IDs, etc.

    private Double confidenceScore; // 0-1 scale

    @CreatedDate
    private Instant createdAt;

    // getters/setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getInvestigationId() { return investigationId; }
    public void setInvestigationId(String investigationId) { this.investigationId = investigationId; }

    public String getAgentName() { return agentName; }
    public void setAgentName(String agentName) { this.agentName = agentName; }

    public String getFindingType() { return findingType; }
    public void setFindingType(String findingType) { this.findingType = findingType; }

    public String getFindingText() { return findingText; }
    public void setFindingText(String findingText) { this.findingText = findingText; }

    public List<String> getEvidence() { return evidence; }
    public void setEvidence(List<String> evidence) { this.evidence = evidence; }

    public Double getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(Double confidenceScore) { this.confidenceScore = confidenceScore; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}

