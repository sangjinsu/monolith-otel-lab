# Observability Design

## Goal

모놀리식 Spring Boot 내부 요청 흐름을 trace, metrics, logs로 확인한다.

## Approach

Micrometer Observation 모델: 하나의 Observation이 span과 metric을 함께 생성한다.
Micrometer Tracing(OpenTelemetry bridge) + OTLP exporter로 Collector에 trace를 보내고,
metric은 Actuator Prometheus registry로 노출한다. (decisions/ADR-0004)

## Trace Flow

```text
POST /orders                          (Spring MVC 자동 root span)
  -> OrderController.createOrder
  -> OrderService.createOrder
  -> InventoryService.reserve
  -> PaymentClient.authorize
  -> OrderRepository.insert
```

> 호출 순서: Reserve -> Authorize -> Insert. 결제 실패 시 저장하지 않는다.

## Span Naming Rule

`@Observed(contextualName = "...")` 또는 ObservationRegistry로 지정.

좋은 예:

```text
OrderController.createOrder
OrderService.createOrder
InventoryService.reserve
PaymentClient.authorize
OrderRepository.insert
```

나쁜 예:

```text
process / run / logic / handler / function
```

## Required Span Attributes (Observation key values)

```text
order.id
user.id
item.count
db.system
db.operation
payment.provider
payment.result
```

민감정보(password, token, email, card number, raw body 등)는 넣지 않는다.

## Error Rule

에러 발생 시 `observation.error(throwable)`로 기록한다(= span error).
저수준 Span을 직접 다루면 recordException + setStatus(ERROR).

## Metrics

방식: **Micrometer + Actuator Prometheus** (앱 /actuator/prometheus를 Prometheus가 직접 scrape).

필수 metrics:

```text
http.server.requests        (Micrometer 기본: count + duration histogram)
order.created.count         (커스텀 Counter)
order.failed.count          (커스텀 Counter)
```

Prometheus 표기 예: http_server_requests_seconds_count, order_created_count_total.

## Logs

Logback structured JSON. Micrometer Tracing이 MDC에 traceId/spanId를 주입하면 JSON 로그에
trace_id/span_id로 포함한다(필드명 매핑).

```text
timestamp / level / message / trace_id / span_id / method / path / status / duration_ms
```

로그와 trace를 연결할 수 있어야 한다.
