package br.com.compasso.professor.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.compasso.professor.entity.Professor;
import br.com.compasso.professor.repository.ProfessorRepository;
import br.com.compasso.shared.error.Exceptions;

@Service
public class ProfessorService {

    private final ProfessorRepository professorRepository;

    public ProfessorService(ProfessorRepository professorRepository) {
        this.professorRepository = professorRepository;
    }

    @Transactional(readOnly = true)
    public Professor obterPorUsuarioId(UUID idUsuario) {
        return professorRepository.findByIdUsuario(idUsuario)
                .orElseThrow(() -> Exceptions.naoEncontrado("Professor não encontrado para a conta autenticada."));
    }

    @Transactional(readOnly = true)
    public boolean cnpjJaCadastrado(String cnpj) {
        return professorRepository.existsByCnpj(cnpj);
    }

    @Transactional
    public Professor criar(UUID idUsuario, String nome, String razaoSocial, String cnpj, String emailContato,
            String telefoneContato) {
        Professor professor = new Professor(idUsuario, nome, razaoSocial, cnpj, emailContato, telefoneContato);
        return professorRepository.save(professor);
    }
}
