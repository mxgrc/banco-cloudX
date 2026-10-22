// Prueba local contra EC2. Actualizar si cambia la IP pública de la instancia.
export const environment = {
  apiBase: '/api',
  keycloakUrl: 'https://auth.54-172-114-153.sslip.io',
  realm: 'banco-cloud',
  clientId: 'banco-cloud-frontend',
  audience: 'banco-cloud-api',
} as const;
