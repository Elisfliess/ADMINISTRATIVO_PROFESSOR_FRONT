package br.com.compasso.usuario.service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import br.com.compasso.shared.error.ErrorCodes;
import br.com.compasso.shared.error.LimiteExcedidoException;
import br.com.compasso.usuario.entity.Usuario;

/**
 * Contabiliza falhas de login para e-mails sem conta, para que o bloqueio por
 * tentativas (RF01) tenha o mesmo comportamento de um e-mail existente e não
 * permita enumeração. Estado em memória é compatível com a instância única do
 * MVP (D-014).
 *
 * <p>O mapa tem capacidade máxima efetiva. Ao atingir {@link #LIMITE_ENTRADAS},
 * entradas expiradas são removidas; se ainda não houver espaço, novas chaves
 * são recusadas com 429 (fail-closed), sem crescer além do limite. Chaves já
 * existentes mantêm o comportamento normal de bloqueio.</p>
 *
 * <p>O bloqueio de {@link Usuario#DURACAO_BLOQUEIO} é iniciado na própria
 * quinta falha registrada, de forma consistente com
 * {@link Usuario#registrarFalhaDeLogin}.</p>
 */
@Component
public class TentativasEmailInexistente {

    private static final Duration JANELA = Usuario.DURACAO_BLOQUEIO;
    static final int LIMITE_ENTRADAS = 10_000;

    private final ConcurrentHashMap<String, Deque<Instant>> tentativas = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Instant> bloqueiosAte = new ConcurrentHashMap<>();
    private final Object trava = new Object();

    public void registrarFalha(String email, Instant agora) {
        synchronized (trava) {
            Deque<Instant> marcos = tentativas.get(email);

            if (marcos == null && tentativas.size() >= LIMITE_ENTRADAS) {
                limparExpirados(agora);
            }

            if (marcos == null && tentativas.size() >= LIMITE_ENTRADAS) {
                throw new LimiteExcedidoException(ErrorCodes.ACCOUNT_LOCKED,
                        "Muitas tentativas de acesso. Tente novamente mais tarde.", JANELA);
            }

            if (marcos == null) {
                marcos = new ArrayDeque<>();
                tentativas.put(email, marcos);
            }

            Instant liberarEm = bloqueiosAte.get(email);
            boolean bloqueado = liberarEm != null && liberarEm.isAfter(agora);

            if (bloqueado) {
                // Durante o bloqueio não registra novo timestamp nem estende o bloqueio.
                throw new LimiteExcedidoException(ErrorCodes.ACCOUNT_LOCKED,
                        "Muitas tentativas de acesso. Tente novamente mais tarde.",
                        Duration.between(agora, liberarEm));
            }

            if (liberarEm != null) {
                // Bloqueio expirou: reinicia a sequência de falhas consecutivas.
                marcos.clear();
                bloqueiosAte.remove(email);
            }

            // Registra a falha enquanto a sequência está abaixo do limite;
            // com o limite atingido, a tentativa entra no fluxo de bloqueio.
            marcos.addLast(agora);

            if (marcos.size() >= Usuario.MAXIMO_TENTATIVAS) {
                Instant novoBloqueioAte = agora.plus(JANELA);
                bloqueiosAte.put(email, novoBloqueioAte);
                throw new LimiteExcedidoException(ErrorCodes.ACCOUNT_LOCKED,
                        "Muitas tentativas de acesso. Tente novamente mais tarde.",
                        Duration.between(agora, novoBloqueioAte));
            }
        }
    }

    /** Exposto apenas para os testes verificarem o limite efetivo de memória. */
    int entradasRegistradas() {
        synchronized (trava) {
            return tentativas.size();
        }
    }

    /**
     * Exposto apenas para os testes verificarem a quantidade de timestamps
     * registrados para um e-mail específico.
     */
    int timestampsRegistrados(String email) {
        synchronized (trava) {
            Deque<Instant> marcos = tentativas.get(email);
            return marcos == null ? 0 : marcos.size();
        }
    }

    private void limparExpirados(Instant agora) {
        tentativas.entrySet().removeIf(entrada -> {
            Instant liberarEm = bloqueiosAte.get(entrada.getKey());
            boolean bloqueado = liberarEm != null && liberarEm.isAfter(agora);

            if (bloqueado) {
                // Estados bloqueados não são removidos pela limpeza.
                return false;
            }

            if (liberarEm != null) {
                // Bloqueio expirado: estado de sequência encerrada pode ser removido.
                bloqueiosAte.remove(entrada.getKey());
                return true;
            }

            // Sequência parcial (sem bloqueio ativo) permanece registrada.
            return false;
        });
    }
}
