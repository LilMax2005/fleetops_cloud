#!/usr/bin/env bash
# Inspección y mantenimiento de consumer groups. Ejemplos:
#   docker exec -it kafka-1 bash /scripts/consumer-groups.sh describe monitoreo-telemetria
#   docker exec -it kafka-1 bash /scripts/consumer-groups.sh reset monitoreo-viajes fleetops.viajes.eventos earliest
set -euo pipefail
BOOTSTRAP="${BOOTSTRAP:-kafka-1:29092}"
case "${1:-list}" in
  list)     kafka-consumer-groups --bootstrap-server "$BOOTSTRAP" --list ;;
  describe) kafka-consumer-groups --bootstrap-server "$BOOTSTRAP" --describe --group "$2" --members --verbose
            kafka-consumer-groups --bootstrap-server "$BOOTSTRAP" --describe --group "$2" ;;
  reset)    kafka-consumer-groups --bootstrap-server "$BOOTSTRAP" --group "$2" --topic "$3" \
              --reset-offsets --to-"${4:-earliest}" --execute ;;
  delete)   kafka-consumer-groups --bootstrap-server "$BOOTSTRAP" --delete --group "$2" ;;
  *) echo "uso: $0 list|describe <grupo>|reset <grupo> <topico> [earliest|latest]|delete <grupo>"; exit 1 ;;
esac
