# Sistema de Avaliações

API de avaliações entre pessoas de uma empresa, com front-end em React. Todas as rotas são autenticadas pelo **Auth0**.

- Front-end: https://sistema-avaliacoes.vercel.app
- API: http://ec2-44-197-175-105.compute-1.amazonaws.com:8080/avaliacoes
- Auth0: tenant `dev-luk1185ok6ie6gvy.us.auth0.com`, API `https://avaliacoes-api`, aplicação SPA `Avaliacoes Front`

| Parte | Tecnologia | Onde roda |
| --- | --- | --- |
| Back-end | Spring Boot 4 (Java 21) + PostgreSQL | EC2 da AWS, via Docker Compose e deploy automático pelo GitHub Actions |
| Front-end | React + Vite + `@auth0/auth0-react` | Vercel |
| Login e roles | Auth0 (roles `ADMIN` e `USER`) | Auth0 |

```
Navegador ──login──▶ Auth0 (devolve access token com as roles)
    │
    │ HTTPS  /api/avaliacoes  + Authorization: Bearer <token>
    ▼
Vercel (front) ──rewrite /api/* ──▶ EC2 :8080 (Spring Boot) ──▶ PostgreSQL (container)
```

O front chama `/api/...` no próprio domínio da Vercel, e o `vercel.json` repassa a chamada para a EC2. Isso evita o bloqueio de *mixed content*: uma página HTTPS não pode chamar uma API HTTP direto, e a EC2 só tem HTTP. Também evita CORS em produção.

## Rotas

| Método | Rota | Quem pode | Resposta |
| --- | --- | --- | --- |
| `POST` | `/avaliacoes` | `ADMIN` | `201` com a avaliação criada |
| `GET` | `/avaliacoes` | `ADMIN` e `USER` | `200` com a lista (mais recentes primeiro) |

Sem token, a API responde `401`. Com um token sem a role necessária, responde `403`. Dados inválidos dão `400` com os campos problemáticos.

### Entidade `Avaliacao`

| Campo | Tipo | Regra |
| --- | --- | --- |
| `id` | Long | gerado pelo banco |
| `emailAvaliador` | String | vem do token (claim `https://avaliacoes-api/email`); se o token não tiver email, usa o do corpo |
| `emailAvaliado` | String | obrigatório, email válido, diferente do avaliador |
| `comentarios` | String | obrigatório, até 2000 caracteres |
| `nota` | Integer | obrigatória, de 0 a 10 |
| `dataAvaliacao` | LocalDate | opcional; se não vier, usa a data de hoje; não pode ser futura |

Exemplo de corpo do `POST /avaliacoes`:

```json
{
  "emailAvaliado": "maria@empresa.com",
  "comentarios": "Ótima colaboração no projeto",
  "nota": 9,
  "dataAvaliacao": "2026-11-10"
}
```

## Como a autenticação funciona

1. O front faz login com o Auth0 pedindo o `audience` da API (`https://avaliacoes-api`). Por isso o Auth0 devolve um **access token JWT** para a API.
2. Uma **Action de Post Login** no Auth0 copia as roles do usuário para o token, na claim `https://avaliacoes-api/roles`, e o email na claim `https://avaliacoes-api/email`. As claims customizadas precisam de um namespace em forma de URL.
3. A API é um **OAuth2 Resource Server** (`spring-boot-starter-security-oauth2-resource-server`). Ela valida a assinatura do token com as chaves públicas do Auth0, o emissor (`issuer-uri`) e o `audience`.
4. `Auth0RolesConverter` transforma `["ADMIN"]` da claim em `ROLE_ADMIN`, e `SecurityConfig` aplica as regras: `POST /avaliacoes` exige `hasRole("ADMIN")` e `GET /avaliacoes` exige `hasAnyRole("ADMIN", "USER")`.
5. O front lê a mesma claim do access token para decidir se mostra o formulário de cadastro, que só aparece para `ADMIN`. Quem garante a regra de verdade é a API.

Arquivos principais do back-end, em `backend/src/main/java/br/edu/insper/avaliacoes/`:

- `SecurityConfig.java`: regras de acesso por role, resource server JWT e CORS.
- `Auth0RolesConverter.java`: converte a claim de roles do Auth0 em authorities do Spring.
- `AvaliacaoController.java`, `AvaliacaoService.java`, `AvaliacaoRepository.java` e `Avaliacao.java`: rotas, regras, acesso ao banco e entidade.
- `AvaliacaoRequest.java`: corpo do POST, com Bean Validation (nota 0–10, emails válidos).
- `ApiExceptionHandler.java`: transforma erros de validação e de regra em respostas `400`.

## Configuração do Auth0

