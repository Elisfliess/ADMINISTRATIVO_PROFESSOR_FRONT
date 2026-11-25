# Ordem de desenvolvimento do Backend — Compasso

**Status:** ordem de desenvolvimento aprovada para o MVP.  
**Base:** arquitetura aprovada em 14/11/2026, D-012, D-013, D-014, RF01–RF61 e SPECs desta pasta.

**Acompanhamento de implementação:** a ordem abaixo representa dependências e sequência recomendada, não o progresso executado. Para o estado real por módulo, migrations, PRs e CI, consulte [IMPLEMENTATION-STATUS.md](IMPLEMENTATION-STATUS.md).

## Princípios

- Implementar o menor fluxo vertical funcional sem antecipar evoluções futuras.
- Respeitar dependências entre módulos; comunicação por Services públicos.
- Casos de uso transversais ficam em `application`.
- Contrato OpenAPI e SPECs são atualizados na mesma PR quando houver mudança aprovada.
- Testes de persistência usam PostgreSQL por Testcontainers; não H2.
- Não confundir decisão documentada com funcionalidade implementada.

## Sequência recomendada

| Ordem | Etapa | Entrega principal | Dependências |
| --- | --- | --- | --- |
| 1 | Fundação e compartilhados | Configuração comum, Problem Details, validação, correlação, segurança base, sessão e infraestrutura de testes | Nenhuma |
| 2 | Usuário | Cadastro, verificação de e-mail, login/logout, recuperação, `/auth/me` e aceite legal | Fundação; coordenação inicial com professor |
| 3 | Professor | Consulta e edição do perfil cadastral do professor | Usuário |
| 4 | Aluno | Cadastro, consulta, busca, edição, histórico, inativação/reativação e operações legais de dados | Usuário e professor; application para inativação |
| 5 | Financeiro — núcleo | Planos, cobranças, estados, baixa, cancelamento, atraso e testes de integridade | Aluno e professor |
| 6 | Agenda | Aulas, recorrência, edição, remarcação, cancelamento e status | Aluno, professor e financeiro |
| 7 | Casos transversais / application | Consolidação de cadastro, inativação, recorrência e operações atômicas entre módulos | Conforme fluxos necessários nas etapas anteriores |
| 8 | Página pública | Perfil, modalidades, prévia/publicação, página pública e analytics | Professor |
| 9 | Integração e fechamento | Exportações, encerramento de conta, revisão de critérios legais, testes integrados e regressão | Todos os módulos |

## Observações de dependência e paralelismo

- A fundação pode ser iniciada sem os módulos de negócio.
- Cadastro cria conta e perfil inicial em uma operação coordenada; o módulo `usuario` não deve depender de `professor`. A coordenação pertence a `application`.
- Financeiro pode ser desenvolvido antes da agenda porque a agenda depende do financeiro, não o contrário.
- Agenda depende dos Services públicos de financeiro para gerar/ajustar/cancelar cobranças por aula.
- `application` não precisa ser construído como uma camada completa antecipadamente. Criar os casos de uso quando houver operação transversal real.
- Página pública pode avançar em paralelo com financeiro/agenda após professor e decisões do perfil estarem estáveis, respeitando as dependências.
- Exportações e encerramento de conta exigem integração dos módulos e devem ser validados como fluxos ponta a ponta.

## Critério de conclusão de cada etapa

- Regras e RFs citados atendidos.
- Endpoints e schemas coerentes com `docs/api/openapi.yaml`.
- Testes unitários e de integração relevantes passando.
- Isolamento por professor coberto por testes.
- Transações e constraints verificadas no PostgreSQL.
- Sem dependência cíclica ou acesso a Repository de outro módulo.
- Documentação da SPEC atualizada com pendências explícitas.

## Fora da sequência do MVP

JWT/refresh, Redis, múltiplas instâncias, filas, Outbox, Quartz, pagamentos online, estorno, MFA, upload de mídia, login de aluno, notificações automáticas, auditoria abrangente e outras evoluções futuras não devem ser inseridas nesta ordem.
