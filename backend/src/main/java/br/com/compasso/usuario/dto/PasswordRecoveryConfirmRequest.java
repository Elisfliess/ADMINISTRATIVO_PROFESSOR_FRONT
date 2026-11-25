package br.com.compasso.usuario.dto;

import jakarta.validation.constraints.NotBlank;

public record PasswordRecoveryConfirmRequest(
        @NotBlank(message = "O token é obrigatório.") String token,
        @NotBlank(message = "A nova senha é obrigatória.") String novaSenha) {
}
