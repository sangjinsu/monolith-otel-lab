# ADR-0003: OpenTelemetry + Tempo Stack

## Status

Accepted

## Context

trace/metrics를 수집·저장·시각화할 백엔드 스택을 정해야 한다.

## Decision

**OpenTelemetry Collector + Grafana Tempo + Prometheus + Grafana** 구성을 사용한다.

- Trace: 앱 -> OTLP(HTTP 4318) -> Collector -> Tempo.
- Metric: 앱 Actuator(`/actuator/prometheus`)를 Prometheus가 직접 scrape.
- Grafana가 Tempo/Prometheus를 datasource로 조회한다.

## Rationale Summary

- Collector를 두면 trace export 경로가 단순해지고 backend 교체가 쉬워진다(예: Jaeger).
- metric은 Spring Actuator로 노출하는 것이 표준이라 Prometheus가 직접 scrape한다.
- Tempo는 로컬 저장만으로 trace 조회가 가능해 실험에 적합하다.

## Alternatives Considered

### Option A: 앱이 Tempo/Jaeger로 직접 export

장점: 컴포넌트 하나 감소.
단점: 백엔드 결합도 증가, 가공/팬아웃 어려움.

### Option B: Trace는 Collector 경유, Metric은 Actuator scrape (채택)

장점: trace 디커플링, metric은 Spring 표준 방식.
단점: trace/metric 수집 경로가 이원화.

## Consequences

collector config는 traces 파이프라인 중심이 된다(metrics 파이프라인은 선택).
metric은 prometheus.yml의 scrape 설정(app:8080 /actuator/prometheus)으로 수집한다.

## Follow-up

Optional: Jaeger 백엔드 비교, Loki 로그 수집, metric도 OTLP로 통일, exemplar로 metric->trace 연결.
