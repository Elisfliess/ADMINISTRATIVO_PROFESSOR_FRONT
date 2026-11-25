package br.com.compasso.usuario.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;

import br.com.compasso.shared.error.ErrorCodes;
import br.com.compasso.shared.error.LimiteExcedidoException;
import br.com.compasso.support.RelogioDeTeste;
import br.com.compasso.usuario.entity.TipoToken;

class LimitadorDeReenvioTest {

    private static final Instant INICIO = Instant.parse("2026-11-15T12:00:00Z");

    private final RelogioDeTeste relogio = new RelogioDeTeste(INICIO);
    private final LimitadorDeReenvio limitador = new LimitadorDeReenvio(relogio);

    @Test
    void permitePrimeiroEnvioEExigeEsperaDeQuarentaECincoSegundos() {
        assertThatCode(() -> limitador.verificar("a@exemplo.test", TipoToken.VERIFICACAO_EMAIL))
                .doesNotThrowAnyException();

        relogio.avancar(Duration.ofSeconds(44));
        assertThatThrownBy(() -> limitador.verificar("a@exemplo.test", TipoToken.VERIFICACAO_EMAIL))
                .isInstanceOfSatisfying(LimiteExcedidoException.class, excecao -> {
                    assertThat(excecao.code()).isEqualTo(ErrorCodes.RESEND_TOO_SOON);
                    assertThat(excecao.segundosDeEspera()).isBetween(1L, 45L);
                });

        relogio.avancar(Duration.ofSeconds(1));
        assertThatCode(() -> limitador.verificar("a@exemplo.test", TipoToken.VERIFICACAO_EMAIL))
                .doesNotThrowAnyException();
    }

    @Test
    void limitaACincoEnviosPorHora() {
        for (int envio = 1; envio <= 5; envio++) {
            assertThatCode(() -> limitador.verificar("b@exemplo.test", TipoToken.RECUPERACAO_SENHA))
                    .doesNotThrowAnyException();
            relogio.avancar(Duration.ofSeconds(45));
        }

        assertThatThrownBy(() -> limitador.verificar("b@exemplo.test", TipoToken.RECUPERACAO_SENHA))
                .isInstanceOfSatisfying(LimiteExcedidoException.class,
                        excecao -> assertThat(excecao.code()).isEqualTo(ErrorCodes.RESEND_LIMIT_EXCEEDED));

        // Somente depois que o envio mais antigo sai da janela de uma hora.
        relogio.avancar(Duration.ofHours(1));
        assertThatCode(() -> limitador.verificar("b@exemplo.test", TipoToken.RECUPERACAO_SENHA))
                .doesNotThrowAnyException();
    }

    @Test
    void separaOsLimitesPorTipoDeTokenEEmail() {
        limitador.verificar("c@exemplo.test", TipoToken.VERIFICACAO_EMAIL);

        assertThatCode(() -> limitador.verificar("c@exemplo.test", TipoToken.RECUPERACAO_SENHA))
                .doesNotThrowAnyException();
        assertThatCode(() -> limitador.verificar("outro@exemplo.test", TipoToken.VERIFICACAO_EMAIL))
                .doesNotThrowAnyException();

        assertThatThrownBy(() -> limitador.verificar("c@exemplo.test", TipoToken.VERIFICACAO_EMAIL))
                .isInstanceOfSatisfying(LimiteExcedidoException.class,
                        excecao -> assertThat(excecao.code()).isEqualTo(ErrorCodes.RESEND_TOO_SOON));
    }

    @Test
    void removeEntradasExpiradasAoAtingirOLimiteDeMemoria() {
        preencherAteOLimite();

        assertThat(limitador.entradasRegistradas()).isEqualTo(LimitadorDeReenvio.LIMITE_ENTRADAS);

        relogio.avancar(Duration.ofHours(1).plusSeconds(1));

        assertThatCode(() -> limitador.verificar("novo@exemplo.test", TipoToken.VERIFICACAO_EMAIL))
                .doesNotThrowAnyException();

        assertThat(limitador.entradasRegistradas()).isEqualTo(1);
    }

    @Test
    void recusaNovasChavesSemCrescerQuandoOLimiteEstaCheioENadaExpirou() {
        preencherAteOLimite();

        assertThatThrownBy(() -> limitador.verificar("excedente@exemplo.test", TipoToken.VERIFICACAO_EMAIL))
                .isInstanceOfSatisfying(LimiteExcedidoException.class,
                        excecao -> assertThat(excecao.code()).isEqualTo(ErrorCodes.RESEND_LIMIT_EXCEEDED));

        assertThat(limitador.entradasRegistradas()).isEqualTo(LimitadorDeReenvio.LIMITE_ENTRADAS);
    }

    @Test
    void chavesJaRegistradasMantemAsRegrasNormaisMesmoComOMapaCheio() {
        preencherAteOLimite();

        assertThatThrownBy(() -> limitador.verificar("email-0@exemplo.test", TipoToken.VERIFICACAO_EMAIL))
                .isInstanceOfSatisfying(LimiteExcedidoException.class,
                        excecao -> assertThat(excecao.code()).isEqualTo(ErrorCodes.RESEND_TOO_SOON));

        assertThat(limitador.entradasRegistradas()).isEqualTo(LimitadorDeReenvio.LIMITE_ENTRADAS);
    }

    private void preencherAteOLimite() {
        for (int indice = 0; indice < LimitadorDeReenvio.LIMITE_ENTRADAS; indice++) {
            limitador.verificar("email-" + indice + "@exemplo.test", TipoToken.VERIFICACAO_EMAIL);
        }
    }
}
