package br.com.compasso.financeiro.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import br.com.compasso.shared.uuid.UuidV7;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

/** Cobrança financeira emitida por plano ou por aula. */
@Entity
@Table(name = "pagamento")
public class Cobranca {

    @Id
    @Column(name = "id_pagamento", nullable = false)
    private UUID id;

    @Column(name = "professor_id", nullable = false)
    private UUID professorId;

    @Column(name = "aluno_id", nullable = false)
    private UUID alunoId;

    @Column(name = "plano_id", nullable = false)
    private UUID planoId;

    @Column(name = "aula_id")
    private UUID aulaId;

    @Column(name = "valor", nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    @Column(name = "vencimento_em", nullable = false)
    private LocalDate vencimentoEm;

    @Column(name = "periodo_inicio", nullable = false)
    private LocalDate periodoInicio;

    @Column(name = "periodo_fim", nullable = false)
    private LocalDate periodoFim;

    @Column(name = "data_pagamento")
    private LocalDate dataPagamento;

    @Convert(converter = CobrancaStatusConverter.class)
    @Column(name = "status", nullable = false, length = 20)
    private CobrancaStatus status;

    @Column(name = "forma_pagamento", length = 30)
    private String formaPagamento;

    @Column(name = "descricao", length = 250)
    private String descricao;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    protected Cobranca() {
    }

    public Cobranca(UUID professorId,
            UUID alunoId,
            UUID planoId,
            UUID aulaId,
            BigDecimal valor,
            LocalDate vencimentoEm,
            LocalDate periodoInicio,
            LocalDate periodoFim,
            CobrancaStatus status) {
        this.professorId = professorId;
        this.alunoId = alunoId;
        this.planoId = planoId;
        this.aulaId = aulaId;
        this.valor = valor;
        this.vencimentoEm = vencimentoEm;
        this.periodoInicio = periodoInicio;
        this.periodoFim = periodoFim;
        this.status = status;
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

    public UUID getProfessorId() {
        return professorId;
    }

    public UUID getAlunoId() {
        return alunoId;
    }

    public UUID getPlanoId() {
        return planoId;
    }

    public UUID getAulaId() {
        return aulaId;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public LocalDate getVencimentoEm() {
        return vencimentoEm;
    }

    public LocalDate getPeriodoInicio() {
        return periodoInicio;
    }

    public LocalDate getPeriodoFim() {
        return periodoFim;
    }

    public LocalDate getDataPagamento() {
        return dataPagamento;
    }

    public CobrancaStatus getStatus() {
        return status;
    }

    public String getFormaPagamento() {
        return formaPagamento;
    }

    public String getDescricao() {
        return descricao;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }

    /** Baixa integral: marca a cobrança como paga e registra a data e forma de pagamento. */
    public void baixar(LocalDate recebidoEm, String formaPagamento) {
        this.status = CobrancaStatus.PAGO;
        this.dataPagamento = recebidoEm;
        this.formaPagamento = formaPagamento;
    }

    /** Cancela cobrança pendente; estado final e imutável. */
    public void cancelar() {
        this.status = CobrancaStatus.CANCELADO;
    }
}
