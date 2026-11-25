package br.com.compasso.professor.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.compasso.professor.dto.ProfessorResponse;
import br.com.compasso.professor.service.ProfessorService;
import br.com.compasso.usuario.security.UsuarioAutenticado;

@RestController
@RequestMapping("/api/v1/professor")
public class ProfessorController {

    private final ProfessorService professorService;

    public ProfessorController(ProfessorService professorService) {
        this.professorService = professorService;
    }

    @GetMapping
    public ProfessorResponse obter(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return ProfessorResponse.de(professorService.obterPorUsuarioId(usuario.id()));
    }
}
