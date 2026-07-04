# 동작 원리 딥다이브 (Observability Deep Dive)

`@Observed` 애노테이션 한 줄이 어떻게 Grafana 화면의 span이 되는가 — 그 여정을 단계별로 따라간다.

- 전체 그림이 먼저 필요하면 → [architecture.md](architecture.md)
- 손으로 확인하려면 → [study-guide.md](study-guide.md)
- 용어가 낯설면 → [.workspace/project/glossary.md](../.workspace/project/glossary.md)

---

## 1. Micrometer Observation — 계측의 단일 진입점

Spring Boot 3의 관측성 철학은 **"Observation 하나로 span과 metric을 동시에"** 다.
`Observation`을 만들면 등록된 handler들이 각자 할 일을 한다 — tracing handler는 span을,
metrics handler는 timer를 만든다. 계측 코드를 한 번만 쓰면 두 신호가 나온다.

이 프로젝트에서 Observation이 만들어지는 세 가지 방식:

| 방식 | 어디서 | 예 |
|---|---|---|
| `@Observed` 애노테이션 | 각 계층 메서드 | [`InventoryService.reserve`](../src/main/java/com/sangjinsu/monolithotellab/inventory/InventoryService.java), [`FakePaymentClient.authorize`](../src/main/java/com/sangjinsu/monolithotellab/payment/FakePaymentClient.java) |
| 수동 Observation API | [`OrderService.insertOrder`](../src/main/java/com/sangjinsu/monolithotellab/order/OrderService.java) | Spring Data 프록시 우회 (§5) |
| 프레임워크 자동 | Spring MVC | root span `http post /orders` (§4) |

---

## 2. `@Observed` → Tempo까지의 여정 (trace 경로)

```text
@Observed 메서드 호출
  │ ①  ObservedAspect (AOP) — ObservabilityConfig에서 빈으로 등록. spring-boot-starter-aop 필요
  ▼
Observation 생성 (name + contextualName)
  │ ②  ObservationRegistry의 handler 체인 실행
  ▼
TracingObservationHandler → span 시작        (micrometer-tracing)
  │ ③  Micrometer Tracing ↔ OpenTelemetry 브리지 (micrometer-tracing-bridge-otel)
  ▼
OTel SDK Span
  │ ④  BatchSpanProcessor — span을 모아서 ~5초 주기로 배치 전송
  │     (→ "요청 직후 Tempo에 trace가 안 보이는" 이유)
  ▼
OtlpHttpSpanExporter → POST http://otel-collector:4318/v1/traces
  │     endpoint: application.yml `management.otlp.tracing.endpoint`
  │     (compose에서는 env OTEL_EXPORTER_OTLP_ENDPOINT로 주입)
  ▼
OTel Collector — receivers.otlp → processors.batch → exporters.otlp/tempo (+debug)
  │     deploy/otel-collector/config.yaml
  ▼
Tempo — distributor(OTLP 수신) → ingester → local blocks (/var/tempo)
  │     deploy/tempo/tempo.yaml · 보존 기간 block_retention: 1h
  ▼
Grafana tempo datasource (:3200 조회)
```

샘플링은 `management.tracing.sampling.probability: 1.0`(전량 수집, 실험용) —
운영에서는 이 값을 낮추는 것이 첫 번째 비용 조절 수단이다.

---

## 3. span 이름은 왜 kebab-case가 되는가

코드에는 분명 이렇게 썼다:

```java
@Observed(name = "order.create", contextualName = "OrderService.createOrder")
```

그런데 Tempo에는 `order-service.create-order`로 보인다. **버그가 아니다.**
micrometer-tracing의 `TracingObservationHandler#getSpanName`이 span 이름을 만들 때
`SpanNameUtil.toLowerHyphen()`으로 **정규화**하기 때문이다 (CamelCase → kebab-case).

| 코드의 contextualName | Tempo 표시 이름 (실측) |
|---|---|
| (Spring MVC 자동) | `http post /orders` |
| `OrderController.createOrder` | `order-controller.create-order` |
| `OrderService.createOrder` | `order-service.create-order` |
| `InventoryService.reserve` | `inventory-service.reserve` |
| `PaymentClient.authorize` | `payment-client.authorize` |
| `OrderRepository.insert` | `order-repository.insert` |

