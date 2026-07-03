# Acceptance Checklist

(검증 완료: 2026-06-29 — docker compose 통합 스택, app host port 18080)

## Functional

- [x] GET /healthz returns 200
- [x] POST /orders creates order (201)
- [x] GET /orders/{order_id} returns order
- [x] POST /orders?fail_payment=true returns failure response (402)

## Observability

- [x] Grafana can connect to Tempo (datasource provisioned; tempo Up)
- [x] Grafana can connect to Prometheus (datasource provisioned; prometheus Up)
- [x] POST /orders trace is visible (Tempo search/traces API)
- [x] Trace includes controller span (order-controller.create-order)
- [x] Trace includes service span (order-service.create-order)
- [x] Trace includes inventory span (inventory-service.reserve)
- [x] Trace includes payment span (payment-client.authorize)
- [x] Trace includes repository span (order-repository.insert)
- [x] Failed payment trace records error (payment-client.authorize STATUS_CODE_ERROR)
- [x] Logs include trace_id
- [x] Logs include span_id
- [x] Prometheus exposes request/order metrics (order_created_count_total, order_failed_count_total, http_server_requests_*)

## Developer Experience

- [x] make up works (docker compose up --build; `APP_PORT` overridable for port conflicts)
- [x] make down works (docker compose down -v)
- [x] make load works (20 ok + 1 fail)
- [x] README explains how to run

## Notes

- Grafana datasource는 provisioning 파일 + 백엔드 동작으로 확인. 대시보드 패널의 시각적 확인은
  사용자가 http://localhost:3000 에서 직접 가능.
- Span 이름은 Micrometer naming convention으로 kebab-case 정규화됨(계층 식별 가능).
