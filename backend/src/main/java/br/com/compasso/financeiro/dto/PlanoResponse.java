package br.com.compasso.financeiro.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import br.com.compasso.financeiro.entity.PlanoPagamento;
import br.com.compasso.financeiro.entity.PeriodicidadePlano;

/** Representação de plano conforme o schema {@code Plano} do contrato. */
public record PlanoResponse(
        UUID id,
        PeriodicidadePlano periodicidade,
        BigDecimal valor,
        LocalDate inicioEm,
        Integer diaVencimento,
        Integer prazoVencimentoDias,
        String status,
        LocalDate encerradoEm) {

    public static PlanoResponse de(PlanoPagamento plano) {
        return new PlanoResponse(
                plano.getId(),
                plano.getPeriodicidade(),
                plano.getValor(),
                plano.getInicioEm(),
                plano.getDiaVencimento(),
                plano.getPrazoVencimentoDias(),
                plano.isAtivo() ? "ATIVO" : "ENCERRADO",
                plano.getEncerradoEm());
    }
}
