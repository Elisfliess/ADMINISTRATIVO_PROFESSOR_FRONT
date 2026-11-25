package br.com.compasso.financeiro;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import br.com.compasso.financeiro.entity.Cobranca;
import br.com.compasso.financeiro.entity.CobrancaStatus;
import br.com.compasso.financeiro.entity.PeriodicidadePlano;
import br.com.compasso.financeiro.entity.PlanoPagamento;
import br.com.compasso.financeiro.repository.CobrancaRepository;
import br.com.compasso.financeiro.repository.PlanoPagamentoRepository;
import br.com.compasso.financeiro.service.GeracaoCobrancaService;
import br.com.compasso.support.AbstractApiIntegrationTest;
import tools.jackson.databind.JsonNode;

@Import(FinanceiroRegrasIntegrationTest.ClockTestConfig.class)
class FinanceiroRegrasIntegrationTest extends AbstractApiIntegrationTest {

    private static final ZoneId ZONA = ZoneId.of("America/Sao_Paulo");

    @Autowired
    private GeracaoCobrancaService geracaoCobrancaService;

    @Autowired
    private br.com.compasso.financeiro.service.FinanceiroService financeiroService;

    @Autowired
    private PlanoPagamentoRepository planoRepository;

    @Autowired
    private CobrancaRepository cobrancaRepository;

    @Autowired
    private MutableClock mutableClock;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @TestConfiguration
    static class ClockTestConfig {
        @Bean
        @Primary
        MutableClock mutableClock() {
            return new MutableClock();
        }
    }

    static class MutableClock extends Clock {
        private volatile Clock delegate = Clock.system(ZONA);

        void fixar(LocalDate data) {
            Instant instante = data.atStartOfDay(ZONA).toInstant();
            delegate = Clock.fixed(instante, ZONA);
        }

        void reset() {
            delegate = Clock.system(ZONA);
        }

        @Override
        public ZoneId getZone() {
            return delegate.getZone();
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return new Clock() {
                @Override
                public ZoneId getZone() {
                    return zone;
                }

                @Override
                public Clock withZone(ZoneId z) {
                    return MutableClock.this.withZone(z);
                }

                @Override
                public Instant instant() {
                    return MutableClock.this.delegate.instant();
                }
            };
        }

        @Override
        public Instant instant() {
            return delegate.instant();
        }
    }

