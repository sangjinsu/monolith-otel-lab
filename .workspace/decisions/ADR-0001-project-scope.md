# ADR-0001: Project Scope

## Status

Accepted

## Context

이 프로젝트가 운영용 서비스인지, 학습/검증용 실험인지에 따라 구현 깊이와 추상화 수준이 크게 달라진다.

## Decision

이 프로젝트는 운영용 서비스가 아니라 **모놀리식 관측성 실험 프로젝트**이다.
목표는 단일 애플리케이션 내부에서 요청 흐름(Handler -> Service -> Inventory -> Payment -> Repository -> DB)을
OpenTelemetry trace로 추적할 수 있음을 검증하는 것이다.

## Rationale Summary

- 실험 목적이므로 과한 추상화와 production 수준 구성을 피하고, local-first 실행 경험을 우선한다.
- Non-Goals: MSA, Kubernetes, 인증/인가, 복잡한 도메인 모델링, 실제 결제 연동, 운영 알림, 대규모 부하 테스트.

## Alternatives Considered

### Option A: 운영 수준 서비스로 구현

장점: 실전에 가까움.
단점: 범위 폭증, 학습 초점 흐려짐, 시간 낭비.

### Option B: 관측성 실험에 집중 (채택)

장점: 학습 목표에 집중, 빠른 반복.
단점: production 고려사항 일부 생략.

## Consequences

코드는 "관측성을 어떻게 적용하는가"를 드러내는 데 최적화된다. 비즈니스 로직은 의도적으로 단순하다.

## Follow-up

실험 결과가 좋으면 Optional Extensions(Loki, Pyroscope, Jaeger, modular monolith)로 확장 검토.
