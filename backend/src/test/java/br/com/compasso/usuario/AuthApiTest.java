package br.com.compasso.usuario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
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
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;

import br.com.compasso.support.AbstractApiIntegrationTest;
import jakarta.servlet.http.Cookie;
import tools.jackson.databind.JsonNode;

/**
 * Testes de API do fluxo de conta e autenticação (SPEC usuario, contrato
 * /auth/*, /legal/acceptances). Cobrem apenas comportamento exposto no contrato.
 */
class AuthApiTest extends AbstractApiIntegrationTest {

    private static final String PROBLEM_JSON = "application/problem+json";

    @Test
    void registroCriaContaPendenteDeVerificacaoSemSessao() throws Exception {
        Csrf csrf = csrf();
        String email = emailUnico();
        String cnpj = cnpjUnico();

        MvcResult resultado = postJson("/api/v1/auth/register", registro(email, SENHA_PADRAO, cnpj), csrf, null)
                .andExpect(status().isAccepted())
                .andExpect(content().string(""))
                .andReturn();

        assertThat(resultado.getResponse().getCookie("JSESSIONID")).isNull();

        Map<String, Object> usuario = jdbcTemplate.queryForMap(
                "SELECT id_usuario, email, senha_hash, email_verificado_em FROM usuario WHERE email = ?", email);
        assertThat(usuario.get("email")).isEqualTo(email);
        assertThat(usuario.get("email_verificado_em")).isNull();
        assertThat((String) usuario.get("senha_hash")).startsWith("$2");

        Integer professores = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM professor WHERE id_usuario = ? AND cnpj = ?", Integer.class,
                usuario.get("id_usuario"), cnpj);
        assertThat(professores).isEqualTo(1);

