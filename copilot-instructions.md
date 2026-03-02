# Copilot Instructions — IdPay Ranker Service

Purpose
-------
This repository contains the IdPay Ranker Spring Boot service. The main entry point is `IdPayRankerApplication` (see `src/main/java/it/gov/pagopa/ranker/IdPayRankerApplication.java`). The service processes ranking logic, integrates with Kafka / Azure Service Bus, and persists data in MongoDB.

Quick Start
-----------
Build the project (from repository root):

```bash
mvn clean package
```

Run locally with Maven:

```bash
mvn spring-boot:run
```

Run the packaged jar (artifact under `target/`):

```bash
java -jar target/*.jar
```

Run tests:

```bash
mvn test
```

Docker
------
Build Docker image (Dockerfile uses `mvn clean package -DskipTests` during build):

```bash
docker build -t idpay-ranker .
```

Run container (set required env variables, see Environment section):

```bash
docker run -e MONGODB_URI="mongodb://host:27017" -p 8080:8080 idpay-ranker
```

Configuration & Environment
---------------------------
Primary YAML: `src/main/resources/application.yml`.

Essential environment variables (minimal set to run locally) — verify with the team which of these are mandatory for dev:

- `MONGODB_URI` — MongoDB connection string (e.g. `mongodb://localhost:27017/idpay`)
- `MONGODB_DBNAME` — database name (if not in URI)
- `KAFKA_TRANSACTIONS_BROKER` / `KAFKA_ERRORS_BROKER` — Kafka bootstrap servers (or use a local test broker)
- `SERVICEBUS_ONBOARDING_CONNECTION_STRING` — Service Bus connection string (if Service Bus binders are used)
- `RANKER_PORT` — HTTP port override (default 8080)
- `LOG_LEVEL_ROOT` — logging level (optional)
- `APPLICATION_INSIGHTS_KEY` or agent configuration — if using App Insights via Dockerfile

Refer to `application.yml` for full configuration and additional Kafka/Mongo/Service Bus properties. The Dockerfile includes an Application Insights Java agent `applicationinsights-agent.jar`; ensure the appropriate key/config is provided for production runs.

Code Map (key packages and files)
--------------------------------
- Main application: `src/main/java/it/gov/pagopa/ranker/IdPayRankerApplication.java`
- Domain & modules: `src/main/java/it/gov/pagopa/ranker` (connector, controller, domain, repository, service, strategy, stream)
- Shared/common utilities and config: `src/main/java/it/gov/pagopa/common`
- Resources: `src/main/resources/application.yml`
- Tests: `src/test/java`
- Build descriptor: `pom.xml`
- Dockerfile: `Dockerfile`

Patterns & Libraries
--------------------
- Java 21 (project configured for Java 21)
- Spring Boot 3.x
- Spring Cloud Stream with Kafka and Azure Service Bus binders
- Spring Data MongoDB (with retry/limit handling patterns)
- Lombok, Jackson
- Actuator and health indicators
- Embedded Mongo (Flapdoodle) used in tests

Developer Tasks & Examples
--------------------------
- Build and run unit/integration tests:

```bash
mvn test
```

- Build and run locally:

```bash
mvn clean package
java -jar target/*.jar
```

- Build and run in Docker (skip tests in image build):

```bash
docker build -t idpay-ranker .
docker run -e MONGODB_URI="mongodb://host:27017" -p 8080:8080 idpay-ranker
```

Contribution Guidance (short)
-----------------------------
- Run tests locally before opening a PR.
- Follow existing package structure and naming conventions under `it.gov.pagopa.*`.
- PR checklist: run `mvn test`, update `application.yml` docs if config changes, add unit/integration tests for new behavior, include a short description of env vars required to run the change locally.

Copilot / Assistant Usage Tips
-----------------------------
When asking Copilot (or an LLM) for code changes, include:

- Short goal statement (e.g., "Add retry for Mongo write on 429")
- Relevant files opened as context (preferably `IdPayRankerApplication.java`, target service class, and `application.yml`).
- Example prompt template:

"I need to implement <feature>. The main service is `IdPayRankerApplication` and the target package is `it.gov.pagopa.ranker.<module>`. Update `<file>` to do <behavior>, keep existing logging and exception patterns, and add tests under `src/test/java/<package>`." 

Verification Checklist
----------------------
- `mvn clean package` completes successfully and produces `target/*.jar`.
- `mvn test` passes (or at least unit tests pass locally).
- Application starts and health endpoint returns `UP` on configured port (`RANKER_PORT` or 8080).

Open Questions / Action Items
----------------------------
- Confirm minimal required env vars for development (which ones can be stubbed or mocked).
- Provide a recommended local/dev configuration example (`application-local.yml` or `.env.example`) for Mongo/Kafka/Service Bus.
- Confirm whether Application Insights agent is required in CI/builds and provide instrumentation key if needed.
- Provide CI pipeline steps if different Maven goals are required in CI.

Next steps I can take for you
----------------------------
- Create a `.env.example` with the minimal env vars.
- Add a short `CONTRIBUTING.md` with commit/PR conventions.
- Update `README.md` to reference this `copilot-instructions.md`.

If you want me to proceed with any of these next steps, tell me which one to do first.
