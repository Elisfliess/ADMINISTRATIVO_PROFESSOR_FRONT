package br.com.compasso.usuario.controller;

import java.time.Duration;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.com.compasso.application.usecase.CadastroProfessorUseCase;
import br.com.compasso.usuario.dto.CurrentUserResponse;
import br.com.compasso.usuario.dto.EmailRequest;
import br.com.compasso.usuario.dto.LoginRequest;
import br.com.compasso.usuario.dto.PasswordRecoveryConfirmRequest;
import br.com.compasso.usuario.dto.RegisterRequest;
import br.com.compasso.usuario.dto.VerifyEmailRequest;
import br.com.compasso.usuario.entity.Usuario;
import br.com.compasso.usuario.security.UsuarioAutenticado;
import br.com.compasso.usuario.service.AutenticacaoService;
import br.com.compasso.usuario.service.ContaService;
import br.com.compasso.usuario.service.LegalDocumentService;
import br.com.compasso.usuario.service.UsuarioService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final String COOKIE_SESSAO = "JSESSIONID";

    private final CadastroProfessorUseCase cadastroProfessorUseCase;
    private final AutenticacaoService autenticacaoService;
    private final ContaService contaService;
    private final UsuarioService usuarioService;
    private final LegalDocumentService legalDocumentService;
    private final SecurityContextRepository securityContextRepository;
    private final CookieCsrfTokenRepository csrfTokenRepository;

    public AuthController(CadastroProfessorUseCase cadastroProfessorUseCase,
            AutenticacaoService autenticacaoService, ContaService contaService, UsuarioService usuarioService,
            LegalDocumentService legalDocumentService, SecurityContextRepository securityContextRepository,
            CookieCsrfTokenRepository csrfTokenRepository) {
        this.cadastroProfessorUseCase = cadastroProfessorUseCase;
        this.autenticacaoService = autenticacaoService;
        this.contaService = contaService;
        this.usuarioService = usuarioService;
        this.legalDocumentService = legalDocumentService;
        this.securityContextRepository = securityContextRepository;
        this.csrfTokenRepository = csrfTokenRepository;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void registrar(@Valid @RequestBody RegisterRequest requisicao) {
        cadastroProfessorUseCase.registrar(requisicao);
    }

    @PostMapping("/verify-email")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void verificarEmail(@Valid @RequestBody VerifyEmailRequest requisicao) {
        contaService.verificarEmail(requisicao.token());
    }

    @PostMapping("/verification/resend")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void reenviarVerificacao(@Valid @RequestBody EmailRequest requisicao) {
        contaService.reenviarVerificacao(requisicao.email());
    }

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void login(@Valid @RequestBody LoginRequest requisicao, HttpServletRequest request,
            HttpServletResponse response) {
        Usuario usuario = autenticacaoService.autenticar(requisicao.email(), requisicao.senha());

        HttpSession sessaoExistente = request.getSession(false);
        if (sessaoExistente != null) {
            request.changeSessionId();
        }

        UsuarioAutenticado principal = new UsuarioAutenticado(usuario.getId(), usuario.getEmail());
        var autenticacao = UsernamePasswordAuthenticationToken.authenticated(principal, null,
                List.of(new SimpleGrantedAuthority("ROLE_PROFESSOR")));

        var contexto = SecurityContextHolder.createEmptyContext();
        contexto.setAuthentication(autenticacao);
        SecurityContextHolder.setContext(contexto);
        securityContextRepository.saveContext(contexto, request, response);
        request.getSession(true);

        // Renova o token CSRF depois de autenticar. O login gera e emite um novo
        // token CSRF (cookie XSRF-TOKEN + header X-XSRF-TOKEN); o cliente deve
        // utilizar o novo par. O mecanismo double-submit stateless não revoga
        // server-side um par antigo completo.
        CsrfToken novoToken = csrfTokenRepository.generateToken(request);
        csrfTokenRepository.saveToken(novoToken, request, response);
        request.setAttribute(CsrfToken.class.getName(), novoToken);
        request.setAttribute(novoToken.getParameterName(), novoToken);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        HttpSession sessao = request.getSession(false);
        SecurityContextHolder.clearContext();

        if (sessao != null) {
            sessao.invalidate();
        }

        ResponseCookie cookieRemovido = ResponseCookie.from(COOKIE_SESSAO, "")
                .path(caminhoDoCookie(request))
                .maxAge(Duration.ZERO)
                .httpOnly(true)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookieRemovido.toString());
    }

    @GetMapping("/me")
    public CurrentUserResponse sessaoAtual(@AuthenticationPrincipal UsuarioAutenticado principal) {
        Usuario usuario = usuarioService.obterPorId(principal.id());

        return new CurrentUserResponse(
                usuario.getId(),
                usuario.getEmail(),
                usuario.isEmailVerificado(),
                legalDocumentService.termosAceitos(usuario.getId()));
    }

    @PostMapping("/password-recovery")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void solicitarRecuperacaoDeSenha(@Valid @RequestBody EmailRequest requisicao) {
        contaService.solicitarRecuperacaoDeSenha(requisicao.email());
    }

    @PostMapping("/password-recovery/confirm")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void confirmarRecuperacaoDeSenha(@Valid @RequestBody PasswordRecoveryConfirmRequest requisicao) {
        contaService.confirmarRecuperacaoDeSenha(requisicao.token(), requisicao.novaSenha());
    }

    private String caminhoDoCookie(HttpServletRequest request) {
        String contexto = request.getContextPath();
        return contexto.isEmpty() ? "/" : contexto;
    }
}
