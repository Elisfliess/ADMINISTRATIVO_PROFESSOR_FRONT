package br.com.compasso.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MvcResult;

import br.com.compasso.support.AbstractApiIntegrationTest;

/**
 * Cobertura de CORS: apenas as origens configuradas recebem credenciais; as
 * demais são rejeitadas antes de chegar à aplicação.
 */
class CorsApiTest extends AbstractApiIntegrationTest {

    private static final String ORIGEM_PERMITIDA = "http://localhost:5173";
    private static final String ORIGEM_NAO_PERMITIDA = "http://malicioso.example";

    @Test
    void preflightDeOrigemPermitidaRetornaCabecalhosComCredenciais() throws Exception {
        MvcResult resultado = mockMvc.perform(options("/api/v1/auth/login")
                .header(HttpHeaders.ORIGIN, ORIGEM_PERMITIDA)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "content-type,x-xsrf-token"))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(resultado.getResponse().getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
                .isEqualTo(ORIGEM_PERMITIDA);
        assertThat(resultado.getResponse().getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS))
                .isEqualTo("true");
        assertThat(resultado.getResponse().getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS))
                .contains("POST");
        assertThat(resultado.getResponse().getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS))
                .containsIgnoringCase("X-XSRF-TOKEN");
    }

    @Test
    void preflightDeOrigemNaoPermitidaEhRejeitadoSemCabecalhoCors() throws Exception {
        MvcResult resultado = mockMvc.perform(options("/api/v1/auth/login")
                .header(HttpHeaders.ORIGIN, ORIGEM_NAO_PERMITIDA)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isForbidden())
                .andReturn();

        assertThat(resultado.getResponse().getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isNull();
        assertThat(resultado.getResponse().getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS)).isNull();
    }

    @Test
    void requisicaoRealDeOrigemPermitidaRecebeCabecalhoCors() throws Exception {
        MvcResult resultado = mockMvc.perform(get("/api/v1/auth/me")
                .header(HttpHeaders.ORIGIN, ORIGEM_PERMITIDA))
                .andExpect(status().isUnauthorized())
                .andReturn();

        assertThat(resultado.getResponse().getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
                .isEqualTo(ORIGEM_PERMITIDA);
        assertThat(resultado.getResponse().getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS))
                .isEqualTo("true");
    }
}
