package org.pallapati.aiincidentinvestigation.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.pallapati.aiincidentinvestigation.model.EscalationTicket;
import org.pallapati.aiincidentinvestigation.repository.EscalationTicketRepository;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads mock tickets (with previous resolutions) and vectorizes them into the same VectorStore.
 * This enhances retrieval-augmented suggestions during investigation.
 */
@Component
public class TicketBootstrapService {

    private final EscalationTicketRepository ticketRepository;
    private final VectorStore vectorStore;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TicketBootstrapService(EscalationTicketRepository ticketRepository, VectorStore vectorStore) {
        this.ticketRepository = ticketRepository;
        this.vectorStore = vectorStore;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void loadTickets() {
        try {
            var resource = new ClassPathResource("sample-tickets.json");
            if (!resource.exists()) return;
            List<Map<String, Object>> entries = objectMapper.readValue(resource.getInputStream(), new TypeReference<>() {});
            List<Document> docs = new ArrayList<>();
            List<EscalationTicket> toPersist = new ArrayList<>();
            for (Map<String, Object> e : entries) {
                EscalationTicket t = new EscalationTicket();
                t.setTitle((String) e.get("title"));
                t.setDescription((String) e.get("description"));
                t.setSeverity((String) e.get("severity"));
                t.setRootCause((String) e.get("rootCause"));
                @SuppressWarnings("unchecked")
                List<String> pr = (List<String>) e.getOrDefault("previousResolutions", List.of());
                @SuppressWarnings("unchecked")
                List<String> ra = (List<String>) e.getOrDefault("recommendedActions", List.of());
                t.setPreviousResolutions(pr);
                t.setRecommendedActions(ra);
                t.setStatus("CLOSED");
                toPersist.add(t);

                String content = t.getTitle() + "\n" + t.getDescription() + "\nRoot cause: " + t.getRootCause() +
                        "\nPrev resolutions: " + String.join("; ", pr) +
                        "\nRecommended: " + String.join("; ", ra);
                Map<String,Object> md = new HashMap<>();
                md.put("type", "ticket");
                md.put("severity", t.getSeverity());
                md.put("rootCause", t.getRootCause());
                md.put("title", t.getTitle());
                docs.add(new Document(content, md));
            }
            if (!toPersist.isEmpty()) ticketRepository.saveAll(toPersist);
            if (!docs.isEmpty()) vectorStore.add(docs);
        } catch (IOException ex) {
            // ignore; mock tickets are optional
        }
    }
}

