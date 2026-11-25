# Plano de CI/CD

## Estado

- **Backend:** workflow implementado em `.github/workflows/backend-ci.yml`.
- **Frontend:** workflow implementado em `.github/workflows/frontend-ci.yml`; execução validada com sucesso no PR #9 (run #6).

## Backend — `backend-ci.yml`

O workflow do Backend:
- Executa em Pull Requests para `main` e em pushes para `main`, quando há alterações em `backend/**`, `docs/api/**` ou no próprio workflow.
- Configura Java 21 (Temurin).
- Usa o Maven Wrapper versionado no repositório.
- Executa `./mvnw --batch-mode --no-transfer-progress verify`.
- Executa testes de integração com PostgreSQL 17.11 efêmero via Testcontainers. PostgreSQL 16 não faz parte da matriz atual e não deve ser descrito como compatibilidade verificada.
- Não acessa o Supabase compartilhado e não depende de credenciais externas.
- Declara permissões mínimas de leitura do conteúdo do repositório.
- Usa concorrência por referência para cancelar execuções antigas da mesma branch.

A CI deve falhar se compilação, migrations ou testes falharem. Não adicionar passos que apenas simulem sucesso.

## Frontend — `frontend-ci.yml`

O frontend utiliza:
- Node.js 22 LTS, mínimo **22.22.2**, recomendado **22.23.5** (fixado em `frontend/.nvmrc`).
- npm 10.x e `frontend/package-lock.json`.
- `npm ci` para instalação reproduzível.
- `npm run typecheck` para verificação TypeScript.
- `npm run lint` para ESLint.
- `npm run test:run` para testes unitários/componentes com Vitest.
- `npm run build` para build de produção.
- `npx playwright install --with-deps chromium` e `npm run test:e2e` para testes E2E.

O workflow executa em Pull Requests para `main` e pushes para `main`, com filtros para alterações no frontend, documentação de stack/CI e o próprio workflow. O job usa somente permissões de leitura e instala dependências a partir do lockfile versionado.

## Segurança e dependências

- Ativar verificações disponíveis no plano GitHub atual.
- Manter Dependabot para GitHub Actions e adicionar ecossistemas de aplicação quando os manifestos existirem.
- Fixar Actions de terceiros em SHA completo quando viável.
- Declarar permissões mínimas por workflow (`permissions` explícitas).
- Não imprimir segredos nem payloads sensíveis.
- Não usar `pull_request_target` para executar código não confiável.
- Não conceder `write-all`.
- PostgreSQL de CI deve ser efêmero e conter somente dados fictícios.

## Pendência não bloqueante

- Confirmar a versão de Node.js instalada nos computadores da faculdade. Ambientes abaixo de 22.22.2 devem atualizar para uma versão compatível da linha 22 LTS.

Antes de cada merge, revisar os checks aplicáveis e exigir CI verde, além da revisão humana.

## GitHub Free / repositório privado

A proteção técnica de `main` pode não estar disponível no plano atual. Até eventual mudança de plano, o processo depende de revisão humana, CI verde e disciplina de não fazer push direto.
