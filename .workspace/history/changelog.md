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
- Span metrics: Tempo metrics-generator -> Prometheus remote write (`traces_spanmetrics_*` RED metrics).
- Alerting: Grafana managed alert rule for payment authorization span errors.
- Logging: Logback structured JSON with `trace_id` / `span_id`; per-request access log filter.

### Infra
- Dockerfile (multi-stage, JRE 21, non-root), docker-compose (app, postgres, otel-collector, tempo, prometheus, grafana).
- Grafana provisioning: Tempo + Prometheus datasources, dashboard with Actuator metrics + Tempo span metrics panels,
  and span metrics alert rule.
- Makefile (`COMPOSE ?=`, `APP_PORT` overridable), scripts/load.sh, README.
- App host port defaults to `10080` to avoid common local `8080` conflicts; container-internal app port remains `8080`.

### Verification (2026-06-29)
- `./gradlew test` — 12 tests PASS.
- docker compose integration: functional + trace (5-layer spans, failure error span) + metrics + JSON logs all PASS.

### ADRs
- 0001 scope, 0002 monolith/Java packages, 0003 otel/tempo + actuator metrics,
  0004 Micrometer observability, 0005 PostgreSQL+JPA, 0006 Java/Spring Boot, 0007 Tempo span metrics.

### Learning material (2026-07-04)
- docs/: architecture(구성도 4종 mermaid + 설정 파일 지도), observability-deep-dive(동작 원리),
  study-guide(체크포인트 9 · 연습문제 6 · FAQ) + Grafana 실캡처 3장(docs/images/).
- README 학습 가이드 섹션 + What to Check에 실제 kebab-case span 이름 반영,
  AGENTS.md Trace Design에 이름 정규화 노트.
- 교육용 코드 주석(영어): 관측성 핵심 지점 6곳 + 테스트 4클래스 헤더.
- LICENSE(MIT), GitHub topics 9종(opentelemetry, spring-boot, micrometer, grafana-tempo 등).
