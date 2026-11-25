# Regras de integridade de planos, aulas e cobranças

As regras abaixo são aplicadas pelo PostgreSQL nas migrations Flyway. A aplicação continua responsável pelas operações de negócio; as triggers verificam se os dados persistidos respeitam as regras.

## Planos

- Um plano ativo sem cobranças pode ter suas configurações financeiras alteradas.
- Depois que qualquer cobrança for criada, as configurações originais ficam protegidas.
- Um plano encerrado não pode ser reativado; a data de encerramento pode ser corrigida sem reabrir o plano.
- Para mudar as condições financeiras, a aplicação encerra o plano anterior e cria outro. As cobranças existentes continuam vinculadas ao plano original.

São configurações protegidas: periodicidade, valor, dia de vencimento, prazo de vencimento e data de início.

## Exclusão de registros

- O papel `compasso_runtime` não pode excluir planos nem cobranças. Planos encerrados e cobranças canceladas permanecem como registros históricos; operações de negócio usam os estados previstos, sem apagar esses registros.

## Cobranças

- A cobrança deve pertencer ao mesmo aluno, professor e plano.
- O valor deve ser igual ao valor registrado no plano de origem.
- Plano mensal: a cobrança não pode apontar para uma aula; período e vencimento devem corresponder ao ciclo individual ancorado no início original do plano.
- Plano por aula: a cobrança precisa apontar para uma aula do mesmo aluno e professor; período e vencimento são calculados a partir da data local da aula em `America/Sao_Paulo`.
- Uma cobrança paga é final. Apenas descrição e forma de pagamento podem ser alteradas.
- Uma cobrança cancelada é totalmente imutável.
- Uma cobrança pendente de aula pode acompanhar uma remarcação, mas a aula e a cobrança devem ser atualizadas na mesma transação.
- Cancelar uma aula com cobrança pendente exige cancelar a cobrança na mesma transação.
- Cobranças pagas não acompanham remarcações posteriores.

O banco não cria, cancela ou remarca cobranças automaticamente. Essas operações continuam sendo responsabilidade da aplicação.

## Triggers e garantias reais (V2)

- **`validar_e_proteger_pagamento`** (`trg_pagamento_validar_integridade`, `BEFORE INSERT OR UPDATE ON pagamento`, por linha): valida consistência da cobrança com plano/aula/aluno/professor, valor, periodicidade, período e vencimento. Bloqueia o plano com `SELECT ... FOR UPDATE`; para cobrança por aula, bloqueia a aula vinculada com `SELECT ... FOR UPDATE` e valida status/pertencimento da aula e coerência de datas. Em `UPDATE`, aplica regras de imutabilidade/transição (ex.: cobrança cancelada imutável; cobrança paga com campos financeiros protegidos; restrições de alteração conforme tipo de plano/status). **Garante** integridade estrutural e serialização no acesso a plano/aula durante operações em `pagamento`. **Depende da aplicação** para orquestrar casos de uso, decidir transições permitidas e, quando exigido por operações de Agenda, atualizar aula e cobrança na mesma transação.
- **`verificar_integridade_aula_pagamento`** (`trg_aula_integridade_pagamento`, constraint trigger `AFTER UPDATE OF inicio_em, status ON aula`, `DEFERRABLE INITIALLY DEFERRED`, por linha): verifica no **commit** o estado final. Se a aula ficou `cancelada`, não pode existir cobrança `pendente` vinculada. Se a aula foi remarcada (não cancelada), cobranças `pendentes` de plano `por_aula` devem ter período/vencimento compatíveis com a data local da aula e o prazo do plano. **Garante** que inconsistências finais aula↔cobrança pendente não sejam confirmadas. **Depende da aplicação/Agenda** para realizar, na mesma transação, as atualizações necessárias (ex.: cancelar cobrança pendente ao cancelar aula; ajustar cobrança pendente ao remarcar).

## Ordem de bloqueios nas transações

Para reduzir o risco de deadlocks, a aplicação deve seguir a mesma ordem ao trabalhar com uma cobrança:

1. Bloquear o plano com `SELECT ... FOR UPDATE`.
2. Se a cobrança estiver vinculada a uma aula, bloquear a aula com `SELECT ... FOR UPDATE`.
3. Se a cobrança já existir, bloquear a cobrança com `SELECT ... FOR UPDATE`.
4. Executar as alterações necessárias dentro da mesma transação.

### Garantias envolvendo Aula por fluxo (estado atual)

