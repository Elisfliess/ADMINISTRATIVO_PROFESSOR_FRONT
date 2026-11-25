# Arquitetura — instruções para agentes

Antes de implementar ou alterar o backend, leia `docs/architecture/BACKEND-ARCHITECTURE.md`.

Regras essenciais:
- O backend é um monólito modular; não introduza microserviços.
- Respeite os limites entre `usuario`, `professor`, `aluno`, `agenda`, `financeiro`, `pagina_publica` e `shared`.
- Operações transversais são orquestradas pela camada `application`; regras continuam nos módulos.
- Casos de uso que alteram vários módulos de forma indivisível devem compartilhar uma transação.
- E-mail é enviado após commit por meio do contrato `EmailSender`.
- Geração mensal usa `@Scheduled` diário em `America/Sao_Paulo`, com recuperação de períodos não processados.
- Sessões HTTP em memória e uma instância apenas no MVP.
- Flyway controla schema; Hibernate usa `validate`; migrations aplicadas nunca são editadas.
- Testes de persistência usam Testcontainers com PostgreSQL 17, não H2.
- Não ampliar o MVP, adicionar infraestrutura ou tratar propostas de IA como decisões aprovadas.

Consulte também `docs/api/README.md`, `docs/api/openapi.yaml` e a SPEC do módulo afetado. Para requisitos e decisões, use o Notion: D-012 (MVP), D-013 (verificação de e-mail), D-014 (sessão HTTP) e SDA-001 a SDA-009. [D-014 — sessão HTTP no MVP](https://www.notion.so/419f51667c7c817ba5fdd242ffef34a8) complementa a SDA-006. A tabela física `SESSAO` permanece no schema por compatibilidade, mas não é usada pela aplicação no MVP. Se houver conflito entre documentação, interrompa a alteração e peça esclarecimento.
