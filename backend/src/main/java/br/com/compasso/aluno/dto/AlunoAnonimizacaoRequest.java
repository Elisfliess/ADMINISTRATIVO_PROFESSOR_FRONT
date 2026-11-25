package br.com.compasso.aluno.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;

/** Confirmação explícita e senha exigidas para anonimizar um aluno (RF40). */
public record AlunoAnonimizacaoRequest(
        @NotBlank(message = "A senha é obrigatória.")
        String senha,

        @AssertTrue(message = "A confirmação explícita é obrigatória.")
        boolean confirmar) {
}
