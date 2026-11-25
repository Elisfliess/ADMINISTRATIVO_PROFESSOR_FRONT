package br.com.compasso.financeiro.dto;

/** Arquivo CSV gerado pela exportação mensal de cobranças pagas. */
public record CobrancaCsv(String nomeArquivo, byte[] conteudo) {
}
