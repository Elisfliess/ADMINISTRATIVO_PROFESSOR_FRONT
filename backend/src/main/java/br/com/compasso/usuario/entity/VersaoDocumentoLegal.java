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

/** Versão publicada de um documento legal. */
@Entity
@Table(name = "versao_documento_legal")
public class VersaoDocumentoLegal {

    @Id
    @Column(name = "id_versao", nullable = false)
    private UUID id;

    @Convert(converter = TipoDocumentoLegalConverter.class)
    @Column(name = "tipo", nullable = false, length = 30)
    private TipoDocumentoLegal tipo;

    @Column(name = "versao", nullable = false, length = 20)
    private String versao;

    @Column(name = "publicada_em", nullable = false)
    private Instant publicadaEm;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    protected VersaoDocumentoLegal() {
    }

    public VersaoDocumentoLegal(TipoDocumentoLegal tipo, String versao, Instant publicadaEm) {
        this.id = UuidV7.generate();
        this.tipo = tipo;
        this.versao = versao;
        this.publicadaEm = publicadaEm;
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

    public TipoDocumentoLegal getTipo() {
        return tipo;
    }

    public String getVersao() {
        return versao;
    }

    public Instant getPublicadaEm() {
        return publicadaEm;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }
}
