# Compasso — Frontend

Frontend do Compasso, construído com React, TypeScript e Vite.

## Requisitos

- Node.js **22.22.2 ou superior na linha 22.x** (a versão recomendada do projeto está registrada em `.nvmrc`).
- npm 10.x.

Confira as versões:

```bash
node --version
npm --version
```

## Instalação e execução

Na pasta `frontend`:

```bash
npm install
npm run dev
```

Após o primeiro `npm install`, mantenha `package-lock.json` versionado. Nas instalações seguintes e na CI, utilize `npm ci`.

## Scripts

| Comando                | Finalidade                     |
| ---------------------- | ------------------------------ |
| `npm run dev`          | Servidor local Vite            |
| `npm run typecheck`    | Verificação TypeScript         |
| `npm run lint`         | ESLint                         |
| `npm run test`         | Vitest em modo watch           |
| `npm run test:run`     | Vitest uma vez                 |
| `npm run build`        | Typecheck e build de produção  |
| `npm run preview`      | Servir build localmente        |
| `npm run test:e2e`     | Testes E2E Playwright          |
| `npm run format`       | Formatar arquivos com Prettier |
| `npm run format:check` | Verificar formatação           |

Para executar E2E pela primeira vez:

```bash
npx playwright install chromium
npm run build
npm run test:e2e
```

## Configuração da API

A integração HTTP é ativada explicitamente pela presença de `VITE_API_BASE_URL`. Crie `frontend/.env.local` a partir de `.env.example`:

```dotenv
VITE_API_BASE_URL=/api/v1
```

O servidor de desenvolvimento já faz proxy de `/api/v1` para `http://localhost:8080`. Para apontar diretamente ao backend, use:

```dotenv
VITE_API_BASE_URL=http://localhost:8080/api/v1
```

Ajuste a origem conforme a execução real do backend. Em produção, o mesmo caminho pode ser servido por um reverse proxy.

**Sem a variável**, a aplicação roda em modo demonstração: dados fictícios, navegação local e nenhuma chamada de rede. Isso mantém o site utilizável quando o backend não está disponível.

O cliente centralizado em `src/services/api`:

- envia cookies de sessão com `credentials: 'include'`;
- não utiliza JWT/Bearer;
- faz uma leitura prévia em `GET /auth/me` para obter o cookie `XSRF-TOKEN` antes da primeira operação mutável, e reenvia o valor no header `X-XSRF-TOKEN`;
- renova o par cookie/header após o login, como exige o backend;
- interpreta respostas de erro `application/problem+json` e exibe a mensagem ao usuário.

As variáveis com prefixo `VITE_` são públicas e nunca devem conter segredos.

### Fluxos ligados ao backend

`src/services/api/integration.ts` liga as telas aos endpoints implementados pelo backend:

- login e consulta da sessão (`/auth/login`, `/auth/me`);
- cadastro, verificação e reenvio de e-mail (`/auth/register`, `/auth/verify-email`, `/auth/verification/resend`);
- recuperação e redefinição de senha (`/auth/password-recovery*`);
- aceite legal (`/legal/acceptances`) e saída (`/auth/logout`);
- cadastro e edição de alunos (`/alunos`);
- listagem de alunos e cobranças, exportação de recebimentos e baixa/cancelamento de cobrança;
- encerramento da sessão ao sair.

Telas cujos endpoints ainda não existem no backend continuam com comportamento local: agenda, publicação da página pública, indicadores, inativação/anonimização de alunos, exportação e encerramento de conta, além da edição de dados do professor. Nenhum endpoint externo ao contrato foi inventado; `src/services/api/endpoints.ts` contém apenas caminhos de `docs/api/openapi.yaml`.

## Telas do produto

As telas do arquivo **PROFESSOR JOSÉ (Copy)** foram integradas em `src/screens`, mantendo os códigos dos frames no código e nas rotas. A aplicação abre no login (`/`) e, após o aceite dos documentos, leva à área administrativa com navegação por Agenda, Alunos, Cobranças, Página pública, Indicadores e Configurações. Códigos de frame e anotações de especificação não aparecem na interface. Detalhes de estrutura, fluxos, integração e limitações estão em [FIGMA-SCREENS.md](FIGMA-SCREENS.md).

## Documentação

- Stack e versões: `../docs/development/TECH-STACK.md`.
- Contrato HTTP: `../docs/api/openapi.yaml` e `../docs/api/README.md`.
- Instruções para agentes: `AGENTS.md` e `../AGENTS.md`.
