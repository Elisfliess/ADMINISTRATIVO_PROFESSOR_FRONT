# Definition of Done — Compasso

## 1. Objetivo

Definir critérios mínimos e verificáveis para considerar uma alteração pronta para revisão e um Pull Request pronto para merge.

A Definition of Done (DoD) deve ser aplicada proporcionalmente ao tipo de alteração. Ela não exige que todo PR execute validações que não se relacionam ao seu escopo, nem substitui os critérios de aceite das SPECs.

## 2. Critérios gerais

Uma alteração está pronta para revisão quando:

- [ ] atende ao objetivo e aos limites de escopo aprovados;
- [ ] respeita requisitos, regras de negócio, arquitetura e decisões vigentes;
- [ ] não introduz mudanças não solicitadas ou decisões implícitas;
- [ ] código e testes relacionados foram atualizados;
- [ ] build/compilação e verificações aplicáveis foram executados;
- [ ] falhas e avisos relevantes foram analisados, sem ocultar ou desabilitar controles;
- [ ] diff completo foi revisado;
- [ ] documentação técnica e referências afetadas foram atualizadas;
- [ ] não contém segredos, credenciais ou dados pessoais reais;
- [ ] riscos, limitações e pendências estão registrados.

Para documentação, configuração ou alterações sem código executável, substitua critérios de build/teste por revisão de links, consistência, exemplos, sintaxe e compatibilidade com as instruções existentes.

## 3. Testes e evidências

### 3.1. Testes obrigatórios conforme aplicabilidade

- Testes unitários para regras e lógica que possam ser isoladas.
- Testes de integração para persistência, constraints, transações e comportamento dependente do banco.
- Testes de API para contratos, autenticação, autorização, validação e respostas HTTP afetadas.
- Testes de frontend para comportamento alterado, quando a infraestrutura de testes estiver configurada.
- Testes completos disponíveis quando a mudança puder causar regressões em outras áreas.

O backend deve usar PostgreSQL via Testcontainers para testes de persistência. H2 não substitui PostgreSQL. A CI atual utiliza PostgreSQL 17.11; não declare outra versão como validada sem executar testes específicos.

### 3.2. Gate de teste de regressão

Para correção de defeito, deve existir teste que represente o cenário que falhava, sempre que tecnicamente viável.

A evidência preferencial é:
1. executar o teste na versão anterior à correção e observar falha pelo motivo esperado;
2. aplicar a correção e observar o mesmo teste passar;
3. executar os testes relacionados e verificar ausência de regressão.

Se a execução pré-correção não for viável, o PR deve explicar o motivo e demonstrar como o teste cobre o defeito. Não é necessário exigir esse gate para documentação, refatorações sem alteração de comportamento ou novas funcionalidades que não estejam corrigindo um defeito existente.

O teste deve verificar o comportamento observável e relevante, não apenas repetir a implementação ou validar detalhes internos sem valor de regressão.

### 3.3. Determinismo

- Prefira relógio controlável, dados isolados e cenários reproduzíveis.
- Evite esperas reais, dependência de rede e serviços externos em testes automatizados.
- Testes concorrentes devem sincronizar o cenário e verificar os resultados esperados.
- Registre limitações quando uma validação depender de recurso indisponível.

## 4. Validações por categoria

| Categoria | Quando se aplica | Critério |
| --- | --- | --- |
| Build / compilação | Código executável alterado | Build concluído sem erros |
| Suíte automatizada | Mudança com impacto em código | Testes relevantes e suíte completa disponível executados |
| PostgreSQL / Testcontainers | Persistência, migrations, queries, transações ou constraints | Testes de integração com PostgreSQL real passando |
| Flyway | Migration nova ou alterada antes de aplicada | Migration executada desde schema limpo e validada; migrations já aplicadas não são editadas |
| OpenAPI | Contrato, endpoint, DTO, parâmetro, status ou erro alterado | Lint/validação do contrato passando e contrato coerente com implementação |
| Segurança | Autenticação, autorização, sessão, cookies, CORS, CSRF ou dados sensíveis afetados | Casos positivos e negativos relevantes cobertos |
| Frontend | Interface ou lógica de cliente alterada | Lint, testes e build disponíveis; validação visual/manual quando relevante |
| Integração externa | Provedor, e-mail ou serviço externo afetado | Contrato, sucesso e falhas relevantes testados sem depender do serviço em toda execução |
| Runtime / smoke test | Mudança depende de configuração ou ambiente real | Fluxo mínimo validado em ambiente apropriado, com autorização quando compartilhado |
| Documentação | Docs, instruções, templates ou exemplos alterados | Links, caminhos, comandos e afirmações conferidos |

A lista é condicional: marque como não aplicável o que não tiver relação com o escopo. Se uma validação necessária não puder ser feita, registre o motivo, o risco e se isso impede ou não o merge.

## 5. Migrations e ambientes compartilhados

- Migrations devem ser incrementais e compatíveis com o histórico aplicado.
- Nunca editar migration já aplicada; criar nova migration corretiva.
- Validar migrations em PostgreSQL descartável/Testcontainers quando pertinente.
- Não usar o Supabase compartilhado como ambiente de testes automatizados.
- Aplicar migrations em Supabase Dev ou outro ambiente compartilhado somente com autorização explícita e coordenação com a equipe.
- Não exigir migration aplicada em banco compartilhado para todo PR. Essa validação só se aplica quando a entrega depende de execução operacional nesse ambiente.
- Registrar claramente quais ambientes foram alterados, por quem e com qual resultado, quando aplicável.

## 6. Pronto para merge

Além dos critérios de revisão, um PR está pronto para merge quando:

- [ ] atende à DoD aplicável;
- [ ] CI aplicável está verde; se não houver workflow/check aplicável, isso foi registrado no PR;
- [ ] não há comentários de revisão pendentes sem resposta;
- [ ] não há bloqueios conhecidos de segurança, integridade ou requisito;
- [ ] documentação e contrato afetados estão coerentes;
- [ ] pendências não bloqueadoras estão descritas e aceitas;
- [ ] nenhuma ação operacional pendente está sendo tratada como concluída;
- [ ] revisão humana foi realizada conforme o fluxo de contribuição vigente.

A aprovação técnica da IA ou a CI verde não substituem a revisão humana nem autorizam merge. O merge deve seguir a autorização do responsável e o fluxo de contribuição vigente.

## 7. Encerramento e registro de pendências

Ao fechar o trabalho, classifique cada pendência:

- **Bloqueadora:** impede requisito, segurança, integridade ou critério de aceite; deve ser resolvida antes do merge.
- **Não bloqueadora:** limitação conhecida, aceita para a entrega atual; registrar responsável ou momento de reavaliação quando conhecido.
- **Fora do escopo:** não pertence ao objetivo do PR; registrar como tarefa futura quando necessário.

O relatório final deve indicar:
- o que foi implementado;
- quais verificações foram executadas e seus resultados;
- o que não foi verificado e por quê;
- quais decisões foram tomadas ou continuam pendentes;
- estado do PR e da CI;
- pendências e riscos remanescentes.

Não declare uma tarefa concluída apenas porque o código foi escrito. Conclusão exige evidência proporcional ao risco e transparência sobre o que ainda falta.
