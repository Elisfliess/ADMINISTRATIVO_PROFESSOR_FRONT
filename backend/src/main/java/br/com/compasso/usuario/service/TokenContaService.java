package br.com.compasso.usuario.service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.compasso.shared.error.ErrorCodes;
import br.com.compasso.shared.error.Exceptions;
import br.com.compasso.usuario.entity.TipoToken;
import br.com.compasso.usuario.entity.TokenConta;
import br.com.compasso.usuario.repository.TokenContaRepository;

/**
 * Emite e consome tokens de uso único. O banco armazena apenas o hash SHA-256;
 * o valor bruto existe somente em memória e segue no e-mail.
 */
@Service
public class TokenContaService {

    public static final Duration VALIDADE_TOKEN = Duration.ofHours(1);

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Base64.Encoder BASE64_URL = Base64.getUrlEncoder().withoutPadding();

    private final TokenContaRepository tokenContaRepository;
    private final Clock clock;

    public TokenContaService(TokenContaRepository tokenContaRepository, Clock clock) {
        this.tokenContaRepository = tokenContaRepository;
        this.clock = clock;
    }

    @Transactional
    public String emitir(UUID usuarioId, TipoToken tipo) {
        Instant agora = clock.instant();

        // Mantém apenas o token mais recente válido para o mesmo tipo.
        tokenContaRepository.invalidarPendentes(usuarioId, tipo.valorBanco(), agora);

        String tokenBruto = gerarTokenBruto();
        TokenConta token = new TokenConta(usuarioId, tipo, hash(tokenBruto), agora.plus(VALIDADE_TOKEN));
        tokenContaRepository.save(token);

        return tokenBruto;
    }

    @Transactional
    public UUID consumir(String tokenBruto, TipoToken tipo) {
        Instant agora = clock.instant();
        // O SELECT ... FOR UPDATE serializa consumos concorrentes do mesmo token.
        // A transação externa mantém o bloqueio até o commit, garantindo que só
        // uma requisição veja o token ainda não utilizado.
        TokenConta token = tokenContaRepository.buscarParaConsumo(hash(tokenBruto), tipo)
                .orElseThrow(TokenContaService::tokenInvalido);

        if (token.isUsado() || token.estaExpirado(agora)) {
            throw tokenInvalido();
        }

        token.marcarComoUsado(agora);
        return token.getUsuarioId();
    }

    static String hash(String tokenBruto) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return BASE64_URL.encodeToString(digest.digest(tokenBruto.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 indisponível", ex);
        }
    }

    private static String gerarTokenBruto() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return BASE64_URL.encodeToString(bytes);
    }

    private static br.com.compasso.shared.error.ApiException tokenInvalido() {
        return Exceptions.conflito(ErrorCodes.TOKEN_INVALID_OR_EXPIRED,
                "O token é inválido, expirou ou já foi utilizado.");
    }
}
