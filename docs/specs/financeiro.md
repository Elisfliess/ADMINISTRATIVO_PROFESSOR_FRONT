# SPEC — Financeiro

**Status:** primeira versão consolidada.  
**Módulo:** `financeiro`  
**Rastreabilidade:** RF22–RF25, RF42, RF57–RF60; RN03, RN12, RN17, RN20–RN30; A14; D-012.

## 1. Objetivo e responsabilidade

Administrar planos e cobranças como controle administrativo. O Compasso não processa pagamentos, não guarda dados de cartão e não realiza estornos.

## 2. Funcionalidades do MVP

- Criar, consultar, editar quando permitido e encerrar planos.
- Gerar cobranças mensais e por aula.
- Calcular situação de atraso.
- Listar cobranças pendentes, pagas e canceladas.
- Registrar baixa integral e forma de pagamento opcional.
- Cancelar cobrança pendente.
- Exportar CSV mensal de cobranças pagas.
- Agendamento diário de cobranças mensais.

## 3. Regras de negócio

- Um plano ativo por aluno (RN20).
- Somente periodicidade mensal ou por aula (RN20, D-012).
- Mensal: valor fixo, adiantado, cobrança no início do ciclo; vencimento de 1 a 28.
- Ciclo individual ancorado na data original do plano; ajustar fim de mês quando o dia não existir. Período termina no dia anterior ao próximo ciclo.
- Primeiro vencimento mensal não pode anteceder o início do plano.
- Por aula: aula agendada gera cobrança; vencimento é a data local da aula acrescida do prazo em dias; zero significa vencer no dia.
- Atraso começa no dia seguinte ao vencimento, em `America/Sao_Paulo`. Não há multa, juros ou suspensão de aulas.
- Não gerar cobrança para período já ocupado por cobrança ativa do mesmo aluno; avisar professor (RN29).
- Baixa é integral, registra data de recebimento e forma opcional. Não existe pagamento parcial.
- Cobrança paga é final e não pode ser cancelada/estornada.
- Cobrança cancelada é estado final e imutável.
- **Cobrança mensal cancelada (aprovado):** uma cobrança mensal cancelada permanece como registro histórico final e imutável. O gerador automático não pode criar outra cobrança para o mesmo `plano_id` e `periodo_inicio`, mesmo que a cobrança existente esteja cancelada. A existência deve ser considerada independentemente do status. A reemissão, caso seja necessária futuramente, deverá ser um caso de uso explícito e auditável, fora do gerador automático.
- Plano ativo sem nenhuma cobrança pode ter configurações financeiras alteradas, incluindo periodicidade, valor, início, dia de vencimento e prazo. Depois da primeira cobrança (inclusive cancelada), todos esses campos ficam protegidos.
- Plano encerrado não pode ser reativado; data de encerramento pode ser corrigida conforme regra aprovada.
- Para alterar condições protegidas, encerrar plano anterior e criar outro. Histórico de cobranças permanece.
- Inativar aluno encerra plano e cancela apenas cobranças pendentes vinculadas a aulas futuras canceladas; demais cobranças permanecem.
- Exportação mensal inclui somente cobranças pagas.
- Não processar pagamento, não oferecer parcial, estorno ou integração bancária.
- Não existe criação manual/avulsa de cobrança no MVP (D-012). Toda cobrança nasce de um caso de uso previsto: geração mensal do plano ou agendamento de aula em plano por aula. O professor pode consultar, baixar ou cancelar cobranças existentes.
- **Idempotência de geração (PostgreSQL-safe):** geração mensal e por aula utilizam inserção atômica `INSERT ... ON CONFLICT DO NOTHING` para conflitos de unicidade esperados, evitando abortar a transação. Chamadas repetidas/concorrentes não duplicam cobranças. Violações de outras constraints (FK, triggers, regras de negócio no banco) propagam como erro.

## 4. Modelo e persistência

A14 v3.4 e migration:
- `PLANO_PAGAMENTO` e `PAGAMENTO`.
- `vw_pagamento_situacao` calcula atraso; atraso não é persistido como estado separado.
- Chaves compostas garantem mesmo aluno/professor/plano/aula.
- `compasso_runtime` não possui DELETE em plano/cobrança; preservar histórico.
- PostgreSQL protege invariantes estruturais e integridade relacional.
- Respeitar constraints, locks e regras registradas em `backend/docs/database/INTEGRITY-RULES.md`.

## 5. API

