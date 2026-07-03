# Changelog

## Unreleased

### Stack
- (v1) Started with Go (net/http) + SQLite skeleton.
- (v2) Switched to **Spring Boot 3 + Java 21 + Gradle** (per user instruction); removed Go artifacts.
  AGENTS.md fully revised to the Spring Boot + JPA + PostgreSQL + Micrometer Observation spec.

### Application
- REST API: `GET /healthz`, `POST /orders`, `GET /orders/{id}`, `POST /orders?fail_payment=true`.
- Domain flow: InventoryService.reserve -> PaymentClient.authorize -> OrderRepository.insert.
- Persistence: PostgreSQL + Spring Data JPA (Order / OrderItem).

### Observability
- Tracing: Micrometer Observation (`@Observed`) -> Micrometer Tracing (OTel bridge) -> OTLP -> Collector -> Tempo.
- Metrics: Micrometer + Actuator Prometheus (`order.created.count`, `order.failed.count`, `http.server.requests`).
- Logging: Logback structured JSON with `trace_id` / `span_id`; per-request access log filter.

### Infra
- Dockerfile (multi-stage, JRE 21, non-root), docker-compose (app, postgres, otel-collector, tempo, prometheus, grafana).
- Grafana provisioning: Tempo + Prometheus datasources, 4-panel dashboard.
- Makefile (`COMPOSE ?=`, `APP_PORT` overridable), scripts/load.sh, README.

### Verification (2026-06-29)
- `./gradlew test` — 12 tests PASS.
- docker compose integration: functional + trace (5-layer spans, failure error span) + metrics + JSON logs all PASS.

### ADRs
- 0001 scope, 0002 monolith/Java packages, 0003 otel/tempo + actuator metrics,
  0004 Micrometer observability, 0005 PostgreSQL+JPA, 0006 Java/Spring Boot.