1. **Applications → APIs → Create API**: Name `Avaliacoes API`, Identifier `https://avaliacoes-api`, algoritmo RS256. Em *Settings*, ligue **Allow Offline Access**, para o front poder usar refresh tokens.
2. **Applications → Applications → Create Application**: `Avaliacoes Front`, tipo **Single Page Application**. Na aba *URLs*, preencha *Allowed Callback URLs*, *Allowed Logout URLs* e *Allowed Web Origins* com `http://localhost:5173` e a URL da Vercel.
   - Se a API usar a política **Per-app authorization**, o padrão em tenants novos, abra a aplicação → aba **API Access** → `Avaliacoes API` → **Grant Access** em *User-Delegated Access*. Sem isso, o Auth0 não entrega token para a API.
3. **User Management → Roles**: crie `ADMIN` e `USER`.
4. **User Management → Users**: crie os usuários e atribua as roles em cada um, na aba *Roles*.
5. **Actions → Library → Create Action**, trigger *Login / Post Login*, com o código abaixo. Depois clique em **Deploy** e, em **Actions → Triggers → post-login**, coloque a action no fluxo.

```js
exports.onExecutePostLogin = async (event, api) => {
  const namespace = 'https://avaliacoes-api';
  const roles = (event.authorization && event.authorization.roles) || [];
  api.accessToken.setCustomClaim(`${namespace}/roles`, roles);
  api.idToken.setCustomClaim(`${namespace}/roles`, roles);
  if (event.user.email) {
    api.accessToken.setCustomClaim(`${namespace}/email`, event.user.email);
  }
};
```

As roles só entram em um token **novo**. Depois de mudar a role de alguém, clique em **Atualizar** no front, ou em "Já recebi uma role: atualizar permissões". Esses botões chamam `getAccessTokenSilently({ cacheMode: 'off' })`, que ignora o token em cache e pede outro ao Auth0. Sair e entrar de novo também funciona.

### Testes feitos em produção

| Cenário | Resultado |
| --- | --- |
| `GET /avaliacoes` sem token | `401` |
| Login sem role | Front mostra "Sem permissão" |
| ADMIN cadastra pelo front | `201`, avaliador = email do token |
| ADMIN e USER listam | `200` |
| USER faz `POST /avaliacoes` direto na API | `403` |
| USER no front | Formulário de cadastro não aparece |

## Rodar localmente

Back-end (precisa de Docker):

```powershell
cd backend
$env:DB_PASSWORD="senha-local"; $env:AUTH0_DOMAIN="SEU-TENANT.us.auth0.com"
docker compose up --build
```

Front-end:

```powershell
cd frontend
copy .env.example .env.local   # e preencha VITE_AUTH0_DOMAIN e VITE_AUTH0_CLIENT_ID
npm install
npm run dev                    # http://localhost:5173 (o /api vai para http://localhost:8080)
```

Testes do back-end, que usam H2 em memória e tokens simulados com `spring-security-test`:

```powershell
cd backend
mvn verify
```

## Deploy do back-end (GitHub Actions → EC2)

O workflow `.github/workflows/backend.yml` roda a cada push na `main` que mexa em `backend/`:

1. **Testes**: `mvn verify`.
2. **Build da imagem**: constrói o `backend/Dockerfile` e publica no GitHub Container Registry (`ghcr.io/<repo>/api`). A EC2 tem só 2 GB de RAM, então o Maven roda no GitHub, não na máquina.
3. **Deploy**: copia o `docker-compose.yml` para `/home/ubuntu/avaliacoes` na EC2, escreve o `.env` com os secrets, faz `docker compose pull` e `docker compose up -d`.
4. **Verificação**: chama `GET /avaliacoes` sem token e espera `401`, o que prova que a API subiu e está protegida.

Configuração do repositório, em **Settings → Secrets and variables → Actions**:

| Tipo | Nome | Valor |
| --- | --- | --- |
| Secret | `EC2_HOST` | `ec2-44-197-175-105.compute-1.amazonaws.com` |
| Secret | `EC2_SSH_KEY` | conteúdo do arquivo `.pem` |
| Secret | `DB_PASSWORD` | senha do PostgreSQL |
| Variable | `AUTH0_DOMAIN` | domínio do tenant, ex.: `dev-xxxx.us.auth0.com` |
| Variable | `AUTH0_AUDIENCE` | `https://avaliacoes-api` |
| Variable | `CORS_ALLOWED_ORIGINS` | URL do front na Vercel |

API publicada: `http://ec2-44-197-175-105.compute-1.amazonaws.com:8080/avaliacoes`

## Deploy do front-end (Vercel)

O diretório do projeto na Vercel é `frontend/`. As variáveis de ambiente de produção são `VITE_AUTH0_DOMAIN`, `VITE_AUTH0_CLIENT_ID` e `VITE_AUTH0_AUDIENCE`. Para publicar pela CLI:

```powershell
cd frontend
npx vercel login
npx vercel --prod
```

O `frontend/vercel.json` faz o rewrite de `/api/*` para a API na EC2.

## Testando com curl

```bash
# sem token: 401
curl -i http://ec2-44-197-175-105.compute-1.amazonaws.com:8080/avaliacoes

# com token (copie o access token do login)
curl -i -H "Authorization: Bearer $TOKEN" http://ec2-44-197-175-105.compute-1.amazonaws.com:8080/avaliacoes
```
