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
