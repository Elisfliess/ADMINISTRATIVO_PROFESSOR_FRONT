package br.com.compasso.professor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import br.com.compasso.support.AbstractApiIntegrationTest;
import tools.jackson.databind.JsonNode;

/**
 * Testes de API do módulo professor (SPEC professor, GET /professor): o titular
 * é sempre resolvido pela sessão, nunca por identificador enviado pelo cliente.
 */
class ProfessorApiTest extends AbstractApiIntegrationTest {

    @Test
    void consultaSemSessaoRetorna401EmProblemDetails() throws Exception {
        MvcResult resultado = getJson("/api/v1/professor", null)
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(header().exists("X-Request-Id"))
                .andReturn();

        JsonNode problema = json(resultado);
        assertThat(problema.get("status").asInt()).isEqualTo(401);
        assertThat(problema.get("requestId").asText())
                .isEqualTo(resultado.getResponse().getHeader("X-Request-Id"));
    }

    @Test
    void consultaRetornaSomenteOsCamposDoContratoDoTitularAutenticado() throws Exception {
        ContaTeste conta = contaAutenticada();
        aceitarDocumentosLegais(conta);

        MvcResult resultado = getJson("/api/v1/professor", conta.sessao())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Professor Teste"))
                .andExpect(jsonPath("$.razaoSocial").value("Professor Teste ME"))
                .andExpect(jsonPath("$.cnpj").value(conta.cnpj()))
                .andExpect(jsonPath("$.emailContato").value("contato." + conta.email()))
                .andReturn();

        JsonNode professor = json(resultado);
        assertThat(professor.get("instagramUrl").isNull()).isTrue();
        assertThat(professor.properties().stream().map(Map.Entry::getKey))
                .containsExactlyInAnyOrder("id", "nome", "razaoSocial", "cnpj", "emailContato",
                        "telefoneContato", "instagramUrl");
        assertThat(professor.get("cnpj").asText()).hasSize(14).containsOnlyDigits();

        UUID idEsperado = jdbcTemplate.queryForObject("""
                SELECT p.id_professor
                  FROM professor p
                  JOIN usuario u ON u.id_usuario = p.id_usuario
                 WHERE u.email = ?
                """, UUID.class, conta.email());
        assertThat(professor.get("id").asText()).isEqualTo(idEsperado.toString());
    }

    @Test
    void contasDiferentesNaoCompartilhamDadosEProfessorIdDoClienteEhIgnorado() throws Exception {
        ContaTeste primeira = contaAutenticada();
        ContaTeste segunda = contaAutenticada();
        aceitarDocumentosLegais(primeira);
        aceitarDocumentosLegais(segunda);

        UUID idSegunda = jdbcTemplate.queryForObject("""
                SELECT p.id_professor
                  FROM professor p
                  JOIN usuario u ON u.id_usuario = p.id_usuario
                 WHERE u.email = ?
                """, UUID.class, segunda.email());

        MvcResult resultado = getJson("/api/v1/professor?professorId=" + idSegunda, primeira.sessao())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cnpj").value(primeira.cnpj()))
                .andReturn();

        JsonNode professor = json(resultado);
        assertThat(professor.get("id").asText()).isNotEqualTo(idSegunda.toString());
        assertThat(professor.get("cnpj").asText()).isEqualTo(primeira.cnpj());

        getJson("/api/v1/professor", segunda.sessao())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cnpj").value(segunda.cnpj()));
    }
}
