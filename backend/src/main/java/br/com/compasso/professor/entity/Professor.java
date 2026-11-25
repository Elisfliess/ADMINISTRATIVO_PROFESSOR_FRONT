package br.com.compasso.professor.entity;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import br.com.compasso.shared.uuid.UuidV7;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

/** Titular da conta. O vínculo com {@code usuario} é único. */
@Entity
@Table(name = "professor")
public class Professor {

    @Id
    @Column(name = "id_professor", nullable = false)
    private UUID id;

    @Column(name = "id_usuario", nullable = false)
    private UUID idUsuario;

    @Column(name = "nome", nullable = false, length = 150)
    private String nome;

    @Column(name = "razao_social", nullable = false, length = 200)
    private String razaoSocial;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "cnpj", nullable = false, length = 14, columnDefinition = "char(14)")
    private String cnpj;

    @Column(name = "email_contato", length = 254)
    private String emailContato;

    @Column(name = "telefone_contato", length = 30)
    private String telefoneContato;

    @Column(name = "instagram_url", length = 500)
    private String instagramUrl;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    protected Professor() {
    }

    public Professor(UUID idUsuario, String nome, String razaoSocial, String cnpj, String emailContato,
            String telefoneContato) {
        this.id = UuidV7.generate();
        this.idUsuario = idUsuario;
        this.nome = nome;
        this.razaoSocial = razaoSocial;
        this.cnpj = cnpj;
        this.emailContato = emailContato;
        this.telefoneContato = telefoneContato;
    }

    @PrePersist
    void aoPersistir() {
        Instant agora = Instant.now();
        if (id == null) {
            id = UuidV7.generate();
        }
        if (criadoEm == null) {
            criadoEm = agora;
        }
        atualizadoEm = agora;
    }

    @PreUpdate
    void aoAtualizar() {
        atualizadoEm = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getIdUsuario() {
        return idUsuario;
    }

    public String getNome() {
        return nome;
    }

    public String getRazaoSocial() {
        return razaoSocial;
    }

    public String getCnpj() {
        return cnpj;
    }

    public String getEmailContato() {
        return emailContato;
    }

    public String getTelefoneContato() {
        return telefoneContato;
    }

    public String getInstagramUrl() {
        return instagramUrl;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }
}
