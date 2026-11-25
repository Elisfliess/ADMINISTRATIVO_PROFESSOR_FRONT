package br.com.compasso.aluno.entity;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import br.com.compasso.shared.uuid.UuidV7;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

/**
 * Aluno administrado por um professor. O aluno não possui conta própria no MVP.
 * A identidade do professor vem sempre da sessão autenticada, nunca do payload.
 */
@Entity
@Table(name = "aluno")
@IdClass(AlunoId.class)
public class Aluno {

    private static final String NOME_ANONIMIZADO = "Aluno anonimizado";

    @Id
    @Column(name = "id_aluno", nullable = false)
    private UUID id;

    @Id
    @Column(name = "professor_id", nullable = false)
    private UUID professorId;

    @Column(name = "nome", nullable = false, length = 150)
    private String nome;

    @Column(name = "telefone", length = 30)
    private String telefone;

    @Column(name = "email", length = 254)
    private String email;

    @Column(name = "data_nascimento")
    private LocalDate dataNascimento;

    @Column(name = "observacoes", columnDefinition = "text")
    private String observacoes;

    @Column(name = "responsavel_nome", length = 150)
    private String responsavelNome;

    @Column(name = "responsavel_contato", length = 254)
    private String responsavelContato;

    @Column(name = "responsavel_informado_em")
    private Instant responsavelInformadoEm;

    @Column(name = "ativo", nullable = false)
    private boolean ativo = true;

    @Column(name = "inativado_em")
    private Instant inativadoEm;

    @Column(name = "anonimizado_em")
    private Instant anonimizadoEm;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    protected Aluno() {
    }

    public Aluno(UUID professorId,
            String nome,
            String telefone,
            LocalDate dataNascimento,
            String email,
            String observacoes,
            String responsavelNome,
            String responsavelContato,
            Instant responsavelInformadoEm) {
        this.id = UuidV7.generate();
        this.professorId = professorId;
        this.nome = nome;
        this.telefone = telefone;
        this.dataNascimento = dataNascimento;
        this.email = email;
        this.observacoes = observacoes;
        this.responsavelNome = responsavelNome;
        this.responsavelContato = responsavelContato;
        this.responsavelInformadoEm = responsavelInformadoEm;
        this.ativo = true;
    }

    /**
     * Substitui os dados cadastrais pelo estado já validado e normalizado pelo
     * Service. Campos opcionais chegam como {@code null} quando devem ser
     * limpos.
     */
    public void atualizarDados(String nome,
            String telefone,
            LocalDate dataNascimento,
            String email,
            String observacoes,
            String responsavelNome,
            String responsavelContato,
            Instant responsavelInformadoEm) {
        this.nome = nome;
        this.telefone = telefone;
        this.dataNascimento = dataNascimento;
        this.email = email;
        this.observacoes = observacoes;
        this.responsavelNome = responsavelNome;
        this.responsavelContato = responsavelContato;
        this.responsavelInformadoEm = responsavelInformadoEm;
    }

    /**
     * Reativa o aluno sem recriar aulas canceladas nem reativar plano encerrado.
     * A data de inativação é limpa porque representa o estado atual do cadastro.
     */
    public void reativar() {
        this.ativo = true;
        this.inativadoEm = null;
    }

    /**
     * Remove os dados pessoais mantendo identificador, vínculo com o professor e
     * registros históricos de aulas, planos e cobranças. A anonimização é um
     * estado final: não há operação de reversão.
     */
    public void anonimizar(Instant agora) {
        this.nome = NOME_ANONIMIZADO;
        this.telefone = null;
        this.email = null;
        this.dataNascimento = null;
        this.observacoes = null;
        this.responsavelNome = null;
        this.responsavelContato = null;
        this.responsavelInformadoEm = null;
        this.ativo = false;
        this.anonimizadoEm = agora;
    }

    public boolean isAnonimizado() {
        return anonimizadoEm != null;
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

    public AlunoId chave() {
        return new AlunoId(id, professorId);
    }

    public UUID getId() {
        return id;
    }

    public UUID getProfessorId() {
        return professorId;
    }

    public String getNome() {
        return nome;
    }

    public String getTelefone() {
        return telefone;
    }

    public String getEmail() {
        return email;
    }

    public LocalDate getDataNascimento() {
        return dataNascimento;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public String getResponsavelNome() {
        return responsavelNome;
    }

    public String getResponsavelContato() {
        return responsavelContato;
    }

    public Instant getResponsavelInformadoEm() {
        return responsavelInformadoEm;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public Instant getInativadoEm() {
        return inativadoEm;
    }

    public Instant getAnonimizadoEm() {
        return anonimizadoEm;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }
}
