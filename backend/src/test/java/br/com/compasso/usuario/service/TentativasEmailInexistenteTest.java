package br.com.compasso.usuario.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;

import br.com.compasso.shared.error.ErrorCodes;
import br.com.compasso.shared.error.LimiteExcedidoException;
import br.com.compasso.support.RelogioDeTeste;
import br.com.compasso.usuario.entity.Usuario;

class TentativasEmailInexistenteTest {

    private static final Instant INICIO = Instant.parse("2026-11-15T12:00:00Z");

    private final RelogioDeTeste relogio = new RelogioDeTeste(INICIO);
    private final TentativasEmailInexistente tentativas = new TentativasEmailInexistente();

    @Test
    void bloqueiaNoQuintoRegistroDeFalha() {
        for (int falha = 1; falha < Usuario.MAXIMO_TENTATIVAS; falha++) {
            tentativas.registrarFalha("a@exemplo.test", relogio.instant());
        }

        assertThatThrownBy(() -> tentativas.registrarFalha("a@exemplo.test", relogio.instant()))
                .isInstanceOfSatisfying(LimiteExcedidoException.class, excecao -> {
                    assertThat(excecao.code()).isEqualTo(ErrorCodes.ACCOUNT_LOCKED);
                    assertThat(excecao.segundosDeEspera()).isEqualTo(900L);
                });

        assertThat(tentativas.timestampsRegistrados("a@exemplo.test")).isEqualTo(Usuario.MAXIMO_TENTATIVAS);
    }

    @Test
    void sextaTentativaDuranteBloqueioNaoAdicionaNovoTimestamp() {
        for (int falha = 1; falha < Usuario.MAXIMO_TENTATIVAS; falha++) {
            tentativas.registrarFalha("a@exemplo.test", relogio.instant());
        }

        assertThatThrownBy(() -> tentativas.registrarFalha("a@exemplo.test", relogio.instant()))
                .isInstanceOfSatisfying(LimiteExcedidoException.class, excecao -> {
                    assertThat(excecao.code()).isEqualTo(ErrorCodes.ACCOUNT_LOCKED);
                });

        assertThatThrownBy(() -> tentativas.registrarFalha("a@exemplo.test", relogio.instant()))
                .isInstanceOf(LimiteExcedidoException.class);

        assertThat(tentativas.timestampsRegistrados("a@exemplo.test")).isEqualTo(Usuario.MAXIMO_TENTATIVAS);
    }

    @Test
    void tentativasDuranteBloqueioMantemFilaEAlcanciamQuantidadeDeTimestamps() {
        for (int falha = 1; falha < Usuario.MAXIMO_TENTATIVAS; falha++) {
            tentativas.registrarFalha("b@exemplo.test", relogio.instant());
        }

        long segundosPrimeiroBloqueio = 0;

        for (int tentativa = 0; tentativa < 100; tentativa++) {
            LimiteExcedidoException excecao = catchThrowableOfType(
                    () -> tentativas.registrarFalha("b@exemplo.test", relogio.instant()),
                    LimiteExcedidoException.class);

            if (tentativa == 0) {
                segundosPrimeiroBloqueio = excecao.segundosDeEspera();
                assertThat(segundosPrimeiroBloqueio).isEqualTo(900L);
            }

            // Tentativas durante o bloqueio não alteram o horário de desbloqueio.
            assertThat(excecao.segundosDeEspera()).isEqualTo(segundosPrimeiroBloqueio);
        }

        assertThat(tentativas.timestampsRegistrados("b@exemplo.test")).isEqualTo(Usuario.MAXIMO_TENTATIVAS);
    }

