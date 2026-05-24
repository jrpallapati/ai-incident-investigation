package org.pallapati.aiincidentinvestigation.repository;

import org.pallapati.aiincidentinvestigation.model.AgentFinding;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface AgentFindingRepository extends MongoRepository<AgentFinding, String> {
    List<AgentFinding> findByInvestigationId(String investigationId);
}

