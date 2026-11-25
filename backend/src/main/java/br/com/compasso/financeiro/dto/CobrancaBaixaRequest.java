package br.com.compasso.financeiro.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/** Requisição de baixa de cobrança. */
public record CobrancaBaixaRequest(

        @NotNull(message = "A data de recebimento é obrigatória.")
        LocalDate recebidoEm,

        @jakarta.validation.constraints.Size(max = 30, message = "A forma de pagamento deve ter no máximo 30 caracteres.")
        String formaPagamento) {
}