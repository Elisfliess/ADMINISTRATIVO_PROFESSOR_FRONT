package br.com.compasso.aluno;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URI;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import br.com.compasso.support.AbstractApiIntegrationTest;
import jakarta.servlet.http.Cookie;
import tools.jackson.databind.JsonNode;

/**
 * Testes de API do núcleo do módulo aluno (SPEC aluno, RF11–RF14, RN15/RN16):
 * cadastro, listagem/busca, consulta e edição parcial, sempre limitados ao
 * professor da sessão. Aluno de outro professor é tratado como inexistente.
 */
class AlunoApiTest extends AbstractApiIntegrationTest {

    private static final String CAMINHO = "/api/v1/alunos";
    private static final MediaType MERGE_PATCH = MediaType.valueOf("application/merge-patch+json");

    @Test
    void requisicaoSemSessaoRetorna401EmProblemDetails() throws Exception {
        MvcResult resultado = getJson(CAMINHO, null)
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(header().exists("X-Request-Id"))
                .andReturn();

        assertThat(json(resultado).get("status").asInt()).isEqualTo(401);
    }

    @Test
    void operacoesExigemAceiteDosDocumentosLegais() throws Exception {
        ContaTeste conta = contaAutenticada();

        getJson(CAMINHO, conta.sessao())
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.code").value("LEGAL_ACCEPTANCE_REQUIRED"));

        postJson(CAMINHO, alunoAdulto("Ana Souza"), conta.csrf(), conta.sessao())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("LEGAL_ACCEPTANCE_REQUIRED"));
    }

    @Test
    void cadastroDeAdultoRetorna201ComLocationECamposDoContrato() throws Exception {
        ContaTeste conta = contaComAceite();

        MvcResult resultado = postJson(CAMINHO, alunoAdulto("Ana Souza"), conta.csrf(), conta.sessao())
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern(".*/api/v1/alunos/[0-9a-f-]{36}$")))
                .andExpect(jsonPath("$.nome").value("Ana Souza"))
                .andExpect(jsonPath("$.telefone").value("11988887777"))
                .andExpect(jsonPath("$.dataNascimento").value("1990-05-10"))
                .andExpect(jsonPath("$.email").value("ana@exemplo.test"))
                .andExpect(jsonPath("$.observacoes").value("Prefere aulas à tarde."))
                .andExpect(jsonPath("$.ativo").value(true))
                .andReturn();

        JsonNode aluno = json(resultado);
        assertThat(aluno.get("responsavelLegal").isNull()).isTrue();
        assertThat(aluno.properties().stream().map(Map.Entry::getKey))
                .containsExactlyInAnyOrder("id", "nome", "telefone", "dataNascimento", "email",
                        "observacoes", "responsavelLegal", "ativo");

        UUID id = UUID.fromString(aluno.get("id").asText());
        assertThat(jdbcTemplate.queryForObject(
                "SELECT professor_id FROM aluno WHERE id_aluno = ?", UUID.class, id))
                .isEqualTo(professorIdDaConta(conta));

        String caminhoCriado = URI.create(resultado.getResponse().getHeader("Location")).getPath();
        getJson(caminhoCriado, conta.sessao())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.nome").value("Ana Souza"));
    }

    @Test
    void menorSemResponsavelLegalRetorna400ENaoPersiste() throws Exception {
        ContaTeste conta = contaComAceite();

        postJson(CAMINHO, alunoMenor("Caio Menor"), conta.csrf(), conta.sessao())
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].field").value("responsavelLegal"))
                .andExpect(jsonPath("$.errors[0].code").value("REQUIRED"));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM aluno WHERE professor_id = ?", Long.class, professorIdDaConta(conta)))
                .isZero();
    }

    @Test
    void menorComResponsavelLegalRetorna201() throws Exception {
        ContaTeste conta = contaComAceite();
        Map<String, Object> corpo = alunoMenor("Caio Menor");
        corpo.put("responsavelLegal", Map.of("nome", "Maria Souza", "contato", "11977776666"));

        postJson(CAMINHO, corpo, conta.csrf(), conta.sessao())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.responsavelLegal.nome").value("Maria Souza"))
                .andExpect(jsonPath("$.responsavelLegal.contato").value("11977776666"));
    }

    @Test
    void propriedadesDesconhecidasInclusiveProfessorIdSaoRejeitadas() throws Exception {
        ContaTeste conta = contaComAceite();

        Map<String, Object> comProfessorId = alunoAdulto("Ana Souza");
        comProfessorId.put("professorId", UUID.randomUUID().toString());

        postJson(CAMINHO, comProfessorId, conta.csrf(), conta.sessao())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_JSON"));

        Map<String, Object> comCpf = alunoAdulto("Ana Souza");
        comCpf.put("cpf", "12345678901");

        postJson(CAMINHO, comCpf, conta.csrf(), conta.sessao())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_JSON"));
    }

    @Test
    void dataDeNascimentoFuturaRetorna400() throws Exception {
        ContaTeste conta = contaComAceite();
        Map<String, Object> corpo = alunoAdulto("Ana Souza");
        corpo.put("dataNascimento", LocalDate.now().plusDays(1).toString());

        postJson(CAMINHO, corpo, conta.csrf(), conta.sessao())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].field").value("dataNascimento"));
    }

    @Test
    void listagemPorPadraoRetornaSomenteAtivosEFiltraPorNomeEAtivo() throws Exception {
        ContaTeste conta = contaComAceite();

        JsonNode anaBeatriz = criarAluno(conta, alunoAdulto("ANA Beatriz"));
        criarAluno(conta, alunoAdulto("Ana Souza"));
        JsonNode bruno = criarAluno(conta, alunoAdulto("Bruno Lima"));

        getJson(CAMINHO, conta.sessao())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3));

        getJson(CAMINHO + "?nome=ANA", conta.sessao())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[*].nome")
                        .value(containsInAnyOrder("ANA Beatriz", "Ana Souza")));

        getJson(CAMINHO + "?page=0&size=1&sort=nome,asc", conta.sessao())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(false))
                .andExpect(jsonPath("$.content[0].nome").value("ANA Beatriz"));

        getJson(CAMINHO + "?page=2&size=1&sort=nome,asc", conta.sessao())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].nome").value("Bruno Lima"))
                .andExpect(jsonPath("$.last").value(true));

        // O PR 1 ainda não expõe a inativação; o estado é semeado direto para
        // exercitar o filtro e o padrão da listagem sobre a coluna persistida.
        jdbcTemplate.update("UPDATE aluno SET ativo = false WHERE id_aluno = ? AND professor_id = ?",
                UUID.fromString(bruno.get("id").asText()), professorIdDaConta(conta));

        getJson(CAMINHO, conta.sessao())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[*].nome")
                        .value(containsInAnyOrder("ANA Beatriz", "Ana Souza")));

        getJson(CAMINHO + "?ativo=true&nome=ANA", conta.sessao())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));

        getJson(CAMINHO + "?ativo=false", conta.sessao())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].nome").value("Bruno Lima"));

        getJson(CAMINHO + "?size=1000", conta.sessao())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(100));

        getJson(CAMINHO + "?sort=email,asc", conta.sessao())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].field").value("sort"));

        assertThat(anaBeatriz.get("nome").asText()).isEqualTo("ANA Beatriz");
    }

    @Test
    void alunoDeOutroProfessorEhTratadoComoInexistente() throws Exception {
        ContaTeste dono = contaComAceite();
        ContaTeste outro = contaComAceite();

        JsonNode aluno = criarAluno(dono, alunoAdulto("Aluno do dono"));
        String caminho = CAMINHO + "/" + aluno.get("id").asText();

        getJson(caminho, outro.sessao())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));

        patchJson(caminho, Map.of("nome", "Nome Indevido"), outro)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));

        getJson(CAMINHO, outro.sessao())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        getJson(CAMINHO + "?professorId=" + professorIdDaConta(dono), outro.sessao())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        getJson(caminho, dono.sessao())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Aluno do dono"));

        assertThat(jdbcTemplate.queryForObject("SELECT nome FROM aluno WHERE id_aluno = ?", String.class,
                UUID.fromString(aluno.get("id").asText()))).isEqualTo("Aluno do dono");
    }

    @Test
    void patchAtualizaSomenteCamposEnviadosELimpaOpcionaisComNull() throws Exception {
        ContaTeste conta = contaComAceite();
        JsonNode criado = criarAluno(conta, alunoAdulto("Ana Souza"));
        String caminho = CAMINHO + "/" + criado.get("id").asText();

        patchJson(caminho, Map.of("nome", "Ana Souza Lima"), conta)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Ana Souza Lima"))
                .andExpect(jsonPath("$.telefone").value("11988887777"))
                .andExpect(jsonPath("$.email").value("ana@exemplo.test"))
                .andExpect(jsonPath("$.observacoes").value("Prefere aulas à tarde."))
                .andExpect(jsonPath("$.dataNascimento").value("1990-05-10"));

        patchJson(caminho, "{}", conta)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Ana Souza Lima"));

        Map<String, Object> limpeza = new HashMap<>();
        limpeza.put("email", null);
        limpeza.put("observacoes", null);

        MvcResult resultado = patchJson(caminho, limpeza, conta)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Ana Souza Lima"))
                .andReturn();

        JsonNode limpo = json(resultado);
        assertThat(limpo.get("email").isNull()).isTrue();
        assertThat(limpo.get("observacoes").isNull()).isTrue();
    }

    @Test
    void patchComObrigatorioNuloOuEmBrancoRetorna400() throws Exception {
        ContaTeste conta = contaComAceite();
        JsonNode criado = criarAluno(conta, alunoAdulto("Ana Souza"));
        String caminho = CAMINHO + "/" + criado.get("id").asText();

        Map<String, Object> nomeNulo = new HashMap<>();
        nomeNulo.put("nome", null);
        patchJson(caminho, nomeNulo, conta)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].field").value("nome"))
                .andExpect(jsonPath("$.errors[0].code").value("REQUIRED"));

        patchJson(caminho, Map.of("telefone", "   "), conta)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("telefone"))
                .andExpect(jsonPath("$.errors[0].code").value("REQUIRED"));

        Map<String, Object> dataNula = new HashMap<>();
        dataNula.put("dataNascimento", null);
        patchJson(caminho, dataNula, conta)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("dataNascimento"))
                .andExpect(jsonPath("$.errors[0].code").value("REQUIRED"));

        patchJson(caminho, Map.of("email", "invalido"), conta)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("email"))
                .andExpect(jsonPath("$.errors[0].code").value("INVALID_FORMAT"));

        patchJson(caminho, Map.of("professorId", UUID.randomUUID().toString()), conta)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_JSON"));
    }

    @Test
    void patchRevalidaResponsavelAoMudarFaixaEtaria() throws Exception {
        ContaTeste conta = contaComAceite();
        JsonNode criado = criarAluno(conta, alunoAdulto("Ana Souza"));
        String caminho = CAMINHO + "/" + criado.get("id").asText();
        String dataDeMenor = LocalDate.now().minusYears(10).toString();

        patchJson(caminho, Map.of("dataNascimento", dataDeMenor), conta)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("responsavelLegal"))
                .andExpect(jsonPath("$.errors[0].code").value("REQUIRED"));

        Map<String, Object> viraMenor = new LinkedHashMap<>();
        viraMenor.put("dataNascimento", dataDeMenor);
        viraMenor.put("responsavelLegal", Map.of("nome", "Maria Souza", "contato", "11977776666"));

        patchJson(caminho, viraMenor, conta)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responsavelLegal.nome").value("Maria Souza"));

        Map<String, Object> removeResponsavel = new HashMap<>();
        removeResponsavel.put("responsavelLegal", null);
        patchJson(caminho, removeResponsavel, conta)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("responsavelLegal"));

        Map<String, Object> voltaAdulto = new LinkedHashMap<>();
        voltaAdulto.put("dataNascimento", "1990-05-10");
        voltaAdulto.put("responsavelLegal", null);

        MvcResult resultado = patchJson(caminho, voltaAdulto, conta)
                .andExpect(status().isOk())
                .andReturn();

        assertThat(json(resultado).get("responsavelLegal").isNull()).isTrue();
    }

    @Test
    void patchAtualizaResponsavelLegalParcialmenteEPreservaCamposOmitidos() throws Exception {
        ContaTeste conta = contaComAceite();
        Map<String, Object> corpo = alunoMenor("Caio Menor");
        corpo.put("responsavelLegal", Map.of("nome", "Maria Souza", "contato", "11977776666"));
        JsonNode criado = criarAluno(conta, corpo);
        String caminho = CAMINHO + "/" + criado.get("id").asText();

        patchJson(caminho, Map.of("responsavelLegal", Map.of("nome", "Maria Silva")), conta)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Caio Menor"))
                .andExpect(jsonPath("$.responsavelLegal.nome").value("Maria Silva"))
                .andExpect(jsonPath("$.responsavelLegal.contato").value("11977776666"));

        patchJson(caminho, Map.of("responsavelLegal", Map.of("contato", "11966665555")), conta)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responsavelLegal.nome").value("Maria Silva"))
                .andExpect(jsonPath("$.responsavelLegal.contato").value("11966665555"));

        patchJson(caminho, Map.of("responsavelLegal", Map.of()), conta)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responsavelLegal.nome").value("Maria Silva"))
                .andExpect(jsonPath("$.responsavelLegal.contato").value("11966665555"));

        Map<String, Object> responsavelNomeNulo = new HashMap<>();
        responsavelNomeNulo.put("nome", null);
        Map<String, Object> patchNomeNulo = new HashMap<>();
        patchNomeNulo.put("responsavelLegal", responsavelNomeNulo);

        patchJson(caminho, patchNomeNulo, conta)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("responsavelLegal.nome"))
                .andExpect(jsonPath("$.errors[0].code").value("REQUIRED"));

        Map<String, Object> removeResponsavel = new HashMap<>();
        removeResponsavel.put("responsavelLegal", null);
        patchJson(caminho, removeResponsavel, conta)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("responsavelLegal"))
                .andExpect(jsonPath("$.errors[0].code").value("REQUIRED"));

        Map<String, Object> voltaAdulto = new LinkedHashMap<>();
        voltaAdulto.put("dataNascimento", "1990-05-10");
        voltaAdulto.put("responsavelLegal", null);

        MvcResult removido = patchJson(caminho, voltaAdulto, conta)
                .andExpect(status().isOk())
                .andReturn();
        assertThat(json(removido).get("responsavelLegal").isNull()).isTrue();

        patchJson(caminho, Map.of("responsavelLegal", Map.of("nome", "João")), conta)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("responsavelLegal.contato"))
                .andExpect(jsonPath("$.errors[0].code").value("REQUIRED"));
    }

    @Test
    void alunoInexistenteOuCorpoIlegivelRetornaErroDoContrato() throws Exception {
        ContaTeste conta = contaComAceite();
        String inexistente = CAMINHO + "/" + UUID.randomUUID();

        getJson(inexistente, conta.sessao())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));

        patchJson(inexistente, Map.of("nome", "Ana"), conta)
                .andExpect(status().isNotFound());

        patchJson(CAMINHO + "/" + UUID.randomUUID(), "{", conta)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_JSON"));
    }

    private ContaTeste contaComAceite() throws Exception {
        ContaTeste conta = contaAutenticada();
        aceitarDocumentosLegais(conta);
        return conta;
    }

    private Map<String, Object> alunoAdulto(String nome) {
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("nome", nome);
        corpo.put("telefone", "11988887777");
        corpo.put("dataNascimento", "1990-05-10");
        corpo.put("email", "ana@exemplo.test");
        corpo.put("observacoes", "Prefere aulas à tarde.");
        corpo.put("responsavelLegal", null);
        return corpo;
    }

    private Map<String, Object> alunoMenor(String nome) {
        Map<String, Object> corpo = alunoAdulto(nome);
        corpo.put("dataNascimento", LocalDate.now().minusYears(10).toString());
        return corpo;
    }

    private JsonNode criarAluno(ContaTeste conta, Map<String, Object> corpo) throws Exception {
        MvcResult resultado = postJson(CAMINHO, corpo, conta.csrf(), conta.sessao())
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
}
