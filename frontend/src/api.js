import { config } from './config.js'

async function apiFetch(path, token, options = {}) {
  const resposta = await fetch(`${config.apiUrl}${path}`, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`,
      ...options.headers,
    },
  })

  const texto = await resposta.text()
  let dados = null
  try {
    dados = texto ? JSON.parse(texto) : null
  } catch {
    dados = null
  }

  if (!resposta.ok) {
    const erro = new Error(mensagemDeErro(resposta.status, dados))
    erro.status = resposta.status
    erro.campos = dados?.campos ?? {}
    throw erro
  }
  return dados
}

function mensagemDeErro(status, dados) {
  if (status === 401) return 'Token inválido ou expirado. Saia e faça login novamente.'
  if (status === 403) return 'Seu usuário não tem permissão para esta ação.'
  if (dados?.error) return dados.error
  if (status >= 500) return 'A API está indisponível no momento.'
  return `Erro inesperado (HTTP ${status}).`
}

export function listarAvaliacoes(token) {
  return apiFetch('/avaliacoes', token)
}

export function cadastrarAvaliacao(token, avaliacao) {
  return apiFetch('/avaliacoes', token, { method: 'POST', body: JSON.stringify(avaliacao) })
}

/** Lê as roles da claim customizada do access token (a mesma que a API usa). */
export function rolesDoToken(token) {
  try {
    const base64 = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')
    const payload = JSON.parse(atob(base64))
    const roles = payload[`${config.claimsNamespace}/roles`]
    return Array.isArray(roles) ? roles.map((r) => String(r).toUpperCase()) : []
  } catch {
    return []
  }
}
