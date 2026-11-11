import { useCallback, useEffect, useMemo, useState } from 'react'
import { useAuth0 } from '@auth0/auth0-react'
import { cadastrarAvaliacao, listarAvaliacoes, rolesDoToken } from './api.js'

const hoje = () => new Date().toLocaleDateString('sv-SE') // AAAA-MM-DD no fuso local
const formatarData = (iso) => (iso ? iso.split('-').reverse().join('/') : '')
const classeDaNota = (nota) => (nota >= 8 ? 'alta' : nota >= 5 ? 'media' : 'baixa')

export default function App() {
  const { isLoading, isAuthenticated, error, loginWithRedirect, logout, user, getAccessTokenSilently } = useAuth0()
  const [roles, setRoles] = useState([])
  const [avaliacoes, setAvaliacoes] = useState([])
  const [carregando, setCarregando] = useState(false)
  const [erro, setErro] = useState(null)

  const carregar = useCallback(async () => {
    setCarregando(true)
    setErro(null)
    try {
      const token = await getAccessTokenSilently()
      const rolesAtuais = rolesDoToken(token)
      setRoles(rolesAtuais)
      if (rolesAtuais.includes('ADMIN') || rolesAtuais.includes('USER')) {
        setAvaliacoes(await listarAvaliacoes(token))
      }
    } catch (e) {
      setErro(e.message)
    } finally {
      setCarregando(false)
    }
  }, [getAccessTokenSilently])

  useEffect(() => {
    if (isAuthenticated) carregar()
  }, [isAuthenticated, carregar])

  if (isLoading) {
    return <main className="center"><p className="muted">Carregando…</p></main>
  }

  if (!isAuthenticated) {
    return (
      <main className="center">
        <div className="card narrow login">
          <span className="logo" aria-hidden>⭐</span>
          <h1>Avaliações</h1>
          <p className="muted">Entre com sua conta da empresa para ver e registrar avaliações entre colegas.</p>
          {error && <p className="alert">{error.message}</p>}
          <button className="primary" onClick={() => loginWithRedirect()}>Entrar com Auth0</button>
        </div>
      </main>
    )
  }

  const isAdmin = roles.includes('ADMIN')
  const temAcesso = isAdmin || roles.includes('USER')

  return (
    <>
      <header className="topbar">
        <div className="brand"><span aria-hidden>⭐</span> Avaliações</div>
        <div className="user">
          <div className="user-info">
            <span className="email">{user?.email ?? user?.name}</span>
            <span className="roles">
              {roles.length ? roles.map((r) => <span key={r} className={`chip ${r.toLowerCase()}`}>{r}</span>)
                : <span className="chip">sem role</span>}
            </span>
          </div>
          <button className="ghost" onClick={() => logout({ logoutParams: { returnTo: window.location.origin } })}>Sair</button>
        </div>
      </header>

      <main className="container">
        {erro && <p className="alert">{erro}</p>}

        {!carregando && !temAcesso && !erro && (
          <div className="card">
            <h2>Sem permissão</h2>
            <p className="muted">
              Seu usuário não tem as roles <b>ADMIN</b> ou <b>USER</b> no Auth0. Peça para um administrador atribuir uma
              role e faça login novamente.
            </p>
          </div>
        )}

        {temAcesso && (
          <div className={isAdmin ? 'grid' : ''}>
            {isAdmin && (
              <AvaliacaoForm
                emailAvaliador={user?.email}
                onCadastrada={(nova) => setAvaliacoes((lista) => [nova, ...lista])}
              />
            )}
            <ListaAvaliacoes avaliacoes={avaliacoes} carregando={carregando} onAtualizar={carregar} />
          </div>
        )}
      </main>
    </>
  )
}

