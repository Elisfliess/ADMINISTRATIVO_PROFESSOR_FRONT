package br.com.compasso.application.security;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import br.com.compasso.shared.error.ErrorCodes;
import br.com.compasso.shared.error.ProblemResponseWriter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.ObjectMapper;

/** Resposta 401 em Problem Details para requisições sem sessão válida. */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public RestAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException {
        ProblemResponseWriter.escrever(objectMapper, request, response, HttpStatus.UNAUTHORIZED,
                ErrorCodes.UNAUTHORIZED, "É necessário autenticar para acessar este recurso.", null);
    }
}
