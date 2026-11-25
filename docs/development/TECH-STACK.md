# Stack tecnológica — Compasso

**Status:** stack principal definida; bootstrap e preparação técnica do Backend integrados à `main`. A fundação do Frontend está sendo preparada na branch `feature/frontend-fundacao-typescript`; a implementação funcional dos módulos continua pendente.

## 1. Front-end

| Item | Definição |
| --- | --- |
| Biblioteca de interface | React |
| Linguagem | TypeScript com `strict: true` |
| Ferramenta de desenvolvimento/build | Vite |
| Versão do Vite | **8.3.8 (fixada)** |
| Plugin React | `@vitejs/plugin-react` 6.x |
| Runtime | Node.js 22 LTS, mínimo **22.22.2**; versão recomendada **22.23.5** |
| Gerenciador de pacotes | npm 10.x |
| Estilização | Tailwind CSS 4 com plugin oficial do Vite |
| Rotas | Adicionar React Router quando os fluxos de navegação forem implementados |
| Estado compartilhado | Context API; Providers criados sob demanda |
| HTTP | `fetch` nativo por cliente centralizado |
| Testes unitários/componentes | Vitest, React Testing Library e jsdom |
| Testes E2E | Playwright |
| Qualidade | ESLint 9 + typescript-eslint + Prettier |

### Compatibilidade do Node.js

Vite 8 exige Node.js **20.19+ ou 22.12+**. O projeto padroniza a linha Node 22 LTS, com mínimo **22.22.2** para atender também aos requisitos de runtime das dependências de desenvolvimento. O arquivo `.nvmrc` fixa a versão recomendada **22.23.5**, e `frontend/package.json` declara `>=22.22.2 <23`.

**Pendência não bloqueante:** confirmar a versão instalada nos computadores da faculdade. Ambientes abaixo de 22.22.2 devem atualizar o Node dentro da linha 22 LTS; não rebaixar Vite ou dependências sem decisão técnica.

Fonte: [Documentação oficial — Vite 8](https://vite.dev/blog/announcing-vite8).

### Instalação reproduzível

- npm é o gerenciador oficial do frontend; utilizar npm 10.x.
- Versionar `package-lock.json` e utilizar `npm ci` no CI.
- Não versionar `node_modules`.
- Fixar Vite em 8.3.8; atualizações de versões devem ocorrer por PR.
- Restringir dependências de desenvolvimento e justificar novas bibliotecas.

### Convenções de arquitetura do frontend

- Componentes funcionais React com TypeScript.
- Tipos compartilhados em `src/types`; tipos específicos podem permanecer próximos ao módulo quando isso reduzir acoplamento.
- Separar apresentação, estado e acesso HTTP sem criar camadas artificiais.
- `services/api` concentra URL base, credenciais, CSRF, serialização e tratamento de erros HTTP.
- Context API somente quando houver estado realmente compartilhado; não criar Providers antecipadamente.
- Validar entradas no frontend para UX, sem substituir as validações e regras do backend.
- Não adicionar Redux, Zustand, Axios ou TanStack Query sem necessidade demonstrada.

## 2. Back-end

| Item | Definição |
| --- | --- |
| Linguagem | Java |
| Versão-base | **Java 21 LTS** |
| Framework | Spring Boot |
| Versão do Spring Boot | **4.1.2** |
| Build | Maven com Maven Wrapper 3.3.4 (distribuição Maven 3.9.9) |
| Empacotamento | JAR |
| Pacote-base | `br.com.compasso` |

As dependências devem permanecer sob o gerenciamento de versões do Spring Boot; não definir versões individuais sem justificativa.

Dependências do bootstrap:
- Spring MVC (`spring-boot-starter-webmvc`) para a API REST.
- Bean Validation (`spring-boot-starter-validation`) para validação de entradas.
- Spring Data JPA (`spring-boot-starter-data-jpa`) para persistência relacional.
- PostgreSQL JDBC Driver (`org.postgresql:postgresql`).
- Flyway Core e módulo PostgreSQL para migrations versionadas.
- Spring Boot Actuator para health checks.
- Spring Boot Test para testes.
- Testcontainers PostgreSQL e integração Spring Boot para testes de integração com PostgreSQL real.

A estratégia de autenticação stateful com Spring Security e sessão HTTP está aprovada (D-014/SDA-006), mas a dependência e os fluxos ainda precisam ser implementados no backend. O contrato OpenAPI 3.1 está definido em `docs/api/openapi.yaml` e validado por lint na CI; isso não significa que os endpoints estejam implementados. As convenções de erro também estão especificadas no contrato.

## 3. Banco de dados

| Item | Definição |
| --- | --- |
| SGBD | PostgreSQL |
| Modelo | Relacional |
| Identificadores | UUIDv7 |
| Banco padrão de desenvolvimento | Projeto Supabase exclusivo para desenvolvimento |
| Banco local | Opcional, por conta de cada integrante |
| Ambiente de testes na CI | PostgreSQL 17.11 efêmero via Testcontainers |
| Migrações | Flyway |

A aplicação acessa PostgreSQL convencional por JDBC. O Backend não deve depender de Supabase Auth, Storage, Edge Functions ou APIs proprietárias do provedor sem decisão técnica posterior.

O projeto Supabase de desenvolvimento deve ser separado de qualquer ambiente de produção. Usar somente dados fictícios. Não versionar credenciais nem usar credenciais administrativas como acesso permanente da aplicação.

A CI atual executa os testes de persistência com PostgreSQL 17.11, correspondente ao Supabase Dev. PostgreSQL 16 não faz parte da matriz atual, portanto essa compatibilidade não deve ser descrita como validada. Se a equipe ainda precisar de uma validação adicional em PostgreSQL 16 por exigência da versão vigente da A14, ela deverá ser confirmada e planejada separadamente.

O modelo de dados e as regras de integridade estão descritos na Atividade 14 do Notion. As estruturas serão implementadas por migrations Flyway e validadas com testes de integração no PostgreSQL.

UUIDv7 deverá ser gerado pela aplicação Java por biblioteca compatível, conforme a A14. Não utilizar `UUID.randomUUID()` para as chaves do domínio.

## 4. Integração contínua

- Plataforma: GitHub Actions.
- Back-end: compilação, testes unitários e testes de integração.
- Contrato OpenAPI 3.1: lint do arquivo oficial `docs/api/openapi.yaml`.
- Front-end: instalação reproduzível via `npm ci`, typecheck, lint, testes Vitest, E2E Playwright e build.
- Banco de CI: PostgreSQL temporário, com dados exclusivamente fictícios.
- Pull Requests para `main` devem passar pelas verificações antes do merge.

Não criar verificações fictícias que retornem sucesso sem executar validações reais.

## 5. Itens ainda não decididos

- Modalidade de conexão Supabase (Direct ou Session Pooler) adequada à rede de cada integrante.
- Hospedagem, ambientes e estratégia de deploy de produção.
- Momento de introdução do roteador React, conforme os fluxos de navegação reais.

## 6. Alterações nesta decisão

Atualizações de versões principais e tecnologias devem ser deliberadas, justificadas e validadas por Pull Request. Preservar a compatibilidade com Java 21 e as restrições dos computadores da equipe.
