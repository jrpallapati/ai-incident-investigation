package org.pallapati.aiincidentinvestigation.dto;

/**
 * Request DTO to initiate an incident investigation.
 */
public class InvestigationRequest {
    private String query;
    private boolean createTicketIfNeeded;
    private String severityThreshold; // LOW, MEDIUM, HIGH, CRITICAL

    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }

    public boolean isCreateTicketIfNeeded() { return createTicketIfNeeded; }
    public void setCreateTicketIfNeeded(boolean createTicketIfNeeded) { this.createTicketIfNeeded = createTicketIfNeeded; }

    public String getSeverityThreshold() { return severityThreshold; }
    public void setSeverityThreshold(String severityThreshold) { this.severityThreshold = severityThreshold; }
}

