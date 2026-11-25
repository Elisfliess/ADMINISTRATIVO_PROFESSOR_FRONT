package br.com.compasso.aluno.dto;

import java.util.List;

/** Resumo informativo da anonimização, sem alterar dados. */
public record AlunoAnonimizacaoPreviaResponse(
        List<String> dadosPessoais,
        List<String> registrosPreservados) {
}
