package br.com.compasso.shared.error;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.ObjectMapper;

/**
 * Serializa Problem Details fora do ciclo do MVC, usado por filtros de
 * segurança e de autorização que respondem antes dos controllers.
 */
public final class ProblemResponseWriter {

    public static final MediaType PROBLEM_JSON = MediaType.parseMediaType("application/problem+json");

    private ProblemResponseWriter() {
    }

    public static void escrever(ObjectMapper objectMapper, HttpServletRequest request, HttpServletResponse response,
            HttpStatus status, String code, String detail, List<ValidationError> errors) throws IOException {
        ProblemDetail problema = ProblemDetailFactory.criar(request, status, code,
                ProblemDetailFactory.tituloDe(status), detail, errors);

        response.setStatus(status.value());
        response.setContentType(PROBLEM_JSON.toString());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getOutputStream(), problema);
    }
}
