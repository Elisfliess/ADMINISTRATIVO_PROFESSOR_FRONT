package br.com.compasso.usuario.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import br.com.compasso.shared.error.ErrorCodes;
import br.com.compasso.shared.error.LimiteExcedidoException;
import br.com.compasso.usuario.entity.TipoToken;

/**
 * Aplica os limites de reenvio de e-mails de verificação e recuperação: espera
 * mínima de 45 segundos e no máximo 5 envios por hora, por e-mail e tipo. O
 * limite também vale para e-mails sem conta, evitando enumeração. Estado em
 * memória é compatível com a instância única do MVP.
 *
 * <p>Para manter o limite de memória efetivo, a limpeza de entradas expiradas
 * ocorre quando o mapa atinge {@link #LIMITE_ENTRADAS}. Se ainda assim não
 * houver espaço, novas chaves são recusadas com 429 (fail-closed) em vez de
 * ampliar o mapa; chaves já existentes continuam sujeitas às regras normais.
 * A limpeza só é possível porque o estado é local e a janela é curta.</p>
 */
@Component
public class LimitadorDeReenvio {

    private static final Duration ESPERA_MINIMA = Duration.ofSeconds(45);
    private static final Duration JANELA = Duration.ofHours(1);
    private static final int MAXIMO_POR_JANELA = 5;
    static final int LIMITE_ENTRADAS = 10_000;

    private final Clock clock;
    private final ConcurrentHashMap<String, Deque<Instant>> envios = new ConcurrentHashMap<>();
    private final Object trava = new Object();

    public LimitadorDeReenvio(Clock clock) {
        this.clock = clock;
    }

    public void verificar(String email, TipoToken tipo) {
        Instant agora = clock.instant();
        String chave = tipo.valorBanco() + ":" + email;

        synchronized (trava) {
            Deque<Instant> marcos = envios.get(chave);

            if (marcos == null && envios.size() >= LIMITE_ENTRADAS) {
                limparExpirados(agora);
            }

            if (marcos == null && envios.size() >= LIMITE_ENTRADAS) {
                throw new LimiteExcedidoException(ErrorCodes.RESEND_LIMIT_EXCEEDED,
                        "Limite temporário de solicitações atingido. Tente novamente mais tarde.", JANELA);
            }

            if (marcos == null) {
                marcos = new ArrayDeque<>();
                envios.put(chave, marcos);
            }

            marcos.removeIf(momento -> !momento.plus(JANELA).isAfter(agora));

            if (!marcos.isEmpty()) {
                Instant liberaEm = marcos.peekLast().plus(ESPERA_MINIMA);
                if (agora.isBefore(liberaEm)) {
                    throw new LimiteExcedidoException(ErrorCodes.RESEND_TOO_SOON,
                            "Aguarde antes de solicitar um novo envio.", Duration.between(agora, liberaEm));
                }
            }

            if (marcos.size() >= MAXIMO_POR_JANELA) {
                Instant liberaEm = marcos.peekFirst().plus(JANELA);
                throw new LimiteExcedidoException(ErrorCodes.RESEND_LIMIT_EXCEEDED,
                        "Limite de envios por hora excedido.", Duration.between(agora, liberaEm));
            }

            marcos.addLast(agora);
        }
    }

    /** Exposto apenas para os testes verificarem o limite efetivo de memória. */
    int entradasRegistradas() {
        synchronized (trava) {
            return envios.size();
        }
    }

    private void limparExpirados(Instant agora) {
        envios.entrySet().removeIf(entrada -> {
            Deque<Instant> marcos = entrada.getValue();
            marcos.removeIf(momento -> !momento.plus(JANELA).isAfter(agora));
            return marcos.isEmpty();
        });
    }
}
