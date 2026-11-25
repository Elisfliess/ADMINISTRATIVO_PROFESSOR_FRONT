package br.com.compasso.financeiro.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.compasso.financeiro.entity.Cobranca;

import jakarta.persistence.LockModeType;

public interface CobrancaRepository extends JpaRepository<Cobranca, UUID>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<Cobranca> {

    boolean existsByPlanoId(UUID planoId);

    boolean existsByPlanoIdAndPeriodoInicio(UUID planoId, LocalDate periodoInicio);

    boolean existsByAulaId(UUID aulaId);

    Optional<Cobranca> findByIdAndProfessorId(UUID id, UUID professorId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Cobranca c where c.id = :id and c.professorId = :professorId")
    Optional<Cobranca> findByIdAndProfessorIdForUpdate(@Param("id") UUID id, @Param("professorId") UUID professorId);

    List<Cobranca> findByProfessorId(UUID professorId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Cobranca c where c.professorId = :professorId")
    List<Cobranca> findByProfessorIdForUpdate(@Param("professorId") UUID professorId);

    @Query(value = "SELECT c FROM Cobranca c WHERE c.professorId = :professorId AND c.status = :status")
    List<Cobranca> findByProfessorIdAndStatus(@Param("professorId") UUID professorId, @Param("status") String status);

    @Query("SELECT c FROM Cobranca c WHERE c.professorId = :professorId AND c.status = br.com.compasso.financeiro.entity.CobrancaStatus.PAGO AND c.dataPagamento >= :inicioMes AND c.dataPagamento <= :fimMes")
    List<Cobranca> buscarPagasPorMes(@Param("professorId") UUID professorId, @Param("inicioMes") LocalDate inicioMes, @Param("fimMes") LocalDate fimMes);

    /**
     * Insere cobrança mensal de forma atômica e idempotente. Conflito de unicidade
     * (plano/período mensal) não aborta a transação.
     *
     * @return 1 se inseriu, 0 se já existia
     */
    @Modifying
    @Query(value = """
            INSERT INTO pagamento (
                id_pagamento, professor_id, aluno_id, plano_id, aula_id, valor,
                vencimento_em, periodo_inicio, periodo_fim, data_pagamento, status,
                forma_pagamento, descricao, criado_em, atualizado_em
            ) VALUES (
                :id, :professorId, :alunoId, :planoId, NULL, :valor,
                :vencimentoEm, :periodoInicio, :periodoFim, NULL, :status,
                NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
            )
            ON CONFLICT (plano_id, periodo_inicio) WHERE aula_id IS NULL DO NOTHING
            """, nativeQuery = true)
    int inserirMensalSeNaoExistir(@Param("id") UUID id,
            @Param("professorId") UUID professorId,
            @Param("alunoId") UUID alunoId,
            @Param("planoId") UUID planoId,
            @Param("valor") BigDecimal valor,
            @Param("vencimentoEm") LocalDate vencimentoEm,
            @Param("periodoInicio") LocalDate periodoInicio,
            @Param("periodoFim") LocalDate periodoFim,
            @Param("status") String status);

    /**
     * Insere cobrança por aula de forma atômica e idempotente. Conflito de unicidade
     * para a mesma aula ativa não aborta a transação.
     *
     * @return 1 se inseriu, 0 se já existia cobrança ativa para a aula
     */
    @Modifying
    @Query(value = """
            INSERT INTO pagamento (
                id_pagamento, professor_id, aluno_id, plano_id, aula_id, valor,
                vencimento_em, periodo_inicio, periodo_fim, data_pagamento, status,
                forma_pagamento, descricao, criado_em, atualizado_em
            ) VALUES (
                :id, :professorId, :alunoId, :planoId, :aulaId, :valor,
                :vencimentoEm, :periodoInicio, :periodoFim, NULL, :status,
                NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
            )
            ON CONFLICT (aula_id) WHERE aula_id IS NOT NULL AND status IN ('pendente', 'pago') DO NOTHING
            """, nativeQuery = true)
    int inserirPorAulaSeNaoExistir(@Param("id") UUID id,
            @Param("professorId") UUID professorId,
            @Param("alunoId") UUID alunoId,
            @Param("planoId") UUID planoId,
            @Param("aulaId") UUID aulaId,
            @Param("valor") BigDecimal valor,
            @Param("vencimentoEm") LocalDate vencimentoEm,
            @Param("periodoInicio") LocalDate periodoInicio,
            @Param("periodoFim") LocalDate periodoFim,
            @Param("status") String status);
}