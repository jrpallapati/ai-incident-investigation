package org.pallapati.aiincidentinvestigation.dto;

/**
 * Query DTO for semantic log search.
 */
public class LogSearchRequest {
    private String query;
    private Integer topK = 8;

    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }

    public Integer getTopK() { return topK; }
    public void setTopK(Integer topK) { this.topK = topK; }
}

