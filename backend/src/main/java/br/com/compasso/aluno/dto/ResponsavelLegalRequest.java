package br.com.compasso.aluno.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Responsável legal exigido para alunos menores de 18 anos (RN16). */
public record ResponsavelLegalRequest(
        @NotBlank(message = "O nome do responsável legal é obrigatório.")
        @Size(max = 150, message = "O nome do responsável legal deve ter no máximo 150 caracteres.")
        String nome,

        @NotBlank(message = "O contato do responsável legal é obrigatório.")
        @Size(max = 30, message = "O contato do responsável legal deve ter no máximo 30 caracteres.")
        String contato) {
}
