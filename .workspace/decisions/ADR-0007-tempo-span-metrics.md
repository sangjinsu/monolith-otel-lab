# ADR-0007: Tempo Span Metrics

## Status
Accepted

## Context
Tempo trace는 개별 요청의 원인 분석에는 좋지만, alerting과 장기 추세 확인에는 숫자형 time series가 필요하다.
현재 프로젝트는 앱이 직접 노출하는 Actuator/Micrometer metrics만 Prometheus가 scrape하고 있다.

## Decision
Tempo metrics-generator의 `span-metrics` processor를 활성화해 trace span에서 RED metrics를 생성한다.
생성된 `traces_spanmetrics_*` metric은 Prometheus remote write receiver(`/api/v1/write`)로 저장한다.

## Rationale Summary
이 방식은 Spring Boot 코드 계측을 늘리지 않고, 이미 수집 중인 span에서 request rate, error, latency 지표를 얻는다.
Grafana의 trace-to-metrics 탐색과 span 단위 dashboard/alerting 실험에도 바로 연결된다.

## Alternatives Considered
### TraceQL metrics only
장점: 별도 저장소 설정 없이 즉석 분석 가능.
단점: alerting/장기 추세에는 제약이 크고, 현재 Grafana 11.2 환경에서는 TraceQL alerting을 목표로 삼기 어렵다.

### OpenTelemetry Collector spanmetrics connector
장점: Tempo에 덜 의존한다.
단점: 현재 lab의 trace backend 실험 초점이 Tempo이므로 구성 요소가 늘어나고 학습 경로가 분산된다.

## Consequences
Prometheus는 remote write receiver와 exemplar storage feature를 켜야 한다.
Span metrics는 trace sampling의 영향을 받을 수 있으므로, 이 lab에서는 기존처럼 sampling probability 1.0을 유지한다.

## Follow-up
Span metrics가 안정적으로 들어온 뒤 Grafana alert rule provisioning을 추가한다.
