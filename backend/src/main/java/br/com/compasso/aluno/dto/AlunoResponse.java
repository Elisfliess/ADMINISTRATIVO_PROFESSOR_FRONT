package br.com.compasso.aluno.dto;

import java.time.LocalDate;
import java.util.UUID;

import br.com.compasso.aluno.entity.Aluno;

/** Representação do aluno conforme o schema {@code Aluno} do contrato. */
public record AlunoResponse(
        UUID id,
        String nome,
        String telefone,
        LocalDate dataNascimento,
        String email,
        String observacoes,
        ResponsavelLegalResponse responsavelLegal,
        boolean ativo) {

    public static AlunoResponse de(Aluno aluno) {
        ResponsavelLegalResponse responsavel = aluno.getResponsavelNome() == null
                ? null
                : new ResponsavelLegalResponse(aluno.getResponsavelNome(), aluno.getResponsavelContato());

        return new AlunoResponse(
                aluno.getId(),
                aluno.getNome(),
                aluno.getTelefone(),
                aluno.getDataNascimento(),
                aluno.getEmail(),
                aluno.getObservacoes(),
                responsavel,
                aluno.isAtivo());
    }
}
