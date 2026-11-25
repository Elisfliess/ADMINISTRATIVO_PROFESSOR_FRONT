package br.com.compasso.financeiro.dto;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

/** Representação de cobrança conforme o schema {@code Cobranca} do contrato. */
public class CobrancaResponse {

    private static final ZoneId ZONA_CALENDARIO = ZoneId.of("America/Sao_Paulo");

    private final UUID id;
    private final UUID alunoId;
    private final UUID planoId;
    private final UUID aulaId;
    private final BigDecimal valor;
    private final LocalDate periodoInicio;
    private final LocalDate periodoFim;
    private final LocalDate vencimentoEm;
    private final String status;
    private final String situacao;
    private final LocalDate recebidoEm;
    private final String formaPagamento;

    public CobrancaResponse(UUID id, UUID alunoId, UUID planoId, UUID aulaId,
            BigDecimal valor, LocalDate periodoInicio, LocalDate periodoFim,
            LocalDate vencimentoEm, String status, String situacao, LocalDate recebidoEm, String formaPagamento) {
        this.id = id;
        this.alunoId = alunoId;
        this.planoId = planoId;
        this.aulaId = aulaId;
        this.valor = valor;
        this.periodoInicio = periodoInicio;
        this.periodoFim = periodoFim;
        this.vencimentoEm = vencimentoEm;
        this.status = status;
        this.situacao = situacao;
        this.recebidoEm = recebidoEm;
        this.formaPagamento = formaPagamento;
    }

    public static CobrancaResponse de(br.com.compasso.financeiro.entity.Cobranca cobranca, Clock clock) {
        String situacao = calcularSituacao(cobranca, clock);
        return new CobrancaResponse(
                cobranca.getId(),
                cobranca.getAlunoId(),
                cobranca.getPlanoId(),
                cobranca.getAulaId(),
                cobranca.getValor(),
                cobranca.getPeriodoInicio(),
                cobranca.getPeriodoFim(),
                cobranca.getVencimentoEm(),
                cobranca.getStatus().toString(),
                situacao,
                cobranca.getDataPagamento(),
                cobranca.getFormaPagamento());
    }

    public static CobrancaResponse de(br.com.compasso.financeiro.entity.Cobranca cobranca) {
        return de(cobranca, Clock.systemDefaultZone());
    }

    private static String calcularSituacao(br.com.compasso.financeiro.entity.Cobranca cobranca, Clock clock) {
        if (cobranca.getStatus() != br.com.compasso.financeiro.entity.CobrancaStatus.PENDENTE) {
            return cobranca.getStatus().toString();
        }
        LocalDate hoje = LocalDate.now(clock.withZone(ZONA_CALENDARIO));
        if (cobranca.getVencimentoEm() != null && cobranca.getVencimentoEm().isBefore(hoje)) {
            return "EM_ATRASO";
        }
        return "PENDENTE";
    }

    public UUID getId() { return id; }
    public UUID getAlunoId() { return alunoId; }
    public UUID getPlanoId() { return planoId; }
    public UUID getAulaId() { return aulaId; }
    public BigDecimal getValor() { return valor; }
    public LocalDate getPeriodoInicio() { return periodoInicio; }
    public LocalDate getPeriodoFim() { return periodoFim; }
    public LocalDate getVencimentoEm() { return vencimentoEm; }
    public String getStatus() { return status; }
    public String getSituacao() { return situacao; }
    public LocalDate getRecebidoEm() { return recebidoEm; }
    public String getFormaPagamento() { return formaPagamento; }
}