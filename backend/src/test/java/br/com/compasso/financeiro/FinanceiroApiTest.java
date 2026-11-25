package br.com.compasso.financeiro;

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import br.com.compasso.support.AbstractApiIntegrationTest;
import br.com.compasso.financeiro.entity.CobrancaStatus;
import jakarta.servlet.http.Cookie;
import tools.jackson.databind.JsonNode;

/**
 * Testes de API do núcleo financeiro (SPEC financeiro, RF22–RF25, RN20–RN30):
 * criação e consulta de planos, regra de um plano ativo por aluno e impedimento
 * de edição após geração de cobrança, sempre no escopo do professor da sessão.
 */
class FinanceiroApiTest extends AbstractApiIntegrationTest {

    private static final String CAMINHO_PLANOS = "/api/v1/alunos/{alunoId}/planos";
    private static final MediaType MERGE_PATCH = MediaType.valueOf("application/merge-patch+json");

    @Test
    void operacaoSemSessaoRetorna401EmProblemDetails() throws Exception {
        String inexistente = "/api/v1/planos/" + UUID.randomUUID();

        getJson(inexistente, null)
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(header().exists("X-Request-Id"));
    }

    @Test
    void operacoesExigemAceiteDosDocumentosLegais() throws Exception {
        ContaTeste conta = contaAutenticada();

        getJson("/api/v1/planos/" + UUID.randomUUID(), conta.sessao())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("LEGAL_ACCEPTANCE_REQUIRED"));

