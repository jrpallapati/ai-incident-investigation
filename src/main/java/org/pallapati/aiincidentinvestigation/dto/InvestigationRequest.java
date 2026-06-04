package org.pallapati.aiincidentinvestigation.dto;

/**
 * Request DTO to initiate an incident investigation.
 */
public class InvestigationRequest {
    private String query;
    // Nullable: if null, LLM will decide whether to escalate
    private Boolean createTicketIfNeeded;
    private String severityThreshold; // LOW, MEDIUM, HIGH, CRITICAL

    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }

    public Boolean getCreateTicketIfNeeded() { return createTicketIfNeeded; }
    public void setCreateTicketIfNeeded(Boolean createTicketIfNeeded) { this.createTicketIfNeeded = createTicketIfNeeded; }

    public String getSeverityThreshold() { return severityThreshold; }
    public void setSeverityThreshold(String severityThreshold) { this.severityThreshold = severityThreshold; }
}
