package br.com.compasso.usuario.entity;

import java.time.Instant;
import java.util.UUID;

import br.com.compasso.shared.uuid.UuidV7;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/**
 * Token de uso único de verificação de e-mail ou recuperação de senha. Somente
 * o hash é persistido; o valor bruto nunca é gravado nem devolvido pela API.
 */
@Entity
@Table(name = "token_conta")
public class TokenConta {

    @Id
    @Column(name = "id_token", nullable = false)
    private UUID id;

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Convert(converter = TipoTokenConverter.class)
    @Column(name = "tipo", nullable = false, length = 30)
    private TipoToken tipo;

    @Column(name = "token_hash", nullable = false, length = 255)
    private String tokenHash;

    @Column(name = "expira_em", nullable = false)
    private Instant expiraEm;

    @Column(name = "usado_em")
    private Instant usadoEm;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    protected TokenConta() {
    }

    public TokenConta(UUID usuarioId, TipoToken tipo, String tokenHash, Instant expiraEm) {
        this.id = UuidV7.generate();
        this.usuarioId = usuarioId;
        this.tipo = tipo;
        this.tokenHash = tokenHash;
        this.expiraEm = expiraEm;
    }

    public boolean estaExpirado(Instant agora) {
        return !expiraEm.isAfter(agora);
    }

    public void marcarComoUsado(Instant agora) {
        this.usadoEm = agora;
    }

    public boolean isUsado() {
        return usadoEm != null;
    }

    @PrePersist
    void aoPersistir() {
        if (id == null) {
            id = UuidV7.generate();
        }
        if (criadoEm == null) {
            criadoEm = Instant.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getUsuarioId() {
        return usuarioId;
    }

    public TipoToken getTipo() {
        return tipo;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public Instant getExpiraEm() {
        return expiraEm;
    }

    public Instant getUsadoEm() {
        return usadoEm;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }
}
