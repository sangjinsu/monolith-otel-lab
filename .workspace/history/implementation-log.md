# Implementation Log

## Format

```text
Date:
Task:
Files Changed:
Decision Summary:
Verification:
Next:
```

---

## Entries

### 2026-06-29 (1) — Go 부트스트랩 (이후 전환됨)

```text
Task: #1 부트스트랩 (Go net/http + SQLite 가정)
Decision Summary: 초기에는 Go 스택으로 시작. (이후 사용자 지시로 Spring Boot로 전환되어 무효)
Verification: go mod init 성공.
```

### 2026-06-29 (2) — 스택 전환: Spring Boot + JPA

```text
Task: #1 스택 전환 부트스트랩
Files Changed: go 산출물 제거; settings.gradle, build.gradle, gradle wrapper, main class;
  AGENTS.md v2 개정; .workspace ADR/문서 전면 갱신.
Decision Summary: 사용자 지시로 Spring Boot 3 + Java 21 + Gradle + PostgreSQL + Micrometer Observation.
Verification: gradle wrapper 8.10.2, ./gradlew compileJava BUILD SUCCESSFUL.
```

### 2026-06-29 (3) — 도메인/JPA/REST + 테스트 + 관측성

```text
Task: #2 도메인+JPA+REST, #3 테스트, #4 관측성
Files Changed:
  - order(Order/OrderItem/OrderStatus/OrderRepository/OrderService/dto), inventory, payment(fake),
    web(OrderController/HealthController/GlobalExceptionHandler/RequestLoggingFilter), application.yml
  - platform/observability/ObservabilityConfig(ObservedAspect), logback-spring.xml
  - 각 계층 @Observed(contextualName) + Tracer 태그(order.id/user.id/payment.result 등) + 메트릭 카운터
  - 테스트: OrderServiceTest(Mockito), OrderControllerTest(@WebMvcTest), OrderRepositoryTest(@DataJpaTest/H2),
    MonolithOtelLabApplicationTests(@SpringBootTest context-load)
Decision Summary:
  - 흐름 Reserve->Authorize->Insert. 실패 시 미저장 + order.failed.count.
  - OrderRepository.insert span은 ObservationRegistry로 save 감싸 생성.
  - 테스트 DB는 Testcontainers(colima docker 전략 미탐지) 대신 H2 슬라이스로 전환. 실제 PG는 compose에서 검증.
Verification: ./gradlew test BUILD SUCCESSFUL (12 tests). JSON 로그에 trace_id/span_id 필드 확인.
```

### 2026-06-29 (4) — 컨테이너화 + 스택 설정 + DX

```text
Task: #5 컨테이너화, #6 Makefile/load/README
Files Changed:
  - Dockerfile(멀티스테이지, JRE21, non-root), .dockerignore, docker-compose.yml(app/postgres/collector/tempo/prometheus/grafana)
  - deploy/otel-collector/config.yaml(traces), deploy/tempo/tempo.yaml(local), deploy/prometheus/prometheus.yml(app actuator scrape),
    deploy/grafana provisioning(datasources Tempo+Prometheus, dashboards) + dashboards/monolith-otel-lab.json
  - Makefile(COMPOSE ?= docker compose), scripts/load.sh(+x), README.md, .gitignore
Decision Summary: metric은 actuator 직접 scrape, trace는 collector->tempo. 대시보드 4패널(요청율/p95/주문성공/실패).
Verification: ./gradlew bootJar 성공(jar 생성). docker-compose config OK(6 services).
Next: #7 make up 통합 검증 (현재 docker-compose up --build 진행 중) + .workspace 마감.
```

### 2026-06-29 (5) — 통합 검증 완료 + GitHub 공개

```text
Task: #7 통합 검증 / 초기 커밋 / GitHub push
Decision Summary: 로컬 8080 점유(외부 요인)로 app 호스트 포트를 ${APP_PORT:-8080}로 파라미터화.
Verification: functional/trace(5계층+error span)/metric/log 전부 PASS — validation/test-results.md.
  초기 커밋 0762abf(65 files) 후 public 저장소 sangjinsu/monolith-otel-lab 생성·push.
```

### 2026-07-03 ~ 07-04 (6) — 학습자료 업그레이드

