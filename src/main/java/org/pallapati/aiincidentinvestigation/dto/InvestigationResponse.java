package org.pallapati.aiincidentinvestigation.dto;

import java.util.List;

/**
 * REST response representing the consolidated investigation outcome.
 */
public class InvestigationResponse {
    private String investigationId;
    private String query;
    private String status;
    private List<String> symptoms;
    private List<String> affectedServices;
    private String rootCause;
    private Double confidenceScore;
    private List<String> recommendedActions;
    private String summary;
    private boolean ticketCreated;
    private String ticketId;

    public String getInvestigationId() { return investigationId; }
    public void setInvestigationId(String investigationId) { this.investigationId = investigationId; }

    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public List<String> getSymptoms() { return symptoms; }
    public void setSymptoms(List<String> symptoms) { this.symptoms = symptoms; }

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

    public boolean isTicketCreated() { return ticketCreated; }
    public void setTicketCreated(boolean ticketCreated) { this.ticketCreated = ticketCreated; }

    public String getTicketId() { return ticketId; }
    public void setTicketId(String ticketId) { this.ticketId = ticketId; }
}