    @Test
    void quintaFalhaIniciaBloqueioDeQuinzeMinutosAPartirDela() {
        Instant t1 = INICIO;
        Instant t2 = INICIO.plusSeconds(120);
        Instant t3 = INICIO.plusSeconds(240);
        Instant t4 = INICIO.plusSeconds(360);
        Instant t5 = INICIO.plusSeconds(480);

        tentativas.registrarFalha("c@exemplo.test", t1);
        tentativas.registrarFalha("c@exemplo.test", t2);
        tentativas.registrarFalha("c@exemplo.test", t3);
        tentativas.registrarFalha("c@exemplo.test", t4);

        // A quinta falha inicia o bloqueio de 15 minutos a partir dela.
        assertThatThrownBy(() -> tentativas.registrarFalha("c@exemplo.test", t5))
                .isInstanceOfSatisfying(LimiteExcedidoException.class, excecao -> {
                    assertThat(excecao.segundosDeEspera()).isEqualTo(900L);
                });

        relogio.avancar(Duration.ofMinutes(10));

        // Com o bloqueio iniciado na quinta falha (t=8min), ao tentar às 10min o
        // tempo restante é de 13 minutos.
        assertThatThrownBy(() -> tentativas.registrarFalha("c@exemplo.test", relogio.instant()))
                .isInstanceOfSatisfying(LimiteExcedidoException.class, excecao -> {
                    assertThat(excecao.segundosDeEspera()).isEqualTo(780L);
                });
    }

    @Test
    void cincoFalhasDistribuidasEmIntervalosSuperioresAoBloqueioTambemBloqueiam() {
        Instant t1 = INICIO;
        Instant t2 = INICIO.plusSeconds(16 * 60);
        Instant t3 = INICIO.plusSeconds(32 * 60);
        Instant t4 = INICIO.plusSeconds(48 * 60);
        Instant t5 = INICIO.plusSeconds(64 * 60);

        tentativas.registrarFalha("f@exemplo.test", t1);
        tentativas.registrarFalha("f@exemplo.test", t2);
        tentativas.registrarFalha("f@exemplo.test", t3);
        tentativas.registrarFalha("f@exemplo.test", t4);

        // Cinco falhas em intervalos superiores a 15 minutos também bloqueiam:
        // a contagem consecutiva não é descartada pela passagem do tempo.
        assertThatThrownBy(() -> tentativas.registrarFalha("f@exemplo.test", t5))
                .isInstanceOfSatisfying(LimiteExcedidoException.class, excecao -> {
                    assertThat(excecao.segundosDeEspera()).isEqualTo(900L);
                });
    }

    @Test
    void quatroFalhasDistribuidasNaoSaoDescartadasPelaPassagemDoTempo() {
        tentativas.registrarFalha("g@exemplo.test", INICIO);
        tentativas.registrarFalha("g@exemplo.test", INICIO.plusSeconds(6 * 60));
        tentativas.registrarFalha("g@exemplo.test", INICIO.plusSeconds(12 * 60));
        tentativas.registrarFalha("g@exemplo.test", INICIO.plusSeconds(18 * 60));

        relogio.avancar(Duration.ofMinutes(20));

        // Quatro falhas seguidas não devem ser descartadas apenas pela passagem
        // do tempo; a quinta falha deve iniciar o bloqueio.
        assertThatThrownBy(() -> tentativas.registrarFalha("g@exemplo.test", relogio.instant()))
                .isInstanceOfSatisfying(LimiteExcedidoException.class, excecao -> {
                    assertThat(excecao.segundosDeEspera()).isEqualTo(900L);
                });
    }

    @Test
    void liberaNovamenteDepoisDaJanelaDeBloqueio() {
        for (int falha = 1; falha < Usuario.MAXIMO_TENTATIVAS; falha++) {
            tentativas.registrarFalha("d@exemplo.test", relogio.instant());
        }

        assertThatThrownBy(() -> tentativas.registrarFalha("d@exemplo.test", relogio.instant()))
                .isInstanceOf(LimiteExcedidoException.class);

        relogio.avancar(Usuario.DURACAO_BLOQUEIO.plusSeconds(1));

        assertThatCode(() -> tentativas.registrarFalha("d@exemplo.test", relogio.instant()))
                .doesNotThrowAnyException();

        // Após a expiração da janela, a contagem reinicia e uma nova sequência
        // pode ser iniciada (aqui com um e-mail novo), podendo bloquear de novo.
        for (int falha = 1; falha < Usuario.MAXIMO_TENTATIVAS; falha++) {
            tentativas.registrarFalha("e@exemplo.test", relogio.instant());
        }

        assertThatThrownBy(() -> tentativas.registrarFalha("e@exemplo.test", relogio.instant()))
                .isInstanceOf(LimiteExcedidoException.class);
    }

