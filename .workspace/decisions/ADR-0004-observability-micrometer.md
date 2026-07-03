# ADR-0004: Observability via Micrometer Observation

## Status

Accepted (supersedes the earlier "OTel Go SDK / OTel Metrics SDK" decision)

## Context

Spring Boot 3 환경에서 trace와 metric을 어떻게 계측할지 정해야 한다. 계층별 커스텀 span
(OrderService.createOrder 등)을 명시적으로 만들면서도 코드 침투를 줄이고 싶다.

## Decision

**Micrometer Observation** 모델을 사용한다.

- Trace: Micrometer Tracing + **OpenTelemetry bridge**(`micrometer-tracing-bridge-otel`) +
  OTLP exporter(`opentelemetry-exporter-otlp`)로 Collector(HTTP 4318)에 전송 -> Tempo.
- 계층 span: `@Observed`(ObservedAspect) 또는 `ObservationRegistry`로 생성, `contextualName`으로 span 이름 지정.
- Metric: **Micrometer + Actuator Prometheus registry**. Prometheus가 앱의 `/actuator/prometheus`를 직접 scrape.

## Rationale Summary

- 하나의 Observation이 span+metric을 동시에 만들어 계측이 일관된다.
- Spring MVC/HTTP는 자동으로 root span(http.server.requests)을 만든다.
- metric은 actuator로 노출하는 것이 Spring 표준이며, trace(Collector 경유)와 경로가 분리되어도 운영이 단순하다.

## Alternatives Considered

### Option A: Micrometer Observation (채택)

장점: Spring 네이티브, span+metric 통합, 자동 계측, @Observed로 계층 span.
단점: 내부적으로 OTel bridge를 거치므로 OTel 순수 API와 약간 다른 추상화.

### Option B: OTel Java Agent + @WithSpan

장점: HTTP/JDBC 자동 계측, 코드 침투 최소.
단점: javaagent 실행 구성 필요, Spring 메트릭과 이원화 가능.

### Option C: OTel Java SDK 수동

장점: 가장 명시적.
단점: 보일러플레이트 많음, Spring 자동 계측 이점 포기.

## Consequences

- 의존성: spring-boot-starter-actuator, spring-boot-starter-aop, micrometer-tracing-bridge-otel,
  opentelemetry-exporter-otlp, micrometer-registry-prometheus.
- metric 파이프라인은 Collector 필수가 아님(app actuator 직접 scrape).
- 필수 metric: http.server.requests(자동), order.created.count / order.failed.count(커스텀 Counter).

## Follow-up

필요 시 metric도 OTLP로 Collector에 보내는 구성(Micrometer OTLP registry)으로 전환 가능.
trace<->log 연결은 MDC traceId/spanId로, 추후 exemplar로 metric->trace 연결 검토.
