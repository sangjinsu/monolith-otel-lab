# Project Spec

## Project Name

monolith-otel-lab

## Purpose

모놀리식 Spring Boot 애플리케이션에서 OpenTelemetry와 Grafana Tempo를 적용하여 요청 흐름,
내부 계층별 처리 시간, 에러 발생 위치를 추적할 수 있는지 검증한다.

## Main Scenario

POST /orders 요청 하나가 다음 내부 흐름을 거쳐야 한다.

```text
OrderController
  -> OrderService
  -> InventoryService
  -> PaymentClient
  -> OrderRepository (JPA)
  -> PostgreSQL
```

## Required APIs

- GET /healthz
- POST /orders
- GET /orders/{order_id}
- POST /orders?fail_payment=true

## Required Observability

- OpenTelemetry tracing (Micrometer Observation/Tracing, OTel bridge)
- Grafana Tempo trace backend
- Prometheus metrics (Micrometer + Actuator)
- Grafana dashboard
- JSON logs with trace_id and span_id

## Acceptance Criteria

- 주문 생성 trace를 Grafana Tempo에서 볼 수 있다.
- trace 안에 controller, service, inventory, payment, repository span이 보인다.
- 실패 요청에서 error span이 기록된다.
- JSON log에 trace_id와 span_id가 포함된다.
- make up, make load로 로컬 검증이 가능하다.