    @Test
    void limpezaPorCapacidadeNaoDescartaSequenciaParcialAindaRelevante() {
        // Quatro falhas para a chave original: sequência parcial em aberto.
        for (int falha = 0; falha < Usuario.MAXIMO_TENTATIVAS - 1; falha++) {
            tentativas.registrarFalha("a@exemplo.test", relogio.instant());
        }

        // Avança o relógio para além de 15 minutos: a sequência parcial deve
        // permanecer registrada mesmo após o tempo decorrido.
        relogio.avancar(Duration.ofMinutes(20));

        // Preenche o mapa até o limite de 10.000 chaves.
        while (tentativas.entradasRegistradas() < TentativasEmailInexistente.LIMITE_ENTRADAS) {
            tentativas.registrarFalha("email-" + tentativas.entradasRegistradas() + "@exemplo.test", relogio.instant());
        }

        // Nova chave: condição real de capacidade dispara a limpeza.
        assertThatThrownBy(() -> tentativas.registrarFalha("novo@exemplo.test", relogio.instant()))
                .isInstanceOfSatisfying(LimiteExcedidoException.class, excecao -> {
                    assertThat(excecao.code()).isEqualTo(ErrorCodes.ACCOUNT_LOCKED);
                });

        // A sequência parcial da chave original não pode ter sido descartada:
        // a quinta falha deve iniciar o bloqueio.
        assertThatThrownBy(() -> tentativas.registrarFalha("a@exemplo.test", relogio.instant()))
                .isInstanceOfSatisfying(LimiteExcedidoException.class, excecao -> {
                    assertThat(excecao.code()).isEqualTo(ErrorCodes.ACCOUNT_LOCKED);
                    assertThat(excecao.segundosDeEspera()).isEqualTo(900L);
                });
    }

    @Test
    void removeEntradasExpiradasAoAtingirOLimiteDeMemoria() {
        for (int indice = 0; indice < TentativasEmailInexistente.LIMITE_ENTRADAS; indice++) {
            String email = "email-" + indice + "@exemplo.test";
            try {
                for (int falha = 0; falha < Usuario.MAXIMO_TENTATIVAS; falha++) {
                    tentativas.registrarFalha(email, relogio.instant());
                }
            } catch (LimiteExcedidoException excecao) {
                // A quinta falha registra o bloqueio; a exceção faz parte do fluxo.
            }
            relogio.avancar(Usuario.DURACAO_BLOQUEIO.plusSeconds(1));
        }

        assertThat(tentativas.entradasRegistradas()).isEqualTo(TentativasEmailInexistente.LIMITE_ENTRADAS);

        assertThatCode(() -> tentativas.registrarFalha("novo@exemplo.test", relogio.instant()))
                .doesNotThrowAnyException();

        assertThat(tentativas.entradasRegistradas()).isEqualTo(1);
    }

    @Test
    void recusaNovasChavesSemCrescerQuandoOLimiteEstaCheioENadaExpirou() {
        for (int indice = 0; indice < TentativasEmailInexistente.LIMITE_ENTRADAS; indice++) {
            tentativas.registrarFalha("email-" + indice + "@exemplo.test", relogio.instant());
        }

        assertThatThrownBy(() -> tentativas.registrarFalha("excedente@exemplo.test", relogio.instant()))
                .isInstanceOfSatisfying(LimiteExcedidoException.class,
                        excecao -> assertThat(excecao.code()).isEqualTo(ErrorCodes.ACCOUNT_LOCKED));

        assertThat(tentativas.entradasRegistradas()).isEqualTo(TentativasEmailInexistente.LIMITE_ENTRADAS);
    }

    @Test
    void chavesJaRegistradasContinuamContandoMesmoComOMapaCheio() {
        for (int indice = 0; indice < TentativasEmailInexistente.LIMITE_ENTRADAS; indice++) {
            tentativas.registrarFalha("email-" + indice + "@exemplo.test", relogio.instant());
        }

        assertThatCode(() -> tentativas.registrarFalha("email-0@exemplo.test", relogio.instant()))
                .doesNotThrowAnyException();

        assertThat(tentativas.entradasRegistradas()).isEqualTo(TentativasEmailInexistente.LIMITE_ENTRADAS);
    }
}
