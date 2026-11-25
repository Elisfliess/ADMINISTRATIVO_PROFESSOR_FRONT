package br.com.compasso.financeiro.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import br.com.compasso.financeiro.entity.PeriodicidadePlano;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

/** Criação de plano de pagamento conforme o schema {@code PlanoCreate}. */
public record PlanoCreateRequest(
        @NotNull(message = "A periodicidade é obrigatória.")
        PeriodicidadePlano periodicidade,

        @NotNull(message = "O valor é obrigatório.")
        @DecimalMin(value = "0", message = "O valor não pode ser negativo.")
        @jakarta.validation.constraints.Digits(integer = 8, fraction = 2, message = "O valor deve ter no máximo 8 dígitos inteiros e 2 decimais.")
        BigDecimal valor,

        @NotNull(message = "O início do plano é obrigatório.")
        LocalDate inicioEm,

        Integer diaVencimento,

        @jakarta.validation.constraints.Min(value = 0, message = "O prazo de vencimento deve ser maior ou igual a zero.")
        Integer prazoVencimentoDias) {
}
