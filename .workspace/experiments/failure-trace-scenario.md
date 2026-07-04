# Trace Scenario: Payment Failure

## Purpose

실패 요청에서 error span이 제대로 기록되는지 확인한다.

## Request

```bash
curl -X POST "http://localhost:10080/orders?fail_payment=true" \
  -H "Content-Type: application/json" \
  -d '{"user_id":"user-1","items":[{"sku":"item-1","quantity":2}]}'
```

## Expected Trace

```text
POST /orders
  -> OrderController.createOrder
  -> OrderService.createOrder
  -> InventoryService.reserve
  -> PaymentClient.authorize  ERROR
```

## Actual Result — 2026-06-29 (PASS)

```text
http post /orders
  -> order-controller.create-order
  -> order-service.create-order
  -> inventory-service.reserve
  -> payment-client.authorize   STATUS_CODE_ERROR, event "payment authorization failed"
(order-repository.insert 없음 — 결제 실패로 미저장)
```

- 응답 402 `{"error":"payment authorization failed"}`.
- 메트릭 `order_failed_count_total` 증가 확인.
- JSON 로그에 trace_id 포함.
