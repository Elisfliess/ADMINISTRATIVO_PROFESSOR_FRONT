package br.com.compasso.shared.error;

import java.time.Duration;

import org.springframework.http.HttpStatus;

/** Limite específico excedido (429), com indicação de espera para nova tentativa. */
public class LimiteExcedidoException extends ApiException {

    private final transient Duration espera;

    public LimiteExcedidoException(String code, String message, Duration espera) {
        super(HttpStatus.TOO_MANY_REQUESTS, code, message);
        this.espera = espera;
    }

    public Duration espera() {
        return espera;
    }

    public long segundosDeEspera() {
        long segundos = espera.toSeconds();
        return Math.max(segundos, 1);
    }
}
