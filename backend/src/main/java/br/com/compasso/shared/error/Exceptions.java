package br.com.compasso.shared.error;

import java.util.List;

import org.springframework.http.HttpStatus;

public final class Exceptions {

    private Exceptions() {
    }

    public static ApiException requisicaoInvalida(String code, String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, code, message);
    }

    public static ApiException requisicaoInvalida(String message, List<ValidationError> errors) {
        return new ApiException(HttpStatus.BAD_REQUEST, ErrorCodes.VALIDATION_ERROR, message, errors);
    }

    public static ApiException naoAutenticado(String code, String message) {
        return new ApiException(HttpStatus.UNAUTHORIZED, code, message);
    }

    public static ApiException acessoNegado(String code, String message) {
        return new ApiException(HttpStatus.FORBIDDEN, code, message);
    }

    public static ApiException naoEncontrado(String message) {
        return new ApiException(HttpStatus.NOT_FOUND, ErrorCodes.NOT_FOUND, message);
    }

    public static ApiException naoEncontrado(String code, String message) {
        return new ApiException(HttpStatus.NOT_FOUND, code, message);
    }

    public static ApiException conflito(String code, String message) {
        return new ApiException(HttpStatus.CONFLICT, code, message);
    }
}
