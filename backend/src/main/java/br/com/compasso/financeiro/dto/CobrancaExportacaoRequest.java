package br.com.compasso.financeiro.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Confirmação de senha e mês exigidos pela exportação mensal de cobranças (RF39). */
public record CobrancaExportacaoRequest(

        @NotBlank(message = "O mês é obrigatório.")
        @Pattern(regexp = "^[0-9]{4}-(0[1-9]|1[0-2])$", message = "O mês deve estar no formato YYYY-MM.")
        String mes,

        @NotBlank(message = "A senha é obrigatória.")
        String senha) {
}