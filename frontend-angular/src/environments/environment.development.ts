// Desarrollo local: ng serve en :4200 y microservicios en :8080 (flota), :8082 (admin-rabbitmq), :8083 (admin-kafka) y :8084 (monitoreo).
export const environment = {
  production: false,
  entra: {
    tenantId: '72fd0b5a-8a6a-4cff-89f6-bde961f7e250',
    spaClientId: '5c766c26-af69-44d1-a8db-04c1f5d41049',
    apiScope: 'api://30c9b696-b377-4418-9a14-59449389b268/api.access',
    redirectUri: 'http://localhost:4200/',
  },
  api: {
    flota: 'http://localhost:8080',
    monitoreo: 'http://localhost:8084',
    adminRabbit: 'http://localhost:8082',
    adminKafka: 'http://localhost:8083',
  },
};
