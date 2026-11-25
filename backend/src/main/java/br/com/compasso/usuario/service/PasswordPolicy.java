package br.com.compasso.usuario.service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import br.com.compasso.shared.error.ErrorCodes;
import br.com.compasso.shared.error.Exceptions;
import br.com.compasso.shared.error.ValidationError;

/**
 * Política mínima de senha aprovada para o MVP: no mínimo 8 caracteres, no
 * máximo 72 bytes em UTF-8 (limite do BCrypt) e presença de ao menos uma letra
 * e um dígito. Não são exigidos caracteres especiais obrigatórios.
 */
public final class PasswordPolicy {

    public static final int TAMANHO_MINIMO = 8;
    /** Limite do BCrypt, medido em bytes e não em caracteres. */
    public static final int TAMANHO_MAXIMO = 72;

    private PasswordPolicy() {
    }

    public static void validar(String senha) {
        if (senha == null) {
            throw Exceptions.requisicaoInvalida("Um ou mais campos são inválidos.", List.of(
                    new ValidationError("senha", ErrorCodes.PASSWORD_TOO_SHORT,
                            "A senha deve ter no mínimo 8 caracteres.")));
        }

        List<ValidationError> erros = new ArrayList<>();

        if (senha.length() < TAMANHO_MINIMO) {
            erros.add(new ValidationError("senha", ErrorCodes.PASSWORD_TOO_SHORT,
                    "A senha deve ter no mínimo 8 caracteres."));
        } else if (senha.getBytes(StandardCharsets.UTF_8).length > TAMANHO_MAXIMO) {
            erros.add(new ValidationError("senha", ErrorCodes.PASSWORD_TOO_LONG,
                    "A senha deve ter no máximo 72 bytes em UTF-8."));
        }

        boolean possuiLetra = senha.chars().anyMatch(Character::isLetter);
        boolean possuiDigito = senha.chars().anyMatch(Character::isDigit);

        if (!possuiLetra || !possuiDigito) {
            erros.add(new ValidationError("senha", ErrorCodes.PASSWORD_TOO_WEAK,
                    "A senha deve conter ao menos uma letra e um número."));
        }

        if (!erros.isEmpty()) {
            throw Exceptions.requisicaoInvalida("Um ou mais campos são inválidos.", erros);
        }
    }
}
