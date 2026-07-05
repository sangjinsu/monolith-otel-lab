# Current Plan

## Current Phase

Phase 6 완료: 로컬 Kubernetes 예제 추가 및 kind runtime 검증 (2026-07-05).
프로젝트는 "실행 가능한 관측성 lab" + "읽고 실습하는 교재" 두 역할을 모두 갖춤.

## Done

- [x] Phase 1 (2026-06-29): Spring Boot 모놀리식 + 관측성 스택 구현·통합검증·GitHub 공개
      (상세: history/implementation-log.md 1~5)
- [x] Phase 2 (2026-07-03~04): 학습자료 업그레이드
  - [x] docs/architecture.md — 구성도 4종(mermaid) + 설정 파일 지도
  - [x] docs/observability-deep-dive.md — @Observed→Tempo 체인, kebab-case 원리, 실측 예
  - [x] docs/study-guide.md — 읽기 순서, 체크포인트 9, 연습문제 6, FAQ
  - [x] docs/images/ — Grafana 실캡처 3장 (dashboard / trace-success / trace-failure)
  - [x] README 학습 섹션 + 실제 span 이름, AGENTS.md 정규화 노트
  - [x] 교육용 코드 주석(영어) + 테스트 4클래스 헤더
  - [x] LICENSE(MIT) + GitHub topics 9종
  - [x] 검증: gradlew test 12 PASS, 링크 전수, 라이브 체크포인트 재실행
- [x] Phase 3 (2026-07-04): 로컬 8080 충돌 회피
  - [x] docker-compose app host port 기본값을 10080으로 변경
  - [x] scripts/load.sh와 README 실행 예시를 10080 기준으로 갱신
  - [x] .workspace handoff/검증 문서 갱신
- [x] Phase 4 (2026-07-04): Tempo span metrics
  - [x] Tempo metrics-generator + span-metrics processor 설정
  - [x] Prometheus remote write receiver + exemplar storage 설정
  - [x] Grafana datasource/dashboard에 span metrics 연결
  - [x] README/docs/.workspace 설계 문서 갱신
  - [x] runtime Prometheus query로 `traces_spanmetrics_*` 검증
- [x] Phase 5 (2026-07-04): Grafana alert 테스트
  - [x] span metrics 기반 Grafana managed alert rule provisioning 추가
  - [x] README/docs/.workspace alert 테스트 절차 갱신
  - [x] Grafana API로 rule provisioning 확인
  - [x] `make load`로 Firing 상태 검증
- [x] Phase 6 (2026-07-05): 로컬 Kubernetes 예제
  - [x] kind config + raw manifests + Kustomize ConfigMap generator 추가
  - [x] Makefile k8s 타깃 추가
  - [x] README/docs/.workspace k8s 실습 문서 갱신
  - [x] `kubectl apply --dry-run=client -k deploy` 검증
  - [x] `make k8s-up`, `make k8s-load` runtime 검증
  - [x] 운영 구조에 가깝게 app/data/observability namespace 목적별 분리

## Backlog (Could Have)

- Loki(로그 수집·양방향 점프), Pyroscope(프로파일링), Jaeger(백엔드 교체 실험),
  Spring Modulith 비교 — AGENTS.md Optional Extensions
- CamelCase span 이름 유지 커스텀 handler (study-guide 연습문제 6 — 학습자 몫으로 남김)
- colima 환경 Testcontainers 활성화

## Blockers

없음.

## Next Action

선택: 로컬 kind 실습이 끝나면 `make k8s-down`으로 클러스터 정리.