    @Test
    void exportacaoCsvSegueContratoAprovado() throws Exception {
        ContaTeste conta = contaComAceite();
        JsonNode aluno = criarAluno(conta, alunoAdulto("Jose, \"da Silva\""));
        JsonNode plano = criarPlano(conta, aluno, planoMensal());

        UUID professorId = professorIdDaConta(conta);
        UUID alunoId = UUID.fromString(aluno.get("id").asText());
        UUID planoId = UUID.fromString(plano.get("id").asText());

        // Ciclos válidos para plano inicio 2026-01-10, diaVencimento 10
        UUID pagaNoMes = inserirCobranca(professorId, alunoId, planoId, new BigDecimal("150.00"),
                LocalDate.of(2026, 1, 10), LocalDate.of(2026, 1, 10), LocalDate.of(2026, 2, 9),
                CobrancaStatus.PAGO, LocalDate.of(2026, 2, 5), "PIX", "=HYPERLINK(\"x\")");
        // Vencida em fevereiro (ciclo 10/02-09/03), recebida em fevereiro => deve entrar em fevereiro
        inserirCobranca(professorId, alunoId, planoId, new BigDecimal("150.00"),
                LocalDate.of(2026, 2, 10), LocalDate.of(2026, 2, 10), LocalDate.of(2026, 3, 9),
                CobrancaStatus.PAGO, LocalDate.of(2026, 2, 20), "Dinheiro", "Descricao, com virgula");
        // Paga em março => fora do mês (ciclo 10/03-09/04)
        inserirCobranca(professorId, alunoId, planoId, new BigDecimal("150.00"),
                LocalDate.of(2026, 3, 10), LocalDate.of(2026, 3, 10), LocalDate.of(2026, 4, 9),
                CobrancaStatus.PAGO, LocalDate.of(2026, 3, 1), "PIX", "Fora");
        // Não paga => fora
        inserirCobranca(professorId, alunoId, planoId, new BigDecimal("150.00"),
                LocalDate.of(2026, 4, 10), LocalDate.of(2026, 4, 10), LocalDate.of(2026, 5, 9),
                CobrancaStatus.PENDENTE, null, null, null);

        MvcResult resultado = postJson("/api/v1/cobrancas/exportacao",
                Map.of("mes", "2026-02", "senha", SENHA_PADRAO), conta.csrf(), conta.sessao())
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("cobrancas-2026-02.csv")))
                .andReturn();

        byte[] bytes = resultado.getResponse().getContentAsByteArray();
        assertThat(bytes[0]).isEqualTo((byte) 0xEF);
        assertThat(bytes[1]).isEqualTo((byte) 0xBB);
        assertThat(bytes[2]).isEqualTo((byte) 0xBF);
        String csv = new String(bytes, StandardCharsets.UTF_8);
        assertThat(csv).startsWith("\uFEFFAluno,Valor,Vencimento,Recebimento,Período inicial,Período final,Forma de pagamento,Descrição\r\n");
        assertThat(csv).contains("\r\n");
        long linhasDados = csv.lines().count() - 1;
        assertThat(linhasDados).isEqualTo(2);
        assertThat(csv).contains("\"Jose, \"\"da Silva\"\"\"");
        assertThat(csv).contains("150.00");
        assertThat(csv).contains("2026-02-05");
        assertThat(csv).contains("'=HYPERLINK");
        assertThat(csv).doesNotContain(alunoId.toString());
        assertThat(csv).doesNotContain(planoId.toString());
        assertThat(csv).doesNotContain(pagaNoMes.toString());

        // Senha inválida
        postJson("/api/v1/cobrancas/exportacao", Map.of("mes", "2026-02", "senha", "errada"), conta.csrf(), conta.sessao())
                .andExpect(status().isForbidden());

        // Isolamento entre professores
        ContaTeste outro = contaComAceite();
        postJson("/api/v1/cobrancas/exportacao", Map.of("mes", "2026-02", "senha", SENHA_PADRAO), outro.csrf(), outro.sessao())
                .andExpect(status().isOk())
                .andExpect(result -> {
                    String outroCsv = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
                    assertThat(outroCsv).doesNotContain("Jose");
                });
    }

    @Test
    void planoPorAulaValidaPrazoNaCriacao() throws Exception {
        ContaTeste conta = contaComAceite();
        JsonNode aluno = criarAluno(conta, alunoAdulto("Ana Souza"));
        String caminho = "/api/v1/alunos/" + aluno.get("id").asText() + "/planos";

        // zero aceito
        Map<String, Object> zero = planoPorAula();
        zero.put("prazoVencimentoDias", 0);
        postJson(caminho, zero, conta.csrf(), conta.sessao())
                .andExpect(status().isCreated())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.prazoVencimentoDias").value(0));

        // novo aluno para negativo/ausente
        JsonNode aluno2 = criarAluno(conta, alunoAdulto("Beto"));
        String caminho2 = "/api/v1/alunos/" + aluno2.get("id").asText() + "/planos";
        Map<String, Object> negativo = planoPorAula();
        negativo.put("prazoVencimentoDias", -1);
        postJson(caminho2, negativo, conta.csrf(), conta.sessao())
                .andExpect(status().isBadRequest());

        Map<String, Object> ausente = planoPorAula();
        ausente.put("prazoVencimentoDias", null);
        postJson(caminho2, ausente, conta.csrf(), conta.sessao())
                .andExpect(status().isBadRequest());
    }

    @Test
    void baixaRejeitaDataFutura() throws Exception {
        ContaTeste conta = contaComAceite();
        JsonNode aluno = criarAluno(conta, alunoAdulto("Ana Souza"));
        JsonNode plano = criarPlano(conta, aluno, planoMensal());
        UUID professorId = professorIdDaConta(conta);
        UUID cobrancaId = inserirCobranca(professorId, UUID.fromString(aluno.get("id").asText()),
                UUID.fromString(plano.get("id").asText()), new BigDecimal("150.00"),
                LocalDate.of(2026, 1, 10), LocalDate.of(2026, 1, 10), LocalDate.of(2026, 2, 9),
                CobrancaStatus.PENDENTE, null, null, null);

        LocalDate hoje = LocalDate.now(ZONA);
        postJson("/api/v1/cobrancas/" + cobrancaId + "/baixa",
                Map.of("recebidoEm", hoje.plusDays(1).toString()), conta.csrf(), conta.sessao())
                .andExpect(status().isBadRequest());
        postJson("/api/v1/cobrancas/" + cobrancaId + "/baixa",
                Map.of("recebidoEm", hoje.toString()), conta.csrf(), conta.sessao())
                .andExpect(status().isOk());
    }

    @Test
    void geracaoMensalAncoraEmDiaOriginalERecuperaCiclos() {
        try {
            // Plano iniciado no dia 31; dia de vencimento deve ser 1-28 (regra), mas a ancoragem do ciclo usa inicioEm.
            ContaTesteDados dados = criarContaAlunoPlanoMensal(LocalDate.of(2026, 1, 31), 28, new BigDecimal("100.00"));
            fixarClock(LocalDate.of(2026, 5, 15));

            geracaoCobrancaService.gerarCobrancasMensais();

            List<Cobranca> cobrancas = cobrancaRepository.findByProfessorId(dados.professorId)
                    .stream().sorted(Comparator.comparing(Cobranca::getPeriodoInicio)).toList();
            assertThat(cobrancas).extracting(Cobranca::getPeriodoInicio)
                    .containsExactly(
                            LocalDate.of(2026, 1, 31),
                            LocalDate.of(2026, 2, 28),
                            LocalDate.of(2026, 3, 31),
                            LocalDate.of(2026, 4, 30));
            assertThat(cobrancas).extracting(Cobranca::getPeriodoFim)
                    .containsExactly(
                            LocalDate.of(2026, 2, 27),
                            LocalDate.of(2026, 3, 30),
                            LocalDate.of(2026, 4, 29),
                            LocalDate.of(2026, 5, 30));

            // idempotência
            geracaoCobrancaService.gerarCobrancasMensais();
            assertThat(cobrancaRepository.findByProfessorId(dados.professorId)).hasSize(4);
        } finally {
            mutableClock.reset();
        }
    }

    @Test
    void geracaoMensalNaoGeraCicloFuturo() {
        try {
            ContaTesteDados dados = criarContaAlunoPlanoMensal(LocalDate.of(2026, 5, 10), 10, new BigDecimal("100.00"));
            fixarClock(LocalDate.of(2026, 5, 9));
            geracaoCobrancaService.gerarCobrancasMensais();
            assertThat(cobrancaRepository.findByProfessorId(dados.professorId)).isEmpty();

            fixarClock(LocalDate.of(2026, 5, 10));
            geracaoCobrancaService.gerarCobrancasMensais();
            assertThat(cobrancaRepository.findByProfessorId(dados.professorId)).hasSize(1);
            assertThat(cobrancaRepository.findByProfessorId(dados.professorId).get(0).getPeriodoInicio())
                    .isEqualTo(LocalDate.of(2026, 5, 10));
        } finally {
            mutableClock.reset();
        }
    }

    @Test
    void geracaoPorAulaPermiteAulasDiferentesNoMesmoDiaEImpedeDuplicidade() {
        ContaTesteDados dados = criarContaAlunoPlanoPorAula(LocalDate.of(2026, 1, 1), 0, new BigDecimal("80.00"));
        UUID aula1 = inserirAula(dados.professorId, dados.alunoId, LocalDate.of(2026, 2, 10));
        UUID aula2 = inserirAula(dados.professorId, dados.alunoId, LocalDate.of(2026, 2, 10));

        geracaoCobrancaService.gerarCobrancaPorAula(dados.professorId, dados.alunoId, aula1, LocalDate.of(2026, 2, 10));
        geracaoCobrancaService.gerarCobrancaPorAula(dados.professorId, dados.alunoId, aula2, LocalDate.of(2026, 2, 10));
        geracaoCobrancaService.gerarCobrancaPorAula(dados.professorId, dados.alunoId, aula1, LocalDate.of(2026, 2, 10));

        List<Cobranca> cobrancas = cobrancaRepository.findByProfessorId(dados.professorId);
        assertThat(cobrancas).hasSize(2);
        assertThat(cobrancas).extracting(Cobranca::getAulaId)
                .containsExactlyInAnyOrder(aula1, aula2);
        assertThat(cobrancas).allMatch(c -> c.getPeriodoInicio().equals(LocalDate.of(2026, 2, 10)));
    }

    @Test
    void geracaoPorAulaNaoDuplicaEmChamadasConcorrentes() throws Exception {
        ContaTesteDados dados = criarContaAlunoPlanoPorAula(LocalDate.of(2026, 1, 1), 2, new BigDecimal("80.00"));
        UUID aula = inserirAula(dados.professorId, dados.alunoId, LocalDate.of(2026, 2, 10));
        LocalDate data = LocalDate.of(2026, 2, 10);

        executarConcorrente(2, () -> {
            geracaoCobrancaService.gerarCobrancaPorAula(dados.professorId, dados.alunoId, aula, data);
            return null;
        });

        assertThat(cobrancaRepository.findByProfessorId(dados.professorId)).hasSize(1);
    }

    @Test
    void geracaoMensalNaoDuplicaEmExecucoesConcorrentes() throws Exception {
        try {
            ContaTesteDados dados = criarContaAlunoPlanoMensal(LocalDate.of(2026, 1, 10), 10, new BigDecimal("100.00"));
            fixarClock(LocalDate.of(2026, 3, 15));

            executarConcorrente(2, () -> {
                geracaoCobrancaService.gerarCobrancasMensais();
                return null;
            });

            assertThat(cobrancaRepository.findByProfessorId(dados.professorId)).hasSize(3);
        } finally {
            mutableClock.reset();
        }
    }

    @Test
    void geracaoMensalCobreDias29e30EAnoBissextoEComum() {
        try {
            // 29 em ano bissexto
            ContaTesteDados bissexto = criarContaAlunoPlanoMensal(LocalDate.of(2024, 1, 29), 28, new BigDecimal("100.00"));
            fixarClock(LocalDate.of(2024, 3, 15));
            geracaoCobrancaService.gerarCobrancasMensais();
            // ciclo 2024-03-29 inicia após 2024-03-15, portanto não deve ser gerado
            assertThat(cobrancaRepository.findByProfessorId(bissexto.professorId))
                    .extracting(Cobranca::getPeriodoInicio)
                    .containsExactlyInAnyOrder(LocalDate.of(2024, 1, 29), LocalDate.of(2024, 2, 29));

            // 29 em ano comum e 30
            ContaTesteDados comum29 = criarContaAlunoPlanoMensal(LocalDate.of(2025, 1, 29), 28, new BigDecimal("100.00"));
            ContaTesteDados dia30 = criarContaAlunoPlanoMensal(LocalDate.of(2025, 1, 30), 28, new BigDecimal("100.00"));
            fixarClock(LocalDate.of(2025, 4, 15));
            geracaoCobrancaService.gerarCobrancasMensais();
            // ciclos com início após 2025-04-15 não devem ser gerados
            assertThat(cobrancaRepository.findByProfessorId(comum29.professorId))
                    .extracting(Cobranca::getPeriodoInicio)
                    .containsExactlyInAnyOrder(LocalDate.of(2025, 1, 29), LocalDate.of(2025, 2, 28), LocalDate.of(2025, 3, 29));
            assertThat(cobrancaRepository.findByProfessorId(dia30.professorId))
                    .extracting(Cobranca::getPeriodoInicio)
                    .containsExactlyInAnyOrder(LocalDate.of(2025, 1, 30), LocalDate.of(2025, 2, 28), LocalDate.of(2025, 3, 30));
        } finally {
            mutableClock.reset();
        }
    }

    @Test
    void geracaoMensalRecuperaVariosCiclosEDoisPlanosSimultaneos() throws Exception {
        try {
            ContaTesteDados p1 = criarContaAlunoPlanoMensal(LocalDate.of(2026, 1, 10), 10, new BigDecimal("100.00"));
            ContaTesteDados p2 = criarContaAlunoPlanoMensal(LocalDate.of(2026, 2, 5), 5, new BigDecimal("90.00"));
            fixarClock(LocalDate.of(2026, 6, 20));

            executarConcorrente(2, () -> {
                geracaoCobrancaService.gerarCobrancasMensais();
                return null;
            });

            assertThat(cobrancaRepository.findByProfessorId(p1.professorId)).hasSize(6); // jan-jun
            assertThat(cobrancaRepository.findByProfessorId(p2.professorId)).hasSize(5); // fev-jun
        } finally {
            mutableClock.reset();
        }
    }

    @Test
    void geracaoPorAulaRespeitaRegrasDePlanoEVencimento() throws Exception {
        ContaTesteDados porAula = criarContaAlunoPlanoPorAula(LocalDate.of(2026, 1, 1), 0, new BigDecimal("80.00"));
        UUID aulaPrazo0 = inserirAula(porAula.professorId, porAula.alunoId, LocalDate.of(2026, 2, 10));
        geracaoCobrancaService.gerarCobrancaPorAula(porAula.professorId, porAula.alunoId, aulaPrazo0, LocalDate.of(2026, 2, 10));
        assertThat(cobrancaRepository.findByProfessorId(porAula.professorId)).singleElement()
                .satisfies(c -> assertThat(c.getVencimentoEm()).isEqualTo(LocalDate.of(2026, 2, 10)));

        ContaTesteDados prazoPositivo = criarContaAlunoPlanoPorAula(LocalDate.of(2026, 1, 1), 3, new BigDecimal("80.00"));
        UUID aulaPrazo3 = inserirAula(prazoPositivo.professorId, prazoPositivo.alunoId, LocalDate.of(2026, 2, 10));
        geracaoCobrancaService.gerarCobrancaPorAula(prazoPositivo.professorId, prazoPositivo.alunoId, aulaPrazo3, LocalDate.of(2026, 2, 10));
        assertThat(cobrancaRepository.findByProfessorId(prazoPositivo.professorId)).singleElement()
                .satisfies(c -> assertThat(c.getVencimentoEm()).isEqualTo(LocalDate.of(2026, 2, 13)));

        // plano mensal não gera
        ContaTesteDados mensal = criarContaAlunoPlanoMensal(LocalDate.of(2026, 1, 1), 10, new BigDecimal("100.00"));
        UUID aulaMensal = inserirAula(mensal.professorId, mensal.alunoId, LocalDate.of(2026, 2, 10));
        geracaoCobrancaService.gerarCobrancaPorAula(mensal.professorId, mensal.alunoId, aulaMensal, LocalDate.of(2026, 2, 10));
        assertThat(cobrancaRepository.findByProfessorId(mensal.professorId)).isEmpty();

        // plano encerrado não gera
        ContaTesteDados encerrado = criarContaAlunoPlanoPorAula(LocalDate.of(2026, 1, 1), 0, new BigDecimal("80.00"));
        PlanoPagamento plano = planoRepository.findById(encerrado.planoId).orElseThrow();
        plano.encerrar(LocalDate.of(2026, 1, 20));
        planoRepository.saveAndFlush(plano);
        UUID aulaEnc = inserirAula(encerrado.professorId, encerrado.alunoId, LocalDate.of(2026, 2, 10));
        geracaoCobrancaService.gerarCobrancaPorAula(encerrado.professorId, encerrado.alunoId, aulaEnc, LocalDate.of(2026, 2, 10));
        assertThat(cobrancaRepository.findByProfessorId(encerrado.professorId)).isEmpty();
    }

    @Test
    void geracaoPorAulaRejeitaAulaDeOutroAlunoOuProfessor() throws Exception {
        ContaTesteDados dono = criarContaAlunoPlanoPorAula(LocalDate.of(2026, 1, 1), 0, new BigDecimal("80.00"));
        ContaTesteDados outro = criarContaAlunoPlanoPorAula(LocalDate.of(2026, 1, 1), 0, new BigDecimal("80.00"));

        // Aula pertence a outro professor/aluno: bloqueio/revalidação na aplicação impede geração inconsistente.
        UUID aulaOutroProfessor = inserirAula(outro.professorId, outro.alunoId, LocalDate.of(2026, 2, 10));
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                geracaoCobrancaService.gerarCobrancaPorAula(dono.professorId, dono.alunoId, aulaOutroProfessor, LocalDate.of(2026, 2, 10)))
                .isInstanceOfSatisfying(br.com.compasso.shared.error.ApiException.class, ex -> {
                    assertThat(ex.status().value()).isEqualTo(404);
                    assertThat(ex.code()).isEqualTo(br.com.compasso.shared.error.ErrorCodes.NOT_FOUND);
                });
        assertThat(cobrancaRepository.findByProfessorId(dono.professorId)).isEmpty();

        // Aluno diferente do mesmo professor: tentativa não deve gerar cobrança para o plano do aluno A.
        ContaTeste conta = contaComAceite();
        JsonNode alunoA = criarAluno(conta, alunoAdulto("MesmoProf A"));
        JsonNode alunoB = criarAluno(conta, alunoAdulto("MesmoProf B"));
        UUID professorId = professorIdDaConta(conta);
        UUID alunoAId = UUID.fromString(alunoA.get("id").asText());
        UUID alunoBId = UUID.fromString(alunoB.get("id").asText());
        Map<String, Object> planoAlunoA = new LinkedHashMap<>(planoPorAula());
        planoAlunoA.put("prazoVencimentoDias", 0);
        criarPlano(conta, alunoA, planoAlunoA);
        UUID aulaDoAlunoB = inserirAula(professorId, alunoBId, LocalDate.of(2026, 3, 10));
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                geracaoCobrancaService.gerarCobrancaPorAula(professorId, alunoAId, aulaDoAlunoB, LocalDate.of(2026, 3, 10)))
                .isInstanceOfSatisfying(br.com.compasso.shared.error.ApiException.class, ex -> {
                    assertThat(ex.status().value()).isEqualTo(404);
                    assertThat(ex.code()).isEqualTo(br.com.compasso.shared.error.ErrorCodes.NOT_FOUND);
                });
        assertThat(cobrancaRepository.findByProfessorId(professorId)).isEmpty();
    }

    @Test
    void geracaoMensalConflitoUnicidadeAtomicoMantemTransacaoConsistente() {
        try {
            ContaTesteDados dados = criarContaAlunoPlanoMensal(LocalDate.of(2026, 1, 10), 10, new BigDecimal("100.00"));
            fixarClock(LocalDate.of(2026, 2, 15));
            geracaoCobrancaService.gerarCobrancasMensais();

            // Repetição após já existir: idempotente (inserção atômica, sem abortar transação PostgreSQL).
            geracaoCobrancaService.gerarCobrancasMensais();
            List<Cobranca> cobrancas = cobrancaRepository.findByProfessorId(dados.professorId);
            assertThat(cobrancas).hasSize(2);
            assertThat(cobrancas).extracting(Cobranca::getPeriodoInicio)
                    .containsExactlyInAnyOrder(LocalDate.of(2026, 1, 10), LocalDate.of(2026, 2, 10));

            // Conflito real de unicidade no banco tratado atomicamente; mesma transação segue consistente.
            TransactionTemplate tx = new TransactionTemplate(transactionManager);
            Integer count = tx.execute(status -> {
                Cobranca existente = cobrancaRepository.findByProfessorId(dados.professorId).get(0);
                int inseridos = cobrancaRepository.inserirMensalSeNaoExistir(
                        UUID.randomUUID(), existente.getProfessorId(), existente.getAlunoId(), existente.getPlanoId(),
                        existente.getValor(), existente.getVencimentoEm(), existente.getPeriodoInicio(), existente.getPeriodoFim(),
                        br.com.compasso.financeiro.entity.CobrancaStatus.PENDENTE.valorBanco());
                assertThat(inseridos).isZero();
                return cobrancaRepository.findByProfessorId(dados.professorId).size();
            });
            assertThat(count).isEqualTo(2);
            assertThat(cobrancaRepository.findByProfessorId(dados.professorId)).hasSize(2);
        } finally {
            mutableClock.reset();
        }
    }

    @Test
    void geracaoPorAulaConflitoUnicidadeAtomicoENaoMascaraOutrasViolacoes() {
        ContaTesteDados dados = criarContaAlunoPlanoPorAula(LocalDate.of(2026, 1, 1), 0, new BigDecimal("80.00"));
        UUID aulaId = inserirAula(dados.professorId, dados.alunoId, LocalDate.of(2026, 2, 10));

        geracaoCobrancaService.gerarCobrancaPorAula(dados.professorId, dados.alunoId, aulaId, LocalDate.of(2026, 2, 10));
        // Repetição após já existir.
        geracaoCobrancaService.gerarCobrancaPorAula(dados.professorId, dados.alunoId, aulaId, LocalDate.of(2026, 2, 10));
        assertThat(cobrancaRepository.findByProfessorId(dados.professorId)).hasSize(1);

        // Conflito de unicidade esperado tratado atomicamente (ON CONFLICT DO NOTHING) sem invalidar transação.
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        Integer count = tx.execute(status -> {
            int inseridos = cobrancaRepository.inserirPorAulaSeNaoExistir(
                    UUID.randomUUID(), dados.professorId, dados.alunoId, dados.planoId, aulaId,
                    new BigDecimal("80.00"), LocalDate.of(2026, 2, 10), LocalDate.of(2026, 2, 10), LocalDate.of(2026, 2, 10),
                    br.com.compasso.financeiro.entity.CobrancaStatus.PENDENTE.valorBanco());
            assertThat(inseridos).isZero();
            return cobrancaRepository.findByProfessorId(dados.professorId).size();
        });
        assertThat(count).isEqualTo(1);

        // Outra violação de integridade (FK de aula inexistente) não deve ser mascarada como idempotência/sucesso.
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                tx.execute(status -> cobrancaRepository.inserirPorAulaSeNaoExistir(
                        UUID.randomUUID(), dados.professorId, dados.alunoId, dados.planoId, UUID.randomUUID(),
                        new BigDecimal("80.00"), LocalDate.of(2026, 2, 11), LocalDate.of(2026, 2, 11), LocalDate.of(2026, 2, 11),
                        br.com.compasso.financeiro.entity.CobrancaStatus.PENDENTE.valorBanco())))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThat(cobrancaRepository.findByProfessorId(dados.professorId)).hasSize(1);
    }

    @Test
    void baixaConcorrenteECancelamentoConcorrenteNaoGeramEstadoInconsistente() throws Exception {
        ContaTeste conta = contaComAceite();
        JsonNode aluno = criarAluno(conta, alunoAdulto("Concorrencia"));
        JsonNode plano = criarPlano(conta, aluno, planoMensal());
        UUID professorId = professorIdDaConta(conta);
        UUID usuarioId = usuarioIdDaConta(conta);
        UUID alunoId = UUID.fromString(aluno.get("id").asText());
        UUID planoId = UUID.fromString(plano.get("id").asText());

        // duas baixas simultâneas
        UUID cBaixa = inserirCobranca(professorId, alunoId, planoId, new BigDecimal("150.00"),
                LocalDate.of(2026, 1, 10), LocalDate.of(2026, 1, 10), LocalDate.of(2026, 2, 9),
                CobrancaStatus.PENDENTE, null, null, null);
        executarConcorrenteIgnorandoConflitos(2, () -> {
            financeiroService.baixar(usuarioId, cBaixa,
                    new br.com.compasso.financeiro.dto.CobrancaBaixaRequest(LocalDate.now(ZONA), "PIX"));
            return null;
        });
        Cobranca aposBaixas = cobrancaRepository.findById(cBaixa).orElseThrow();
        assertThat(aposBaixas.getStatus()).isEqualTo(CobrancaStatus.PAGO);

        // baixa x cancelamento simultâneos
        UUID cMix = inserirCobranca(professorId, alunoId, planoId, new BigDecimal("150.00"),
                LocalDate.of(2026, 2, 10), LocalDate.of(2026, 2, 10), LocalDate.of(2026, 3, 9),
                CobrancaStatus.PENDENTE, null, null, null);
        executarConcorrenteDistintoIgnorandoConflitos(
                () -> { financeiroService.baixar(usuarioId, cMix,
                        new br.com.compasso.financeiro.dto.CobrancaBaixaRequest(LocalDate.now(ZONA), "PIX")); return null; },
                () -> { financeiroService.cancelar(usuarioId, cMix); return null; });
        Cobranca aposMix = cobrancaRepository.findById(cMix).orElseThrow();
        assertThat(aposMix.getStatus()).isIn(CobrancaStatus.PAGO, CobrancaStatus.CANCELADO);
    }

    @Test
    void exportacaoCsvNeutralizaFormulaComEspacosEEscapaCampos() throws Exception {
        ContaTeste conta = contaComAceite();
        JsonNode aluno = criarAluno(conta, alunoAdulto("Ana \"A\" \nB"));
        JsonNode plano = criarPlano(conta, aluno, planoMensal());
        UUID professorId = professorIdDaConta(conta);
        UUID alunoId = UUID.fromString(aluno.get("id").asText());
        UUID planoId = UUID.fromString(plano.get("id").asText());

        inserirCobranca(professorId, alunoId, planoId, new BigDecimal("150.00"),
                LocalDate.of(2026, 1, 10), LocalDate.of(2026, 1, 10), LocalDate.of(2026, 2, 9),
                CobrancaStatus.PAGO, LocalDate.of(2026, 2, 5), "  +PIX", "  @cmd");

        MvcResult resultado = postJson("/api/v1/cobrancas/exportacao",
                Map.of("mes", "2026-02", "senha", SENHA_PADRAO), conta.csrf(), conta.sessao())
                .andExpect(status().isOk())
                .andReturn();

        String csv = resultado.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(csv).contains("'  +PIX");
        assertThat(csv).contains("'  @cmd");
        assertThat(csv).contains("\"Ana \"\"A\"\" \nB\"");
    }

    @Test
    void cobrancaMensalCanceladaNaoPodeSerGeradaNovamenteAutomaticamente() {
        try {
            ContaTesteDados dados = criarContaAlunoPlanoMensal(LocalDate.of(2026, 1, 10), 10, new BigDecimal("100.00"));
            fixarClock(LocalDate.of(2026, 1, 15));
            geracaoCobrancaService.gerarCobrancasMensais();

            List<Cobranca> antes = cobrancaRepository.findByProfessorId(dados.professorId);
            assertThat(antes).hasSize(1);
            Cobranca cobranca = antes.get(0);
            cobranca.cancelar();
            cobrancaRepository.saveAndFlush(cobranca);

            // Nova execução do gerador automático não deve recriar cobrança para o mesmo plano/período.
            geracaoCobrancaService.gerarCobrancasMensais();
            geracaoCobrancaService.gerarCobrancasMensais();

            List<Cobranca> depois = cobrancaRepository.findByProfessorId(dados.professorId);
            assertThat(depois).hasSize(1);
            assertThat(depois.get(0).getId()).isEqualTo(cobranca.getId());
            assertThat(depois.get(0).getStatus()).isEqualTo(CobrancaStatus.CANCELADO);
            assertThat(depois.get(0).getPeriodoInicio()).isEqualTo(LocalDate.of(2026, 1, 10));
        } finally {
            mutableClock.reset();
        }
    }

    @Test
    void criacaoConcorrenteDePlanoAtivoMantemUnicoPlanoEConflitoEspecifico() throws Exception {
        ContaTeste conta = contaComAceite();
        JsonNode aluno = criarAluno(conta, alunoAdulto("Concorrente Plano"));
        UUID alunoId = UUID.fromString(aluno.get("id").asText());
        UUID professorId = professorIdDaConta(conta);

        java.util.concurrent.atomic.AtomicInteger conflitos = new java.util.concurrent.atomic.AtomicInteger();
        java.util.concurrent.atomic.AtomicInteger sucessos = new java.util.concurrent.atomic.AtomicInteger();
        java.util.concurrent.atomic.AtomicReference<String> mensagemConflito = new java.util.concurrent.atomic.AtomicReference<>();

        executarConcorrente(2, () -> {
            try {
                postJson("/api/v1/alunos/" + alunoId + "/planos", planoMensal(), conta.csrf(), conta.sessao())
                        .andExpect(result -> {
                            int status = result.getResponse().getStatus();
                            if (status == 201) {
                                sucessos.incrementAndGet();
                            } else if (status == 409) {
                                conflitos.incrementAndGet();
                                String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
                                mensagemConflito.set(body);
                            } else {
                                throw new AssertionError("Status inesperado: " + status);
                            }
                        });
                return null;
            } catch (Exception e) {
                throw e;
            }
        });

        assertThat(sucessos.get() + conflitos.get()).isEqualTo(2);
        assertThat(planoRepository.findByAlunoIdAndProfessorId(alunoId, professorId)).hasSize(1);
        // Pelo menos uma tentativa deve ter criado; a outra, se conflitante, deve ser conflito de plano ativo.
        if (conflitos.get() > 0) {
            assertThat(mensagemConflito.get()).contains("plano ativo");
        }
    }

    @Test
    void concorrenciaFinanceiroEAlteracaoOuCancelamentoDeAulaMantemConsistencia() throws Exception {
        ContaTesteDados dados = criarContaAlunoPlanoPorAula(LocalDate.of(2026, 1, 1), 0, new BigDecimal("80.00"));
        UUID aulaId = inserirAula(dados.professorId, dados.alunoId, LocalDate.of(2026, 2, 10));
        geracaoCobrancaService.gerarCobrancaPorAula(dados.professorId, dados.alunoId, aulaId, LocalDate.of(2026, 2, 10));
        assertThat(cobrancaRepository.findByProfessorId(dados.professorId)).hasSize(1);

        // Concorrência: baixa financeira vs atualização/cancelamento de aula (operações reais no banco/PostgreSQL).
        // Triggers/locks devem evitar estado inconsistente e deadlock.
        UUID cobrancaId = cobrancaRepository.findByProfessorId(dados.professorId).get(0).getId();
        UUID usuarioId = jdbcTemplate.queryForObject(
                "SELECT u.id_usuario FROM professor p JOIN usuario u ON u.id_usuario = p.id_usuario WHERE p.id_professor = ?",
                UUID.class, dados.professorId);

        executarConcorrenteDistintoIgnorandoConflitos(
                () -> {
                    financeiroService.baixar(usuarioId, cobrancaId,
                            new br.com.compasso.financeiro.dto.CobrancaBaixaRequest(LocalDate.now(ZONA), "PIX"));
                    return null;
                },
                () -> {
                    jdbcTemplate.update("UPDATE aula SET status = 'cancelada' WHERE id_aula = ?", aulaId);
                    return null;
                });

        Cobranca finalCobranca = cobrancaRepository.findById(cobrancaId).orElseThrow();
        // Estado final deve ser consistente (pago conforme baixa bem-sucedida, ou pendente se baixa não prevaleceu)
        assertThat(finalCobranca.getStatus()).isIn(CobrancaStatus.PAGO, CobrancaStatus.PENDENTE, CobrancaStatus.CANCELADO);
        // Nunca deve restar combinação inconsistente proibida; se aula cancelada persistiu, cobrança pendente não pode permanecer.
        String statusAula = jdbcTemplate.queryForObject("SELECT status FROM aula WHERE id_aula = ?", String.class, aulaId);
        if ("cancelada".equals(statusAula)) {
            assertThat(finalCobranca.getStatus()).isNotEqualTo(CobrancaStatus.PENDENTE);
        }
    }

    // --- helpers ---

    private void fixarClock(LocalDate hojeSaoPaulo) {
        mutableClock.fixar(hojeSaoPaulo);
    }

    private ContaTesteDados criarContaAlunoPlanoMensal(LocalDate inicio, int diaVencimento, BigDecimal valor) {
        return criarContaAlunoPlano(PeriodicidadePlano.MENSAL, inicio, diaVencimento, null, valor);
    }

    private ContaTesteDados criarContaAlunoPlanoPorAula(LocalDate inicio, Integer prazo, BigDecimal valor) {
        return criarContaAlunoPlano(PeriodicidadePlano.POR_AULA, inicio, null, prazo, valor);
    }

    private ContaTesteDados criarContaAlunoPlano(PeriodicidadePlano periodicidade, LocalDate inicio, Integer diaVencimento,
            Integer prazo, BigDecimal valor) {
        try {
            ContaTeste conta = contaComAceite();
            JsonNode aluno = criarAluno(conta, alunoAdulto("Aluno " + UUID.randomUUID()));
            UUID professorId = professorIdDaConta(conta);
            UUID alunoId = UUID.fromString(aluno.get("id").asText());
            PlanoPagamento plano = new PlanoPagamento(professorId, alunoId, periodicidade, valor, inicio, diaVencimento, prazo);
            plano = planoRepository.save(plano);
            return new ContaTesteDados(conta, professorId, alunoId, plano.getId());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private JsonNode criarPlano(ContaTeste conta, JsonNode aluno, Map<String, Object> corpo) throws Exception {
        MvcResult resultado = postJson("/api/v1/alunos/" + aluno.get("id").asText() + "/planos", corpo,
                conta.csrf(), conta.sessao())
                .andExpect(status().isCreated())
                .andReturn();
        return json(resultado);
    }

    private UUID inserirCobranca(UUID professorId, UUID alunoId, UUID planoId, BigDecimal valor,
            LocalDate vencimentoEm, LocalDate periodoInicio, LocalDate periodoFim,
            CobrancaStatus status, LocalDate dataPagamento, String formaPagamento, String descricao) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO pagamento
                    (id_pagamento, professor_id, aluno_id, plano_id, valor, vencimento_em,
                     periodo_inicio, periodo_fim, status, data_pagamento, forma_pagamento, descricao)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                RETURNING id_pagamento
                """, UUID.class,
                UUID.randomUUID(), professorId, alunoId, planoId, valor, vencimentoEm,
                periodoInicio, periodoFim, status.valorBanco(), dataPagamento, formaPagamento, descricao);
    }

    private UUID inserirAula(UUID professorId, UUID alunoId, LocalDate dataLocal) {
        UUID aulaId = UUID.randomUUID();
        OffsetDateTime inicio = dataLocal.atTime(10, 0).atZone(ZONA).toOffsetDateTime();
        OffsetDateTime fim = dataLocal.atTime(11, 0).atZone(ZONA).toOffsetDateTime();
        jdbcTemplate.update("""
                INSERT INTO aula (id_aula, professor_id, aluno_id, modalidade_id, inicio_em, fim_em, status)
                VALUES (?, ?, ?, NULL, ?, ?, 'agendada')
                """, aulaId, professorId, alunoId, inicio, fim);
        return aulaId;
    }

    private Map<String, Object> planoMensal() {
        Map<String, Object> plano = new LinkedHashMap<>();
        plano.put("periodicidade", "MENSAL");
        plano.put("valor", new BigDecimal("150.00"));
        plano.put("inicioEm", "2026-01-10");
        plano.put("diaVencimento", 10);
        plano.put("prazoVencimentoDias", null);
        return plano;
    }

    private Map<String, Object> planoPorAula() {
        Map<String, Object> plano = new LinkedHashMap<>();
        plano.put("periodicidade", "POR_AULA");
        plano.put("valor", new BigDecimal("80.00"));
        plano.put("inicioEm", "2026-01-12");
        plano.put("diaVencimento", null);
        plano.put("prazoVencimentoDias", 7);
        return plano;
    }

    private Map<String, Object> alunoAdulto(String nome) {
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("nome", nome);
        corpo.put("telefone", "11988887777");
        corpo.put("dataNascimento", "1990-05-10");
        corpo.put("email", "ana+" + UUID.randomUUID() + "@exemplo.test");
        corpo.put("observacoes", "Obs");
        corpo.put("responsavelLegal", null);
        return corpo;
    }

    private JsonNode criarAluno(ContaTeste conta, Map<String, Object> corpo) throws Exception {
        MvcResult resultado = postJson("/api/v1/alunos", corpo, conta.csrf(), conta.sessao())
                .andExpect(status().isCreated())
                .andReturn();
        return json(resultado);
    }

    private UUID professorIdDaConta(ContaTeste conta) {
        return jdbcTemplate.queryForObject("""
                SELECT p.id_professor
                  FROM professor p
                  JOIN usuario u ON u.id_usuario = p.id_usuario
                 WHERE u.email = ?
                """, UUID.class, conta.email());
    }

    private UUID usuarioIdDaConta(ContaTeste conta) {
        return jdbcTemplate.queryForObject("SELECT id_usuario FROM usuario WHERE email = ?", UUID.class, conta.email());
    }

    private void executarConcorrenteIgnorandoConflitos(int threads, Callable<Void> tarefa) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        try {
            CountDownLatch pronto = new CountDownLatch(threads);
            CountDownLatch iniciar = new CountDownLatch(1);
            List<Future<Void>> futures = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                futures.add(executor.submit(() -> {
                    pronto.countDown();
                    iniciar.await(10, TimeUnit.SECONDS);
                    try {
                        return tarefa.call();
                    } catch (br.com.compasso.shared.error.ApiException e) {
                        if (br.com.compasso.shared.error.ErrorCodes.CONFLICT.equals(e.code())) {
                            return null;
                        }
                        throw e;
                    } catch (org.springframework.dao.DataIntegrityViolationException e) {
                        // corrida perdida: proteção de integridade/estado impediu transição inconsistente
                        return null;
                    }
                }));
            }
            assertThat(pronto.await(10, TimeUnit.SECONDS)).isTrue();
            iniciar.countDown();
            for (Future<Void> f : futures) {
                f.get(20, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
        }
    }

    @SafeVarargs
    private void executarConcorrenteDistintoIgnorandoConflitos(Callable<Void>... tarefas) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(tarefas.length);
        try {
            CountDownLatch pronto = new CountDownLatch(tarefas.length);
            CountDownLatch iniciar = new CountDownLatch(1);
            List<Future<Void>> futures = new ArrayList<>();
            for (Callable<Void> tarefa : tarefas) {
                futures.add(executor.submit(() -> {
                    pronto.countDown();
                    iniciar.await(10, TimeUnit.SECONDS);
                    try {
                        return tarefa.call();
                    } catch (br.com.compasso.shared.error.ApiException e) {
                        if (br.com.compasso.shared.error.ErrorCodes.CONFLICT.equals(e.code())) {
                            return null;
                        }
                        throw e;
                    } catch (org.springframework.dao.DataIntegrityViolationException e) {
                        // corrida perdida: proteção de integridade/estado impediu transição inconsistente
                        return null;
                    }
                }));
            }
            assertThat(pronto.await(10, TimeUnit.SECONDS)).isTrue();
            iniciar.countDown();
            for (Future<Void> f : futures) {
                f.get(20, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
        }
    }

    private ContaTeste contaComAceite() throws Exception {
        ContaTeste conta = contaAutenticada();
        aceitarDocumentosLegais(conta);
        return conta;
    }

    private void executarConcorrente(int threads, Callable<Void> tarefa) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        try {
            CountDownLatch pronto = new CountDownLatch(threads);
            CountDownLatch iniciar = new CountDownLatch(1);
            List<Future<Void>> futures = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                futures.add(executor.submit(() -> {
                    pronto.countDown();
                    iniciar.await(10, TimeUnit.SECONDS);
                    return tarefa.call();
                }));
            }
            assertThat(pronto.await(10, TimeUnit.SECONDS)).isTrue();
            iniciar.countDown();
            for (Future<Void> f : futures) {
                f.get(20, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
        }
    }

    private record ContaTesteDados(ContaTeste conta, UUID professorId, UUID alunoId, UUID planoId) {}
}
