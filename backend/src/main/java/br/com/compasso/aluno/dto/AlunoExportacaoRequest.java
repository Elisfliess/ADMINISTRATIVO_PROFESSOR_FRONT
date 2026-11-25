package br.com.compasso.aluno.dto;

import jakarta.validation.constraints.NotBlank;

/** Confirmação de senha exigida pelas operações legais de dados (RF39/RF40). */
public record AlunoExportacaoRequest(
        @NotBlank(message = "A senha é obrigatória.")
        String senha) {
}
