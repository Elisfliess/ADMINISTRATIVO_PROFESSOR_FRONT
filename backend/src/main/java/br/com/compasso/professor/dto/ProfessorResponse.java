package br.com.compasso.professor.dto;

import java.util.UUID;

import br.com.compasso.professor.entity.Professor;

/** Dados cadastrais do professor autenticado, sem atributos internos. */
public record ProfessorResponse(
        UUID id,
        String nome,
        String razaoSocial,
        String cnpj,
        String emailContato,
        String telefoneContato,
        String instagramUrl) {

    public static ProfessorResponse de(Professor professor) {
        return new ProfessorResponse(
                professor.getId(),
                professor.getNome(),
                professor.getRazaoSocial(),
                professor.getCnpj(),
                professor.getEmailContato(),
                professor.getTelefoneContato(),
                professor.getInstagramUrl());
    }
}
