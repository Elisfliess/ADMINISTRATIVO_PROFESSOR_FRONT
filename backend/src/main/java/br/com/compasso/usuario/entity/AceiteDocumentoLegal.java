package br.com.compasso.usuario.entity;

import java.time.Instant;
import java.util.UUID;

import br.com.compasso.shared.uuid.UuidV7;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/**
 * Registro de aceite de uma versão de documento legal. O instante é sempre
 * gerado pelo servidor.
 */
@Entity
@Table(name = "aceite_documento_legal")
public class AceiteDocumentoLegal {

    @Id
    @Column(name = "id_aceite", nullable = false)
    private UUID id;

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Column(name = "versao_id", nullable = false)
    private UUID versaoId;

    @Column(name = "aceito_em", nullable = false)
    private Instant aceitoEm;

    protected AceiteDocumentoLegal() {
    }

    public AceiteDocumentoLegal(UUID usuarioId, UUID versaoId, Instant aceitoEm) {
        this.id = UuidV7.generate();
        this.usuarioId = usuarioId;
        this.versaoId = versaoId;
        this.aceitoEm = aceitoEm;
    }

    @PrePersist
    void aoPersistir() {
        if (id == null) {
            id = UuidV7.generate();
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getUsuarioId() {
        return usuarioId;
    }

    public UUID getVersaoId() {
        return versaoId;
    }

    public Instant getAceitoEm() {
        return aceitoEm;
    }
}
