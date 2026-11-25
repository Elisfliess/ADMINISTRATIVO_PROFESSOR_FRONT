package br.com.compasso.usuario.dto;

import jakarta.validation.constraints.NotBlank;

public record LegalAcceptanceRequest(
        @NotBlank(message = "A versão dos termos é obrigatória.") String termosVersao,
        @NotBlank(message = "A versão da política é obrigatória.") String politicaVersao) {
}
