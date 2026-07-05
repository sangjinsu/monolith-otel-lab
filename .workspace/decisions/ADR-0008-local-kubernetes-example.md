# ADR-0008: Local Kubernetes Example

## Status
Accepted

## Context
Docker Compose 기반 lab은 로컬 학습에 충분하지만, 사용자가 Kubernetes 환경에서 같은 관측성 구조를
실험해보고 싶다고 요청했다. 운영용 Kubernetes 배포가 아니라 compose 구조를 Kubernetes 리소스로
비교하는 학습 예제가 필요하다.

## Decision
`kind` 기반 local Kubernetes 예제를 추가한다.
리소스는 목적별 namespace로 분리한다.

```text
monolith-otel-app             Spring Boot app
monolith-otel-data            PostgreSQL
monolith-otel-observability   OpenTelemetry Collector, Tempo, Prometheus, Grafana
```

Grafana provisioning은 기존 compose 파일을 Kustomize `configMapGenerator`로 재사용한다.
namespace가 다른 의존성은 Kubernetes DNS 규칙에 맞춰 namespace-qualified service name을 사용한다.

## Rationale Summary
- `kind`는 Docker/Colima 환경에서 재현성이 좋고 local image load가 단순하다.
- raw manifests는 Kubernetes 기본 리소스 구조를 학습하기 좋다.
- 기존 Grafana dashboard/alert 파일을 재사용하면 compose와 k8s 실습 결과가 drift되지 않는다.
- app/data/observability namespace 분리는 실제 운영 구조와 더 비슷하고, cross-namespace service discovery를
  명확히 보여준다.

## Alternatives Considered
### Helm chart 기반
장점: 운영 배포 방식에 가깝다.
단점: 학습 초점이 Helm values로 이동하고, compose와 1:1 비교가 어려워진다.

### OpenTelemetry Operator 포함
장점: Kubernetes 운영형 자동 계측을 경험할 수 있다.
단점: 초기 k8s 실습 범위가 커진다. 이번 단계에서는 수동 manifest가 더 적절하다.

## Consequences
`make k8s-up/down/load/logs/status` 타깃을 제공한다.
host port는 compose와 동일하게 app `10080`, Grafana `3000`, Prometheus `9090`을 사용하므로
compose stack과 동시에 띄우면 포트 충돌이 날 수 있다.
동일 namespace 안에서는 짧은 service name을 유지하고, namespace를 넘는 연결은
`service.namespace.svc.cluster.local` 형태로 명시한다.

## Follow-up
다음 확장으로 Helm chart 버전, OpenTelemetry Operator auto-instrumentation, Ingress/TLS/PVC 구성을 검토한다.
