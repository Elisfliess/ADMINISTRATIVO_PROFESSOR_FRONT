# Arquitetura

Esta pasta contém a referência técnica vigente para implementação do backend.

- [Arquitetura do Backend](BACKEND-ARCHITECTURE.md): organização modular, limites entre módulos, transações, agendamentos, e-mail, segurança, persistência e testes.

As decisões arquiteturais aprovadas também possuem registros individuais (SDA-001 a SDA-009) no Notion. A [D-014 — sessão HTTP no MVP](https://www.notion.so/419f51667c7c817ba5fdd242ffef34a8) complementa a SDA-006 e esclarece que a tabela `SESSAO` é mantida no schema, mas não será usada pela aplicação no MVP. Requisitos, MVP, regras de negócio e decisões funcionais permanecem no Notion.

**Importante:** decisões aprovadas não significam que todos os componentes já estejam implementados. Consulte o estado do código e os testes antes de afirmar que uma capacidade está concluída.
