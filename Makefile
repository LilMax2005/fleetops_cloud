.PHONY: kafka-up kafka-down ep5-up ep5-down help local-up local-down local-restart local-logs local-ps local-clean rabbitmq-check rabbitmq-dlq-demo api-smoke-test env-check git-status

COMPOSE_FILE := platform/local/docker-compose.yml
ENV_FILE_OPTION := $(if $(wildcard .env),--env-file .env,)
DOCKER_COMPOSE := docker compose $(ENV_FILE_OPTION) -f $(COMPOSE_FILE)

help:
	@echo "FleetOps - comandos disponibles"
	@echo ""
	@echo "  make local-up       Levanta la infraestructura local"
	@echo "  make local-down     Detiene la infraestructura local"
	@echo "  make local-restart  Reinicia la infraestructura local"
	@echo "  make local-logs     Muestra logs de Docker Compose"
	@echo "  make local-ps       Lista contenedores locales"
	@echo "  make local-clean    Detiene el entorno y elimina volumenes locales"
	@echo "  make rabbitmq-check Verifica cluster RabbitMQ y topologia"
	@echo "  make rabbitmq-dlq-demo Ejecuta demo de mensajes a las tres DLQ"
	@echo "  make api-smoke-test Prueba los endpoints REST contra localhost:8080"
	@echo "  make env-check      Verifica archivos de entorno esperados"
	@echo "  make git-status     Muestra el estado Git"
	@echo "  make kafka-up       EP5: levanta Zookeeper, 3 brokers Kafka, topicos y Kafka UI"
	@echo "  make ep5-up         EP5: plataforma local + Kafka en flota + monitoreo/admin-kafka/admin-rabbitmq"
	@echo "  make ep5-down       EP5: detiene la plataforma EP5 (Kafka sigue arriba)"
	@echo "  make kafka-down     EP5: detiene Kafka y Kafka UI"

local-up:
	$(DOCKER_COMPOSE) up -d

local-down:
	$(DOCKER_COMPOSE) down

local-restart: local-down local-up

local-logs:
	$(DOCKER_COMPOSE) logs -f

local-ps:
	$(DOCKER_COMPOSE) ps

local-clean:
	$(DOCKER_COMPOSE) down -v --remove-orphans

rabbitmq-check:
	./scripts/unix/rabbitmq-cluster-check.sh

rabbitmq-dlq-demo:
	./scripts/unix/rabbitmq-dlq-demo.sh

api-smoke-test:
	./scripts/unix/api-smoke-test.sh

env-check:
	@test -f .env.example || (echo "ERROR: falta .env.example" && exit 1)
	@if [ -f .env ]; then echo "OK: .env local encontrado"; else echo "WARN: .env no existe. Copia .env.example a .env para personalizar valores locales."; fi

git-status:
	git status

# --- EP5: Apache Kafka ---
KAFKA_COMPOSE := docker compose --env-file platform/local/kafka/.env -f platform/local/kafka/docker-compose.yml
KAFKA_UI_COMPOSE := docker compose --env-file platform/local/kafka-ui/.env -f platform/local/kafka-ui/docker-compose.yml
EP5_COMPOSE := $(DOCKER_COMPOSE) -f platform/local/docker-compose.ep5.yml

kafka-up:
	$(KAFKA_COMPOSE) up -d
	$(KAFKA_UI_COMPOSE) up -d

kafka-down:
	$(KAFKA_UI_COMPOSE) down
	$(KAFKA_COMPOSE) down

ep5-up: kafka-up
	$(EP5_COMPOSE) up -d --build

ep5-down:
	$(EP5_COMPOSE) down
