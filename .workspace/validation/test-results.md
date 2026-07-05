# Test Results

## Latest Result — 2026-07-04 (Grafana alert test)

### Static configuration validation

```text
Command: ruby -e 'require "yaml"; ARGV.each { |path| YAML.load_file(path); puts "OK #{path}" }' deploy/grafana/provisioning/alerting/span-metrics-alerts.yaml deploy/grafana/provisioning/datasources/datasources.yaml deploy/grafana/provisioning/dashboards/dashboards.yaml docker-compose.yml
Result:  PASS

Command: docker compose config
Result:  PASS

Command: git diff --check
Result:  PASS
```

### Grafana provisioning verification

```text
Command: docker compose up -d --force-recreate grafana
Result:  Grafana restarted and provisioning reloaded

Command: curl -fsS -u admin:admin http://localhost:3000/api/v1/provisioning/alert-rules
Result:  uid=payment-span-errors, title="Payment authorization span errors",
         ruleGroup=span-metrics-alerts

Command: curl -fsS -u admin:admin http://localhost:3000/api/dashboards/uid/monolith-otel-lab
Result:  dashboard title "monolith-otel-lab" returned; dashboard provisioning not broken
```

### Alert firing verification

```text
Command: PromQL sum(increase(traces_spanmetrics_calls_total{service="monolith-otel-lab",span_name="payment-client.authorize",status_code="STATUS_CODE_ERROR"}[2m]))
Result:  before make load: 0

Command: make load
Result:  sent 20 successful orders and 1 failing payment request; exit 0

Command: same PromQL after make load
Result:  value > 1

Command: curl -fsS -u admin:admin http://localhost:3000/api/prometheus/grafana/api/v1/alerts
Result:  Payment authorization span errors state=Alerting, activeAt=2026-07-04T05:31:50Z

Command: curl -fsS -u admin:admin http://localhost:3000/api/alertmanager/grafana/api/v2/alerts
Result:  Payment authorization span errors status.state=active
```

### Unit / slice tests

```text
Command: ./gradlew test --rerun-tasks
Result:  BUILD SUCCESSFUL in 5s; 4 actionable tasks executed
```

## Previous Result — 2026-07-04 (Tempo span metrics)

### Static configuration validation

```text
Command: jq empty deploy/grafana/dashboards/monolith-otel-lab.json
Result:  PASS

Command: ruby -e 'require "yaml"; ARGV.each { |path| YAML.load_file(path); puts "OK #{path}" }' deploy/tempo/tempo.yaml deploy/prometheus/prometheus.yml deploy/grafana/provisioning/datasources/datasources.yaml docker-compose.yml
Result:  PASS

Command: docker compose config
Result:  PASS
```

### Runtime span metrics verification

```text
Command: docker compose up -d --force-recreate grafana
Result:  Grafana restarted and provisioning reloaded

Command: curl -fsS http://localhost:10080/healthz
Result:  {"status":"ok"}

Command: curl -fsS 'http://localhost:9090/api/v1/label/__name__/values' | jq -r '.data[] | select(test("^traces_spanmetrics_"))'
Result:  traces_spanmetrics_calls_total, traces_spanmetrics_latency_bucket,
         traces_spanmetrics_latency_count, traces_spanmetrics_latency_sum,
         traces_spanmetrics_size_total

Command: PromQL sum(traces_spanmetrics_calls_total{service="monolith-otel-lab"}) by (span_name,status_code)
Result:  11 series; includes http post /orders=21 and payment-client.authorize STATUS_CODE_ERROR=1

Command: PromQL histogram_quantile(0.95, sum(traces_spanmetrics_latency_bucket{service="monolith-otel-lab"}) by (le, span_name))
Result:  8 series; includes payment-client.authorize, order-service.create-order, http post /orders

Command: curl -fsS -u admin:admin http://localhost:3000/api/dashboards/uid/monolith-otel-lab
Result:  dashboard includes panels 5~7:
         Span request rate by span, Span p95 latency by span, Span error rate by span
```

### Unit / slice tests and hygiene

```text
Command: ./gradlew test --rerun-tasks
Result:  BUILD SUCCESSFUL in 5s; 4 actionable tasks executed

Command: git diff --check
Result:  PASS
```

