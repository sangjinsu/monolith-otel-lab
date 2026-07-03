# Test Results

## Latest Result — 2026-06-29

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

- **App host port**: 로컬 8080이 ssh 터널 + 다른 프로젝트 k8s 컨테이너로 점유되어, docker-compose에서
  app 포트를 `${APP_PORT:-8080}`로 파라미터화하고 검증은 `APP_PORT=18080`으로 수행했다. 컨테이너 내부 포트와
  Prometheus scrape(`app:8080`)는 영향 없음.
- **Span 이름 형식**: Micrometer Observation이 `contextualName`을 kebab-case로 정규화하여
  `OrderController.createOrder` -> `order-controller.create-order`로 나타난다. 계층 식별에는 충분.
- **Testcontainers**: colima 환경에서 Testcontainers의 docker 전략 탐지가 실패하여 JPA 슬라이스 테스트는
  H2로 수행. 실제 PostgreSQL 동작은 위 통합 스택에서 검증됨.
```
