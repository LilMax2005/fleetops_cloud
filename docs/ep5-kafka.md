# EP5: Apache Kafka, administración y frontend Angular

Esta entrega agrega streaming con Apache Kafka sobre la plataforma existente (servicio-flota, ms-alerta,
simulador y RabbitMQ en clúster) sin reemplazar la mensajería RabbitMQ.

## Qué se agregó

| Componente | Ruta | Puerto | Rol |
| --- | --- | --- | --- |
| Productor Kafka en servicio-flota | `backend/servicios/servicio-flota/.../messaging/kafka` | 8080 | Publica `TripStreamEvent` y `TelemetryStreamEvent` después del commit |
| servicio-monitoreo | `backend/servicios/servicio-monitoreo` | 8084 | Consumidor Kafka: métricas por vehículo y viaje, idempotente, con DLT |
| admin-kafka | `backend/servicios/admin-kafka` | 8083 | API de administración de tópicos, particiones, configs y consumer groups (AdminClient) |
| admin-rabbitmq | `backend/servicios/admin-rabbitmq` | 8082 | API de administración de colas, exchanges y bindings (AmqpAdmin + Management API) |
| fleetops-common | `backend/servicios/fleetops-common` | — | Contratos de eventos, validación, manejo de errores Kafka (DLT) y seguridad Entra ID |
| Kafka + Zookeeper | `platform/local/kafka` | 9092-9094 | 3 Zookeeper + 3 brokers, RF 3, min ISR 2, creación controlada de tópicos |
| Kafka UI | `platform/local/kafka-ui` | 8090 | Consola web con login |
| Frontend Angular | `frontend-angular` | 4200 | MSAL (Entra ID), panel, incidentes, telemetría, analítica y consolas admin |
| Azure | `infra/azure` | — | Política APIM (`validate-azure-ad-token`) y despliegue a Container Apps |

Los servicios originales (Spring Boot 4.1) siguen siendo proyectos Maven independientes. Los módulos EP5
(Spring Boot 3.5) se construyen juntos desde `backend/servicios/pom.xml`:

```bash
cd backend/servicios && ./mvnw verify          # fleetops-common, servicio-monitoreo, admin-kafka, admin-rabbitmq
cd backend/servicios/servicio-flota && ./mvnw test
```

## Flujo de eventos

```
POST /api/v1/telemetry ─► RabbitMQ fleetops.telemetry.queue ─► servicio-flota (posición + alertas SPEEDING/LOW_FUEL ─► ms-alerta)
                                                                     │ after commit
                                                                     ▼
                                                    Kafka fleetops.telemetria.gps (6 particiones, key = vehicleId)
                                                                     │ group monitoreo-telemetria (3 hilos)
                                                                     ▼
                                                               servicio-monitoreo ─► /api/v1/analytics/*

POST /api/v1/incidents, POST /api/v1/trips/demo, POST /api/v1/trips/{id}/complete
      ─► Kafka fleetops.viajes.eventos (3 particiones, key = tripId) ─► group monitoreo-viajes
```

Mensajes inválidos o que fallan tras 3 intentos (backoff exponencial) van a `<tópico>.DLT` y los audita el
grupo `monitoreo-dlt-auditoria`.

| Tópico | Particiones | Retención | Notas |
| --- | --- | --- | --- |
| `fleetops.viajes.eventos` | 3 | 7 días | min.insync.replicas 2 |
| `fleetops.telemetria.gps` | 6 | 24 h | segment.ms 1 h, lz4 |
| `*.DLT` | 1 | 14 días | |

Productor: `acks=all`, idempotencia, lz4, `max.block.ms=5000` (si Kafka cae, flota sigue operando con RabbitMQ).
Consumidor: `read_committed`, commit manual por registro, `CooperativeStickyAssignor`.

## Levantar en local

```bash
cp platform/local/kafka/.env.example platform/local/kafka/.env
cp platform/local/kafka-ui/.env.example platform/local/kafka-ui/.env
make ep5-up        # Kafka + Kafka UI + plataforma original con Kafka activo + módulos EP5
make ep5-down && make kafka-down
```

Sin el override EP5 (`make local-up`), servicio-flota arranca con `FLEETOPS_KAFKA_ENABLED=false` y se comporta
igual que antes.

Frontend: `cd frontend-angular && npm ci && npm start` (configurar tenant y client ids en `src/environments`).

## Azure

`infra/azure/deploy-container-apps.sh` construye las imágenes en ACR y despliega los cuatro servicios en
Container Apps. Las APIs se publican detrás de API Management con `infra/azure/apim-policy.xml`, que valida el
JWT de Entra ID (audiencia y scope `api.access`) antes de llegar al backend.
