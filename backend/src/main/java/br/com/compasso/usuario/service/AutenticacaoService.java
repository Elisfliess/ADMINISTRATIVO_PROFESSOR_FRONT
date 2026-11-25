package br.com.compasso.usuario.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.compasso.shared.error.ApiException;
import br.com.compasso.shared.error.ErrorCodes;
import br.com.compasso.shared.error.Exceptions;
import br.com.compasso.shared.error.LimiteExcedidoException;
import br.com.compasso.usuario.entity.Usuario;

/**
 * Autenticação por credenciais com bloqueio temporário após cinco falhas
 * consecutivas. A resposta não distingue e-mail existente de inexistente.
 */
@Service
public class AutenticacaoService {

    private final UsuarioService usuarioService;
    private final TentativasEmailInexistente tentativasEmailInexistente;
    private final Clock clock;

    public AutenticacaoService(UsuarioService usuarioService, TentativasEmailInexistente tentativasEmailInexistente,
            Clock clock) {
        this.usuarioService = usuarioService;
        this.tentativasEmailInexistente = tentativasEmailInexistente;
        this.clock = clock;
    }

    @Transactional(noRollbackFor = ApiException.class)
    public Usuario autenticar(String emailBruto, String senha) {
        String email = UsuarioService.normalizarEmail(emailBruto);
        Instant agora = clock.instant();

        Optional<Usuario> encontrado = usuarioService.buscarPorEmail(email);

        if (encontrado.isEmpty()) {
            usuarioService.executarComparacaoFicticia(senha);
            tentativasEmailInexistente.registrarFalha(email, agora);
            throw credenciaisInvalidas();
        }

        Usuario usuario = encontrado.get();

        if (usuario.estaBloqueado(agora)) {
            throw bloqueado(agora, usuario.getBloqueadoAte());
        }

        if (!usuarioService.senhaConfere(senha, usuario.getSenhaHash())) {
            usuario.registrarFalhaDeLogin(agora);

            if (usuario.estaBloqueado(agora)) {
                throw bloqueado(agora, usuario.getBloqueadoAte());
            }

            throw credenciaisInvalidas();
        }

        if (!usuario.podeAutenticar()) {
            throw Exceptions.acessoNegado(ErrorCodes.ACCOUNT_DISABLED, "Esta conta não está disponível para acesso.");
        }

        if (!usuario.isEmailVerificado()) {
            throw Exceptions.acessoNegado(ErrorCodes.EMAIL_NOT_VERIFIED,
                    "Confirme o endereço de e-mail antes de acessar.");
        }

        usuario.registrarAcessoBemSucedido(agora);
        return usuario;
    }

    /**
     * Confirma a senha do usuário autenticado para operações sensíveis sem
     * alterar contadores de tentativa ou bloqueio de login.
     */
    @Transactional(readOnly = true)
    public boolean confirmarSenha(UUID usuarioId, String senha) {
        Usuario usuario = usuarioService.obterPorId(usuarioId);
        return usuarioService.senhaConfere(senha, usuario.getSenhaHash());
    }

    private LimiteExcedidoException bloqueado(Instant agora, Instant bloqueadoAte) {
        return new LimiteExcedidoException(ErrorCodes.ACCOUNT_LOCKED,
                "Muitas tentativas de acesso. Tente novamente mais tarde.", Duration.between(agora, bloqueadoAte));
    }

    private br.com.compasso.shared.error.ApiException credenciaisInvalidas() {
        return Exceptions.naoAutenticado(ErrorCodes.INVALID_CREDENTIALS, "E-mail ou senha inválidos.");
    }
}
