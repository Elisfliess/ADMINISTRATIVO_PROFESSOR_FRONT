package br.com.compasso.financeiro.service;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Serviço público de geração de cobranças, disponibilizado para outros módulos
 * (ex.: Agenda) sem expor repositórios ou entidades internas do Financeiro.
 */
public interface GeracaoCobrancaService {

    /**
     * Gera cobranças mensais para todos os planos mensais ativos.
     * Idempotente: executar mais de uma vez não duplica cobranças.
     * Recupera períodos não processados após indisponibilidade.
     */
    void gerarCobrancasMensais();

    /**
     * Gera uma cobrança por aula para um plano com periodicidade POR_AULA.
     *
     * @param professorId identificador do professor
     * @param alunoId     identificador do aluno
     * @param aulaId      identificador da aula
     * @param dataAula    data local da aula em America/Sao_Paulo
     */
    void gerarCobrancaPorAula(UUID professorId, UUID alunoId, UUID aulaId, LocalDate dataAula);
}