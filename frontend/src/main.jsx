import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { Auth0Provider } from '@auth0/auth0-react'
import App from './App.jsx'
import { config } from './config.js'
import './styles.css'

function ConfiguracaoAusente() {
  return (
    <main className="center">
      <div className="card narrow">
        <h1>Configuração do Auth0 ausente</h1>
        <p className="muted">
          Defina <code>VITE_AUTH0_DOMAIN</code> e <code>VITE_AUTH0_CLIENT_ID</code> no arquivo{' '}
          <code>.env.local</code> (ou nas variáveis de ambiente da Vercel) e gere o build de novo.
        </p>
      </div>
    </main>
  )
}

createRoot(document.getElementById('root')).render(
  <StrictMode>
    {config.domain && config.clientId ? (
      <Auth0Provider
        domain={config.domain}
        clientId={config.clientId}
        authorizationParams={{
          redirect_uri: window.location.origin,
          audience: config.audience,
        }}
        cacheLocation="localstorage"
        useRefreshTokens
        useRefreshTokensFallback
      >
        <App />
      </Auth0Provider>
    ) : (
      <ConfiguracaoAusente />
    )}
  </StrictMode>,
)
