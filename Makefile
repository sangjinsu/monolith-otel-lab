# Container runtime. Prefers the `docker compose` plugin, falls back to the
# standalone `docker-compose` binary. Override with: make up COMPOSE="nerdctl compose"
COMPOSE ?= $(shell docker compose version >/dev/null 2>&1 && echo docker compose || echo docker-compose)

.PHONY: up down logs test load build

up:
	$(COMPOSE) up --build

down:
	$(COMPOSE) down -v

logs:
	$(COMPOSE) logs -f app otel-collector

test:
	./gradlew test

build:
	./gradlew bootJar

load:
	./scripts/load.sh
