package br.com.compasso.aluno.entity;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * Chave composta de {@code aluno}: o identificador do aluno junto ao professor
 * proprietário. O schema garante, por FK composta, que todas as referências ao
 * aluno carreguem também o professor.
 */
public class AlunoId implements Serializable {

    private UUID id;
    private UUID professorId;

    public AlunoId() {
    }

    public AlunoId(UUID id, UUID professorId) {
        this.id = id;
        this.professorId = professorId;
    }

    public UUID getId() {
        return id;
    }

    public UUID getProfessorId() {
        return professorId;
    }

    @Override
    public boolean equals(Object outro) {
        if (this == outro) {
            return true;
        }
        if (!(outro instanceof AlunoId alunoId)) {
            return false;
        }
        return Objects.equals(id, alunoId.id) && Objects.equals(professorId, alunoId.professorId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, professorId);
    }
}
