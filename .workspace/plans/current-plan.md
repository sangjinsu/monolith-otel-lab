# Current Plan

## Current Phase

Phase 1: Spring Boot 모놀리식 + 관측성 스택 부트스트랩.

## Tasks

- [x] (구) Go 골격 생성 -> 사용자 지시로 제거
- [x] Spring Boot(Gradle) 골격 + Wrapper(8.10.2) + compileJava 검증
- [x] AGENTS.md v2(Spring Boot) 전면 개정
- [x] .workspace 갱신 (ADR/architecture/observability-design/spec 등)
- [ ] 주문 도메인 + JPA(PostgreSQL) + REST API (web/order/inventory/payment)
- [ ] 기본 테스트 (JUnit5 / MockMvc / Testcontainers)
- [ ] Micrometer Observation tracing + JSON 로깅(trace_id/span_id) + 메트릭
- [ ] Dockerfile + docker-compose (postgres 포함)
- [ ] otel-collector / tempo / prometheus / grafana 설정 + 최소 대시보드
- [ ] Makefile / scripts/load.sh / README
- [ ] make up / make load 검증

## Confirmed Decisions

Java 21 / Spring Boot 3 / Gradle(Groovy) / PostgreSQL + Spring Data JPA /
Micrometer Observation(OTel bridge) + Actuator Prometheus / docker compose / Grafana 대시보드 포함.

## Current Focus

OTel 없이 동작하는 비즈니스 로직(REST + 도메인 + JPA)을 먼저 만들고, 그 위에 Micrometer Observation을 입힌다.

## Blockers

없음.

## Next Action

주문 도메인 + JPA + REST API 구현 (Task #2).
