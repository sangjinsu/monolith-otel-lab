# Architecture

## Architecture Type

이 프로젝트는 모놀리식 Spring Boot 애플리케이션이다.

```text
Single Process
Single Deployment Unit
Single Database (PostgreSQL)
Multiple Internal Modules (Java packages)
```

## Internal Modules

base package: `com.sangjinsu.monolithotellab`

```text
web         REST controller, exception handler        (OrderController, HealthController, GlobalExceptionHandler)
order       domain service, JPA entity, repository, dto (OrderService, Order, OrderItem, OrderRepository)
inventory   inventory service                          (InventoryService)
payment     fake payment client                        (FakePaymentClient)
platform
  observability  Observation/Tracing 설정              (ObservabilityConfig: ObservedAspect 등)
  (logging/persistence 보조 설정은 필요 시)
```

## Architecture Rule

서비스를 여러 프로세스로 분리하지 않는다.

피해야 할 구조:

```text
order-service
payment-service
inventory-service
```

이 프로젝트의 목적은 MSA가 아니라, 모놀리식 내부 흐름을 관측하는 것이다.
계층(web/order/inventory/payment)이 span 이름과 1:1로 대응하도록 한다.

## Build & Runtime

```text
Java 21 (LTS) / Spring Boot 3 / Gradle (Groovy) + Wrapper 8.10.2
Database: PostgreSQL (docker compose)
Trace: Micrometer Tracing(OTel bridge) -> OTLP -> Collector -> Tempo
Metric: Micrometer -> Actuator /actuator/prometheus -> Prometheus scrape
Log: Logback structured JSON (MDC traceId/spanId)
```

(참고: decisions/ADR-0002, ADR-0004, ADR-0005, ADR-0006)
