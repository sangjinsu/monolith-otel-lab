# Glossary

이 프로젝트에서 자주 쓰는 관측성 용어 정리.

- **Trace**: 하나의 요청이 시스템 내부를 거치는 전체 경로. 여러 span으로 구성된다.
- **Span**: trace를 구성하는 단위 작업. 시작/종료 시각, 이름, attribute, status를 가진다.
  부모-자식 관계로 트리를 이룬다.
- **Root span**: trace의 최상위 span. 이 프로젝트에서는 HTTP 미들웨어가 요청당 하나 생성한다.
- **trace_id / span_id**: trace와 span의 고유 식별자. 로그에 포함하면 로그-트레이스 상관이 가능하다.
- **Context propagation**: `context.Context`를 통해 span 정보를 함수 호출 사이로 전달하는 것.
  내부 호출은 모두 ctx를 첫 인자로 받는다.
- **Resource attribute**: telemetry 생산자(서비스)를 식별하는 속성.
  예: service.name, service.version, deployment.environment.
- **OTLP (OpenTelemetry Protocol)**: telemetry를 전송하는 표준 프로토콜. gRPC(4317)/HTTP(4318).
- **Exporter**: SDK가 telemetry를 외부로 내보내는 컴포넌트 (예: otlptracegrpc).
- **Sampler**: 어떤 trace를 수집할지 결정. 로컬 실험은 always_on(AlwaysSample).
- **OpenTelemetry Collector**: telemetry를 수신/가공/전달하는 중간 서비스.
  이 프로젝트에서는 trace를 Tempo로, metrics를 Prometheus로 라우팅한다.
- **Grafana Tempo**: 분산 trace 저장/조회 백엔드.
- **Prometheus**: 시계열 metrics 저장/쿼리 시스템. Collector의 :8889를 scrape한다.
- **Grafana**: Tempo/Prometheus를 datasource로 연결해 trace/metrics를 시각화하는 대시보드.