- **Geração de cobrança por aula (`INSERT` em `pagamento`):** a aplicação bloqueia o **Plano**, realiza lock mínimo da **Aula** vinculada (quando aplicável, via `SELECT ... FOR UPDATE` na tabela `aula`, sem dependência Java do módulo Agenda) e insere de forma atômica/idempotente (`ON CONFLICT DO NOTHING`). **Garantia:** consistência do vínculo e serialização no banco, com `validar_e_proteger_pagamento` validando/bloqueando regras definitivas na inserção. **Limitação:** a aplicação não realiza toda a coordenação do domínio Agenda; sem integração de domínio/serviço Agenda, a revalidação completa de elegibilidade/estado da aula na aplicação é limitada. As triggers do PostgreSQL permanecem responsáveis por garantias adicionais de integridade.
- **Baixa e cancelamento de cobrança vinculada a aula (`UPDATE` de status em `pagamento`):** a aplicação adquire lock de **Plano**, lock mínimo de **Aula** quando aplicável (via `SELECT ... FOR UPDATE` na tabela `aula`, sem dependência Java do módulo Agenda) e lock de **Cobrança**, revalidando estado/vínculo após os bloqueios. Ao persistir, `validar_e_proteger_pagamento` bloqueia a aula (`FOR UPDATE`) e revalida regras do vínculo financeiro; `verificar_integridade_aula_pagamento` impõe consistência final em alterações de aula (adiada ao commit) e exige coordenação transacional adequada nas operações de Agenda. **Garantia:** validação/serialização no banco durante a atualização da cobrança. **Limitação:** a integração com o domínio Agenda continua limitada (revalidação completa de elegibilidade/estado da aula na aplicação não é realizada); as triggers permanecem garantias definitivas para regras de vínculo e consistência final.
- **Cobranças mensais (sem aula):** não há etapa de bloqueio de Aula; aplica-se a ordem Plano → Cobrança quando aplicável.
- **Operações de Agenda (cancelamento/remarcação de aula):** dependem da implementação/integração do módulo Agenda; `verificar_integridade_aula_pagamento` impõe consistência final no commit quando houver cobrança pendente por aula.

**Proposta objetiva (etapa futura, se requerida):** expor integração mínima para lock/leitura consistente de Aula no contexto de Agenda (ou infraestrutura compartilhada acordada) para que Financeiro execute identificar plano/aula → lock plano → lock aula → lock cobrança → revalidar → operar com revalidação completa de domínio, respeitando fronteiras arquiteturais. Requer decisão de arquitetura/integração.

Na remarcação, a aplicação/Agenda deve atualizar a aula e a cobrança pendente na mesma transação. A trigger adiada `verificar_integridade_aula_pagamento` verifica a consistência no commit; se uma das atualizações faltar ou terminar com datas divergentes, toda a transação é revertida.

Na operação de cancelamento da aula, a aplicação/Agenda deve atualizar o status da aula e cancelar a cobrança pendente na mesma transação.

Esses bloqueios duram até o fim da transação. Não devem ser mantidos enquanto a aplicação aguarda interação do usuário ou realiza chamadas externas.

### Idempotência e unicidade estrutural de cobranças

- **Mensal:** geração idempotente via inserção atômica `INSERT ... ON CONFLICT DO NOTHING` sobre `uq_pagamento_plano_periodo_inicio_mensal (plano_id, periodo_inicio) WHERE aula_id IS NULL` (V5), que torna explícita a unicidade por plano/início de período para cobranças mensais (**qualquer status, inclusive `cancelado`**). `uq_pagamento_aluno_periodo_ativo` (V1, ativa e sem aula, por aluno/período) permanece como proteção complementar contra sobreposição no escopo do aluno. **Regra aprovada:** uma cobrança mensal cancelada permanece como registro histórico final e imutável; o gerador automático não pode criar outra cobrança para o mesmo `plano_id` e `periodo_inicio`, mesmo que a cobrança existente esteja cancelada (existência considerada independentemente do status). A reemissão, caso seja necessária futuramente, deverá ser um caso de uso explícito e auditável, fora do gerador automático.
- **Por aula:** idempotência para cobranças ativas por `uq_pagamento_aula_ativo (aula_id) WHERE status IN ('pendente','pago')` com inserção atômica; cobranças já existentes para a aula (inclusive canceladas) são tratadas como não geráveis novamente no serviço. Conflitos/violações de integridade não relacionados à idempotência esperada não são convertidos em sucesso.

## Escopo

Estas regras não implementam agrupamento operacional de alunos, cobrança coletiva, rateio de valores ou bloqueio de sobreposição de períodos. Cada aluno continua sendo administrado individualmente no MVP.