계층 식별에는 전혀 지장이 없으므로 이 프로젝트는 정규화된 이름을 그대로 쓴다.
CamelCase를 유지하고 싶다면 `TracingObservationHandler`를 상속해 `getSpanName`을
오버라이드해야 하는데, 자동 구성된 handler 교체는 버전에 취약하다 —
[study-guide 연습문제 6](study-guide.md#5-연습문제)의 도전 과제로 남겨둔다.

**덤 — 원시 JSON의 함정**: Tempo API로 trace JSON을 열면 `"name": "org.springframework.boot"`가
보이는데, 이것은 span이 아니라 **InstrumentationScope**(어떤 라이브러리가 계측했는지 식별,
version 3.4.5)다. OTLP 데이터 모델은 `Resource(누가) → InstrumentationScope(무엇으로) → Span(작업)`
3단 구조다.

---

## 4. root span은 누가 만드나

우리는 HTTP 요청용 span 코드를 쓴 적이 없다. Spring Boot가 `ServerHttpObservationFilter`를
자동 등록하기 때문이다 (observation 이름 `http.server.requests`, order = `HIGHEST_PRECEDENCE + 1` —
사실상 필터 체인 맨 앞). 이 필터가:

1. 요청마다 root span(`http post /orders`)을 열고,
2. 같은 이름의 **타이머 메트릭**(`http_server_requests_seconds_*`)을 만들고,
3. MDC에 `traceId`/`spanId`를 넣는다 (§8의 로그 상관이 공짜로 되는 이유).

uri 라벨은 실제 경로가 아니라 **라우트 템플릿**(`/orders/{orderId}`)이다 — 주문 ID가 백만 개여도
시계열은 하나. 메트릭 카디널리티 보호의 교과서적 예다.

---

## 5. Spring Data 인터페이스와 수동 Observation

`OrderRepository`는 인터페이스고 구현체는 Spring Data가 런타임에 만드는 프록시다.
우리 코드가 아니므로 `@Observed`를 붙일 수 없다. 그래서 [`OrderService.insertOrder`](../src/main/java/com/sangjinsu/monolithotellab/order/OrderService.java)가
호출 지점을 수동 Observation으로 감싼다:

```java
Observation.createNotStarted("order.repository.insert", observationRegistry)
        .contextualName("OrderRepository.insert")
        .lowCardinalityKeyValue("db.system", "postgresql")
        .lowCardinalityKeyValue("db.operation", "insert")
        .observe(() -> orderRepository.save(order));
```

`@Observed`가 내부적으로 하는 일을 그대로 노출한 형태라, 애노테이션 방식과 API 방식을
비교 학습하기에 좋은 지점이다.

---

## 6. span attribute와 에러 기록

**태깅** — [`OrderService.tagCreateSpan`](../src/main/java/com/sangjinsu/monolithotellab/order/OrderService.java)은
`tracer.currentSpan().tag(...)`로 현재 span에 `order.id`/`user.id`/`item.count`를 붙인다.
ObservedAspect가 메서드 **진입 전에** span을 열어두므로, 본문에서 "현재 span"을 잡을 수 있다.

**카디널리티** — `lowCardinalityKeyValue`는 metric 라벨로도 전파될 수 있으므로 값의 종류가
유한해야 한다(예: `db.operation=insert`). `order.id`처럼 무한한 값은 span 태그로만 쓴다.
민감정보(password, token, email 등)는 절대 넣지 않는다 — [AGENTS.md § Span Attribute Rules](../AGENTS.md).

**에러** — `@Observed` 메서드에서 예외가 던져지면 aspect가 `observation.error(ex)`를 호출하고,
bridge가 span을 `STATUS_CODE_ERROR` + exception 이벤트로 기록한다.
[`FakePaymentClient`](../src/main/java/com/sangjinsu/monolithotellab/payment/FakePaymentClient.java)는
throw 직전에 `payment.result=failed`를 태깅해 "왜 실패했는지"를 span에 남긴다.

---

## 7. Metric 경로 — pull 모델과 이름 변환

**등록**: `OrderService` 생성자에서 `Counter.builder("order.created.count")...register(meterRegistry)`.
**노출**: PrometheusMeterRegistry가 `/actuator/prometheus`로 렌더링, Prometheus가 5초마다 pull.

Micrometer 이름과 Prometheus 이름은 다르다 — 변환 규칙을 알아야 쿼리를 쓸 수 있다:

| Micrometer (코드) | Prometheus (쿼리) | 규칙 |
|---|---|---|
| `order.created.count` (Counter) | `order_created_count_total` | `.`→`_`, Counter는 `_total` 접미 |
| `order.failed.count` (Counter) | `order_failed_count_total` | 〃 |
| `http.server.requests` (Timer) | `http_server_requests_seconds_count` / `_sum` / `_bucket` | 기본 단위 seconds 삽입, 3종 시계열로 분해 |

**p95의 비밀**: `histogram_quantile(0.95, ...)`는 `_bucket` 시계열이 있어야 동작한다.
Micrometer는 기본으로 bucket을 내보내지 **않으므로** application.yml에
`management.metrics.distribution.percentiles-histogram.http.server.requests: true`를 켰다.
이 설정을 지우면 대시보드 p95 패널이 "No data"가 된다 — 직접 실험해볼 것.

---

## 8. Log ↔ Trace 상관 — MDC 한 줄의 마법

1. `ServerHttpObservationFilter`가 observation scope를 열면 Micrometer Tracing이
   **MDC**에 `traceId`/`spanId`를 넣는다.
2. [`logback-spring.xml`](../src/main/resources/logback-spring.xml)의 pattern provider가
   `%mdc{traceId}`를 **`trace_id`** 필드로 개명해 JSON에 출력한다 (AGENTS.md 로그 필드 규약).
3. [`RequestLoggingFilter`](../src/main/java/com/sangjinsu/monolithotellab/web/RequestLoggingFilter.java)는
   기본 순서(LOWEST_PRECEDENCE)라 observation 필터 **안쪽**에서 실행 → 로그 시점에 MDC가 차 있다.

실측 예 — 주문 생성 요청 하나의 로그(발췌):

```json
{"logger":"http.access","message":"request completed",
 "trace_id":"56d2c61fd756945017b96da2d5e1f336","span_id":"033dc86454011189",
 "method":"POST","path":"/orders","status":201,"duration_ms":320}
```

이 `trace_id`를 Grafana Tempo에 붙여넣으면 **정확히 그 요청의** trace가 열린다 —
실제로 [trace-success.png](images/trace-success.png)가 바로 이 trace이며, trace 전체
길이 **320.27ms**가 로그의 `duration_ms: 320`과 일치한다. 같은 요청을 로그와 trace
양쪽에서 보고 있다는 증거다.
"에러 로그 발견 → trace로 점프 → 어느 계층이 문제인지 확인"이 이 실험의 최종 시나리오다.
([study-guide 체크포인트 7](study-guide.md#4-실습-체크포인트)에서 직접 해본다.)

---

## 9. 계측 코드 침투 — 정직한 트레이드오프

`OrderService` 생성자를 보면 의존성이 6개다: repository, inventory, payment에 더해
**observationRegistry, tracer, meterRegistry**. 관측성 관심사가 비즈니스 클래스에
스며든 것이다. AGENTS.md 코딩 규칙 7("관측성 설정은 platform 패키지에 모은다")과
긴장 관계에 있는, 명시적 계측 방식의 실제 비용이다.

| 접근 | 침투 | 제어 | 이 프로젝트 |
|---|---|---|---|
| 수동/명시 계측 (현재) | 높음 | 세밀함 (attribute·카운터 위치 자유) | ✅ 학습 목적으로 채택 |
| `@Observed`만 사용 | 낮음 | 이름/기본 태그 수준 | 부분 사용 |
| OTel Java Agent | 없음 (바이트코드) | 커스텀 span은 결국 코드 필요 | [ADR-0004](../.workspace/decisions/ADR-0004-observability-micrometer.md)에서 대안으로 검토 |

개선 여지: 카운터 2개와 태깅을 `OrderMetrics` 컴포넌트로 추출하면 생성자가 다이어트된다 —
[study-guide 연습문제 5](study-guide.md#5-연습문제)로 직접 해보길.

---

## 10. 더 읽을거리

- 설계 결정 기록: [.workspace/decisions/](../.workspace/decisions/) (ADR 0001~0006)
- 관측성 설계 원칙: [.workspace/project/observability-design.md](../.workspace/project/observability-design.md)
- 검증 기록(실측 데이터): [.workspace/validation/test-results.md](../.workspace/validation/test-results.md)
- 공식 문서: [Micrometer Observation](https://docs.micrometer.io/micrometer/reference/observation.html) ·
  [OpenTelemetry](https://opentelemetry.io/docs/) · [Grafana Tempo](https://grafana.com/docs/tempo/latest/)
