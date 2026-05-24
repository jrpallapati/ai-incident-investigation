package org.pallapati.aiincidentinvestigation.model;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

/**
 * High-level investigation aggregate persisted for each user query.
 *
 * - Holds final summary fields produced through multi-agent reasoning.
 * - Individual AgentFinding records capture step-by-step evidence.
 */
@Document(collection = "incident_investigations")
public class IncidentInvestigation {

    @Id
    private String id;

    private String query;
    private String status; // CREATED, RUNNING, COMPLETED, FAILED

    private List<String> detectedSymptoms;
    private List<String> affectedServices;

    private String rootCause;
    private Double confidenceScore; // confidence in root cause

    private List<String> recommendedActions;
    private String summary;

    @CreatedDate
    private Instant createdAt;
    @LastModifiedDate
    private Instant updatedAt;

    // Getters/setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public List<String> getDetectedSymptoms() { return detectedSymptoms; }
    public void setDetectedSymptoms(List<String> detectedSymptoms) { this.detectedSymptoms = detectedSymptoms; }

    public List<String> getAffectedServices() { return affectedServices; }
    public void setAffectedServices(List<String> affectedServices) { this.affectedServices = affectedServices; }

    public String getRootCause() { return rootCause; }
    public void setRootCause(String rootCause) { this.rootCause = rootCause; }

    public Double getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(Double confidenceScore) { this.confidenceScore = confidenceScore; }

    public List<String> getRecommendedActions() { return recommendedActions; }
    public void setRecommendedActions(List<String> recommendedActions) { this.recommendedActions = recommendedActions; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}

