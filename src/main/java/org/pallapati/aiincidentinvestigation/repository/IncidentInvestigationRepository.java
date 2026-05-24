package org.pallapati.aiincidentinvestigation.repository;

import org.pallapati.aiincidentinvestigation.model.IncidentInvestigation;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface IncidentInvestigationRepository extends MongoRepository<IncidentInvestigation, String> {
}

