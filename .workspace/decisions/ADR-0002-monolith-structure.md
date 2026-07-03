# ADR-0002: Monolith Structure

## Status

Accepted

## Context

관측성 실험을 하면서도 내부 계층 경계를 명확히 드러내야 span 설계가 깔끔해진다.
구조를 어떻게 나눌지 결정이 필요하다.

## Decision

**단일 프로세스, 단일 배포 단위, 단일 데이터베이스**를 유지하되 내부 모듈을 Java 패키지로 분리한다.
서비스를 여러 프로세스로 쪼개지 않는다. base package `com.sangjinsu.monolithotellab` 아래
`web` / `order` / `inventory` / `payment` / `platform`으로 나눈다.

## Rationale Summary

- MSA가 아니라 모놀리식 내부 흐름 관측이 목적이므로 프로세스 분리는 불필요하다.
- 계층(web/order/inventory/payment)이 span 이름과 1:1로 대응하도록 한다.
- Spring 컴포넌트 스캔 범위 내에서 패키지로 경계를 표현한다.

## Alternatives Considered

### Option A: services/*-service 디렉터리로 분리

장점: 추후 MSA 전환 용이.
단점: 프로젝트 목적(모놀리식 관측)과 정면 충돌, 불필요한 복잡도.

### Option B: 단일 프로세스 + 패키지 분리 (채택)

장점: 목적 부합, span 계층이 명확.
단점: 모듈 간 경계가 코드 규율에 의존.

## Consequences

각 계층이 자신의 Observation(span)을 생성한다. trace 트리가 패키지/계층 구조를 그대로 반영한다.

## Follow-up

Spring Modulith로 모듈 경계를 강제하고 span 설계가 더 명확해지는지 비교(Optional Extension).
