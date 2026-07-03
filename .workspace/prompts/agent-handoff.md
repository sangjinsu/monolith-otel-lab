# Agent Handoff

## Current State — 2026-06-29 (구현 + 검증 완료)

Spring Boot 모놀리식 + 관측성 스택이 구현되고 통합 검증까지 통과했다.
모든 acceptance criteria 충족 (validation/acceptance-checklist.md, test-results.md).

검증 시점에 docker compose 스택이 기동 중일 수 있다. 정리하려면 `make down` (또는
`docker-compose down -v`). 로컬 8080 충돌로 app은 `APP_PORT=18080`으로 띄웠다.

## What Was Built

- Java 21 / Spring Boot 3 / Gradle. base package `com.sangjinsu.monolithotellab`.
- web(OrderController, HealthController, GlobalExceptionHandler, RequestLoggingFilter),
  order(JPA entity/repository/service/dto), inventory, payment(fake), platform/observability.
- PostgreSQL + Spring Data JPA. 흐름 Reserve -> Authorize -> Insert.
- Micrometer Observation(@Observed) -> Micrometer Tracing(OTel bridge) -> OTLP -> Collector -> Tempo.
- Metrics: Micrometer + Actuator /actuator/prometheus (Prometheus scrape). order.created/failed counters.
- Logback structured JSON with trace_id/span_id.
- docker-compose: app, postgres, otel-collector(contrib), tempo, prometheus, grafana(+datasources+dashboard).
- Makefile(up/down/logs/test/load), scripts/load.sh, README.

## Verified

- functional: healthz/create/get/fail(402)/404
- trace: 정상 trace 5계층 span, 실패 trace payment error span
- metric: order_created/failed, http_server_requests, Prometheus scrape
- log: JSON trace_id/span_id
- ./gradlew test: 12 tests PASS

## Follow-up (optional)

- Span 이름을 CamelCase로 정확히 표기하려면 커스텀 ObservationConvention (현재 kebab-case).
- colima에서 Testcontainers 활성화(현재 JPA 테스트는 H2).
- Optional Extensions: Loki, Pyroscope, Jaeger 비교, Spring Modulith.

## Caution

- `.workspace`에 비밀값/숨겨진 chain-of-thought 전문을 저장하지 않는다.
- AGENTS.md가 최상위 스펙. 주요 스택/구조 변경은 사용자 확인 후 반영.
