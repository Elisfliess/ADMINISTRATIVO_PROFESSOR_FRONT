package br.com.compasso.shared.error;

import java.net.URI;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import br.com.compasso.shared.web.RequestIdFilter;
import jakarta.servlet.http.HttpServletRequest;

/** Monta Problem Details (RFC 9457) com as extensões exigidas pelo contrato. */
public final class ProblemDetailFactory {

    private ProblemDetailFactory() {
    }

    public static ProblemDetail criar(HttpServletRequest request, HttpStatus status, String code, String title,
            String detail, List<ValidationError> errors) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, detail);
        problema.setType(URI.create("about:blank"));
        problema.setTitle(title);
        problema.setInstance(URI.create(request.getRequestURI()));
        problema.setProperty("code", code);
        problema.setProperty("requestId", RequestIdFilter.obter(request));

        if (errors != null && !errors.isEmpty()) {
            problema.setProperty("errors", errors);
        }

        return problema;
    }

    public static String tituloDe(HttpStatus status) {
        return switch (status) {
            case BAD_REQUEST -> "Requisição inválida";
            case UNAUTHORIZED -> "Não autenticado";
            case FORBIDDEN -> "Acesso negado";
            case NOT_FOUND -> "Recurso não encontrado";
            case METHOD_NOT_ALLOWED -> "Método não permitido";
            case CONFLICT -> "Conflito";
            case TOO_MANY_REQUESTS -> "Limite excedido";
            default -> "Erro interno";
        };
    }
}
