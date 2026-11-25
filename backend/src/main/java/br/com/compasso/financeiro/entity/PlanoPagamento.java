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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Plano de pagamento de um aluno, administrado pelo professor titular. */
@Entity
@Table(name = "plano_pagamento")
public class PlanoPagamento {

    @Id
    @Column(name = "id_plano", nullable = false)
    private UUID id;

    @Column(name = "professor_id", nullable = false)
    private UUID professorId;

    @Column(name = "aluno_id", nullable = false)
    private UUID alunoId;

    @Convert(converter = PeriodicidadePlanoConverter.class)
    @Column(name = "periodicidade", nullable = false, length = 20)
    private PeriodicidadePlano periodicidade;

    @Column(name = "valor", nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "dia_vencimento")
    private Integer diaVencimento;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "prazo_vencimento_dias")
    private Integer prazoVencimentoDias;

    @Column(name = "inicio_em", nullable = false)
    private LocalDate inicioEm;

    @Column(name = "encerrado_em")
    private LocalDate encerradoEm;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    protected PlanoPagamento() {
    }

    public PlanoPagamento(UUID professorId,
            UUID alunoId,
            PeriodicidadePlano periodicidade,
            BigDecimal valor,
            LocalDate inicioEm,
            Integer diaVencimento,
            Integer prazoVencimentoDias) {
        this.professorId = professorId;
        this.alunoId = alunoId;
        this.periodicidade = periodicidade;
        this.valor = valor;
        this.inicioEm = inicioEm;
        this.diaVencimento = diaVencimento;
        this.prazoVencimentoDias = prazoVencimentoDias;
    }

    public boolean isAtivo() {
        return encerradoEm == null;
    }

    public void encerrar(LocalDate encerradoEm) {
        this.encerradoEm = encerradoEm;
    }

    public void alterarConfiguracao(PeriodicidadePlano periodicidade,
            BigDecimal valor,
            LocalDate inicioEm,
            Integer diaVencimento,
            Integer prazoVencimentoDias) {
        this.periodicidade = periodicidade;
        this.valor = valor;
        this.inicioEm = inicioEm;
        this.diaVencimento = diaVencimento;
        this.prazoVencimentoDias = prazoVencimentoDias;
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

    public PeriodicidadePlano getPeriodicidade() {
        return periodicidade;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public Integer getDiaVencimento() {
        return diaVencimento;
    }

    public Integer getPrazoVencimentoDias() {
        return prazoVencimentoDias;
    }

    public LocalDate getInicioEm() {
        return inicioEm;
    }

    public LocalDate getEncerradoEm() {
        return encerradoEm;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }
}