Contrato em `docs/api/openapi.yaml`:
- `GET/POST /alunos/{alunoId}/planos`
- `GET/PATCH /planos/{planoId}` — o PATCH aceita periodicidade, valor, início, dia de vencimento e prazo somente enquanto o plano estiver ativo e sem cobranças.
- `POST /planos/{planoId}/encerramento`
- `GET /cobrancas`
- `GET /cobrancas/{cobrancaId}`
- `POST /cobrancas/{cobrancaId}/baixa`
- `POST /cobrancas/{cobrancaId}/cancelamento`
- `POST /cobrancas/exportacao` com corpo `{ "mes": "YYYY-MM", "senha": "..." }`

## 6. Validações e erros

- Valor não negativo conforme A14; moeda representada em decimal JSON e mapeada para tipo decimal no Java, nunca ponto flutuante binário para cálculo financeiro.
- Dia de vencimento mensal: 1–28.
- Prazo por aula: inteiro >= 0.
- Campos específicos de periodicidade devem ser coerentes: mensal usa diaVencimento; por aula usa prazoVencimentoDias.
- Baixa só para pendente; valor integral; data válida.
- Não permitir cancelar paga ou alterar cobrança cancelada.
- Não permitir editar campos financeiros protegidos do plano após geração de cobrança.
- Conflitos de período/plano ativo/estado geram 409.
- Rejeitar propriedades desconhecidas; erros RFC 9457.

## 7. Segurança e isolamento

- Todas as rotas administrativas exigem sessão.
- Escopo por professor vem da sessão; 404 para recursos de outro professor.
- CSV requer professor autenticado; exportação de dados do titular/aluno segue confirmação de senha quando exigida por RF39/RF44.
- Nunca retornar dados de outro professor ou detalhes internos do banco.

## 8. Transações e dependências

- **Direção arquitetural e dependências:** Financeiro pode depender de professor/aluno; não depende de agenda. A integração prevista é que Agenda invoque o Financeiro por Service público para operações relacionadas a aulas.
- **Integração atual vs. futura (Agenda):** o Financeiro disponibiliza o serviço público `GeracaoCobrancaService` (geração de cobranças por aula) para integração futura. Isso **não** significa que a integração completa com o módulo Agenda já esteja implementada. O bloqueio mínimo da linha de Aula é realizado diretamente pela implementação atual (`bloquearAulaSeAplicavel()`, quando aplicável) para aproximar a ordem de bloqueios, mas **não equivale** à integração completa do domínio Agenda (revalidação completa de elegibilidade/estado da aula na aplicação permanece limitada).
- **Operações transversais implementadas:** casos transversais já implementados no módulo Financeiro (ex.: criação/gestão de plano e operações financeiras indivisíveis no próprio contexto) continuam coordenados em application/transações conforme implementado. A coordenação transacional de **remarcação e cancelamento de aula com suas cobranças** depende da integração adequada com Agenda; quando essa coordenação ocorrer, deve respeitar atualização na mesma transação e as garantias das triggers.
- **Geração mensal:** agendamento por `@Scheduled` diário em `America/Sao_Paulo`, delegando para application; recuperação de períodos não processados após indisponibilidade.
- **Concorrência e integridade:** constraints PostgreSQL evitam duplicidade. Ordem de bloqueios adotada na aplicação: **Plano → Aula (quando aplicável) → Cobrança** (com limitações de integração de domínio Agenda descritas na seção 10 e em `backend/docs/database/INTEGRITY-RULES.md`). As triggers do PostgreSQL (`validar_e_proteger_pagamento`/`trg_pagamento_validar_integridade` e `verificar_integridade_aula_pagamento`/`trg_aula_integridade_pagamento`) continuam impondo as garantias de integridade descritas em `backend/docs/database/INTEGRITY-RULES.md`.
- Não manter transação aberta durante envio de e-mail ou chamadas externas.
- Sem Quartz, fila, Outbox ou infraestrutura distribuída no MVP.

## 9. Testes e critérios de aceite

- Um plano ativo por aluno.
- Ciclo mensal individual e ajuste de fim de mês corretos.
- Vencimento mensal limitado ao início do plano quando aplicável.
- Por aula: prazo 0 vence no dia da aula local.
- Atraso inicia no dia seguinte no fuso São Paulo.
- Cobrança de período já ocupado não é duplicada.
- Cobrança mensal cancelada não é recriada automaticamente para o mesmo plano e início de período.
- Baixa integral registra data e forma opcional.
- Paga e cancelada respeitam imutabilidade.
- Plano protege campos financeiros após primeira cobrança.
- Encerrado não reativa.
- Remarcação/cancelamento de aula atualiza cobrança em transação única.
- CSV inclui somente pagas do mês solicitado.
- Testar concorrência e constraints com PostgreSQL Testcontainers, não H2.