        postJson("/api/v1/alunos/" + UUID.randomUUID() + "/planos", planoMensal(), conta.csrf(), conta.sessao())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("LEGAL_ACCEPTANCE_REQUIRED"));
    }

    @Test
    void planoMensalRetorna201ComCamposDoContrato() throws Exception {
        ContaTeste conta = contaComAceite();
        JsonNode aluno = criarAluno(conta, alunoAdulto("Ana Souza"));

        MvcResult resultado = postJson(CAMINHO_PLANOS.replace("{alunoId}", aluno.get("id").asText()),
                planoMensal(), conta.csrf(), conta.sessao())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.periodicidade").value("MENSAL"))
                .andExpect(jsonPath("$.valor").value(closeTo(150.0, 0.001)))
                .andExpect(jsonPath("$.inicioEm").value("2026-01-10"))
                .andExpect(jsonPath("$.diaVencimento").value(10))
                .andExpect(jsonPath("$.prazoVencimentoDias").value(nullValue()))
                .andExpect(jsonPath("$.status").value("ATIVO"))
                .andExpect(jsonPath("$.encerradoEm").value(nullValue()))
                .andReturn();

        JsonNode plano = json(resultado);
        org.assertj.core.api.Assertions.assertThat(plano.get("id").asText()).isNotBlank();
    }

    @Test
    void alunoComPlanoAtivoNaoPodeTerSegundoPlano() throws Exception {
        ContaTeste conta = contaComAceite();
        JsonNode aluno = criarAluno(conta, alunoAdulto("Ana Souza"));

        postJson(CAMINHO_PLANOS.replace("{alunoId}", aluno.get("id").asText()), planoMensal(), conta.csrf(), conta.sessao())
                .andExpect(status().isCreated());

        postJson(CAMINHO_PLANOS.replace("{alunoId}", aluno.get("id").asText()), planoMensal(), conta.csrf(), conta.sessao())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    @Test
    void planoPorAulaAceitaPrazoDeVencimento() throws Exception {
        ContaTeste conta = contaComAceite();
        JsonNode aluno = criarAluno(conta, alunoAdulto("Ana Souza"));

        postJson(CAMINHO_PLANOS.replace("{alunoId}", aluno.get("id").asText()), planoPorAula(), conta.csrf(), conta.sessao())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.periodicidade").value("POR_AULA"))
                .andExpect(jsonPath("$.valor").value(closeTo(80.0, 0.001)))
                .andExpect(jsonPath("$.diaVencimento").value(nullValue()))
                .andExpect(jsonPath("$.prazoVencimentoDias").value(7))
                .andExpect(jsonPath("$.status").value("ATIVO"));
    }

    @Test
    void configuracaoIncoerenteRetorna400ENaoPersistePlano() throws Exception {
        ContaTeste conta = contaComAceite();
        JsonNode aluno = criarAluno(conta, alunoAdulto("Ana Souza"));

        Map<String, Object> plano = planoMensal();
        plano.put("prazoVencimentoDias", 5);

        postJson(CAMINHO_PLANOS.replace("{alunoId}", aluno.get("id").asText()), plano, conta.csrf(), conta.sessao())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void planoInexistenteRetorna404() throws Exception {
        ContaTeste conta = contaComAceite();

        getJson("/api/v1/planos/" + UUID.randomUUID(), conta.sessao())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void planoDeAlunoInexistenteRetorna404NaCriacao() throws Exception {
        ContaTeste conta = contaComAceite();

        postJson("/api/v1/alunos/" + UUID.randomUUID() + "/planos", planoMensal(), conta.csrf(), conta.sessao())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void patchDePlanoSemCobrancasAplicaCamposEBuscaValidacao() throws Exception {
        ContaTeste conta = contaComAceite();
        JsonNode aluno = criarAluno(conta, alunoAdulto("Ana Souza"));

        MvcResult criado = postJson(CAMINHO_PLANOS.replace("{alunoId}", aluno.get("id").asText()), planoMensal(),
                conta.csrf(), conta.sessao())
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode plano = json(criado);
        String caminho = "/api/v1/planos/" + plano.get("id").asText();

        patchJson(caminho, Map.of("valor", 200.00, "inicioEm", "2026-02-01", "diaVencimento", 15), conta)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valor").value(closeTo(200.0, 0.001)))
                .andExpect(jsonPath("$.inicioEm").value("2026-02-01"))
                .andExpect(jsonPath("$.diaVencimento").value(15))
                .andExpect(jsonPath("$.status").value("ATIVO"));

        patchJson(caminho, Map.of("periodicidade", "MENSAL", "prazoVencimentoDias", 5), conta)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void patchDePlanoComCobrancaRetorna409() throws Exception {
        ContaTeste conta = contaComAceite();
        JsonNode aluno = criarAluno(conta, alunoAdulto("Ana Souza"));

        MvcResult criado = postJson(CAMINHO_PLANOS.replace("{alunoId}", aluno.get("id").asText()), planoMensal(),
                conta.csrf(), conta.sessao())
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode plano = json(criado);

        UUID professorId = professorIdDaConta(conta);
        jdbcTemplate.update("""
                INSERT INTO pagamento (id_pagamento, professor_id, aluno_id, plano_id, valor, vencimento_em, periodo_inicio, periodo_fim, status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'pendente')
                """,
                UUID.randomUUID(),
                professorId,
                UUID.fromString(aluno.get("id").asText()),
                UUID.fromString(plano.get("id").asText()),
                new BigDecimal("150.00"),
                LocalDate.of(2026, 1, 10),
                LocalDate.of(2026, 1, 10),
                LocalDate.of(2026, 2, 9));

        patchJson("/api/v1/planos/" + plano.get("id").asText(), Map.of("valor", 250.00), conta)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    @Test
    void encerramentoRetorna204EPlanoPassaAEscerrado() throws Exception {
        ContaTeste conta = contaComAceite();
        JsonNode aluno = criarAluno(conta, alunoAdulto("Ana Souza"));

        MvcResult criado = postJson(CAMINHO_PLANOS.replace("{alunoId}", aluno.get("id").asText()), planoMensal(),
                conta.csrf(), conta.sessao())
                .andExpect(status().isCreated())
                .andReturn();
        String caminho = "/api/v1/planos/" + json(criado).get("id").asText();

        postJson(caminho + "/encerramento", Map.of(), conta.csrf(), conta.sessao())
                .andExpect(status().isNoContent());

        getJson(caminho, conta.sessao())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ENCERRADO"))
                .andExpect(jsonPath("$.encerradoEm").isNotEmpty());
    }

    @Test
    void encerramentoDePlanoInexistenteRetorna404() throws Exception {
        ContaTeste conta = contaComAceite();

        postJson("/api/v1/planos/" + UUID.randomUUID() + "/encerramento", Map.of(), conta.csrf(), conta.sessao())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void planoDeOutroProfessorRetorna404() throws Exception {
        ContaTeste dono = contaComAceite();
        ContaTeste outro = contaComAceite();

        JsonNode aluno = criarAluno(dono, alunoAdulto("Aluno do dono"));
        String caminho = "/api/v1/alunos/" + aluno.get("id").asText() + "/planos";

        postJson(caminho, planoMensal(), dono.csrf(), dono.sessao())
                .andExpect(status().isCreated());

        getJson("/api/v1/planos/" + UUID.randomUUID(), outro.sessao())
                .andExpect(status().isNotFound());

        postJson(caminho, planoMensal(), outro.csrf(), outro.sessao())
                .andExpect(status().isNotFound());
    }

    @Test
    void listarCobrancasRetornaCobrancasComFiltros() throws Exception {
        ContaTeste conta = contaComAceite();
        JsonNode aluno = criarAluno(conta, alunoAdulto("Ana Souza"));
        JsonNode plano = criarPlano(conta, aluno, planoMensal());

        UUID professorId = professorIdDaConta(conta);
        UUID cobrancaId = inserirCobranca(professorId, UUID.fromString(aluno.get("id").asText()),
                UUID.fromString(plano.get("id").asText()), new BigDecimal("150.00"),
                LocalDate.of(2026, 1, 10), LocalDate.of(2026, 1, 10), LocalDate.of(2026, 2, 9),
                CobrancaStatus.PAGO, LocalDate.of(2026, 1, 12), "PIX");

        getJson("/api/v1/cobrancas", conta.sessao())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(cobrancaId.toString()))
                .andExpect(jsonPath("$.content[0].status").value("PAGO"))
                .andExpect(jsonPath("$.content[0].recebidoEm").value("2026-01-12"))
                .andExpect(jsonPath("$.content[0].formaPagamento").value("PIX"));

        getJson("/api/v1/cobrancas?mes=2026-01&status=PAGO", conta.sessao())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        getJson("/api/v1/cobrancas?mes=2026-03", conta.sessao())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        getJson("/api/v1/cobrancas?mes=2026/01", conta.sessao())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        getJson("/api/v1/cobrancas?status=QUALQUER", conta.sessao())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void cobrancaInexistenteRetorna404() throws Exception {
        ContaTeste conta = contaComAceite();

        getJson("/api/v1/cobrancas/" + UUID.randomUUID(), conta.sessao())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void cobrancaDeOutroProfessorRetorna404() throws Exception {
        ContaTeste dono = contaComAceite();
        ContaTeste outro = contaComAceite();

        JsonNode aluno = criarAluno(dono, alunoAdulto("Aluno do dono"));
        JsonNode plano = criarPlano(dono, aluno, planoMensal());

        UUID cobrancaId = inserirCobranca(professorIdDaConta(dono), UUID.fromString(aluno.get("id").asText()),
                UUID.fromString(plano.get("id").asText()), new BigDecimal("150.00"),
                LocalDate.of(2026, 1, 10), LocalDate.of(2026, 1, 10), LocalDate.of(2026, 2, 9),
                CobrancaStatus.PENDENTE, null, null);

        getJson("/api/v1/cobrancas/" + cobrancaId, outro.sessao())
                .andExpect(status().isNotFound());

        postJson("/api/v1/cobrancas/" + cobrancaId + "/baixa", Map.of("recebidoEm", "2026-01-12"), outro.csrf(),
                outro.sessao())
                .andExpect(status().isNotFound());

        postJson("/api/v1/cobrancas/" + cobrancaId + "/cancelamento", Map.of(), outro.csrf(), outro.sessao())
                .andExpect(status().isNotFound());
    }

    @Test
    void baixaRegistraRecebimentoIntegral() throws Exception {
        ContaTeste conta = contaComAceite();
        JsonNode aluno = criarAluno(conta, alunoAdulto("Ana Souza"));
        JsonNode plano = criarPlano(conta, aluno, planoMensal());

        UUID cobrancaId = inserirCobranca(professorIdDaConta(conta), UUID.fromString(aluno.get("id").asText()),
                UUID.fromString(plano.get("id").asText()), new BigDecimal("150.00"),
                LocalDate.of(2026, 1, 10), LocalDate.of(2026, 1, 10), LocalDate.of(2026, 2, 9),
                CobrancaStatus.PENDENTE, null, null);

        postJson("/api/v1/cobrancas/" + cobrancaId + "/baixa",
                Map.of("recebidoEm", "2026-01-12", "formaPagamento", "PIX"), conta.csrf(), conta.sessao())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAGO"))
                .andExpect(jsonPath("$.recebidoEm").value("2026-01-12"))
                .andExpect(jsonPath("$.formaPagamento").value("PIX"));

        getJson("/api/v1/cobrancas/" + cobrancaId, conta.sessao())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAGO"));
    }

    @Test
    void baixaDeCobrancaJaBaixadaRetorna409() throws Exception {
        ContaTeste conta = contaComAceite();
        JsonNode aluno = criarAluno(conta, alunoAdulto("Ana Souza"));
        JsonNode plano = criarPlano(conta, aluno, planoMensal());

        UUID cobrancaId = inserirCobranca(professorIdDaConta(conta), UUID.fromString(aluno.get("id").asText()),
                UUID.fromString(plano.get("id").asText()), new BigDecimal("150.00"),
                LocalDate.of(2026, 1, 10), LocalDate.of(2026, 1, 10), LocalDate.of(2026, 2, 9),
                CobrancaStatus.PAGO, LocalDate.of(2026, 1, 12), "PIX");

        postJson("/api/v1/cobrancas/" + cobrancaId + "/baixa", Map.of("recebidoEm", "2026-01-15"), conta.csrf(),
                conta.sessao())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    @Test
    void cancelamentoRetorna204EAtualizaSituacao() throws Exception {
        ContaTeste conta = contaComAceite();
        JsonNode aluno = criarAluno(conta, alunoAdulto("Ana Souza"));
        JsonNode plano = criarPlano(conta, aluno, planoMensal());

        UUID cobrancaId = inserirCobranca(professorIdDaConta(conta), UUID.fromString(aluno.get("id").asText()),
                UUID.fromString(plano.get("id").asText()), new BigDecimal("150.00"),
                LocalDate.of(2026, 1, 10), LocalDate.of(2026, 1, 10), LocalDate.of(2026, 2, 9),
                CobrancaStatus.PENDENTE, null, null);

        postJson("/api/v1/cobrancas/" + cobrancaId + "/cancelamento", Map.of(), conta.csrf(), conta.sessao())
                .andExpect(status().isNoContent());

        getJson("/api/v1/cobrancas/" + cobrancaId, conta.sessao())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADO"));

        postJson("/api/v1/cobrancas/" + cobrancaId + "/cancelamento", Map.of(), conta.csrf(), conta.sessao())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    @Test
    void cancelamentoDeCobrancaPagaRetorna409() throws Exception {
        ContaTeste conta = contaComAceite();
        JsonNode aluno = criarAluno(conta, alunoAdulto("Ana Souza"));
        JsonNode plano = criarPlano(conta, aluno, planoMensal());

        UUID cobrancaId = inserirCobranca(professorIdDaConta(conta), UUID.fromString(aluno.get("id").asText()),
                UUID.fromString(plano.get("id").asText()), new BigDecimal("150.00"),
                LocalDate.of(2026, 1, 10), LocalDate.of(2026, 1, 10), LocalDate.of(2026, 2, 9),
                CobrancaStatus.PAGO, LocalDate.of(2026, 1, 12), "PIX");

        postJson("/api/v1/cobrancas/" + cobrancaId + "/cancelamento", Map.of(), conta.csrf(), conta.sessao())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    private JsonNode criarPlano(ContaTeste conta, JsonNode aluno, Map<String, Object> corpo) throws Exception {
        MvcResult resultado = postJson(CAMINHO_PLANOS.replace("{alunoId}", aluno.get("id").asText()), corpo,
                conta.csrf(), conta.sessao())
                .andExpect(status().isCreated())
                .andReturn();
        return json(resultado);
    }

    private UUID inserirCobranca(UUID professorId,
            UUID alunoId,
            UUID planoId,
            java.math.BigDecimal valor,
            LocalDate vencimentoEm,
            LocalDate periodoInicio,
            LocalDate periodoFim,
            CobrancaStatus status,
            LocalDate dataPagamento,
            String formaPagamento) {

        return jdbcTemplate.queryForObject("""
                INSERT INTO pagamento
                    (id_pagamento, professor_id, aluno_id, plano_id, valor, vencimento_em,
                     periodo_inicio, periodo_fim, status, data_pagamento, forma_pagamento)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                RETURNING id_pagamento
                """, UUID.class,
                UUID.randomUUID(), professorId, alunoId, planoId, valor, vencimentoEm,
                periodoInicio, periodoFim, status.valorBanco(), dataPagamento, formaPagamento);
    }

    private Map<String, Object> planoMensal() {
        Map<String, Object> plano = new java.util.LinkedHashMap<>();
        plano.put("periodicidade", "MENSAL");
        plano.put("valor", new BigDecimal("150.00"));
        plano.put("inicioEm", "2026-01-10");
        plano.put("diaVencimento", 10);
        plano.put("prazoVencimentoDias", null);
        return plano;
    }

    private Map<String, Object> planoPorAula() {
        Map<String, Object> plano = new java.util.LinkedHashMap<>();
        plano.put("periodicidade", "POR_AULA");
        plano.put("valor", new BigDecimal("80.00"));
        plano.put("inicioEm", "2026-01-12");
        plano.put("diaVencimento", null);
        plano.put("prazoVencimentoDias", 7);
        return plano;
    }

    private Map<String, Object> alunoAdulto(String nome) {
        Map<String, Object> corpo = new java.util.LinkedHashMap<>();
        corpo.put("nome", nome);
        corpo.put("telefone", "11988887777");
        corpo.put("dataNascimento", "1990-05-10");
        corpo.put("email", "ana@exemplo.test");
        corpo.put("observacoes", "Prefere aulas à tarde.");
        corpo.put("responsavelLegal", null);
        return corpo;
    }

    private JsonNode criarAluno(ContaTeste conta, Map<String, Object> corpo) throws Exception {
        MvcResult resultado = postJson("/api/v1/alunos", corpo, conta.csrf(), conta.sessao())
                .andExpect(status().isCreated())
                .andReturn();
        return json(resultado);
    }

    private ResultActions patchJson(String caminho, Map<String, Object> corpo, ContaTeste conta) throws Exception {
        return patchJson(caminho, objectMapper.writeValueAsString(corpo), conta);
    }

    private ResultActions patchJson(String caminho, String corpo, ContaTeste conta) throws Exception {
        return mockMvc.perform(patch(caminho)
                .contentType(MERGE_PATCH)
                .content(corpo)
                .cookie(new Cookie("XSRF-TOKEN", conta.csrf().token()))
                .header("X-XSRF-TOKEN", conta.csrf().token())
                .session(conta.sessao()));
    }

    private UUID professorIdDaConta(ContaTeste conta) {
        return jdbcTemplate.queryForObject("""
                SELECT p.id_professor
                  FROM professor p
                  JOIN usuario u ON u.id_usuario = p.id_usuario
                 WHERE u.email = ?
                """, UUID.class, conta.email());
    }

    private ContaTeste contaComAceite() throws Exception {
        ContaTeste conta = contaAutenticada();
        aceitarDocumentosLegais(conta);
        return conta;
    }
}
