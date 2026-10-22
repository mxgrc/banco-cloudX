// Configuración pública local. Cambiar issuer y destino API al preparar AWS.
export const environment = {
  apiBase: '/api',
  keycloakUrl: 'http://localhost:8080',
  realm: 'banco-cloud',
  clientId: 'banco-cloud-frontend',
  audience: 'banco-cloud-api',
} as const;
