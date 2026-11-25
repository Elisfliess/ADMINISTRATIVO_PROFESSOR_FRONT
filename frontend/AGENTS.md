# Instruções para agentes — Frontend Compasso

Antes de alterar o frontend, leia o `AGENTS.md` da raiz do repositório, `.ai/PROJECT_CONTEXT.md`, `.ai/DEVELOPMENT_RULES.md`, `docs/development/TECH-STACK.md`, `docs/api/README.md` e `docs/api/openapi.yaml` quando a tarefa envolver integração HTTP.

## Stack e comandos

- React + TypeScript com `strict: true`.
- Vite **8.3.8**; Node.js 22 LTS, mínimo **22.22.2**.
- npm é o único gerenciador de pacotes aprovado (linha 10.x).
- `npm run dev`: desenvolvimento.
- `npm run typecheck`: verificação TypeScript.
- `npm run lint`: ESLint.
- `npm run test:run`: testes unitários/componentes.
- `npm run build`: typecheck e build de produção.
- `npm run test:e2e`: testes Playwright (requer browsers instalados).

## Arquitetura

- Mantenha componentes pequenos, coesos e tipados.
- Não crie Context Providers até existir estado realmente compartilhado.
- Não introduza React Router até os fluxos de navegação exigirem rotas.
- Centralize chamadas HTTP em `src/services/api`; não espalhe `fetch` pelos componentes.
- Utilize `credentials: 'include'` e a sessão HTTP do backend; não implemente JWT/Bearer.
- Requisições mutáveis devem enviar `X-XSRF-TOKEN` com o valor do cookie `XSRF-TOKEN`, quando o cookie estiver disponível.
- Trate erros `application/problem+json` conforme RFC 9457 e `docs/api/README.md`.
- Não duplique regras de negócio do backend. Validações no frontend são apoio de UX.
- Alinhe DTOs e contratos ao `docs/api/openapi.yaml`; não invente endpoints.
- Não adicione bibliotecas sem necessidade demonstrável e justificativa.
- Considere acessibilidade, responsividade e privacidade desde o início.

## Qualidade e entrega

- Adicione testes para comportamento novo e fluxos críticos.
- Não ignore erros de TypeScript, lint ou testes.
- Não inclua dados reais, segredos ou arquivos `.env`.
- Faça mudanças pequenas e relacionadas; siga Conventional Commits e GitHub Flow.
- Não afirme que testes foram executados sem realmente executá-los.
