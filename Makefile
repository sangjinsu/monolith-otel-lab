# Container runtime. Prefers the `docker compose` plugin, falls back to the
# standalone `docker-compose` binary. Override with: make up COMPOSE="nerdctl compose"
COMPOSE ?= $(shell docker compose version >/dev/null 2>&1 && echo docker compose || echo docker-compose)
DOCKER ?= docker
KIND ?= kind
KUBECTL ?= kubectl
KIND_CLUSTER ?= monolith-otel-lab
K8S_APP_NAMESPACE ?= monolith-otel-app
K8S_DATA_NAMESPACE ?= monolith-otel-data
K8S_OBSERVABILITY_NAMESPACE ?= monolith-otel-observability
K8S_IMAGE ?= monolith-otel-lab:k8s-local
K8S_KIND_CONFIG ?= deploy/k8s/kind-config.yaml
K8S_KUSTOMIZE_DIR ?= deploy

.PHONY: up down logs test load build k8s-up k8s-down k8s-dry-run k8s-load k8s-logs k8s-status

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

k8s-up:
	@if ! $(KIND) get clusters 2>/dev/null | grep -qx "$(KIND_CLUSTER)"; then \
		$(KIND) create cluster --name "$(KIND_CLUSTER)" --config "$(K8S_KIND_CONFIG)"; \
	fi
	$(KUBECTL) config use-context "kind-$(KIND_CLUSTER)"
	$(DOCKER) build -t "$(K8S_IMAGE)" .
	$(KIND) load docker-image "$(K8S_IMAGE)" --name "$(KIND_CLUSTER)"
	$(KUBECTL) apply -k "$(K8S_KUSTOMIZE_DIR)"
	$(KUBECTL) -n "$(K8S_APP_NAMESPACE)" set image deployment/app app="$(K8S_IMAGE)"
	$(KUBECTL) -n "$(K8S_DATA_NAMESPACE)" rollout status deployment/postgres --timeout=180s
	$(KUBECTL) -n "$(K8S_OBSERVABILITY_NAMESPACE)" rollout status deployment/tempo --timeout=180s
	$(KUBECTL) -n "$(K8S_OBSERVABILITY_NAMESPACE)" rollout status deployment/prometheus --timeout=180s
	$(KUBECTL) -n "$(K8S_OBSERVABILITY_NAMESPACE)" rollout status deployment/otel-collector --timeout=180s
	$(KUBECTL) -n "$(K8S_OBSERVABILITY_NAMESPACE)" rollout status deployment/grafana --timeout=180s
	$(KUBECTL) -n "$(K8S_APP_NAMESPACE)" rollout status deployment/app --timeout=240s

k8s-down:
	$(KIND) delete cluster --name "$(KIND_CLUSTER)"

k8s-dry-run:
	$(KUBECTL) apply -k "$(K8S_KUSTOMIZE_DIR)" --dry-run=client

k8s-load:
	BASE_URL=http://localhost:10080 ./scripts/load.sh

k8s-logs:
	$(KUBECTL) -n "$(K8S_APP_NAMESPACE)" logs deployment/app --all-containers=true --tail=100
	$(KUBECTL) -n "$(K8S_OBSERVABILITY_NAMESPACE)" logs deployment/otel-collector --all-containers=true --tail=100

k8s-status:
	$(KUBECTL) -n "$(K8S_APP_NAMESPACE)" get pods,svc
	$(KUBECTL) -n "$(K8S_DATA_NAMESPACE)" get pods,svc
	$(KUBECTL) -n "$(K8S_OBSERVABILITY_NAMESPACE)" get pods,svc
