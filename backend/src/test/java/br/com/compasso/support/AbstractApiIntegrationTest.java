package br.com.compasso.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import br.com.compasso.shared.email.EmailMessage;
import br.com.compasso.shared.email.EmailSender;
import jakarta.servlet.http.Cookie;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Base dos testes de API: PostgreSQL real via Testcontainers, migrations Flyway
 * aplicadas e e-mail substituído por um dublê controlável.
 *
 * <p>O container é iniciado uma única vez por JVM (padrão singleton) para que
 * as classes de teste de cada módulo compartilhem o mesmo contexto Spring e o
 * mesmo banco, sem reiniciar o PostgreSQL a cada classe. A limpeza é feita por
 * dados únicos por teste, não por truncamento.</p>
 */
@SpringBootTest(properties = {
        "spring.flyway.enabled=true"
})
@AutoConfigureMockMvc
public abstract class AbstractApiIntegrationTest {

    protected static final String SENHA_PADRAO = "Senha1234";
    protected static final String CAMINHO_LOGIN = "/api/v1/auth/login";

    private static final Pattern TOKEN_NO_CORPO = Pattern.compile("token=([A-Za-z0-9_-]+)");
    private static final AtomicLong SEQUENCIA = new AtomicLong(System.nanoTime() % 1_000_000L);

    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = BancoDeTeste.instancia();

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Autowired
    protected ObjectMapper objectMapper;

    @MockitoBean
    protected EmailSender emailSender;

    protected static String emailUnico() {
        return "professor" + SEQUENCIA.incrementAndGet() + "@exemplo.test";
    }

    protected static String cnpjUnico() {
        return CnpjTestFactory.gerar(SEQUENCIA.incrementAndGet());
    }

    protected static String cnpjAlfanumericoUnico() {
        return CnpjTestFactory.gerarAlfanumerico(SEQUENCIA.incrementAndGet());
    }

    /** Obtém o par cookie/header CSRF a partir de uma requisição pública. */
    protected Csrf csrf() throws Exception {
        return csrfDaResposta(mockMvc.perform(get("/api/v1/auth/me")).andReturn());
    }

    /**
     * Extrai o último cookie CSRF emitido na resposta. O login rotaciona o
     * token e pode acrescentar um novo Set-Cookie à resposta.
     */
    protected static Csrf csrfDaResposta(MvcResult resultado) {
        Cookie cookie = Arrays.stream(resultado.getResponse().getCookies())
                .filter(candidato -> "XSRF-TOKEN".equals(candidato.getName()))
                .reduce((primeiro, segundo) -> segundo)
                .orElse(null);

        assertThat(cookie).as("cookie XSRF-TOKEN").isNotNull();
        assertThat(cookie.isHttpOnly()).as("cookie CSRF legível pelo JavaScript do frontend").isFalse();
        return new Csrf(cookie.getValue());
    }

    protected ResultActions postJson(String caminho, Map<String, Object> corpo, Csrf csrf, MockHttpSession sessao)
            throws Exception {
        MockHttpServletRequestBuilder requisicao = post(caminho)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(corpo))
                .cookie(new Cookie("XSRF-TOKEN", csrf.token()))
                .header("X-XSRF-TOKEN", csrf.token());

        if (sessao != null) {
            requisicao.session(sessao);
        }

