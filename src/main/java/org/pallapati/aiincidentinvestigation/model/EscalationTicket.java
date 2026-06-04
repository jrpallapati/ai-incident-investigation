package org.pallapati.aiincidentinvestigation.model;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

/**
 * Persisted ticket representing the decision to escalate based on the investigation.
 */
@Document(collection = "escalation_tickets")
public class EscalationTicket {

    @Id
    private String id;

    private String investigationId;
    private String title;
    private String description;

    private String severity; // LOW, MEDIUM, HIGH, CRITICAL
    private String status; // OPEN, CLOSED

    private String rootCause;
    private List<String> recommendedActions;
    private List<String> evidence; // references to log chunks or findings

    // New: historical resolutions to enable solution recommendation
    private List<String> previousResolutions;

    @CreatedDate
    private Instant createdAt;
    @LastModifiedDate
    private Instant updatedAt;

    // Getters/setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getInvestigationId() { return investigationId; }
    public void setInvestigationId(String investigationId) { this.investigationId = investigationId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRootCause() { return rootCause; }
    public void setRootCause(String rootCause) { this.rootCause = rootCause; }

    public List<String> getRecommendedActions() { return recommendedActions; }
    public void setRecommendedActions(List<String> recommendedActions) { this.recommendedActions = recommendedActions; }

    public List<String> getEvidence() { return evidence; }
    public void setEvidence(List<String> evidence) { this.evidence = evidence; }

    public List<String> getPreviousResolutions() { return previousResolutions; }
    public void setPreviousResolutions(List<String> previousResolutions) { this.previousResolutions = previousResolutions; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
