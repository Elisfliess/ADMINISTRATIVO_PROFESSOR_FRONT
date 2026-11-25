# Compasso

Plataforma web de apoio à gestão de aulas particulares de música, com área administrativa para o professor e página pública de divulgação.

> **Status:** em desenvolvimento. A especificação do MVP está consolidada e a implementação técnica está em andamento.

## Fontes de verdade

- **Notion:** problema, comunidade, MVP, requisitos, regras de negócio, decisões de produto, UX e acompanhamento acadêmico.
- **Este repositório:** código-fonte, contrato OpenAPI, arquitetura técnica, SPECs de módulos, testes, instruções de desenvolvimento e automações.
- **Regra de sincronização:** uma decisão de produto aprovada no Notion orienta a implementação; alterações de contrato, arquitetura ou comportamento técnico devem ser refletidas neste repositório na mesma PR. Não trate itens especificados no OpenAPI/SPEC como implementados sem evidência no código e nos testes.

## Estrutura

```text
.
├── .github/                 # Workflows e modelos de contribuição
├── .ai/                     # Contexto e regras para assistentes de IA
├── docs/                    # Documentação técnica do software
├── frontend/                # Aplicação React + TypeScript (fundação inicial)
├── backend/                 # API Spring Boot
└── infrastructure/          # Área reservada para configurações de infraestrutura
```

## Começando

Consulte o [guia de Setup Local](docs/SETUP.md) para preparar o ambiente e executar o backend pelo terminal, inclusive em Windows sem acesso de administrador. O [README do backend](backend/README.md) contém instruções específicas da API e do banco. Para o frontend, consulte [frontend/README.md](frontend/README.md).

Outras referências:
- [Contribuição](CONTRIBUTING.md)
- [Segurança](SECURITY.md)
- [Contexto para IAs](AGENTS.md)
- [Stack tecnológica](docs/development/TECH-STACK.md)
- [Preparação de CI](docs/development/CI-SETUP.md)
- [Ordem de desenvolvimento do backend](docs/development/DEVELOPMENT-ORDER.md)
- [Contrato HTTP OpenAPI 3.1](docs/api/openapi.yaml)
- [SPECs dos módulos](docs/specs/)

## Estado da implementação

### Backend

A API utiliza Java 21, Spring Boot 4.1.2, Spring Data JPA/Hibernate, PostgreSQL 17 e Flyway, organizada como monólito modular.

Implementado na `main`:
- Fundação compartilhada, autenticação stateful, confirmação de e-mail, recuperação de senha e aceite legal.
- Consulta do perfil do professor.
- Núcleo cadastral de alunos, reativação, exportação CSV e anonimização.
- Núcleo financeiro: planos, cobranças, geração mensal, serviço público de geração por aula, baixa/cancelamento e exportação CSV.

Ainda não implementado ou incompleto:
- Agenda e integração transacional completa entre Agenda e Financeiro.
- Inativação e histórico de aulas de alunos.
- Edição do perfil do professor.
- Página pública e analytics.
- Exportação/encerramento da conta do titular e integração com provedor real de e-mail.

Consulte [backend/README.md](backend/README.md) para a lista detalhada de operações implementadas e pendências.

### Frontend

A fundação React 19, TypeScript, Vite e Tailwind CSS está no repositório, com TypeScript strict, ferramentas de qualidade, testes e CI. O cliente HTTP centralizado está preparado para sessão por cookies, CSRF e erros Problem Details.

**As telas e jornadas funcionais do produto ainda não estão implementadas.** A existência do cliente HTTP e da configuração de testes não equivale a uma interface MVP concluída. Consulte [frontend/README.md](frontend/README.md).

### Banco de dados e contrato

- PostgreSQL 17 é a versão de referência; os testes de integração usam PostgreSQL 17.11 via Testcontainers.
- Flyway contém migrations V1 a V5. A última confirmação disponível do Supabase Dev registra V1 e V2 aplicadas; a aplicação de V3 a V5 no banco compartilhado precisa ser verificada, não presumida.
- O contrato HTTP do MVP está especificado em [OpenAPI 3.1](docs/api/openapi.yaml), mas parte dos endpoints ainda não possui implementação.

## Integração recente

- PR #8: fundação, autenticação e professor inicial.
- PR #9: fundação React + TypeScript.
- PRs #11 e #12: núcleo cadastral e operações de dados de alunos.
- PR #17: núcleo financeiro e sexta rodada de correções, integrado à `main`.

No commit de referência atual (`703ca21`), o workflow [Backend CI](https://github.com/IsaqueCarlos28/Compasso/actions/runs/43964854735) concluiu com sucesso. A descrição do PR #17 também registra 157/157 testes backend aprovados localmente em PostgreSQL 17.11/Testcontainers.

## Próximas entregas técnicas

1. Implementar o módulo Agenda.
2. Integrar Agenda e Financeiro nos fluxos de criação/recorrência, remarcação e cancelamento.
3. Completar operações de aluno dependentes da Agenda.
4. Desenvolver as jornadas funcionais do frontend.
5. Implementar página pública e analytics.
6. Realizar integração ponta a ponta, validação de segurança, acessibilidade e regressão.

Não publicar percentual global de conclusão sem definir uma métrica de cobertura funcional. Agenda, página pública e telas funcionais ainda são entregas pendentes.

## Planejamento

A sequência e as dependências de implementação ficam em [docs/development/DEVELOPMENT-ORDER.md](docs/development/DEVELOPMENT-ORDER.md). O andamento detalhado de produto e acadêmico permanece no Notion.