# Trace Scenario: Baseline Order Creation

## Purpose

정상 주문 생성 요청에서 모놀리식 내부 흐름이 trace로 잘 보이는지 확인한다.

## Request

```bash
curl -X POST "http://localhost:10080/orders" \
  -H "Content-Type: application/json" \
  -d '{"user_id":"user-1","items":[{"sku":"item-1","quantity":2}]}'
```

## Expected Trace

```text
POST /orders
  -> OrderController.createOrder
  -> OrderService.createOrder
  -> InventoryService.reserve
  -> PaymentClient.authorize
  -> OrderRepository.insert
```

## Actual Result — 2026-06-29 (PASS)

Tempo에 저장된 정상 trace의 span (kebab-case로 정규화됨):

```text
http post /orders                       (Spring MVC root span)
  -> order-controller.create-order
  -> order-service.create-order
  -> inventory-service.reserve
  -> payment-client.authorize
  -> order-repository.insert
```

- 모든 span success.
- 응답 201 `{"order_id":"...","status":"created"}`.
- 메트릭 `order_created_count_total` 증가 확인.
