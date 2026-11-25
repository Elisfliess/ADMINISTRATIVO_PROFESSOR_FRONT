package br.com.compasso.shared.web;

import java.io.IOException;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import br.com.compasso.shared.uuid.UuidV7;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Gera o identificador de correlação de cada requisição. O valor é devolvido no
 * header {@code X-Request-Id}, disponível no Problem Details como
 * {@code requestId} e incluído no MDC dos logs.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

    public static final String ATRIBUTO = "compasso.requestId";
    public static final String HEADER = "X-Request-Id";
    public static final String MDC_CHAVE = "requestId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String requestId = UuidV7.generate().toString();
        request.setAttribute(ATRIBUTO, requestId);
        response.setHeader(HEADER, requestId);

        try (MDC.MDCCloseable ignored = MDC.putCloseable(MDC_CHAVE, requestId)) {
            chain.doFilter(request, response);
        }
    }

    public static String obter(HttpServletRequest request) {
        Object requestId = request.getAttribute(ATRIBUTO);
        return requestId == null ? null : requestId.toString();
    }
}
