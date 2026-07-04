# Agent Handoff

## Current State — 2026-07-04 (Phase 4: Tempo span metrics 완료)

구현·검증·GitHub 공개(Phase 1)에 이어, 저장소를 **학습 교재**로 쓸 수 있는 문서 계층을 추가했다.
Phase 3에서는 로컬 8080 충돌 회피를 위해 기본 app host port를 10080으로 변경했다.
Phase 4에서는 Tempo metrics-generator 기반 span metrics를 추가했고 Prometheus/Grafana에서 검증했다.

## What Exists

- **앱**: Java 21 / Spring Boot 3.4.5 / Gradle. 패키지 web·order·inventory·payment·platform.observability.
  흐름 Reserve→Authorize→Insert(실패 시 미저장). PostgreSQL + Spring Data JPA.
- **관측성**: @Observed(Micrometer Observation) → OTel bridge → OTLP HTTP → Collector → Tempo.
  메트릭은 Actuator /actuator/prometheus(Prometheus pull)와 Tempo span metrics(Prometheus remote write)를 함께 사용.
  로그 JSON + MDC trace_id/span_id.
- **스택**: docker-compose 6서비스. `make up/down/logs/test/load`. 기본 app host port는 10080이며 APP_PORT로 override.
- **학습 문서**: docs/{architecture, observability-deep-dive, study-guide}.md(한국어) +
  docs/images/ 실캡처 3장. README에 학습 진입점. 코드에 교육 주석(영어).
- **설계 기록**: .workspace (ADR 0001~0007, spec/architecture/observability-design/glossary,
  validation/test-results, experiments 2종).

## Key Facts (다음 세션이 알아야 할 것)

- **span 이름은 kebab-case로 표시됨** (`order-service.create-order`) — Micrometer
  SpanNameUtil.toLowerHyphen 정규화. 문서 전반이 실제 이름 기준. 코드 수정으로 되돌리지 말 것
  (의도된 결정, deep-dive §3).
- Tempo `block_retention: 1h` — 오래된 trace는 사라짐. 데모/스크린샷 전 `make load` 필수.
- 로컬 8080은 종종 다른 프로세스가 점유 → 기본 `make up`은 host 10080을 사용. 필요 시 `APP_PORT`와 `BASE_URL` env로 override.
- Tempo 원시 JSON의 `org.springframework.boot`는 span이 아니라 InstrumentationScope.
- 테스트는 H2(4계층 슬라이스); 실제 PG는 compose 통합으로 검증.
- span metrics 기본 metric 이름은 `traces_spanmetrics_calls_total`, `traces_spanmetrics_latency_*`,
  `traces_spanmetrics_size_total`. 기본 label은 `service`, `span_name`, `span_kind`, `status_code`.

## Verified (2026-07-04)

- ./gradlew test 12 PASS · docs/README 상대링크 전수 OK
- 기본 host port 10080 정책 적용: `docker compose config`, `curl /healthz`, `make load`로 검증
- span metrics runtime 검증 완료:
  `traces_spanmetrics_calls_total`, `traces_spanmetrics_latency_*`, `traces_spanmetrics_size_total`
  확인. `payment-client.authorize` error span count=1, span p95 latency query 8 series 반환.
- Grafana dashboard에는 span metrics panel 3개가 provisioning됨:
  Span request rate by span, Span p95 latency by span, Span error rate by span.
- 라이브: PromQL 3종 반환, 성공 trace 6 spans(payment가 지연 지배), 실패 trace 5 spans
  (payment error + insert 부재), 로그 trace_id ↔ Tempo trace 상관 실측
  (56d2c61f... = deep-dive §8 예시 = trace-success.png 동일 trace)

## Next Recommended Step (선택)

Backlog: Loki / Pyroscope / Jaeger 교체 실험 / Spring Modulith 비교 (AGENTS.md Optional Extensions).

## Caution

- `.workspace`에 비밀값/숨은 chain-of-thought 전문 금지. 판단은 요약(ADR)으로.
- AGENTS.md가 최상위 스펙. 스택/구조 변경은 사용자 확인 후.
- 스크린샷 재캡처 시: 헤드리스 브라우저는 bash 호출 간 상태가 유실될 수 있으니
  로그인(API 쿠키 주입)~캡처를 한 호출에 몰아서 실행할 것.
