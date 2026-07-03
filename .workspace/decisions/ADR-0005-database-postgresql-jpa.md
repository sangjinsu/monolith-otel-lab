# ADR-0005: Database = PostgreSQL + Spring Data JPA

## Status

Accepted (supersedes the earlier SQLite/modernc decision)

## Context

스택을 Spring Boot + JPA로 전환하면서 데이터베이스를 재검토했다. AGENTS.md(v1)는 SQLite를 명시했으나,
JPA(Hibernate) 맥락에서 dialect/계측 궁합을 고려해 다시 정한다.

## Decision

**PostgreSQL** + **Spring Data JPA (Hibernate)** 를 사용한다. docker compose에 postgres 컨테이너를 추가한다.

## Rationale Summary

- JPA 표준 조합으로 dialect/드라이버가 가장 안정적이고 예제가 풍부하다.
- OTel JDBC/Hibernate 계측과 datasource observation 궁합이 좋다.
- compose에 컨테이너 하나 추가하는 비용은 실험에서 충분히 수용 가능하다.

## Alternatives Considered

### Option A: PostgreSQL + JPA (채택)

장점: JPA 표준, 계측 친화, production-like.
단점: compose 컨테이너 1개 추가, 기동 시간 소폭 증가.

### Option B: SQLite 유지

장점: 컨테이너 불필요, 파일 기반.
단점: JPA에서 community SQLite dialect 필요(덜 일반적), 동시성/계측 예제 부족.

### Option C: H2

장점: 가장 간단, JPA 친화.
단점: 영속성 약함(재시작 시 데이터 손실 가능). 테스트 프로파일 용도로는 고려.

## Consequences

application.yml에 postgres datasource를 설정하고 Hibernate ddl-auto(또는 schema.sql)로 스키마를 만든다.
테스트는 Testcontainers PostgreSQL(또는 H2 프로파일)로 수행한다.

## Follow-up

테스트 격리를 위해 Testcontainers 사용. 운영 마이그레이션 도구(Flyway 등)는 실험 범위 밖.