        return mockMvc.perform(requisicao);
    }

    protected ResultActions postJsonSemCsrf(String caminho, Map<String, Object> corpo) throws Exception {
        return postJsonSemCsrf(caminho, corpo, null);
    }

    protected ResultActions postJsonSemCsrf(String caminho, Map<String, Object> corpo, MockHttpSession sessao)
            throws Exception {
        MockHttpServletRequestBuilder requisicao = post(caminho)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(corpo));

        if (sessao != null) {
            requisicao.session(sessao);
        }

        return mockMvc.perform(requisicao);
    }

    /**
     * Envia POST com cookie e header CSRF escolhidos individualmente, para
     * cobrir cenários de par divergente.
     */
    protected ResultActions postJsonComTokensCsrf(String caminho, Map<String, Object> corpo,
            String tokenCookie, String tokenHeader, MockHttpSession sessao) throws Exception {
        MockHttpServletRequestBuilder requisicao = post(caminho)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(corpo))
                .cookie(new Cookie("XSRF-TOKEN", tokenCookie))
                .header("X-XSRF-TOKEN", tokenHeader);

        if (sessao != null) {
            requisicao.session(sessao);
        }

        return mockMvc.perform(requisicao);
    }

    protected ResultActions getJson(String caminho, MockHttpSession sessao) throws Exception {
        MockHttpServletRequestBuilder requisicao = get(caminho);

        if (sessao != null) {
            requisicao.session(sessao);
        }

        return mockMvc.perform(requisicao);
    }

    protected Map<String, Object> registro(String email, String senha, String cnpj) {
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("email", email);
        corpo.put("senha", senha);
        corpo.put("nome", "Professor Teste");
        corpo.put("razaoSocial", "Professor Teste ME");
        corpo.put("cnpj", cnpj);
        corpo.put("emailContato", "contato." + email);
        corpo.put("telefoneContato", "11999999999");
        return corpo;
    }

    protected void registrar(String email, String senha, String cnpj, Csrf csrf) throws Exception {
        postJson("/api/v1/auth/register", registro(email, senha, cnpj), csrf, null)
                .andExpect(status().isAccepted());
    }

    protected String tokenDeVerificacao(String email) {
        return ultimoToken(email, "Confirme seu e-mail no Compasso");
    }

    protected String tokenDeRecuperacao(String email) {
        return ultimoToken(email, "Recuperação de senha do Compasso");
    }

    protected SessaoAutenticada login(String email, String senha, Csrf csrf) throws Exception {
        MvcResult resultado = postJson(CAMINHO_LOGIN, Map.of("email", email, "senha", senha), csrf, null)
                .andExpect(status().isNoContent())
                .andReturn();

        return new SessaoAutenticada(sessaoDo(resultado), csrfDaResposta(resultado));
    }

    /** Cadastra, confirma o e-mail e autentica a conta de teste. */
    protected ContaTeste contaAutenticada() throws Exception {
        return contaAutenticada(cnpjUnico());
    }

    /** Variante que permite escolher o CNPJ, por exemplo alfanumérico. */
    protected ContaTeste contaAutenticada(String cnpj) throws Exception {
        Csrf csrf = csrf();
        String email = emailUnico();

        registrar(email, SENHA_PADRAO, cnpj, csrf);

        String tokenVerificacao = tokenDeVerificacao(email);
        postJson("/api/v1/auth/verify-email", Map.of("token", tokenVerificacao), csrf, null)
                .andExpect(status().isNoContent());

        SessaoAutenticada login = login(email, SENHA_PADRAO, csrf);

        return new ContaTeste(email, SENHA_PADRAO, cnpj, login.sessao(), login.csrf());
    }

    protected void aceitarDocumentosLegais(ContaTeste conta) throws Exception {
        aceitarDocumentosLegais(conta.sessao(), conta.csrf());
    }

    protected void aceitarDocumentosLegais(MockHttpSession sessao, Csrf csrf) throws Exception {
        postJson("/api/v1/legal/acceptances", versoesLegais(), csrf, sessao)
                .andExpect(status().isNoContent());
    }

    protected static Map<String, Object> versoesLegais() {
        return Map.of("termosVersao", "1.0", "politicaVersao", "1.0");
    }

    protected static MockHttpSession sessaoDo(MvcResult resultado) {
        MockHttpSession sessao = (MockHttpSession) resultado.getRequest().getSession(false);
        assertThat(sessao).as("sessão criada no login").isNotNull();
        return sessao;
    }

    protected JsonNode json(MvcResult resultado) throws Exception {
        return objectMapper.readTree(resultado.getResponse().getContentAsString());
    }

    /**
     * Recupera o último token enviado para o endereço com o assunto informado.
     * Falha o teste quando nenhum e-mail correspondente foi despachado.
     */
    protected String ultimoToken(String email, String assunto) {
        ArgumentCaptor<EmailMessage> captura = ArgumentCaptor.forClass(EmailMessage.class);
        verify(emailSender, atLeastOnce()).enviar(captura.capture());

        String corpo = captura.getAllValues().stream()
                .filter(mensagem -> mensagem.destinatario().equals(email) && mensagem.assunto().equals(assunto))
                .reduce((primeira, segunda) -> segunda)
                .map(EmailMessage::corpo)
                .orElseThrow(() -> new AssertionError("E-mail não enviado para " + email + " com assunto " + assunto));

        Matcher matcher = TOKEN_NO_CORPO.matcher(corpo);
        assertThat(matcher.find()).as("token presente no corpo do e-mail").isTrue();
        return matcher.group(1);
    }

    protected record Csrf(String token) {
    }

    protected record SessaoAutenticada(MockHttpSession sessao, Csrf csrf) {
    }

    protected record ContaTeste(String email, String senha, String cnpj, MockHttpSession sessao, Csrf csrf) {
    }

    /** Container compartilhado por todas as classes de teste de API desta JVM. */
    private static final class BancoDeTeste {

        private static PostgreSQLContainer instancia;

        private BancoDeTeste() {
        }

        static synchronized PostgreSQLContainer instancia() {
            if (instancia == null) {
                instancia = new PostgreSQLContainer(DockerImageName.parse(
                        System.getenv().getOrDefault("POSTGRES_TEST_IMAGE", "postgres:17.11-alpine")))
                        .withInitScript("db/create-test-roles.sql");
                instancia.start();
            }
            return instancia;
        }
    }
}
