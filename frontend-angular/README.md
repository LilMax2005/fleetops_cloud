# FleetOps · Frontend (Angular 22)

SPA Angular standalone que inicia sesión con **Microsoft Entra ID** (MSAL Angular, Authorization Code Flow + PKCE)
y consume los microservicios a través de **Azure API Management** enviando el JWT en `Authorization: Bearer`.

## Vistas
| Ruta | Vista | Backend |
|---|---|---|
| `#/login` | Inicio de sesión con Microsoft | Entra ID |
| `#/` | Panel: viajes activos, detalle y alertas | servicio-flota |
| `#/viajes` | Crear viajes y cambiar estado (publica eventos Kafka) | servicio-flota |
| `#/flota` | Vehículos y simulador de telemetría GPS (Kafka) | servicio-flota |
| `#/analitica` | KPIs en streaming, refresco cada 5 s | servicio-monitoreo |
| `#/admin/kafka` | Tópicos, configuraciones, particiones y consumer groups | admin-kafka (rol FleetOps.Admin) |
| `#/admin/rabbitmq` | Nodos, colas, exchanges, bindings y prueba de DLQ | admin-rabbitmq (rol FleetOps.Admin) |

## Estructura
```
src/app/
  core/auth      msal.config.ts (instancia, guard e interceptor MSAL), auth.service.ts (signals de sesión), auth.guard.ts
  core/api       servicios HTTP por microservicio + error-message.ts
  core/models    interfaces TypeScript de los DTO
  layout         shell con barra lateral
  features/*     un componente standalone por vista (carga diferida)
```

## Configuración
Editar `src/environments/environment.ts` (producción) y `environment.development.ts` (local):
`tenantId`, `spaClientId` (fleetops-spa), `apiScope` (`api://<client-id fleetops-api>/api.access`), `redirectUri`
y las URLs de APIM. `MsalInterceptor` agrega el token a toda llamada hacia esas URLs.

En el registro `fleetops-spa` agregar como URI de redirección SPA `http://localhost:4200/` y `https://stfleetopsweb.z1.web.core.windows.net/`.

## Comandos
```bash
npm install
npm start                 # http://localhost:4200 (usa environment.development.ts)
npm run build             # dist/fleetops-frontend/browser
az storage blob upload-batch -s dist/fleetops-frontend/browser -d '$web' --account-name stfleetopsweb --overwrite
```