## 10. Pendências

- Criação manual de cobrança está fora do MVP; não reintroduzir `POST /cobrancas` sem revisão explícita do escopo D-012.
- Geração automática mensal implementada via `@Scheduled` diário em `America/Sao_Paulo`, delegando para `GeracaoCobrancaService`. Recuperação de ciclos não processados após indisponibilidade.
- Serviço público `GeracaoCobrancaService` disponível para integração futura com Agenda (sem integração neste PR).
- Colunas do CSV definidas (nesta ordem): Aluno, Valor, Vencimento, Recebimento, Período inicial, Período final, Forma de pagamento, Descrição. Cabeçalho com grafia exata, UTF-8 com BOM, separador vírgula, CRLF e escaping RFC 4180 com neutralização de fórmulas.
- Exportação via `POST /api/v1/cobrancas/exportacao` com mês e senha no corpo da requisição.
- Mês da exportação baseado em `data_pagamento` (mês de recebimento).
- CSV com BOM UTF-8, vírgula, CRLF, escaping RFC 4180 e proteção contra CSV Injection.
- Precisão monetária: `NUMERIC(10,2)`, valor zero permitido, valores futuros de recebimento proibidos.
- Ordem de bloqueios adotada na aplicação (6ª rodada): **Plano → Aula (quando aplicável) → Cobrança** nos fluxos de geração por aula, baixa e cancelamento. A aplicação utiliza bloqueio explícito da Aula por meio de `bloquearAulaSeAplicavel()` (lock mínimo da linha em `aula`, sem dependência Java do módulo Agenda), respeitando essa sequência. Cobranças mensais não envolvem etapa de Aula. A aplicação não realiza toda a coordenação do domínio Agenda; a integração com o módulo Agenda continua limitada (revalidação completa de elegibilidade/estado da aula na aplicação é limitada). As triggers do PostgreSQL permanecem responsáveis por garantias adicionais de integridade.
- Mecanismos reais de integridade no banco (V2): `validar_e_proteger_pagamento` (`trg_pagamento_validar_integridade`, BEFORE INSERT/UPDATE em `pagamento`) valida consistência da cobrança e bloqueia plano e, quando aplicável, aula vinculada; `verificar_integridade_aula_pagamento` (`trg_aula_integridade_pagamento`, constraint trigger adiada AFTER UPDATE OF `inicio_em`,`status` em `aula`, verificação no commit) impõe consistência final aula↔cobrança pendente (ex.: aula cancelada não pode manter cobrança pendente; remarcação exige atualização compatível da cobrança pendente na mesma transação, coordenada pela aplicação/Agenda). Responsabilidades e limites detalhados em `backend/docs/database/INTEGRITY-RULES.md`.
- Índice único parcial `uq_plano_ativo_por_aluno` garante um plano ativo por aluno.
- Índice único parcial `uq_pagamento_aula_ativo` garante uma cobrança ativa por aula; `uq_pagamento_aluno_periodo_ativo` protege cobranças mensais ativas por período do aluno.
- Índice único parcial `uq_pagamento_plano_periodo_inicio_mensal (plano_id, periodo_inicio) WHERE aula_id IS NULL` (V5) torna explícita a unicidade estrutural de cobranças mensais por plano/início de período para **todos os status** (incluindo cancelado), alinhada à idempotência e à regra aprovada: o gerador automático não recria cobrança para o mesmo `plano_id` e `periodo_inicio`, independentemente do status. Complementa `uq_pagamento_aluno_periodo_ativo`.
- **Cobrança mensal cancelada (aprovado):** registro histórico final e imutável; existência considerada independentemente do status; sem recriação automática pelo gerador. Reemissão, se necessária futuramente, deverá ser caso de uso explícito e auditável, fora do gerador automático (não implementada).
- **PATCH de plano (merge patch):** propriedade ausente preserva valor; campos não limpáveis (`periodicidade`, `valor`, `inicioEm`) rejeitam `null`; `diaVencimento`/`prazoVencimentoDias` podem ser limpos com `null` quando compatível com o estado final da periodicidade/validação. Alterações permitidas apenas para plano ativo sem cobranças.
- **Criação de plano:** conflito de unicidade do plano ativo (`uq_plano_ativo_por_aluno`) é traduzido especificamente para o conflito de negócio esperado (409); outras violações de integridade não são convertidas nesse conflito.
