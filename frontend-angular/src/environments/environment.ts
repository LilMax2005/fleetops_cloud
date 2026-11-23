// Producción: build servido desde Azure Storage Static Website, consumiendo APIM.
export const environment = {
  production: true,
  entra: {
    tenantId: 'REEMPLAZAR-TENANT-ID',
    spaClientId: 'REEMPLAZAR-CLIENT-ID-fleetops-spa',
    // scope delegado expuesto por el registro fleetops-api
    apiScope: 'api://REEMPLAZAR-CLIENT-ID-fleetops-api/api.access',
    redirectUri: 'https://stfleetopsweb.z1.web.core.windows.net/',
  },
  // Todas las llamadas pasan por Azure API Management (valida el JWT antes del backend)
  api: {
    flota: 'https://apim-municipalidad-mj.azure-api.net/fleetops',
    monitoreo: 'https://apim-municipalidad-mj.azure-api.net/fleetops-monitoreo',
    adminKafka: 'https://apim-municipalidad-mj.azure-api.net/fleetops-admin-kafka',
    adminRabbit: 'https://apim-municipalidad-mj.azure-api.net/fleetops-admin-rabbitmq',
  },
};
