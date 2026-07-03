# Open Questions

## Resolved

- 스택: Go(net/http) -> **Spring Boot 3 + Java 21 + Gradle(Groovy)** (사용자 지시, ADR-0006).
- DB: SQLite -> **PostgreSQL + Spring Data JPA** (ADR-0005).
- 관측성: OTel Go SDK -> **Micrometer Observation/Tracing(OTel bridge)** (ADR-0004).
- 메트릭: **Micrometer + Actuator `/actuator/prometheus`** 직접 scrape.
- Trace export: OTLP HTTP(4318) -> Collector -> Tempo.
- 호출 순서: Reserve -> Authorize -> Insert (결제 실패 시 미저장).
- Grafana: datasource + 최소 대시보드 provisioning.
- 컨테이너 런타임: docker compose (이 환경은 `docker-compose` 바이너리; Makefile `COMPOSE` 변수로 override).
- 통합 검증(2026-06-29): functional/trace/metric/log 전부 PASS (validation/test-results.md).

## Open / Follow-up

- **Span 이름 형식**: Micrometer Observation이 `contextualName`을 kebab-case로 정규화하여
  `OrderController.createOrder` -> `order-controller.create-order`로 보인다. 계층 식별에는 충분.
  정확히 CamelCase로 표기하려면 커스텀 `ObservationConvention`(또는 ObservationFilter)로 span name을
  지정해야 한다. (실험 목적에는 현재로 충분 — 후속 개선 후보)
- **App host port**: 로컬 8080이 ssh 터널/다른 프로젝트 컨테이너로 점유될 수 있어 docker-compose에서
  `${APP_PORT:-8080}`로 파라미터화했다. 기본은 8080, 충돌 시 `make up APP_PORT=18080`.
- **Testcontainers on colima**: docker 전략 자동 탐지가 실패하여 JPA 슬라이스 테스트는 H2로 수행 중.
  colima에서 Testcontainers를 쓰려면 DOCKER_HOST / TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE 등 추가 설정 필요.
- Loki / Pyroscope / Jaeger 비교 / Spring Modulith: Could Have(미착수).

## AGENTS.md Update

- 사용자가 직접 지시하여 AGENTS.md를 v2(Spring Boot + JPA + PostgreSQL + Micrometer)로 전면 개정 완료.
