// Producción: build servido desde Azure Storage Static Website (stfleetopseft6813), consumiendo APIM.
export const environment = {
  production: true,
  entra: {
    tenantId: '72fd0b5a-8a6a-4cff-89f6-bde961f7e250',
    spaClientId: '5c766c26-af69-44d1-a8db-04c1f5d41049',
    // scope delegado expuesto por el registro fleetops-api
    apiScope: 'api://30c9b696-b377-4418-9a14-59449389b268/api.access',
    redirectUri: 'https://stfleetopseft6813.z15.web.core.windows.net/',
  },
  // Todas las llamadas pasan por Azure API Management (valida el JWT antes del backend)
  api: {
    flota: 'https://apim-fleetops-eft-5276.azure-api.net/flota',
    monitoreo: 'https://apim-fleetops-eft-5276.azure-api.net/fleetops-monitoreo',
    adminKafka: 'https://apim-fleetops-eft-5276.azure-api.net/fleetops-admin-kafka',
    adminRabbit: 'https://apim-fleetops-eft-5276.azure-api.net/fleetops-admin-rabbitmq',
  },
};