        assertThat(tokenDeVerificacao(email)).isNotBlank();
    }

    @Test
    void registroDeEmailExistenteRespondeIgualEEnviaAvisoDeSeguranca() throws Exception {
        Csrf csrf = csrf();
        String email = emailUnico();
        String cnpj = cnpjUnico();

        MvcResult primeiro = postJson("/api/v1/auth/register", registro(email, SENHA_PADRAO, cnpj), csrf, null)
                .andExpect(status().isAccepted())
                .andReturn();

        MvcResult segundo = postJson("/api/v1/auth/register", registro(email, SENHA_PADRAO, cnpjUnico()), csrf, null)
                .andExpect(status().isAccepted())
                .andReturn();

        assertThat(segundo.getResponse().getContentAsString())
                .isEqualTo(primeiro.getResponse().getContentAsString());

        verify(emailSender).enviar(argThat(mensagem -> mensagem.destinatario().equals(email)
                && mensagem.assunto().equals("Aviso de segurança do Compasso")));

        Integer tokens = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                  FROM token_conta t
                  JOIN usuario u ON u.id_usuario = t.usuario_id
                 WHERE u.email = ? AND t.tipo = 'verificacao_email'
                """, Integer.class, email);
        assertThat(tokens).isEqualTo(1);

        Integer professores = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                  FROM professor p
                  JOIN usuario u ON u.id_usuario = p.id_usuario
                 WHERE u.email = ?
                """, Integer.class, email);
        assertThat(professores).isEqualTo(1);
    }

    @Test
    void registroComCnpjInvalidoRetorna400ComCodigoDeCampo() throws Exception {
        MvcResult resultado = postJson("/api/v1/auth/register",
                registro(emailUnico(), SENHA_PADRAO, "11111111111111"), csrf(), null)
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].field").value("cnpj"))
                .andExpect(jsonPath("$.errors[0].code").value("CNPJ_INVALID"))
                .andReturn();

        assertProblemaCompleto(resultado);
    }

    @Test
    void registroComSenhaFracaRetorna400() throws Exception {
        postJson("/api/v1/auth/register", registro(emailUnico(), "abc", cnpjUnico()), csrf(), null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].field").value("senha"))
                .andExpect(jsonPath("$.errors[0].code").value("PASSWORD_TOO_SHORT"));
    }

    @Test
    void registroComPropriedadeDesconhecidaRetorna400() throws Exception {
        Map<String, Object> corpo = new LinkedHashMap<>(registro(emailUnico(), SENHA_PADRAO, cnpjUnico()));
        corpo.put("professorId", UUID.randomUUID().toString());

        postJson("/api/v1/auth/register", corpo, csrf(), null)
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("MALFORMED_JSON"));
    }

    @Test
    void verificacaoComTokenInvalidoRetorna409() throws Exception {
        postJson("/api/v1/auth/verify-email", Map.of("token", "token-inexistente"), csrf(), null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("TOKEN_INVALID_OR_EXPIRED"));
    }

    @Test
    void loginComEmailNaoVerificadoRetorna403() throws Exception {
        Csrf csrf = csrf();
        String email = emailUnico();
        registrar(email, SENHA_PADRAO, cnpjUnico(), csrf);

        postJson(CAMINHO_LOGIN, Map.of("email", email, "senha", SENHA_PADRAO), csrf, null)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("EMAIL_NOT_VERIFIED"));
    }

    @Test
    void loginValidoCriaSessaoEConsultaIdentidade() throws Exception {
        ContaTeste conta = contaAutenticada();

        MvcResult me = getJson("/api/v1/auth/me", conta.sessao())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(conta.email()))
                .andExpect(jsonPath("$.emailVerificado").value(true))
                .andExpect(jsonPath("$.termosAceitos").value(false))
                .andExpect(header().exists("X-Request-Id"))
                .andReturn();

        JsonNode corpo = json(me);
        assertThat(corpo.properties().stream().map(Map.Entry::getKey))
                .containsExactlyInAnyOrder("id", "email", "emailVerificado", "termosAceitos");
        assertThat(UUID.fromString(corpo.get("id").asText())).isNotNull();
    }

    @Test
    void loginComCredenciaisInvalidasRetornaMesmaRespostaParaEmailExistenteEInexistente() throws Exception {
        Csrf csrf = csrf();
        ContaTeste conta = contaAutenticada();

        MvcResult senhaErrada = postJson(CAMINHO_LOGIN,
                Map.of("email", conta.email(), "senha", "SenhaErrada1"), csrf, null)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andReturn();

        MvcResult emailInexistente = postJson(CAMINHO_LOGIN,
                Map.of("email", emailUnico(), "senha", SENHA_PADRAO), csrf, null)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andReturn();

        assertThat(json(emailInexistente).get("detail").asText())
                .isEqualTo(json(senhaErrada).get("detail").asText());
    }

    @Test
    void cincoFalhasBloqueiamContaExistentePorQuinzeMinutos() throws Exception {
        Csrf csrf = csrf();
        ContaTeste conta = contaAutenticada();

        for (int tentativa = 1; tentativa <= 4; tentativa++) {
            postJson(CAMINHO_LOGIN, Map.of("email", conta.email(), "senha", "SenhaErrada1"), csrf, null)
                    .andExpect(status().isUnauthorized());
        }

        postJson(CAMINHO_LOGIN, Map.of("email", conta.email(), "senha", "SenhaErrada1"), csrf, null)
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("ACCOUNT_LOCKED"))
                .andExpect(header().exists("Retry-After"));

        postJson(CAMINHO_LOGIN, Map.of("email", conta.email(), "senha", conta.senha()), csrf, null)
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("ACCOUNT_LOCKED"));
    }

    @Test
    void cincoFalhasParaEmailInexistenteTambemRetornam429() throws Exception {
        Csrf csrf = csrf();
        String email = emailUnico();

        for (int tentativa = 1; tentativa <= 4; tentativa++) {
            postJson(CAMINHO_LOGIN, Map.of("email", email, "senha", SENHA_PADRAO), csrf, null)
                    .andExpect(status().isUnauthorized());
        }

        postJson(CAMINHO_LOGIN, Map.of("email", email, "senha", SENHA_PADRAO), csrf, null)
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("ACCOUNT_LOCKED"))
                .andExpect(header().exists("Retry-After"));
    }

    @Test
    void loginSemCsrfRetorna403() throws Exception {
        postJsonSemCsrf(CAMINHO_LOGIN, Map.of("email", emailUnico(), "senha", SENHA_PADRAO))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void logoutInvalidaSessaoNoServidor() throws Exception {
        ContaTeste conta = contaAutenticada();

        MvcResult resultado = postJson("/api/v1/auth/logout", Map.of(), conta.csrf(), conta.sessao())
                .andExpect(status().isNoContent())
                .andReturn();

        Cookie cookieSessao = resultado.getResponse().getCookie("JSESSIONID");
        assertThat(cookieSessao).isNotNull();
        assertThat(cookieSessao.getMaxAge()).isZero();

        getJson("/api/v1/auth/me", conta.sessao())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void aceiteLegalPendenteBloqueiaRecursosELiberaAposRegistro() throws Exception {
        ContaTeste conta = contaAutenticada();

        getJson("/api/v1/professor", conta.sessao())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("LEGAL_ACCEPTANCE_REQUIRED"));

        postJson("/api/v1/legal/acceptances",
                Map.of("termosVersao", "9.9", "politicaVersao", "1.0"), conta.csrf(), conta.sessao())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("LEGAL_VERSION_NOT_FOUND"));

        aceitarDocumentosLegais(conta);

        getJson("/api/v1/professor", conta.sessao()).andExpect(status().isOk());
        getJson("/api/v1/auth/me", conta.sessao())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.termosAceitos").value(true));

        Integer aceites = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                  FROM aceite_documento_legal a
                  JOIN usuario u ON u.id_usuario = a.usuario_id
                 WHERE u.email = ?
                """, Integer.class, conta.email());
        assertThat(aceites).isEqualTo(2);
    }

    @Test
    void recuperacaoDeSenhaUsaTokenUnicoEInvalidaSenhaAnterior() throws Exception {
        Csrf csrf = csrf();
        ContaTeste conta = contaAutenticada();
        String novaSenha = "NovaSenha123";

        postJson("/api/v1/auth/password-recovery", Map.of("email", conta.email()), csrf, null)
                .andExpect(status().isAccepted());

        String token = tokenDeRecuperacao(conta.email());

        postJson("/api/v1/auth/password-recovery/confirm",
                Map.of("token", token, "novaSenha", novaSenha), csrf, null)
                .andExpect(status().isNoContent());

        login(conta.email(), novaSenha, csrf);

        postJson("/api/v1/auth/password-recovery/confirm",
                Map.of("token", token, "novaSenha", "OutraSenha123"), csrf, null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("TOKEN_INVALID_OR_EXPIRED"));

        postJson(CAMINHO_LOGIN, Map.of("email", conta.email(), "senha", conta.senha()), csrf, null)
                .andExpect(status().isUnauthorized());
    }

    @Test
    void recuperacaoParaEmailInexistenteRespondeGenericoSemEnviarEmail() throws Exception {
        String email = emailUnico();

        postJson("/api/v1/auth/password-recovery", Map.of("email", email), csrf(), null)
                .andExpect(status().isAccepted());

        verify(emailSender, never()).enviar(argThat(mensagem -> mensagem.destinatario().equals(email)));
    }

    @Test
    void reenvioDeVerificacaoRespeitaEsperaDeQuarentaECincoSegundos() throws Exception {
        Csrf csrf = csrf();
        String email = emailUnico();
        registrar(email, SENHA_PADRAO, cnpjUnico(), csrf);

        postJson("/api/v1/auth/verification/resend", Map.of("email", email), csrf, null)
                .andExpect(status().isAccepted());

        postJson("/api/v1/auth/verification/resend", Map.of("email", email), csrf, null)
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("RESEND_TOO_SOON"))
                .andExpect(header().exists("Retry-After"));
    }

    @Test
    void registroComCnpjAlfanumericoValidoAceitaEConsultaLoNormalizado() throws Exception {
        String cnpj = cnpjAlfanumericoUnico();

        assertThat(cnpj).hasSize(14).containsPattern("[A-Z]");

        ContaTeste conta = contaAutenticada(cnpj);
        aceitarDocumentosLegais(conta);

        getJson("/api/v1/professor", conta.sessao())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cnpj").value(cnpj));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT cnpj FROM professor WHERE cnpj = ?", String.class, cnpj))
                .isEqualTo(cnpj);
    }

    @Test
    void registroComCnpjAlfanumericoComDigitoVerificadorInvalidoRetorna400() throws Exception {
        String valido = cnpjAlfanumericoUnico();
        char ultimoDigito = valido.charAt(13);
        String invalido = valido.substring(0, 13) + (ultimoDigito == '0' ? '1' : '0');

        postJson("/api/v1/auth/register", registro(emailUnico(), SENHA_PADRAO, invalido), csrf(), null)
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].field").value("cnpj"))
                .andExpect(jsonPath("$.errors[0].code").value("CNPJ_INVALID"));
    }

    @Test
    void registroComCnpjAlfanumericoEmMinusculasRetorna400DeFormato() throws Exception {
        String minusculo = cnpjAlfanumericoUnico().toLowerCase();

        postJson("/api/v1/auth/register", registro(emailUnico(), SENHA_PADRAO, minusculo), csrf(), null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].field").value("cnpj"))
                .andExpect(jsonPath("$.errors[0].code").value("INVALID_FORMAT"));
    }

    @Test
    void registroComSenhaAcimaDeSetentaEDoisBytesRetorna400() throws Exception {
        String senha = "Senha1" + "á".repeat(34);

        assertThat(senha.getBytes(StandardCharsets.UTF_8)).hasSize(74);

        postJson("/api/v1/auth/register", registro(emailUnico(), senha, cnpjUnico()), csrf(), null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].field").value("senha"))
                .andExpect(jsonPath("$.errors[0].code").value("PASSWORD_TOO_LONG"));
    }

    @Test
    void registroComSenhaMultibyteDentroDoLimiteEhAceitoEPodeAutenticar() throws Exception {
        Csrf csrf = csrf();
        String email = emailUnico();
        String senha = "Senha1" + "á".repeat(33);

        assertThat(senha).hasSizeLessThan(72);
        assertThat(senha.getBytes(StandardCharsets.UTF_8)).hasSize(72);

        registrar(email, senha, cnpjUnico(), csrf);
        postJson("/api/v1/auth/verify-email", Map.of("token", tokenDeVerificacao(email)), csrf, null)
                .andExpect(status().isNoContent());

        login(email, senha, csrf);
    }

    @Test
    void verificacaoComTokenJaConsumidoRetorna409() throws Exception {
        Csrf csrf = csrf();
        String email = emailUnico();
        registrar(email, SENHA_PADRAO, cnpjUnico(), csrf);

        String token = tokenDeVerificacao(email);
        postJson("/api/v1/auth/verify-email", Map.of("token", token), csrf, null)
                .andExpect(status().isNoContent());

        postJson("/api/v1/auth/verify-email", Map.of("token", token), csrf, null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("TOKEN_INVALID_OR_EXPIRED"));
    }

    @Test
    void verificacaoComTokenExpiradoRetorna409() throws Exception {
        Csrf csrf = csrf();
        String email = emailUnico();
        registrar(email, SENHA_PADRAO, cnpjUnico(), csrf);

        String token = tokenDeVerificacao(email);
        jdbcTemplate.update("""
                UPDATE token_conta t
                   SET expira_em = now() - interval '2 hours'
                  FROM usuario u
                 WHERE u.id_usuario = t.usuario_id
                   AND u.email = ?
                   AND t.tipo = 'verificacao_email'
                """, email);

        postJson("/api/v1/auth/verify-email", Map.of("token", token), csrf, null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("TOKEN_INVALID_OR_EXPIRED"));
    }

    @Test
    void consumoConcorrenteDoMesmoTokenAceitaApenasUmaRequisicao() throws Exception {
        Csrf csrf = csrf();
        String email = emailUnico();
        registrar(email, SENHA_PADRAO, cnpjUnico(), csrf);
        String token = tokenDeVerificacao(email);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch largada = new CountDownLatch(1);
        try {
            Callable<Integer> requisicao = () -> {
                largada.await(10, TimeUnit.SECONDS);
                return postJson("/api/v1/auth/verify-email", Map.of("token", token), csrf, null)
                        .andReturn().getResponse().getStatus();
            };

            Future<Integer> primeira = executor.submit(requisicao);
            Future<Integer> segunda = executor.submit(requisicao);
            largada.countDown();

            assertThat(List.of(
                    primeira.get(20, TimeUnit.SECONDS),
                    segunda.get(20, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(204, 409);
        } finally {
            largada.countDown();
            executor.shutdownNow();
        }

        Integer verificados = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM usuario
                 WHERE email = ? AND email_verificado_em IS NOT NULL
                """, Integer.class, email);
        assertThat(verificados).isEqualTo(1);
    }

    @Test
    void loginRotacionaTokenCsrfEExigeNovoParEmOperacoesProtegidas() throws Exception {
        Csrf csrfAntesDoLogin = csrf();
        String email = emailUnico();
        registrar(email, SENHA_PADRAO, cnpjUnico(), csrfAntesDoLogin);
        postJson("/api/v1/auth/verify-email", Map.of("token", tokenDeVerificacao(email)),
                csrfAntesDoLogin, null)
                .andExpect(status().isNoContent());

        MvcResult resultado = postJson(CAMINHO_LOGIN,
                Map.of("email", email, "senha", SENHA_PADRAO), csrfAntesDoLogin, null)
                .andExpect(status().isNoContent())
                .andReturn();

        MockHttpSession sessao = sessaoDo(resultado);
        Csrf csrfDepoisDoLogin = csrfDaResposta(resultado);

        assertThat(csrfDepoisDoLogin.token())
                .as("login deve emitir um novo token CSRF")
                .isNotEqualTo(csrfAntesDoLogin.token());

        postJsonComTokensCsrf("/api/v1/legal/acceptances", versoesLegais(),
                csrfDepoisDoLogin.token(), csrfAntesDoLogin.token(), sessao)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        postJson("/api/v1/legal/acceptances", versoesLegais(), csrfDepoisDoLogin, sessao)
                .andExpect(status().isNoContent());
    }

    @Test
    void operacaoProtegidaSemCsrfRetorna403() throws Exception {
        ContaTeste conta = contaAutenticada();

        postJsonSemCsrf("/api/v1/legal/acceptances", versoesLegais(), conta.sessao())
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void recuperacaoDeSenhaComTokenExpiradoRetorna409() throws Exception {
        Csrf csrf = csrf();
        ContaTeste conta = contaAutenticada();

        postJson("/api/v1/auth/password-recovery", Map.of("email", conta.email()), csrf, null)
                .andExpect(status().isAccepted());

        String token = tokenDeRecuperacao(conta.email());
        jdbcTemplate.update("""
                UPDATE token_conta t
                   SET expira_em = now() - interval '2 hours'
                  FROM usuario u
                 WHERE u.id_usuario = t.usuario_id
                   AND u.email = ?
                   AND t.tipo = 'recuperacao_senha'
                """, conta.email());

        postJson("/api/v1/auth/password-recovery/confirm",
                Map.of("token", token, "novaSenha", "NovaSenha123"), csrf, null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("TOKEN_INVALID_OR_EXPIRED"));
    }

    private void assertProblemaCompleto(MvcResult resultado) throws Exception {
        JsonNode problema = json(resultado);

        assertThat(problema.get("type").asText()).isEqualTo("about:blank");
        assertThat(problema.get("title").asText()).isNotBlank();
        assertThat(problema.get("status").asInt()).isEqualTo(400);
        assertThat(problema.get("detail").asText()).isNotBlank();
        assertThat(problema.get("instance").asText()).isEqualTo("/api/v1/auth/register");
        assertThat(UUID.fromString(problema.get("requestId").asText())).isNotNull();
        assertThat(resultado.getResponse().getHeader("X-Request-Id"))
                .isEqualTo(problema.get("requestId").asText());
        assertThat(resultado.getResponse().getContentType()).startsWith(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    }
}
