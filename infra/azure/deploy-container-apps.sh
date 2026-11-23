#!/usr/bin/env bash
# EP5: construye en ACR y despliega/actualiza en Azure Container Apps servicio-flota (con Kafka)
# y los modulos EP5 servicio-monitoreo, admin-rabbitmq y admin-kafka.
# Requisitos: az login, Container Apps Environment, ACR, Azure Database for PostgreSQL y una VM
# en la misma VNet con RabbitMQ (platform/local/docker-compose.yml) y Kafka (platform/local/kafka).
set -euo pipefail

RG="${RG:-rg-fleetops}"
ENV_NAME="${ENV_NAME:-cae-fleetops-dev}"
ACR="${ACR:-acrfleetops}"
TAG="${TAG:-1.0.0}"
VM_IP="${VM_IP:?IP privada de la VM con RabbitMQ y Kafka (ej. 10.20.0.4)}"
PG_HOST="${PG_HOST:?host del Azure Database for PostgreSQL}"
PG_USER="${PG_USER:?usuario PostgreSQL}"
PG_PASSWORD="${PG_PASSWORD:?clave PostgreSQL}"
RABBITMQ_USER="${RABBITMQ_USER:-fleetops}"
RABBITMQ_PASSWORD="${RABBITMQ_PASSWORD:?clave RabbitMQ}"
SIMULATOR_SERVICE_TOKEN="${FLEETOPS_SIMULATOR_SERVICE_TOKEN:?token de servicio compartido flota/simulador}"
ENTRA_TENANT_ID="${ENTRA_TENANT_ID:?tenant Entra ID}"
ENTRA_API_CLIENT_ID="${ENTRA_API_CLIENT_ID:?client id de la API FleetOps}"
FRONTEND_ORIGIN="${FRONTEND_ORIGIN:-https://stfleetopsweb.z1.web.core.windows.net}"

cd "$(dirname "$0")/../../backend/servicios"

common_env=(
  "KAFKA_BOOTSTRAP_SERVERS=$VM_IP:9092,$VM_IP:9093,$VM_IP:9094"
  "SPRING_RABBITMQ_ADDRESSES=$VM_IP:5672,$VM_IP:5673"
  "SPRING_RABBITMQ_USERNAME=$RABBITMQ_USER" "SPRING_RABBITMQ_PASSWORD=secretref:rabbit-pass"
  "POSTGRES_HOST=$PG_HOST" "POSTGRES_DB=fleetops" "POSTGRES_USER=$PG_USER"
  "POSTGRES_PASSWORD=secretref:db-pass" "POSTGRES_SSLMODE=require"
  "ENTRA_TENANT_ID=$ENTRA_TENANT_ID" "ENTRA_API_CLIENT_ID=$ENTRA_API_CLIENT_ID"
  "ENTRA_API_APP_ID_URI=api://$ENTRA_API_CLIENT_ID" "FRONTEND_ORIGIN=$FRONTEND_ORIGIN"
)

deploy() { # imagen nombre-app contexto dockerfile [env extra...]
  local image=$1 app=$2 context=$3 dockerfile=$4; shift 4
  az acr build -r "$ACR" -t "$image:$TAG" -f "$dockerfile" --build-arg MODULE="$image" "$context"
  local secrets=("rabbit-pass=$RABBITMQ_PASSWORD" "db-pass=$PG_PASSWORD" "sim-token=$SIMULATOR_SERVICE_TOKEN")
  if az containerapp show -g "$RG" -n "$app" >/dev/null 2>&1; then
    az containerapp secret set -g "$RG" -n "$app" --secrets "${secrets[@]}" >/dev/null
    az containerapp update -g "$RG" -n "$app" --image "$ACR.azurecr.io/$image:$TAG" \
      --set-env-vars "${common_env[@]}" "$@"
  else
    az containerapp create -g "$RG" -n "$app" --environment "$ENV_NAME" \
      --image "$ACR.azurecr.io/$image:$TAG" --registry-server "$ACR.azurecr.io" \
      --target-port 8080 --ingress external --min-replicas 1 --max-replicas 3 \
      --cpu 0.5 --memory 1.0Gi --secrets "${secrets[@]}" \
      --env-vars "${common_env[@]}" "$@"
  fi
}

deploy servicio-flota ca-fleetops-servicio-flota servicio-flota servicio-flota/Dockerfile \
  "SPRING_PROFILES_ACTIVE=dev" "FLEETOPS_KAFKA_ENABLED=true" "FLEETOPS_KAFKA_CREATE_TOPICS=true" \
  "FLEETOPS_SIMULATOR_SERVICE_TOKEN=secretref:sim-token"
deploy servicio-monitoreo ca-fleetops-servicio-monitoreo . Dockerfile.ep5
deploy admin-rabbitmq ca-fleetops-admin-rabbitmq . Dockerfile.ep5 "RABBITMQ_MANAGEMENT_URL=http://$VM_IP:15672"
deploy admin-kafka ca-fleetops-admin-kafka . Dockerfile.ep5

echo "Listo. Registra cada FQDN como backend en Azure API Management con infra/azure/apim-policy.xml."
