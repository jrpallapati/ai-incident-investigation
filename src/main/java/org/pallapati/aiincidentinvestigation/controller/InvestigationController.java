package org.pallapati.aiincidentinvestigation.controller;

import org.pallapati.aiincidentinvestigation.dto.InvestigationRequest;
import org.pallapati.aiincidentinvestigation.dto.InvestigationResponse;
import org.pallapati.aiincidentinvestigation.model.AgentFinding;
import org.pallapati.aiincidentinvestigation.model.EscalationTicket;
import org.pallapati.aiincidentinvestigation.model.IncidentInvestigation;
import org.pallapati.aiincidentinvestigation.orchestrator.InvestigationOrchestratorService;
import org.pallapati.aiincidentinvestigation.repository.AgentFindingRepository;
import org.pallapati.aiincidentinvestigation.repository.EscalationTicketRepository;
import org.pallapati.aiincidentinvestigation.repository.IncidentInvestigationRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class InvestigationController {

    private final InvestigationOrchestratorService orchestratorService;
    private final IncidentInvestigationRepository investigationRepository;
    private final AgentFindingRepository findingRepository;
    private final EscalationTicketRepository ticketRepository;

    public InvestigationController(InvestigationOrchestratorService orchestratorService,
                                   IncidentInvestigationRepository investigationRepository,
                                   AgentFindingRepository findingRepository,
                                   EscalationTicketRepository ticketRepository) {
        this.orchestratorService = orchestratorService;
        this.investigationRepository = investigationRepository;
        this.findingRepository = findingRepository;
        this.ticketRepository = ticketRepository;
    }

    @PostMapping("/investigations")
    public ResponseEntity<InvestigationResponse> createInvestigation(@RequestBody InvestigationRequest request) {
        return ResponseEntity.ok(orchestratorService.investigate(request));
    }

    @GetMapping("/investigations/{id}")
    public ResponseEntity<IncidentInvestigation> getInvestigation(@PathVariable String id) {
        return investigationRepository.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/investigations/{id}/findings")
    public ResponseEntity<List<AgentFinding>> getFindings(@PathVariable String id) {
        return ResponseEntity.ok(findingRepository.findByInvestigationId(id));
    }

    @GetMapping("/tickets")
    public ResponseEntity<List<EscalationTicket>> getTickets() {
        return ResponseEntity.ok(ticketRepository.findAll());
    }

    @GetMapping("/tickets/{id}")
    public ResponseEntity<EscalationTicket> getTicket(@PathVariable String id) {
        return ticketRepository.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
}

