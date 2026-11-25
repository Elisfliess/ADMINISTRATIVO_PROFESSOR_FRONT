package br.com.compasso.shared.error;

import java.util.List;

import org.springframework.http.HttpStatus;

/**
 * Exceção de negócio traduzida para Problem Details. A mensagem é destinada ao
 * cliente e nunca deve conter dados sensíveis, SQL ou detalhes internos.
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;
    private final transient List<ValidationError> errors;

    public ApiException(HttpStatus status, String code, String message) {
        this(status, code, message, List.of());
    }

    public ApiException(HttpStatus status, String code, String message, List<ValidationError> errors) {
        super(message);
        this.status = status;
        this.code = code;
        this.errors = List.copyOf(errors);
    }

    public HttpStatus status() {
        return status;
    }

    public String code() {
        return code;
    }

    public List<ValidationError> errors() {
        return errors;
    }
}
