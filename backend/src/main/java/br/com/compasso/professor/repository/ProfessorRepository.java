package br.com.compasso.professor.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.compasso.professor.entity.Professor;

public interface ProfessorRepository extends JpaRepository<Professor, UUID> {

    Optional<Professor> findByIdUsuario(UUID idUsuario);

    boolean existsByCnpj(String cnpj);
}
