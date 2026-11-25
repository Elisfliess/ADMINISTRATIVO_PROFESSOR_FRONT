package br.com.compasso.support;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** Relógio mutável para testar janelas de tempo sem espera real. */
public final class RelogioDeTeste extends Clock {

    private Instant agora;
    private final ZoneId zona;

    public RelogioDeTeste(Instant inicio) {
        this(inicio, ZoneOffset.UTC);
    }

    private RelogioDeTeste(Instant agora, ZoneId zona) {
        this.agora = agora;
        this.zona = zona;
    }

    public void avancar(Duration duracao) {
        agora = agora.plus(duracao);
    }

    @Override
    public Instant instant() {
        return agora;
    }

    @Override
    public ZoneId getZone() {
        return zona;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return new RelogioDeTeste(agora, zone);
    }
}
