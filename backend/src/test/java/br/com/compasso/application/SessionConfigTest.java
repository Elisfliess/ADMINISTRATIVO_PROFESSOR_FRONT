package br.com.compasso.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import br.com.compasso.support.AbstractApiIntegrationTest;

/**
 * Garante que a sessão HTTP do MVP continua configurada para 30 minutos de
 * inatividade, transporte exclusivo por cookie e cookie HttpOnly.
 *
 * <p>A expiração real por inatividade não é exercitada aqui: com MockMvc não há
 * contêiner de servlet gerenciando o relógio da sessão. A cobertura verifica a
 * configuração efetiva usada pelo servidor embarcado.</p>
 */
class SessionConfigTest extends AbstractApiIntegrationTest {

    @Autowired
    private Environment environment;

    @Test
    void sessaoUsaTrintaMinutosPorCookieHttpOnly() {
        assertThat(environment.getProperty("server.servlet.session.timeout")).isEqualTo("30m");
        assertThat(environment.getProperty("server.servlet.session.tracking-modes")).isEqualTo("cookie");
        assertThat(environment.getProperty("server.servlet.session.cookie.http-only")).isEqualTo("true");
    }
}
