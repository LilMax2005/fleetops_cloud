// Desarrollo local: ng serve en :4200 y microservicios en :8080 (flota), :8082 (admin-rabbitmq), :8083 (admin-kafka) y :8084 (monitoreo).
export const environment = {
  production: false,
  entra: {
    tenantId: 'REEMPLAZAR-TENANT-ID',
    spaClientId: 'REEMPLAZAR-CLIENT-ID-fleetops-spa',
    apiScope: 'api://REEMPLAZAR-CLIENT-ID-fleetops-api/api.access',
    redirectUri: 'http://localhost:4200/',
  },
  api: {
    flota: 'http://localhost:8080',
    monitoreo: 'http://localhost:8084',
    adminRabbit: 'http://localhost:8082',
    adminKafka: 'http://localhost:8083',
  },
};
