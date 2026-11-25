# Contexto técnico do projeto

## Identificação
- **Projeto:** Compasso — Professor Particular de Música
- **Natureza:** Projeto Integrador acadêmico.
- **Comunidade atendida:** professor particular de música.
- **Objetivo geral:** apoiar a organização da atividade de aulas particulares, conforme escopo e MVP documentados no Notion.

## Estado do projeto
O bootstrap técnico do Backend e a preparação do ambiente foram integrados à `main`. As migrations Flyway V1 e V2 foram criadas, aplicadas e validadas no Supabase Dev. A implementação dos módulos de domínio, casos de uso, autenticação e endpoints funcionais ainda está pendente; não confunda schema inicial disponível com domínio implementado. Consulte o código, as SPECs e o contrato OpenAPI para distinguir decisões aprovadas de funcionalidades já entregues.

A fundação do Frontend está sendo preparada na branch `feature/frontend-fundacao-typescript`. O frontend utiliza React + TypeScript + Vite 8, conforme `docs/development/TECH-STACK.md`. A integração dessa fundação à `main` ainda depende da revisão e do merge do PR #9.

## Stack aprovada
- **Front-end:** React com TypeScript em modo strict.
- **Build e desenvolvimento:** Vite 8.0.0.
- **Runtime do frontend:** Node.js 22 LTS, mínimo 22.22.2, recomendado 22.23.5.
- **Gerenciador do frontend:** npm 10.x.
- **Back-end:** Java 21 LTS com Spring Boot 4.1.2.
- **Build do backend:** Maven Wrapper 3.3.4 com distribuição Maven 3.9.9; empacotamento JAR.
- **Banco de dados:** PostgreSQL.
- **Banco padrão de desenvolvimento:** projeto Supabase dedicado à equipe; versão atual 17.11.
- **Conexão JDBC:** Session Pooler do Supabase, porta 5432.
- **Migrations:** Flyway.
- **Credenciais:** `compasso_runtime` para runtime e `compasso_migrator` para migrations.
- **Testes de integração do backend:** Testcontainers com PostgreSQL 17.
- **Repositório:** monorepo.
- **CI:** GitHub Actions.

O backend deve utilizar PostgreSQL convencional por JDBC e permanecer independente de recursos proprietários do Supabase. Banco local é opcional. Docker é necessário para executar localmente os testes Testcontainers, mas não para iniciar a API.

## Fonte de verdade
A página principal do projeto no Notion contém o estado atual, requisitos, decisões, UX, modelo de dados e planejamento. Consulte-a antes de responder sobre comportamento esperado do produto.

**Página do projeto:** https://app.notion.com/p/PI-Professor-particular-de-musica-3a6f51667c7c82d49d00811988e84e86

**Plano de preparação do Backend:** https://app.notion.com/p/416f51667c7c81cf8477fd251963ca88

**Decisões arquiteturais (SDA):** pasta [02_DECISOES](https://www.notion.so/405f51667c7c818bbc4ec21a5d80ecff) no Notion. Consulte também [D-014 — sessão HTTP no MVP](https://www.notion.so/419f51667c7c817ba5fdd242ffef34a8), que complementa a SDA-006.

O GitHub contém código, testes e as referências técnicas: [arquitetura](../docs/architecture/BACKEND-ARCHITECTURE.md), [contrato HTTP OpenAPI](../docs/api/openapi.yaml), [SPECs dos módulos](../docs/specs/README.md) e [ordem de desenvolvimento](../docs/development/DEVELOPMENT-ORDER.md).

## Estado das migrations
- V1 e V2 aplicadas e validadas no Supabase Dev.
- Versão atual do schema: 2, registrada pelo Flyway.
- Flyway permanece desativado no profile padrão; o profile `migration` utiliza credenciais específicas.
- O papel de runtime não possui permissão `DELETE` em `plano_pagamento` e `pagamento`, preservando registros históricos.

## Regra de atualização
Quando uma decisão técnica for aprovada, atualize a documentação técnica do repositório por Pull Request. Não copie para cá todo o conteúdo funcional do Notion.
