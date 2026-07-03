# LLM Workspace

이 디렉터리는 LLM 코드 에이전트가 프로젝트 맥락을 유지하기 위한 작업 위키이다.

## Read Order

작업 시작 전 다음 순서로 읽는다.

1. project/spec.md
2. project/architecture.md
3. project/observability-design.md
4. plans/current-plan.md
5. decisions/*.md
6. history/implementation-log.md
7. prompts/agent-handoff.md

## Write Policy

작업 중 중요한 변경이 있으면 다음을 갱신한다.

- plans/current-plan.md
- history/implementation-log.md
- validation/acceptance-checklist.md

작업 완료 후 다음을 갱신한다.

- history/changelog.md
- validation/test-results.md
- prompts/agent-handoff.md

## Forbidden Content

다음 정보는 저장하지 않는다.

- API key
- access token
- refresh token
- password
- 개인 식별 정보
- 민감한 사용자 정보
- LLM의 숨겨진 chain-of-thought 전문
