# please note..

A version-controlled, cloud-synced notes platform inspired by Notability and GitHub.

## Overview
please note.. treats each note like a repository:
- Every edit creates a new revision
- History is preserved and inspectable
- Sync conflicts are explicit, not silent

## Tech Stack
- **Backend:** Java 17, Spring Boot 3, Hibernate, PostgreSQL
- **Infrastructure:** Docker & Docker Compose
- **Planned:** Kafka, Next.js, GitHub Actions

## Architecture
- `backend/`: Spring Boot application
- `infra/`: Docker Compose configs
- `.github/`: CI/CD workflows (planned)

## Running Locally
1. **Start Postgres:**
   `docker compose -f infra/compose.yml up -d`
2. **Run Backend:**
   `cd backend && ./mvnw spring-boot:run`
3. **Health Check:**
   `curl http://localhost:8080/actuator/health`

## Roadmap
- **Phase 1:** Backend MVP & PostgreSQL persistence (Current)
- **Phase 2:** Optimistic concurrency & Search indexing
- **Phase 3:** Kafka integration & Event-driven architecture
