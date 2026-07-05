# 학습 가이드 (Study Guide)

이 저장소를 **교재처럼** 사용하는 방법 — 읽기 순서, 실습 체크포인트, 연습문제.

**배울 수 있는 것**: 모놀리식 Spring Boot 앱에 OpenTelemetry 기반 관측성(trace / metric / log)을
입히는 전 과정 — MSA가 아니어도 내부 계층 흐름을 추적할 수 있음을 눈으로 확인한다.

**전제**: Docker(또는 colima 등) + make. JDK 21은 로컬 테스트 실행 시에만 필요하다.
로컬 Kubernetes 실습은 추가로 kind와 kubectl이 필요하다.

---

## 1. 용어 5분 정리

trace, span, root span, context propagation, OTLP, Collector, resource attribute...
→ [.workspace/project/glossary.md](../.workspace/project/glossary.md) 를 먼저 훑고 온다.

---

## 2. 코드 읽기 순서

비즈니스 흐름 → 관측성 순서로 읽는 것을 권한다. 각 파일에 교육용 주석이 달려 있다.

| # | 파일 | 여기서 볼 것 |
|---|---|---|
| 1 | [`web/OrderController.java`](../src/main/java/com/sangjinsu/monolithotellab/web/OrderController.java) | HTTP 계약, `fail_payment` 쿼리 파라미터 |
| 2 | [`order/OrderService.java`](../src/main/java/com/sangjinsu/monolithotellab/order/OrderService.java) | 핵심 흐름 Reserve→Authorize→Insert, 수동 Observation, 커스텀 Counter |
| 3 | [`inventory/InventoryService.java`](../src/main/java/com/sangjinsu/monolithotellab/inventory/InventoryService.java) | 가장 단순한 `@Observed` 사용 예 |
| 4 | [`payment/FakePaymentClient.java`](../src/main/java/com/sangjinsu/monolithotellab/payment/FakePaymentClient.java) | 지연 시뮬레이션, 실패 시 span 태깅 + throw |
| 5 | [`order/Order.java`](../src/main/java/com/sangjinsu/monolithotellab/order/Order.java) · [`OrderRepository.java`](../src/main/java/com/sangjinsu/monolithotellab/order/OrderRepository.java) | JPA 매핑, `@EntityGraph` |
| 6 | [`web/GlobalExceptionHandler.java`](../src/main/java/com/sangjinsu/monolithotellab/web/GlobalExceptionHandler.java) | 예외 → 402/404 매핑 |
| 7 | [`web/RequestLoggingFilter.java`](../src/main/java/com/sangjinsu/monolithotellab/web/RequestLoggingFilter.java) | 요청당 1줄 구조화 로그, MDC 타이밍 |
| 8 | [`platform/observability/ObservabilityConfig.java`](../src/main/java/com/sangjinsu/monolithotellab/platform/observability/ObservabilityConfig.java) | `@Observed`를 살아있게 만드는 단 하나의 빈 |
| 9 | [`application.yml`](../src/main/resources/application.yml) · [`logback-spring.xml`](../src/main/resources/logback-spring.xml) | OTLP endpoint, 샘플링, 히스토그램, trace_id 매핑 |
| 10 | [`docker-compose.yml`](../docker-compose.yml) · [`deploy/`](../deploy/) | 스택 조립, [설정 파일 지도](architecture.md#5-설정-파일-지도--어떤-설정이-어떤-구간을-지배하는가) |

테스트도 교재다 — 4개 클래스가 Spring 테스트 전략 4계층(순수 단위 / @WebMvcTest /
@DataJpaTest / @SpringBootTest)을 하나씩 시연한다. [`src/test/`](../src/test/java/com/sangjinsu/monolithotellab/) 참조.

---

## 3. 포트 안내

기본 host 포트는 `10080`이다. 이미 사용 중이면:

```bash
make up APP_PORT=18080                      # 호스트 포트만 변경 (컨테이너 내부는 8080 유지)
BASE_URL=http://localhost:18080 make load   # load.sh도 같은 포트로
```

---

## 4. 실습 체크포인트

순서대로 진행한다. 각 단계의 "기대 결과"가 나오면 통과.

### CP1 — 스택 기동
```bash
make up        # 첫 실행은 이미지 빌드로 수 분 소요
```
**기대**: `docker ps`에 컨테이너 6개(app, postgres, otel-collector, tempo, prometheus, grafana).

### CP2 — 헬스 체크
```bash
curl localhost:10080/healthz
```
**기대**: `{"status":"ok"}`

### CP3 — 트래픽 생성
```bash
make load
```
**기대**: `sending 20 successful orders ... sending 1 failing order ... done`

### CP4 — 성공 trace 찾기 (Grafana → Tempo)
1. http://localhost:3000 접속 (admin / admin — 비밀번호 변경 프롬프트는 Skip 가능)
2. 좌측 **Explore** → datasource **Tempo** 선택
3. **Search** 탭에서 Span Name에 `order-service.create-order` 입력 → Run query
   (⚠️ `OrderService.createOrder`로 검색하면 **안 나온다** — [이름 정규화](observability-deep-dive.md#3-span-이름은-왜-kebab-case가-되는가) 때문)
4. 아무 trace나 클릭

**기대**: 아래 5계층 트리 — `payment-client.authorize`가 가장 긴 구간(50~300ms)임을 확인.

```text
http post /orders
 └─ order-controller.create-order
     └─ order-service.create-order
         ├─ inventory-service.reserve
         ├─ payment-client.authorize
         └─ order-repository.insert
```

![성공 trace](images/trace-success.png)

### CP5 — 실패 trace의 error span
Explore → Tempo에서 **TraceQL** 탭에 아래 입력:
```traceql
{ status = error }
```
**기대**: trace를 열면 `payment-client.authorize` span에 에러 표시(빨간색) +
`payment authorization failed` 이벤트. 그리고 **`order-repository.insert` span이 없다** —
결제 실패 시 저장하지 않는 비즈니스 규칙이 trace 구조로 증명된다.

![실패 trace](images/trace-failure.png)

### CP6 — Prometheus 쿼리 3개
http://localhost:9090 에서 각각 실행:

```promql
order_created_count_total
```
```promql
sum(rate(http_server_requests_seconds_count[1m])) by (uri)
```
```promql
histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket[5m])) by (le, uri))
```
**기대**: ① 누적 주문 수(예: 21), ② uri별 초당 요청률, ③ uri별 p95 지연(초).
이름이 코드(`order.created.count`)와 다른 이유는 [변환 규칙](observability-deep-dive.md#7-metric-경로--pull-모델과-이름-변환) 참조.

### CP7 — 로그 ↔ trace 상관 (이 실험의 하이라이트)
```bash
make logs        # 또는: docker compose logs app | grep '"status":201' | tail -1
```
1. 201 로그 한 줄에서 `"trace_id":"..."` 값을 복사
2. Grafana Explore → Tempo → **TraceQL** 탭에 trace_id를 그대로 붙여넣고 실행

**기대**: 방금 그 로그를 남긴 **바로 그 요청**의 trace가 열린다.
실무 시나리오("에러 로그 발견 → trace 점프 → 병목 계층 식별")의 축소판이다.

### CP8 — span metrics 기반 alert 테스트
```bash
make load
```

1. Grafana → **Alerting** → **Alert rules**
2. `Payment authorization span errors` rule을 연다.
3. 필요하면 API로도 확인한다.

```bash
curl -fsS -u admin:admin http://localhost:3000/api/alertmanager/grafana/api/v2/alerts \
  | jq '.[] | select(.labels.alertname=="Payment authorization span errors") | {labels,status}'
```

**기대**: `make load`의 실패 결제 요청 때문에 10~30초 안에 rule이 Firing으로 바뀐다.
새 실패 요청이 없으면 최근 2분 window가 지나며 Normal로 돌아간다.

### CP9 — 정리
```bash
make down        # 볼륨 포함 삭제 (-v)
```

---

## 5. 연습문제

난이도 순. 모두 `make test`(단위 테스트)와 스택 재기동으로 검증할 수 있다.

| # | 난이도 | 과제 | 힌트 |
|---|---|---|---|
| 1 | ⭐ | `order-service.create-order` span에 `order.status` attribute 추가 | `OrderService.tagCreateSpan` 참고 |
| 2 | ⭐⭐ | 가짜 `NotificationService`(주문 완료 알림) 계층 추가 + span으로 확인 | `InventoryService` 복제 → `@Observed` → trace에 6번째 계층 |
| 3 | ⭐⭐ | `order.items.total` Counter 추가(주문당 아이템 수 누적), Prometheus에서 확인 | Counter 등록 → `_total` 이름으로 쿼리 |
| 4 | ⭐⭐ | `FakePaymentClient`의 sleep을 500~800ms로 올리고 대시보드 p95 변화 관찰 | 재빌드 후 `make load` 수회 |
| 5 | ⭐⭐⭐ | 카운터·태깅을 `OrderMetrics` 컴포넌트로 추출해 `OrderService` 생성자 다이어트 | [딥다이브 §9](observability-deep-dive.md#9-계측-코드-침투--정직한-트레이드오프)의 트레이드오프 해소 |
| 6 | ⭐⭐⭐⭐ | span 이름을 CamelCase 그대로 유지 | `TracingObservationHandler#getSpanName` 오버라이드 — 자동 구성 handler 교체 필요, 버전 취약성 직접 체험 |

---

## 6. 자주 걸리는 함정 (FAQ)

| 증상 | 원인 / 해법 |
|---|---|
| Tempo에서 `OrderService.createOrder` 검색이 안 됨 | 이름이 kebab-case로 정규화됨 → `order-service.create-order`로 검색 ([원리](observability-deep-dive.md#3-span-이름은-왜-kebab-case가-되는가)) |
| 요청 직후 trace가 안 보임 | BatchSpanProcessor가 ~5초 배치 전송 → 몇 초 기다렸다 재검색 |
| 예전 trace가 사라짐 | Tempo `block_retention: 1h` (tempo.yaml) → `make load`로 새로 생성 |
| `make up`에서 8080 충돌 | `make up APP_PORT=18080` (§3) |
| `make up`에서 `unknown flag: --build` | docker CLI가 compose 플러그인을 못 찾는 환경(예: 제거된 Docker Desktop의 깨진 플러그인 심링크). Makefile이 `docker-compose`로 자동 폴백한다. 수동 지정: `make up COMPOSE=docker-compose` |
| p95 패널이 No data | `percentiles-histogram` 설정 확인 — `_bucket` 시계열이 있어야 `histogram_quantile` 동작 |
| Alert가 바로 Firing 되지 않음 | Grafana rule interval(10s), Tempo metrics-generator, Prometheus remote write 지연 때문에 10~30초 기다린다. 그래도 안 되면 `make load`를 다시 실행 |
| Grafana 첫 로그인에서 비밀번호 변경 요구 | 실험 환경이므로 Skip 해도 무방 |

---

## 7. 다음 단계

### 로컬 Kubernetes 실습

Compose 실습을 마쳤다면 같은 구조를 kind 기반 Kubernetes에서도 띄워볼 수 있다.
host port는 compose와 동일하게 `10080`, `3000`, `9090`을 사용하므로 compose stack과 동시에 실행하지 않는다.

```bash
make down          # compose stack이 떠 있다면 먼저 정리
make k8s-up
curl -fsS http://localhost:10080/healthz
make k8s-load
```

확인 위치:

```text
Grafana:    http://localhost:3000
Prometheus: http://localhost:9090
```

상태와 로그:

```bash
make k8s-status
make k8s-logs
```

정리:

```bash
make k8s-down
```

이 예제는 Helm이나 OpenTelemetry Operator를 쓰지 않고 raw manifest + Kustomize ConfigMap generator로 구성한다.
목표는 운영 배포가 아니라 compose와 Kubernetes 리소스를 1:1로 비교하는 학습이다.

기본 과정을 마쳤다면 [AGENTS.md의 Optional Extensions](../AGENTS.md#optional-extensions):

- **Loki** — 로그도 수집해 `trace_id`로 trace↔log 양방향 점프
- **Pyroscope** — CPU 프로파일링으로 "느린 함수" 식별
- **Jaeger** — 같은 계측을 유지한 채 backend만 교체 (OTel의 이식성 증명)
- **Spring Modulith** — 모듈 경계를 강제했을 때 span 설계 변화 비교
