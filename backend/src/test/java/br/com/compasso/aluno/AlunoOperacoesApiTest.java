package br.com.compasso.aluno;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import br.com.compasso.support.AbstractApiIntegrationTest;
import jakarta.servlet.http.Cookie;
import tools.jackson.databind.JsonNode;

/**
 * Operações do ciclo de vida e de dados do aluno que não dependem de Agenda ou
 * Financeiro: reativação, exportação CSV e anonimização.
 */
class AlunoOperacoesApiTest extends AbstractApiIntegrationTest {

    private static final String CAMINHO = "/api/v1/alunos";
    private static final MediaType MERGE_PATCH = MediaType.parseMediaType("application/merge-patch+json");

    @Test
    void reativacaoAlteraSomenteOEstadoDoAluno() throws Exception {
        ContaTeste conta = contaComAceite();
        JsonNode criado = criarAluno(conta, alunoAdulto("Ana Souza"));
        UUID alunoId = UUID.fromString(criado.get("id").asText());

        jdbcTemplate.update("""
                UPDATE aluno
                   SET ativo = false,
                       inativado_em = CURRENT_TIMESTAMP
                 WHERE id_aluno = ?
                   AND professor_id = ?
                """, alunoId, professorIdDaConta(conta));

        postJson(CAMINHO + "/" + alunoId + "/reativacao", Map.of(), conta.csrf(), conta.sessao())
                .andExpect(status().isNoContent());

        getJson(CAMINHO + "/" + alunoId, conta.sessao())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(true));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT inativado_em IS NULL FROM aluno WHERE id_aluno = ?", Boolean.class, alunoId)).isTrue();

        postJson(CAMINHO + "/" + alunoId + "/reativacao", Map.of(), conta.csrf(), conta.sessao())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    @Test
    void exportacaoCsvExigeSenhaEUsaEscapingDoPadrao() throws Exception {
        ContaTeste conta = contaComAceite();
        Map<String, Object> corpo = alunoAdulto("Ana Souza");
        corpo.put("observacoes", "Aulas, \"online\" e à tarde\nlinha 2");
        JsonNode criado = criarAluno(conta, corpo);
        String caminho = CAMINHO + "/" + criado.get("id").asText();

        postJson(caminho + "/exportacao", Map.of("senha", "SenhaErrada1"), conta.csrf(), conta.sessao())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        postJson(caminho + "/exportacao", Map.of(), conta.csrf(), conta.sessao())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("senha"));

