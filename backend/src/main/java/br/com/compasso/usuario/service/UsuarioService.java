package br.com.compasso.usuario.service;

import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.compasso.shared.error.Exceptions;
import br.com.compasso.usuario.entity.Usuario;
import br.com.compasso.usuario.repository.UsuarioRepository;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final String hashFicticio;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        // Hash descartável usado para manter tempo de resposta semelhante quando
        // o e-mail informado não existe.
        this.hashFicticio = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    public static String normalizarEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    @Transactional(readOnly = true)
    public Optional<Usuario> buscarPorEmail(String emailNormalizado) {
        return usuarioRepository.findByEmail(emailNormalizado);
    }

    @Transactional(readOnly = true)
    public Usuario obterPorId(UUID id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> Exceptions.naoEncontrado("Conta não encontrada."));
    }

    @Transactional
    public Usuario criarConta(String emailNormalizado, String senha) {
        PasswordPolicy.validar(senha);
        Usuario usuario = new Usuario(emailNormalizado, passwordEncoder.encode(senha));
        return usuarioRepository.save(usuario);
    }

    @Transactional
    public void marcarEmailVerificado(UUID usuarioId) {
        Usuario usuario = obterPorId(usuarioId);
        usuario.marcarEmailVerificado(Instant.now());
    }

    @Transactional
    public void definirNovaSenha(UUID usuarioId, String novaSenha) {
        Usuario usuario = obterPorId(usuarioId);
        usuario.alterarSenha(passwordEncoder.encode(novaSenha));
        usuario.desbloquear();
    }

    public boolean senhaConfere(String senha, String senhaHash) {
        return passwordEncoder.matches(senha, senhaHash);
    }

    /** Consome tempo equivalente a uma verificação real, sem revelar ausência da conta. */
    public void executarComparacaoFicticia(String senha) {
        passwordEncoder.matches(senha, hashFicticio);
    }
}
