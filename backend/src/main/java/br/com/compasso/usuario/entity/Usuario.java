package br.com.compasso.usuario.entity;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import br.com.compasso.shared.uuid.UuidV7;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

/**
 * Conta de acesso do professor. A tabela {@code sessao} é legada e não é
 * mapeada pela aplicação (D-014).
 */
@Entity
@Table(name = "usuario")
public class Usuario {

    public static final int MAXIMO_TENTATIVAS = 5;
    public static final Duration DURACAO_BLOQUEIO = Duration.ofMinutes(15);

    @Id
    @Column(name = "id_usuario", nullable = false)
    private UUID id;

    @Column(name = "email", nullable = false, length = 254)
    private String email;

    @Column(name = "senha_hash", nullable = false, length = 255)
    private String senhaHash;

    @Column(name = "ativo", nullable = false)
    private boolean ativo = true;

    @Column(name = "email_verificado_em")
    private Instant emailVerificadoEm;

    @Column(name = "tentativas_login", nullable = false)
    private int tentativasLogin;

    @Column(name = "bloqueado_ate")
    private Instant bloqueadoAte;

    @Column(name = "ultimo_acesso_em")
    private Instant ultimoAcessoEm;

    @Column(name = "encerramento_solicitado_em")
    private Instant encerramentoSolicitadoEm;

    @Column(name = "encerrada_em")
    private Instant encerradaEm;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    protected Usuario() {
    }

    public Usuario(String email, String senhaHash) {
        this.id = UuidV7.generate();
        this.email = email;
        this.senhaHash = senhaHash;
        this.ativo = true;
    }

    public boolean estaBloqueado(Instant agora) {
        return bloqueadoAte != null && bloqueadoAte.isAfter(agora);
    }

    public void registrarFalhaDeLogin(Instant agora) {
        tentativasLogin = tentativasLogin + 1;
        if (tentativasLogin >= MAXIMO_TENTATIVAS) {
            tentativasLogin = 0;
            bloqueadoAte = agora.plus(DURACAO_BLOQUEIO);
        }
    }

    public void registrarAcessoBemSucedido(Instant agora) {
        tentativasLogin = 0;
        bloqueadoAte = null;
        ultimoAcessoEm = agora;
    }

    public void marcarEmailVerificado(Instant agora) {
        if (emailVerificadoEm == null) {
            emailVerificadoEm = agora;
        }
    }

    public void alterarSenha(String senhaHash) {
        this.senhaHash = senhaHash;
    }

    public void desbloquear() {
        tentativasLogin = 0;
        bloqueadoAte = null;
    }

    public boolean isEmailVerificado() {
        return emailVerificadoEm != null;
    }

    public boolean isContaEncerrada() {
        return encerradaEm != null;
    }

    public boolean podeAutenticar() {
        return ativo && !isContaEncerrada() && encerramentoSolicitadoEm == null;
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

    public String getEmail() {
        return email;
    }

    public String getSenhaHash() {
        return senhaHash;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public Instant getEmailVerificadoEm() {
        return emailVerificadoEm;
    }

    public int getTentativasLogin() {
        return tentativasLogin;
    }

    public Instant getBloqueadoAte() {
        return bloqueadoAte;
    }

    public Instant getUltimoAcessoEm() {
        return ultimoAcessoEm;
    }

    public Instant getEncerramentoSolicitadoEm() {
        return encerramentoSolicitadoEm;
    }

    public Instant getEncerradaEm() {
        return encerradaEm;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }
}
