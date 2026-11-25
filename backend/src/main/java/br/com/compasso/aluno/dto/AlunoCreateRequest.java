package br.com.compasso.aluno.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** Cadastro de aluno conforme RN15/RN16 e o schema {@code AlunoCreate}. */
public record AlunoCreateRequest(
        @NotBlank(message = "O nome é obrigatório.")
        @Size(max = 150, message = "O nome deve ter no máximo 150 caracteres.")
        String nome,

        @NotBlank(message = "O telefone é obrigatório.")
        @Size(max = 30, message = "O telefone deve ter no máximo 30 caracteres.")
        String telefone,

        @NotNull(message = "A data de nascimento é obrigatória.")
        @PastOrPresent(message = "A data de nascimento não pode estar no futuro.")
        LocalDate dataNascimento,

        @Email(message = "Informe um endereço de e-mail válido.")
        @Size(max = 254, message = "O e-mail deve ter no máximo 254 caracteres.")
        String email,

        String observacoes,

        @Valid
        ResponsavelLegalRequest responsavelLegal) {
}
