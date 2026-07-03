# ADR-0006: Language & Framework = Java 21 + Spring Boot 3

## Status

Accepted (supersedes the earlier Go/net-http decision)

## Context

초기에는 Go(net/http) 모놀리식으로 시작했으나, 사용자가 모놀리식 서버를 **Spring Boot + JPA**로
구현하도록 지시했다. 언어/프레임워크/빌드 도구를 재확정해야 한다.

## Decision

- 언어: **Java 21 (LTS)**
- 프레임워크: **Spring Boot 3 (Spring MVC)**
- 빌드: **Gradle (Groovy DSL)** + Gradle Wrapper (8.10.2)

## Rationale Summary

- 사용자 명시 지시(최우선).
- Spring Boot는 Micrometer Observation/Tracing, Actuator 등 관측성 기능이 풍부해 이 실험에 적합.
- 환경에 Java 21(Temurin), Gradle이 이미 설치되어 있어 즉시 빌드 가능.
- Groovy DSL은 build.gradle 가독성이 익숙하고 예제가 많다.

## Alternatives Considered

### Option A: Java + Spring Boot (채택)

장점: 사용자 지시, 풍부한 관측성 생태계, 자동 계측.
단점: Go 대비 부팅/메모리 무거움(실험엔 무관).

### Option B: Kotlin + Spring Boot

장점: 간결, null 안전.
단점: 일부 OTel/Micrometer 예제가 Java 기준. 사용자는 Java 선택.

## Consequences

코드는 src/main/java/com/sangjinsu/monolithotellab 패키지로 구성된다.
이전 Go 산출물(go.mod, cmd/, internal/)은 제거되었다.

## Follow-up

필요 시 Kotlin 전환/ Spring Modulith 도입 검토(실험 범위 밖).
