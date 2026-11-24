#!/usr/bin/env bash
# Administración replicable de tópicos FleetOps. Uso:
#   docker compose run --rm kafka-init                    (desde platform/local/kafka)
#   BOOTSTRAP=localhost:9092 ./create-topics.sh           (con Kafka CLI instalado)
set -euo pipefail
BOOTSTRAP="${BOOTSTRAP:-kafka-1:29092}"
RF="${REPLICATION_FACTOR:-3}"

create() { # nombre particiones config...
  local name=$1 partitions=$2; shift 2
  local cfg=()
  for c in "$@"; do cfg+=(--config "$c"); done
  kafka-topics --bootstrap-server "$BOOTSTRAP" --create --if-not-exists \
    --topic "$name" --partitions "$partitions" --replication-factor "$RF" "${cfg[@]}"
  # si el tópico ya existía, se re-aplican las configuraciones deseadas
  local joined; joined=$(IFS=,; echo "$*")
  kafka-configs --bootstrap-server "$BOOTSTRAP" --alter --entity-type topics --entity-name "$name" --add-config "$joined"
}

# Ciclo de vida de viajes: auditoría 7 días, orden por viaje (key = tripId)
create fleetops.viajes.eventos 3 retention.ms=604800000 cleanup.policy=delete min.insync.replicas=2

# Telemetría GPS: alto volumen, 3 particiones, retención corta 24 h, segmentos de 1 h
create fleetops.telemetria.gps 3 retention.ms=86400000 cleanup.policy=delete segment.ms=3600000 min.insync.replicas=2 compression.type=lz4

# Dead letter topics: 14 días para investigar mensajes fallidos
create fleetops.viajes.eventos.DLT 1 retention.ms=1209600000 cleanup.policy=delete
create fleetops.telemetria.gps.DLT 1 retention.ms=1209600000 cleanup.policy=delete

echo "== Tópicos =="
kafka-topics --bootstrap-server "$BOOTSTRAP" --describe --topic 'fleetops\..*' | grep -v '^\s*Topic:.*Partition:' || true
echo "== Consumer groups =="
kafka-consumer-groups --bootstrap-server "$BOOTSTRAP" --list || true
