package br.com.compasso.financeiro.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.compasso.financeiro.entity.PeriodicidadePlano;
import br.com.compasso.financeiro.entity.PlanoPagamento;

import jakarta.persistence.LockModeType;

public interface PlanoPagamentoRepository extends JpaRepository<PlanoPagamento, UUID> {
    /** Consulta sempre limitada ao professor proprietário. */
    Optional<PlanoPagamento> findByIdAndProfessorId(UUID id, UUID professorId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PlanoPagamento p where p.id = :id and p.professorId = :professorId")
    Optional<PlanoPagamento> findByIdAndProfessorIdForUpdate(@Param("id") UUID id, @Param("professorId") UUID professorId);

    Optional<PlanoPagamento> findByAlunoIdAndProfessorIdAndEncerradoEmIsNull(UUID alunoId, UUID professorId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PlanoPagamento p where p.alunoId = :alunoId and p.professorId = :professorId and p.encerradoEm is null")
    Optional<PlanoPagamento> findByAlunoIdAndProfessorIdAndEncerradoEmIsNullForUpdate(@Param("alunoId") UUID alunoId, @Param("professorId") UUID professorId);

    List<PlanoPagamento> findByAlunoIdAndProfessorId(UUID alunoId, UUID professorId);

    List<PlanoPagamento> findByPeriodicidadeAndEncerradoEmIsNull(PeriodicidadePlano periodicidade);
}