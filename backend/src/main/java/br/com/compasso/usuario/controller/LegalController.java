package br.com.compasso.usuario.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.com.compasso.usuario.dto.LegalAcceptanceRequest;
import br.com.compasso.usuario.security.UsuarioAutenticado;
import br.com.compasso.usuario.service.LegalDocumentService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/legal")
public class LegalController {

    private final LegalDocumentService legalDocumentService;

    public LegalController(LegalDocumentService legalDocumentService) {
        this.legalDocumentService = legalDocumentService;
    }

    @PostMapping("/acceptances")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void aceitar(@AuthenticationPrincipal UsuarioAutenticado usuario,
            @Valid @RequestBody LegalAcceptanceRequest requisicao) {
        legalDocumentService.registrarAceite(usuario.id(), requisicao.termosVersao(), requisicao.politicaVersao());
    }
}