function AvaliacaoForm({ emailAvaliador, onCadastrada }) {
  const { getAccessTokenSilently } = useAuth0()
  const vazio = { emailAvaliado: '', nota: null, comentarios: '', dataAvaliacao: hoje() }
  const [form, setForm] = useState(vazio)
  const [enviando, setEnviando] = useState(false)
  const [erro, setErro] = useState(null)
  const [sucesso, setSucesso] = useState(false)

  const alterar = (campo) => (e) => setForm({ ...form, [campo]: e.target.value })

  async function enviar(e) {
    e.preventDefault()
    setErro(null)
    setSucesso(false)
    if (form.nota === null) {
      setErro({ mensagem: 'Escolha uma nota de 0 a 10.', campos: {} })
      return
    }
    setEnviando(true)
    try {
      const token = await getAccessTokenSilently()
      const nova = await cadastrarAvaliacao(token, { ...form, emailAvaliador })
      onCadastrada(nova)
      setForm(vazio)
      setSucesso(true)
    } catch (e2) {
      setErro({ mensagem: e2.message, campos: e2.campos ?? {} })
    } finally {
      setEnviando(false)
    }
  }

  const erroCampo = (campo) => erro?.campos?.[campo] && <span className="field-error">{erro.campos[campo]}</span>

  return (
    <form className="card form" onSubmit={enviar} noValidate>
      <h2>Nova avaliação</h2>
      <p className="muted small">Avaliador: <b>{emailAvaliador}</b></p>

      <label>
        Email do avaliado
        <input type="email" required placeholder="colega@empresa.com" value={form.emailAvaliado}
          onChange={alterar('emailAvaliado')} />
        {erroCampo('emailAvaliado')}
      </label>

      <fieldset>
        <legend>Nota</legend>
        <div className="notas" role="radiogroup">
          {Array.from({ length: 11 }, (_, n) => (
            <button key={n} type="button" role="radio" aria-checked={form.nota === n}
              className={`nota-opcao ${form.nota === n ? `ativa ${classeDaNota(n)}` : ''}`}
              onClick={() => setForm({ ...form, nota: n })}>
              {n}
            </button>
          ))}
        </div>
        {erroCampo('nota')}
      </fieldset>

      <label>
        Comentários
        <textarea rows={4} required maxLength={2000} placeholder="Pontos fortes, pontos a desenvolver…"
          value={form.comentarios} onChange={alterar('comentarios')} />
        {erroCampo('comentarios')}
      </label>

      <label>
        Data da avaliação
        <input type="date" max={hoje()} value={form.dataAvaliacao} onChange={alterar('dataAvaliacao')} />
        {erroCampo('dataAvaliacao')}
      </label>

      {erro && <p className="alert">{erro.mensagem}</p>}
      {sucesso && <p className="success">Avaliação cadastrada.</p>}

      <button className="primary" type="submit" disabled={enviando}>
        {enviando ? 'Enviando…' : 'Cadastrar avaliação'}
      </button>
    </form>
  )
}

function ListaAvaliacoes({ avaliacoes, carregando, onAtualizar }) {
  const [busca, setBusca] = useState('')

  const filtradas = useMemo(() => {
    const termo = busca.trim().toLowerCase()
    if (!termo) return avaliacoes
    return avaliacoes.filter((a) => a.emailAvaliado.includes(termo) || a.emailAvaliador.includes(termo))
  }, [avaliacoes, busca])

  const media = filtradas.length
    ? (filtradas.reduce((soma, a) => soma + a.nota, 0) / filtradas.length).toFixed(1)
    : '–'

  return (
    <section className="card">
      <div className="list-header">
        <div>
          <h2>Avaliações</h2>
          <p className="muted small">{filtradas.length} avaliação(ões) · média {media}</p>
        </div>
        <button className="ghost" onClick={onAtualizar} disabled={carregando}>
          {carregando ? 'Atualizando…' : 'Atualizar'}
        </button>
      </div>

      <input type="search" className="search" placeholder="Filtrar por email…" value={busca}
        onChange={(e) => setBusca(e.target.value)} />

      {carregando && avaliacoes.length === 0 ? (
        <p className="muted">Carregando avaliações…</p>
      ) : filtradas.length === 0 ? (
        <p className="muted empty">Nenhuma avaliação encontrada.</p>
      ) : (
        <ul className="avaliacoes">
          {filtradas.map((a) => (
            <li key={a.id} className="avaliacao">
              <div className={`nota ${classeDaNota(a.nota)}`}>{a.nota}</div>
              <div className="conteudo">
                <div className="linha">
                  <span className="avaliado">{a.emailAvaliado}</span>
                  <span className="data">{formatarData(a.dataAvaliacao)}</span>
                </div>
                <p className="comentarios">{a.comentarios}</p>
                <span className="muted small">avaliado por {a.emailAvaliador}</span>
              </div>
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}
