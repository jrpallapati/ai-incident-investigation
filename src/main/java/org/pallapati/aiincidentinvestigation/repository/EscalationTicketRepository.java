package org.pallapati.aiincidentinvestigation.repository;

import org.pallapati.aiincidentinvestigation.model.EscalationTicket;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface EscalationTicketRepository extends MongoRepository<EscalationTicket, String> {
}

