# Política de decisões para agentes

- Requisitos, MVP, regras de negócio, UX e decisões funcionais: consultar Notion.
- Decisões técnicas aprovadas e sua implementação: registrar em `docs/architecture/` ou ADRs, distinguindo explicitamente o que foi decidido do que já está implementado.
- Decisões vigentes relevantes no Notion: D-012 (escopo do MVP), D-013 (verificação de e-mail) e D-014 (sessão HTTP stateful, cookie `JSESSIONID`, 30 minutos de inatividade, memória e uma instância no MVP). D-014 complementa a SDA-006; a tabela `SESSAO` permanece no schema, mas não é usada pela aplicação no MVP.
- Uma hipótese ou recomendação de IA não é uma decisão aprovada.
- Se houver conflito entre documentos, apontar a divergência e não escolher silenciosamente.
- Não alterar escopo nem contratos públicos sem solicitação/revisão.
- Para mudanças que afetem requisitos, citar os IDs oficiais no PR e solicitar validação humana.
