export const config = {
  domain: import.meta.env.VITE_AUTH0_DOMAIN,
  clientId: import.meta.env.VITE_AUTH0_CLIENT_ID,
  audience: import.meta.env.VITE_AUTH0_AUDIENCE || 'https://avaliacoes-api',
  claimsNamespace: import.meta.env.VITE_AUTH0_CLAIMS_NAMESPACE || 'https://avaliacoes-api',
  apiUrl: import.meta.env.VITE_API_URL || '/api',
}
