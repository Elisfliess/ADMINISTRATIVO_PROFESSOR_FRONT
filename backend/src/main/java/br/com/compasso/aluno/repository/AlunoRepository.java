package br.com.compasso.aluno.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.compasso.aluno.entity.Aluno;
import br.com.compasso.aluno.entity.AlunoId;

public interface AlunoRepository extends JpaRepository<Aluno, AlunoId> {

    /** Consulta sempre limitada ao professor proprietário. */
    @Query("select a from Aluno a where a.id = :id and a.professorId = :professorId")
    Optional<Aluno> buscarPorIdEProfessor(@Param("id") UUID id, @Param("professorId") UUID professorId);

    Page<Aluno> findByProfessorIdAndAtivo(UUID professorId, boolean ativo, Pageable pageable);

    Page<Aluno> findByProfessorIdAndNomeContainingIgnoreCaseAndAtivo(UUID professorId, String nome, boolean ativo,
            Pageable pageable);
}
