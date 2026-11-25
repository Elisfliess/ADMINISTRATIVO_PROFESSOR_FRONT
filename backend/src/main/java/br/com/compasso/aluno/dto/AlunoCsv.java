package br.com.compasso.aluno.dto;

/** Arquivo CSV gerado para exportação de dados do aluno. */
public record AlunoCsv(String nomeArquivo, byte[] conteudo) {
}
