# AI Incident Investigation (Spring Boot + Spring AI + LangGraph4j)

A production-grade, multi-agent backend that performs incident investigations using:
- Local log ingestion and intelligent chunking at startup
- OpenAI embeddings (text-embedding-3-small)
- MongoDB Atlas Vector Search via Spring AI VectorStore
- Multi-agent LLM reasoning (GPT-4o) with dedicated prompts per agent
- Explicit LangGraph DAG orchestration with parallel branches
- REST APIs for investigations and semantic log search (no UI)

## Tech stack
- Java 21, Spring Boot 4.x
- Spring Web MVC, Spring Data MongoDB
- Spring AI (OpenAI + MongoDB Atlas Vector Store)
- OpenAI GPT-4o (chat) and text-embedding-3-small (embeddings)
- LangGraph4j for multi-agent orchestration (parallel + sequential)

## Environment variables
- `OPENAI_API_KEY` (required)
- `MONGODB_URI` (required) → used by `spring.mongodb.uri` (and mirrored to `spring.data.mongodb.uri`)

Never hardcode secrets. The application reads them from environment variables via `application.properties`.

## Configuration (src/main/resources/application.properties)
Key settings:
- `spring.mongodb.uri=${MONGODB_URI}` (also mirrored to `spring.data.mongodb.uri` for compatibility)
- `spring.ai.openai.chat.model=gpt-4o`
- `spring.ai.openai.embedding.model=text-embedding-3-small`
- `spring.ai.vectorstore.type=mongodb-atlas`
- `spring.ai.vectorstore.mongodb.atlas.collection-name=vector_store`
- `spring.ai.vectorstore.mongodb.atlas.vector-index-name=default`
- `spring.ai.vectorstore.mongodb.atlas.path-name=embedding`
- `app.logs.path=./sample-logs`
- `app.ingestion.enabled=true`

## MongoDB Atlas Vector Search setup
This app stores vectors in the `vector_store` collection. Create an Atlas Vector Search index named `default`:

Example definition (adjust to Atlas UI schema format):
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
- `numDimensions` must match the embedding model dimensions (text-embedding-3-small is 1536 at the time of writing).
- We store raw text in the Document and mirror metadata under `metadata.*`.

## Project structure
- `org.pallapati.aiincidentinvestigation`
  - `config` → OpenAI ChatClient, Mongo auditing, orchestration graph beans
  - `controller` → REST endpoints (investigations, tickets, logs search)
  - `dto` → API DTOs (InvestigationRequest/Response, LogSearch*)
  - `exception` → Global exception handler
  - `model` → MongoDB entities (LogChunk, IncidentInvestigation, AgentFinding, EscalationTicket)
  - `orchestrator` → LangGraph DAG (InvestigationGraphBuilder) + orchestrator service
  - `repository` → Spring Data repositories
  - `service` → Log ingestion, chunking, embeddings, semantic search, startup initializer
  - `service.agent` → Detection, Correlation, RootCause, Remediation, Summary, Escalation agents
  - `util` → helpers (e.g., HashingUtil)

## How it works
Startup ingestion flow:
1. Scan `./sample-logs` recursively for `.log` and `.txt` (preserves timestamps, trace/session IDs).
2. Chunk logs intelligently (groups stack traces and related lines; avoids large in-memory loads).
3. Convert chunks to Spring AI Documents with metadata and call OpenAI embeddings.
4. Store vectors and metadata in MongoDB Atlas Vector Store. Dedup using `contentHash` and `embeddingStatus`.

Investigation flow (runtime, orchestrated by LangGraph DAG):
1. `semanticSearch` → Vector similarity search on logs using natural-language query → produces `snippets[]`.
2. Parallel: `detection` and `correlation` agents use `snippets[]` to derive symptoms/services/severity and cross-service patterns.
3. `rootCause` agent consumes symptoms+patterns+snippets to determine probable root cause + confidence.
4. `remediation` agent proposes actions.
5. `summary` agent generates executive/technical summary.
6. `escalation` agent decides on ticket creation and persists it when needed.
7. Persist `IncidentInvestigation` + stepwise `AgentFinding` records.

## Build and test
- Build + run tests:
```
./mvnw clean test
```
Tests mock VectorStore and avoid real OpenAI/Mongo calls.

- Run app:
```
./mvnw spring-boot:run
```
Ensure `OPENAI_API_KEY` and `MONGODB_URI` are set.

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

## Admin/debug endpoints
- Status (logs found, chunks persisted, vector docs):
```
curl -s http://localhost:8080/api/debug/status | jq
```
- Rescan and ingest any new log files:
```
curl -s -X POST http://localhost:8080/api/debug/rescan | jq
```
- Force re-embed all existing chunks (if index changed):
```
curl -s -X POST http://localhost:8080/api/debug/reembed | jq
```

## Troubleshooting
- Empty search results
  - Ensure the Atlas Search index exists on `vector_store`, name `default`, with path `embedding` and 1536 dims.
  - Verify `/api/debug/status` shows `vectorDocsInAtlas > 0`.
- OpenAI errors
  - Ensure `OPENAI_API_KEY` is valid and has access to `gpt-4o` and `text-embedding-3-small`.
- Invalid log directory
  - Ensure `app.logs.path` exists and contains `.log`/`.txt` files.

## Security
- Never commit keys. All secrets must come from environment variables.

## Notes
- No UI. No mock AI. Real OpenAI calls at runtime.
- Agents have independent prompts, ChatClient calls, and persist their own AgentFindings.
- Orchestration uses an explicit LangGraph DAG with parallel branches and deterministic hand-offs.

## Ticket knowledge base (mock) and suggestions
- On startup, the app loads sample tickets from `src/main/resources/sample-tickets.json`.
- Each ticket contains: title, description, severity, rootCause, previousResolutions[], recommendedActions[].
- Tickets are persisted (Mongo) and also vectorized into the same VectorStore (metadata.type=ticket) to enable retrieval-augmented suggestions during investigations.
- When an investigation runs, semantic search can surface both log chunks and prior tickets, improving remediation suggestions.

## Escalation behavior (LLM-assisted and override)
- `createTicketIfNeeded` in InvestigationRequest is optional (nullable):
  - If omitted (null), the system lets the LLM-driven escalation agent decide based on severity, confidence, and summary.
  - If explicitly `true` or `false`, the user overrides the LLM decision.
- Duplicate protection: if a ticket already exists for the same investigation, the existing ticket is returned instead of creating a new one.

### Examples
- LLM decides escalation (field omitted)
```
curl -s -X POST http://localhost:8080/api/investigations \
  -H 'Content-Type: application/json' \
  -d '{ "query": "authentication token expiration impacting sign-ins" }'
```

- Force create a ticket
```
curl -s -X POST http://localhost:8080/api/investigations \
  -H 'Content-Type: application/json' \
  -d '{ "query": "websocket disconnect storm in us-east-1", "createTicketIfNeeded": true }'
```

- Force do not create a ticket
```
curl -s -X POST http://localhost:8080/api/investigations \
  -H 'Content-Type: application/json' \
  -d '{ "query": "TURN allocation failures causing media connection drops", "createTicketIfNeeded": false }'
```
