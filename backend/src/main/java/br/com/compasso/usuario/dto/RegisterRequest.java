package br.com.compasso.usuario.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "O e-mail é obrigatório.")
        @Email(message = "Informe um endereço de e-mail válido.")
        @Size(max = 254, message = "O e-mail deve ter no máximo 254 caracteres.")
        String email,

        @NotBlank(message = "A senha é obrigatória.")
        String senha,

        @NotBlank(message = "O nome é obrigatório.")
        @Size(max = 150, message = "O nome deve ter no máximo 150 caracteres.")
        String nome,

        @NotBlank(message = "A razão social é obrigatória.")
        @Size(max = 200, message = "A razão social deve ter no máximo 200 caracteres.")
        String razaoSocial,

        @NotBlank(message = "O CNPJ é obrigatório.")
        @Pattern(regexp = "^[0-9A-Z]{12}[0-9]{2}$",
                message = "O CNPJ deve ter 14 caracteres: 12 alfanuméricos (A-Z ou 0-9) e 2 dígitos verificadores.")
        String cnpj,

        @Email(message = "Informe um endereço de e-mail de contato válido.")
        @Size(max = 254, message = "O e-mail de contato deve ter no máximo 254 caracteres.")
        String emailContato,

        @Size(max = 30, message = "O telefone de contato deve ter no máximo 30 caracteres.")
        String telefoneContato) {
}
