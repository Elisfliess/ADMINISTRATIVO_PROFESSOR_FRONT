# SPEC — Agenda

**Status:** primeira versão consolidada.  
**Módulo:** `agenda`  
**Rastreabilidade:** RF16–RF21, RF61; RN01–RN14; A14; D-012.

## 1. Objetivo e responsabilidade

Administrar aulas individuais na agenda, incluindo criação, recorrência semanal, consulta, edição, remarcação, cancelamento e status. Uma aula pertence a um aluno. Aulas em grupo são representadas por aulas individuais de alunos diferentes no mesmo horário.

## 2. Funcionalidades do MVP

- Criar e consultar aulas.
- Pré-visualizar a criação e cobranças previstas antes de confirmar.
- Criar série semanal até uma data final.
- Visões diária/semanal são responsabilidade da apresentação; API fornece dados por intervalo.
- Editar aula agendada.
- Remarcar a mesma aula por formulário.
- Pré-visualizar os efeitos financeiros do cancelamento.
- Cancelar definitivamente.
- Registrar dada/falta e alternar/desfazer esses dois estados conforme regras.
- Criar reposição como nova aula, sem vínculo com a aula original.

## 3. Regras de negócio

- Duração padrão 60 minutos; fim sugerido pelo início + 60 minutos e editável (RN01).
- Cada aula pertence a um aluno. Aulas no mesmo horário são permitidas; não implementar bloqueio de sobreposição (RN02).
- Local não é campo específico; professor registra em observações (RN04).
- Repetição cria uma aula por semana até a data final. Antes de confirmar, informar quantidade de aulas e, no plano por aula, quantidade de cobranças previstas (RF61).
- Depois de criadas, ocorrências são independentes: editar/remarcar/cancelar uma não altera as demais (RN05).
- Aula pode ser marcada dada/falta a partir do horário de início. Dada e falta podem ser trocadas entre si ou desfeitas (RN06).
- Falta é cobrada; mudança entre dada/falta não altera cobrança (RN08).
- Cancelamento é definitivo; não reabrir nem reagendar a mesma aula (RN10).
- Reposição é nova aula, sem vínculo com original. Para cancelamento avisado pelo aluno, regra de negócio prevê até 30 dias, mas sistema não valida o prazo. Quando professor cancela, ele é responsável por repor (RN11, RN13).
- Remarcação move a mesma aula; cobrança pendente vinculada acompanha nova data na mesma transação; cobrança paga não muda.
- Cancelar aula com cobrança pendente de plano por aula exige cancelamento da cobrança na mesma transação. Plano mensal não muda por cancelamento de aula.
- Ordem de locks para operações financeiras associadas: plano, aula, cobrança.

## 4. Modelo e persistência

A14:
- `AULA` pertence ao professor, aluno e modalidade por chaves estrangeiras compostas.
- Estados de negócio incluem agendada, dada, falta e cancelada; mapear para enums HTTP `AGENDADA`, `DADA`, `FALTA`, `CANCELADA`.
- V1 possui trigger `trg_aula_cancelada_final`, que impede transição de CANCELADA para outro status. Esta trigger é proteção de integridade no PostgreSQL, não implementação do caso de uso. A aplicação continua responsável por validar e executar o cancelamento.
- Não criar tabela de recorrência como entidade permanente sem necessidade: recorrência é comando de criação que materializa aulas independentes.

## 5. API

Contrato em `docs/api/openapi.yaml`:
- `GET /aulas` por intervalo, aluno e status.
- `POST /aulas/previa` calcula quantidade de aulas e cobranças previstas sem persistir nada; para plano por aula, apresenta valores e vencimentos antes da confirmação.
- `POST /aulas` cria aula única ou série semanal e recalcula/valida os dados da prévia antes de persistir.
- `GET /aulas/{aulaId}`.
- `PATCH /aulas/{aulaId}` edita aula.
- `POST /aulas/{aulaId}/remarcacao`.
- `GET /aulas/{aulaId}/cancelamento/previa` informa se há cobrança vinculada e o impacto financeiro, sem mutação.
- `POST /aulas/{aulaId}/cancelamento` executa o cancelamento após confirmação.
- `PATCH /aulas/{aulaId}/status` aceita somente `DADA` e `FALTA`; cancelamento é realizado pela operação específica.

Não existe endpoint para reabrir aula cancelada nem para alterar toda uma série já criada.

## 6. Validações e erros

- Início anterior ao fim; duração positiva.
- Aluno e modalidade pertencem ao professor autenticado.
- Série semanal exige data final válida e quantidade positiva; validar limite operacional razoável sem inventar máximo de produto, e registrar eventual limite técnico antes de aplicá-lo.
- Aula cancelada não aceita edição/remarcação nem alteração de status.
- O endpoint de status aceita somente `DADA` e `FALTA`; o cancelamento sempre usa o caso de uso específico, que aplica a mesma confirmação e os efeitos financeiros do RF20/RF21.
- Dada/falta só a partir do início.
- 404 para aula de outro professor; 409 para transição inválida; 400 para estrutura/formato inválido.
- Rejeitar propriedades desconhecidas.

## 7. Segurança e isolamento

Todas as operações exigem sessão. Consultas e mutações sempre escopadas ao professor da sessão. Nunca aceitar professorId como fonte de autorização.

## 8. Transações e dependências

- Agenda pode depender de professor, aluno e financeiro via Services públicos.
- Agenda pode acionar financeiro; financeiro não chama agenda.
- Criar série e respectivas cobranças por aula deve ser atômico quando uma falha inviabiliza o conjunto aprovado.
- Remarcar e atualizar cobrança pendente são uma transação.
- Cancelar aula e cobrança pendente correspondente são uma transação.
- Locks em ordem: plano → aula → cobrança.
- A trigger PostgreSQL é defesa adicional de integridade. Se ela rejeitar uma transição, traduzir para conflito de estado sem expor SQL/stack trace.

## 9. Testes e critérios de aceite

- Duração sugerida 60 min, editável.
- Aulas simultâneas são permitidas.
- Prévia de aula única/série apresenta quantidade e, para plano por aula, valores e vencimentos previstos sem persistência.
- Prévia de cancelamento informa impacto sobre cobrança pendente ou paga sem mutação.
- A criação efetiva recalcula os valores e vencimentos; não confia cegamente na prévia.
- Série semanal cria contagem correta e ocorrências independentes.
- Remarcação preserva ID da aula e atualiza somente cobrança pendente.
- Cobrança paga permanece inalterada.
- Cancelamento mantém histórico e cancela cobrança pendente por aula atomicamente.
- Trigger impede reabertura mesmo se atualização tentar contornar Service.
- Dada/falta só a partir do início e não altera cobrança.
- Reposição gera nova aula sem vínculo com origem.
- Isolamento entre professores testado.

## 10. Pendências

- Especificar limite técnico de quantidade de ocorrências por comando de recorrência se necessário, sem mudar a regra de produto.
- Confirmar nomes exatos das colunas e enum no schema ao implementar.
