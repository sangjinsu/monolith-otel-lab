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