Notes:

- Tempo 최신 로그에서 `error|failed|panic|fatal` 패턴은 확인되지 않음.
- Prometheus remote write receiver는 compose command flag로 활성화됨.

## Previous Result — 2026-07-04 (app host port 10080)

### Port / compose verification

```text
Command: docker compose config
Result:  app port interpolation OK — published "10080", target 8080

Command: lsof -nP -iTCP:10080 -sTCP:LISTEN || true
Result:  no existing listener before stack startup

Command: docker compose up -d --build
Result:  app image built, app container started

Command: docker compose ps
Result:  monolith-otel-lab-app-1 Up, 0.0.0.0:10080->8080/tcp

Command: curl -fsS http://localhost:10080/healthz
Result:  {"status":"ok"}

Command: make load
Result:  sent 20 successful orders to http://localhost:10080/orders and 1 failing payment request; exit 0
```

Notes:

- `make up` still resolves to `docker compose up --build` (`make -n up` verified).
- Verification used detached `docker compose up -d --build` to avoid leaving the foreground compose session attached.
- Container-internal app port remains `8080`; Prometheus still scrapes `app:8080`.

### Unit / slice tests

```text
Command: ./gradlew test --rerun-tasks
Result:  BUILD SUCCESSFUL in 6s; 4 actionable tasks executed
```

## Previous Full Observability Result — 2026-06-29

### Unit / slice tests

```text
Command: ./gradlew test
Result:  BUILD SUCCESSFUL — 12 tests
  - OrderServiceTest (Mockito) 4
  - OrderControllerTest (@WebMvcTest / MockMvc) 5
  - OrderRepositoryTest (@DataJpaTest / H2) 2
  - MonolithOtelLabApplicationTests (@SpringBootTest context-load) 1
```

### Integration — docker compose stack

```text
Command: docker-compose up -d --build   (app on host port 18080; see note)
Stack:   app, postgres(16), otel-collector(contrib 0.110), tempo(2.6.1), prometheus(2.54), grafana(11.2) — all Up
```

Functional:

```text
GET  /healthz                       -> 200 {"status":"ok"}
POST /orders                        -> 201 {"order_id":"...","status":"created"}
GET  /orders/{id}                   -> 200 {order_id,user_id,status,items}
POST /orders?fail_payment=true      -> 402 {"error":"payment authorization failed"}
GET  /orders/does-not-exist         -> 404
```

Metrics (Actuator + Prometheus):

```text
order_created_count_total = 21
order_failed_count_total  = 2
http_server_requests_seconds_count{method=POST,status=201,uri="/orders"} = 21
Prometheus scrape of app:8080 /actuator/prometheus OK; query order_created_count_total returns data.
```

Traces (Tempo search/traces API):

```text
SUCCESS POST /orders trace spans:
  http post /orders
    -> order-controller.create-order
    -> order-service.create-order
    -> inventory-service.reserve
    -> payment-client.authorize
    -> order-repository.insert
FAILURE (fail_payment=true) trace:
  payment-client.authorize has STATUS_CODE_ERROR + event "payment authorization failed"
  (order-repository.insert absent — not persisted on payment failure)
```

Logs:

```text
JSON logs include trace_id / span_id.
e.g. {"...","logger":"http.access","message":"request completed","trace_id":"05c1a7f9...","span_id":"d9ebc57a...","method":"GET","path":"/actuator/prometheus","status":200,"duration_ms":33}
```

### Notes

- **App host port**: 이 당시 로컬 8080 점유 때문에 `APP_PORT=18080`으로 검증했다.
  2026-07-04부터 기본 host port는 `10080`이며, 컨테이너 내부 포트와 Prometheus scrape(`app:8080`)는 영향 없음.
- **Span 이름 형식**: Micrometer Observation이 `contextualName`을 kebab-case로 정규화하여
  `OrderController.createOrder` -> `order-controller.create-order`로 나타난다. 계층 식별에는 충분.
- **Testcontainers**: colima 환경에서 Testcontainers의 docker 전략 탐지가 실패하여 JPA 슬라이스 테스트는
  H2로 수행. 실제 PostgreSQL 동작은 위 통합 스택에서 검증됨.
```
