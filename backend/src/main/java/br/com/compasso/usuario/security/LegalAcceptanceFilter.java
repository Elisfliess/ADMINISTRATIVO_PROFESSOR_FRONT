package br.com.compasso.usuario.security;

import java.io.IOException;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import br.com.compasso.shared.error.ErrorCodes;
import br.com.compasso.shared.error.ProblemResponseWriter;
import br.com.compasso.usuario.service.LegalDocumentService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.ObjectMapper;

/**
 * Enquanto o aceite dos documentos legais estiver pendente, bloqueia as demais
 * funções administrativas. Leitura dos documentos (frontend), logout,
 * consulta da sessão, aceite, exportação e encerramento permanecem
 * disponíveis (RF35).
 *
 * <p>Não é um bean de filtro independente: é registrado apenas dentro da cadeia
 * do Spring Security, para rodar depois da autenticação e uma única vez.</p>
 */
public class LegalAcceptanceFilter extends OncePerRequestFilter {

    private static final String PREFIXO_API = "/api/";
    private static final Set<String> PERMITIDOS = Set.of(
            "/api/v1/auth/logout",
            "/api/v1/auth/me",
            "/api/v1/legal/acceptances",
            "/api/v1/account/exportacao",
            "/api/v1/account/encerramento");

    private final LegalDocumentService legalDocumentService;
    private final ObjectMapper objectMapper;

    public LegalAcceptanceFilter(LegalDocumentService legalDocumentService, ObjectMapper objectMapper) {
        this.legalDocumentService = legalDocumentService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();

        if (autenticacao == null || !(autenticacao.getPrincipal() instanceof UsuarioAutenticado usuario)) {
            chain.doFilter(request, response);
            return;
        }

        String caminho = caminho(request);

        if (!caminho.startsWith(PREFIXO_API) || PERMITIDOS.contains(caminho)) {
            chain.doFilter(request, response);
            return;
        }

        if (!legalDocumentService.termosAceitos(usuario.id())) {
            ProblemResponseWriter.escrever(objectMapper, request, response, HttpStatus.FORBIDDEN,
                    ErrorCodes.LEGAL_ACCEPTANCE_REQUIRED,
                    "É necessário aceitar os Termos de Uso e a Política de Privacidade.", null);
            return;
        }

        chain.doFilter(request, response);
    }

    private String caminho(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String contexto = request.getContextPath();
        return contexto.isEmpty() ? uri : uri.substring(contexto.length());
    }
}
