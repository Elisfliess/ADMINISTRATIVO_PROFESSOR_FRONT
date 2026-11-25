package br.com.compasso.usuario.service;

import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.compasso.shared.email.EmailDispatcher;
import br.com.compasso.usuario.entity.TipoToken;
import br.com.compasso.usuario.entity.Usuario;

/**
 * Casos de uso de verificação de e-mail e recuperação de senha. Todas as
 * respostas públicas são genéricas: a existência da conta nunca é revelada.
 */
@Service
public class ContaService {

    private final UsuarioService usuarioService;
    private final TokenContaService tokenContaService;
    private final LimitadorDeReenvio limitadorDeReenvio;
    private final EmailDispatcher emailDispatcher;
    private final String urlPublica;

    public ContaService(UsuarioService usuarioService, TokenContaService tokenContaService,
            LimitadorDeReenvio limitadorDeReenvio, EmailDispatcher emailDispatcher,
            @Value("${compasso.app.url-publica}") String urlPublica) {
        this.usuarioService = usuarioService;
        this.tokenContaService = tokenContaService;
        this.limitadorDeReenvio = limitadorDeReenvio;
        this.emailDispatcher = emailDispatcher;
        this.urlPublica = urlPublica;
    }

    @Transactional
    public void enviarEmailDeVerificacao(Usuario usuario) {
        // Emitir o token invalida pedidos anteriores do mesmo tipo.
        String token = tokenContaService.emitir(usuario.getId(), TipoToken.VERIFICACAO_EMAIL);
        emailDispatcher.enviarAposCommit(EmailsDeConta.verificacao(usuario.getEmail(), urlPublica, token));
    }

    @Transactional
    public void reenviarVerificacao(String emailBruto) {
        String email = UsuarioService.normalizarEmail(emailBruto);
        limitadorDeReenvio.verificar(email, TipoToken.VERIFICACAO_EMAIL);

        usuarioService.buscarPorEmail(email)
                .filter(usuario -> usuario.podeAutenticar() && !usuario.isEmailVerificado())
                .ifPresent(this::enviarEmailDeVerificacao);
    }

    @Transactional
    public void verificarEmail(String tokenBruto) {
        UUID usuarioId = tokenContaService.consumir(tokenBruto, TipoToken.VERIFICACAO_EMAIL);
        usuarioService.marcarEmailVerificado(usuarioId);
    }

    @Transactional
    public void solicitarRecuperacaoDeSenha(String emailBruto) {
        String email = UsuarioService.normalizarEmail(emailBruto);
        limitadorDeReenvio.verificar(email, TipoToken.RECUPERACAO_SENHA);

        Optional<Usuario> elegivel = usuarioService.buscarPorEmail(email)
                .filter(usuario -> usuario.podeAutenticar() && usuario.isEmailVerificado());

        if (elegivel.isEmpty()) {
            return;
        }

        Usuario usuario = elegivel.get();
        String token = tokenContaService.emitir(usuario.getId(), TipoToken.RECUPERACAO_SENHA);
        emailDispatcher.enviarAposCommit(
                EmailsDeConta.recuperacaoDeSenha(usuario.getEmail(), urlPublica, token));
    }

    @Transactional
    public void confirmarRecuperacaoDeSenha(String tokenBruto, String novaSenha) {
        PasswordPolicy.validar(novaSenha);
        UUID usuarioId = tokenContaService.consumir(tokenBruto, TipoToken.RECUPERACAO_SENHA);
        usuarioService.definirNovaSenha(usuarioId, novaSenha);
    }

    @Transactional
    public void enviarAvisoDeEmailExistente(String emailBruto) {
        String email = UsuarioService.normalizarEmail(emailBruto);
        emailDispatcher.enviarAposCommit(EmailsDeConta.avisoEmailJaCadastrado(email, urlPublica));
    }
}
