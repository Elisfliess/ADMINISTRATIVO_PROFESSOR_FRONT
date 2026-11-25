# Estado da implementação — Compasso

**Data de referência:** 16/11/2026  
**Branch de referência:** `main`  
**Commit de referência:** `703ca21b80cbfb072fdc88939daf43d7cfe4b97f`  
**Escopo:** fotografia técnica do código e das validações disponíveis. Requisitos, MVP e decisões de produto continuam no Notion.

> Este documento é um retrato datado, não uma declaração automática de conclusão do MVP. Atualize-o quando módulos forem integrados à `main`, e não apenas quando uma SPEC ou endpoint for planejado.

## 1. Resumo por área

| Área | Estado | Evidência e limite |
|---|---|---|
| Fundação backend | Implementada | Configuração Spring, infraestrutura compartilhada, Problem Details, validação, correlação e UUIDv7. |
| Usuário/autenticação | Implementada com dependência externa pendente | Cadastro, verificação, login/logout, sessão, recuperação, reenvio e aceite legal. Provedor real de e-mail ainda não integrado; padrão `noop`. |
| Professor | Parcial | Criação coordenada no cadastro e `GET /professor`; `PATCH /professor` não implementado. |
| Aluno | Parcial | Cadastro, consulta, filtro, edição, reativação, exportação CSV e anonimização. Inativação e histórico de aulas dependem da Agenda. |
| Financeiro | Núcleo implementado | Planos, cobranças, geração mensal, geração por aula via serviço público, baixa, cancelamento e exportação CSV. |
| Agenda | Não implementada | Há pacote/estrutura e SPEC; endpoints e casos de uso ainda não estão implementados. |
| Integração Agenda/Financeiro | Pendente | O serviço de geração por aula não equivale à coordenação transacional completa de criação/recorrência, remarcação e cancelamento. |
| Página pública | Não implementada | SPEC/contrato existem; perfil público, modalidades, prévia/publicação e analytics ainda pendentes. |
| Frontend | Fundação técnica | React 19, TypeScript, Vite, Tailwind, strict mode, ferramentas de qualidade, testes e cliente HTTP. Telas e jornadas do MVP não implementadas. |

## 2. Histórico de integração relevante

| PR | Conteúdo | Situação na referência |
|---|---|---|
| #8 | Fundação, autenticação e professor inicial | Merged |
| #9 | Fundação React + TypeScript | Merged |
| #11 | Núcleo cadastral de alunos | Merged |
| #12 | Reativação, exportação e anonimização de alunos | Merged |
| #15 | Primeira integração do núcleo financeiro | Revertido posteriormente pelo PR #16 |
| #16 | Reversão intermediária do PR #15 | Merged |
| #17 | Restauração do Financeiro com sexta rodada de correções | Merged; Financeiro presente na `main` |

## 3. Financeiro — limites importantes

O controller atual expõe operações de planos por aluno (listar/criar), consulta/atualização/encerramento de plano, listagem/consulta de cobranças, baixa, cancelamento e exportação CSV. O serviço também oferece geração mensal agendada e geração de cobrança por aula para futura integração com Agenda.

O módulo Agenda ainda não implementa os fluxos que chamariam esse serviço. O Financeiro não deve ser descrito como integrado à Agenda.

A sexta rodada corrigiu a coordenação de bloqueios e a idempotência no PostgreSQL. A regra de bloqueio é Plano → Aula (quando aplicável) → Cobrança. A unicidade mensal foi reforçada pela migration V5. A linha de Aula bloqueada diretamente no Financeiro é uma proteção mínima, não substitui a integração do domínio Agenda.

## 4. Banco de dados e migrations

- PostgreSQL 17 é a versão de referência; Testcontainers utiliza PostgreSQL 17.11.
- O repositório contém V1–V5:
  - V1: schema inicial;
  - V2: integridade de planos, aulas e cobranças;
  - V3: versões legais iniciais;
  - V4: CNPJ alfanumérico;
  - V5: unicidade de cobrança mensal por plano e início de período.
- Última confirmação disponível para o Supabase Dev: V1 e V2 aplicadas/validadas.
- Aplicação de V3, V4 e V5 no banco compartilhado: **não confirmada nesta revisão**. A V5 foi validada em PostgreSQL 17.11/Testcontainers; isso não comprova aplicação no Supabase Dev.
- A A14 vigente no Notion é a versão 3.4. A V1 tem origem em uma versão anterior do modelo; a equivalência integral do schema atual com A14 v3.4 ainda precisa ser verificada e registrada.

Não executar migrations no banco compartilhado apenas porque existem no repositório. Conferir o histórico do Flyway, compatibilidade e dados existentes e seguir o procedimento autorizado descrito em `backend/README.md`.

## 5. Testes e CI

- O PR #17 registrou 39/39 testes direcionados do Financeiro e 157/157 testes da suíte backend aprovados localmente com PostgreSQL 17.11/Testcontainers.
- O workflow **Backend CI** concluiu com sucesso no commit `703ca21b80cbfb072fdc88939daf43d7cfe4b97f`: [execução #43964854735](https://github.com/IsaqueCarlos28/Compasso/actions/runs/43964854735).
- Essa evidência não significa que funcionalidades ainda não implementadas estejam cobertas. Novos módulos exigirão seus próprios testes.
- O contrato OpenAPI é a especificação HTTP; não prova, por si só, a existência dos endpoints.

## 6. Próximas entregas

1. Implementar Agenda conforme `docs/specs/agenda.md`.
2. Integrar Agenda e Financeiro com atomicidade em criação/recorrência, remarcação e cancelamento.
3. Completar inativação e histórico de alunos.
4. Desenvolver as telas/jornadas frontend consumindo o contrato.
5. Implementar página pública, publicação e analytics.
6. Integrar provedor real de e-mail e fechar topologia de hospedagem/CORS/CSRF/cookies.
7. Validar migrations e equivalência entre A14 v3.4, Flyway e banco de desenvolvimento.
8. Executar integração ponta a ponta, regressão, acessibilidade e validação com o professor.

## 7. Fontes

- [README do backend](../../backend/README.md)
- [Contrato OpenAPI](../api/openapi.yaml)
- [Ordem de desenvolvimento](DEVELOPMENT-ORDER.md)
- [Arquitetura do backend](../architecture/BACKEND-ARCHITECTURE.md)
- [SPEC Financeiro](../specs/financeiro.md)
- [SPEC Agenda](../specs/agenda.md)
- [PR #17](https://github.com/IsaqueCarlos28/Compasso/pull/17)
