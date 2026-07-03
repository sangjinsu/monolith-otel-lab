# Container runtime. Override with: make up COMPOSE="nerdctl compose"
COMPOSE ?= docker compose

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
