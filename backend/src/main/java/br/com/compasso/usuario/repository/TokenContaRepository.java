package br.com.compasso.usuario.repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.compasso.usuario.entity.TipoToken;
import br.com.compasso.usuario.entity.TokenConta;
import jakarta.persistence.LockModeType;

public interface TokenContaRepository extends JpaRepository<TokenConta, UUID> {

    /**
     * Carrega o token com bloqueio pessimista para que validação e consumo
     * sejam atômicos mesmo com requisições concorrentes.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT t
              FROM TokenConta t
             WHERE t.tokenHash = :tokenHash
               AND t.tipo = :tipo
            """)
    Optional<TokenConta> buscarParaConsumo(@Param("tokenHash") String tokenHash,
            @Param("tipo") TipoToken tipo);

    @Modifying(flushAutomatically = true)
    @Query(value = """
            UPDATE token_conta
               SET usado_em = :agora
             WHERE usuario_id = :usuarioId
               AND tipo = :tipo
               AND usado_em IS NULL
            """, nativeQuery = true)
    int invalidarPendentes(@Param("usuarioId") UUID usuarioId, @Param("tipo") String tipo,
            @Param("agora") Instant agora);
}
