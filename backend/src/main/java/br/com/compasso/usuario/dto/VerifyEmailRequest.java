package br.com.compasso.usuario.dto;

import jakarta.validation.constraints.NotBlank;

public record VerifyEmailRequest(@NotBlank(message = "O token é obrigatório.") String token) {
}