        MvcResult resultado = postJson(caminho + "/exportacao", Map.of("senha", SENHA_PADRAO),
                conta.csrf(), conta.sessao())
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, startsWith("text/csv")))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("attachment")))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        containsString("aluno-" + criado.get("id").asText() + ".csv")))
                .andReturn();

        String csv = resultado.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(csv).contains("id,nome,telefone,email,dataNascimento,observacoes,"
                + "responsavelNome,responsavelContato,ativo,inativadoEm,anonimizadoEm,criadoEm,atualizadoEm");
        assertThat(csv).contains("Ana Souza");
        assertThat(csv).contains("\"Aulas, \"\"online\"\" e à tarde\nlinha 2\"");
    }

    @Test
    void exportacaoNeutralizaFormulasSemAlterarDadosPersistidos() throws Exception {
        ContaTeste conta = contaComAceite();
        JsonNode criado = criarAluno(conta, alunoAdulto("Ana Souza"));
        UUID alunoId = UUID.fromString(criado.get("id").asText());

        jdbcTemplate.update("""
                UPDATE aluno
                   SET nome = ?,
                       telefone = ?,
                       email = ?,
                       observacoes = ?,
                       responsavel_nome = ?,
                       responsavel_contato = ?
                 WHERE id_aluno = ?
                """,
                "=1+1",
                "+5511999999999",
                "-soma@exemplo.test",
                "=HYPERLINK(\"http://exemplo.test\",\"clique\"),fim",
                "  @SUM(A1:A2)",
                "\t@contato",
                alunoId);

        String csv = exportarCsv(conta, alunoId);

        assertThat(csv).contains("'=1+1");
        assertThat(csv).contains("'+5511999999999");
        assertThat(csv).contains("'-soma@exemplo.test");
        assertThat(csv).contains("\"'=HYPERLINK(\"\"http://exemplo.test\"\",\"\"clique\"\"),fim\"");
        assertThat(csv).contains("'  @SUM(A1:A2)");
        assertThat(csv).contains("'\t@contato");

        assertThat(jdbcTemplate.queryForObject(
                "SELECT nome FROM aluno WHERE id_aluno = ?", String.class, alunoId)).isEqualTo("=1+1");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT observacoes FROM aluno WHERE id_aluno = ?", String.class, alunoId))
                .isEqualTo("=HYPERLINK(\"http://exemplo.test\",\"clique\"),fim");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT responsavel_nome FROM aluno WHERE id_aluno = ?", String.class, alunoId))
                .isEqualTo("  @SUM(A1:A2)");
    }

    @Test
    void exportacaoPreservaValoresComunsSemAlteracao() throws Exception {
        ContaTeste conta = contaComAceite();
        JsonNode criado = criarAluno(conta, alunoAdulto("Ana Souza"));

        String csv = exportarCsv(conta, UUID.fromString(criado.get("id").asText()));
        String[] registros = csv.split("\r\n", -1);

        assertThat(registros).hasSize(3);
        assertThat(registros[1]).startsWith(criado.get("id").asText()
                + ",Ana Souza,11988887777,ana@exemplo.test,1990-05-10,Prefere aulas à tarde.,,,true,");
        assertThat(registros[1]).doesNotContain("'");
        assertThat(registros[1]).doesNotContain("\"");
    }

    @Test
    void exportacaoUsaCrlfEntreRegistrosEPreservaQuebrasInternas() throws Exception {
        ContaTeste conta = contaComAceite();
        Map<String, Object> corpo = alunoAdulto("Ana Souza");
        corpo.put("observacoes", "Primeira linha\nSegunda linha");
        JsonNode criado = criarAluno(conta, corpo);

        String csv = exportarCsv(conta, UUID.fromString(criado.get("id").asText()));

        assertThat(csv).endsWith("\r\n");
        assertThat(csv).doesNotContain("\r\n\r\n");

        String[] registros = csv.split("\r\n", -1);
        assertThat(registros).hasSize(3);
        assertThat(registros[0]).isEqualTo("id,nome,telefone,email,dataNascimento,observacoes,"
                + "responsavelNome,responsavelContato,ativo,inativadoEm,anonimizadoEm,criadoEm,atualizadoEm");
        assertThat(registros[1]).contains("\"Primeira linha\nSegunda linha\"");
        assertThat(registros[2]).isEmpty();
    }

    @Test
    void previaAnonimizacaoDescreveCamposSemAlterarDados() throws Exception {
        ContaTeste conta = contaComAceite();
        Map<String, Object> corpo = alunoMenor("Caio Menor");
        corpo.put("responsavelLegal", Map.of("nome", "Maria Souza", "contato", "11977776666"));
        JsonNode criado = criarAluno(conta, corpo);
        String caminho = CAMINHO + "/" + criado.get("id").asText();

        getJson(caminho + "/anonimizacao/previa", conta.sessao())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dadosPessoais", containsInAnyOrder(
                        "nome", "telefone", "email", "dataNascimento", "observacoes",
                        "responsavelLegal.nome", "responsavelLegal.contato",
                        "responsavelLegal.informadoEm")))
                .andExpect(jsonPath("$.registrosPreservados", containsInAnyOrder(
                        "identificador do aluno", "professor", "aulas", "planos", "cobranças")));

        getJson(caminho, conta.sessao())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Caio Menor"))
                .andExpect(jsonPath("$.telefone").value("11988887777"))
                .andExpect(jsonPath("$.responsavelLegal.nome").value("Maria Souza"));
    }

    @Test
    void anonimizacaoExigeSenhaEConfirmacaoEPreservaIdentificadores() throws Exception {
        ContaTeste conta = contaComAceite();
        Map<String, Object> corpo = alunoMenor("Caio Menor");
        corpo.put("responsavelLegal", Map.of("nome", "Maria Souza", "contato", "11977776666"));
        JsonNode criado = criarAluno(conta, corpo);
        String caminho = CAMINHO + "/" + criado.get("id").asText();
        UUID alunoId = UUID.fromString(criado.get("id").asText());

        postJson(caminho + "/anonimizacao", Map.of("senha", "SenhaErrada1", "confirmar", true),
                conta.csrf(), conta.sessao())
                .andExpect(status().isForbidden());

        postJson(caminho + "/anonimizacao", Map.of("senha", SENHA_PADRAO, "confirmar", false),
                conta.csrf(), conta.sessao())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("confirmar"));

        postJson(caminho + "/anonimizacao", Map.of("senha", SENHA_PADRAO, "confirmar", true),
                conta.csrf(), conta.sessao())
                .andExpect(status().isNoContent());

        MvcResult resultado = getJson(caminho, conta.sessao())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Aluno anonimizado"))
                .andExpect(jsonPath("$.telefone").value(nullValue()))
                .andExpect(jsonPath("$.email").value(nullValue()))
                .andExpect(jsonPath("$.dataNascimento").value(nullValue()))
                .andExpect(jsonPath("$.observacoes").value(nullValue()))
                .andExpect(jsonPath("$.responsavelLegal").value(nullValue()))
                .andExpect(jsonPath("$.ativo").value(false))
                .andReturn();

        assertThat(json(resultado).get("id").asText()).isEqualTo(alunoId.toString());
        assertThat(jdbcTemplate.queryForObject(
                "SELECT anonimizado_em IS NOT NULL FROM aluno WHERE id_aluno = ?", Boolean.class, alunoId)).isTrue();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT professor_id FROM aluno WHERE id_aluno = ?", UUID.class, alunoId))
                .isEqualTo(professorIdDaConta(conta));
        assertThat(jdbcTemplate.queryForObject(
                "SELECT telefone IS NULL AND data_nascimento IS NULL FROM aluno WHERE id_aluno = ?",
                Boolean.class, alunoId)).isTrue();

        postJson(caminho + "/anonimizacao", Map.of("senha", SENHA_PADRAO, "confirmar", true),
                conta.csrf(), conta.sessao())
                .andExpect(status().isConflict());

        postJson(caminho + "/reativacao", Map.of(), conta.csrf(), conta.sessao())
                .andExpect(status().isConflict());

        patchJson(caminho, Map.of("nome", "Outro Nome"), conta)
                .andExpect(status().isConflict());

        getJson(caminho + "/anonimizacao/previa", conta.sessao())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dadosPessoais").isEmpty());
    }

    @Test
    void operacoesDeOutroProfessorRetornam404() throws Exception {
        ContaTeste dona = contaComAceite();
        ContaTeste outra = contaComAceite();
        JsonNode criado = criarAluno(dona, alunoAdulto("Ana Souza"));
        String caminho = CAMINHO + "/" + criado.get("id").asText();

        postJson(caminho + "/reativacao", Map.of(), outra.csrf(), outra.sessao())
                .andExpect(status().isNotFound());

        postJson(caminho + "/exportacao", Map.of("senha", SENHA_PADRAO), outra.csrf(), outra.sessao())
                .andExpect(status().isNotFound());

        postJson(caminho + "/anonimizacao", Map.of("senha", SENHA_PADRAO, "confirmar", true),
                outra.csrf(), outra.sessao())
                .andExpect(status().isNotFound());

        getJson(caminho + "/anonimizacao/previa", outra.sessao())
                .andExpect(status().isNotFound());

        getJson(caminho, dona.sessao())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Ana Souza"))
                .andExpect(jsonPath("$.ativo").value(true));
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

    private String exportarCsv(ContaTeste conta, UUID alunoId) throws Exception {
        MvcResult resultado = postJson(CAMINHO + "/" + alunoId + "/exportacao",
                Map.of("senha", SENHA_PADRAO), conta.csrf(), conta.sessao())
                .andExpect(status().isOk())
                .andReturn();
        return resultado.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private ResultActions patchJson(String caminho, Map<String, Object> corpo, ContaTeste conta) throws Exception {
        return mockMvc.perform(patch(caminho)
                .contentType(MERGE_PATCH)
                .content(objectMapper.writeValueAsString(corpo))
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