```text
Task: #8~#11 학습자료 계층 추가 (짜임새/구성도 리뷰 후속)
Files Changed:
  - docs/architecture.md(구성도 4종 mermaid + 설정 파일 지도),
    docs/observability-deep-dive.md(동작 원리, kebab-case 원리, InstrumentationScope, 실측 로그↔trace),
    docs/study-guide.md(읽기 순서, 체크포인트 8, 연습문제 6, FAQ) — 한국어
  - docs/images/{dashboard,trace-success,trace-failure}.png — Grafana 실캡처(합계 ~334KB)
  - README.md(학습 가이드 섹션, 실제 kebab-case span 이름+주의 박스, docs 링크, License),
    AGENTS.md(Trace Design 정규화 노트)
  - 교육용 코드 주석(영어): ObservabilityConfig, OrderService, FakePaymentClient,
    RequestLoggingFilter, logback-spring.xml, application.yml, 테스트 4클래스 헤더
  - LICENSE(MIT), .gitignore(+.gstack/ — 사용자 추가), GitHub topics 9종
Decision Summary:
  - span 이름 kebab-case 문제는 코드 수정(커스텀 handler, 버전 취약) 대신
    현실 문서화 + 원리 설명(SpanNameUtil.toLowerHyphen)으로 해결. 도전 연습문제로 남김.
  - Tempo 원시 JSON의 org.springframework.boot는 span이 아니라 InstrumentationScope임을 확인·문서화.
  - 스크린샷은 헤드리스 브라우저로 캡처(폼 로그인 불안정 → API 로그인 세션 쿠키 주입 방식).
Verification:
  - ./gradlew test 12개 PASS(주석/문서 변경 회귀 없음)
  - docs/README 상대링크 전수 존재 확인
  - 라이브 재검증: PromQL 3종 데이터 반환, 성공 trace 6 spans(payment 289.61/320.27ms),
    실패 trace 5 spans(payment error, insert 부재), 로그 trace_id(56d2c61f...)↔Tempo 상관 실측
    (deep-dive §8 예시·trace-success.png와 동일 trace)
Next: 커밋+push. Optional Extensions(Loki/Pyroscope/Jaeger/Modulith)는 백로그.
```

### 2026-07-04 (7) — make up 포터빌리티 수정 (compose 자동 감지)

```text
Task: 사용자 리포트 — `make up`이 "unknown flag: --build"로 실패
Root Cause: ~/.docker/cli-plugins/docker-compose가 제거된 Docker Desktop을 가리키는
  깨진 심링크(2023-05) → docker CLI가 compose 플러그인 로드 실패.
Files Changed:
  - Makefile: COMPOSE ?= $(shell docker compose version ... && echo docker compose || echo docker-compose)
    — 플러그인/단독 바이너리 자동 감지 (override는 기존대로 COMPOSE= 로 가능)
  - README.md: 자동 감지 노트, docs/study-guide.md: FAQ 행 추가("unknown flag: --build")
  - (환경, repo 외) 깨진 심링크를 /opt/homebrew/lib/docker/cli-plugins/docker-compose로 교체
Verification: docker compose version 5.2.0 OK, make -n up → "docker compose up --build",
  폴백 분기/수동 override 확인, docker compose config --services 6개 파싱 OK.
Next: 없음.
```

### 2026-07-04 (8) — make up app 기본 포트 변경

```text
Task: 사용자 리포트 — host 8080이 이미 점유되어 `make up` app 컨테이너 바인딩 실패
Root Cause: 다른 로컬 컨테이너가 0.0.0.0:8080을 사용 중인 상태에서 app 기본 host port도 8080으로 설정됨.
Files Changed:
  - docker-compose.yml: app host port 기본값을 ${APP_PORT:-10080}:8080으로 변경
  - scripts/load.sh: 기본 BASE_URL을 http://localhost:10080으로 변경
  - README.md, .workspace 문서: 실행 URL과 포트 정책 갱신
Decision Summary: 컨테이너 내부 포트와 Prometheus scrape(app:8080)는 유지하고,
  사용자가 접근하는 host port 기본값만 10080으로 분리. APP_PORT override는 유지.
Verification: docker compose config에서 published 10080 확인, ./gradlew test --rerun-tasks BUILD SUCCESSFUL,
  docker compose up -d --build 후 app 0.0.0.0:10080->8080/tcp 확인,
  curl http://localhost:10080/healthz -> {"status":"ok"}, make load exit 0.
Next: 없음.
```

### 2026-07-04 (9) — Tempo span metrics 추가

```text
Task: Tempo metrics-generator 기반 span metrics 추가
Files Changed:
  - deploy/tempo/tempo.yaml: metrics_generator + span-metrics processor + Prometheus remote_write
  - deploy/prometheus/prometheus.yml, docker-compose.yml: remote write receiver + exemplar storage
  - deploy/grafana provisioning/dashboard: Prometheus exemplars, Tempo tracesToMetrics, span metrics panels 3개
  - README.md, docs/*, .workspace 문서: span metrics 설명과 검증 결과 갱신
Decision Summary: 앱 metric은 기존 Actuator scrape를 유지하고, trace에서 파생되는 span RED metric은
  Tempo가 생성해 Prometheus로 remote write한다. Alert provisioning은 이번 범위에서 제외.
Verification:
  - jq/YAML/docker compose config 정적 검증 PASS
  - docker compose up -d --force-recreate grafana 후 Grafana API dashboard panel 5~7 확인
  - Prometheus metric names: traces_spanmetrics_calls_total, traces_spanmetrics_latency_bucket/count/sum, traces_spanmetrics_size_total
  - PromQL: payment-client.authorize STATUS_CODE_ERROR=1, span p95 latency query returns 8 span series
  - ./gradlew test --rerun-tasks BUILD SUCCESSFUL, git diff --check PASS
Next: 없음.
```
