# Agent Handoff

## Current State — 2026-07-04 (Phase 2: 학습자료 업그레이드 완료)

구현·검증·GitHub 공개(Phase 1)에 이어, 저장소를 **학습 교재**로 쓸 수 있는 문서 계층을 추가했다.
모든 변경은 검증 후 커밋·push됨 (public: github.com/sangjinsu/monolith-otel-lab, MIT).

## What Exists

- **앱**: Java 21 / Spring Boot 3.4.5 / Gradle. 패키지 web·order·inventory·payment·platform.observability.
  흐름 Reserve→Authorize→Insert(실패 시 미저장). PostgreSQL + Spring Data JPA.
- **관측성**: @Observed(Micrometer Observation) → OTel bridge → OTLP HTTP → Collector → Tempo.
  메트릭 Actuator /actuator/prometheus(Prometheus pull). 로그 JSON + MDC trace_id/span_id.
- **스택**: docker-compose 6서비스. `make up/down/logs/test/load`. APP_PORT로 호스트 포트 override.
- **학습 문서**: docs/{architecture, observability-deep-dive, study-guide}.md(한국어) +
  docs/images/ 실캡처 3장. README에 학습 진입점. 코드에 교육 주석(영어).
- **설계 기록**: .workspace (ADR 0001~0006, spec/architecture/observability-design/glossary,
  validation/test-results, experiments 2종).

## Key Facts (다음 세션이 알아야 할 것)

- **span 이름은 kebab-case로 표시됨** (`order-service.create-order`) — Micrometer
  SpanNameUtil.toLowerHyphen 정규화. 문서 전반이 실제 이름 기준. 코드 수정으로 되돌리지 말 것
  (의도된 결정, deep-dive §3).
- Tempo `block_retention: 1h` — 오래된 trace는 사라짐. 데모/스크린샷 전 `make load` 필수.
- 로컬 8080은 종종 다른 프로세스가 점유 → 검증은 `make up APP_PORT=18080` + `BASE_URL` env.
- Tempo 원시 JSON의 `org.springframework.boot`는 span이 아니라 InstrumentationScope.
- 테스트는 H2(4계층 슬라이스); 실제 PG는 compose 통합으로 검증.

## Verified (2026-07-04)

- ./gradlew test 12 PASS · docs/README 상대링크 전수 OK
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
