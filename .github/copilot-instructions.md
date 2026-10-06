# Copilot Instructions

## Build, test, and analysis

- Use JDK 25. This repository has no Maven wrapper; use the locally installed `mvn`.
- Compile, package, and run the test suite: `mvn clean verify`
- Run all tests: `mvn test`
- Run one test class: `mvn test -Dtest=RankerServiceTest`
- Run one test method: `mvn test -Dtest=RankerServiceTest#testExecute_whenNewPreallocation_shouldAddAndSend`
- There is no standalone formatter or linter configured. CI runs SonarCloud with JaCoCo; reproduce its coverage preparation when needed with:
  `mvn clean org.jacoco:jacoco-maven-plugin:0.8.14:prepare-agent verify org.jacoco:jacoco-maven-plugin:0.8.14:report -B`

Tests use JUnit Jupiter and Mockito. Mongo repository integration tests use the shared embedded-Mongo test support under `src/test/java/it/gov/pagopa/common/mongo/`; use that support rather than introducing a live MongoDB dependency.

## Architecture

This is a Spring Boot 4 service that manages initiative budget reservations for IDPay onboarding, using MongoDB/Cosmos DB as the source of counters and preallocations.

- `RankerConsumerController` periodically asks `InitiativeCountersService` for configured initiatives with available budget. `CycleOrchestrator` schedules no more than `app.ranker.processor.max-parallel-sessions` workers.
- Each `SessionWorker` locks the matching Azure Service Bus session, processes one onboarding message at a time, completes successful messages, abandons failures, and stops when its initiative budget is exhausted or its configured idle timeout is reached.
- `RankerService` deserializes the Service Bus message, attaches Service Bus sequence/enqueue metadata, makes the reservation, and publishes the onboarding DTO through the `rankerProducer-out-0` Spring Cloud Stream binding to the admissibility Service Bus queue. Its HTTP controller exposes preallocation and recovery operations over the same service.
- `InitiativeCountersService` first atomically increments the initiative's onboarded/reserved counters and decrements residual budget, then inserts the preallocation. A failed conditional Mongo update surfaces as `BudgetExhaustedException`, which stops the session worker.
- `trxProcessor` is the Kafka functional consumer. `TransactionInProgressService` validates each event, then delegates to the strategy selected by `TransactionInProgressProcessorStrategyFactory` for its `SyncTrxStatus`. Strategies update preallocation and counter state; processing errors go to the `errors` output binding.

`application.yml` is the contract for binding names, queue/topic destinations, environment-variable overrides, Mongo connection settings, and scheduling/concurrency limits. Keep its Spring Cloud Stream function definition and binding names aligned with producer/consumer bean names.

## Repository conventions

- Persist preallocation and onboarding IDs as `userId + "_" + initiativeId`; `Onboarding.buildId` and `InitiativeCountersService.ID_SEPARATOR` establish the required order. Do not reverse or independently reimplement this format.
- Amounts are `long` values in cents. Budget-changing code must retain the atomic `MongoTemplate.findAndModify` conditional update pattern in `InitiativeCountersAtomicRepositoryImpl`; a read-then-write flow can over-reserve budget under concurrent workers.
- Treat `DuplicateKeyException` from the reservation path as budget exhaustion, as this is the Cosmos DB signal for an unmet residual-budget predicate in this application.
- Add transaction event behavior by implementing `TransactionInProgressProcessorStrategy` for exactly one status; it is registered automatically through the factory's injected strategy list.
- Use constructor injection and pair service/repository interfaces with their `*Impl` implementations. Configuration values use `@ConfigurationProperties` for grouped settings and `@Value` for individual settings.
- Use `tools.jackson.*` imports, not `com.fasterxml.jackson.*`; Spring Boot 4 provides Jackson 3.
- Log externally supplied identifiers or payload fields only after applying `CommonUtils.sanitizeString` or `RankerProducer.sanitizeField`. Keep message acknowledgement behavior explicit: complete only after successful processing and abandon retryable failures.
