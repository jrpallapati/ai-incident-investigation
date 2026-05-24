# AI Incident Investigation (Spring Boot + Spring AI)

A production-grade, multi-agent backend that performs incident investigations using:
- Local log ingestion and intelligent chunking
- OpenAI embeddings (text-embedding-3-small)
- MongoDB Atlas Vector Search via Spring AI VectorStore
- Multi-agent LLM reasoning (GPT-4o) with dedicated prompts per agent
- REST APIs for investigations and semantic log search

No UI is included.

## Tech stack
- Java 21
- Spring Boot 4.x
- Spring Web MVC
- Spring Data MongoDB
- Spring AI (OpenAI + MongoDB Atlas Vector Store)
- OpenAI GPT-4o (chat) and text-embedding-3-small (embeddings)

## Environment variables
- `OPENAI_API_KEY` (required)
- `MONGODB_URI` (required) → used by `spring.data.mongodb.uri`

Never hardcode secrets. The application reads them from environment variables via `application.properties`.

## Configuration (src/main/resources/application.properties)
Key settings:
- `spring.data.mongodb.uri=${MONGODB_URI}`
- `spring.ai.openai.chat.model=gpt-4o`
- `spring.ai.openai.embedding.model=text-embedding-3-small`
- `spring.ai.vectorstore.type=mongodb-atlas`
- `spring.ai.vectorstore.mongodb.atlas.collection-name=app_log_chunks`
- `spring.ai.vectorstore.mongodb.atlas.vector-index-name=app_log_vector_index`
- `spring.ai.vectorstore.mongodb.atlas.path-name=embedding`
- `app.logs.path=./sample-logs`
- `app.ingestion.enabled=true`

## MongoDB Atlas Vector Search setup
This app expects a collection for vector storage and an Atlas Vector Search index.

1) Create a database (e.g., `incident-investigation`) and a collection named `app_log_chunks`.
2) Create a Vector Search index for that collection (name: `app_log_vector_index`). Example index definition:

```
{
  "fields": [
    { "type": "vector", "path": "embedding", "numDimensions": 1536, "similarity": "cosine" },
    { "type": "filter", "path": "metadata.serviceName" },
    { "type": "filter", "path": "metadata.severity" },
    { "type": "filter", "path": "metadata.timestamp" },
    { "type": "filter", "path": "metadata.traceId" },
    { "type": "filter", "path": "metadata.sessionId" }
  ]
}
```

Notes:
- `numDimensions` must match the OpenAI embedding model dimensions (text-embedding-3-small currently 1536).
- The app stores embeddings under the `embedding` path and metadata under `metadata.*`.

## Project structure
- `org.pallapati.aiincidentinvestigation`
  - `config` → OpenAI ChatClient + Mongo auditing config
  - `controller` → REST endpoints
  - `dto` → API DTOs (InvestigationRequest/Response, LogSearch*)
  - `exception` → Global exception handler
  - `model` → MongoDB entities (LogChunk, IncidentInvestigation, AgentFinding, EscalationTicket)
  - `orchestrator` → InvestigationOrchestratorService (multi-agent flow)
  - `repository` → Spring Data repositories
  - `service` → Log ingestion, chunking, embeddings, semantic search
  - `service.agent` → Detection, Correlation, RootCause, Remediation, Summary, Escalation agents
  - `util` → helpers (e.g., HashingUtil)
  - `vector` → VectorStore configuration (relies on Spring AI autoconfig)

## How it works
Startup ingestion flow:
1. Scan `./sample-logs` recursively for `.log` and `.txt`.
2. Chunk logs intelligently (preserves stack traces, timestamps, trace/session IDs).
3. Convert chunks to Documents with metadata and call OpenAI embeddings via Spring AI.
4. Store vectors and metadata in MongoDB Atlas Vector Store. Dedup using `contentHash` and `embeddingStatus`.

Investigation flow (runtime):
1. Vector similarity search on logs using natural-language query.
2. DetectionAgent → symptoms, impacted services, severity.
3. CorrelationAgent → cross-service patterns.
4. RootCauseAgent → probable root cause + confidence.
5. RemediationAgent → recommended actions.
6. SummaryAgent → executive/technical summary.
7. EscalationAgent → optionally creates EscalationTicket based on severity threshold.
8. Persist IncidentInvestigation + AgentFinding records.

## Build and test
- Build + run tests:
```
./mvnw clean test
```
Tests mock the VectorStore so no real Mongo/embedding calls are made.

- Run app:
```
./mvnw spring-boot:run
```
Ensure `OPENAI_API_KEY` and `MONGODB_URI` are set in your shell.

## REST API
Base path: `/api`

- Create investigation
```
curl -s -X POST http://localhost:8080/api/investigations \
  -H 'Content-Type: application/json' \
  -d '{
        "query": "Why are users unable to join meetings?",
        "createTicketIfNeeded": true,
        "severityThreshold": "MEDIUM"
      }'
```

- Semantic log search
```
curl -s -X POST http://localhost:8080/api/logs/search \
  -H 'Content-Type: application/json' \
  -d '{ "query": "websocket disconnect storm", "topK": 8 }'
```

- Get investigation
```
curl -s http://localhost:8080/api/investigations/{id}
```

- Get findings
```
curl -s http://localhost:8080/api/investigations/{id}/findings
```

- List tickets
```
curl -s http://localhost:8080/api/tickets
```

- Get ticket
```
curl -s http://localhost:8080/api/tickets/{id}
```

## Operational notes
- Chunking is heuristic and aims to keep related lines together, including stack traces.
- Embeddings are generated with OpenAI (real calls) at runtime; do not run tests with real keys.
- The system stores raw chunk text and metadata; vector search returns semantically similar chunks.
- Each agent uses an independent system prompt and persists its own `AgentFinding` for auditability.

## Troubleshooting
- "Invalid log directory" → ensure `app.logs.path` exists and contains `.log`/`.txt` files.
- MongoDB errors → verify `MONGODB_URI` connectivity and the Vector Search index exists.
- OpenAI errors → ensure `OPENAI_API_KEY` is exported and has access to the specified models.

## Security
- Never commit keys. All secrets must come from environment variables.

## License
This project uses dependencies with their respective licenses. Refer to each dependency’s license file.

